package com.example

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import android.graphics.Bitmap
import android.util.Base64
import com.example.crypto.DuetCryptoManager
import com.example.crypto.PhotoCompressor
import com.example.data.AppConfigUrlValidator
import com.example.data.AppVersionComparator
import com.example.data.BONDLY_ADMIN_PACKAGE_ID
import com.example.data.BucketListItem
import com.example.data.CareCheckIn
import com.example.data.CouplePromise
import com.example.data.CoupleSpace
import com.example.data.DailyPromptDeck
import com.example.data.DecryptedMessageItem
import com.example.data.EncryptedMessage
import com.example.data.LiveAppConfig
import com.example.data.LoveDoodle
import com.example.data.LoveJarNote
import com.example.data.LoveLetter
import com.example.data.MoodEntry
import com.example.data.PartnerBannedNotice
import com.example.data.PartnerPresence
import com.example.data.UserProfile
import com.example.data.VibeDedication
import com.example.notifications.DuetNotificationStateStore
import com.example.telemetry.DeviceSpecsCollector
import com.example.telemetry.DeviceSpecsSnapshot
import com.example.ui.DuetUiState
import com.example.ui.SessionStage
import com.example.ui.components.AccountSuspendedScreen
import com.example.ui.components.AppearanceStudioSheet
import com.example.ui.components.BucketListSheet
import com.example.ui.components.CareCheckInSheet
import com.example.ui.components.ChatSettingsSheet
import com.example.ui.components.CouplePromisesSheet
import com.example.ui.components.DateSpinnerSheet
import com.example.ui.components.DeviceDetailsSheet
import com.example.ui.components.DuetBrandLogoBadge
import com.example.ui.components.DuetSettingsSheet
import com.example.ui.components.ForceUpdateDialog
import com.example.ui.components.LoveDoodleSheet
import com.example.ui.components.LoveGamesSheet
import com.example.ui.components.LoveJarSheet
import com.example.ui.components.LoveLettersSheet
import com.example.ui.components.MaintenanceModeScreen
import com.example.ui.components.MoodHistorySheet
import com.example.ui.components.OfflineConnectivityPrompt
import com.example.ui.components.OptionalUpdateDialog
import com.example.ui.components.PartnerStatusSheet
import com.example.ui.components.PhotoLightboxDialog
import com.example.ui.components.RemovePartnerWarningSheet
import com.example.ui.components.SongVibeDedicationSheet
import com.example.ui.components.TopAnnouncementBanner
import com.example.ui.components.TopPromoBanner
import com.example.ui.components.TopUpdateAvailableBanner
import com.example.ui.components.UpcomingMaintenanceBanner
import com.example.ui.components.WidgetStudioSheet
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.DatesScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MomentsScreen
import com.example.ui.screens.PairingScreen
import com.example.ui.theme.AppIconPreset
import com.example.ui.theme.AppThemePreset
import com.example.ui.theme.ChatWallpaperPreset
import com.example.ui.theme.DuetAppearanceState
import com.example.ui.theme.DuetTheme
import com.example.widget.DuetWidgetSnapshot
import com.example.widget.WidgetStateStore
import com.example.widget.WidgetThemeStyle
import java.io.ByteArrayOutputStream
import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `read app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Duet", appName)
    }

    @Test
    fun `appearance studio shows shining exclusive themes and switches themes safely`() {
        var feedback = ""
        composeTestRule.setContent {
            DuetTheme(themePreset = AppThemePreset.BLUSH_ROSE) {
                Column {
                    AppIconPreset.entries.forEach { preset ->
                        DuetBrandLogoBadge(
                            appearance = DuetAppearanceState(iconPreset = preset)
                        )
                    }
                    AppearanceStudioSheet(
                        appearance = DuetAppearanceState(),
                        onFeedback = { feedback = it },
                        onDismiss = {}
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Theme, Logo & Icon Studio").assertIsDisplayed()
        composeTestRule.onNodeWithText("✦ EXCLUSIVE").assertIsDisplayed()
        composeTestRule.onNodeWithTag("app_theme_royal_gold").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertTrue(feedback.contains("Royal Obsidian"))

        composeTestRule.onNodeWithTag("app_theme_velvet").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertTrue(feedback.contains("Velvet Dusk"))
    }

    @Test
    fun `partner status sheet auto refreshes without refresh button or placeholder text`() {
        var autoRefreshCount = 0
        val selfPresence = PartnerPresence(
            userId = "self-uid",
            displayName = "Sam",
            statusShared = true,
            batteryPercent = 76L,
            charging = false,
            networkType = "WIFI",
            deviceStatus = "ACTIVE_IN_DUET",
            activeAppName = "Duet"
        )

        composeTestRule.setContent {
            DuetTheme(themePreset = AppThemePreset.ROYAL_OBSIDIAN) {
                PartnerStatusSheet(
                    accountName = "Sam",
                    isSelf = true,
                    isPaired = true,
                    inviteCode = "123456",
                    presence = selfPresence,
                    hasUsageAccessPermission = true,
                    onDismiss = {},
                    onSendLovePulse = {},
                    onRemindCharge = {},
                    onAutoRefresh = { autoRefreshCount++ },
                    onShareCode = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        assertTrue("Expected auto-refresh to trigger on open", autoRefreshCount >= 1)
        composeTestRule.onNodeWithText("Refresh Live Device Status").assertDoesNotExist()
        composeTestRule.onNodeWithText("Refreshing automatically", substring = true, ignoreCase = true)
            .assertDoesNotExist()
    }

    @Test
    fun `home screen renders dark exclusive theme live presence with partner nickname and battery`() {
        val couple = CoupleSpace(
            id = "couple-1",
            inviteCode = "654321",
            creatorUid = "u1",
            partnerUid = "u2",
            memberUids = listOf("u1", "u2"),
            creatorName = "Sam",
            partnerName = "Aria",
            status = "paired"
        )
        val uiState = DuetUiState(
            stage = SessionStage.Ready,
            currentUserId = "u1",
            profile = UserProfile(userId = "u1", displayName = "Sam", partnerNickname = "My Sunshine"),
            couple = couple,
            myPresence = PartnerPresence(
                userId = "u1",
                displayName = "Sam",
                statusShared = true,
                batteryPercent = 91L,
                networkType = "WIFI",
                deviceStatus = "ACTIVE_IN_DUET"
            ),
            partnerPresence = PartnerPresence(
                userId = "u2",
                displayName = "Aria",
                statusShared = true,
                batteryPercent = 84L,
                charging = true,
                networkType = "CELLULAR",
                deviceStatus = "IN_USE",
                activeAppName = "Instagram"
            )
        )

        composeTestRule.setContent {
            DuetTheme(themePreset = AppThemePreset.ROYAL_OBSIDIAN) {
                HomeScreen(
                    state = uiState,
                    appearance = DuetAppearanceState(themePreset = AppThemePreset.ROYAL_OBSIDIAN),
                    onTapPartnerAccount = {},
                    onTapMyAccount = {},
                    onShareMood = { _, _ -> },
                    onSendComfortHug = {},
                    onSendLovePulse = {},
                    onSubmitDailyAnswer = {},
                    onOpenWidgetStudio = {},
                    onOpenAppearanceStudio = {},
                    onOpenDatesTab = {},
                    onOpenSettings = {},
                    onCopyInviteCode = {},
                    onShareInviteCode = {},
                    onRequestNotificationPermission = {},
                    onOpenUsageAccessSettings = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("partner_account_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("partner_battery_pill", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("My Sunshine", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("84%", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun `chat screen displays settings button nickname reactions and delete overlay`() {
        val multiReactionMsg = EncryptedMessage(
            id = "m1",
            senderUid = "u2",
            senderName = "Aria",
            reactions = mapOf("u1" to "HEART", "u2" to "FIRE")
        )
        assertEquals(mapOf("u1" to "HEART", "u2" to "FIRE"), multiReactionMsg.effectiveReactions())

        val couple = CoupleSpace(
            id = "couple-1",
            inviteCode = "654321",
            creatorUid = "u1",
            partnerUid = "u2",
            memberUids = listOf("u1", "u2"),
            creatorName = "Sam",
            partnerName = "Aria",
            status = "paired"
        )
        val uiState = DuetUiState(
            stage = SessionStage.Ready,
            currentUserId = "u1",
            profile = UserProfile(userId = "u1", displayName = "Sam", partnerNickname = "Sweetheart"),
            couple = couple,
            messages = listOf(
                DecryptedMessageItem(raw = multiReactionMsg, text = "Good night love!", decryptedOk = true)
            )
        )

        var openedSettings = false
        var deletedMessageId = ""

        composeTestRule.setContent {
            DuetTheme(themePreset = AppThemePreset.AURORA_CYBER) {
                ChatScreen(
                    state = uiState,
                    appearance = DuetAppearanceState(
                        themePreset = AppThemePreset.AURORA_CYBER,
                        chatWallpaper = ChatWallpaperPreset.MIDNIGHT_AURORA
                    ),
                    onSendText = {},
                    onSendPhoto = { _, _, _ -> },
                    onSendHeartPulse = {},
                    onReactMessage = { _, _, _ -> },
                    onDeleteMessage = { deletedMessageId = it },
                    onTypingChanged = {},
                    onMarkMessagesSeen = {},
                    onOpenEncryptionInfo = {},
                    onOpenChatSettings = { openedSettings = true },
                    onTapPartnerHeader = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Sweetheart").assertIsDisplayed()
        composeTestRule.onNodeWithTag("chat_settings_button").performClick()
        assertTrue(openedSettings)

        // Tap reaction pill to inspect reactions & trigger Delete Message
        composeTestRule.onNodeWithTag("message_reaction_pill_m1").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("message_reaction_picker_bar").assertIsDisplayed()
        composeTestRule.onNodeWithTag("delete_chat_message_action").assertIsDisplayed().performClick()
        assertEquals("m1", deletedMessageId)
    }

    @Test
    fun `chat settings sheet saves partner nickname and switches chat wallpaper`() {
        var savedNickname = ""
        var wallpaperFeedback = ""

        composeTestRule.setContent {
            DuetTheme(themePreset = AppThemePreset.AURORA_CYBER) {
                ChatSettingsSheet(
                    partnerOriginalName = "Aria",
                    currentPartnerNickname = "Sweetheart",
                    appearance = DuetAppearanceState(themePreset = AppThemePreset.AURORA_CYBER),
                    onSavePartnerNickname = { savedNickname = it },
                    onOpenEncryptionInfo = {},
                    onOpenAppearanceStudio = {},
                    onFeedback = { wallpaperFeedback = it },
                    onDismiss = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Private Chat Settings").assertIsDisplayed()
        composeTestRule.onNodeWithTag("partner_nickname_input").performTextReplacement("My Angel")
        composeTestRule.onNodeWithTag("save_partner_nickname_button").performClick()
        assertEquals("My Angel", savedNickname)

        composeTestRule.onNodeWithTag("chat_wallpaper_pick_photo_button").performScrollTo()
        composeTestRule.onNodeWithTag("chat_wallpaper_royal_gold_weave").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertTrue(wallpaperFeedback.contains("Obsidian Gold"))
    }

    @Test
    fun `device specs collector produces valid compact json within firestore limit`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val collected = DeviceSpecsCollector.collect(context)

        assertTrue(collected.androidVersion.startsWith("Android"))
        assertTrue(collected.apiLevel.contains("API"))
        assertTrue(collected.duetAppVersion.contains("1617"))
        assertTrue(collected.imeiStatus.isNotBlank())

        val json = collected.toCompactJson()
        assertTrue("Expected compact JSON <= 4096 chars, got ${json.length}", json.length <= 4096)

        val restored = DeviceSpecsSnapshot.fromJson(json)
        assertNotNull(restored)
        assertEquals(collected.deviceName, restored?.deviceName)
        assertEquals(collected.androidVersion, restored?.androidVersion)
        assertEquals(collected.securityPatch, restored?.securityPatch)
        assertEquals(collected.duetAppVersion, restored?.duetAppVersion)

        val sections = restored!!.toSections()
        assertEquals(5, sections.size)
        assertTrue(sections.all { it.items.isNotEmpty() })

        val report = restored.toPlainTextReport("My Device")
        assertTrue(report.contains("Device Name:"))
        assertTrue(report.contains("Security Patch Update:"))
    }

    @Test
    fun `device details sheet displays self and partner shared device details`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val mySpecs = DeviceSpecsCollector.collect(context)
        val partnerSpecs = DeviceSpecsSnapshot(
            deviceName = "Pixel 9 Pro",
            manufacturer = "Google",
            brand = "Google",
            model = "Pixel 9 Pro",
            loggedInAccount = "partner@example.com",
            androidVersion = "Android 16",
            apiLevel = "API 36",
            securityPatch = "2026-05-05",
            duetAppVersion = "v1617 (build 1)",
            imeiStatus = "OS-Protected on Android 16 (Dial *#06#)"
        )
        val partnerPresence = PartnerPresence(
            userId = "partner-uid",
            displayName = "Aria",
            statusShared = true,
            deviceName = partnerSpecs.deviceName,
            deviceSpecsJson = partnerSpecs.toCompactJson()
        )

        composeTestRule.setContent {
            DuetTheme {
                DeviceDetailsSheet(
                    myName = "Sam",
                    partnerName = "Aria",
                    isPaired = true,
                    initialShowSelf = true,
                    myDeviceSpecs = mySpecs,
                    partnerPresence = partnerPresence,
                    shareDeviceStatus = true,
                    onRefreshMine = {},
                    onFeedback = {},
                    onDismiss = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Device Details (A to Z)").assertIsDisplayed()
        composeTestRule.onNodeWithTag("device_details_tab_partner").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Partner Logged In • partner@example.com").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Pixel 9 Pro").onFirst().assertIsDisplayed()
    }

    @Test
    fun `offline connectivity prompt appears on disconnect auto dismisses on reconnect and reappears`() {
        var onlineState by mutableStateOf(true)

        composeTestRule.setContent {
            DuetTheme(themePreset = AppThemePreset.CELESTIAL_SAPPHIRE) {
                OfflineConnectivityPrompt(isOnline = onlineState)
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("no_internet_prompt").assertDoesNotExist()

        // First disconnect -> prompt appears immediately
        onlineState = false
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("no_internet_prompt").assertIsDisplayed()
        composeTestRule.onNodeWithText("No Internet Connection").assertIsDisplayed()

        // Reconnect -> offline prompt auto-exits immediately
        onlineState = true
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("no_internet_prompt").assertDoesNotExist()

        // Second disconnect while app is still open -> prompt appears again!
        onlineState = false
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("no_internet_prompt").assertIsDisplayed()
    }

    @Test
    fun `chat screen shows sent or seen receipt strictly once after outgoing message`() {
        val couple = CoupleSpace(
            id = "couple-1",
            inviteCode = "654321",
            creatorUid = "u1",
            partnerUid = "u2",
            memberUids = listOf("u1", "u2"),
            creatorName = "Sam",
            partnerName = "Aria",
            status = "paired"
        )
        var seenByPartner by mutableStateOf<String?>(null)

        composeTestRule.setContent {
            val outgoing = EncryptedMessage(
                id = "m_out",
                senderUid = "u1",
                senderName = "Sam",
                seenByUid = seenByPartner
            )
            val uiState = DuetUiState(
                stage = SessionStage.Ready,
                currentUserId = "u1",
                couple = couple,
                messages = listOf(
                    DecryptedMessageItem(raw = outgoing, text = "Thinking about you!", decryptedOk = true)
                )
            )
            DuetTheme(themePreset = AppThemePreset.SOLARIS_ROSE_GOLD) {
                ChatScreen(
                    state = uiState,
                    appearance = DuetAppearanceState(themePreset = AppThemePreset.SOLARIS_ROSE_GOLD),
                    onSendText = {},
                    onSendPhoto = { _, _, _ -> },
                    onSendHeartPulse = {},
                    onReactMessage = { _, _, _ -> },
                    onTypingChanged = {},
                    onMarkMessagesSeen = {},
                    onOpenEncryptionInfo = {},
                    onTapPartnerHeader = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("chat_latest_receipt_label").assertIsDisplayed()
        assertEquals(
            "Expected Sent status strictly once (never duplicated inside bubble)",
            1,
            composeTestRule.onAllNodesWithText("Sent", useUnmergedTree = true).fetchSemanticsNodes().size
        )

        // Partner sees message -> Seen appears strictly once after message
        seenByPartner = "u2"
        composeTestRule.waitForIdle()
        assertEquals(
            "Expected Seen status strictly once (never duplicated inside bubble)",
            1,
            composeTestRule.onAllNodesWithText("Seen", substring = true, useUnmergedTree = true).fetchSemanticsNodes().size
        )
    }

    @Test
    fun `daily prompt expiry countdown flags last hour before midnight IST accurately`() {
        val ist = TimeZone.getTimeZone("Asia/Kolkata")
        val fortyFiveMinBeforeMidnight = Calendar.getInstance(ist).apply {
            set(2026, Calendar.MAY, 15, 23, 15, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val urgent = DailyPromptDeck.computeExpiryCountdown(fortyFiveMinBeforeMidnight)
        assertTrue(urgent.isExpiringSoon)
        assertEquals(0L, urgent.hours)
        assertEquals(45L, urgent.minutes)

        val twoAndHalfHoursLeft = Calendar.getInstance(ist).apply {
            set(2026, Calendar.MAY, 15, 21, 30, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val normal = DailyPromptDeck.computeExpiryCountdown(twoAndHalfHoursLeft)
        assertFalse(normal.isExpiringSoon)
        assertEquals(2L, normal.hours)
        assertEquals(30L, normal.minutes)
    }

    @Test
    fun `mood history sheet filters partner entries and sends warm hug`() {
        var huggedId = ""
        val moods = listOf(
            MoodEntry(id = "m_self", userId = "u1", authorName = "Sam", moodKey = "IN_LOVE", note = "Missing your smile", energyLevel = 5L),
            MoodEntry(id = "m_partner", userId = "u2", authorName = "Aria", moodKey = "NEED_HUGS", note = "Long day at work", energyLevel = 2L)
        )

        composeTestRule.setContent {
            DuetTheme(themePreset = AppThemePreset.CRIMSON_RUBY) {
                MoodHistorySheet(
                    moods = moods,
                    myUid = "u1",
                    partnerName = "Aria",
                    onSendComfortHug = { huggedId = it },
                    onDismiss = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Mood History & Insights").assertIsDisplayed()
        composeTestRule.onNodeWithTag("mood_filter_partner").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("mood_history_hug_m_partner").assertIsDisplayed().performClick()
        assertEquals("m_partner", huggedId)
    }

    @Test
    fun `bucket list sheet displays dreams and adds new shared dream`() {
        var addedTitle = ""
        var addedCategory = ""
        val existing = listOf(
            BucketListItem(
                id = "b1",
                coupleId = "c1",
                creatorUid = "u2",
                creatorName = "Aria",
                title = "Watch Northern Lights",
                category = "TRAVEL",
                completed = false
            )
        )

        composeTestRule.setContent {
            DuetTheme(themePreset = AppThemePreset.NEON_LOTUS) {
                BucketListSheet(
                    items = existing,
                    partnerName = "Aria",
                    isBusy = false,
                    onAddItem = { t, c ->
                        addedTitle = t
                        addedCategory = c
                    },
                    onToggleCompleted = {},
                    onDeleteItem = {},
                    onDismiss = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Couple Bucket List").assertIsDisplayed()
        composeTestRule.onNodeWithTag("bucket_list_input").performTextInput("Midnight chai drive")
        composeTestRule.onNodeWithTag("bucket_list_add_button").performClick()
        assertEquals("Midnight chai drive", addedTitle)
        assertEquals("DATE_NIGHT", addedCategory)

        composeTestRule.onNodeWithTag("bucket_list_lazy_column")
            .performScrollToNode(hasTestTag("bucket_item_b1"))
        composeTestRule.onNodeWithText("Watch Northern Lights", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun `date spinner and love games sheets send romantic invites to private chat`() {
        var sentInvite = ""
        composeTestRule.setContent {
            DuetTheme(themePreset = AppThemePreset.SOLARIS_ROSE_GOLD) {
                DateSpinnerSheet(
                    partnerName = "Aria",
                    onSendInviteToChat = { sentInvite = it },
                    onDismiss = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Date Night Spinner").assertIsDisplayed()
        composeTestRule.onNodeWithTag("date_spinner_send_invite_button").performScrollTo().performClick()
        assertTrue(sentInvite.contains("Date Night Invite"))
    }

    @Test
    fun `love games sheet switches between truth deck and love coupons`() {
        var sentCoupon = ""
        composeTestRule.setContent {
            DuetTheme(themePreset = AppThemePreset.CELESTIAL_SAPPHIRE) {
                LoveGamesSheet(
                    partnerName = "Aria",
                    onSendToPrivateChat = { sentCoupon = it },
                    onDismiss = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Love Deck & Coupons").assertIsDisplayed()
        composeTestRule.onNodeWithTag("games_tab_coupons").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("coupon_gift_breakfast").assertIsDisplayed().performClick()
        assertTrue(sentCoupon.contains("Breakfast in Bed"))
    }

    @Test
    fun `new radiant exclusive themes appear in appearance studio and apply cleanly`() {
        var lastFeedback = ""
        composeTestRule.setContent {
            DuetTheme(themePreset = AppThemePreset.BLUSH_ROSE) {
                AppearanceStudioSheet(
                    appearance = DuetAppearanceState(),
                    onFeedback = { lastFeedback = it },
                    onDismiss = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("app_theme_solaris").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertTrue(lastFeedback.contains("Solaris Rose Gold"))

        composeTestRule.onNodeWithTag("app_theme_lotus").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertTrue(lastFeedback.contains("Neon Lotus"))
    }

    @Test
    fun `private chat renders rounded header and compact settings button under exclusive theme`() {
        val couple = CoupleSpace(
            id = "couple-1",
            inviteCode = "654321",
            creatorUid = "u1",
            partnerUid = "u2",
            memberUids = listOf("u1", "u2"),
            creatorName = "Sam",
            partnerName = "Aria",
            status = "paired"
        )
        val uiState = DuetUiState(
            stage = SessionStage.Ready,
            currentUserId = "u1",
            couple = couple
        )

        composeTestRule.setContent {
            DuetTheme(themePreset = AppThemePreset.ROYAL_OBSIDIAN) {
                ChatScreen(
                    state = uiState,
                    appearance = DuetAppearanceState(themePreset = AppThemePreset.ROYAL_OBSIDIAN),
                    onSendText = {},
                    onSendPhoto = { _, _, _ -> },
                    onSendHeartPulse = {},
                    onReactMessage = { _, _, _ -> },
                    onTypingChanged = {},
                    onMarkMessagesSeen = {},
                    onOpenEncryptionInfo = {},
                    onOpenChatSettings = {},
                    onTapPartnerHeader = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("chat_rounded_header").assertIsDisplayed()
        composeTestRule.onNodeWithTag("chat_settings_button").assertIsDisplayed()
    }

    @Test
    fun `hd photo lightbox displays sender timestamp hd badge and save hd download button`() {
        val sampleBitmap = Bitmap.createBitmap(64, 48, Bitmap.Config.ARGB_8888)
        val out = ByteArrayOutputStream()
        sampleBitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
        val sampleBase64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        sampleBitmap.recycle()

        val dims = PhotoCompressor.inspectImageDimensions(sampleBase64)
        assertEquals(64 to 48, dims)

        composeTestRule.setContent {
            DuetTheme(themePreset = AppThemePreset.AURORA_CYBER) {
                PhotoLightboxDialog(
                    imageBase64 = sampleBase64,
                    caption = "Sunset coffee date",
                    subtitle = "Sent by Aria • May 15 • 6:40 PM IST",
                    senderLabel = "Sent by Aria",
                    sentAtLabel = "May 15 • 6:40 PM IST",
                    locationLabel = "Marine Drive",
                    onDismiss = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("lightbox_download_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("lightbox_sender_text").assertIsDisplayed()
        composeTestRule.onNodeWithTag("lightbox_timestamp_text").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sent by Aria").assertIsDisplayed()
        composeTestRule.onNodeWithText("May 15 • 6:40 PM IST").assertIsDisplayed()
        composeTestRule.onNodeWithText("Save HD").assertIsDisplayed()
    }

    @Test
    fun `love letters sheet composes and seals a new love letter`() {
        var sentLetterTitle = ""
        composeTestRule.setContent {
            DuetTheme(themePreset = AppThemePreset.ROYAL_OBSIDIAN) {
                LoveLettersSheet(
                    letters = listOf(
                        LoveLetter(
                            id = "l1",
                            coupleId = "c1",
                            senderUid = "u2",
                            senderName = "Aria",
                            title = "Open on our anniversary",
                            body = "Forever yours!",
                            sealStyle = "ROYAL_GOLD"
                        )
                    ),
                    myUid = "u1",
                    partnerName = "Aria",
                    isBusy = false,
                    onSendLetter = { title, _, _, _ -> sentLetterTitle = title },
                    onOpenLetter = {},
                    onDeleteLetter = {},
                    onDismiss = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("love_letter_title_input").performScrollTo().performTextInput("My Secret Note")
        composeTestRule.onNodeWithTag("love_letter_body_input").performScrollTo().performTextInput("You make every day brighter.")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("love_letter_send_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertEquals("My Secret Note", sentLetterTitle)
    }

    @Test
    fun `love jar sheet drops a new shared wish into the jar`() {
        var addedJarText = ""
        composeTestRule.setContent {
            DuetTheme(themePreset = AppThemePreset.SOLARIS_ROSE_GOLD) {
                LoveJarSheet(
                    notes = listOf(
                        LoveJarNote(id = "j1", coupleId = "c1", authorUid = "u2", authorName = "Aria", kind = "WISH", text = "Stargazing night")
                    ),
                    myUid = "u1",
                    partnerName = "Aria",
                    isBusy = false,
                    onAddNote = { _, text -> addedJarText = text },
                    onToggleGranted = {},
                    onDeleteNote = {},
                    onDismiss = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("love_jar_input").performScrollTo().performTextInput("Cook pasta together")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("love_jar_add_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertEquals("Cook pasta together", addedJarText)
    }

    @Test
    fun `song vibe dedication sheet dedicates a song to partner`() {
        var dedicatedSong = ""
        composeTestRule.setContent {
            DuetTheme(themePreset = AppThemePreset.CRIMSON_RUBY) {
                SongVibeDedicationSheet(
                    dedications = listOf(
                        VibeDedication(id = "v1", coupleId = "c1", senderUid = "u2", senderName = "Aria", songTitle = "Perfect", artistName = "Ed Sheeran")
                    ),
                    myUid = "u1",
                    partnerName = "Aria",
                    isBusy = false,
                    onDedicateSong = { song, _, _, _ -> dedicatedSong = song },
                    onToggleLoved = {},
                    onDeleteDedication = {},
                    onDismiss = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("vibe_song_input").performScrollTo().performTextInput("Kesariya")
        composeTestRule.onNodeWithTag("vibe_artist_input").performScrollTo().performTextInput("Arijit Singh")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("vibe_dedicate_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertEquals("Kesariya", dedicatedSong)
    }

    @Test
    fun `care checkin sheet sends quick preset and acknowledges partner nudge`() {
        var sentCareType = ""
        var ackedCareId = ""
        composeTestRule.setContent {
            DuetTheme(themePreset = AppThemePreset.CELESTIAL_SAPPHIRE) {
                CareCheckInSheet(
                    checkIns = listOf(
                        CareCheckIn(id = "c1", coupleId = "cp1", senderUid = "u2", senderName = "Aria", careType = "WATER", message = "Drink water!")
                    ),
                    myUid = "u1",
                    partnerName = "Aria",
                    isBusy = false,
                    onSendCheckIn = { type, _ -> sentCareType = type },
                    onAcknowledge = { ackedCareId = it },
                    onDismiss = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("care_preset_water").performClick()
        assertEquals("WATER", sentCareType)
        composeTestRule.onNodeWithTag("care_checkin_sheet")
            .performScrollToNode(hasTestTag("care_ack_button_c1"))
        composeTestRule.onNodeWithTag("care_ack_button_c1").performClick()
        assertEquals("c1", ackedCareId)
    }

    @Test
    fun `love doodle sheet displays interactive drawing canvas`() {
        composeTestRule.setContent {
            DuetTheme(themePreset = AppThemePreset.NEON_LOTUS) {
                LoveDoodleSheet(
                    doodles = emptyList(),
                    myUid = "u1",
                    partnerName = "Aria",
                    isBusy = false,
                    onSendDoodle = { _, _ -> },
                    onToggleLoved = {},
                    onDeleteDoodle = {},
                    onFeedback = {},
                    onDismiss = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("doodle_canvas").assertIsDisplayed()
    }

    @Test
    fun `couple promises sheet creates and co-signs pinky promises`() {
        var addedPromise = ""
        var coSignedId = ""
        composeTestRule.setContent {
            DuetTheme(themePreset = AppThemePreset.ROYAL_OBSIDIAN) {
                CouplePromisesSheet(
                    promises = listOf(
                        CouplePromise(
                            id = "p1",
                            coupleId = "cp1",
                            creatorUid = "u2",
                            creatorName = "Aria",
                            title = "Always say goodnight",
                            category = "LOVE",
                            signedByUids = listOf("u2")
                        )
                    ),
                    myUid = "u1",
                    partnerName = "Aria",
                    bondScorePoints = 420,
                    isBusy = false,
                    onAddPromise = { title, _ -> addedPromise = title },
                    onCoSignPromise = { coSignedId = it.id },
                    onDeletePromise = {},
                    onShareQuizResultToChat = {},
                    onDismiss = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("promise_input").performTextInput("Weekly chai date")
        composeTestRule.onNodeWithTag("promise_add_button").performClick()
        assertEquals("Weekly chai date", addedPromise)
        composeTestRule.onNodeWithTag("couple_promises_sheet")
            .performScrollToNode(hasTestTag("promise_cosign_button_p1"))
        composeTestRule.onNodeWithTag("promise_cosign_button_p1").performClick()
        assertEquals("p1", coSignedId)
    }

    @Test
    fun `widget state store persists all extended widget fields and studio renders 8 widgets`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = WidgetStateStore(context)
        val expected = DuetWidgetSnapshot(
            coupleId = "c-88",
            coupleTitle = "Sam & Aria",
            partnerName = "Aria",
            batteryBadge = "92% ⚡",
            daysTogetherLabel = "365 Days Together",
            anniversaryCountdown = "In 12 days",
            birthdayCountdown = "In 40 days",
            dailyQuestionText = "What moment made you smile today?",
            dailyQuestionStatus = "Partner answered • Unlock now",
            dailyQuestionExpiry = "Expires in 3h 20m",
            partnerNetwork = "Wi-Fi",
            partnerActiveApp = "Active in Instagram",
            partnerDeviceName = "Pixel 9 Pro",
            loveJarHighlight = "WISH: Beach sunrise",
            songVibeHighlight = "🎵 Kesariya • Arijit Singh",
            bucketSummary = "4/10 Dreams Done",
            themeStyle = WidgetThemeStyle.ROYAL_GOLD
        )
        store.saveSnapshot(expected)
        val restored = store.readSnapshot()
        assertEquals("365 Days Together", restored.daysTogetherLabel)
        assertEquals("In 12 days", restored.anniversaryCountdown)
        assertEquals("In 40 days", restored.birthdayCountdown)
        assertEquals("What moment made you smile today?", restored.dailyQuestionText)
        assertEquals("Active in Instagram", restored.partnerActiveApp)
        assertEquals("WISH: Beach sunrise", restored.loveJarHighlight)
        assertEquals("🎵 Kesariya • Arijit Singh", restored.songVibeHighlight)
        assertEquals("4/10 Dreams Done", restored.bucketSummary)
        assertEquals(WidgetThemeStyle.ROYAL_GOLD, restored.themeStyle)

        composeTestRule.setContent {
            DuetTheme(themePreset = AppThemePreset.ROYAL_OBSIDIAN) {
                WidgetStudioSheet(
                    coupleTitle = "Sam & Aria",
                    partnerName = "Aria",
                    batteryBadge = "92% ⚡",
                    moodLine = "🥰 In Love",
                    countdownLine = "In 12 days",
                    statusFooter = "Active in Instagram",
                    lastMsgPreview = "Good morning love!",
                    selectedTheme = WidgetThemeStyle.ROYAL_GOLD,
                    onSelectTheme = {},
                    onPinResult = { _, _ -> },
                    onDismiss = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("8 Interactive Home Widgets").assertIsDisplayed()
        composeTestRule.onNodeWithTag("widget_studio_sheet")
            .performScrollToNode(hasTestTag("pin_countdown_widget_button"))
        composeTestRule.onNodeWithTag("pin_countdown_widget_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("widget_studio_sheet")
            .performScrollToNode(hasTestTag("pin_hub_widget_button"))
        composeTestRule.onNodeWithTag("pin_hub_widget_button").assertIsDisplayed()
    }

    @Test
    fun `crypto manager rotates keypair and normalizes couple safety number digits`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val crypto = DuetCryptoManager(context, "rotate-test-user")
        val initialKey = crypto.getMyPublicKeyBase64()
        val rotatedKey = crypto.rotateKeyPair()

        assertTrue(initialKey.isNotBlank())
        assertTrue(rotatedKey.isNotBlank())
        assertNotEquals(initialKey, rotatedKey)
        assertEquals(rotatedKey, crypto.getMyPublicKeyBase64())

        val formatted = "48291 03948 11223 44556 7788"
        assertEquals("482910394811223445567788", DuetCryptoManager.normalizeSafetyNumberDigits(formatted))
        assertEquals("482910394811223445567788", DuetCryptoManager.normalizeSafetyNumberDigits("48291-03948 11223-44556 7788"))
    }

    @Test
    fun `widget resetToUnpaired and notification store clear couple data cleanly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val widgetStore = WidgetStateStore(context)
        widgetStore.saveSnapshot(
            DuetWidgetSnapshot(
                coupleId = "couple-wipe",
                coupleTitle = "Sam & Aria",
                partnerName = "Aria",
                lastMsg1 = "Aria: Miss you!",
                anniversaryCountdown = "In 12 days",
                loveJarHighlight = "WISH: Stargazing",
                songVibeHighlight = "🎵 Kesariya",
                bucketSummary = "4/10 Dreams Done",
                themeStyle = WidgetThemeStyle.ROYAL_GOLD
            )
        )

        widgetStore.resetToUnpaired()
        val cleared = widgetStore.readSnapshot()
        assertEquals("", cleared.coupleId)
        assertEquals("Duet", cleared.coupleTitle)
        assertEquals("Private Chat", cleared.partnerName)
        assertEquals("No private messages yet", cleared.lastMsg1)
        assertEquals(WidgetThemeStyle.ROYAL_GOLD, cleared.themeStyle)

        val notifStore = DuetNotificationStateStore(context)
        notifStore.setLastExcludedInviteCode("482917")
        assertEquals("482917", notifStore.getLastExcludedInviteCode())
        notifStore.setPendingRemovalNotice("Aria removed the partner link.")
        assertEquals("Aria removed the partner link.", notifStore.consumePendingRemovalNotice())
        assertNull(notifStore.consumePendingRemovalNotice())
    }

    @Test
    fun `remove partner warning sheet shows data loss warning and gates confirmation on safety number`() {
        val safetyNumber = "12345 67890 11223 34455 6677"
        var confirmedInput = ""

        composeTestRule.setContent {
            DuetTheme(themePreset = AppThemePreset.ROYAL_OBSIDIAN) {
                RemovePartnerWarningSheet(
                    partnerName = "Aria",
                    isPaired = true,
                    inviteCode = "482917",
                    safetyNumber = safetyNumber,
                    isBusy = false,
                    onConfirmRemove = { confirmedInput = it },
                    onDismiss = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Remove as Partner")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("remove_partner_warning_card")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("confirm_remove_partner_button")
            .performScrollTo()
            .assertIsNotEnabled()

        // Wrong safety number keeps destructive reset disabled
        composeTestRule.onNodeWithTag("remove_partner_safety_input")
            .performScrollTo()
            .performTextInput("99999")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("confirm_remove_partner_button")
            .performScrollTo()
            .assertIsNotEnabled()

        // Entering exact Couple Safety Number (without spaces) enables destructive reset
        composeTestRule.onNodeWithTag("remove_partner_safety_input")
            .performScrollTo()
            .performTextReplacement("123456789011223344556677")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("remove_partner_safety_verified")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("confirm_remove_partner_button")
            .performScrollTo()
            .assertIsEnabled()
            .performClick()

        assertEquals("123456789011223344556677", confirmedInput)
    }

    @Test
    fun `settings sheet shows Remove as Partner option and 8 Widgets label`() {
        var openedRemovePartner = false
        composeTestRule.setContent {
            DuetTheme(themePreset = AppThemePreset.BLUSH_ROSE) {
                DuetSettingsSheet(
                    myNickname = "Sam",
                    partnerName = "Aria",
                    inviteCode = "482917",
                    isPaired = true,
                    shareDeviceStatus = true,
                    backgroundSyncEnabled = true,
                    hasNotificationPermission = true,
                    hasUsageAccessPermission = true,
                    onUpdateNickname = {},
                    onToggleShareStatus = {},
                    onToggleBackgroundSync = {},
                    onRequestNotificationPermission = {},
                    onSendTestNotification = {},
                    onOpenUsageAccessSettings = {},
                    onOpenBatterySettings = {},
                    onCopyCode = {},
                    onShareCode = {},
                    onOpenEncryptionInfo = {},
                    onOpenRemovePartner = { openedRemovePartner = true },
                    onSignOut = {},
                    onDismiss = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("8 Widgets").assertIsDisplayed()
        composeTestRule.onNodeWithTag("settings_remove_partner_button")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        assertTrue(openedRemovePartner)
    }

    @Test
    fun `pairing screen opens directly on Enter Code tab when initialJoinTab is true`() {
        composeTestRule.setContent {
            DuetTheme(themePreset = AppThemePreset.BLUSH_ROSE) {
                PairingScreen(
                    defaultNickname = "Sam",
                    isBusy = false,
                    bannerMessage = "Partner removed. All shared data reset.",
                    initialJoinTab = true,
                    onCreateSpace = {},
                    onJoinSpace = { _, _ -> },
                    onSignOut = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("join_couple_code_input")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Partner removed. All shared data reset.")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `live app config version comparator, url validator, and ban helpers work accurately`() {
        assertTrue(AppVersionComparator.isCurrentLowerThanMin("1617", 1617, "1618"))
        assertTrue(AppVersionComparator.isCurrentLowerThanMin("1617", 1617, "1617.1"))
        assertTrue(AppVersionComparator.isCurrentLowerThanMin("1617", 1617, "v1620"))
        assertFalse(AppVersionComparator.isCurrentLowerThanMin("1617", 1617, "1617"))
        assertFalse(AppVersionComparator.isCurrentLowerThanMin("1617", 1617, "1616"))
        assertFalse(AppVersionComparator.isCurrentLowerThanMin("1617", 1617, ""))

        val configRequiresUpdate = LiveAppConfig(
            forceUpdate = true,
            minVersion = "1618",
            updateLink = "https://play.google.com/store/apps/details?id=com.aistudio.duet.kvrxqm"
        )
        assertTrue(configRequiresUpdate.isUpdateRequired("1617", 1617))
        // Even without forceUpdate flag, a newer available version blocks old versions
        assertTrue(configRequiresUpdate.copy(forceUpdate = false).isUpdateRequired("1617", 1617))
        // New version (1618+) remains usable
        assertFalse(configRequiresUpdate.isUpdateRequired("1618", 1618))

        assertTrue(AppConfigUrlValidator.isSafeWebUrl("https://duet.app/update"))
        assertTrue(AppConfigUrlValidator.isSafeWebUrl("http://status.duet.app"))
        assertFalse(AppConfigUrlValidator.isSafeWebUrl("javascript:alert(1)"))
        assertFalse(AppConfigUrlValidator.isSafeWebUrl("file:///etc/passwd"))
        assertFalse(AppConfigUrlValidator.isSafeWebUrl("not-a-url"))

        val bannedByStatus = UserProfile(status = "Banned", banReason = "Abuse report")
        assertTrue(bannedByStatus.isAccountBanned())
        assertEquals("Abuse report", bannedByStatus.resolvedBanReason())

        val bannedByFlag = UserProfile(isBanned = true, reason = "Spamming")
        assertTrue(bannedByFlag.isAccountBanned())
        assertEquals("Spamming", bannedByFlag.resolvedBanReason())

        val activeUser = UserProfile(status = "active", isBanned = false)
        assertFalse(activeUser.isAccountBanned())
    }

    @Test
    fun `account suspended screen displays admin ban reason and triggers account switch`() {
        var clickedSwitch = false
        composeTestRule.setContent {
            DuetTheme(themePreset = AppThemePreset.ROYAL_OBSIDIAN) {
                AccountSuspendedScreen(
                    reason = "Violated community guidelines",
                    onTryDifferentAccount = { clickedSwitch = true }
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("banned_account_screen").assertIsDisplayed()
        composeTestRule.onNodeWithText("Account Suspended").assertIsDisplayed()
        composeTestRule.onNodeWithText("You have been banned by the admin.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Violated community guidelines").assertIsDisplayed()
        composeTestRule.onNodeWithTag("banned_try_other_account_button")
            .performScrollTo()
            .performClick()
        assertTrue(clickedSwitch)
    }

    @Test
    fun `maintenance screen displays admin maintenance message`() {
        composeTestRule.setContent {
            DuetTheme(themePreset = AppThemePreset.BLUSH_ROSE) {
                MaintenanceModeScreen(message = "Upgrading encryption servers until 2:00 AM IST.")
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("maintenance_mode_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("maintenance_message_text").assertIsDisplayed()
        composeTestRule.onNodeWithText("Upgrading encryption servers until 2:00 AM IST.").assertIsDisplayed()
    }

    @Test
    fun `top announcement banner renders clickable link and dismisses cleanly`() {
        var openedBannerUrl = ""
        var dismissedBanner = false
        composeTestRule.setContent {
            DuetTheme(themePreset = AppThemePreset.BLUSH_ROSE) {
                TopAnnouncementBanner(
                    visible = true,
                    bannerText = "New Valentine themes unlocked!",
                    bannerLink = "https://duet.example.com/news",
                    onOpenLink = { openedBannerUrl = it },
                    onDismiss = { dismissedBanner = true }
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("live_announcement_banner").assertIsDisplayed()
        composeTestRule.onNodeWithText("New Valentine themes unlocked!").assertIsDisplayed()
        composeTestRule.onNodeWithTag("announcement_banner_link_button").performClick()
        assertEquals("https://duet.example.com/news", openedBannerUrl)
        composeTestRule.onNodeWithTag("announcement_banner_dismiss_button").performClick()
        assertTrue(dismissedBanner)
    }

    @Test
    fun `force update dialog displays Update Available, Download Now, and triggers admin update link`() {
        var openedUpdateUrl = ""
        composeTestRule.setContent {
            DuetTheme(themePreset = AppThemePreset.BLUSH_ROSE) {
                ForceUpdateDialog(
                    currentVersion = "1617",
                    minVersion = "1618",
                    updateMessage = "Security patch required.",
                    updateLink = "https://bondly.example.com/download/duet-1618.apk",
                    onOpenUpdateLink = { openedUpdateUrl = it },
                    releaseTitle = "Duet v1618"
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("force_update_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithText("Update Available").assertIsDisplayed()
        composeTestRule.onNodeWithText("Download Now").assertIsDisplayed()
        composeTestRule.onNodeWithText("Security patch required.").assertIsDisplayed()
        composeTestRule.onAllNodesWithTag("optional_update_later_button").assertCountEquals(0)
        composeTestRule.onNodeWithTag("force_update_link_button").performClick()
        assertEquals("https://bondly.example.com/download/duet-1618.apk", openedUpdateUrl)
    }

    @Test
    fun `bondly admin package id and multi-doc live app config merge handle optional updates cleanly`() {
        assertEquals("com.aistudio.bondlyadmin.vzrqnp", BONDLY_ADMIN_PACKAGE_ID)

        val live = LiveAppConfig(
            maintenanceMode = false,
            showBanner = false
        )
        val maintenance = LiveAppConfig(
            upcomingMaintenance = true,
            upcomingMaintenanceText = "Brief server upgrade tonight at 11:45 PM IST."
        )
        val updates = LiveAppConfig(
            forceUpdate = false,
            showUpdatePopup = true,
            showUpdateBanner = true,
            latestVersion = "1620",
            updateTitle = "Spring Glow Release",
            updateMessage = "Faster chat delivery and new maintenance alerts.",
            updateLink = "https://play.google.com/store/apps/details?id=com.aistudio.duet.kvrxqm"
        )
        val banners = LiveAppConfig(
            showBanner = true,
            bannerTitle = "Bondly Notice",
            bannerText = "Welcome to Duet 2.0!",
            bannerType = "update"
        )

        val merged = LiveAppConfig.mergeConfigs(live, maintenance, updates, banners)
        assertFalse(merged.maintenanceMode)
        assertTrue(merged.upcomingMaintenance)
        assertEquals("Brief server upgrade tonight at 11:45 PM IST.", merged.upcomingMaintenanceText)
        assertTrue(merged.showBanner)
        assertEquals("Bondly Notice", merged.bannerTitle)
        assertTrue(merged.isNewerOptionalVersion("1617", 1617))
        // Once an update is pushed/available, old version 1617 is blocked while new version 1620 is allowed
        assertTrue(merged.isForceUpdateRequired("1617", 1617))
        assertFalse(merged.isForceUpdateRequired("1620", 1620))
        assertTrue(merged.hasAnyUpdateForNotification("1617", 1617))
        assertEquals("1620", merged.notificationUpdateVersion("1617"))
        assertEquals(BONDLY_ADMIN_PACKAGE_ID, merged.resolvedAdminPackageId())
    }

    @Test
    fun `optional update dialog shows release notes and triggers update or later callbacks`() {
        var clickedUpdateUrl = ""
        var clickedLater = false
        composeTestRule.setContent {
            DuetTheme(themePreset = AppThemePreset.BLUSH_ROSE) {
                OptionalUpdateDialog(
                    currentVersion = "1617",
                    newVersion = "1620",
                    title = "Spring Glow Update",
                    releaseNotes = "Instant background notifications & faster HD photos.",
                    updateLink = "https://play.google.com/store/apps/details?id=com.aistudio.duet.kvrxqm",
                    onOpenUpdateLink = { clickedUpdateUrl = it },
                    onDismissLater = { clickedLater = true }
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("optional_update_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("optional_update_version_badge").assertIsDisplayed()
        composeTestRule.onNodeWithText("Instant background notifications & faster HD photos.").assertIsDisplayed()
        composeTestRule.onNodeWithTag("optional_update_now_button").performClick()
        assertEquals("https://play.google.com/store/apps/details?id=com.aistudio.duet.kvrxqm", clickedUpdateUrl)
        composeTestRule.onNodeWithTag("optional_update_later_button").performClick()
        assertTrue(clickedLater)
    }

    @Test
    fun `update available banner and upcoming maintenance banner display and trigger actions`() {
        var openedPopup = false
        var dismissedUpdateBanner = false
        var dismissedUpcoming = false
        composeTestRule.setContent {
            DuetTheme(themePreset = AppThemePreset.CELESTIAL_SAPPHIRE) {
                Column {
                    UpcomingMaintenanceBanner(
                        visible = true,
                        message = "Scheduled server tune-up at 2:00 AM IST.",
                        onDismiss = { dismissedUpcoming = true }
                    )
                    TopUpdateAvailableBanner(
                        visible = true,
                        versionLabel = "1620",
                        title = "Duet v1620 Out Now",
                        summary = "Includes new widgets & stability boosts.",
                        onOpenPopup = { openedPopup = true },
                        onDismiss = { dismissedUpdateBanner = true }
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("upcoming_maintenance_banner").assertIsDisplayed()
        composeTestRule.onNodeWithTag("app_update_banner").assertIsDisplayed()
        composeTestRule.onNodeWithTag("app_update_banner_open_button").performClick()
        assertTrue(openedPopup)
        composeTestRule.onNodeWithTag("app_update_banner_dismiss_button").performClick()
        assertTrue(dismissedUpdateBanner)
        composeTestRule.onNodeWithTag("upcoming_maintenance_dismiss_button").performClick()
        assertTrue(dismissedUpcoming)
    }

    @Test
    fun `maintenance screen displays custom title, eta badge, and bondly admin package label`() {
        composeTestRule.setContent {
            DuetTheme(themePreset = AppThemePreset.ROYAL_OBSIDIAN) {
                MaintenanceModeScreen(
                    message = "Database tuning in progress.",
                    title = "Scheduled Care",
                    eta = "15 mins",
                    adminPackageLabel = BONDLY_ADMIN_PACKAGE_ID
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("maintenance_mode_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("maintenance_title_text").assertIsDisplayed()
        composeTestRule.onNodeWithText("Scheduled Care").assertIsDisplayed()
        composeTestRule.onNodeWithTag("maintenance_eta_badge").assertIsDisplayed()
        composeTestRule.onNodeWithText("Estimated return: 15 mins", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun `settings sheet shows Bondly updates card and companion admin button when installed`() {
        var openedUpdateDetails = false
        var openedAdminCompanion = false
        val updateConfig = LiveAppConfig(
            showUpdatePopup = true,
            latestVersion = "1622",
            updateTitle = "Duet v1622"
        )
        composeTestRule.setContent {
            DuetTheme(themePreset = AppThemePreset.BLUSH_ROSE) {
                DuetSettingsSheet(
                    myNickname = "Sam",
                    partnerName = "Aria",
                    inviteCode = "482917",
                    isPaired = true,
                    shareDeviceStatus = true,
                    backgroundSyncEnabled = true,
                    hasNotificationPermission = true,
                    hasUsageAccessPermission = true,
                    liveAppConfig = updateConfig,
                    isAdminAppInstalled = true,
                    onUpdateNickname = {},
                    onToggleShareStatus = {},
                    onToggleBackgroundSync = {},
                    onRequestNotificationPermission = {},
                    onSendTestNotification = {},
                    onOpenUsageAccessSettings = {},
                    onOpenBatterySettings = {},
                    onCopyCode = {},
                    onShareCode = {},
                    onOpenUpdateDetails = { openedUpdateDetails = true },
                    onOpenAdminCompanion = { openedAdminCompanion = true },
                    onOpenEncryptionInfo = {},
                    onSignOut = {},
                    onDismiss = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("settings_updates_card")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("settings_view_update_button")
            .performScrollTo()
            .performClick()
        assertTrue(openedUpdateDetails)
        composeTestRule.onNodeWithTag("settings_open_admin_button")
            .performScrollTo()
            .performClick()
        assertTrue(openedAdminCompanion)
    }

    @Test
    fun `LiveAppConfig sparse merge handles banners promo feature flags version control and dynamic KV`() {
        val docs = listOf(
            LiveAppConfig.parseMapWithPresence(
                docId = "live",
                collectionHint = "app_config",
                data = mapOf(
                    "announcement" to mapOf(
                        "enabled" to true,
                        "title" to "Scheduled Announcement",
                        "message" to "",
                        "link" to "https://bondly.example.com/news"
                    ),
                    "promoBanner" to mapOf(
                        "active" to true,
                        "badge" to "VIP",
                        "title" to "Weekend Theme Drop",
                        "text" to "Try Royal Obsidian tonight",
                        "ctaText" to "Open",
                        "link" to "https://bondly.example.com/promo",
                        "color" to "gold"
                    )
                )
            ),
            LiveAppConfig.parseMapWithPresence(
                docId = "shared_journal",
                collectionHint = "feature_flags",
                data = mapOf("key" to "shared_journal", "enabled" to false)
            ),
            LiveAppConfig.parseMapWithPresence(
                docId = "date_planner",
                collectionHint = "feature_flags",
                data = mapOf("featureKey" to "datePlanner", "isEnabled" to false)
            ),
            LiveAppConfig.parseMapWithPresence(
                docId = "mood_sync",
                collectionHint = "feature_flags",
                data = mapOf("name" to "moodSync", "active" to false)
            ),
            LiveAppConfig.parseMapWithPresence(
                docId = "daily_prompt_check",
                collectionHint = "feature_flags",
                data = mapOf("id" to "dailyPromptCheck", "enabled" to false)
            ),
            LiveAppConfig.parseMapWithPresence(
                docId = "android",
                collectionHint = "version_control",
                data = mapOf(
                    "minVersion" to "2.0.0",
                    "latestVersion" to "2.1.0",
                    "forceUpdate" to true,
                    "blockedVersions" to listOf("1617")
                )
            ),
            LiveAppConfig.parseMapWithPresence(
                docId = "welcome_message",
                collectionHint = "dynamic_config",
                data = mapOf(
                    "key" to "welcome_message",
                    "value" to "Bondly Holiday Week is Live!"
                )
            )
        )

        val merged = LiveAppConfig.mergeParsedDocs(docs)
        assertTrue(merged.shouldShowAnnouncementBanner())
        assertEquals("Scheduled Announcement", merged.resolvedBannerText())
        assertTrue(merged.shouldShowPromoBanner())
        assertEquals("VIP", merged.promoBadge)
        assertEquals("gold", merged.promoColor)
        assertFalse(merged.sharedJournalEnabled)
        assertFalse(merged.datePlannerEnabled)
        assertFalse(merged.moodSyncEnabled)
        assertFalse(merged.dailyPromptEnabled)
        assertTrue(merged.isForceUpdateRequired("1617", 1617))
        assertTrue(merged.isVersionBlocked("1617", 1617))
        assertEquals("Bondly Holiday Week is Live!", merged.dynamicHomeNotice())

        // Semver version check against numeric build string "1617"
        assertTrue(AppVersionComparator.isCurrentLowerThanMin("1617", 1617, "1.0.1"))
        assertTrue(AppVersionComparator.isCurrentLowerThanMin("1617", 1617, "2.0.0"))
        assertFalse(AppVersionComparator.isCurrentLowerThanMin("1617", 1617, "1.0.0"))
        assertTrue(AppVersionComparator.isCurrentLowerThanMin("1617", 1617, "1618"))
    }

    @Test
    fun `top announcement and promo banners render title-only announcements and trigger promo CTA`() {
        var promoOpenedUrl = ""
        var promoDismissed = false
        composeTestRule.setContent {
            DuetTheme(themePreset = AppThemePreset.BLUSH_ROSE) {
                Column {
                    TopAnnouncementBanner(
                        visible = true,
                        bannerText = "",
                        bannerTitle = "Title Only Announcement",
                        bannerLink = "",
                        onOpenLink = {},
                        onDismiss = {}
                    )
                    TopPromoBanner(
                        visible = true,
                        promoTitle = "Love Week Offer",
                        promoText = "Exclusive Velvet theme unlocked!",
                        promoBadge = "PROMO",
                        promoCtaText = "Check It",
                        promoLink = "https://bondly.example.com/offer",
                        promoColor = "purple",
                        onOpenLink = { promoOpenedUrl = it },
                        onDismiss = { promoDismissed = true }
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("live_announcement_banner").assertIsDisplayed()
        composeTestRule.onNodeWithText("Title Only Announcement").assertIsDisplayed()
        composeTestRule.onNodeWithTag("live_promo_banner").assertIsDisplayed()
        composeTestRule.onNodeWithTag("promo_banner_badge", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("promo_banner_cta_button").performClick()
        assertEquals("https://bondly.example.com/offer", promoOpenedUrl)
        composeTestRule.onNodeWithTag("promo_banner_dismiss_button").performClick()
        assertTrue(promoDismissed)
    }

    @Test
    fun `home screen hides paused features shows disabled cards and shows dynamic notice`() {
        val pausedConfig = LiveAppConfig(
            sharedJournalEnabled = false,
            datePlannerEnabled = false,
            moodSyncEnabled = false,
            dailyPromptEnabled = false,
            dynamicConfig = mapOf("welcome_message" to "Server-side Valentine Week Tips")
        )
        val couple = CoupleSpace(
            id = "couple_flags",
            inviteCode = "123456",
            creatorUid = "u1",
            creatorName = "Maya",
            partnerUid = "u2",
            partnerName = "Liam",
            status = "paired",
            memberUids = listOf("u1", "u2")
        )
        val uiState = DuetUiState(
            stage = SessionStage.Ready,
            currentUserId = "u1",
            couple = couple
        )

        composeTestRule.setContent {
            DuetTheme(themePreset = AppThemePreset.BLUSH_ROSE) {
                HomeScreen(
                    state = uiState,
                    liveAppConfig = pausedConfig,
                    onTapPartnerAccount = {},
                    onTapMyAccount = {},
                    onShareMood = { _, _ -> },
                    onSendComfortHug = {},
                    onSendLovePulse = {},
                    onSubmitDailyAnswer = {},
                    onOpenWidgetStudio = {},
                    onOpenAppearanceStudio = {},
                    onOpenDatesTab = {},
                    onOpenSettings = {},
                    onCopyInviteCode = {},
                    onShareInviteCode = {},
                    onRequestNotificationPermission = {},
                    onOpenUsageAccessSettings = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("home_dynamic_notice_card").assertIsDisplayed()
        composeTestRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("feature_disabled_mood_sync"))
        composeTestRule.onNodeWithTag("feature_disabled_mood_sync").assertIsDisplayed()
        composeTestRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("feature_disabled_daily_prompt"))
        composeTestRule.onNodeWithTag("feature_disabled_daily_prompt").assertIsDisplayed()
        composeTestRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("feature_disabled_shared_journal_banner"))
        composeTestRule.onNodeWithTag("feature_disabled_shared_journal_banner").assertIsDisplayed()
        composeTestRule.onAllNodesWithTag("open_love_letters_tile").assertCountEquals(0)
        composeTestRule.onAllNodesWithTag("open_date_spinner_tile").assertCountEquals(0)
    }

    @Test
    fun `moments and dates screens display paused state when turned off via feature flags`() {
        val couple = CoupleSpace(
            id = "couple_tabs",
            inviteCode = "654321",
            creatorUid = "u1",
            creatorName = "Maya",
            partnerUid = "u2",
            partnerName = "Liam",
            status = "paired",
            memberUids = listOf("u1", "u2")
        )
        val uiState = DuetUiState(
            stage = SessionStage.Ready,
            currentUserId = "u1",
            couple = couple
        )

        composeTestRule.setContent {
            DuetTheme(themePreset = AppThemePreset.BLUSH_ROSE) {
                Column {
                    MomentsScreen(
                        state = uiState,
                        showInternalFab = true,
                        featureEnabled = false,
                        onUploadMoment = { _, _, _, _ -> },
                        onToggleLove = {},
                        onDeleteMoment = {}
                    )
                    DatesScreen(
                        state = uiState,
                        showInternalFab = true,
                        featureEnabled = false,
                        onUpdateAnniversary = { _, _, _ -> },
                        onUpdateBirthday = { _, _, _ -> },
                        onAddSpecialEvent = { _, _, _, _, _, _ -> },
                        onDeleteSpecialEvent = {}
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("feature_disabled_shared_journal").assertIsDisplayed()
        composeTestRule.onAllNodesWithTag("moments_upload_fab").assertCountEquals(0)
        composeTestRule.onAllNodesWithTag("add_special_date_fab").assertCountEquals(0)
    }

    @Test
    fun `settings sheet shows dynamic key value config and feature flag status`() {
        val config = LiveAppConfig(
            sharedJournalEnabled = false,
            datePlannerEnabled = true,
            moodSyncEnabled = false,
            dailyPromptEnabled = true,
            minVersion = "2.0.0",
            dynamicConfig = mapOf(
                "welcome_message" to "Hello from Bondly Admin",
                "max_daily_pulses" to "50"
            )
        )
        composeTestRule.setContent {
            DuetTheme(themePreset = AppThemePreset.BLUSH_ROSE) {
                DuetSettingsSheet(
                    myNickname = "Sam",
                    partnerName = "Aria",
                    inviteCode = "482917",
                    isPaired = true,
                    shareDeviceStatus = true,
                    backgroundSyncEnabled = true,
                    hasNotificationPermission = true,
                    hasUsageAccessPermission = true,
                    liveAppConfig = config,
                    isAdminAppInstalled = false,
                    onUpdateNickname = {},
                    onToggleShareStatus = {},
                    onToggleBackgroundSync = {},
                    onRequestNotificationPermission = {},
                    onSendTestNotification = {},
                    onOpenUsageAccessSettings = {},
                    onOpenBatterySettings = {},
                    onCopyCode = {},
                    onShareCode = {},
                    onOpenEncryptionInfo = {},
                    onSignOut = {},
                    onDismiss = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("settings_feature_flags_section").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("settings_version_control_summary").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("settings_dynamic_config_section").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Hello from Bondly Admin").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `maintenance mode stays active when unrelated OTA docs have newer timestamps or missing updatedAt`() {
        val docs = listOf(
            LiveAppConfig.parseMapWithPresence(
                docId = "live",
                collectionHint = "maintenance",
                updatedAt = null,
                data = mapOf(
                    "enabled" to true,
                    "title" to "Scheduled Maintenance",
                    "message" to "Server maintenance in progress."
                )
            ),
            LiveAppConfig.parseMapWithPresence(
                docId = "live",
                collectionHint = "app_config",
                updatedAt = com.google.firebase.Timestamp(java.util.Date(System.currentTimeMillis() + 60_000L)),
                data = mapOf(
                    "maintenanceMode" to false,
                    "showBanner" to true,
                    "bannerText" to "Holiday offer"
                )
            ),
            LiveAppConfig.parseMapWithPresence(
                docId = "live",
                collectionHint = "updates",
                updatedAt = com.google.firebase.Timestamp(java.util.Date(System.currentTimeMillis() + 120_000L)),
                data = mapOf(
                    "updateAvailable" to true,
                    "downloadLink" to "https://bondly.example.com/duet-latest.apk"
                )
            )
        )

        val merged = LiveAppConfig.mergeParsedDocs(docs)
        assertTrue(merged.maintenanceMode)
        assertEquals("Server maintenance in progress.", merged.maintenanceMessage)
        assertTrue(merged.showBanner)
        assertTrue(merged.isForceUpdateRequired("1617", 1617))
        assertEquals("https://bondly.example.com/duet-latest.apk", merged.effectiveUpdateLink("com.aistudio.duet.kvrxqm"))
    }

    @Test
    fun `suspended account screen renders live reactivation hint and refresh status button`() {
        var refreshedCount = 0
        var signedOutCount = 0
        composeTestRule.setContent {
            DuetTheme(themePreset = AppThemePreset.BLUSH_ROSE) {
                AccountSuspendedScreen(
                    reason = "Policy review in progress",
                    isRefreshing = false,
                    onRefreshStatus = { refreshedCount++ },
                    onTryDifferentAccount = { signedOutCount++ }
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("banned_account_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("banned_live_hint").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("banned_refresh_status_button").performScrollTo().assertIsDisplayed().performClick()
        assertEquals(1, refreshedCount)
        composeTestRule.onNodeWithTag("banned_try_other_account_button").performScrollTo().performClick()
        assertEquals(1, signedOutCount)
    }

    @Test
    fun `home screen marks banned partner with admin reason and isolated wipe option`() {
        var removePartnerClicked = false
        val couple = CoupleSpace(
            id = "couple_partner_ban",
            inviteCode = "991122",
            creatorUid = "u1",
            creatorName = "Maya",
            partnerUid = "u2",
            partnerName = "Liam",
            status = "paired",
            memberUids = listOf("u1", "u2")
        )
        val banNotice = PartnerBannedNotice(
            partnerUid = "u2",
            partnerName = "Liam",
            reason = "Spam reports reviewed by Bondly Admin"
        )
        val uiState = DuetUiState(
            stage = SessionStage.Ready,
            currentUserId = "u1",
            couple = couple,
            partnerBanned = banNotice
        )

        composeTestRule.setContent {
            DuetTheme(themePreset = AppThemePreset.BLUSH_ROSE) {
                HomeScreen(
                    state = uiState,
                    onTapPartnerAccount = {},
                    onTapMyAccount = {},
                    onShareMood = { _, _ -> },
                    onSendComfortHug = {},
                    onSendLovePulse = {},
                    onSubmitDailyAnswer = {},
                    onOpenWidgetStudio = {},
                    onOpenAppearanceStudio = {},
                    onOpenDatesTab = {},
                    onOpenSettings = {},
                    onCopyInviteCode = {},
                    onShareInviteCode = {},
                    onRequestNotificationPermission = {},
                    onOpenUsageAccessSettings = {},
                    onOpenRemovePartner = { removePartnerClicked = true }
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("partner_account_card"))
        composeTestRule.onNodeWithTag("partner_banned_badge", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("partner_banned_notice_card"))
        composeTestRule.onNodeWithTag("partner_banned_notice_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("partner_banned_reason_text", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("partner_banned_remove_data_button").performClick()
        assertTrue(removePartnerClicked)
    }

    @Test
    fun `chat header displays banned badge when partner account is suspended`() {
        val couple = CoupleSpace(
            id = "couple_chat_ban",
            inviteCode = "991122",
            creatorUid = "u1",
            creatorName = "Maya",
            partnerUid = "u2",
            partnerName = "Liam",
            status = "paired",
            memberUids = listOf("u1", "u2")
        )
        val banNotice = PartnerBannedNotice(
            partnerUid = "u2",
            partnerName = "Liam",
            reason = "Spam reports reviewed by Bondly Admin"
        )
        val uiState = DuetUiState(
            stage = SessionStage.Ready,
            currentUserId = "u1",
            couple = couple,
            partnerBanned = banNotice
        )

        composeTestRule.setContent {
            DuetTheme(themePreset = AppThemePreset.BLUSH_ROSE) {
                ChatScreen(
                    state = uiState,
                    onSendText = {},
                    onSendPhoto = { _, _, _ -> },
                    onSendHeartPulse = {},
                    onReactMessage = { _, _, _ -> },
                    onTypingChanged = {},
                    onMarkMessagesSeen = {},
                    onOpenEncryptionInfo = {},
                    onTapPartnerHeader = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("chat_header_banned_badge", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun `partner status sheet displays banned badge admin reason and remove partner button`() {
        val banNotice = PartnerBannedNotice(
            partnerUid = "u2",
            partnerName = "Liam",
            reason = "Spam reports reviewed by Bondly Admin"
        )
        composeTestRule.setContent {
            DuetTheme(themePreset = AppThemePreset.BLUSH_ROSE) {
                PartnerStatusSheet(
                    accountName = "Liam",
                    isSelf = false,
                    isPaired = true,
                    inviteCode = "991122",
                    presence = null,
                    partnerBanNotice = banNotice,
                    onDismiss = {},
                    onSendLovePulse = {},
                    onRemindCharge = {},
                    onShareCode = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("partner_sheet_banned_badge")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("partner_sheet_banned_card")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("partner_sheet_remove_partner_button")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `user profile unban state clears account ban detection cleanly`() {
        val bannedProfile = UserProfile(
            id = "u2",
            userId = "u2",
            displayName = "Liam",
            status = "banned",
            isBanned = true,
            banReason = "Abuse"
        )
        assertTrue(bannedProfile.isAccountBanned())
        assertEquals("Abuse", bannedProfile.resolvedBanReason())

        val reactivatedProfile = bannedProfile.copy(
            status = "active",
            isBanned = false,
            banned = false,
            banReason = null
        )
        assertFalse(reactivatedProfile.isAccountBanned())
        assertNull(reactivatedProfile.resolvedBanReason())
    }
}
