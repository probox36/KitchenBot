package KitchenBot;

public class Event implements DutyDay {
    @Override
    public boolean isRegular() {
        return false;
    }

    private String reason;
    private boolean notificationNeeded;
    private String message;

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public boolean isNotificationNeeded() { return notificationNeeded; }
    public void setNotificationNeeded(boolean notificationNeeded) { this.notificationNeeded = notificationNeeded; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Event(String reason) {
        this.reason = reason;
    }

    @Override
    public String toString() {
        return "Событие: " + reason;
    }
}
