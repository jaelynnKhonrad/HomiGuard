package edu.uph.m23si1.homiguard.model;

public class HistoryModel {
    private String status;
    private String unlock;
    private String lock;

    public HistoryModel() {} // Firebase butuh konstruktor kosong

    public HistoryModel(String status, String unlock, String lock) {
        this.status = status;
        this.unlock = unlock;
        this.lock = lock;
    }

    public String getStatus() { return status; }
    public String getUnlock() { return unlock; }
    public String getLock() { return lock; }
}
