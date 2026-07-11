package com.exam_system.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.bootstrap")
public class BootstrapProperties {

    private boolean seedAdmin;
    private String adminUsername = "admin";
    private String adminPassword = "admin123";
    private String adminName = "System Admin";

    public boolean isSeedAdmin() {
        return seedAdmin;
    }

    public void setSeedAdmin(boolean seedAdmin) {
        this.seedAdmin = seedAdmin;
    }

    public String getAdminUsername() {
        return adminUsername;
    }

    public void setAdminUsername(String adminUsername) {
        this.adminUsername = adminUsername;
    }

    public String getAdminPassword() {
        return adminPassword;
    }

    public void setAdminPassword(String adminPassword) {
        this.adminPassword = adminPassword;
    }

    public String getAdminName() {
        return adminName;
    }

    public void setAdminName(String adminName) {
        this.adminName = adminName;
    }
}
