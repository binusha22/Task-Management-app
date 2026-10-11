

const CONFIG = {
  USERS_SHEET: "Users",
  TASKS_SHEET: "Form Responses1",
  EDIT_REQUESTS_SHEET: "Edit_Requests",
  NOTIFICATIONS_SHEET: "Notifications",
  AUDIT_LOGS_SHEET: "Audit_Logs",
  CONFIG_LISTS_SHEET: "Config_Lists",
  RECOVERY_ADMIN_EMAIL: "admin@gmail.com",
  RECOVERY_ADMIN_DEFAULT_PASS: "admin123"
};

function doGet(e) {
  return handleRequest(e ? e.parameter : {});
}

function doPost(e) {
  var params = {};
  if (e && e.postData && e.postData.contents) {
    try {
      params = JSON.parse(e.postData.contents);
    } catch (err) {
      params = e.parameter || {};
    }
  } else if (e && e.parameter) {
    params = e.parameter;
  }
  return handleRequest(params);
}

function handleRequest(params) {
  var action = params.action;
  var response = { success: false, message: "Invalid action or parameters", data: null };

  try {
    var ss = SpreadsheetApp.getActiveSpreadsheet();
    ensureSystemSheetsExist(ss);

    switch (action) {
      case "test_connection":
        response = testConnection(ss);
        break;

      case "provision_recovery_admin":
        response = provisionRecoveryAdmin(ss);
        break;

      case "login":
        response = handleLogin(ss, params.email, params.password);
        break;

      case "get_tasks":
        response = getTasks(ss, params.email, params.role);
        break;

      case "get_next_ticket":
        response = getNextTicketNumber(ss);
        break;

      case "create_task":
        response = createTask(ss, params.email, params.role, params.task);
        break;

      case "update_task":
        response = updateTask(ss, params.email, params.role, params.task);
        break;

      case "delete_task":
        response = deleteTask(ss, params.email, params.role, params.ticketNumber);
        break;

      case "submit_edit_request":
        response = submitEditRequest(ss, params.email, params.name, params.ticketNumber, params.reason);
        break;

      case "get_edit_requests":
        response = getEditRequests(ss, params.email, params.role);
        break;

      case "process_edit_request":
        response = processEditRequest(ss, params.email, params.role, params.requestId, params.decision, params.remarks);
        break;

      case "get_config_lists":
        response = getConfigLists(ss);
        break;

      case "manage_config_list":
        response = manageConfigList(ss, params.email, params.role, params.category, params.value, params.operation);
        break;

      case "get_users":
        response = getUsers(ss, params.email, params.role);
        break;

      case "create_user":
        response = createUser(ss, params.email, params.role, params.newUser);
        break;

      case "update_user":
        response = updateUser(ss, params.email, params.role, params.targetUser);
        break;

      case "change_password":
        response = changePassword(ss, params.email, params.currentPassword, params.newPassword);
        break;

      case "get_reports":
        response = getReports(ss, params.email, params.role, params.startDate, params.endDate);
        break;

      case "get_notifications":
        response = getNotifications(ss, params.email);
        break;

      case "mark_notification_read":
        response = markNotificationRead(ss, params.email, params.notificationId);
        break;

      default:
        response = { success: false, message: "Unknown action: " + action };
        break;
    }
  } catch (err) {
    response = { success: false, message: "Server error: " + err.toString() };
  }

  return ContentService.createTextOutput(JSON.stringify(response))
    .setMimeType(ContentService.MimeType.JSON);
}

function ensureSystemSheetsExist(ss) {
  var usersSheet = ss.getSheetByName(CONFIG.USERS_SHEET);
  if (!usersSheet) {
    usersSheet = ss.insertSheet(CONFIG.USERS_SHEET);
    usersSheet.appendRow(["f_name", "l_name", "role", "email", "password"]);
  }

  var tasksSheet = ss.getSheetByName(CONFIG.TASKS_SHEET);
  if (!tasksSheet) {
    tasksSheet = ss.insertSheet(CONFIG.TASKS_SHEET);
    tasksSheet.appendRow(["Timestamp", "Ticket Number", "Select Employee", "Job Type", "Date of perform", "Employee name", "Description of the task", "Department List", "status"]);
  }

  var editSheet = ss.getSheetByName(CONFIG.EDIT_REQUESTS_SHEET);
  if (!editSheet) {
    editSheet = ss.insertSheet(CONFIG.EDIT_REQUESTS_SHEET);
    editSheet.appendRow(["RequestId", "TicketNumber", "RequesterEmail", "RequesterName", "Reason", "RequestedAt", "Status", "AdminRemarks", "ProcessedAt"]);
  }

  var notifSheet = ss.getSheetByName(CONFIG.NOTIFICATIONS_SHEET);
  if (!notifSheet) {
    notifSheet = ss.insertSheet(CONFIG.NOTIFICATIONS_SHEET);
    notifSheet.appendRow(["NotificationId", "RecipientEmail", "Title", "Message", "Timestamp", "ReadStatus", "TicketNumber"]);
  }

  var auditSheet = ss.getSheetByName(CONFIG.AUDIT_LOGS_SHEET);
  if (!auditSheet) {
    auditSheet = ss.insertSheet(CONFIG.AUDIT_LOGS_SHEET);
    auditSheet.appendRow(["Timestamp", "UserEmail", "Action", "Details"]);
  }

  var configSheet = ss.getSheetByName(CONFIG.CONFIG_LISTS_SHEET);
  if (!configSheet) {
    configSheet = ss.insertSheet(CONFIG.CONFIG_LISTS_SHEET);
    configSheet.appendRow(["Category", "Value", "IsActive"]);
    var defaultJobs = [
      "Network Cabling / Route", "Software Installation", "Hardware Maintenance",
      "Printer Troubleshooting", "Database Backup", "System Access Request"
    ];
    var defaultDepts = [
      "Accounts / Finance", "Information Technology", "Human Resources",
      "Production / Operations", "Procurement / Logistics", "Management"
    ];
    defaultJobs.forEach(function(j) { configSheet.appendRow(["JobType", j, "TRUE"]); });
    defaultDepts.forEach(function(d) { configSheet.appendRow(["Department", d, "TRUE"]); });
  }
}

function testConnection(ss) {
  var usersCount = 0;
  var tasksCount = 0;
  var uSheet = ss.getSheetByName(CONFIG.USERS_SHEET);
  if (uSheet) usersCount = Math.max(0, uSheet.getLastRow() - 1);
  var tSheet = ss.getSheetByName(CONFIG.TASKS_SHEET);
  if (tSheet) tasksCount = Math.max(0, tSheet.getLastRow() - 1);

  return {
    success: true,
    message: "Google Sheets connection verified successfully",
    data: {
      spreadsheetName: ss.getName(),
      spreadsheetId: ss.getId(),
      usersCount: usersCount,
      tasksCount: tasksCount,
      serverTime: new Date().toISOString()
    }
  };
}

function handleLogin(ss, email, password) {
  if (!email || !password) {
    return { success: false, message: "Email and password are required" };
  }
  var cleanEmail = email.trim().toLowerCase();
  var sheet = ss.getSheetByName(CONFIG.USERS_SHEET);
  var data = sheet.getDataRange().getValues();

  var headers = data.length ? data[0].map(function(h) { return String(h).trim(); }) : [];
  var fNameIdx = headers.indexOf("f_name");
  var lNameIdx = headers.indexOf("l_name");
  var roleIdx = headers.indexOf("role");
  var emailIdx = headers.indexOf("email");
  var passIdx = headers.indexOf("password");

  for (var i = 1; i < data.length; i++) {
    var row = data[i];
    var rowEmail = String(row[emailIdx] || "").trim().toLowerCase();
    var rowPass = String(row[passIdx] || "").trim();

    if (rowEmail === cleanEmail) {
      if (rowPass === String(password).trim()) {
        var role = String(row[roleIdx] || "User").trim();
        var fName = String(row[fNameIdx] || "").trim();
        var lName = String(row[lNameIdx] || "").trim();

        return createLoginResponse(ss, cleanEmail, fName, lName, role);
      } else {
        logAudit(ss, cleanEmail, "LOGIN_FAILED", "Invalid password attempt");
        return { success: false, message: "Invalid email or password" };
      }
    }
  }

  if (cleanEmail === CONFIG.RECOVERY_ADMIN_EMAIL.toLowerCase() &&
      String(password).trim() === CONFIG.RECOVERY_ADMIN_DEFAULT_PASS) {
    var provisionResult = provisionRecoveryAdmin(ss);
    if (!provisionResult.success) return provisionResult;

    var refreshedData = sheet.getDataRange().getValues();
    var refreshedHeaders = refreshedData.length
      ? refreshedData[0].map(function(h) { return String(h).trim(); })
      : [];
    var refreshedEmailIdx = refreshedHeaders.indexOf("email");
    var refreshedPasswordIdx = refreshedHeaders.indexOf("password");
    var refreshedFirstNameIdx = refreshedHeaders.indexOf("f_name");
    var refreshedLastNameIdx = refreshedHeaders.indexOf("l_name");
    var refreshedRoleIdx = refreshedHeaders.indexOf("role");

    for (var j = 1; j < refreshedData.length; j++) {
      var refreshedRow = refreshedData[j];
      if (String(refreshedRow[refreshedEmailIdx] || "").trim().toLowerCase() === cleanEmail) {
        if (String(refreshedRow[refreshedPasswordIdx] || "").trim() !== String(password).trim()) {
          return { success: false, message: "Invalid email or password" };
        }
        return createLoginResponse(
          ss,
          cleanEmail,
          String(refreshedRow[refreshedFirstNameIdx] || "").trim(),
          String(refreshedRow[refreshedLastNameIdx] || "").trim(),
          String(refreshedRow[refreshedRoleIdx] || "User").trim()
        );
      }
    }

    return { success: false, message: "Recovery administrator account could not be provisioned" };
  }

  if (data.length <= 1) {
    return { success: false, message: "No users configured in spreadsheet" };
  }

  return { success: false, message: "Account not authorized. Contact ushan." };
}

function createLoginResponse(ss, email, firstName, lastName, role) {
  logAudit(ss, email, "LOGIN_SUCCESS", "User logged in with role: " + role);
  return {
    success: true,
    message: "Authentication successful",
    data: {
      email: email,
      firstName: firstName,
      lastName: lastName,
      fullName: (firstName + " " + lastName).trim(),
      role: role,
      token: Utilities.base64Encode(email + ":" + role + ":" + new Date().getTime())
    }
  };
}

function getTasks(ss, requesterEmail, requesterRole) {
  var sheet = ss.getSheetByName(CONFIG.TASKS_SHEET);
  var data = sheet.getDataRange().getValues();
  if (data.length <= 1) {
    return { success: true, message: "No tasks found", data: [] };
  }

  var headers = data[0].map(function(h) { return String(h).trim(); });
  var colMap = {};
  headers.forEach(function(h, idx) { colMap[h] = idx; });

  var isAdmin = String(requesterRole).toLowerCase() === "admin";
  var requesterClean = String(requesterEmail || "").trim().toLowerCase();

  var userFullName = "";
  if (!isAdmin && requesterClean) {
    var uSheet = ss.getSheetByName(CONFIG.USERS_SHEET);
    var uData = uSheet.getDataRange().getValues();
    for (var u = 1; u < uData.length; u++) {
      if (String(uData[u][3] || "").trim().toLowerCase() === requesterClean) {
        userFullName = (String(uData[u][0] || "") + " " + String(uData[u][1] || "")).trim().toLowerCase();
        break;
      }
    }
  }

  var tasks = [];
  for (var i = 1; i < data.length; i++) {
    var row = data[i];
    var ticketNum = String(row[colMap["Ticket Number"]] || "").trim();
    if (!ticketNum) continue;

    var selectEmp = String(row[colMap["Select Employee"]] || "").trim();
    var empName = String(row[colMap["Employee name"]] || "").trim();

    if (!isAdmin) {
      var matchEmp = (userFullName && (selectEmp.toLowerCase().indexOf(userFullName) !== -1 || empName.toLowerCase().indexOf(userFullName) !== -1));
      var matchEmail = (selectEmp.toLowerCase() === requesterClean || empName.toLowerCase().indexOf(requesterClean) !== -1);
      if (!matchEmp && !matchEmail) {
        continue;
      }
    }

    tasks.push({
      rowIndex: i + 1,
      timestamp: formatTimestamp(row[colMap["Timestamp"]]),
      ticketNumber: ticketNum,
      selectEmployee: selectEmp,
      jobType: String(row[colMap["Job Type"]] || "").trim(),
      dateOfPerform: formatDateOnly(row[colMap["Date of perform"]]),
      employeeName: empName,
      description: String(row[colMap["Description of the task"]] || "").trim(),
      department: String(row[colMap["Department List"]] || "").trim(),
      status: String(row[colMap["status"]] || "Pending").trim()
    });
  }

  tasks.sort(function(a, b) {
    return new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime();
  });

  return { success: true, message: "Tasks retrieved successfully", data: tasks };
}

function getNextTicketNumber(ss) {
  var lock = LockService.getScriptLock();
  try {
    lock.waitLock(10000);
    var currentYear = new Date().getFullYear();
    var prefix = "ITM " + currentYear + "-";
    var maxNum = 0;

    var sheet = ss.getSheetByName(CONFIG.TASKS_SHEET);
    var data = sheet.getDataRange().getValues();

    if (data.length > 1) {
      var headers = data[0].map(function(h) { return String(h).trim(); });
      var ticketColIdx = headers.indexOf("Ticket Number");
      if (ticketColIdx !== -1) {
        for (var i = 1; i < data.length; i++) {
          var val = String(data[i][ticketColIdx] || "").trim();
          if (val.indexOf(prefix) === 0) {
            var numStr = val.substring(prefix.length);
            var num = parseInt(numStr, 10);
            if (!isNaN(num) && num > maxNum) {
              maxNum = num;
            }
          }
        }
      }
    }

    var nextNum = maxNum + 1;
    var nextTicket = prefix + (nextNum < 10 ? "0" + nextNum : nextNum);
    return { success: true, message: "Next ticket generated", data: { nextTicketNumber: nextTicket } };
  } catch (e) {
    return { success: false, message: "Lock timeout or error: " + e.toString() };
  } finally {
    lock.releaseLock();
  }
}

function createTask(ss, email, role, task) {
  if (!task) return { success: false, message: "Task data is missing" };

  var lock = LockService.getScriptLock();
  try {
    lock.waitLock(15000);
    var nextTicketResult = getNextTicketNumber(ss);
    if (!nextTicketResult.success) return nextTicketResult;

    var ticketNumber = task.ticketNumber || nextTicketResult.data.nextTicketNumber;
    var sheet = ss.getSheetByName(CONFIG.TASKS_SHEET);
    var data = sheet.getDataRange().getValues();
    var headers = data[0].map(function(h) { return String(h).trim(); });

    for (var i = 1; i < data.length; i++) {
      if (String(data[i][headers.indexOf("Ticket Number")] || "").trim() === ticketNumber) {
        var resolved = getNextTicketNumber(ss);
        ticketNumber = resolved.data.nextTicketNumber;
        break;
      }
    }

    var now = new Date();
    var timestampStr = Utilities.formatDate(now, Session.getScriptTimeZone() || "GMT", "MM/dd/yyyy HH:mm:ss");

    var rowValues = [];
    headers.forEach(function(h) {
      switch (h) {
        case "Timestamp": rowValues.push(timestampStr); break;
        case "Ticket Number": rowValues.push(ticketNumber); break;
        case "Select Employee": rowValues.push(task.selectEmployee || ""); break;
        case "Job Type": rowValues.push(task.jobType || ""); break;
        case "Date of perform": rowValues.push(task.dateOfPerform || ""); break;
        case "Employee name": rowValues.push(task.employeeName || ""); break;
        case "Description of the task": rowValues.push(task.description || ""); break;
        case "Department List": rowValues.push(task.department || ""); break;
        case "status": rowValues.push(task.status || "Pending"); break;
        default: rowValues.push(""); break;
      }
    });

    sheet.appendRow(rowValues);
    logAudit(ss, email, "TASK_CREATED", "Ticket: " + ticketNumber + ", Employee: " + task.selectEmployee);

    addNotification(ss, "admin@all", "New Task Created: " + ticketNumber,
      (task.selectEmployee || "An employee") + " submitted a task: " + (task.jobType || ""), ticketNumber);

    return {
      success: true,
      message: "Task created successfully",
      data: { ticketNumber: ticketNumber, timestamp: timestampStr }
    };
  } catch (err) {
    return { success: false, message: "Error creating task: " + err.toString() };
  } finally {
    lock.releaseLock();
  }
}

function updateTask(ss, email, role, task) {
  if (!task || !task.ticketNumber) return { success: false, message: "Ticket Number required" };

  var sheet = ss.getSheetByName(CONFIG.TASKS_SHEET);
  var data = sheet.getDataRange().getValues();
  var headers = data[0].map(function(h) { return String(h).trim(); });
  var ticketColIdx = headers.indexOf("Ticket Number");
  var statusColIdx = headers.indexOf("status");

  var targetRowIdx = -1;
  for (var i = 1; i < data.length; i++) {
    if (String(data[i][ticketColIdx] || "").trim() === String(task.ticketNumber).trim()) {
      targetRowIdx = i + 1;
      break;
    }
  }

  if (targetRowIdx === -1) {
    return { success: false, message: "Task with ticket " + task.ticketNumber + " not found" };
  }

  var currentStatus = String(data[targetRowIdx - 1][statusColIdx] || "").trim();
  var isAdmin = String(role).toLowerCase() === "admin";

  if (!isAdmin && currentStatus.toLowerCase() === "completed") {
    return {
      success: false,
      message: "Completed tasks cannot be edited directly by users. Please submit an edit request."
    };
  }

  headers.forEach(function(h, colIdx) {
    var val = null;
    switch (h) {
      case "Select Employee": if (task.selectEmployee !== undefined) val = task.selectEmployee; break;
      case "Job Type": if (task.jobType !== undefined) val = task.jobType; break;
      case "Date of perform": if (task.dateOfPerform !== undefined) val = task.dateOfPerform; break;
      case "Employee name": if (task.employeeName !== undefined) val = task.employeeName; break;
      case "Description of the task": if (task.description !== undefined) val = task.description; break;
      case "Department List": if (task.department !== undefined) val = task.department; break;
      case "status": if (task.status !== undefined && (isAdmin || currentStatus.toLowerCase() !== "completed")) val = task.status; break;
    }
    if (val !== null) {
      sheet.getRange(targetRowIdx, colIdx + 1).setValue(val);
    }
  });

  logAudit(ss, email, "TASK_UPDATED", "Updated ticket: " + task.ticketNumber + ", Status: " + (task.status || currentStatus));

  return { success: true, message: "Task updated successfully", data: { ticketNumber: task.ticketNumber } };
}

function deleteTask(ss, email, role, ticketNumber) {
  if (!ticketNumber) return { success: false, message: "Ticket Number required" };
  var sheet = ss.getSheetByName(CONFIG.TASKS_SHEET);
  var data = sheet.getDataRange().getValues();
  var headers = data[0].map(function(h) { return String(h).trim(); });
  var ticketColIdx = headers.indexOf("Ticket Number");
  var statusColIdx = headers.indexOf("status");

  var targetRowIdx = -1;
  for (var i = 1; i < data.length; i++) {
    if (String(data[i][ticketColIdx] || "").trim() === String(ticketNumber).trim()) {
      targetRowIdx = i + 1;
      break;
    }
  }

  if (targetRowIdx === -1) {
    return { success: false, message: "Task not found" };
  }

  var isAdmin = String(role).toLowerCase() === "admin";
  var currentStatus = String(data[targetRowIdx - 1][statusColIdx] || "").trim();

  if (!isAdmin && currentStatus.toLowerCase() === "completed") {
    return { success: false, message: "Completed tasks cannot be deleted by normal users" };
  }

  sheet.deleteRow(targetRowIdx);
  logAudit(ss, email, "TASK_DELETED", "Deleted ticket: " + ticketNumber);
  return { success: true, message: "Task deleted successfully" };
}

function submitEditRequest(ss, email, name, ticketNumber, reason) {
  if (!ticketNumber || !reason) return { success: false, message: "Ticket Number and reason are required" };

  var editSheet = ss.getSheetByName(CONFIG.EDIT_REQUESTS_SHEET);
  var reqId = "REQ-" + new Date().getTime();
  var nowStr = Utilities.formatDate(new Date(), Session.getScriptTimeZone() || "GMT", "yyyy-MM-dd HH:mm:ss");

  editSheet.appendRow([reqId, ticketNumber, email || "", name || "", reason, nowStr, "Pending", "", ""]);
  logAudit(ss, email, "EDIT_REQUEST_SUBMITTED", "Ticket: " + ticketNumber + ", Reason: " + reason);

  addNotification(ss, "admin@all", "Edit Request for " + ticketNumber,
    (name || email) + " requested an edit: " + reason, ticketNumber);

  return { success: true, message: "Edit request submitted successfully. Awaiting admin approval.", data: { requestId: reqId } };
}

function getEditRequests(ss, email, role) {
  var editSheet = ss.getSheetByName(CONFIG.EDIT_REQUESTS_SHEET);
  var data = editSheet.getDataRange().getValues();
  if (data.length <= 1) return { success: true, message: "No edit requests", data: [] };

  var isAdmin = String(role).toLowerCase() === "admin";
  var cleanEmail = String(email || "").trim().toLowerCase();
  var requests = [];

  for (var i = 1; i < data.length; i++) {
    var row = data[i];
    var reqEmail = String(row[2] || "").trim().toLowerCase();
    if (!isAdmin && reqEmail !== cleanEmail) continue;

    requests.push({
      requestId: String(row[0] || ""),
      ticketNumber: String(row[1] || ""),
      requesterEmail: String(row[2] || ""),
      requesterName: String(row[3] || ""),
      reason: String(row[4] || ""),
      requestedAt: String(row[5] || ""),
      status: String(row[6] || "Pending"),
      adminRemarks: String(row[7] || ""),
      processedAt: String(row[8] || "")
    });
  }

  return { success: true, message: "Edit requests retrieved", data: requests };
}

function processEditRequest(ss, adminEmail, role, requestId, decision, remarks) {
  if (String(role).toLowerCase() !== "admin") {
    return { success: false, message: "Only administrators can process edit requests" };
  }
  var editSheet = ss.getSheetByName(CONFIG.EDIT_REQUESTS_SHEET);
  var data = editSheet.getDataRange().getValues();
  var rowIdx = -1;
  var targetRow = null;

  for (var i = 1; i < data.length; i++) {
    if (String(data[i][0] || "").trim() === String(requestId).trim()) {
      rowIdx = i + 1;
      targetRow = data[i];
      break;
    }
  }

  if (rowIdx === -1) return { success: false, message: "Request not found" };

  var nowStr = Utilities.formatDate(new Date(), Session.getScriptTimeZone() || "GMT", "yyyy-MM-dd HH:mm:ss");
  editSheet.getRange(rowIdx, 7).setValue(decision); // Status
  editSheet.getRange(rowIdx, 8).setValue(remarks || ""); // AdminRemarks
  editSheet.getRange(rowIdx, 9).setValue(nowStr); // ProcessedAt

  var ticketNumber = targetRow[1];
  var requesterEmail = targetRow[2];

  if (decision === "Approved") {
    var tSheet = ss.getSheetByName(CONFIG.TASKS_SHEET);
    var tData = tSheet.getDataRange().getValues();
    var tHeaders = tData[0].map(function(h) { return String(h).trim(); });
    for (var j = 1; j < tData.length; j++) {
      if (String(tData[j][tHeaders.indexOf("Ticket Number")] || "").trim() === ticketNumber) {
        tSheet.getRange(j + 1, tHeaders.indexOf("status") + 1).setValue("In Progress");
        break;
      }
    }
  }

  addNotification(ss, requesterEmail, "Edit Request " + decision + " for " + ticketNumber,
    "Your edit request was " + decision + ". Remarks: " + (remarks || "None"), ticketNumber);

  logAudit(ss, adminEmail, "EDIT_REQUEST_PROCESSED", "ID: " + requestId + ", Decision: " + decision);
  return { success: true, message: "Edit request marked as " + decision };
}

function getConfigLists(ss) {
  var sheet = ss.getSheetByName(CONFIG.CONFIG_LISTS_SHEET);
  var data = sheet.getDataRange().getValues();
  var jobTypes = [];
  var departments = [];

  for (var i = 1; i < data.length; i++) {
    var category = String(data[i][0] || "").trim();
    var val = String(data[i][1] || "").trim();
    var active = String(data[i][2] || "").toUpperCase() === "TRUE";
    if (!active || !val) continue;

    if (category === "JobType") jobTypes.push(val);
    else if (category === "Department") departments.push(val);
  }

  return { success: true, message: "Config lists retrieved", data: { jobTypes: jobTypes, departments: departments } };
}

function manageConfigList(ss, adminEmail, role, category, value, operation) {
  if (String(role).toLowerCase() !== "admin") return { success: false, message: "Admin authorization required" };
  var sheet = ss.getSheetByName(CONFIG.CONFIG_LISTS_SHEET);
  var data = sheet.getDataRange().getValues();

  if (operation === "add") {
    sheet.appendRow([category, value, "TRUE"]);
    logAudit(ss, adminEmail, "CONFIG_LIST_ADDED", category + ": " + value);
    return { success: true, message: "Item added successfully" };
  } else if (operation === "delete") {
    for (var i = 1; i < data.length; i++) {
      if (String(data[i][0] || "") === category && String(data[i][1] || "") === value) {
        sheet.deleteRow(i + 1);
        logAudit(ss, adminEmail, "CONFIG_LIST_DELETED", category + ": " + value);
        return { success: true, message: "Item deleted successfully" };
      }
    }
  }
  return { success: false, message: "Item not found or invalid operation" };
}

function getUsers(ss, adminEmail, role) {
  if (String(role).toLowerCase() !== "admin") return { success: false, message: "Admin access required" };
  var sheet = ss.getSheetByName(CONFIG.USERS_SHEET);
  var data = sheet.getDataRange().getValues();
  var headers = data[0].map(function(h) { return String(h).trim(); });
  var users = [];

  for (var i = 1; i < data.length; i++) {
    var row = data[i];
    var email = String(row[headers.indexOf("email")] || "").trim();
    if (!email) continue;

    users.push({
      firstName: String(row[headers.indexOf("f_name")] || "").trim(),
      lastName: String(row[headers.indexOf("l_name")] || "").trim(),
      role: String(row[headers.indexOf("role")] || "User").trim(),
      email: email
    });
  }

  return { success: true, message: "Users retrieved", data: users };
}

function createUser(ss, adminEmail, role, newUser) {
  if (String(role).toLowerCase() !== "admin") return { success: false, message: "Admin privileges required" };
  if (!newUser || !newUser.email || !newUser.password) return { success: false, message: "Email and password required" };

  var sheet = ss.getSheetByName(CONFIG.USERS_SHEET);
  var data = sheet.getDataRange().getValues();
  var headers = data[0].map(function(h) { return String(h).trim(); });
  var cleanEmail = newUser.email.trim().toLowerCase();

  for (var i = 1; i < data.length; i++) {
    if (String(data[i][headers.indexOf("email")] || "").trim().toLowerCase() === cleanEmail) {
      return { success: false, message: "User with this email already exists" };
    }
  }

  sheet.appendRow([newUser.firstName || "", newUser.lastName || "", newUser.role || "User", cleanEmail, newUser.password]);
  logAudit(ss, adminEmail, "USER_CREATED", "Created user: " + cleanEmail + " (" + (newUser.role || "User") + ")");
  return { success: true, message: "User created successfully" };
}

function updateUser(ss, adminEmail, role, targetUser) {
  if (String(role).toLowerCase() !== "admin") return { success: false, message: "Admin privileges required" };
  if (!targetUser || !targetUser.email) return { success: false, message: "Target email required" };

  var sheet = ss.getSheetByName(CONFIG.USERS_SHEET);
  var data = sheet.getDataRange().getValues();
  var headers = data[0].map(function(h) { return String(h).trim(); });
  var cleanEmail = targetUser.email.trim().toLowerCase();
  var rowIdx = -1;

  for (var i = 1; i < data.length; i++) {
    if (String(data[i][headers.indexOf("email")] || "").trim().toLowerCase() === cleanEmail) {
      rowIdx = i + 1;
      break;
    }
  }

  if (rowIdx === -1) return { success: false, message: "User not found" };

  if (targetUser.role !== undefined) sheet.getRange(rowIdx, headers.indexOf("role") + 1).setValue(targetUser.role);
  if (targetUser.firstName !== undefined) sheet.getRange(rowIdx, headers.indexOf("f_name") + 1).setValue(targetUser.firstName);
  if (targetUser.lastName !== undefined) sheet.getRange(rowIdx, headers.indexOf("l_name") + 1).setValue(targetUser.lastName);
  if (targetUser.password !== undefined && targetUser.password.length > 0) sheet.getRange(rowIdx, headers.indexOf("password") + 1).setValue(targetUser.password);

  logAudit(ss, adminEmail, "USER_UPDATED", "Updated user: " + cleanEmail);
  return { success: true, message: "User updated successfully" };
}

function changePassword(ss, email, currentPassword, newPassword) {
  if (!email || !newPassword) return { success: false, message: "Missing email or new password" };
  var sheet = ss.getSheetByName(CONFIG.USERS_SHEET);
  var data = sheet.getDataRange().getValues();
  var headers = data[0].map(function(h) { return String(h).trim(); });
  var cleanEmail = email.trim().toLowerCase();

  for (var i = 1; i < data.length; i++) {
    if (String(data[i][headers.indexOf("email")] || "").trim().toLowerCase() === cleanEmail) {
      var passCol = headers.indexOf("password") + 1;
      if (currentPassword && String(data[i][headers.indexOf("password")] || "").trim() !== currentPassword.trim()) {
        return { success: false, message: "Current password does not match" };
      }
      sheet.getRange(i + 1, passCol).setValue(newPassword.trim());
      logAudit(ss, cleanEmail, "PASSWORD_CHANGED", "User changed password");
      return { success: true, message: "Password updated successfully" };
    }
  }
  return { success: false, message: "User not found" };
}

function getReports(ss, email, role, startDate, endDate) {
  var tasksRes = getTasks(ss, email, role);
  var tasks = tasksRes.data || [];

  var byEmployee = {};
  var byDepartment = {};
  var byJobType = {};
  var byStatus = { "Pending": 0, "In Progress": 0, "Completed": 0, "Rejected": 0 };

  tasks.forEach(function(t) {
    var emp = t.selectEmployee || t.employeeName || "Unassigned";
    var dept = t.department || "General";
    var job = t.jobType || "Other";
    var st = t.status || "Pending";

    byEmployee[emp] = (byEmployee[emp] || 0) + 1;
    byDepartment[dept] = (byDepartment[dept] || 0) + 1;
    byJobType[job] = (byJobType[job] || 0) + 1;
    byStatus[st] = (byStatus[st] || 0) + 1;
  });

  return {
    success: true,
    message: "Report generated",
    data: {
      totalTasks: tasks.length,
      byStatus: byStatus,
      byEmployee: byEmployee,
      byDepartment: byDepartment,
      byJobType: byJobType
    }
  };
}

function addNotification(ss, recipientEmail, title, message, ticketNumber) {
  var sheet = ss.getSheetByName(CONFIG.NOTIFICATIONS_SHEET);
  var id = "NOTIF-" + new Date().getTime() + "-" + Math.floor(Math.random() * 1000);
  var nowStr = Utilities.formatDate(new Date(), Session.getScriptTimeZone() || "GMT", "yyyy-MM-dd HH:mm:ss");
  sheet.appendRow([id, recipientEmail, title, message, nowStr, "UNREAD", ticketNumber || ""]);
}

function getNotifications(ss, email) {
  var sheet = ss.getSheetByName(CONFIG.NOTIFICATIONS_SHEET);
  var data = sheet.getDataRange().getValues();
  var notifs = [];
  var cleanEmail = String(email || "").trim().toLowerCase();

  for (var i = 1; i < data.length; i++) {
    var row = data[i];
    var rec = String(row[1] || "").trim().toLowerCase();
    if (rec === "admin@all" || rec === cleanEmail) {
      notifs.push({
        id: String(row[0]),
        recipient: rec,
        title: String(row[2]),
        message: String(row[3]),
        timestamp: String(row[4]),
        isRead: String(row[5]) === "READ",
        ticketNumber: String(row[6])
      });
    }
  }

  notifs.sort(function(a, b) {
    return new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime();
  });

  return { success: true, message: "Notifications retrieved", data: notifs.slice(0, 50) };
}

function markNotificationRead(ss, email, notificationId) {
  var sheet = ss.getSheetByName(CONFIG.NOTIFICATIONS_SHEET);
  var data = sheet.getDataRange().getValues();
  for (var i = 1; i < data.length; i++) {
    if (String(data[i][0]) === String(notificationId)) {
      sheet.getRange(i + 1, 6).setValue("READ");
      return { success: true, message: "Notification marked as read" };
    }
  }
  return { success: false, message: "Notification not found" };
}

function provisionRecoveryAdmin(ss) {
  var sheet = ss.getSheetByName(CONFIG.USERS_SHEET);
  var data = sheet.getDataRange().getValues();
  if (!data.length || data[0].every(function(value) { return String(value).trim() === ""; })) {
    sheet.getRange(1, 1, 1, 5).setValues([["f_name", "l_name", "role", "email", "password"]]);
    data = sheet.getDataRange().getValues();
  }
  var headers = data[0].map(function(h) { return String(h).trim(); });
  var emailCol = headers.indexOf("email");
  if (emailCol === -1) {
    return { success: false, message: "Users sheet must include an email header" };
  }

  for (var i = 1; i < data.length; i++) {
    if (String(data[i][emailCol] || "").trim().toLowerCase() === CONFIG.RECOVERY_ADMIN_EMAIL.toLowerCase()) {
      return { success: true, message: "Recovery administrator account already exists" };
    }
  }

  sheet.appendRow(["Admin", "Recovery", "Admin", CONFIG.RECOVERY_ADMIN_EMAIL, CONFIG.RECOVERY_ADMIN_DEFAULT_PASS]);
  logAudit(ss, "SYSTEM", "RECOVERY_ADMIN_PROVISIONED", "Emergency recovery administrator created");
  return { success: true, message: "Recovery administrator account provisioned successfully" };
}

function logAudit(ss, userEmail, action, details) {
  try {
    var sheet = ss.getSheetByName(CONFIG.AUDIT_LOGS_SHEET);
    var nowStr = Utilities.formatDate(new Date(), Session.getScriptTimeZone() || "GMT", "yyyy-MM-dd HH:mm:ss");
    sheet.appendRow([nowStr, userEmail || "Anonymous", action, details]);
  } catch (e) {}
}

function formatTimestamp(val) {
  if (!val) return "";
  if (val instanceof Date) {
    return Utilities.formatDate(val, Session.getScriptTimeZone() || "GMT", "MM/dd/yyyy HH:mm:ss");
  }
  return String(val);
}

function formatDateOnly(val) {
  if (!val) return "";
  if (val instanceof Date) {
    return Utilities.formatDate(val, Session.getScriptTimeZone() || "GMT", "yyyy-MM-dd");
  }
  return String(val);
}
