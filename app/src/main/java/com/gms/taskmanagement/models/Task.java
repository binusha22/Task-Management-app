package com.gms.taskmanagement.models;

import java.io.Serializable;

public class Task implements Serializable {
    private int rowIndex;
    private String timestamp;
    private String ticketNumber;
    private String selectEmployee;
    private String jobType;
    private String dateOfPerform;
    private String employeeName;
    private String description;
    private String department;
    private String status;

    public Task() {
        this.status = "Pending";
    }

    public int getRowIndex() { return rowIndex; }
    public void setRowIndex(int rowIndex) { this.rowIndex = rowIndex; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public String getTicketNumber() { return ticketNumber; }
    public void setTicketNumber(String ticketNumber) { this.ticketNumber = ticketNumber; }

    public String getSelectEmployee() { return selectEmployee; }
    public void setSelectEmployee(String selectEmployee) { this.selectEmployee = selectEmployee; }

    public String getJobType() { return jobType; }
    public void setJobType(String jobType) { this.jobType = jobType; }

    public String getDateOfPerform() { return dateOfPerform; }
    public void setDateOfPerform(String dateOfPerform) { this.dateOfPerform = dateOfPerform; }

    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isCompleted() {
        return "Completed".equalsIgnoreCase(status);
    }
}
