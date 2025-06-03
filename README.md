# Backend Trial Android Application

## Project Overview
This is an Android application that provides a tab-based interface with three main features: Home, Camera, and Gallery. The app is built using Kotlin and follows modern Android development practices. It includes integration with a backend server for iris analysis functionality.

## Technical Stack
- **Language**: Kotlin 2.0.21
- **Minimum SDK**: 24 (Android 7.0 Nougat)
- **Target SDK**: 35 (Android 15)
- **Compile SDK**: 35
- **Java Version**: 11
- **Architecture**: Standard Android Activity-Fragment architecture
- **UI Components**: 
  - AppCompat 1.6.1
  - Material Design Components 1.11.0
  - TabLayout for navigation
- **Networking**: 
  - OkHttp 4.9.3 for HTTP client
  - Glide 4.12.0 for image loading
- **Image Processing**:
  - UCrop 2.2.8 for image cropping
- **Compose Integration**:
  - Compose BOM: 2024.09.00
  - Material3
  - Activity Compose 1.10.1
  - Lifecycle Runtime KTX 2.9.0

## Project Structure
```
app/src/main/
├── java/com/example/backend_trial/
│   ├── MainActivity.kt                 # Main activity with tab navigation
│   ├── HomeFragment.kt                # Home screen implementation
│   ├── CameraFragment.kt              # Camera functionality
│   ├── GalleryFragment.kt             # Gallery view implementation
│   ├── ApiConfig.kt                   # API configuration
│   └── network/
│       ├── ApiService.kt              # API service interface
│       └── NetworkModule.kt           # Network configuration
├── res/
│   ├── layout/
│   │   ├── activity_main.xml         # Main activity layout
│   │   ├── fragment_home.xml         # Home screen layout
│   │   ├── fragment_camera.xml       # Camera screen layout
│   │   └── fragment_gallery.xml      # Gallery screen layout
│   ├── xml/
│   │   ├── network_security_config.xml  # Network security settings
│   │   └── file_paths.xml            # File provider paths
│   └── ...
└── AndroidManifest.xml               # App manifest
```

## Application Flow

### 1. Home Screen
- Initial landing page
- Displays app information and instructions
- Provides navigation to other sections

### 2. Camera Screen
- Implements camera functionality using CameraX
- Features:
  - Live camera preview
  - Iris capture capability
  - Image capture button
  - Flash control
  - Camera switch (front/back)
- Image Processing:
  - Automatic iris detection
  - Image cropping using UCrop
  - Image compression before upload

### 3. Gallery Screen
- Displays captured images
- Features:
  - Grid view of images
  - Image details view
  - Delete functionality
  - Share capability
- Image Loading:
  - Efficient image loading with Glide
  - Caching implementation
  - Memory management

### 4. Backend Integration
- API Communication:
  - Base URL: `http://37.27.221.244:8000`
  - Endpoint: `/analyze_iris/`
  - Method: POST
  - Content Type: multipart/form-data
- Network Configuration:
  - Connection Timeout: 30 seconds
  - Write Timeout: 30 seconds
  - Read Timeout: 60 seconds
- Error Handling:
  - Network error handling
  - Server error handling
  - Timeout handling

## Dependencies and Versions

### Core Dependencies
```gradle
// AndroidX Core
implementation("androidx.core:core-ktx:1.16.0")
implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.0")

// UI Components
implementation("androidx.appcompat:appcompat:1.6.1")
implementation("com.google.android.material:material:1.11.0")

// Networking
implementation("com.squareup.okhttp3:okhttp:4.9.3")
implementation("com.github.bumptech.glide:glide:4.12.0")

// Image Processing
implementation("com.github.yalantis:ucrop:2.2.8")
```

### Compose Dependencies
```gradle
implementation(platform("androidx.compose:compose-bom:2024.09.00"))
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.material3:material3")
implementation("androidx.activity:activity-compose:1.10.1")
```

### Testing Dependencies
```gradle
testImplementation("junit:junit:4.13.2")
androidTestImplementation("androidx.test.ext:junit:1.2.1")
androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
```

## Build Configuration

### Gradle Configuration
- Android Gradle Plugin: 8.10.0
- Kotlin Plugin: 2.0.21
- Compose Plugin: 2.0.21

### Build Types
- Debug:
  - Minify enabled: false
  - Debuggable: true
- Release:
  - Minify enabled: false
  - ProGuard rules applied

## Server Configuration

### Changing Server IP Address
The server IP address needs to be updated in two locations:

1. **ApiConfig.kt**
```kotlin
object ApiConfig {
    const val BASE_URL = "http://YOUR_NEW_IP:8000"
    // ... other configurations
}
```

2. **network_security_config.xml**
```xml
<network-security-config>
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="true">YOUR_NEW_IP</domain>
    </domain-config>
</network-security-config>
```

## Setup and Installation

### Prerequisites
- Android Studio (latest version recommended)
- Android SDK 35
- Gradle 8.10.0
- JDK 11
- Backend server running (if testing API integration)

### Building the Project
1. Clone the repository
2. Open the project in Android Studio
3. Update server configuration if needed (see Server Configuration section)
4. Sync Gradle files
5. Build and run the application

## Testing

### Unit Tests
- API service tests
- ViewModel tests
- Repository tests
- Utility function tests

### Integration Tests
- API integration tests
- Camera functionality tests
- Gallery functionality tests
- Image processing tests

### UI Tests
- Navigation tests
- Camera capture tests
- Gallery view tests
- Error handling tests

## Troubleshooting

### Common Issues and Solutions

1. **API Connection Issues**
   - Verify server IP address in both configuration files
   - Check network connectivity
   - Verify server is running and accessible
   - Check API endpoint configuration

2. **Camera Issues**
   - Verify camera permissions
   - Check device compatibility
   - Verify camera initialization
   - Check camera preview implementation

3. **Gallery Issues**
   - Verify storage permissions
   - Check file provider configuration
   - Verify image loading implementation
   - Check memory management

4. **Build Issues**
   - Verify Gradle version
   - Check SDK versions
   - Verify dependency versions
   - Check ProGuard rules

## Future Improvements
1. Add proper error handling
2. Implement proper image caching
3. Add unit tests
4. Implement proper state management
5. Add proper documentation for each fragment
6. Implement proper logging system
7. Add analytics tracking
8. Implement proper error reporting
9. Add offline support
10. Implement proper image compression
11. Add biometric authentication
12. Implement secure storage
13. Add user preferences
14. Implement background processing
15. Add push notifications

## Contributing
[Add contribution guidelines here]

## License
[Add license information here] 