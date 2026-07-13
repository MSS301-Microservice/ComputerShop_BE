package sp26.group3.computer.sba301_computershop.service.impl;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sp26.group3.computer.sba301_computershop.client.CartServiceClient;
import sp26.group3.computer.sba301_computershop.client.OrderServiceClient;
import sp26.group3.computer.sba301_computershop.config.VNPayConfig;
import sp26.group3.computer.sba301_computershop.dto.request.payment.CreatePaymentSchedulesRequest;
import sp26.group3.computer.sba301_computershop.dto.request.payment.VnpayUrlRequest;
import sp26.group3.computer.sba301_computershop.dto.response.PaymentDTO;
import sp26.group3.computer.sba301_computershop.dto.response.PaymentScheduleResponse;
import sp26.group3.computer.sba301_computershop.dto.response.cart.InternalCartResponse;
import sp26.group3.computer.sba301_computershop.dto.response.order.InternalOrderResponse;
import sp26.group3.computer.sba301_computershop.dto.response.payment.InstallmentPackageResponse;
import sp26.group3.computer.sba301_computershop.entity.InstallmentPackage;
import sp26.group3.computer.sba301_computershop.entity.OrderPaymentSchedule;
import sp26.group3.computer.sba301_computershop.enums.PaymentStatus;
import sp26.group3.computer.sba301_computershop.enums.PaymentMode;
import sp26.group3.computer.sba301_computershop.exception.AppException;
import sp26.group3.computer.sba301_computershop.exception.ErrorCode;
import sp26.group3.computer.sba301_computershop.repository.InstallmentPackageRepository;
import sp26.group3.computer.sba301_computershop.repository.OrderPaymentScheduleRepository;
import sp26.group3.computer.sba301_computershop.service.PaymentService;
import sp26.group3.computer.sba301_computershop.util.VNPayUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    VNPayConfig vnPayConfig;
    OrderPaymentScheduleRepository orderPaymentScheduleRepository;
    InstallmentPackageRepository installmentPackageRepository;
    CartServiceClient cartServiceClient;
    OrderServiceClient orderServiceClient;

    @Override
    public PaymentDTO createVnPayPayment(HttpServletRequest request, int orderId, String bankCode,
            Integer installmentNo) {
        String paymentUrl = buildVnPayUrl(orderId, bankCode, installmentNo, VNPayUtil.getIpAddress(request));
        return PaymentDTO.builder()
                .code("00")
                .message("success")
                .paymentUrl(paymentUrl)
                .build();
    }

    @Override
    public String createVnPayUrlInternal(VnpayUrlRequest request) {
        return buildVnPayUrl(request.getOrderId(), request.getBankCode(), request.getInstallmentNo(), request.getIpAddress());
    }

    /** Dùng chung bởi cả createPayment (khách hàng retry qua PaymentController) lẫn
     * order-service (gọi ngay lúc đặt hàng) — chỉ cần order_payment_schedule cục
     * bộ (đã denormalize totalAmount/paymentMode), không cần gọi order-service. */
    private String buildVnPayUrl(int orderId, String bankCode, Integer installmentNo, String ipAddress) {
        List<OrderPaymentSchedule> schedules = orderPaymentScheduleRepository
                .findByOrderIdOrderByInstallmentNoAsc(orderId);
        if (schedules.isEmpty()) {
            throw new AppException(ErrorCode.ORDER_NOT_FOUND);
        }
        OrderPaymentSchedule first = schedules.get(0);

        BigDecimal amount;
        int finalInstallmentNo = 0;
        if (first.getPaymentMode() == PaymentMode.INSTALLMENT) {
            OrderPaymentSchedule targetPayment;
            if (installmentNo != null) {
                targetPayment = schedules.stream()
                        .filter(s -> s.getInstallmentNo() == installmentNo)
                        .findFirst()
                        .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
            } else {
                targetPayment = schedules.stream()
                        .filter(schedule -> schedule.getStatus() == PaymentStatus.UNPAID)
                        .findFirst()
                        .orElseThrow(() -> new AppException(ErrorCode.ORDER_ALREADY_PAID));
            }
            amount = BigDecimal.valueOf(targetPayment.getAmount() + targetPayment.getPenaltyAmount());
            finalInstallmentNo = targetPayment.getInstallmentNo();
        } else {
            amount = BigDecimal.valueOf(first.getTotalAmount());
        }
        long finalAmount = amount
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValue();
        String vnp_TxnRef = VNPayUtil.getRandomNumber(8) + "_" + orderId + "_" + finalInstallmentNo;

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", vnPayConfig.getVnp_Version());
        vnp_Params.put("vnp_Command", vnPayConfig.getVnp_Command());
        vnp_Params.put("vnp_TmnCode", vnPayConfig.getVnp_TmnCode());
        vnp_Params.put("vnp_Amount", String.valueOf(finalAmount));
        vnp_Params.put("vnp_CurrCode", "VND");

        if (bankCode != null && !bankCode.isEmpty()) {
            vnp_Params.put("vnp_BankCode", bankCode);
        }

        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", "Thanh toan don hang: " + orderId);
        vnp_Params.put("vnp_OrderType", vnPayConfig.getOrderType());
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", vnPayConfig.getVnp_ReturnUrl());
        vnp_Params.put("vnp_IpAddr", ipAddress);

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnp_CreateDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

        cld.add(Calendar.MINUTE, 15);
        String vnp_ExpireDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = vnp_Params.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII));
                query.append('=');
                query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                if (itr.hasNext()) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }
        String queryUrl = query.toString();
        String vnp_SecureHash = VNPayConfig.hmacSHA512(vnPayConfig.getVnp_HashSecret(), hashData.toString());
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;
        return vnPayConfig.getVnp_PayUrl() + "?" + queryUrl;
    }

    @Override
    @Transactional
    public String handleVnPayCallback(HttpServletRequest request) {

        log.info("Received VNPay Callback return url hit");

        Map<String, String> fields = new HashMap<>();
        for (Enumeration<String> params = request.getParameterNames(); params.hasMoreElements();) {
            String fieldName = params.nextElement();
            String fieldValue = request.getParameter(fieldName);

            if (fieldValue != null && fieldValue.length() > 0) {
                fields.put(fieldName, fieldValue);
            }
        }

        fields.remove("vnp_SecureHashType");
        fields.remove("vnp_SecureHash");

        String txnRef = request.getParameter("vnp_TxnRef");
        String[] parts = (txnRef != null) ? txnRef.split("_") : new String[0];
        String orderIdStr = (parts.length > 1) ? parts[1] : "";
        String instNoStr = (parts.length > 2) ? parts[2] : "0";
        return "http://localhost:3000/payment-result?orderId=" + orderIdStr + "&installmentNo=" + instNoStr;
    }

    @Override
    @Transactional
    public Map<String, String> handleVnPayIpn(HttpServletRequest request) {
        log.info("Received VNPay IPN notification");

        Map<String, String> fields = new HashMap<>();
        for (Enumeration<String> params = request.getParameterNames(); params.hasMoreElements();) {
            String fieldName = params.nextElement();
            String fieldValue = request.getParameter(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                fields.put(fieldName, fieldValue);
            }
        }

        String vnp_SecureHash = request.getParameter("vnp_SecureHash");
        fields.remove("vnp_SecureHashType");
        fields.remove("vnp_SecureHash");

        String signValue = vnPayConfig.hashAllFields(fields);
        Map<String, String> response = new HashMap<>();

        if (signValue.equals(vnp_SecureHash)) {
            String txnRef = request.getParameter("vnp_TxnRef");
            String responseCode = request.getParameter("vnp_ResponseCode");
            String vnpTransactionNo = request.getParameter("vnp_TransactionNo");

            try {
                String[] parts = txnRef.split("_");
                int orderId = Integer.parseInt(parts[1]);
                int installmentNo = Integer.parseInt(parts[2]);

                List<OrderPaymentSchedule> schedules = orderPaymentScheduleRepository
                        .findByOrderIdOrderByInstallmentNoAsc(orderId);

                if (schedules.isEmpty()) {
                    response.put("RspCode", "01");
                    response.put("Message", "Order not found");
                } else if (schedules.get(0).getPaymentMode() == PaymentMode.FULL
                        && schedules.get(0).getStatus() == PaymentStatus.PAID) {
                    response.put("RspCode", "02");
                    response.put("Message", "Order already confirmed");
                } else {
                    if ("00".equals(responseCode)) {
                        OrderPaymentSchedule schedule = orderPaymentScheduleRepository
                                .findByOrderIdAndInstallmentNo(orderId, installmentNo)
                                .orElse(null);

                        if (schedule != null &&
                                (schedule.getStatus() == PaymentStatus.UNPAID ||
                                        schedule.getStatus() == PaymentStatus.OVERDUE ||
                                        schedule.getStatus() == PaymentStatus.FAILED)) {

                            schedule.setStatus(PaymentStatus.PAID);
                            schedule.setPaidDate(LocalDate.now());
                            schedule.setVnpTransactionNo(vnpTransactionNo);
                            orderPaymentScheduleRepository.saveAndFlush(schedule);
                            log.info("Schedule updated and FLUSHED to PAID for OrderId={} and InstallmentNo={}",
                                    orderId, installmentNo);
                        }

                        // Clear cart for both FULL and Down payment (installmentNo=0 or full)
                        if (installmentNo == 0 || schedules.get(0).getPaymentMode() == PaymentMode.FULL) {
                            int orderUserId = schedules.get(0).getUserId();
                            InternalOrderResponse order = orderServiceClient.getOrder(orderId);
                            InternalCartResponse cart = cartServiceClient.getCartByUser(orderUserId);
                            if (order != null && order.getItems() != null && cart != null && cart.getItems() != null) {
                                Set<Integer> orderedVariantIds = new HashSet<>();
                                for (InternalOrderResponse.Item item : order.getItems()) {
                                    orderedVariantIds.add(item.getVariantId());
                                }
                                for (InternalCartResponse.Item cartItem : cart.getItems()) {
                                    if (orderedVariantIds.contains(cartItem.getVariantId())) {
                                        cartServiceClient.deleteCartItem(cartItem.getCartItemId());
                                    }
                                }
                                log.info("Ordered items cleared from cart via IPN for user {}", orderUserId);
                            }
                        }
                    } else {
                        log.warn("Giao dịch VNPay thất bại cho Order ID: {}. Mã lỗi: {}", orderId, responseCode);
                        log.info("Order {} marked as FAILED via IPN", orderId);

                        OrderPaymentSchedule schedule = orderPaymentScheduleRepository
                                .findByOrderIdAndInstallmentNo(orderId, installmentNo)
                                .orElse(null);

                        if (schedule != null && schedule.getStatus() == PaymentStatus.UNPAID) {
                            schedule.setVnpTransactionNo(vnpTransactionNo);
                            schedule.setStatus(PaymentStatus.FAILED);
                            orderPaymentScheduleRepository.save(schedule);
                        }
                    }
                    response.put("RspCode", "00");
                    response.put("Message", "Confirm Success");
                }
            } catch (Exception e) {
                log.error("IPN Process Error: {}", e.getMessage());
                response.put("RspCode", "99");
                response.put("Message", "Unknown error");
            }
        } else {
            log.error("VNPay IPN Invalid Signature");
            response.put("RspCode", "97");
            response.put("Message", "Invalid signature");
        }
        return response;
    }

    // ==== Dùng bởi order-service qua InternalPaymentController (Giai đoạn 5) ====

    @Override
    public InstallmentPackageResponse getInstallmentPackage(int packageId) {
        InstallmentPackage pack = installmentPackageRepository.findById(packageId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        return InstallmentPackageResponse.builder()
                .packageId(pack.getPackageId())
                .name(pack.getName())
                .durationMonths(pack.getDurationMonths())
                .interestRate(pack.getInterestRate())
                .minOrderAmount(pack.getMinOrderAmount())
                .downPaymentPercentage(pack.getDownPaymentPercentage())
                .active(pack.isActive())
                .build();
    }

    @Override
    @Transactional
    public void createPaymentSchedulesForOrder(CreatePaymentSchedulesRequest request) {
        int orderId = request.getOrderId();
        int userId = request.getUserId();
        double totalAmount = request.getTotalAmount();

        if (request.getPaymentMethod() == sp26.group3.computer.sba301_computershop.enums.PaymentMethod.COD) {
            OrderPaymentSchedule schedule = OrderPaymentSchedule.builder()
                    .orderId(orderId)
                    .userId(userId)
                    .totalAmount(totalAmount)
                    .paymentMode(request.getPaymentMode())
                    .installmentNo(1)
                    .amount(totalAmount)
                    .dueDate(LocalDate.now().plusDays(7))
                    .status(PaymentStatus.UNPAID)
                    .build();
            orderPaymentScheduleRepository.save(schedule);
            log.info("Created COD payment schedule for orderId={}", orderId);
        } else if (request.getPaymentMode() == PaymentMode.FULL) {
            OrderPaymentSchedule schedule = OrderPaymentSchedule.builder()
                    .orderId(orderId)
                    .userId(userId)
                    .totalAmount(totalAmount)
                    .paymentMode(request.getPaymentMode())
                    .installmentNo(0)
                    .amount(totalAmount)
                    .dueDate(LocalDate.now().plusDays(7))
                    .status(PaymentStatus.UNPAID)
                    .build();
            orderPaymentScheduleRepository.save(schedule);
        } else {
            // Installment payments
            InstallmentPackage pack = installmentPackageRepository.findById(request.getInstallmentPackageId())
                    .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
            double downPaymentPercentage = pack.getDownPaymentPercentage();
            double downPaymentAmount = totalAmount * (downPaymentPercentage / 100.0);
            double remainingBalance = totalAmount - downPaymentAmount;

            OrderPaymentSchedule downPayment = OrderPaymentSchedule.builder()
                    .orderId(orderId)
                    .userId(userId)
                    .totalAmount(totalAmount)
                    .paymentMode(request.getPaymentMode())
                    .installmentPackage(pack)
                    .installmentNo(0) // 0 represents the down payment
                    .amount(Math.round(downPaymentAmount * 100.0) / 100.0)
                    .dueDate(LocalDate.now()) // Payable now
                    .status(PaymentStatus.UNPAID)
                    .build();
            orderPaymentScheduleRepository.save(downPayment);

            double interestRatePerMonth = (pack.getInterestRate() / 100.0) / 12.0;
            int durationMonths = pack.getDurationMonths();

            double monthlyPayment;
            if (interestRatePerMonth > 0) {
                monthlyPayment = (remainingBalance * interestRatePerMonth
                        * Math.pow(1 + interestRatePerMonth, durationMonths))
                        / (Math.pow(1 + interestRatePerMonth, durationMonths) - 1);
            } else {
                monthlyPayment = remainingBalance / durationMonths;
            }
            monthlyPayment = Math.round(monthlyPayment * 100.0) / 100.0;

            for (int i = 1; i <= durationMonths; i++) {
                OrderPaymentSchedule schedule = OrderPaymentSchedule.builder()
                        .orderId(orderId)
                        .userId(userId)
                        .totalAmount(totalAmount)
                        .paymentMode(request.getPaymentMode())
                        .installmentPackage(pack)
                        .installmentNo(i)
                        .amount(monthlyPayment)
                        .dueDate(LocalDate.now().plusMonths(i))
                        .status(PaymentStatus.UNPAID)
                        .build();
                orderPaymentScheduleRepository.save(schedule);
            }
            log.info("Created down payment + {} installment schedules for orderId={}", durationMonths, orderId);
        }
    }

    @Override
    public List<PaymentScheduleResponse> getPaymentSchedulesByOrder(int orderId) {
        return orderPaymentScheduleRepository.findByOrderIdOrderByInstallmentNoAsc(orderId)
                .stream()
                .map(schedule -> PaymentScheduleResponse.builder()
                        .paymentScheduleId(schedule.getPaymentScheduleId())
                        .installmentNo(schedule.getInstallmentNo())
                        .amount(schedule.getAmount())
                        .penaltyAmount(schedule.getPenaltyAmount())
                        .dueDate(schedule.getDueDate())
                        .paidDate(schedule.getPaidDate())
                        .vnpTransactionNo(schedule.getVnpTransactionNo())
                        .status(schedule.getStatus())
                        .build())
                .toList();
    }
}
