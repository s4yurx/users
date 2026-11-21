package com.example.ui.theme

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.example.R
import com.example.widget.DuetLoveWidgetProvider
import com.example.widget.DuetQuickChatWidgetProvider
import com.example.widget.DuetThemeWidgetProvider
import com.example.widget.WidgetStateStore
import com.example.widget.WidgetThemeStyle
import java.io.File
import java.io.FileOutputStream
import kotlin.math.min
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

enum class AppThemePreset(
    val key: String,
    val label: String,
    val shortLabel: String,
    val emoji: String,
    val isDark: Boolean,
    val colorScheme: ColorScheme,
    val heroStart: Color,
    val heroEnd: Color,
    val bubbleAccentA: Color,
    val bubbleAccentB: Color,
    val swatchPrimary: Color,
    val swatchSecondary: Color,
    @DrawableRes val widgetBgRes: Int,
    val widgetTitleHex: String,
    val widgetBodyHex: String,
    val widgetSubHex: String,
    val widgetChipTextHex: String,
    val isExclusive: Boolean = false,
    val exclusiveAccent: Color = Color(0xFFFFD875),
    val tagline: String = "",
    val heroMid: Color? = null
) {
    BLUSH_ROSE(
        key = "ROSE",
        label = "Blush Rose",
        shortLabel = "Rose",
        emoji = "🌸",
        isDark = false,
        colorScheme = lightColorScheme(
            primary = RosePrimary,
            onPrimary = OnRosePrimary,
            primaryContainer = RosePrimaryContainer,
            onPrimaryContainer = OnRosePrimaryContainer,
            secondary = TerracottaSecondary,
            onSecondary = OnTerracottaSecondary,
            secondaryContainer = TerracottaSecondaryContainer,
            onSecondaryContainer = OnTerracottaSecondaryContainer,
            tertiary = RoseGoldTertiary,
            onTertiary = OnRoseGoldTertiary,
            tertiaryContainer = RoseGoldTertiaryContainer,
            onTertiaryContainer = OnRoseGoldTertiaryContainer,
            background = WarmParchmentBackground,
            onBackground = OnWarmParchment,
            surface = WarmBlushSurface,
            onSurface = OnWarmParchment,
            surfaceVariant = WarmBlushSurfaceVariant,
            onSurfaceVariant = OnWarmBlushSurfaceVariant,
            outline = WarmOutline,
            outlineVariant = WarmOutlineVariant
        ),
        heroStart = DeepVelvetCard,
        heroEnd = DeepVelvetCardLighter,
        bubbleAccentA = Color(0xFFFF8BA7),
        bubbleAccentB = Color(0xFFFFC8A2),
        swatchPrimary = Color(0xFFA83E53),
        swatchSecondary = Color(0xFFFFD9DF),
        widgetBgRes = R.drawable.bg_widget_duet_rose,
        widgetTitleHex = "#3A1624",
        widgetBodyHex = "#622B3E",
        widgetSubHex = "#855262",
        widgetChipTextHex = "#8E2D45"
    ),
    VELVET_DUSK(
        key = "VELVET",
        label = "Velvet Dusk",
        shortLabel = "Velvet",
        emoji = "🍷",
        isDark = true,
        colorScheme = darkColorScheme(
            primary = VelvetRosePrimary,
            onPrimary = OnVelvetRosePrimary,
            primaryContainer = VelvetRoseContainer,
            onPrimaryContainer = OnVelvetRoseContainer,
            secondary = VelvetTerracottaSecondary,
            onSecondary = OnVelvetTerracottaSecondary,
            secondaryContainer = VelvetTerracottaContainer,
            onSecondaryContainer = OnVelvetTerracottaContainer,
            tertiary = VelvetGoldTertiary,
            onTertiary = OnVelvetGoldTertiary,
            tertiaryContainer = VelvetGoldContainer,
            onTertiaryContainer = OnVelvetGoldContainer,
            background = VelvetPlumBackground,
            onBackground = OnVelvetPlum,
            surface = VelvetPlumSurface,
            onSurface = OnVelvetPlum,
            surfaceVariant = VelvetPlumSurfaceVariant,
            onSurfaceVariant = OnVelvetPlumSurfaceVariant,
            outline = VelvetOutline,
            outlineVariant = VelvetOutlineVariant
        ),
        heroStart = Color(0xFF3E182A),
        heroEnd = Color(0xFF6B2542),
        bubbleAccentA = Color(0xFFF2A2B4),
        bubbleAccentB = Color(0xFFF3C697),
        swatchPrimary = Color(0xFF4E2034),
        swatchSecondary = Color(0xFFF2A2B4),
        widgetBgRes = R.drawable.bg_widget_duet_velvet,
        widgetTitleHex = "#FFF2F4",
        widgetBodyHex = "#F3C4D0",
        widgetSubHex = "#D5A4B3",
        widgetChipTextHex = "#FFD9DF"
    ),
    MIDNIGHT_OCEAN(
        key = "MIDNIGHT",
        label = "Midnight Ocean",
        shortLabel = "Ocean",
        emoji = "🌊",
        isDark = true,
        colorScheme = darkColorScheme(
            primary = Color(0xFF76D1FF),
            onPrimary = Color(0xFF003549),
            primaryContainer = Color(0xFF0B4E6B),
            onPrimaryContainer = Color(0xFFC2EBFF),
            secondary = Color(0xFFA7C8FF),
            onSecondary = Color(0xFF0D2F5E),
            secondaryContainer = Color(0xFF21467B),
            onSecondaryContainer = Color(0xFFD7E5FF),
            tertiary = Color(0xFF9BEAD6),
            onTertiary = Color(0xFF00382D),
            tertiaryContainer = Color(0xFF0F5244),
            onTertiaryContainer = Color(0xFFBFFCEB),
            background = Color(0xFF091322),
            onBackground = Color(0xFFE6F1FC),
            surface = Color(0xFF101D33),
            onSurface = Color(0xFFE6F1FC),
            surfaceVariant = Color(0xFF1B2E4C),
            onSurfaceVariant = Color(0xFFB5C9E2),
            outline = Color(0xFF7D95B5),
            outlineVariant = Color(0xFF2B4163)
        ),
        heroStart = Color(0xFF0E294D),
        heroEnd = Color(0xFF1D4F7A),
        bubbleAccentA = Color(0xFF67D4FF),
        bubbleAccentB = Color(0xFFA7B8FF),
        swatchPrimary = Color(0xFF15365F),
        swatchSecondary = Color(0xFF76D1FF),
        widgetBgRes = R.drawable.bg_widget_duet_midnight,
        widgetTitleHex = "#ECF7FF",
        widgetBodyHex = "#BBE4FF",
        widgetSubHex = "#95BFE0",
        widgetChipTextHex = "#E4F5FF"
    ),
    LAVENDER_DREAM(
        key = "LAVENDER",
        label = "Lavender Dream",
        shortLabel = "Lilac",
        emoji = "💜",
        isDark = false,
        colorScheme = lightColorScheme(
            primary = Color(0xFF6D49B5),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFEBDFFF),
            onPrimaryContainer = Color(0xFF25065E),
            secondary = Color(0xFF964F86),
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFFFFD8F0),
            onSecondaryContainer = Color(0xFF3A0832),
            tertiary = Color(0xFF4F629E),
            onTertiary = Color(0xFFFFFFFF),
            tertiaryContainer = Color(0xFFDCE3FF),
            onTertiaryContainer = Color(0xFF0B1F56),
            background = Color(0xFFFAF6FF),
            onBackground = Color(0xFF1F162C),
            surface = Color(0xFFFAF6FF),
            onSurface = Color(0xFF1F162C),
            surfaceVariant = Color(0xFFEFE6FA),
            onSurfaceVariant = Color(0xFF4D4061),
            outline = Color(0xFF817098),
            outlineVariant = Color(0xFFD8CAEB)
        ),
        heroStart = Color(0xFF321E5A),
        heroEnd = Color(0xFF5A3791),
        bubbleAccentA = Color(0xFFB58CFF),
        bubbleAccentB = Color(0xFFFF9BD2),
        swatchPrimary = Color(0xFF6D49B5),
        swatchSecondary = Color(0xFFEBDFFF),
        widgetBgRes = R.drawable.bg_widget_duet_lavender,
        widgetTitleHex = "#2A164B",
        widgetBodyHex = "#4E2E7E",
        widgetSubHex = "#6D5196",
        widgetChipTextHex = "#5A3299"
    ),
    SUNSET_PEACH(
        key = "PEACH",
        label = "Sunset Peach",
        shortLabel = "Peach",
        emoji = "🍑",
        isDark = false,
        colorScheme = lightColorScheme(
            primary = Color(0xFFC4552B),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFFFDCCE),
            onPrimaryContainer = Color(0xFF3E1200),
            secondary = Color(0xFF9C5E35),
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFFFFE0C9),
            onSecondaryContainer = Color(0xFF381A04),
            tertiary = Color(0xFFA84C64),
            onTertiary = Color(0xFFFFFFFF),
            tertiaryContainer = Color(0xFFFFD9E1),
            onTertiaryContainer = Color(0xFF3E071B),
            background = Color(0xFFFFF9F2),
            onBackground = Color(0xFF281810),
            surface = Color(0xFFFFF9F2),
            onSurface = Color(0xFF281810),
            surfaceVariant = Color(0xFFF9E9DC),
            onSurfaceVariant = Color(0xFF5D4437),
            outline = Color(0xFF917262),
            outlineVariant = Color(0xFFE7CEBE)
        ),
        heroStart = Color(0xFF4E2212),
        heroEnd = Color(0xFF8E3F24),
        bubbleAccentA = Color(0xFFFFA573),
        bubbleAccentB = Color(0xFFFFD384),
        swatchPrimary = Color(0xFFC4552B),
        swatchSecondary = Color(0xFFFFDCCE),
        widgetBgRes = R.drawable.bg_widget_duet_peach,
        widgetTitleHex = "#3E2114",
        widgetBodyHex = "#6B3A24",
        widgetSubHex = "#8A5B44",
        widgetChipTextHex = "#7D3F1E"
    ),
    EMERALD_FOREST(
        key = "EMERALD",
        label = "Emerald Night",
        shortLabel = "Forest",
        emoji = "🌿",
        isDark = true,
        colorScheme = darkColorScheme(
            primary = Color(0xFF6CD9B4),
            onPrimary = Color(0xFF003828),
            primaryContainer = Color(0xFF0C523D),
            onPrimaryContainer = Color(0xFFB5F7DF),
            secondary = Color(0xFFA6D5C0),
            onSecondary = Color(0xFF0E3728),
            secondaryContainer = Color(0xFF234E3D),
            onSecondaryContainer = Color(0xFFC5F2DD),
            tertiary = Color(0xFFE9C98E),
            onTertiary = Color(0xFF422C04),
            tertiaryContainer = Color(0xFF5D4215),
            onTertiaryContainer = Color(0xFFFFE5B5),
            background = Color(0xFF0A1714),
            onBackground = Color(0xFFE4F4EE),
            surface = Color(0xFF10221D),
            onSurface = Color(0xFFE4F4EE),
            surfaceVariant = Color(0xFF1B342D),
            onSurfaceVariant = Color(0xFFB3CFC4),
            outline = Color(0xFF7D9A8F),
            outlineVariant = Color(0xFF2D4B42)
        ),
        heroStart = Color(0xFF0D3329),
        heroEnd = Color(0xFF1B5745),
        bubbleAccentA = Color(0xFF6CD9B4),
        bubbleAccentB = Color(0xFFA8E6CF),
        swatchPrimary = Color(0xFF154738),
        swatchSecondary = Color(0xFF6CD9B4),
        widgetBgRes = R.drawable.bg_widget_duet_emerald,
        widgetTitleHex = "#EAFFF7",
        widgetBodyHex = "#B7EEDB",
        widgetSubHex = "#8EC8B5",
        widgetChipTextHex = "#DCFFF3"
    ),
    KYOTO_SAKURA(
        key = "SAKURA",
        label = "Kyoto Sakura",
        shortLabel = "Sakura",
        emoji = "🌺",
        isDark = false,
        colorScheme = lightColorScheme(
            primary = Color(0xFFCC4478),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFFFD9E7),
            onPrimaryContainer = Color(0xFF3E001F),
            secondary = Color(0xFF8F4B63),
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFFFFDCE6),
            onSecondaryContainer = Color(0xFF3B081F),
            tertiary = Color(0xFFAA6048),
            onTertiary = Color(0xFFFFFFFF),
            tertiaryContainer = Color(0xFFFFDFD5),
            onTertiaryContainer = Color(0xFF3D1609),
            background = Color(0xFFFFF7FA),
            onBackground = Color(0xFF2B141D),
            surface = Color(0xFFFFF7FA),
            onSurface = Color(0xFF2B141D),
            surfaceVariant = Color(0xFFFCE8F0),
            onSurfaceVariant = Color(0xFF603B4A),
            outline = Color(0xFF996C7E),
            outlineVariant = Color(0xFFEAC8D6)
        ),
        heroStart = Color(0xFF5E1E3B),
        heroEnd = Color(0xFF9E3964),
        bubbleAccentA = Color(0xFFFF9EC4),
        bubbleAccentB = Color(0xFFFFD1E2),
        swatchPrimary = Color(0xFFCC4478),
        swatchSecondary = Color(0xFFFFD9E7),
        widgetBgRes = R.drawable.bg_widget_duet_sakura,
        widgetTitleHex = "#421528",
        widgetBodyHex = "#702847",
        widgetSubHex = "#8F4E69",
        widgetChipTextHex = "#9C2252"
    ),
    ROYAL_OBSIDIAN(
        key = "ROYAL_GOLD",
        label = "Royal Obsidian",
        shortLabel = "Royal Gold",
        emoji = "👑",
        isDark = true,
        colorScheme = darkColorScheme(
            primary = Color(0xFFF5CA6E),
            onPrimary = Color(0xFF221600),
            primaryContainer = Color(0xFF47330B),
            onPrimaryContainer = Color(0xFFFFEAB5),
            secondary = Color(0xFFE3B983),
            onSecondary = Color(0xFF2A1802),
            secondaryContainer = Color(0xFF362612),
            onSecondaryContainer = Color(0xFFFCE2BD),
            tertiary = Color(0xFFFFB2C2),
            onTertiary = Color(0xFF3B0E1A),
            tertiaryContainer = Color(0xFF531B2A),
            onTertiaryContainer = Color(0xFFFFD9E1),
            background = Color(0xFF070709),
            onBackground = Color(0xFFFAF3E3),
            surface = Color(0xFF121116),
            onSurface = Color(0xFFFAF3E3),
            surfaceVariant = Color(0xFF211C15),
            onSurfaceVariant = Color(0xFFD6C7A9),
            outline = Color(0xFFA28A5B),
            outlineVariant = Color(0xFF3E3320)
        ),
        heroStart = Color(0xFF191208),
        heroEnd = Color(0xFF4A3410),
        bubbleAccentA = Color(0xFFFFD97D),
        bubbleAccentB = Color(0xFFC89B3C),
        swatchPrimary = Color(0xFF15120C),
        swatchSecondary = Color(0xFFF5CA6E),
        widgetBgRes = R.drawable.bg_widget_duet_royal_gold,
        widgetTitleHex = "#FFF2CF",
        widgetBodyHex = "#F4D07A",
        widgetSubHex = "#CBB17A",
        widgetChipTextHex = "#FFE59D",
        isExclusive = true,
        exclusiveAccent = Color(0xFFFFD86B),
        tagline = "24K Obsidian & Liquid Gold",
        heroMid = Color(0xFF33230C)
    ),
    AURORA_CYBER(
        key = "AURORA",
        label = "Aurora Cyber",
        shortLabel = "Aurora",
        emoji = "🌌",
        isDark = true,
        colorScheme = darkColorScheme(
            primary = Color(0xFF4DF3FF),
            onPrimary = Color(0xFF002B33),
            primaryContainer = Color(0xFF084255),
            onPrimaryContainer = Color(0xFFC2FAFF),
            secondary = Color(0xFFF66CFF),
            onSecondary = Color(0xFF3B0042),
            secondaryContainer = Color(0xFF4A125C),
            onSecondaryContainer = Color(0xFFFBD2FF),
            tertiary = Color(0xFF8CFFBA),
            onTertiary = Color(0xFF00381C),
            tertiaryContainer = Color(0xFF0C4F31),
            onTertiaryContainer = Color(0xFFC6FFDD),
            background = Color(0xFF060918),
            onBackground = Color(0xFFEAF7FF),
            surface = Color(0xFF0E142D),
            onSurface = Color(0xFFEAF7FF),
            surfaceVariant = Color(0xFF192246),
            onSurfaceVariant = Color(0xFFB7CAE8),
            outline = Color(0xFF6B85B8),
            outlineVariant = Color(0xFF283768)
        ),
        heroStart = Color(0xFF0C2348),
        heroEnd = Color(0xFF3E155C),
        bubbleAccentA = Color(0xFF4DF3FF),
        bubbleAccentB = Color(0xFFF66CFF),
        swatchPrimary = Color(0xFF122854),
        swatchSecondary = Color(0xFF4DF3FF),
        widgetBgRes = R.drawable.bg_widget_duet_aurora,
        widgetTitleHex = "#ECFBFF",
        widgetBodyHex = "#B6F6FF",
        widgetSubHex = "#C9B4F7",
        widgetChipTextHex = "#8DF8FF",
        isExclusive = true,
        exclusiveAccent = Color(0xFF4DF3FF),
        tagline = "Northern Lights Prism Glass",
        heroMid = Color(0xFF144E6B)
    ),
    AMETHYST_ECLIPSE(
        key = "AMETHYST",
        label = "Amethyst Eclipse",
        shortLabel = "Amethyst",
        emoji = "🔮",
        isDark = true,
        colorScheme = darkColorScheme(
            primary = Color(0xFFD596FF),
            onPrimary = Color(0xFF2B0047),
            primaryContainer = Color(0xFF47186B),
            onPrimaryContainer = Color(0xFFF1DAFF),
            secondary = Color(0xFFFF99C8),
            onSecondary = Color(0xFF420624),
            secondaryContainer = Color(0xFF5C173B),
            onSecondaryContainer = Color(0xFFFFD8E8),
            tertiary = Color(0xFFFFD38C),
            onTertiary = Color(0xFF3D2400),
            tertiaryContainer = Color(0xFF563506),
            onTertiaryContainer = Color(0xFFFFE6BF),
            background = Color(0xFF0D0718),
            onBackground = Color(0xFFF8EFFF),
            surface = Color(0xFF190E2B),
            onSurface = Color(0xFFF8EFFF),
            surfaceVariant = Color(0xFF291744),
            onSurfaceVariant = Color(0xFFD3BBE6),
            outline = Color(0xFF8E6EAD),
            outlineVariant = Color(0xFF412863)
        ),
        heroStart = Color(0xFF28104A),
        heroEnd = Color(0xFF681F5E),
        bubbleAccentA = Color(0xFFD596FF),
        bubbleAccentB = Color(0xFFFF99C8),
        swatchPrimary = Color(0xFF341459),
        swatchSecondary = Color(0xFFFF99C8),
        widgetBgRes = R.drawable.bg_widget_duet_amethyst,
        widgetTitleHex = "#FBF1FF",
        widgetBodyHex = "#E5C4FF",
        widgetSubHex = "#D8A8D8",
        widgetChipTextHex = "#F0D2FF",
        isExclusive = true,
        exclusiveAccent = Color(0xFFE09EFF),
        tagline = "Imperial Violet & Rose Quartz",
        heroMid = Color(0xFF4E1966)
    ),
    CHAMPAGNE_IVORY(
        key = "CHAMPAGNE",
        label = "Champagne Ivory",
        shortLabel = "Ivory Gold",
        emoji = "🥂",
        isDark = false,
        colorScheme = lightColorScheme(
            primary = Color(0xFF9E742A),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFF8E6C2),
            onPrimaryContainer = Color(0xFF2D1D02),
            secondary = Color(0xFF7D563A),
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFFF5E2D3),
            onSecondaryContainer = Color(0xFF2E180A),
            tertiary = Color(0xFF9D4E5E),
            onTertiary = Color(0xFFFFFFFF),
            tertiaryContainer = Color(0xFFFFD9DF),
            onTertiaryContainer = Color(0xFF3B0B17),
            background = Color(0xFFFCF8F0),
            onBackground = Color(0xFF241B10),
            surface = Color(0xFFFCF8F0),
            onSurface = Color(0xFF241B10),
            surfaceVariant = Color(0xFFF3E9D8),
            onSurfaceVariant = Color(0xFF5B4D3A),
            outline = Color(0xFF948064),
            outlineVariant = Color(0xFFE3D3B8)
        ),
        heroStart = Color(0xFF3A2A15),
        heroEnd = Color(0xFF7A5724),
        bubbleAccentA = Color(0xFFE5BD6B),
        bubbleAccentB = Color(0xFFF5D8A3),
        swatchPrimary = Color(0xFF9E742A),
        swatchSecondary = Color(0xFFF8E6C2),
        widgetBgRes = R.drawable.bg_widget_duet_ivory_gold,
        widgetTitleHex = "#2D200E",
        widgetBodyHex = "#5A411E",
        widgetSubHex = "#7D633F",
        widgetChipTextHex = "#6B4B12",
        isExclusive = true,
        exclusiveAccent = Color(0xFFD19F3B),
        tagline = "Haute Alabaster & Gilded Bronze",
        heroMid = Color(0xFF5F431B)
    ),
    SOLARIS_ROSE_GOLD(
        key = "SOLARIS",
        label = "Solaris Rose Gold",
        shortLabel = "Solaris",
        emoji = "🌅",
        isDark = false,
        colorScheme = lightColorScheme(
            primary = Color(0xFFC84664),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFFFD8DF),
            onPrimaryContainer = Color(0xFF420618),
            secondary = Color(0xFFD16D36),
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFFFFE2CF),
            onSecondaryContainer = Color(0xFF3D1903),
            tertiary = Color(0xFFB37924),
            onTertiary = Color(0xFFFFFFFF),
            tertiaryContainer = Color(0xFFFFE9C4),
            onTertiaryContainer = Color(0xFF362100),
            background = Color(0xFFFFF6F3),
            onBackground = Color(0xFF2B131C),
            surface = Color(0xFFFFF8F5),
            onSurface = Color(0xFF2B131C),
            surfaceVariant = Color(0xFFFCE6E1),
            onSurfaceVariant = Color(0xFF643B45),
            outline = Color(0xFF9E6B76),
            outlineVariant = Color(0xFFEDCAC4)
        ),
        heroStart = Color(0xFF7D2448),
        heroEnd = Color(0xFFE88D54),
        bubbleAccentA = Color(0xFFFF8F6B),
        bubbleAccentB = Color(0xFFFFC77E),
        swatchPrimary = Color(0xFFD6566A),
        swatchSecondary = Color(0xFFFFC082),
        widgetBgRes = R.drawable.bg_widget_duet_solaris,
        widgetTitleHex = "#3E1524",
        widgetBodyHex = "#74293F",
        widgetSubHex = "#994D5C",
        widgetChipTextHex = "#A32A4B",
        isExclusive = true,
        exclusiveAccent = Color(0xFFFF8F6B),
        tagline = "Radiant Rose-Gold & Sunset Amber",
        heroMid = Color(0xFFC94B63)
    ),
    CELESTIAL_SAPPHIRE(
        key = "SAPPHIRE",
        label = "Celestial Sapphire",
        shortLabel = "Sapphire",
        emoji = "💎",
        isDark = true,
        colorScheme = darkColorScheme(
            primary = Color(0xFF7CE7FF),
            onPrimary = Color(0xFF002F3D),
            primaryContainer = Color(0xFF0A4766),
            onPrimaryContainer = Color(0xFFC8F4FF),
            secondary = Color(0xFFA2BAFF),
            onSecondary = Color(0xFF0A245E),
            secondaryContainer = Color(0xFF1C3A7A),
            onSecondaryContainer = Color(0xFFD9E3FF),
            tertiary = Color(0xFFE7B8FF),
            onTertiary = Color(0xFF3A0F56),
            tertiaryContainer = Color(0xFF512470),
            onTertiaryContainer = Color(0xFFF5DFFF),
            background = Color(0xFF050C1D),
            onBackground = Color(0xFFEAF4FF),
            surface = Color(0xFF0C1833),
            onSurface = Color(0xFFEAF4FF),
            surfaceVariant = Color(0xFF15274F),
            onSurfaceVariant = Color(0xFFB5CCE8),
            outline = Color(0xFF6B8BB8),
            outlineVariant = Color(0xFF263E70)
        ),
        heroStart = Color(0xFF071B42),
        heroEnd = Color(0xFF288CC4),
        bubbleAccentA = Color(0xFF7CE7FF),
        bubbleAccentB = Color(0xFF9EAFFF),
        swatchPrimary = Color(0xFF123F8A),
        swatchSecondary = Color(0xFF7CE7FF),
        widgetBgRes = R.drawable.bg_widget_duet_sapphire,
        widgetTitleHex = "#EDF7FF",
        widgetBodyHex = "#BDE8FF",
        widgetSubHex = "#96C4E8",
        widgetChipTextHex = "#8CEAFF",
        isExclusive = true,
        exclusiveAccent = Color(0xFF7CE7FF),
        tagline = "Midnight Sapphire & Starlight Cyan",
        heroMid = Color(0xFF154B9C)
    ),
    CRIMSON_RUBY(
        key = "RUBY",
        label = "Crimson Ruby",
        shortLabel = "Ruby",
        emoji = "🌹",
        isDark = true,
        colorScheme = darkColorScheme(
            primary = Color(0xFFFF86A4),
            onPrimary = Color(0xFF470019),
            primaryContainer = Color(0xFF6B102E),
            onPrimaryContainer = Color(0xFFFFD8E2),
            secondary = Color(0xFFFFB28A),
            onSecondary = Color(0xFF471800),
            secondaryContainer = Color(0xFF662A0F),
            onSecondaryContainer = Color(0xFFFFE0CF),
            tertiary = Color(0xFFFFD98A),
            onTertiary = Color(0xFF3D2700),
            tertiaryContainer = Color(0xFF573909),
            onTertiaryContainer = Color(0xFFFFEBC2),
            background = Color(0xFF120409),
            onBackground = Color(0xFFFFEFF3),
            surface = Color(0xFF210A13),
            onSurface = Color(0xFFFFEFF3),
            surfaceVariant = Color(0xFF361221),
            onSurfaceVariant = Color(0xFFE3B9C7),
            outline = Color(0xFFA66B7F),
            outlineVariant = Color(0xFF542235)
        ),
        heroStart = Color(0xFF2A0614),
        heroEnd = Color(0xFFB83050),
        bubbleAccentA = Color(0xFFFF7A9A),
        bubbleAccentB = Color(0xFFFFB48A),
        swatchPrimary = Color(0xFF8A1337),
        swatchSecondary = Color(0xFFFF86A4),
        widgetBgRes = R.drawable.bg_widget_duet_ruby,
        widgetTitleHex = "#FFF0F4",
        widgetBodyHex = "#FFC2D2",
        widgetSubHex = "#E69DB2",
        widgetChipTextHex = "#FF99B5",
        isExclusive = true,
        exclusiveAccent = Color(0xFFFF7A9A),
        tagline = "Imperial Ruby & Molten Rose",
        heroMid = Color(0xFF7A1233)
    ),
    NEON_LOTUS(
        key = "LOTUS",
        label = "Neon Lotus",
        shortLabel = "Lotus",
        emoji = "🪷",
        isDark = true,
        colorScheme = darkColorScheme(
            primary = Color(0xFFFF8CE0),
            onPrimary = Color(0xFF46003A),
            primaryContainer = Color(0xFF631157),
            onPrimaryContainer = Color(0xFFFFD8F5),
            secondary = Color(0xFFFFD36E),
            onSecondary = Color(0xFF3A2500),
            secondaryContainer = Color(0xFF543707),
            onSecondaryContainer = Color(0xFFFFE8B5),
            tertiary = Color(0xFF9DF2FF),
            onTertiary = Color(0xFF003640),
            tertiaryContainer = Color(0xFF0C4F5C),
            onTertiaryContainer = Color(0xFFC9FAFF),
            background = Color(0xFF0E0518),
            onBackground = Color(0xFFFDF0FF),
            surface = Color(0xFF1A0B2B),
            onSurface = Color(0xFFFDF0FF),
            surfaceVariant = Color(0xFF2D1447),
            onSurfaceVariant = Color(0xFFDABBE8),
            outline = Color(0xFF996CAE),
            outlineVariant = Color(0xFF4A256B)
        ),
        heroStart = Color(0xFF1A072D),
        heroEnd = Color(0xFFC23C8E),
        bubbleAccentA = Color(0xFFFF8CE0),
        bubbleAccentB = Color(0xFFFFD36E),
        swatchPrimary = Color(0xFF6B1A7A),
        swatchSecondary = Color(0xFFFF8CE0),
        widgetBgRes = R.drawable.bg_widget_duet_lotus,
        widgetTitleHex = "#FFF0FC",
        widgetBodyHex = "#FBC2F0",
        widgetSubHex = "#E4A6DB",
        widgetChipTextHex = "#FFA8EA",
        isExclusive = true,
        exclusiveAccent = Color(0xFFFF8CE0),
        tagline = "Iridescent Orchid & Electric Gold",
        heroMid = Color(0xFF6B1A7A)
    );

    fun heroGradientColors(): List<Color> = listOfNotNull(heroStart, heroMid, heroEnd)

    fun widgetTitleColorInt(): Int = AndroidColor.parseColor(widgetTitleHex)
    fun widgetBodyColorInt(): Int = AndroidColor.parseColor(widgetBodyHex)
    fun widgetSubColorInt(): Int = AndroidColor.parseColor(widgetSubHex)
    fun widgetChipTextColorInt(): Int = AndroidColor.parseColor(widgetChipTextHex)

    fun toWidgetThemeStyle(): WidgetThemeStyle = WidgetThemeStyle.fromKey(key)

    companion object {
        fun fromKey(raw: String?): AppThemePreset =
            entries.firstOrNull { it.key.equals(raw, ignoreCase = true) } ?: BLUSH_ROSE
    }
}

enum class ChatWallpaperPreset(
    val key: String,
    val label: String,
    val emoji: String,
    val isDarkCanvas: Boolean,
    val gradientColors: List<Color>,
    val accentDotColor: Color
) {
    THEME_DEFAULT(
        key = "THEME_DEFAULT",
        label = "Theme Match",
        emoji = "🎨",
        isDarkCanvas = false,
        gradientColors = listOf(Color(0xFFF9EFF2), Color(0xFFF2E2E8)),
        accentDotColor = Color(0xFFA83E53)
    ),
    VELVET_STARS(
        key = "VELVET_STARS",
        label = "Velvet Starlight",
        emoji = "✨",
        isDarkCanvas = true,
        gradientColors = listOf(Color(0xFF150914), Color(0xFF281126), Color(0xFF1B0E24)),
        accentDotColor = Color(0xFFFFD98A)
    ),
    ROSE_SILK(
        key = "ROSE_SILK",
        label = "Blush Silk",
        emoji = "🌹",
        isDarkCanvas = false,
        gradientColors = listOf(Color(0xFFFFF4F7), Color(0xFFFCE2EB), Color(0xFFF7D5E2)),
        accentDotColor = Color(0xFFD65A85)
    ),
    MIDNIGHT_AURORA(
        key = "MIDNIGHT_AURORA",
        label = "Aurora Sky",
        emoji = "🌌",
        isDarkCanvas = true,
        gradientColors = listOf(Color(0xFF070D21), Color(0xFF10284A), Color(0xFF2B1247)),
        accentDotColor = Color(0xFF4DF3FF)
    ),
    ROYAL_GOLD_WEAVE(
        key = "ROYAL_GOLD_WEAVE",
        label = "Obsidian Gold",
        emoji = "👑",
        isDarkCanvas = true,
        gradientColors = listOf(Color(0xFF09080A), Color(0xFF1A140A), Color(0xFF0E0B07)),
        accentDotColor = Color(0xFFF5CA6E)
    ),
    WHISPER_HEARTS(
        key = "WHISPER_HEARTS",
        label = "Whisper Hearts",
        emoji = "💕",
        isDarkCanvas = false,
        gradientColors = listOf(Color(0xFFFFF8F2), Color(0xFFFDE9E4), Color(0xFFF9DCE2)),
        accentDotColor = Color(0xFFE06D88)
    ),
    SAGE_MIST(
        key = "SAGE_MIST",
        label = "Emerald Mist",
        emoji = "🌿",
        isDarkCanvas = true,
        gradientColors = listOf(Color(0xFF091815), Color(0xFF123028), Color(0xFF0D211C)),
        accentDotColor = Color(0xFF7BE5C2)
    ),
    CUSTOM_PHOTO(
        key = "CUSTOM_PHOTO",
        label = "Custom Photo",
        emoji = "🖼️",
        isDarkCanvas = true,
        gradientColors = listOf(Color(0xFF1E1B24), Color(0xFF332A3E)),
        accentDotColor = Color(0xFFFFFFFF)
    );

    companion object {
        fun fromKey(raw: String?): ChatWallpaperPreset =
            entries.firstOrNull { it.key.equals(raw, ignoreCase = true) } ?: THEME_DEFAULT
    }
}

enum class AppIconPreset(
    val key: String,
    val label: String,
    val aliasClassName: String,
    val previewStart: Color,
    val previewEnd: Color,
    @DrawableRes val bgDrawableRes: Int?,
    @DrawableRes val fgDrawableRes: Int?,
    val isClassicPhoto: Boolean = false
) {
    CLASSIC(
        key = "CLASSIC",
        label = "Lanterns",
        aliasClassName = "com.example.LauncherClassic",
        previewStart = Color(0xFF3E182A),
        previewEnd = Color(0xFFA83E53),
        bgDrawableRes = null,
        fgDrawableRes = R.drawable.ic_duet_logo,
        isClassicPhoto = true
    ),
    ROSE(
        key = "ROSE",
        label = "Rose Hearts",
        aliasClassName = "com.example.LauncherRose",
        previewStart = Color(0xFFFF85A1),
        previewEnd = Color(0xFFC22D54),
        bgDrawableRes = R.drawable.bg_icon_rose,
        fgDrawableRes = R.drawable.fg_icon_rose
    ),
    MIDNIGHT(
        key = "MIDNIGHT",
        label = "Midnight",
        aliasClassName = "com.example.LauncherMidnight",
        previewStart = Color(0xFF173A69),
        previewEnd = Color(0xFF0A162B),
        bgDrawableRes = R.drawable.bg_icon_midnight,
        fgDrawableRes = R.drawable.fg_icon_midnight
    ),
    VELVET(
        key = "VELVET",
        label = "Velvet Gold",
        aliasClassName = "com.example.LauncherVelvet",
        previewStart = Color(0xFF4F1D37),
        previewEnd = Color(0xFF210B16),
        bgDrawableRes = R.drawable.bg_icon_velvet,
        fgDrawableRes = R.drawable.fg_icon_velvet
    ),
    SUNSET(
        key = "SUNSET",
        label = "Sunset",
        aliasClassName = "com.example.LauncherSunset",
        previewStart = Color(0xFFFFB27A),
        previewEnd = Color(0xFFD6513C),
        bgDrawableRes = R.drawable.bg_icon_sunset,
        fgDrawableRes = R.drawable.fg_icon_sunset
    ),
    MINIMAL(
        key = "MINIMAL",
        label = "Obsidian",
        aliasClassName = "com.example.LauncherMinimal",
        previewStart = Color(0xFF292B33),
        previewEnd = Color(0xFF121318),
        bgDrawableRes = R.drawable.bg_icon_minimal,
        fgDrawableRes = R.drawable.fg_icon_minimal
    );

    companion object {
        fun fromKey(raw: String?): AppIconPreset =
            entries.firstOrNull { it.key.equals(raw, ignoreCase = true) } ?: CLASSIC
    }
}

data class DuetAppearanceState(
    val themePreset: AppThemePreset = AppThemePreset.BLUSH_ROSE,
    val iconPreset: AppIconPreset = AppIconPreset.CLASSIC,
    val customLogoPath: String? = null,
    val customLogoVersion: Long = 0L,
    val customBrandTitle: String = "",
    val bubblesEnabled: Boolean = true,
    val chatWallpaper: ChatWallpaperPreset = ChatWallpaperPreset.THEME_DEFAULT,
    val customChatWallpaperPath: String? = null,
    val customChatWallpaperVersion: Long = 0L,
    val chatWallpaperDimAlpha: Float = 0.28f
)

object DuetAppearanceStore {
    private const val TAG = "DuetAppearanceStore"
    private const val PREFS_NAME = "duet_appearance_prefs"
    private const val KEY_THEME = "app_theme_preset"
    private const val KEY_ICON = "app_icon_preset"
    private const val KEY_CUSTOM_LOGO_PATH = "custom_logo_path"
    private const val KEY_CUSTOM_LOGO_VER = "custom_logo_ver"
    private const val KEY_BRAND_TITLE = "custom_brand_title"
    private const val KEY_BUBBLES = "bubbles_enabled"
    private const val KEY_CHAT_WALLPAPER = "chat_wallpaper_preset"
    private const val KEY_CUSTOM_WALLPAPER_PATH = "custom_chat_wallpaper_path"
    private const val KEY_CUSTOM_WALLPAPER_VER = "custom_chat_wallpaper_ver"
    private const val KEY_CHAT_WALLPAPER_DIM = "chat_wallpaper_dim_alpha"
    private const val CUSTOM_LOGO_FILENAME = "custom_app_logo.jpg"
    private const val CUSTOM_WALLPAPER_FILENAME = "custom_chat_wallpaper.jpg"

    @Volatile
    private var initialized = false
    private val _state = MutableStateFlow(DuetAppearanceState())

    fun state(context: Context): StateFlow<DuetAppearanceState> {
        ensureLoaded(context.applicationContext)
        return _state.asStateFlow()
    }

    fun current(context: Context): DuetAppearanceState {
        ensureLoaded(context.applicationContext)
        return _state.value
    }

    private fun ensureLoaded(appContext: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val storedLogo = prefs.getString(KEY_CUSTOM_LOGO_PATH, null)?.takeIf { File(it).exists() }
            val storedWallpaper = prefs.getString(KEY_CUSTOM_WALLPAPER_PATH, null)?.takeIf { File(it).exists() }
            val rawWallpaperPreset = ChatWallpaperPreset.fromKey(
                prefs.getString(KEY_CHAT_WALLPAPER, ChatWallpaperPreset.THEME_DEFAULT.key)
            )
            val resolvedWallpaper = if (rawWallpaperPreset == ChatWallpaperPreset.CUSTOM_PHOTO && storedWallpaper == null) {
                ChatWallpaperPreset.THEME_DEFAULT
            } else {
                rawWallpaperPreset
            }
            _state.value = DuetAppearanceState(
                themePreset = AppThemePreset.fromKey(prefs.getString(KEY_THEME, AppThemePreset.BLUSH_ROSE.key)),
                iconPreset = AppIconPreset.fromKey(prefs.getString(KEY_ICON, AppIconPreset.CLASSIC.key)),
                customLogoPath = storedLogo,
                customLogoVersion = prefs.getLong(KEY_CUSTOM_LOGO_VER, 0L),
                customBrandTitle = prefs.getString(KEY_BRAND_TITLE, "") ?: "",
                bubblesEnabled = prefs.getBoolean(KEY_BUBBLES, true),
                chatWallpaper = resolvedWallpaper,
                customChatWallpaperPath = storedWallpaper,
                customChatWallpaperVersion = prefs.getLong(KEY_CUSTOM_WALLPAPER_VER, 0L),
                chatWallpaperDimAlpha = prefs.getFloat(KEY_CHAT_WALLPAPER_DIM, 0.28f).coerceIn(0f, 0.75f)
            )
            initialized = true
        }
    }

    fun selectTheme(context: Context, preset: AppThemePreset) {
        val appCtx = context.applicationContext
        ensureLoaded(appCtx)
        appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_THEME, preset.key)
            .apply()
        _state.update { it.copy(themePreset = preset) }

        WidgetStateStore(appCtx).setThemeStyle(preset.toWidgetThemeStyle())
        refreshAllWidgets(appCtx)
    }

    fun cycleTheme(context: Context): AppThemePreset {
        val appCtx = context.applicationContext
        ensureLoaded(appCtx)
        val entries = AppThemePreset.entries
        val nextIndex = (entries.indexOf(_state.value.themePreset) + 1) % entries.size
        val next = entries[nextIndex]
        selectTheme(appCtx, next)
        return next
    }

    /**
     * Switches the Android home screen launcher icon immediately via `<activity-alias>` components.
     * Enables the target alias BEFORE disabling others so Android never loses the launcher entry.
     */
    fun selectIconPreset(context: Context, preset: AppIconPreset): Boolean {
        val appCtx = context.applicationContext
        ensureLoaded(appCtx)
        val pm = appCtx.packageManager
        val pkg = appCtx.packageName

        return try {
            val enableComponent = ComponentName(pkg, preset.aliasClassName)
            pm.setComponentEnabledSetting(
                enableComponent,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
            AppIconPreset.entries
                .filter { it != preset }
                .forEach { other ->
                    val disableComponent = ComponentName(pkg, other.aliasClassName)
                    pm.setComponentEnabledSetting(
                        disableComponent,
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP
                    )
                }

            appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_ICON, preset.key)
                .apply()
            _state.update { it.copy(iconPreset = preset) }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed switching launcher icon alias to ${preset.aliasClassName}", e)
            false
        }
    }

    suspend fun saveCustomLogo(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        val appCtx = context.applicationContext
        ensureLoaded(appCtx)
        try {
            val rawBitmap = decodeUriToSoftwareBitmap(appCtx, uri) ?: return@withContext false
            val square = centerCropSquare(rawBitmap, 512)
            if (square != rawBitmap) rawBitmap.recycle()

            val outFile = File(appCtx.filesDir, CUSTOM_LOGO_FILENAME)
            FileOutputStream(outFile).use { stream ->
                square.compress(Bitmap.CompressFormat.JPEG, 90, stream)
                stream.flush()
            }
            square.recycle()

            val newVersion = System.currentTimeMillis()
            appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_CUSTOM_LOGO_PATH, outFile.absolutePath)
                .putLong(KEY_CUSTOM_LOGO_VER, newVersion)
                .apply()
            _state.update {
                it.copy(customLogoPath = outFile.absolutePath, customLogoVersion = newVersion)
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed saving custom logo from URI", e)
            false
        }
    }

    fun clearCustomLogo(context: Context) {
        val appCtx = context.applicationContext
        ensureLoaded(appCtx)
        runCatching { File(appCtx.filesDir, CUSTOM_LOGO_FILENAME).delete() }
        appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_CUSTOM_LOGO_PATH)
            .putLong(KEY_CUSTOM_LOGO_VER, System.currentTimeMillis())
            .apply()
        _state.update { it.copy(customLogoPath = null, customLogoVersion = System.currentTimeMillis()) }
    }

    fun setCustomBrandTitle(context: Context, title: String) {
        val appCtx = context.applicationContext
        ensureLoaded(appCtx)
        val clean = title.trim().take(24)
        appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_BRAND_TITLE, clean)
            .apply()
        _state.update { it.copy(customBrandTitle = clean) }
    }

    fun setBubblesEnabled(context: Context, enabled: Boolean) {
        val appCtx = context.applicationContext
        ensureLoaded(appCtx)
        appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_BUBBLES, enabled)
            .apply()
        _state.update { it.copy(bubblesEnabled = enabled) }
    }

    fun selectChatWallpaper(context: Context, preset: ChatWallpaperPreset) {
        val appCtx = context.applicationContext
        ensureLoaded(appCtx)
        val effective = if (preset == ChatWallpaperPreset.CUSTOM_PHOTO && _state.value.customChatWallpaperPath == null) {
            ChatWallpaperPreset.THEME_DEFAULT
        } else {
            preset
        }
        appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CHAT_WALLPAPER, effective.key)
            .apply()
        _state.update { it.copy(chatWallpaper = effective) }
    }

    fun setChatWallpaperDim(context: Context, dimAlpha: Float) {
        val appCtx = context.applicationContext
        ensureLoaded(appCtx)
        val clamped = dimAlpha.coerceIn(0f, 0.75f)
        appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putFloat(KEY_CHAT_WALLPAPER_DIM, clamped)
            .apply()
        _state.update { it.copy(chatWallpaperDimAlpha = clamped) }
    }

    suspend fun saveCustomChatWallpaper(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        val appCtx = context.applicationContext
        ensureLoaded(appCtx)
        try {
            val rawBitmap = decodeUriToSoftwareBitmap(appCtx, uri, maxLongestEdge = 1280)
                ?: return@withContext false
            val outFile = File(appCtx.filesDir, CUSTOM_WALLPAPER_FILENAME)
            FileOutputStream(outFile).use { stream ->
                rawBitmap.compress(Bitmap.CompressFormat.JPEG, 88, stream)
                stream.flush()
            }
            rawBitmap.recycle()

            val newVersion = System.currentTimeMillis()
            appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_CHAT_WALLPAPER, ChatWallpaperPreset.CUSTOM_PHOTO.key)
                .putString(KEY_CUSTOM_WALLPAPER_PATH, outFile.absolutePath)
                .putLong(KEY_CUSTOM_WALLPAPER_VER, newVersion)
                .apply()
            _state.update {
                it.copy(
                    chatWallpaper = ChatWallpaperPreset.CUSTOM_PHOTO,
                    customChatWallpaperPath = outFile.absolutePath,
                    customChatWallpaperVersion = newVersion
                )
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed saving custom chat wallpaper", e)
            false
        }
    }

    fun clearCustomChatWallpaper(context: Context) {
        val appCtx = context.applicationContext
        ensureLoaded(appCtx)
        runCatching { File(appCtx.filesDir, CUSTOM_WALLPAPER_FILENAME).delete() }
        val newVersion = System.currentTimeMillis()
        appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CHAT_WALLPAPER, ChatWallpaperPreset.THEME_DEFAULT.key)
            .remove(KEY_CUSTOM_WALLPAPER_PATH)
            .putLong(KEY_CUSTOM_WALLPAPER_VER, newVersion)
            .apply()
        _state.update {
            it.copy(
                chatWallpaper = ChatWallpaperPreset.THEME_DEFAULT,
                customChatWallpaperPath = null,
                customChatWallpaperVersion = newVersion
            )
        }
    }

    fun refreshAllWidgets(context: Context) {
        WidgetStateStore.updateAllDuetWidgets(context)
    }

    private fun decodeUriToSoftwareBitmap(
        context: Context,
        uri: Uri,
        maxLongestEdge: Int = 720
    ): Bitmap? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                val src = ImageDecoder.createSource(context.contentResolver, uri)
                return ImageDecoder.decodeBitmap(src) { decoder, info, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    val w = info.size.width
                    val h = info.size.height
                    val longest = maxOf(w, h)
                    if (longest > maxLongestEdge && longest > 0) {
                        val scale = maxLongestEdge.toFloat() / longest.toFloat()
                        decoder.setTargetSize(
                            (w * scale).toInt().coerceAtLeast(1),
                            (h * scale).toInt().coerceAtLeast(1)
                        )
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "ImageDecoder bitmap decode failed, falling back to stream", e)
            }
        }
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun centerCropSquare(src: Bitmap, targetSize: Int): Bitmap {
        val side = min(src.width, src.height).coerceAtLeast(1)
        val left = (src.width - side) / 2
        val top = (src.height - side) / 2
        val cropped = Bitmap.createBitmap(src, left, top, side, side)
        return if (side > targetSize) {
            val scaled = Bitmap.createScaledBitmap(cropped, targetSize, targetSize, true)
            if (scaled != cropped) cropped.recycle()
            scaled
        } else {
            cropped
        }
    }
}
