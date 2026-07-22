package com.exam_system.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.bootstrap")
public class BootstrapProperties {

    private boolean seedAdmin;
    private boolean seedLocalTestUsers;
    private String adminUsername = "admin";
    private String adminPassword = "admin123";
    private String adminName = "System Admin";
    private String professorUsername = "professor";
    private String professorPassword = "professor123";
    private String professorName = "Sample Professor";
    private String studentUsername = "student";
    private String studentPassword = "student123";
    private String studentName = "Sample Student";

    public boolean isSeedAdmin() {
        return seedAdmin;
    }

    public void setSeedAdmin(boolean seedAdmin) {
        this.seedAdmin = seedAdmin;
    }

    public boolean isSeedLocalTestUsers() {
        return seedLocalTestUsers;
    }

    public void setSeedLocalTestUsers(boolean seedLocalTestUsers) {
        this.seedLocalTestUsers = seedLocalTestUsers;
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

    public String getProfessorUsername() {
        return professorUsername;
    }

    public void setProfessorUsername(String professorUsername) {
        this.professorUsername = professorUsername;
    }

    public String getProfessorPassword() {
        return professorPassword;
    }

    public void setProfessorPassword(String professorPassword) {
        this.professorPassword = professorPassword;
    }

    public String getProfessorName() {
        return professorName;
    }

    public void setProfessorName(String professorName) {
        this.professorName = professorName;
    }

    public String getStudentUsername() {
        return studentUsername;
    }

    public void setStudentUsername(String studentUsername) {
        this.studentUsername = studentUsername;
    }

    public String getStudentPassword() {
        return studentPassword;
    }

    public void setStudentPassword(String studentPassword) {
        this.studentPassword = studentPassword;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }
}
