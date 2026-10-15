package dev.folio.reader.data

import dev.folio.reader.R
import dev.folio.reader.s

/** What to show when Rukn opens: the first-install tour, the one-time notes after an update, or nothing. */
enum class Welcome { Tour, WhatsNew, Nothing }

object Onboarding {
    /** `0.12.0` > `0.11.2`; missing or non-numeric parts count as 0. */
    fun compare(a: String, b: String): Int {
        val x = a.split('.').map { it.toIntOrNull() ?: 0 }
        val y = b.split('.').map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(x.size, y.size)) {
            val d = x.getOrElse(i) { 0 }.compareTo(y.getOrElse(i) { 0 })
            if (d != 0) return d
        }
        return 0
    }

    /**
     * [lastSeen] is the version whose notes were closed last (empty when never), [tourDone] whether the tour was
     * finished or skipped, [hasData] whether the library already holds anything (an update from a version that
     * did not store these two values), [current] the installed version.
     */
    fun decide(lastSeen: String, tourDone: Boolean, hasData: Boolean, current: String): Welcome = when {
        !tourDone && lastSeen.isEmpty() && !hasData -> Welcome.Tour
        lastSeen.isNotEmpty() && compare(current, lastSeen) <= 0 -> Welcome.Nothing // same version, or a downgrade
        else -> Welcome.WhatsNew
    }
}

/** Short notes per version, taken from the changelog; shown once after an update. */
object ReleaseNotes {
    private val notes: Map<String, List<String>> get() = mapOf(
        "0.14.1" to listOf(s(R.string.new_family_icon_note)),
        "0.14.0" to listOf(
            s(R.string.manhal_rename_note),
            s(R.string.arabic_note),
            s(R.string.new_icon_note),
        ),
        "0.13.1" to listOf(
            s(R.string.a_theme_setting_system_light),
            s(R.string.settings_opens_at_once_backups),
        ),
        "0.13.0" to listOf(
            s(R.string.a_loading_screen_while_a),
            s(R.string.book_details_start_with_your),
            s(R.string.every_clear_delete_or_remove),
        ),
        "0.12.1" to listOf(
            s(R.string.moving_your_library_into_the),
            s(R.string.the_new_rukn_icon),
        ),
        "0.12.0" to listOf(
            s(R.string.a_first_run_tour_and),
            s(R.string.your_data_now_lives_in),
            s(R.string.a_better_opening_screen_and),
        ),
        "0.11.2" to listOf(s(R.string.zoomed_pages_scroll)),
        "0.11.1" to listOf(s(R.string.double_tap_zooms_out)),
        "0.11.0" to listOf(
            s(R.string.edit_pdfs_highlighter_pen_underline),
            s(R.string.save_as_new_overwrite_with),
        ),
    )

    /** The notes for [version], or for the newest older version that has some. */
    fun forVersion(version: String): List<String> =
        notes[version] ?: notes.keys.filter { Onboarding.compare(it, version) <= 0 }.maxWithOrNull { a, b -> Onboarding.compare(a, b) }?.let { notes[it] }.orEmpty()
}
