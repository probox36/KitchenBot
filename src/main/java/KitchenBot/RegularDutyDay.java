package KitchenBot;

public class RegularDutyDay implements DutyDay {
    @Override
    public boolean isRegular() {
        return true;
    }
    private long userOnDuty;

    public long getUserOnDuty() { return userOnDuty; }
    public void setUserOnDuty(long userOnDuty) { this.userOnDuty = userOnDuty; }

    public RegularDutyDay(long userOnDuty) {
        this.userOnDuty = userOnDuty;
    }

    @Override
    public String toString() {
        return "Дежурство: " + userOnDuty;
    }
}
