# Draggable DatePicker for Android 📅

A **modern, lightweight, and fully customizable date range picker** for **Jetpack Compose**,
featuring **smooth drag-based date selection**—perfect for travel, booking, and scheduling apps.

Built with performance, flexibility, and Compose best practices in mind.

---

## ✨ Features

- **Draggable Date Range Selection**  
  Select a continuous range of dates with a smooth drag gesture.

- **Jetpack Compose Native**  
  100% built using Jetpack Compose — no XML, no legacy Views.

- **State-Driven API**  
  Clean and predictable state management using `rememberDraggableDateRangePickerState()`.

- **Highly Customizable**  
  Customize year range, colors, typography, cell decorations, and tags.

- **Lightweight & Performant**  
  Minimal dependencies and optimized Canvas drawing for smooth scrolling.

- **Booking App Friendly**  
  Designed for use cases like flights, hotels, and event scheduling.

---

## 📸 Preview

<p align="center">
  <img src="assets/preview.gif" width="420" alt="Draggable DatePicker Preview"/>
</p>

## 📦 Installation

### 1️⃣ Add JitPack Repository

Add JitPack to your `settings.gradle.kts` file:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

### 2️⃣ Add Dependency

Add the library to your app-level `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.dev-rahuljangra:draggable-datepicker:<latest-version>")
}
```

🔗 **Latest version:** [GitHub Repository](https://github.com/dev-rahuljangra/draggable-datepicker)

## 🛠️ Usage

Using the `DraggableDateRangePicker` in your Compose code is simple.
Just remember to use `rememberDraggableDateRangePickerState()` to manage the selection.

- Adding Tags to Dates

You can attach one or more **tags** to specific dates (for example: holidays, prices, events, or
offers).

Tags are applied via the picker state using the `setTags()` API.


```kotlin
@Composable
fun MyDateSelectionScreen() {
    val state = rememberDraggableDateRangePickerState()
      LaunchedEffect(Unit) {
        delay(3000)
    
        state.setTags(
          mapOf(
            LocalDate.of(2026, 1, 1).toEpochDay() to
                    listOf(CalendarTag(text = "New Year"))
          )
        )
      }
    DraggableDateRangePicker(
        state = state,
        startYear = 2026,
        endYear = 2026,
        modifier = Modifier.fillMaxSize()
    )

    // Access selected dates via state.startDate and state.endDate
}
```

## 🎨 Customization

- You can customize the picker to match your app design:

- Year range (startYear, endYear)

- Date cell colors and typography

- Selected range styling

- Tag indicators and alignment

- Disabled / blocked dates (if enabled)

- Detailed theming and styling examples will be added soon.

## 🧩 Common Use Cases

- Flight and hotel booking

- Event scheduling

- Leave management

- Subscription period selection

- Custom calendar-based workflows

## 🤝 Contributing

- Contributions are welcome and appreciated 🙌

- Fork the repository

- Create a new feature branch

- Commit your changes

- Open a pull request

For major changes, please open an issue first to discuss.

## 📄 License

```text
Copyright 2026 Rahul Jangra

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
