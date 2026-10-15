package dev.manhal.reader.data

import android.content.Context
import androidx.annotation.StringRes
import dev.manhal.reader.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Accent palettes. Each is a few tokens; screens never hard-code a colour. Orange is the Zimam default. */
enum class ColourTheme(@StringRes val labelRes: Int, val accent: Long, val containerLight: Long, val onContainerLight: Long, val containerDark: Long, val onContainerDark: Long) {
    Orange(R.string.colour_orange, 0xFFFF5A1F, 0xFFFFDDD0, 0xFF3A0E00, 0xFF4A1D0B, 0xFFFFDDD0),
    Teal(R.string.colour_teal, 0xFF0B7A75, 0xFFC9ECE8, 0xFF00201E, 0xFF0F3F3C, 0xFFC9ECE8),
    Indigo(R.string.colour_indigo, 0xFF4F5BD5, 0xFFDDE0FF, 0xFF0A1050, 0xFF262C6B, 0xFFDDE0FF),
    Rose(R.string.colour_rose, 0xFFC2255C, 0xFFFFD9E3, 0xFF3F0019, 0xFF5A1230, 0xFFFFD9E3),
    Green(R.string.colour_green, 0xFF2B8A3E, 0xFFD3F0D8, 0xFF002108, 0xFF16401E, 0xFFD3F0D8),
    Brown(R.string.colour_brown, 0xFF8B5E34, 0xFFF2DFCB, 0xFF2D1600, 0xFF45301C, 0xFFF2DFCB);

    companion object { fun parse(text: String?): ColourTheme = entries.firstOrNull { it.name == text } ?: Orange }
}

/** The four text sizes; applies to the whole app on top of the phone's own font size. */
enum class TextSize(@StringRes val labelRes: Int, val scale: Float) {
    Small(R.string.text_small, 0.9f), Default(R.string.text_default, 1f), Large(R.string.text_large, 1.15f), Larger(R.string.text_larger, 1.3f);

    companion object { fun parse(text: String?): TextSize = entries.firstOrNull { it.name == text } ?: Default }
}

/** How much room rows and cards get. */
enum class Density(@StringRes val labelRes: Int, val rowPaddingDp: Int, val gapDp: Int, val listGapDp: Int) {
    Comfortable(R.string.density_comfortable, 12, 14, 6), Compact(R.string.density_compact, 6, 8, 2);

    companion object { fun parse(text: String?): Density = entries.firstOrNull { it.name == text } ?: Comfortable }
}

/** Every app-level choice in one place. (Library-level choices such as the home tabs live in the library file so they travel with its backups.) */
data class AppSettings(
    val theme: AppTheme = AppTheme.System,
    val colour: ColourTheme = ColourTheme.Orange,
    val textSize: TextSize = TextSize.Default,
    val density: Density = Density.Comfortable,
    val reduceMotion: Boolean = false,
    val language: AppLanguage = AppLanguage.System,
    val arabicDigits: Boolean = false,
)

/** Where the settings are kept: SharedPreferences on the phone (loaded before the first frame), a map in tests. */
interface SettingsStore {
    fun getString(key: String): String?
    fun getBoolean(key: String, default: Boolean): Boolean
    fun putString(key: String, value: String)
    fun putBoolean(key: String, value: Boolean)
}

class SharedPrefsStore(context: Context) : SettingsStore {
    private val prefs = context.getSharedPreferences(LegacyNames.PREFS_FILE, Context.MODE_PRIVATE)
    override fun getString(key: String) = prefs.getString(key, null)
    override fun getBoolean(key: String, default: Boolean) = prefs.getBoolean(key, default)
    override fun putString(key: String, value: String) { prefs.edit().putString(key, value).apply() }
    override fun putBoolean(key: String, value: Boolean) { prefs.edit().putBoolean(key, value).apply() }
}

/** The one place settings are read and written. The keys of the earlier theme and language settings are kept, so nothing is lost on update. */
class SettingsRepository(private val store: SettingsStore) {
    private val _state = MutableStateFlow(read())
    val state: StateFlow<AppSettings> = _state
    val value: AppSettings get() = _state.value

    private fun read() = AppSettings(
        theme = AppTheme.parse(store.getString(THEME)),
        colour = ColourTheme.parse(store.getString(COLOUR)),
        textSize = TextSize.parse(store.getString(TEXT_SIZE)),
        density = Density.parse(store.getString(DENSITY)),
        reduceMotion = store.getBoolean(REDUCE_MOTION, false),
        language = AppLanguage.parse(store.getString(LANGUAGE)),
        arabicDigits = store.getBoolean(ARABIC_DIGITS, false),
    )

    fun update(change: (AppSettings) -> AppSettings) {
        val old = _state.value
        val new = change(old)
        if (new == old) return
        if (new.theme != old.theme) store.putString(THEME, new.theme.name)
        if (new.colour != old.colour) store.putString(COLOUR, new.colour.name)
        if (new.textSize != old.textSize) store.putString(TEXT_SIZE, new.textSize.name)
        if (new.density != old.density) store.putString(DENSITY, new.density.name)
        if (new.reduceMotion != old.reduceMotion) store.putBoolean(REDUCE_MOTION, new.reduceMotion)
        if (new.language != old.language) store.putString(LANGUAGE, new.language.name)
        if (new.arabicDigits != old.arabicDigits) store.putBoolean(ARABIC_DIGITS, new.arabicDigits)
        _state.value = new
    }

    companion object {
        const val THEME = "app_theme"
        const val COLOUR = "colour_theme"
        const val TEXT_SIZE = "text_size"
        const val DENSITY = "density"
        const val REDUCE_MOTION = "reduce_motion"
        const val LANGUAGE = "app_language"
        const val ARABIC_DIGITS = "arabic_digits"

        @Volatile private var shared: SettingsRepository? = null

        /** One repository for the whole process, so the activity, the view model and the screens see the same state. */
        fun of(context: Context): SettingsRepository = shared ?: synchronized(this) {
            shared ?: SettingsRepository(SharedPrefsStore(context.applicationContext)).also { shared = it }
        }
    }
}

/** WCAG contrast ratio between two ARGB colours; used to keep every palette readable. */
object Contrast {
    private fun channel(v: Int): Double { val c = v / 255.0; return if (c <= 0.03928) c / 12.92 else Math.pow((c + 0.055) / 1.055, 2.4) }
    fun luminance(argb: Long): Double = 0.2126 * channel(((argb shr 16) and 0xFF).toInt()) + 0.7152 * channel(((argb shr 8) and 0xFF).toInt()) + 0.0722 * channel((argb and 0xFF).toInt())
    fun ratio(a: Long, b: Long): Double { val x = luminance(a); val y = luminance(b); return (maxOf(x, y) + 0.05) / (minOf(x, y) + 0.05) }
}
