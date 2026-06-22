package es.brasatech.pinchito.model;

public class WebSocketEvent {
    private String type;      // DEBIT_CREATED, PAYMENT_REQUESTED, PAYMENT_CONFIRMED, PAYMENT_REJECTED
    private String message;   // Friendly message in Spanish
    private String sender;    // Username who triggered the event

    public WebSocketEvent() {}

    public WebSocketEvent(String type, String message, String sender) {
        this.type = type;
        this.message = message;
        this.sender = sender;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }
}
