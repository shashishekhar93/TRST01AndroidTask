# TRST01 Android Task

An enterprise-grade Android application built for employee management, document inspection, and geographic area mapping. The application follows modern Android development best practices, implementing an offline-first MVVM architecture with Kotlin, Room, Hilt, and Coroutines.

---

## 1. Project Overview

TRST01AndroidTask is an employee management system featuring:
- **Employee Directory & Search:** Comprehensive employee listing with branch filtering and real-time search.
- **Employee Details & CRUD:** Detailed view of employee information, along with creation, editing, and deletion capabilities.
- **Document Management & Preview:** Listing of employee identification and official documents with rich previews:
  - **Image Preview:** Smooth image loading and rendering using Glide with shimmer placeholders and error states.
  - **PDF Preview:** Multi-page PDF rendering using Android's built-in `PdfRenderer` with vertical scrolling and disk caching.
- **Geographic Map Areas:** Google Maps-based polygon creation, editing, saving, and clearing for employee operational zones.
- **Offline-First Persistence:** Local database persistence via Room with reactive Flow observation.

---

## 2. Technology Stack

- **Language:** Kotlin
- **UI Architecture:** XML Layouts with ViewBinding
- **Navigation:** Single Activity Architecture with Jetpack Navigation Component (`NavHostFragment`)
- **Architecture Pattern:** MVVM (Model-View-ViewModel) with Repository Pattern
- **Dependency Injection:** Hilt
- **Networking & Parsing:** Retrofit 2, OkHttp 3, Gson Converter
- **Local Database:** Room Persistence Library
- **Concurrency:** Kotlin Coroutines and Flows (`StateFlow`, `SharedFlow`)
- **Image Loading:** Glide
- **Maps:** Google Maps SDK for Android
- **PDF Rendering:** `android.graphics.pdf.PdfRenderer`
- **SDK Configuration:**
  - `compileSdk`: 37
  - `minSdk`: 26 (Android 8.0 Oreo)
  - `targetSdk`: 37

---

## 3. Project Structure

```text
com.shekhar.trst01androidtask/
├── data/
│   ├── local/
│   │   ├── converter/      # Room TypeConverters (e.g. LatLngPoint list converter)
│   │   ├── dao/            # Room DAOs (EmployeeDao, DocumentDao, MapAreaDao)
│   │   ├── database/       # AppDatabase and DatabaseModule (Hilt)
│   │   └── entity/         # Room Entities (EmployeeEntity, DocumentEntity, MapAreaEntity)
│   ├── mapper/             # Data mappers (API DTOs to Room Entities & Documents)
│   ├── remote/
│   │   ├── api/            # Retrofit EmployeeApiService and NetworkModule (Hilt)
│   │   └── model/          # API Response and DTO models
│   └── repository/         # EmployeeRepository interface, implementation, and RepositoryModule
├── ui/
│   ├── add/                # Add Employee screen (ViewModel, Fragment, UI State)
│   ├── delete/             # Delete Employee confirmation bottom sheet
│   ├── details/            # Employee Details screen and DocumentAdapter
│   ├── edit/               # Edit Employee screen (ViewModel, Fragment, UI State)
│   ├── list/               # Employee List screen, search, and branch filtering
│   ├── map/                # Google Maps screen for polygon area creation and editing
│   ├── preview/            # Document Preview screen (Image & multi-page PDF rendering)
│   ├── EmployeeAdapter.kt  # RecyclerView adapter for employee list
│   ├── EmployeeViewModel.kt# ViewModel for employee directory & search
│   └── MainActivity.kt     # Single Activity host
└── util/                   # Shared utilities (e.g. DateFormatter, DocumentUtils)
```

---

## 4. Setup and Installation

1. **Clone the Repository:**
   ```bash
   git clone <repository-url>
   ```
2. **Open in Android Studio:**
   Open Android Studio and select **Open an Existing Project**, pointing to the `TRST01AndroidTask` root directory.
3. **Gradle Sync:**
   Allow Android Studio to sync Gradle and download dependencies.
4. **Configure API Base URL:**
   The API base URL is defined in [NetworkModule.kt](file:///Users/shekharmishra/AndroidStudioProjects/TRST01AndroidTask/app/src/main/java/com/shekhar/trst01androidtask/data/remote/api/NetworkModule.kt):
   ```kotlin
   private const val BASE_URL = "https://mocki.io/"
   ```
5. **Configure Google Maps API Key:**
   Provide your Google Maps API key in [AndroidManifest.xml](file:///Users/shekharmishra/AndroidStudioProjects/TRST01AndroidTask/app/src/main/AndroidManifest.xml):
   ```xml
   <meta-data
       android:name="com.google.android.geo.API_KEY"
       android:value="YOUR_GOOGLE_MAPS_API_KEY" />
   ```
6. **Build and Run:**
   Connect a physical Android device or start an emulator (API 26+) and click **Run 'app'**.

---

## 5. API Usage

- **Base URL:** `https://mocki.io/`
- **Endpoint:** `GET v1/08f6af34-d99f-4ab2-bd09-6c9e953829d4`
- **Purpose:** Fetches initial or refreshed employee directory data, including nested employee details and documents.
- **Response Structure:** Root object (`EmployeeResponse`) containing success status, message code, and a list of `EmployeeDataDto` items holding `EmployeeDetailDto` and `DocumentDto` lists.
- **Parsing:** Parsed automatically via Retrofit's Gson converter.
- **Mapping & Persistence:** Mapped into `EmployeeEntity` and `DocumentEntity` records via [EmployeeMapper.kt](file:///Users/shekharmishra/AndroidStudioProjects/TRST01AndroidTask/app/src/main/java/com/shekhar/trst01androidtask/data/mapper/EmployeeMapper.kt) and stored locally in Room.
- **Offline Access:** The UI observes Room database flows (`observeEmployees()`, `observeEmployeeById()`, `observeDocumentsForEmployee()`, `observeMapAreaForEmployee()`), ensuring seamless offline availability and reactive updates.

---

## 6. Local Database

- **Entities:**
  - `EmployeeEntity`: Stores employee profile details (primary key `employeeId`).
  - `DocumentEntity`: Stores associated documents linked via foreign key to `employees(employeeId)` with `ON DELETE CASCADE`.
  - `MapAreaEntity`: Stores geographic polygon coordinates (`List<LatLngPoint>`) linked via foreign key to `employees(employeeId)` with `ON DELETE CASCADE`.
- **Key Behaviors:**
  - **In-Place Updates:** Employee edits utilize Room's `@Update` operation rather than `REPLACE`, ensuring associated documents and map areas are never wiped out during edits or API refreshes.
  - **Cascading Deletions:** Explicit deletion of an employee correctly triggers foreign-key cascading deletion to remove associated documents and map area records.

---

## 7. Main Application Features

- **Employee List & Search:** View all employees, filter by branch tabs, or search instantly by name, department ID, or employee ID with a quick-clear action.
- **Employee Details:** Inspect employee metadata, verification status, associated documents list, and map area summary.
- **Add, Edit & Delete:** Create new employee records, update existing details in-place safely, or delete employees with cascade cleanup.
- **Document Preview:** Preview image documents via Glide or render multi-page PDFs vertically inside `DocumentPreviewFragment`.
- **Google Maps Integration:** Draw, edit, save, or clear operational boundary polygons on Google Maps.

---

## 8. Assumptions and Limitations

- **Network Availability:** Internet connection is required for initial data synchronization from the mock endpoint and remote PDF/image downloading.
- **Google Maps API Key:** A valid Google Maps API key with Maps SDK enabled is required for map functionality.
- **PDF & Image URLs:** Remote PDF and image URLs provided by data sources must be accessible over HTTP/HTTPS.
- **Caching Directory:** Downloaded remote PDFs are stored in the application's internal cache directory (`context.cacheDir`) to avoid redundant downloads without cluttering permanent storage.

---

## 9. Testing

To run unit tests and build the application, use the following Gradle commands:

```bash
./gradlew :app:testDebugUnitTest --console=plain
./gradlew :app:assembleDebug --console=plain
```
