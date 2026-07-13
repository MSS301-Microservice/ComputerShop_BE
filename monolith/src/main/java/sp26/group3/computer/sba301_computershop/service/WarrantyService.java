package sp26.group3.computer.sba301_computershop.service;

import sp26.group3.computer.sba301_computershop.dto.request.UpdateWarrantyStatusRequest;
import sp26.group3.computer.sba301_computershop.dto.request.warranty.CreateWarrantiesRequest;
import sp26.group3.computer.sba301_computershop.dto.response.WarrantyResponse;

import java.util.List;

public interface WarrantyService {
    void createWarrantiesForOrder(CreateWarrantiesRequest request);

    WarrantyResponse getWarrantyById(int id);

    List<WarrantyResponse> getWarrantiesByOrderId(int orderId);

    List<WarrantyResponse> getWarrantiesByPhoneNumber(String phoneNumber);

    WarrantyResponse updateWarrantyStatus(int id, UpdateWarrantyStatusRequest request);
}
