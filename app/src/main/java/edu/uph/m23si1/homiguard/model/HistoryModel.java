package edu.uph.m23si1.homiguard.model;

public class HistoryModel {

    private boolean isHeader;
    private String headerTitle;

    private String device;
    private String value;
    private int percent;
    private float levelCm;
    private long timestamp;

    public HistoryModel() {}

    // Constructor Header
    public HistoryModel(String headerTitle) {
        this.isHeader = true;
        this.headerTitle = headerTitle;
    }

    // Constructor Item
    public HistoryModel(String device, String value, int percent, float levelCm, long timestamp) {
        this.isHeader = false;
        this.device = device;
        this.value = value;
        this.percent = percent;
        this.levelCm = levelCm;
        this.timestamp = timestamp;
    }

    public boolean isHeader() {
        return isHeader;
    }

    public String getHeaderTitle() {
        return headerTitle;
    }

    public String getDevice() {
        return device;
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
}
