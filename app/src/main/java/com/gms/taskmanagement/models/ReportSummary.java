package com.gms.taskmanagement.models;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class ReportSummary implements Serializable {
    private int totalTasks;
    private Map<String, Integer> byStatus = new HashMap<>();
    private Map<String, Integer> byEmployee = new HashMap<>();
    private Map<String, Integer> byDepartment = new HashMap<>();
    private Map<String, Integer> byJobType = new HashMap<>();

    public ReportSummary() {}

    public int getTotalTasks() { return totalTasks; }
    public void setTotalTasks(int totalTasks) { this.totalTasks = totalTasks; }

    public Map<String, Integer> getByStatus() { return byStatus; }
    public void setByStatus(Map<String, Integer> byStatus) { this.byStatus = byStatus; }

    public Map<String, Integer> getByEmployee() { return byEmployee; }
    public void setByEmployee(Map<String, Integer> byEmployee) { this.byEmployee = byEmployee; }

    public Map<String, Integer> getByDepartment() { return byDepartment; }
    public void setByDepartment(Map<String, Integer> byDepartment) { this.byDepartment = byDepartment; }

    public Map<String, Integer> getByJobType() { return byJobType; }
    public void setByJobType(Map<String, Integer> byJobType) { this.byJobType = byJobType; }
}
