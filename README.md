# GMS Task Management Native Android Application

A complete, lightweight native Android mobile application built with **Java** and **XML layouts** that uses **Google Sheets** as its primary live data source via a secure **Google Apps Script Web App** backend.

---

## 1. Project Overview & Features

- **Platform**: Native Android (API 26 to API 35)
- **Language**: Java
- **UI Framework**: Native Android XML Layouts, Material Design Components
- **Build System**: Gradle 8.14.4 + Android Gradle Plugin 8.6.1 (Command Line, No Android Studio Required)
- **Primary Data Source**: Google Sheets (`Users` and `Form_Responses`)
- **Backend API**: Google Apps Script (`backend/Code.gs`)
- **Push Notifications**: Firebase Cloud Messaging (FCM) & Native Android Notification Channels
- **Roles & Permissions**:
  - **Administrator**: Comprehensive overview, all tasks across employees, live counters, user creation & permission management, edit-request approvals, report analytics, CSV export, dynamic job type & department management, system settings.
  - **Employee (User)**: Personalized dashboard, view own authorized tasks, real-time ticket numbering (`ITM 2026-XX`), task submission, edit requests for completed tasks, notifications.

---

## 2. Spreadsheet Data Structure

### A. Users Table (`Users` Sheet)
| Column | Description | Example Record |
| :--- | :--- | :--- |
| `f_name` | First Name | `Sahath` |
| `l_name` | Last Name | `Talip` |
| `role` | Account Role (`Admin` or `User`) | `Admin` |
| `email` | Login Email | `lankasaltltdzoom@gmail.com` |
| `password` | Authentication Secret | `trst1992` |

*Other existing users in sheet*: `Binusha Perera` (User), `Athif Shaheer` (User), `Manoj Chathuranga` (User).

### B. Task Management Table (`Form_Responses` Sheet)
| Column | Description | Example Record |
| :--- | :--- | :--- |
| `Timestamp` | Submission Timestamp | `10/9/2026 15:03:56` |
| `Ticket Number` | Unique Reference Number | `ITM 2026-15` |
| `Select Employee` | Primary Assigned Employee | `Binusha Perera` |
| `Job Type` | Task Category | `Network Cabling / Route` |
| `Date of perform` | Date Work Carried Out | `2026-10-09` |
| `Employee name` | Team / Personnel Involved | `sachini, chayni, achichi` |
| `Description of the task` | Detailed Work Description | `Readded ip tp the pc network` |
| `Department List` | Associated Department | `Accounts / Finance` |
| `status` | Current Workflow Status | `Completed` |

---

## 3. Google Apps Script Backend Setup

1. Open your Google Spreadsheet containing the `Users` and `Form_Responses` worksheets.
2. In the top toolbar, navigate to **Extensions** > **Apps Script**.
3. Replace any existing code with the complete content of [`backend/Code.gs`](file:///h:/newapp/backend/Code.gs).
4. Click **Save** (`Ctrl + S`).
5. Click **Deploy** > **New deployment**.
6. Select type: **Web app** (gear icon).
   - **Description**: `GMS Task Management API`
   - **Execute as**: `Me (your Google account)`
   - **Who has access**: `Anyone`
7. Click **Deploy**, authorize permissions, and copy the **Web App URL** (`https://script.google.com/macros/s/.../exec`).
8. Paste this URL into the app's **System Setup** screen on first launch.

---

## 4. Development Environment (No Android Studio)

The project builds entirely from the Windows PowerShell / Command Prompt using:
- **Java JDK**: Microsoft OpenJDK 21 LTS (`C:\Program Files\Microsoft\jdk-21.0.11.10-hotspot`)
- **Android SDK**: `C:\Users\USHAN\AppData\Local\Android\Sdk`
  - Platforms: `android-35`
  - Build-tools: `35.0.0`
  - Platform-tools: `adb.exe`
- **Gradle**: Project-local Gradle Wrapper (`gradlew.bat`) configured to Gradle `8.14.4`.

---

## 5. Building the Application

Open PowerShell in `H:\newapp`:

```powershell
# 1. Clean previous builds (optional)
.\gradlew.bat clean

# 2. Build Debug APK
.\gradlew.bat assembleDebug
```

The generated APK is located at:
`H:\newapp\app\build\outputs\apk\debug\app-debug.apk`

---

## 6. Installing on a Physical Android Phone via USB

### Step A: Prepare Your Phone
1. Open **Settings** on your phone.
2. Go to **About Phone** and tap **Build Number** 7 times until you see *"You are now a developer!"*.
3. Go back to **Settings** > **System** (or **Developer Options**).
4. Enable **Developer Options** and turn ON **USB Debugging**.
5. Connect your phone to your Windows laptop with a USB cable.
6. When prompted on your phone's screen, check **"Always allow from this computer"** and tap **Allow**.

### Step B: Verify ADB & Install APK
Open PowerShell:

```powershell
# Verify phone is detected
adb devices
# Output should show:
# List of devices attached
# <device_serial>   device

# Install or update the APK
adb install -r app\build\outputs\apk\debug\app-debug.apk

# Launch the app immediately on your phone
adb shell am start -n com.gms.taskmanagement/.MainActivity
```

---

## 7. Generating a Signed Release APK

To create a production-ready signed APK without Android Studio:

```powershell
# 1. Generate a keystore (run once)
keytool -genkey -v -keystore gms-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias gmskey

# 2. Build unsigned release APK
.\gradlew.bat assembleRelease

# 3. Align and sign with apksigner (located in build-tools/35.0.0)
$BUILD_TOOLS = "C:\Users\USHAN\AppData\Local\Android\Sdk\build-tools\35.0.0"
& "$BUILD_TOOLS\zipalign.exe" -v -p 4 app\build\outputs\apk\release\app-release-unsigned.apk app-release-aligned.apk
& "$BUILD_TOOLS\apksigner.bat" sign --ks gms-release-key.jks --out app-release-signed.apk app-release-aligned.apk
```

---

## 8. Emergency Administrator Account

For emergency account recovery:
- **Email**: `admin@gmail.com`
- **Initial Password**: `admin123`
- If this account is missing from the `Users` sheet, logging in with these credentials provisions it automatically. It can also be provisioned via the **System Setup** screen button.
- The user is prompted to change this password on the profile screen upon login.
