package shared.dto;

public class SystemLogDTO {
    private String adminName;
    private String action;
    private String detail;
    private String createdAt;

    public SystemLogDTO() {}

    public SystemLogDTO(String adminName, String action, String detail, String createdAt) {
        this.adminName = adminName;
        this.action    = action;
        this.detail    = detail;
        this.createdAt = createdAt;
    }

    public String getAdminName() { return adminName; }
    public String getAction()    { return action; }
    public String getDetail()    { return detail; }
    public String getCreatedAt() { return createdAt; }

    public void setAdminName(String adminName) { this.adminName = adminName; }
    public void setAction(String action)       { this.action = action; }
    public void setDetail(String detail)       { this.detail = detail; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}

