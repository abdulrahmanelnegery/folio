package dev.manhal.reader.render

import android.app.Application
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import dev.manhal.reader.ManhalViewModel
import dev.manhal.reader.Strings
import dev.manhal.reader.data.Book
import dev.manhal.reader.data.DayStat
import dev.manhal.reader.data.HomeTab
import dev.manhal.reader.data.HomeTabConfig
import dev.manhal.reader.data.Kind
import dev.manhal.reader.data.ReadSession
import dev.manhal.reader.data.Status
import dev.manhal.reader.data.Tab
import dev.manhal.reader.ui.LibraryScreen
import dev.manhal.reader.ui.LocalClockHour
import dev.manhal.reader.ui.ManhalTheme
import dev.manhal.reader.ui.SettingsScreen
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Reference screenshots of the main screens at the normal and at 130 percent font scale, to catch overflow and crowding.
 * Update the images with `./gradlew :app:recordRoborazziDebug`; CI runs `:app:verifyRoborazziDebug`.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = RobolectricDeviceQualifiers.Pixel5)
class ScreenshotTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var model: ManhalViewModel
    private val options = RoborazziOptions(compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.02f))

    @Before fun setUp() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        Strings.res = app.resources
        model = ManhalViewModel(app)
        val day = 24 * 3600 * 1000L
        val t0 = 1_760_000_000_000L
        listOf(
            Book("1", "u1", "The Count of Monte Cristo", kind = Kind.Pdf, pages = 1200, page = 410, status = Status.Reading, lastOpened = t0, added = t0, readSeconds = 7200, pagesRead = 80),
            Book("2", "u2", "كليلة ودمنة", kind = Kind.Epub, pages = 30, page = 3, status = Status.Reading, lastOpened = t0 - day, added = t0, favorite = true, rating = 4),
            Book("3", "u3", "Notes on Systems", kind = Kind.Text, pages = 200, status = Status.ToRead, added = t0),
            Book("4", "u4", "A Short History of Nearly Everything", kind = Kind.Pdf, pages = 600, page = 600, status = Status.Finished, lastOpened = t0 - 3 * day, added = t0, rating = 5),
        ).forEach(model.repo::add)
        model.repo.update {
            it.copy(
                sessions = listOf(ReadSession("1", t0, 1800, 20), ReadSession("2", t0 - day, 900, 3)),
                days = mapOf("2025-10-09" to DayStat(1800, 20), "2025-10-08" to DayStat(900, 3)),
                homeTabs = listOf(HomeTabConfig(HomeTab.Home), HomeTabConfig(HomeTab.Stats), HomeTabConfig(HomeTab.Library)),
                tourDone = true,
                hiddenNews = app.packageManager.getPackageInfo(app.packageName, 0).versionName.orEmpty(),
            )
        }
    }

    @After fun tearDown() { Strings.res = null }

    private fun show(fontScale: Float, content: @androidx.compose.runtime.Composable () -> Unit) = compose.setContent {
        val base = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(base.density, fontScale), LocalClockHour provides 9) { ManhalTheme { content() } }
    }

    private fun library() = @androidx.compose.runtime.Composable { LibraryScreen(model, onOpenPicker = {}, onSettings = {}, onAbout = {}, onBrowse = { _, _ -> }) }

    private fun shot(name: String) {
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("src/test/screenshots/$name.png", options)
    }

    private fun home(scale: Float, tab: String?, name: String) {
        show(scale, library())
        if (tab != null) { compose.onAllNodesWithText(tab).onFirst().performClick(); compose.waitForIdle() }
        shot(name)
    }

    @Test fun homeNormal() = home(1f, null, "home_normal")
    @Test fun homeLargeFont() = home(1.3f, null, "home_font130")
    @Test fun statsNormal() = home(1f, "Stats", "stats_normal")
    @Test fun statsLargeFont() = home(1.3f, "Stats", "stats_font130")
    @Test fun libraryTabNormal() = home(1f, "Library", "librarytab_normal")
    @Test fun libraryTabLargeFont() = home(1.3f, "Library", "librarytab_font130")

    @Test fun allBooksNormal() { model.libraryTab = Tab.All; show(1f, library()); shot("allbooks_normal") }
    @Test fun allBooksLargeFont() { model.libraryTab = Tab.All; show(1.3f, library()); shot("allbooks_font130") }

    @Test fun settingsNormal() { show(1f) { SettingsScreen(model, onBack = {}) }; shot("settings_normal") }
    @Test fun settingsLargeFont() { show(1.3f) { SettingsScreen(model, onBack = {}) }; shot("settings_font130") }
}
