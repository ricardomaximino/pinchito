package es.brasatech.pinchito.model;

public class PaymentRequest {
    private String id;
    private String whyPaid;
    private String status; // PENDING, CONFIRMED, REJECTED
    private String rejectionReason;
    private String requestDate;

    public PaymentRequest() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getWhyPaid() {
        return whyPaid;
    }

    public void setWhyPaid(String whyPaid) {
        this.whyPaid = whyPaid;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public String getRequestDate() {
        return requestDate;
    }

    public void setRequestDate(String requestDate) {
        this.requestDate = requestDate;
    }
}
