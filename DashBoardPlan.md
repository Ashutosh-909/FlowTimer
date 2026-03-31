# Flow Time — Dashboard Plan

## Purpose

A stats & history screen accessible by swiping right from the home screen. Gives the user a clear view of how much time they've spent in deep flow state, visualised with a bar chart and a calendar heatmap.

---

## A. Feature Overview

### Navigation

- **Entry:** Swipe right on the landing page. Implemented with a `HorizontalPager` (2 pages: Home = page 1, Dashboard = page 0). No bottom bar, no drawer — the same gesture-driven philosophy as the rest of the app.
- **MainActivity** wraps `HorizontalPager`; `HomeScreen` and `DashboardScreen` are the two pages. Pager starts on page 1 (Home). A subtle edge indicator or dot pair hints at the swipeable dashboard.

### Screen Layout (top → bottom)

| Section | Description |
|---|---|
| **Timeframe Selector** | Segmented row of three chips: **Week · Month · Year**. Pixel-styled, `GlowBlue` selected state, `CardSurface` unselected. |
| **Total Focus Time** | Large bold headline: e.g., **"12 h 35 min"** (or **"185 min"** if < 1 h). Aggregated for the selected timeframe. Pixel font, `PixelText` color. |
| **Bar Chart** | Manhattan-style bar chart. X-axis = time buckets (7 days / ~4 weeks / 12 months). Y-axis = total focus minutes. Bars use `GlowBlue` fill with `GlowCyan` top highlight. Tapping a bar shows a tooltip with the exact value. Compose `Canvas`-drawn — no third-party charting library. |
| **Calendar Heatmap** | GitHub-style contribution grid, independent of the timeframe selector. Horizontally scrollable. Each cell = one day, colored by total focus minutes (0 = `SpaceMid`, low = dim `GlowBlue`, high = bright `GlowCyan`, exceptional = `SandGold`). Shows ~6 months of history. Month labels above each column group. |
| **Share Button** | Pixel-styled button at the bottom. Captures the chart + stats into a bitmap, launches the system share sheet (image + optional text). Uses `View.drawToBitmap()` or `Modifier.drawWithContent` → `ImageBitmap` → `FileProvider` URI. |

---

## B. Data Layer — Session History

### Current Gap

The app currently stores only a running counter (`COMPLETED_SESSION_COUNT`) and last completion epoch. The dashboard needs **per-session records** with date and duration to build charts and heatmaps.

### New: Room Database for Session History

A lightweight Room database is introduced alongside the existing DataStore. DataStore continues to own transient timer state; Room owns historical records.

#### Entity: `FlowSession`

```kotlin
@Entity(tableName = "flow_sessions")
data class FlowSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startEpochMillis: Long,       // System.currentTimeMillis() when session started
    val durationMinutes: Int,          // configured duration for the session
    val completedEpochMillis: Long     // System.currentTimeMillis() when session finished
)
```

#### DAO: `FlowSessionDao`

```kotlin
@Dao
interface FlowSessionDao {
    @Insert
    suspend fun insert(session: FlowSession)

    /** All sessions in a date range, ordered oldest-first. */
    @Query("SELECT * FROM flow_sessions WHERE completedEpochMillis BETWEEN :startMillis AND :endMillis ORDER BY completedEpochMillis ASC")
    fun sessionsInRange(startMillis: Long, endMillis: Long): Flow<List<FlowSession>>

    /** Daily aggregation: sum of durationMinutes grouped by calendar date. */
    @Query("""
        SELECT date(completedEpochMillis / 1000, 'unixepoch', 'localtime') AS day,
               SUM(durationMinutes) AS totalMinutes,
               COUNT(*) AS sessionCount
        FROM flow_sessions
        WHERE completedEpochMillis BETWEEN :startMillis AND :endMillis
        GROUP BY day
        ORDER BY day ASC
    """)
    fun dailyAggregates(startMillis: Long, endMillis: Long): Flow<List<DailyAggregate>>
}

data class DailyAggregate(
    val day: String,           // "2026-03-07"
    val totalMinutes: Int,
    val sessionCount: Int
)
```

#### Database: `FlowTimerDatabase`

```kotlin
@Database(entities = [FlowSession::class], version = 1, exportSchema = false)
abstract class FlowTimerDatabase : RoomDatabase() {
    abstract fun flowSessionDao(): FlowSessionDao
}
```

#### Repository: `SessionRepository`

```kotlin
class SessionRepository(private val dao: FlowSessionDao) {
    suspend fun recordSession(startEpoch: Long, durationMinutes: Int, completedEpoch: Long) {
        dao.insert(FlowSession(
            startEpochMillis = startEpoch,
            durationMinutes = durationMinutes,
            completedEpochMillis = completedEpoch
        ))
    }

    fun dailyAggregates(startMillis: Long, endMillis: Long): Flow<List<DailyAggregate>> =
        dao.dailyAggregates(startMillis, endMillis)

    fun sessionsInRange(startMillis: Long, endMillis: Long): Flow<List<FlowSession>> =
        dao.sessionsInRange(startMillis, endMillis)
}
```

### Integration Point

`TimerForegroundService.onTimerFinished()` currently calls `repository.recordSessionCompleted()`. After this change it will **also** insert a `FlowSession` row via `SessionRepository`:

```kotlin
private fun onTimerFinished() {
    // ... existing notification + DataStore logic ...
    serviceScope.launch {
        // existing DataStore writes...
        repository.recordSessionCompleted()

        // NEW: persist session to Room
        val startEpoch = repository.lastStartEpoch.first()
        val duration = repository.flowDurationMinutes.first()
        sessionRepository.recordSession(
            startEpoch = startEpoch,
            durationMinutes = duration,
            completedEpoch = System.currentTimeMillis()
        )
    }
}
```

---

## C. UI Layer

### Package Structure

```
com.ashutosh.flowtimer
├── core/
│   ├── data/
│   │   ├── db/                        # NEW
│   │   │   ├── FlowSession.kt
│   │   │   ├── FlowSessionDao.kt
│   │   │   ├── FlowTimerDatabase.kt
│   │   │   └── DailyAggregate.kt
│   │   ├── SessionRepository.kt       # NEW
│   │   ├── PreferencesRepository.kt
│   │   └── PreferencesKeys.kt
├── ui/
│   ├── dashboard/                     # NEW
│   │   ├── DashboardScreen.kt
│   │   └── DashboardViewModel.kt
│   ├── components/
│   │   ├── FlowBarChart.kt            # NEW — Canvas bar chart
│   │   ├── CalendarHeatmap.kt         # NEW — scrollable heatmap grid
│   │   ├── TimeframeSelector.kt       # NEW — Week/Month/Year chips
│   │   └── ShareButton.kt            # NEW — bitmap capture + share
```

### DashboardViewModel

```kotlin
class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    enum class Timeframe { WEEK, MONTH, YEAR }

    data class UiState(
        val timeframe: Timeframe = Timeframe.WEEK,
        val totalFocusMinutes: Int = 0,
        val barData: List<BarEntry> = emptyList(),       // label + value pairs for chart
        val heatmapData: List<HeatmapDay> = emptyList(), // day + intensity for ~6 months
        val isLoading: Boolean = true
    )

    data class BarEntry(val label: String, val minutes: Int)
    data class HeatmapDay(val date: LocalDate, val totalMinutes: Int)

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun onTimeframeSelected(timeframe: Timeframe) { /* reload bar data */ }
}
```

### DashboardScreen Composable

```kotlin
@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SpaceBackground)
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Title
        Text("DASHBOARD", style = PixelTheme.typography.titleMedium)

        Spacer(Modifier.height(16.dp))

        // Timeframe selector: Week · Month · Year
        TimeframeSelector(
            selected = uiState.timeframe,
            onSelected = viewModel::onTimeframeSelected
        )

        Spacer(Modifier.height(24.dp))

        // Total focus time headline
        TotalFocusText(totalMinutes = uiState.totalFocusMinutes)

        Spacer(Modifier.height(24.dp))

        // Bar chart
        FlowBarChart(
            entries = uiState.barData,
            modifier = Modifier.fillMaxWidth().height(200.dp)
        )

        Spacer(Modifier.height(32.dp))

        // Calendar heatmap (scrollable, independent of timeframe)
        CalendarHeatmap(
            days = uiState.heatmapData,
            modifier = Modifier.fillMaxWidth().height(140.dp)
        )

        Spacer(Modifier.height(24.dp))

        // Share button
        ShareButton(onShare = { /* capture & share */ })

        Spacer(Modifier.height(32.dp))
    }
}
```

---

## D. Component Specs

### 1. TimeframeSelector

- Row of three toggle chips: `WEEK`, `MONTH`, `YEAR`.
- Selected chip: `GlowBlue` background, `PixelText` text, 16 dp corner radius.
- Unselected chip: `CardSurface` background, `PixelTextDim` text, `TimerPillBorder` border.
- Pixel font (`Press Start 2P`), `bodyMedium` (12 sp).
- 48 dp min height per chip (accessibility).
- `contentDescription` on each chip: "Show weekly stats" / "Show monthly stats" / "Show yearly stats".

### 2. FlowBarChart (Canvas)

- Compose `Canvas` composable — **no third-party library**.
- X-axis labels: day names (Week), week numbers (Month), month abbreviations (Year).
- Y-axis: implied by bar height, max value auto-scaled. Optional faint horizontal grid lines at 25 %, 50 %, 75 % of max.
- Bar fill: vertical gradient `GlowBlue` → `GlowCyan` (bottom → top).
- Bar width: proportional to available space with 4 dp gaps.
- Corner radius on bar tops: 4 dp.
- Zero-value bars: don't render (or render as a 2 dp dot).
- Tap interaction: `pointerInput` detects tap position → highlights bar, shows a small tooltip `Text` overlay with exact minutes.
- Accessibility: `Modifier.semantics { contentDescription = "Bar chart showing $totalMinutes minutes ..." }`.
- Animation: bars grow from 0 to actual height with `animateFloatAsState` on first composition.

### 3. CalendarHeatmap

- Grid of small squares (12 dp × 12 dp), 2 dp gap.
- 7 rows (Mon–Sun), columns = weeks. ~26 columns for 6 months.
- Horizontally scrollable (`horizontalScroll`), auto-scrolled to the rightmost (current week) on first load.
- Color scale (5 levels based on daily focus minutes):
  - 0 min → `SpaceMid` (#1B2838)
  - 1–15 min → `GlowBlue` at 30 % alpha
  - 16–45 min → `GlowBlue` at 60 % alpha
  - 46–90 min → `GlowCyan` at 85 % alpha
  - 91+ min → `SandGold` at 100 % alpha
- Month labels (`JAN`, `FEB`, ...) above the first column of each month, `labelSmall` (8 sp), `PixelTextDim`.
- Day-of-week labels on the left (single letter: M, T, W, T, F, S, S), `PixelTextDim`, 8 sp.
- Tap a cell → show tooltip with date + total minutes.
- `Modifier.clearAndSetSemantics {}` on the decorative grid; provide a summary `contentDescription` on the container.

### 4. ShareButton

- Rounded pixel-styled button: `GlowBlue` background, `PixelText` text, 16 dp corner radius.
- Label: "SHARE STATS" in `bodyMedium` pixel font.
- Icon: share/export icon to the left (use `Icons.Default.Share` or a custom pixel icon).
- `contentDescription`: "Share your flow stats".
- On tap:
  1. Compose the stats summary + chart into an `ImageBitmap` using a composable capture approach (render to a `Picture` or `AndroidView` with `View.drawToBitmap()`).
  2. Save to app-internal cache via `FileProvider`.
  3. Launch `Intent.ACTION_SEND` with `image/png` MIME type and optional text: "I focused for X hours this week with Flow Time!".
- 48 dp min height.

---

## E. Dependencies

### New: Room

Add to `libs.versions.toml`:

```toml
[versions]
room = "2.6.1"

[libraries]
androidx-room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
androidx-room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
androidx-room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }

[plugins]
ksp = { id = "com.google.devtools.ksp", version = "2.0.21-1.0.28" }
```

Add to `app/build.gradle.kts`:

```kotlin
plugins {
    // ...existing...
    alias(libs.plugins.ksp)
}

dependencies {
    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
}
```

> **No other new dependencies.** Bar chart and heatmap are Canvas-drawn. Share uses platform intents.

---

## F. Milestones

### Milestone D1 — Data Layer (Session History)

#### Step D1.1: Room setup & entity
- **Goal:** Add Room DB with `FlowSession` entity, DAO, and database singleton.
- **Tasks:**
  - Add Room + KSP deps to version catalog and `app/build.gradle.kts`.
  - Create `FlowSession` entity, `FlowSessionDao`, `DailyAggregate` data class.
  - Create `FlowTimerDatabase` with singleton pattern (manual DI, no Hilt).
  - Create `SessionRepository` wrapping the DAO.
- **Files:** `libs.versions.toml`, `app/build.gradle.kts`, `core/data/db/FlowSession.kt`, `core/data/db/FlowSessionDao.kt`, `core/data/db/FlowTimerDatabase.kt`, `core/data/db/DailyAggregate.kt`, `core/data/SessionRepository.kt`.
- **Acceptance:** `./gradlew assembleDebug` succeeds; unit tests for DAO insert + query pass.
- **Complexity:** M

#### Step D1.2: Record sessions on timer completion
- **Goal:** Persist a `FlowSession` row every time the timer finishes.
- **Tasks:**
  - Inject `SessionRepository` into `TimerForegroundService` (manual construction from `applicationContext`).
  - In `onTimerFinished()`, insert a `FlowSession` with start epoch, duration, and completion epoch.
- **Files:** `core/service/TimerForegroundService.kt`.
- **Acceptance:** After completing a flow session, a row appears in the `flow_sessions` table. Verified via unit/integration test.
- **Complexity:** S

### Milestone D2 — Dashboard UI

#### Step D2.1: HorizontalPager navigation
- **Goal:** Swipe right from HomeScreen to reach DashboardScreen.
- **Tasks:**
  - Wrap `HomeScreen` and `DashboardScreen` (stub) in a `HorizontalPager` inside `MainActivity`.
  - Pager starts on page 1 (Home). Swiping right goes to page 0 (Dashboard).
  - Add `foundation` dependency for `HorizontalPager` (part of Compose BOM, already available).
  - Optional: subtle dot indicator at the top.
- **Files:** `MainActivity.kt`, `ui/dashboard/DashboardScreen.kt` (stub).
- **Acceptance:** Swiping right on the home screen reveals the dashboard stub; swiping left returns to home.
- **Complexity:** S

#### Step D2.2: DashboardViewModel + TimeframeSelector
- **Goal:** ViewModel that queries `SessionRepository` and aggregates data for Week/Month/Year.
- **Tasks:**
  - Create `DashboardViewModel` with `Timeframe` enum and `UiState`.
  - Compute date ranges for week (Mon–Sun), month (1st–last), year (Jan 1–Dec 31) using `java.time` APIs.
  - Query `dailyAggregates()` for bar chart; query full 6-month range for heatmap.
  - Sum `totalFocusMinutes` from aggregates.
  - Create `TimeframeSelector` composable (three chips).
- **Files:** `ui/dashboard/DashboardViewModel.kt`, `ui/components/TimeframeSelector.kt`.
- **Acceptance:** Switching timeframes reloads data; unit tests for date range computation pass.
- **Complexity:** M

#### Step D2.3: FlowBarChart composable
- **Goal:** Canvas-drawn bar chart matching the pixel aesthetic.
- **Tasks:**
  - `FlowBarChart` composable accepting `List<BarEntry>` data.
  - Draw bars with gradient fill, corner radius, grid lines.
  - X-axis labels, auto-scaling Y-axis.
  - Tap-to-highlight with tooltip.
  - Entry animation (`animateFloatAsState`).
  - `@Preview` for each timeframe with sample data.
- **Files:** `ui/components/FlowBarChart.kt`.
- **Acceptance:** Previews render correctly; bars animate on appearance; TalkBack reads chart summary.
- **Complexity:** L

#### Step D2.4: CalendarHeatmap composable
- **Goal:** GitHub-style scrollable heatmap grid.
- **Tasks:**
  - `CalendarHeatmap` composable accepting `List<HeatmapDay>` data.
  - Grid: 7 rows × N columns (weeks), 12 dp cells, 2 dp gap.
  - 5-level color scale (see Component Specs above).
  - Month labels and day-of-week labels.
  - Horizontal scroll, auto-scroll to current week.
  - Tap cell → tooltip with date + minutes.
  - `@Preview` with sample data.
- **Files:** `ui/components/CalendarHeatmap.kt`.
- **Acceptance:** Previews render; scrollable in both preview and device; accessibility annotations present.
- **Complexity:** L

#### Step D2.5: DashboardScreen assembly
- **Goal:** Wire all components into the full `DashboardScreen`.
- **Tasks:**
  - Compose `DashboardScreen` assembling: title → `TimeframeSelector` → total focus text → `FlowBarChart` → `CalendarHeatmap` → `ShareButton`.
  - Vertical scroll for the whole screen.
  - Connect to `DashboardViewModel`.
  - `@Preview` for the complete screen (with mock data).
- **Files:** `ui/dashboard/DashboardScreen.kt`.
- **Acceptance:** Full screen renders with all sections; timeframe switching works; scroll works.
- **Complexity:** M

#### Step D2.6: ShareButton + bitmap capture
- **Goal:** Capture the stats section as an image and trigger system share sheet.
- **Tasks:**
  - Create `ShareButton` composable.
  - Implement bitmap capture of the chart + stats area (use `graphicsLayer` + `toImageBitmap()` or `AndroidView` approach).
  - Save bitmap to app cache directory via `FileProvider`.
  - Launch `Intent.ACTION_SEND` with the image URI.
  - Add `FileProvider` config to `AndroidManifest.xml` if not already present.
  - Share text: "I focused for X hours this week with Flow Time! 🔥".
- **Files:** `ui/components/ShareButton.kt`, `AndroidManifest.xml`, `res/xml/file_paths.xml`.
- **Acceptance:** Tapping share opens the system share sheet with the image; works on API 29+.
- **Complexity:** M

### Milestone D3 — Testing & Polish

#### Step D3.1: Data layer tests
- **Goal:** Unit tests for Room DAO and SessionRepository.
- **Tasks:**
  - In-memory Room database tests for insert, query by range, daily aggregation.
  - Verify date grouping works correctly across timezones.
  - Test `DashboardViewModel` date range calculations.
- **Files:** `src/test/java/com/ashutosh/flowtimer/core/data/db/FlowSessionDaoTest.kt`, `src/test/java/com/ashutosh/flowtimer/ui/dashboard/DashboardViewModelTest.kt`.
- **Acceptance:** All data layer tests pass; edge cases (empty data, single session, midnight boundary) covered.
- **Complexity:** M

#### Step D3.2: UI tests & accessibility
- **Goal:** Compose UI tests for dashboard components + accessibility pass.
- **Tasks:**
  - `createComposeRule()` tests for `TimeframeSelector`, `FlowBarChart`, `CalendarHeatmap`, `DashboardScreen`.
  - Verify `contentDescription` on all interactive elements.
  - Touch targets ≥ 48 dp.
  - TalkBack walkthrough: navigate from home → dashboard → select timeframe → hear chart summary.
  - Test with 200 % font scale.
- **Files:** `src/androidTest/java/com/ashutosh/flowtimer/ui/dashboard/DashboardScreenTest.kt`.
- **Acceptance:** All UI tests green; TalkBack walkthrough documented; no contrast violations.
- **Complexity:** M

---

## G. Visual Design Reference

### Color Usage in Dashboard

| Element | Color Token | Notes |
|---|---|---|
| Background | `SpaceBackground` | Same as home screen |
| Title text | `PixelText` | "DASHBOARD" |
| Total focus number | `PixelText` | Bold, large |
| Total focus unit | `PixelTextDim` | "hours", "min" |
| Active chip | `GlowBlue` bg + `PixelText` text | |
| Inactive chip | `CardSurface` bg + `PixelTextDim` text + `TimerPillBorder` border | |
| Bar fill | `GlowBlue` → `GlowCyan` gradient | Bottom → top |
| Bar chart grid lines | `SpaceMid` at 30 % alpha | Subtle |
| Bar chart labels | `PixelTextDim` | 8 sp pixel font |
| Heatmap cells | `SpaceMid` → `GlowBlue` → `GlowCyan` → `SandGold` | 5-level scale |
| Heatmap labels | `PixelTextDim` | 8 sp |
| Share button | `GlowBlue` bg + `PixelText` text | 16 dp radius |
| Page indicator dots | `GlowBlue` (active), `PixelTextDim` (inactive) | Small 6 dp circles |

### Spacing

- Screen horizontal padding: 16 dp
- Section spacing: 24–32 dp
- Chip internal padding: 12 dp horizontal, 8 dp vertical
- Chart height: 200 dp (bar chart), 140 dp (heatmap)
- All values are multiples of 4 dp

---

## H. Open Questions

| # | Item | Assumption | Status |
|---|---|---|---|
| 1 | Room vs. DataStore for history | Room — relational queries (GROUP BY, date ranges) are essential for aggregation. DataStore is not suitable for list data. | Decided |
| 2 | Chart library | Canvas-drawn, no third-party dep. Keeps APK lean and matches pixel aesthetic. | Decided |
| 3 | Heatmap timespan | 6 months rolling. Could be extended to 1 year later. | Assumed |
| 4 | Share image quality | PNG, screen density resolution. | Assumed |
| 5 | Migration from counter-only to Room | Both systems coexist. Counter in DataStore is kept for the widget's quick read. Room is authoritative for dashboard. Existing sessions before this feature are lost (no backfill). | Accepted |
| 6 | Timezone handling | Dates grouped by device local timezone (`ZoneId.systemDefault()`). | Assumed |