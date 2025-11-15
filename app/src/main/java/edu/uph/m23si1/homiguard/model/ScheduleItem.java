package edu.uph.m23si1.homiguard.model;

public class ScheduleItem {
    private String id;
    private String pageType;
    private String date;
    private String onTime;
    private String offTime;
    private boolean isActive;

    // Diperlukan oleh Firestore
    public ScheduleItem() {}

    public ScheduleItem(String id, String pageType, String date, String onTime, String offTime, boolean isActive) {
        this.id = id;
        this.pageType = pageType;
        this.date = date;
        this.onTime = onTime;
        this.offTime = offTime;
        this.isActive = isActive;
    }

    // 🔹 Getter dan Setter
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPageType() { return pageType; }
    public void setPageType(String pageType) { this.pageType = pageType; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getOnTime() { return onTime; }
    public void setOnTime(String onTime) { this.onTime = onTime; }

    public String getOffTime() { return offTime; }
    public void setOffTime(String offTime) { this.offTime = offTime; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
}
