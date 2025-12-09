package edu.uph.m23si1.homiguard.model;

public class HistoryModel {

    private boolean isHeader;
    private String headerTitle;
    private String device;
    private String deviceName;
    private String value;
    private int percent;        // AMAN: default 0
    private float levelCm;      // AMAN: default 0.0f
    private long timestamp;

    // ✅ WAJIB untuk Firebase
    public HistoryModel() {}

    // ✅ Constructor Header
    public HistoryModel(String headerTitle) {
        this.isHeader = true;
        this.headerTitle = headerTitle;
        this.timestamp = System.currentTimeMillis();
    }

    // ✅ Constructor Item
    public HistoryModel(String device, String value, int percent, float levelCm, long timestamp) {
        this.isHeader = false;
        this.device = device;
        this.value = value;
        this.percent = percent;
        this.levelCm = levelCm;
        this.timestamp = timestamp;
    }

    // ===== GETTER =====

    public boolean isHeader() {
        return isHeader;
    }

    public String getHeaderTitle() {
        return headerTitle;
    }

    public String getDevice() {
        return device;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public String getValue() {
        return value;
    }

    public int getPercent() {
        return percent;
    }

    public float getLevelCm() {
        return levelCm;
    }

    public long getTimestamp() {
        return timestamp;
    }

    // ===== SETTER (BIAR FIREBASE AMAN) =====

    public void setHeader(boolean header) {
        isHeader = header;
    }

    public void setHeaderTitle(String headerTitle) {
        this.headerTitle = headerTitle;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setPercent(int percent) {
        this.percent = percent;
    }

    public void setLevelCm(float levelCm) {
        this.levelCm = levelCm;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}