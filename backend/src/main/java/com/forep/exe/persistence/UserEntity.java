package com.forep.exe.persistence;

import com.forep.exe.domain.Enums.EmployeeLevel;
import com.forep.exe.domain.Enums.EmploymentType;
import com.forep.exe.domain.Enums.Role;
import com.forep.exe.domain.Enums.SeniorityLevel;
import com.forep.exe.domain.Enums.UserStatus;
import com.forep.exe.domain.Enums.WorkingStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "users")
public class UserEntity extends BaseEntity {
    @Column(nullable = false)
    private UUID workspaceId;
    @Column(nullable = false)
    private String fullName;
    @Column(nullable = false)
    private String email;
    private String phone;
    @Column(unique = true)
    private String username;
    @Column(unique = true)
    private String employeeCode;
    private String initialPassword;
    @Column(nullable = false)
    private String passwordHash;
    @Column(nullable = false)
    private boolean mustChangePassword;
    @Column(nullable = false)
    private boolean initialAccountGenerated;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;
    private String avatar;
    private String avatarFileId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;
    private String jobTitle;
    @Enumerated(EnumType.STRING)
    private SeniorityLevel seniorityLevel;
    private Integer skillRating;
    private Integer yearsOfExperience;
    @Column(columnDefinition = "text")
    private String skills;
    private UUID departmentId;
    private UUID jobPositionId;
    private LocalDate dateOfBirth;
    private String gender;
    @Column(columnDefinition = "text")
    private String address;
    @Column(columnDefinition = "text")
    private String personalSummary;
    @Enumerated(EnumType.STRING)
    private EmploymentType employmentType;
    @Enumerated(EnumType.STRING)
    private WorkingStatus workingStatus;
    @Enumerated(EnumType.STRING)
    private EmployeeLevel employeeLevel;
    private Integer monthlyWorkingCapacityHours;
    @Column(columnDefinition = "text")
    private String mainExpertise;
    @Column(columnDefinition = "text")
    private String secondaryExpertise;
    @Column(nullable = false)
    private OffsetDateTime createdAt;
    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    public UUID getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(UUID workspaceId) {
        this.workspaceId = workspaceId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmployeeCode() {
        return employeeCode;
    }

    public void setEmployeeCode(String employeeCode) {
        this.employeeCode = employeeCode;
    }

    public String getInitialPassword() {
        return initialPassword;
    }

    public void setInitialPassword(String initialPassword) {
        this.initialPassword = initialPassword;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public boolean isMustChangePassword() { return mustChangePassword; }

    public void setMustChangePassword(boolean mustChangePassword) { this.mustChangePassword = mustChangePassword; }

    public boolean isInitialAccountGenerated() { return initialAccountGenerated; }

    public void setInitialAccountGenerated(boolean initialAccountGenerated) { this.initialAccountGenerated = initialAccountGenerated; }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getAvatarFileId() {
        return avatarFileId;
    }

    public void setAvatarFileId(String avatarFileId) {
        this.avatarFileId = avatarFileId;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public SeniorityLevel getSeniorityLevel() {
        return seniorityLevel;
    }

    public void setSeniorityLevel(SeniorityLevel seniorityLevel) {
        this.seniorityLevel = seniorityLevel;
    }

    public Integer getSkillRating() {
        return skillRating;
    }

    public void setSkillRating(Integer skillRating) {
        this.skillRating = skillRating;
    }

    public Integer getYearsOfExperience() {
        return yearsOfExperience;
    }

    public void setYearsOfExperience(Integer yearsOfExperience) {
        this.yearsOfExperience = yearsOfExperience;
    }

    public String getSkills() {
        return skills;
    }

    public void setSkills(String skills) {
        this.skills = skills;
    }

    public UUID getDepartmentId() { return departmentId; }
    public void setDepartmentId(UUID departmentId) { this.departmentId = departmentId; }
    public UUID getJobPositionId() { return jobPositionId; }
    public void setJobPositionId(UUID jobPositionId) { this.jobPositionId = jobPositionId; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getPersonalSummary() { return personalSummary; }
    public void setPersonalSummary(String personalSummary) { this.personalSummary = personalSummary; }
    public EmploymentType getEmploymentType() { return employmentType; }
    public void setEmploymentType(EmploymentType employmentType) { this.employmentType = employmentType; }
    public WorkingStatus getWorkingStatus() { return workingStatus; }
    public void setWorkingStatus(WorkingStatus workingStatus) { this.workingStatus = workingStatus; }
    public EmployeeLevel getEmployeeLevel() { return employeeLevel; }
    public void setEmployeeLevel(EmployeeLevel employeeLevel) { this.employeeLevel = employeeLevel; }
    public Integer getMonthlyWorkingCapacityHours() { return monthlyWorkingCapacityHours; }
    public void setMonthlyWorkingCapacityHours(Integer monthlyWorkingCapacityHours) { this.monthlyWorkingCapacityHours = monthlyWorkingCapacityHours; }
    public String getMainExpertise() { return mainExpertise; }
    public void setMainExpertise(String mainExpertise) { this.mainExpertise = mainExpertise; }
    public String getSecondaryExpertise() { return secondaryExpertise; }
    public void setSecondaryExpertise(String secondaryExpertise) { this.secondaryExpertise = secondaryExpertise; }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
