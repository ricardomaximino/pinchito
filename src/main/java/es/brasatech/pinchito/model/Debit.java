package es.brasatech.pinchito.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.List;

public class Debit {
    private String id;
    private String fromUser;
    private String toUser;
    private int quantity;
    private DebitKind kind;
    private String why;
    private String status; // PENDING, PAID
    private String createdDate;
    private List<PaymentRequest> paymentRequests = new ArrayList<>();

    public Debit() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFromUser() {
        return fromUser;
    }

    public void setFromUser(String fromUser) {
        this.fromUser = fromUser;
    }

    public String getToUser() {
        return toUser;
    }

    public void setToUser(String toUser) {
        this.toUser = toUser;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public DebitKind getKind() {
        return kind;
    }

    public void setKind(DebitKind kind) {
        this.kind = kind;
    }

    public String getWhy() {
        return why;
    }

    public void setWhy(String why) {
        this.why = why;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(String createdDate) {
        this.createdDate = createdDate;
    }

    public List<PaymentRequest> getPaymentRequests() {
        return paymentRequests;
    }

    public void setPaymentRequests(List<PaymentRequest> paymentRequests) {
        this.paymentRequests = paymentRequests;
    }

    @JsonIgnore
    public boolean hasPendingPaymentRequest() {
        return paymentRequests != null && paymentRequests.stream().anyMatch(r -> "PENDING".equalsIgnoreCase(r.getStatus()));
    }

    @JsonIgnore
    public PaymentRequest getPendingPaymentRequest() {
        if (paymentRequests == null) return null;
        return paymentRequests.stream().filter(r -> "PENDING".equalsIgnoreCase(r.getStatus())).findFirst().orElse(null);
    }

    @JsonIgnore
    public List<PaymentRequest> getRejectedPaymentRequests() {
        if (paymentRequests == null) return new java.util.ArrayList<>();
        return paymentRequests.stream().filter(r -> "REJECTED".equalsIgnoreCase(r.getStatus())).collect(java.util.stream.Collectors.toList());
    }
}
