# Google Apps Script Backend Deployment Guide

## 1. Open Google Sheets
1. Open your existing Google Spreadsheet containing:
   - Sheet 1: `Users` (Columns: `f_name`, `l_name`, `role`, `email`, `password`)
   - Sheet 2: `Form_Responses` (Columns: `Timestamp`, `Ticket Number`, `Select Employee`, `Job Type`, `Date of perform`, `Employee name`, `Description of the task`, `Department List`, `status`)

## 2. Add the Backend Code
1. Click **Extensions** > **Apps Script** in the top menu of your Google Sheet.
2. In the Apps Script code editor, delete any template code and paste the complete contents of `backend/Code.gs`.
3. Click the **Save** icon (diskette icon) or press `Ctrl + S`.
4. Rename the project to `GMS Task Backend`.

## 3. Deploy as Web App
1. Click the blue **Deploy** button in the top-right corner.
2. Select **New deployment**.
3. Under **Select type** (gear icon), choose **Web app**.
4. Configure the settings:
   - **Description**: `GMS Mobile App API v1`
   - **Execute as**: `Me (your email address)`
   - **Who has access**: `Anyone` (this allows your Android app to call the API via HTTPS POST/GET)
5. Click **Deploy**.
6. Authorize the permissions when prompted:
   - Click **Review Permissions**.
   - Select your Google account.
   - Click **Advanced** > **Go to GMS Task Backend (unsafe)**.
   - Click **Allow**.
7. Copy the **Web App URL** provided (it looks like `https://script.google.com/macros/s/AKfycb.../exec`).

## 4. Enter URL in the Android App
- Launch the GMS Task Management Android app on your phone.
- On first launch, the app displays the **System Setup** screen.
- Paste the Web App URL and tap **Test Connection & Save**.
- The app will automatically connect and navigate to the Login screen!
