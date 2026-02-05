package de.kaiserdragon.callforwardingstatus;

public class FirebaseForwardingResponse {

    public String status;
    public String message;
    public long timestamp;

    public FirebaseForwardingResponse() {}

    public FirebaseForwardingResponse(String status, String message, long timestamp) {
        this.status = status;
        this.message = message;
        this.timestamp = timestamp;
    }
}
