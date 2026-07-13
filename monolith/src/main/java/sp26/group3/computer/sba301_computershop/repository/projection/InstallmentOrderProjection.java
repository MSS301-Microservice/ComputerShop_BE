package sp26.group3.computer.sba301_computershop.repository.projection;

public interface InstallmentOrderProjection {
    Integer getOrderId();
    Integer getUserId();
    Double getOrderTotal();
    Long getTotalInstallments();
    Long getPaidInstallments();
    String getNextDueDate();
}
