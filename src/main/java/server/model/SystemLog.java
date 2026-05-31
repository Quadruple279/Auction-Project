package server.model;

import java.time.LocalDateTime;

public class SystemLog {
    private final String adminName;
    private final String action;
    private final String detail;
    private final LocalDateTime createdAt;

    public SystemLog(String adminName, String action, String detail) {
        this.adminName = adminName;
        this.action    = action;
        this.detail    = detail;
        this.createdAt = LocalDateTime.now();
    }

    public String getAdminName() { return adminName; }
    public String getAction()    { return action; }
    public String getDetail()    { return detail; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}

