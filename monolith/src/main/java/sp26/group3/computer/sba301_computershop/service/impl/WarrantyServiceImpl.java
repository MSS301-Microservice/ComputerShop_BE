package sp26.group3.computer.sba301_computershop.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sp26.group3.computer.sba301_computershop.dto.request.UpdateWarrantyStatusRequest;
import sp26.group3.computer.sba301_computershop.dto.request.warranty.CreateWarrantiesRequest;
import sp26.group3.computer.sba301_computershop.dto.response.ClaimResponse;
import sp26.group3.computer.sba301_computershop.dto.response.WarrantyResponse;
import sp26.group3.computer.sba301_computershop.entity.Warranty;
import sp26.group3.computer.sba301_computershop.enums.WarrantyStatus;
import sp26.group3.computer.sba301_computershop.enums.WarrantyType;
import sp26.group3.computer.sba301_computershop.repository.WarrantyRepository;
import sp26.group3.computer.sba301_computershop.service.WarrantyService;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class WarrantyServiceImpl implements WarrantyService {

    WarrantyRepository warrantyRepository;

    @Override
    @Transactional
    public void createWarrantiesForOrder(CreateWarrantiesRequest request) {
        log.info("Creating warranties for orderId={}", request.getOrderId());

        for (CreateWarrantiesRequest.Item item : request.getItems()) {
            int warrantyMonths = item.getWarrantyMonths();

            if (warrantyMonths > 0) {
                // Check if warranty already exists for this order item
                if (warrantyRepository.findByOrderItemId(item.getOrderItemId()).isEmpty()) {
                    LocalDate startDate = LocalDate.now();
                    LocalDate endDate = startDate.plusMonths(warrantyMonths);

                    Warranty warranty = Warranty.builder()
                            .orderItemId(item.getOrderItemId())
                            .orderId(request.getOrderId())
                            .productId(item.getProductId())
                            .productName(item.getProductName())
                            .recipientPhone(item.getRecipientPhone())
                            .serialNumber(item.getSerialNumber())
                            .startDate(startDate)
                            .endDate(endDate)
                            .description("Warranty for " + item.getProductName())
                            .status(WarrantyStatus.ACTIVE)
                            .type(WarrantyType.MANUFACTURER) // Default type
                            .build();

                    warrantyRepository.save(warranty);
                    log.info("Created warranty for orderItemId={} with endDate={}", item.getOrderItemId(), endDate);
                }
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public WarrantyResponse getWarrantyById(int id) {
        Warranty warranty = warrantyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Warranty not found with id: " + id));
        return toWarrantyResponse(warranty);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WarrantyResponse> getWarrantiesByOrderId(int orderId) {
        return warrantyRepository.findByOrderId(orderId)
                .stream()
                .map(this::toWarrantyResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<WarrantyResponse> getWarrantiesByPhoneNumber(String phoneNumber) {
        log.info("Getting warranties for phoneNumber={}", phoneNumber);
        return warrantyRepository.findByRecipientPhone(phoneNumber)
                .stream()
                .map(this::toWarrantyResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public WarrantyResponse updateWarrantyStatus(int id, UpdateWarrantyStatusRequest request) {
        Warranty warranty = warrantyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Warranty not found with id: " + id));

        warranty.setStatus(request.getStatus());
        Warranty updated = warrantyRepository.save(warranty);

        log.info("Updated warranty id={} to status={}", id, request.getStatus());
        return toWarrantyResponse(updated);
    }

    private WarrantyResponse toWarrantyResponse(Warranty warranty) {
        List<ClaimResponse> claimResponses = null;
        if (warranty.getClaims() != null) {
            claimResponses = warranty.getClaims().stream()
                    .map(claim -> ClaimResponse.builder()
                            .claimId(claim.getClaimId())
                            .warrantyId(claim.getWarranty().getId())
                            .claimDate(claim.getClaimDate())
                            .customerNote(claim.getCustomerNote())
                            .technicianNote(claim.getTechnicianNote())
                            .status(claim.getStatus())
                            .solutionType(claim.getSolutionType())
                            .returnDate(claim.getReturnDate())
                            .build())
                    .collect(Collectors.toList());
        }

        return WarrantyResponse.builder()
                .id(warranty.getId())
                .orderItemId(warranty.getOrderItemId())
                .productId(warranty.getProductId())
                .productName(warranty.getProductName())
                .serialNumber(warranty.getSerialNumber())
                .startDate(warranty.getStartDate())
                .endDate(warranty.getEndDate())
                .description(warranty.getDescription())
                .status(warranty.getStatus())
                .type(warranty.getType())
                .claims(claimResponses)
                .build();
    }
}
