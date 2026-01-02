# Game Logger Testing Guide

## Test Coverage (58/58 passing)

### Unit Tests (15 tests)
- **DiscoverViewModelTest** - Game discovery and search functionality
- **SearchViewModelTest** - Search queries and filtering
- **DiaryViewModelTest** - Game log display logic
- **GalleryViewModelTest** - Photo management logic
- **LogGameViewModelTest** - Game logging business logic
- **TimerViewModelTest** - Play time tracking logic
- **GameDetailsViewModelTest** - Game detail display
- **SettingsViewModelTest** - App settings management
- **ThemeRepositoryTest** - Theme persistence
- **IgdbServiceTest** - External API integration
- **GameModelTest** - Game data model validation
- **GameLogModelTest** - Game log data model
- **GameDetailsUtilsTest** - Utility functions
- **DateUtilsTest** - Date formatting utilities
- **ExampleUnitTest** - Basic test setup

### Integration Tests (43 tests)
- **GameLogDaoIntegrationTest** - Database operations, CRUD, queries
- **DiaryViewModelIntegrationTest** - Game log display, filtering, deletion (6 tests)
- **GalleryViewModelIntegrationTest** - Photo management, filtering (6 tests) 
- **LogGameViewModelIntegrationTest** - Game logging, rating, review updates (9 tests)
- **TimerViewModelIntegrationTest** - Play time tracking, manual adjustments (11 tests)
- **EndToEndIntegrationTest** - Complete user workflows across features (6 tests)
- **ExampleInstrumentedTest** - Basic instrumentation test

## Running Tests

### Unit Tests
In Android Studio or via Terminal:
```bash
./gradlew testDebugUnitTest
# Report: app/build/reports/tests/testDebugUnitTest/index.html
```

### Integration Tests
**Prerequisites:** Android device/emulator connected (API 21+)

```bash
# All integration tests
./gradlew connectedDebugAndroidTest

# Specific test class
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=\
  com.example.gamelogger.ui.features.timer.TimerViewModelIntegrationTest

# Report: app/build/reports/androidTests/connected/debug/index.html
```
---