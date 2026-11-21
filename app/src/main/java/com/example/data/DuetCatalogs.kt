package com.example.data

import androidx.compose.ui.graphics.Color
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

data class MoodOption(
    val key: String,
    val emoji: String,
    val label: String,
    val accentColor: Color,
    val energyLevel: Int,
    val needsComfort: Boolean = false
)

object MoodCatalog {
    val all: List<MoodOption> = listOf(
        MoodOption("IN_LOVE", "🥰", "In Love", Color(0xFFD64C6D), 5),
        MoodOption("MISSING_YOU", "🥺", "Missing You", Color(0xFFC55F7C), 3, needsComfort = true),
        MoodOption("COZY", "☕", "Cozy", Color(0xFFA86F4B), 4),
        MoodOption("GLOWING", "✨", "Glowing", Color(0xFFD1933A), 5),
        MoodOption("PLAYFUL", "😜", "Playful", Color(0xFFD66A48), 5),
        MoodOption("SLEEPY", "🌙", "Sleepy", Color(0xFF6B5B95), 2),
        MoodOption("NEED_HUGS", "🫂", "Need Hugs", Color(0xFF8B5D7A), 2, needsComfort = true),
        MoodOption("OVERWHELMED", "🌧️", "Overwhelmed", Color(0xFF5D6D7E), 1, needsComfort = true),
        MoodOption("GRATEFUL", "🌷", "Grateful", Color(0xFF4A8C6F), 4)
    )

    fun byKey(key: String?): MoodOption =
        all.firstOrNull { it.key == key } ?: all.first()
}

data class DailyPromptExpiryCountdown(
    val hours: Long,
    val minutes: Long,
    val seconds: Long,
    val millisRemaining: Long,
    val isExpiringSoon: Boolean,
    val formattedCompact: String
)

object DailyPromptDeck {
    private val istZone: TimeZone = TimeZone.getTimeZone("Asia/Kolkata")

    val questions: List<String> = listOf(
        "What tiny thing I did recently made your heart feel warm?",
        "If we could teleport anywhere together for 24 hours tonight, where are we going?",
        "Which memory of us always makes you smile on a tough day?",
        "What song instantly reminds you of me?",
        "What is one cozy ritual you want us to start this season?",
        "When did you first realize how special we are together?",
        "What meal should we cook together on our next slow evening?",
        "What is your favorite look or expression I make without noticing?",
        "Describe our dream Sunday morning in three little details.",
        "What is one adventure you want to check off with me this year?",
        "What part of my voice or laugh do you love the most?",
        "If you wrote me a postcard from five years in the future, what would it say?",
        "What small way can I make your week feel lighter right now?",
        "Which photo of us is your secret favorite?",
        "What scent, season, or time of day feels the most like us?",
        "What is a compliment you have been meaning to tell me lately?"
    )

    fun todayDateKey(nowMillis: Long = System.currentTimeMillis()): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = istZone
        }.format(Date(nowMillis))

    fun todayPromptIndex(nowMillis: Long = System.currentTimeMillis()): Int {
        val cal = Calendar.getInstance(istZone).apply { timeInMillis = nowMillis }
        val daySeed = cal.get(Calendar.YEAR) * 366 + cal.get(Calendar.DAY_OF_YEAR)
        return abs(daySeed) % questions.size
    }

    fun questionForIndex(index: Int): String =
        questions[abs(index) % questions.size]

    /**
     * Computes live countdown until today's question expires at 12:00 AM IST (midnight rollover).
     * Flags `isExpiringSoon = true` when 1 hour (3,600,000 ms) or less remains.
     */
    fun computeExpiryCountdown(nowMillis: Long = System.currentTimeMillis()): DailyPromptExpiryCountdown {
        val nextMidnight = Calendar.getInstance(istZone).apply {
            timeInMillis = nowMillis
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val remainingMs = (nextMidnight.timeInMillis - nowMillis).coerceAtLeast(0L)
        val totalSecs = remainingMs / 1000L
        val hours = totalSecs / 3600L
        val minutes = (totalSecs % 3600L) / 60L
        val seconds = totalSecs % 60L
        val isExpiringSoon = remainingMs in 1L..3_600_000L
        val formatted = if (hours > 0L) {
            String.format(Locale.US, "%02dh %02dm %02ds", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02dm %02ds", minutes, seconds)
        }
        return DailyPromptExpiryCountdown(
            hours = hours,
            minutes = minutes,
            seconds = seconds,
            millisRemaining = remainingMs,
            isExpiringSoon = isExpiringSoon,
            formattedCompact = formatted
        )
    }
}

data class BucketCategoryOption(
    val key: String,
    val emoji: String,
    val label: String,
    val accentColor: Color
)

object BucketListCatalog {
    val categories: List<BucketCategoryOption> = listOf(
        BucketCategoryOption("DATE_NIGHT", "🌹", "Date Night", Color(0xFFD64C6D)),
        BucketCategoryOption("TRAVEL", "✈️", "Travel", Color(0xFF3E87D6)),
        BucketCategoryOption("COZY", "🕯️", "Cozy Home", Color(0xFFB57249)),
        BucketCategoryOption("FOODIE", "🍜", "Foodie", Color(0xFFD4892A)),
        BucketCategoryOption("ADVENTURE", "⛰️", "Adventure", Color(0xFF359472)),
        BucketCategoryOption("MILESTONE", "💍", "Milestone", Color(0xFF8C52C4))
    )

    val starterSuggestions: List<Pair<String, String>> = listOf(
        "Watch sunrise together with hot coffee" to "COZY",
        "Late-night drive with our favorite playlist" to "DATE_NIGHT",
        "Cook a homemade candlelit pasta dinner" to "FOODIE",
        "Plan a spontaneous weekend hill-station trip" to "TRAVEL",
        "Stargaze on a rooftop and make wishes" to "ADVENTURE"
    )

    fun byKey(key: String?): BucketCategoryOption =
        categories.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: categories.first()
}

data class DateIdeaOption(
    val id: String,
    val emoji: String,
    val title: String,
    val vibe: String,
    val description: String,
    val wheelColor: Color
)

object DateSpinnerCatalog {
    val ideas: List<DateIdeaOption> = listOf(
        DateIdeaOption(
            id = "candle_cook",
            emoji = "🍝",
            title = "Candlelit Cook-Off",
            vibe = "Cozy",
            description = "Pick one surprise recipe, turn on soft jazz, and cook dinner side-by-side by candlelight.",
            wheelColor = Color(0xFFD94C74)
        ),
        DateIdeaOption(
            id = "midnight_drive",
            emoji = "🚗",
            title = "Midnight Music Drive",
            vibe = "Romantic",
            description = "Windows down, favorite slow songs queued up, and a spontaneous stop for chai or ice cream.",
            wheelColor = Color(0xFF4A6CF7)
        ),
        DateIdeaOption(
            id = "blanket_cinema",
            emoji = "🍿",
            title = "Blanket Fort Cinema",
            vibe = "Cozy",
            description = "Fairy lights, pillow fort, double feature of our comfort movies, and zero phones allowed.",
            wheelColor = Color(0xFF8B5CF6)
        ),
        DateIdeaOption(
            id = "sunset_picnic",
            emoji = "🧺",
            title = "Golden Hour Picnic",
            vibe = "Outdoors",
            description = "Pack snacks, Polaroids, and a cozy sheet to catch the golden sunset together.",
            wheelColor = Color(0xFFF59E0B)
        ),
        DateIdeaOption(
            id = "street_food",
            emoji = "🌮",
            title = "3-Stop Foodie Crawl",
            vibe = "Foodie",
            description = "Starter at spot #1, main dish at spot #2, and decadent dessert at spot #3 — sharing every bite!",
            wheelColor = Color(0xFFEC4899)
        ),
        DateIdeaOption(
            id = "stargaze",
            emoji = "🌌",
            title = "Rooftop Stargazing",
            vibe = "Dreamy",
            description = "Bring warm drinks, lie back under the night sky, and share dreams for the next 5 years.",
            wheelColor = Color(0xFF0EA5E9)
        ),
        DateIdeaOption(
            id = "art_date",
            emoji = "🎨",
            title = "Paint Each Other",
            vibe = "Playful",
            description = "15-minute secret portrait challenge — reveal your masterpieces at the exact same time!",
            wheelColor = Color(0xFF10B981)
        ),
        DateIdeaOption(
            id = "coffee_walk",
            emoji = "☕",
            title = "Slow Café & Bookstore",
            vibe = "Warm",
            description = "Order each other's mystery drinks and pick a book or handwritten note for the other to read.",
            wheelColor = Color(0xFFD97706)
        ),
        DateIdeaOption(
            id = "spa_night",
            emoji = "🫧",
            title = "At-Home Spa Evening",
            vibe = "Relaxing",
            description = "Warm towels, face masks, shoulder massages, and soothing music to melt away the whole week.",
            wheelColor = Color(0xFF14B8A6)
        ),
        DateIdeaOption(
            id = "memory_lane",
            emoji = "📸",
            title = "Photo Memory Night",
            vibe = "Nostalgic",
            description = "Scroll through our oldest photos, recreate one funny pose right now, and save it to Moments!",
            wheelColor = Color(0xFFE11D48)
        )
    )
}

enum class CoupleCardKind(val label: String, val emoji: String, val accent: Color) {
    TRUTH("Sweet Truth", "💌", Color(0xFFD64C6D)),
    DARE("Playful Dare", "🔥", Color(0xFFE06D3B)),
    DEEP("Deep Soul", "🌙", Color(0xFF7C4DFF)),
    WYR("Would You Rather", "✨", Color(0xFF009688))
}

data class CoupleGameCard(
    val id: String,
    val kind: CoupleCardKind,
    val prompt: String
)

object TruthOrLoveCatalog {
    val deck: List<CoupleGameCard> = listOf(
        CoupleGameCard("t1", CoupleCardKind.TRUTH, "What exact moment made you fall even harder for me?"),
        CoupleGameCard("d1", CoupleCardKind.DARE, "Send me a voice note or text right now describing me using only 3 irresistible words."),
        CoupleGameCard("dp1", CoupleCardKind.DEEP, "What is one dream you have never told anyone else that you want us to chase together?"),
        CoupleGameCard("w1", CoupleCardKind.WYR, "Would you rather spend a cozy snowed-in weekend in a cabin or a breezy week by a private beach?"),
        CoupleGameCard("t2", CoupleCardKind.TRUTH, "What tiny habit of mine secretly makes you smile every single time?"),
        CoupleGameCard("d2", CoupleCardKind.DARE, "Take a cute candid selfie right this second and send it to our Private Chat!"),
        CoupleGameCard("dp2", CoupleCardKind.DEEP, "How do you feel most loved when you're having a stressful day?"),
        CoupleGameCard("w2", CoupleCardKind.WYR, "Would you rather relive our very first date or fast-forward for 10 minutes to our 10th anniversary?"),
        CoupleGameCard("t3", CoupleCardKind.TRUTH, "Which outfit or look of mine is your all-time favorite?"),
        CoupleGameCard("d3", CoupleCardKind.DARE, "Write a 2-line mini love poem about us and drop it in Private Chat right now."),
        CoupleGameCard("dp3", CoupleCardKind.DEEP, "What is one thing I do that makes you feel completely safe and understood?"),
        CoupleGameCard("w3", CoupleCardKind.WYR, "Would you rather cook every meal together forever or travel to a brand-new city every month?"),
        CoupleGameCard("t4", CoupleCardKind.TRUTH, "What was your honest first impression of me when we first met?"),
        CoupleGameCard("d4", CoupleCardKind.DARE, "Pick our next date night song and send the title + why it reminds you of me."),
        CoupleGameCard("dp4", CoupleCardKind.DEEP, "If our love story were a movie, what would the title of this current chapter be?"),
        CoupleGameCard("w4", CoupleCardKind.WYR, "Would you rather wake up early for a mountain sunrise together or stay up until 3 AM stargazing?")
    )
}

data class LoveCouponOption(
    val id: String,
    val emoji: String,
    val title: String,
    val subtitle: String,
    val gradientStart: Color,
    val gradientEnd: Color
)

object LoveCouponCatalog {
    val all: List<LoveCouponOption> = listOf(
        LoveCouponOption(
            id = "breakfast",
            emoji = "🥞",
            title = "Breakfast in Bed",
            subtitle = "Redeemable any lazy morning — coffee/tea & your favorite meal served with love.",
            gradientStart = Color(0xFFB83B5E),
            gradientEnd = Color(0xFFF08A5D)
        ),
        LoveCouponOption(
            id = "massage",
            emoji = "💆",
            title = "30-Min Royal Massage",
            subtitle = "Stress-melting shoulder & back massage whenever you've had a long day.",
            gradientStart = Color(0xFF512DA8),
            gradientEnd = Color(0xFFAB47BC)
        ),
        LoveCouponOption(
            id = "movie_veto",
            emoji = "🎬",
            title = "Movie Night Pick Veto",
            subtitle = "100% control over tonight's movie & snack menu — zero complaints allowed!",
            gradientStart = Color(0xFF1565C0),
            gradientEnd = Color(0xFF00ACC1)
        ),
        LoveCouponOption(
            id = "ice_cream",
            emoji = "🍨",
            title = "Late-Night Craving Run",
            subtitle = "Instant craving rescue — ice cream, chocolate, or midnight chai on demand.",
            gradientStart = Color(0xFFAD1457),
            gradientEnd = Color(0xFFEC407A)
        ),
        LoveCouponOption(
            id = "cuddle_hour",
            emoji = "🫂",
            title = "Uninterrupted Cuddle Hour",
            subtitle = "Phones on Do Not Disturb. Just warm hugs, playlists, and closeness.",
            gradientStart = Color(0xFF6A1B9A),
            gradientEnd = Color(0xFFE91E63)
        ),
        LoveCouponOption(
            id = "love_letter",
            emoji = "💌",
            title = "Handwritten Love Letter",
            subtitle = "A heartfelt page listing 10 reasons why you're my favorite human.",
            gradientStart = Color(0xFF8D4E1D),
            gradientEnd = Color(0xFFD99B38)
        ),
        LoveCouponOption(
            id = "no_chores",
            emoji = "✨",
            title = "Zero-Chores VIP Pass",
            subtitle = "Sit back and relax — I've got all the dishes and errands covered today!",
            gradientStart = Color(0xFF00695C),
            gradientEnd = Color(0xFF26A69A)
        ),
        LoveCouponOption(
            id = "surprise_date",
            emoji = "🌹",
            title = "Mystery Surprise Date",
            subtitle = "Just get dressed pretty — every detail of the evening is my secret treat.",
            gradientStart = Color(0xFF880E4F),
            gradientEnd = Color(0xFFD81B60)
        )
    )
}

data class CountdownBreakdown(
    val days: Long,
    val hours: Long,
    val minutes: Long,
    val seconds: Long,
    val isToday: Boolean,
    val nextDateLabel: String,
    val progressToNext: Float
)

object CountdownCalculator {

    fun daysTogether(year: Int, month: Int, day: Int, nowMillis: Long = System.currentTimeMillis()): Long {
        val start = Calendar.getInstance().apply {
            set(year, month - 1, day, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val diff = nowMillis - start.timeInMillis
        return (diff / 86_400_000L).coerceAtLeast(0L)
    }

    fun computeYearlyCountdown(
        month: Int,
        day: Int,
        nowMillis: Long = System.currentTimeMillis()
    ): CountdownBreakdown {
        val now = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val thisYear = now.get(Calendar.YEAR)
        val clampedDayThisYear = clampDay(thisYear, month, day)

        val isToday = now.get(Calendar.MONTH) == (month - 1) &&
            now.get(Calendar.DAY_OF_MONTH) == clampedDayThisYear

        val target = Calendar.getInstance().apply {
            set(thisYear, month - 1, clampedDayThisYear, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (!isToday && target.timeInMillis <= nowMillis) {
            val nextYear = thisYear + 1
            target.set(nextYear, month - 1, clampDay(nextYear, month, day), 0, 0, 0)
            target.set(Calendar.MILLISECOND, 0)
        }

        val diffMillis = (target.timeInMillis - nowMillis).coerceAtLeast(0L)
        val totalSecs = diffMillis / 1000L
        val days = totalSecs / 86_400L
        val hours = (totalSecs % 86_400L) / 3600L
        val minutes = (totalSecs % 3600L) / 60L
        val seconds = totalSecs % 60L

        val dateLabel = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(target.time)
        val progress = if (isToday) 1f else (1f - (days.toFloat() / 365f)).coerceIn(0.02f, 0.99f)

        return CountdownBreakdown(
            days = days,
            hours = hours,
            minutes = minutes,
            seconds = seconds,
            isToday = isToday,
            nextDateLabel = dateLabel,
            progressToNext = progress
        )
    }

    fun computeEventCountdown(
        year: Int,
        month: Int,
        day: Int,
        repeatsYearly: Boolean,
        nowMillis: Long = System.currentTimeMillis()
    ): CountdownBreakdown {
        if (repeatsYearly) return computeYearlyCountdown(month, day, nowMillis)
        val target = Calendar.getInstance().apply {
            set(year, month - 1, clampDay(year, month, day), 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val now = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val isToday = now.get(Calendar.YEAR) == year &&
            now.get(Calendar.MONTH) == (month - 1) &&
            now.get(Calendar.DAY_OF_MONTH) == clampDay(year, month, day)

        val diffMillis = (target.timeInMillis - nowMillis).coerceAtLeast(0L)
        val totalSecs = diffMillis / 1000L
        val days = totalSecs / 86_400L
        val hours = (totalSecs % 86_400L) / 3600L
        val minutes = (totalSecs % 3600L) / 60L
        val seconds = totalSecs % 60L
        val dateLabel = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(target.time)
        val progress = if (isToday || diffMillis == 0L) 1f else (1f - (days.coerceAtMost(365L) / 365f)).coerceIn(0.05f, 0.98f)

        return CountdownBreakdown(
            days = days,
            hours = hours,
            minutes = minutes,
            seconds = seconds,
            isToday = isToday,
            nextDateLabel = dateLabel,
            progressToNext = progress
        )
    }

    private fun clampDay(year: Int, month: Int, day: Int): Int {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, (month - 1).coerceIn(0, 11))
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        return day.coerceIn(1, maxDay)
    }
}

data class LetterSealOption(
    val key: String,
    val emoji: String,
    val label: String,
    val accentStart: Color,
    val accentEnd: Color
)

object LetterSealCatalog {
    val all: List<LetterSealOption> = listOf(
        LetterSealOption("WAX_ROSE", "🌹", "Rose Wax", Color(0xFFA83E53), Color(0xFFE06D82)),
        LetterSealOption("GOLD_ROYAL", "👑", "Royal Gold", Color(0xFF9C721D), Color(0xFFEBC05C)),
        LetterSealOption("STARLIGHT", "✨", "Starlight", Color(0xFF283A8A), Color(0xFF6B8AFD)),
        LetterSealOption("VELVET_HEART", "💌", "Velvet Heart", Color(0xFF6E1F46), Color(0xFFD64B80)),
        LetterSealOption("PAPER_PLANE", "✈️", "Time Capsule", Color(0xFF1E6E64), Color(0xFF42B8A6))
    )

    fun byKey(key: String?): LetterSealOption =
        all.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: all.first()
}

data class LoveJarKindOption(
    val key: String,
    val emoji: String,
    val label: String,
    val tint: Color
)

object LoveJarKindCatalog {
    val all: List<LoveJarKindOption> = listOf(
        LoveJarKindOption("APPRECIATION", "💖", "Appreciation", Color(0xFFD9466E)),
        LoveJarKindOption("WISH", "🌠", "Secret Wish", Color(0xFF7B4CF5)),
        LoveJarKindOption("MEMORY", "🎞️", "Sweet Memory", Color(0xFFD97706)),
        LoveJarKindOption("COMPLIMENT", "✨", "Compliment", Color(0xFF0E9F8E))
    )

    fun byKey(key: String?): LoveJarKindOption =
        all.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: all.first()
}

data class CarePresetOption(
    val key: String,
    val emoji: String,
    val label: String,
    val defaultMessage: String,
    val accent: Color
)

object CareCheckInCatalog {
    val presets: List<CarePresetOption> = listOf(
        CarePresetOption("WATER", "💧", "Hydrate", "Drink a full glass of water right now, sweetheart!", Color(0xFF0284C7)),
        CarePresetOption("FOOD", "🍲", "Eat Well", "Have you eaten properly yet? Don't skip meals!", Color(0xFFD97706)),
        CarePresetOption("MEDS", "💊", "Meds / Vitamins", "Gentle reminder to take your meds or vitamins on time.", Color(0xFF7C3AED)),
        CarePresetOption("REST", "🌙", "Rest Eyes", "Take a 5-minute screen break and breathe deeply.", Color(0xFF4338CA)),
        CarePresetOption("SAFE_HOME", "🏡", "Reached Safely?", "Text me when you reach safely — thinking of you!", Color(0xFF059669)),
        CarePresetOption("PERIOD_CARE", "🌷", "Comfort Care", "Warm blanket, tea & extra gentle hugs sent your way.", Color(0xFFDB2777)),
        CarePresetOption("CUSTOM", "🫶", "Custom Care", "Just checking in to make sure you're feeling okay ❤️", Color(0xFFE11D48))
    )

    fun byKey(key: String?): CarePresetOption =
        presets.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: presets.first()
}

object VibeEmojiCatalog {
    val emojis: List<String> = listOf("🎶", "🌙", "❤️‍🔥", "🌧️", "🚗", "🌅", "💃", "🎧")
}

data class PromiseCategoryOption(
    val key: String,
    val emoji: String,
    val label: String
)

object PromiseCatalog {
    val categories: List<PromiseCategoryOption> = listOf(
        PromiseCategoryOption("TRUST", "🤝", "Trust"),
        PromiseCategoryOption("ROMANCE", "🌹", "Romance"),
        PromiseCategoryOption("QUALITY_TIME", "🕰️", "Us Time"),
        PromiseCategoryOption("GROWTH", "🌱", "Growth"),
        PromiseCategoryOption("FUN", "🎉", "Playful")
    )

    val starters: List<Pair<String, String>> = listOf(
        "Never go to sleep angry without a hug or soft word" to "TRUST",
        "One phone-free date evening every single week" to "QUALITY_TIME",
        "Always say good morning and goodnight no matter how busy" to "ROMANCE",
        "Celebrate each other's tiny wins like big milestones" to "GROWTH",
        "Random surprise ice-cream or chai runs once a month" to "FUN"
    )

    fun byKey(key: String?): PromiseCategoryOption =
        categories.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: categories.first()
}

data class CoupleQuizQuestion(
    val id: String,
    val question: String,
    val optionA: String,
    val optionB: String
)

object CoupleQuizCatalog {
    val questions: List<CoupleQuizQuestion> = listOf(
        CoupleQuizQuestion("q1", "Ideal rainy evening together?", "Chai + Blanket Cinema 🎬", "Midnight Music Drive 🚗"),
        CoupleQuizQuestion("q2", "How do we apologize best?", "Long Silent Hug 🫂", "Sweet Handwritten Note 💌"),
        CoupleQuizQuestion("q3", "Dream surprise gift?", "Planned Weekend Getaway ✈️", "Memory Scrapbook Box 📸"),
        CoupleQuizQuestion("q4", "Favorite love language?", "Quality Undivided Time 🕰️", "Sweet Words & Touch ❤️"),
        CoupleQuizQuestion("q5", "Sunday morning ritual?", "Slow Coffee in Bed ☕", "Sunrise Walk Together 🌅")
    )
}
