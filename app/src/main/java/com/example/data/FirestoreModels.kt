package com.example.data

import android.net.Uri
import androidx.annotation.Keep
import com.example.telemetry.DeviceSpecsSnapshot
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

@Keep
data class BannedAccountNotice(
    val userId: String = "",
    val reason: String? = null
)

@Keep
data class PartnerBannedNotice(
    val partnerUid: String = "",
    val partnerName: String = "Your Partner",
    val reason: String? = null,
    val bannedAtMillis: Long? = null
)

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val avatarInitials: String = "DU",
    val publicKey: String = "",
    val coupleId: String? = null,
    val birthdayMonth: Int? = null,
    val birthdayDay: Int? = null,
    val birthdayYear: Int? = null,
    val shareDeviceStatus: Boolean = true,
    val partnerNickname: String? = null,
    val status: String? = null,
    @get:PropertyName("isBanned")
    @PropertyName("isBanned")
    val isBanned: Boolean = false,
    val banned: Boolean? = null,
    val banReason: String? = null,
    val reason: String? = null,
    val bannedAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun isAccountBanned(): Boolean {
        val normalizedStatus = status?.trim()?.lowercase()
        return isBanned ||
            banned == true ||
            normalizedStatus == "banned" ||
            normalizedStatus == "suspended"
    }

    fun resolvedBanReason(): String? =
        listOf(banReason, reason)
            .firstOrNull { !it.isNullOrBlank() }
            ?.trim()

    companion object {
        fun fromSnapshot(snap: DocumentSnapshot): UserProfile? {
            if (!snap.exists()) return null
            val base = snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: UserProfile(id = snap.id, userId = snap.getString("userId").orEmpty())
            val hasExplicitIsBanned = snap.contains("isBanned")
            val rawIsBanned = if (hasExplicitIsBanned) (snap.getBoolean("isBanned") ?: false) else base.isBanned
            val rawBanned = if (snap.contains("banned")) snap.getBoolean("banned") else base.banned
            val rawStatus = if (snap.contains("status")) snap.getString("status") else base.status
            val rawBanReason = snap.getString("banReason")
                ?: snap.getString("suspensionReason")
                ?: snap.getString("reason")
            val rawBannedAt = snap.getTimestamp("bannedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            return base.copy(
                id = snap.id,
                userId = base.userId.ifBlank { snap.id },
                status = rawStatus,
                isBanned = rawIsBanned,
                banned = rawBanned,
                banReason = rawBanReason ?: base.banReason,
                reason = snap.getString("reason") ?: base.reason,
                bannedAt = rawBannedAt ?: base.bannedAt
            )
        }
    }
}

@Keep
@IgnoreExtraProperties
data class PairCodeDoc(
    @DocumentId val id: String = "",
    val code: String = "",
    val coupleId: String = "",
    val creatorUid: String = "",
    val creatorName: String = "",
    val status: String = "open",
    val claimedByUid: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CoupleSpace(
    @DocumentId val id: String = "",
    val creatorUid: String = "",
    val partnerUid: String? = null,
    val memberUids: List<String> = emptyList(),
    val inviteCode: String = "",
    val status: String = "waiting",
    val creatorName: String = "",
    val partnerName: String? = null,
    val creatorPublicKey: String = "",
    val partnerPublicKey: String? = null,
    val wrappedVaultKey: String = "",
    val anniversaryYear: Int? = null,
    val anniversaryMonth: Int? = null,
    val anniversaryDay: Int? = null,
    val creatorBirthdayMonth: Int? = null,
    val creatorBirthdayDay: Int? = null,
    val partnerBirthdayMonth: Int? = null,
    val partnerBirthdayDay: Int? = null,
    val lovePulseCount: Long = 0L,
    val lastPulseByUid: String? = null,
    val lastPulseAt: Timestamp? = null,
    val dissolvedByUid: String? = null,
    val dissolvedByName: String? = null,
    val dissolvedAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun partnerDisplayName(myUid: String): String =
        if (creatorUid == myUid) partnerName?.takeIf { it.isNotBlank() } ?: "Waiting for Partner"
        else creatorName.ifBlank { "Your Partner" }

    fun resolvedPartnerName(myUid: String, customNickname: String?): String {
        val nick = customNickname?.trim().orEmpty()
        if (nick.isNotBlank() && status == "paired") return nick
        return partnerDisplayName(myUid)
    }

    fun myDisplayName(myUid: String): String =
        if (creatorUid == myUid) creatorName.ifBlank { "You" }
        else partnerName?.takeIf { it.isNotBlank() } ?: "You"

    fun otherUid(myUid: String): String? =
        if (creatorUid == myUid) partnerUid else creatorUid

    fun otherPublicKey(myUid: String): String? =
        if (creatorUid == myUid) partnerPublicKey else creatorPublicKey

    fun myBirthday(myUid: String): Pair<Int, Int>? {
        val m = if (creatorUid == myUid) creatorBirthdayMonth else partnerBirthdayMonth
        val d = if (creatorUid == myUid) creatorBirthdayDay else partnerBirthdayDay
        return if (m != null && d != null) m to d else null
    }

    fun partnerBirthday(myUid: String): Pair<Int, Int>? {
        val m = if (creatorUid == myUid) partnerBirthdayMonth else creatorBirthdayMonth
        val d = if (creatorUid == myUid) partnerBirthdayDay else creatorBirthdayDay
        return if (m != null && d != null) m to d else null
    }
}

@Keep
@IgnoreExtraProperties
data class PartnerPresence(
    @DocumentId val id: String = "",
    val userId: String = "",
    val coupleId: String = "",
    val memberUids: List<String> = emptyList(),
    val displayName: String = "",
    val batteryPercent: Long = -1L,
    val charging: Boolean = false,
    val networkType: String = "OFFLINE",
    val deviceStatus: String = "IN_USE",
    val activeAppName: String = "",
    val deviceName: String = "",
    val deviceSpecsJson: String = "",
    val moodKey: String = "IN_LOVE",
    val moodNote: String = "",
    val statusShared: Boolean = true,
    val typingActive: Boolean = false,
    @ServerTimestamp val typingUpdatedAt: Timestamp? = null,
    @ServerTimestamp val lastSeenAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun parsedDeviceSpecs(): DeviceSpecsSnapshot? =
        if (!statusShared) null else DeviceSpecsSnapshot.fromJson(deviceSpecsJson)

    fun loggedInDeviceLabel(): String {
        if (!statusShared || deviceStatus == "HIDDEN") return ""
        return deviceName.ifBlank { parsedDeviceSpecs()?.deviceName.orEmpty() }
    }

    fun isPartnerActivelyTyping(nowMillis: Long = System.currentTimeMillis()): Boolean {
        if (!typingActive) return false
        val tsMillis = (typingUpdatedAt ?: updatedAt)?.toDate()?.time ?: return true
        return (nowMillis - tsMillis) in -15_000L..12_000L
    }

    fun liveActivityLabel(): String {
        if (!statusShared || deviceStatus == "HIDDEN") return "Status Hidden"
        return when (deviceStatus) {
            "LOCKED" -> "Mobile Locked"
            "ACTIVE_IN_DUET" -> "Active in Duet"
            else -> if (activeAppName.isNotBlank()) "Active in $activeAppName" else "Mobile In Use"
        }
    }

    fun lastSeenIstLabel(): String {
        if (!statusShared || deviceStatus == "HIDDEN") return "Last seen hidden"
        if (deviceStatus == "ACTIVE_IN_DUET") return "Online in Duet"
        val ts = lastSeenAt ?: updatedAt
        return IndiaTimeFormatter.formatLastSeen(ts?.toDate())
    }
}

@Keep
@IgnoreExtraProperties
data class EncryptedMessage(
    @DocumentId val id: String = "",
    val coupleId: String = "",
    val senderUid: String = "",
    val senderName: String = "",
    val memberUids: List<String> = emptyList(),
    val messageType: String = "TEXT",
    val keyMode: String = "ECDH_P256",
    val ciphertext: String = "",
    val iv: String = "",
    val reaction: String? = null,
    val reactions: Map<String, String> = emptyMap(),
    val seenByUid: String? = null,
    @ServerTimestamp val seenAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun isSeenByOther(myUid: String): Boolean =
        !seenByUid.isNullOrBlank() && seenByUid != myUid

    /**
     * Returns active per-UID reactions map, gracefully falling back to legacy single-field reaction
     * if present on older messages.
     */
    fun effectiveReactions(): Map<String, String> {
        val clean = reactions.filterValues { it.isNotBlank() }
        if (clean.isNotEmpty()) return clean
        val legacy = reaction?.takeIf { it.isNotBlank() } ?: return emptyMap()
        val attributedUid = memberUids.firstOrNull { it != senderUid } ?: senderUid.ifBlank { "legacy" }
        return mapOf(attributedUid to legacy)
    }
}

data class DecryptedMessageItem(
    val raw: EncryptedMessage,
    val text: String = "",
    val imageBase64: String? = null,
    val decryptedOk: Boolean = true
)

@Keep
@IgnoreExtraProperties
data class MoodEntry(
    @DocumentId val id: String = "",
    val coupleId: String = "",
    val userId: String = "",
    val authorName: String = "",
    val memberUids: List<String> = emptyList(),
    val moodKey: String = "IN_LOVE",
    val note: String = "",
    val energyLevel: Long = 4L,
    val comfortSentByUid: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SharedMoment(
    @DocumentId val id: String = "",
    val coupleId: String = "",
    val uploaderUid: String = "",
    val uploaderName: String = "",
    val memberUids: List<String> = emptyList(),
    val keyMode: String = "ECDH_P256",
    val ciphertext: String = "",
    val iv: String = "",
    val lovedByUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class DecryptedMomentItem(
    val raw: SharedMoment,
    val caption: String = "",
    val locationTag: String = "",
    val imageBase64: String? = null,
    val decryptedOk: Boolean = true
)

@Keep
@IgnoreExtraProperties
data class SpecialDateEvent(
    @DocumentId val id: String = "",
    val coupleId: String = "",
    val creatorUid: String = "",
    val memberUids: List<String> = emptyList(),
    val title: String = "",
    val category: String = "MILESTONE",
    val year: Long = 2026L,
    val month: Long = 1L,
    val day: Long = 1L,
    val repeatsYearly: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DailyPromptEntry(
    @DocumentId val id: String = "",
    val coupleId: String = "",
    val dateKey: String = "",
    val promptIndex: Long = 0L,
    val memberUids: List<String> = emptyList(),
    val creatorAnswer: String? = null,
    val partnerAnswer: String? = null,
    val creatorAnsweredUid: String? = null,
    val partnerAnsweredUid: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class BucketListItem(
    @DocumentId val id: String = "",
    val coupleId: String = "",
    val creatorUid: String = "",
    val creatorName: String = "",
    val memberUids: List<String> = emptyList(),
    val title: String = "",
    val category: String = "DATE_NIGHT",
    val completed: Boolean = false,
    val completedByUid: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class LoveLetter(
    @DocumentId val id: String = "",
    val coupleId: String = "",
    val senderUid: String = "",
    val senderName: String = "",
    val memberUids: List<String> = emptyList(),
    val title: String = "",
    val body: String = "",
    val sealStyle: String = "WAX_ROSE",
    val unlockAtMillis: Long = 0L,
    val openedByPartner: Boolean = false,
    val openedAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun isUnlocked(nowMillis: Long = System.currentTimeMillis()): Boolean =
        unlockAtMillis <= 0L || nowMillis >= unlockAtMillis
}

@Keep
@IgnoreExtraProperties
data class LoveJarNote(
    @DocumentId val id: String = "",
    val coupleId: String = "",
    val authorUid: String = "",
    val authorName: String = "",
    val memberUids: List<String> = emptyList(),
    val kind: String = "APPRECIATION",
    val text: String = "",
    val granted: Boolean = false,
    val grantedByUid: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class VibeDedication(
    @DocumentId val id: String = "",
    val coupleId: String = "",
    val senderUid: String = "",
    val senderName: String = "",
    val memberUids: List<String> = emptyList(),
    val songTitle: String = "",
    val artistName: String = "",
    val vibeEmoji: String = "🎶",
    val dedicationNote: String = "",
    val lovedByPartner: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CareCheckIn(
    @DocumentId val id: String = "",
    val coupleId: String = "",
    val senderUid: String = "",
    val senderName: String = "",
    val memberUids: List<String> = emptyList(),
    val careType: String = "WATER",
    val message: String = "",
    val acknowledged: Boolean = false,
    val acknowledgedByUid: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class LoveDoodle(
    @DocumentId val id: String = "",
    val coupleId: String = "",
    val artistUid: String = "",
    val artistName: String = "",
    val memberUids: List<String> = emptyList(),
    val title: String = "",
    val imageBase64: String = "",
    val lovedByPartner: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CouplePromise(
    @DocumentId val id: String = "",
    val coupleId: String = "",
    val creatorUid: String = "",
    val creatorName: String = "",
    val memberUids: List<String> = emptyList(),
    val title: String = "",
    val category: String = "TRUST",
    val signedByUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun isFullyCoSigned(): Boolean = signedByUids.size >= 2
}

const val BONDLY_ADMIN_PACKAGE_ID = "com.aistudio.bondlyadmin.vzrqnp"

@Keep
data class ParsedLiveConfigDoc(
    val config: LiveAppConfig,
    val presentFields: Set<String> = emptySet(),
    val priority: Int = 0,
    val updatedAtMillis: Long = 0L,
    val collectionHint: String = "",
    val isMaintenanceDomain: Boolean = false,
    val isUpdateDomain: Boolean = false
)

@Keep
@IgnoreExtraProperties
data class LiveAppConfig(
    @DocumentId val id: String = "live",
    val maintenanceMode: Boolean = false,
    val maintenanceTitle: String = "",
    val maintenanceMessage: String = "",
    val maintenanceEta: String = "",
    val upcomingMaintenance: Boolean = false,
    val upcomingMaintenanceText: String = "",
    val showBanner: Boolean = false,
    val bannerTitle: String = "",
    val bannerText: String = "",
    val bannerLink: String = "",
    val bannerType: String = "info",
    val showPromoBanner: Boolean = false,
    val promoTitle: String = "",
    val promoText: String = "",
    val promoBadge: String = "",
    val promoCtaText: String = "",
    val promoLink: String = "",
    val promoColor: String = "rose",
    val sharedJournalEnabled: Boolean = true,
    val datePlannerEnabled: Boolean = true,
    val moodSyncEnabled: Boolean = true,
    val dailyPromptEnabled: Boolean = true,
    val forceUpdate: Boolean = false,
    val showUpdateBanner: Boolean = false,
    val showUpdatePopup: Boolean = false,
    val minVersion: String = "",
    val latestVersion: String = "",
    val minVersionCode: Int? = null,
    val latestVersionCode: Int? = null,
    val blockedVersions: List<String> = emptyList(),
    val updateTitle: String = "",
    val updateMessage: String = "",
    val updateLink: String = "",
    val dynamicConfig: Map<String, String> = emptyMap(),
    val adminPackageId: String = "",
    val updatedAt: Timestamp? = null
) {
    fun isVersionBlocked(currentVersionName: String, currentVersionCode: Int): Boolean {
        if (blockedVersions.isEmpty()) return false
        return AppVersionComparator.isVersionBlocked(
            currentVersionName = currentVersionName,
            currentVersionCode = currentVersionCode,
            blockedVersions = blockedVersions
        )
    }

    fun hasNewerVersion(currentVersionName: String, currentVersionCode: Int): Boolean {
        if (latestVersionCode != null && latestVersionCode > 0 && currentVersionCode < latestVersionCode) return true
        if (minVersionCode != null && minVersionCode > 0 && currentVersionCode < minVersionCode) return true
        if (latestVersion.isNotBlank() && AppVersionComparator.isCurrentLowerThanMin(
                currentVersionName = currentVersionName,
                currentVersionCode = currentVersionCode,
                minVersionRaw = latestVersion
            )
        ) return true
        if (minVersion.isNotBlank() && AppVersionComparator.isCurrentLowerThanMin(
                currentVersionName = currentVersionName,
                currentVersionCode = currentVersionCode,
                minVersionRaw = minVersion
            )
        ) return true
        return false
    }

    fun hasTargetVersionConfigured(): Boolean =
        minVersion.isNotBlank() ||
            latestVersion.isNotBlank() ||
            (minVersionCode != null && minVersionCode > 0) ||
            (latestVersionCode != null && latestVersionCode > 0)

    fun isForceUpdateRequired(currentVersionName: String, currentVersionCode: Int): Boolean {
        if (isVersionBlocked(currentVersionName, currentVersionCode)) return true
        if (hasNewerVersion(currentVersionName, currentVersionCode)) return true
        if (!hasTargetVersionConfigured() && (forceUpdate || showUpdatePopup || showUpdateBanner)) return true
        if (!forceUpdate) return false
        val codeRequired = minVersionCode != null && minVersionCode > 0 && currentVersionCode < minVersionCode
        if (codeRequired) return true
        val targetMin = minVersion.ifBlank { latestVersion }
        return AppVersionComparator.isCurrentLowerThanMin(
            currentVersionName = currentVersionName,
            currentVersionCode = currentVersionCode,
            minVersionRaw = targetMin
        )
    }

    /** Backward-compatible alias */
    fun isUpdateRequired(currentVersionName: String, currentVersionCode: Int): Boolean =
        isForceUpdateRequired(currentVersionName, currentVersionCode)

    fun targetOptionalVersion(): String =
        latestVersion.ifBlank { minVersion }
            .ifBlank { latestVersionCode?.takeIf { it > 0 }?.toString().orEmpty() }
            .ifBlank { minVersionCode?.takeIf { it > 0 }?.toString().orEmpty() }
            .trim()

    fun isNewerOptionalVersion(currentVersionName: String, currentVersionCode: Int): Boolean =
        hasNewerVersion(currentVersionName, currentVersionCode)

    fun shouldShowOptionalUpdatePopup(currentVersionName: String, currentVersionCode: Int): Boolean {
        if (isForceUpdateRequired(currentVersionName, currentVersionCode)) return false
        val hasNewer = isNewerOptionalVersion(currentVersionName, currentVersionCode)
        return (showUpdatePopup && (targetOptionalVersion().isNotBlank() || updateMessage.isNotBlank())) ||
            (hasNewer && !forceUpdate)
    }

    fun shouldShowOptionalUpdateBanner(currentVersionName: String, currentVersionCode: Int): Boolean {
        if (isForceUpdateRequired(currentVersionName, currentVersionCode)) return false
        val hasNewer = isNewerOptionalVersion(currentVersionName, currentVersionCode)
        return (showUpdateBanner && (targetOptionalVersion().isNotBlank() || updateMessage.isNotBlank() || updateTitle.isNotBlank())) ||
            (hasNewer && !forceUpdate)
    }

    fun hasAnyUpdateForNotification(currentVersionName: String, currentVersionCode: Int): Boolean =
        isForceUpdateRequired(currentVersionName, currentVersionCode) ||
            shouldShowOptionalUpdatePopup(currentVersionName, currentVersionCode) ||
            shouldShowOptionalUpdateBanner(currentVersionName, currentVersionCode)

    fun notificationUpdateVersion(currentVersionName: String): String =
        if (forceUpdate) {
            minVersion.ifBlank { latestVersion }
                .ifBlank { minVersionCode?.takeIf { it > 0 }?.toString().orEmpty() }
                .ifBlank { latestVersionCode?.takeIf { it > 0 }?.toString().orEmpty() }
                .ifBlank { currentVersionName }
        } else {
            targetOptionalVersion().ifBlank { currentVersionName }
        }

    fun effectiveMinVersionLabel(): String =
        latestVersion.ifBlank { minVersion }
            .ifBlank { latestVersionCode?.takeIf { it > 0 }?.toString().orEmpty() }
            .ifBlank { minVersionCode?.takeIf { it > 0 }?.toString().orEmpty() }

    fun resolvedBannerText(): String = bannerText.trim().ifBlank { bannerTitle.trim() }

    fun shouldShowAnnouncementBanner(): Boolean =
        showBanner && resolvedBannerText().isNotBlank()

    fun resolvedPromoText(): String = promoText.trim().ifBlank { promoTitle.trim() }

    fun shouldShowPromoBanner(): Boolean =
        showPromoBanner && resolvedPromoText().isNotBlank()

    fun resolvedAdminPackageId(): String =
        adminPackageId.trim().ifBlank { BONDLY_ADMIN_PACKAGE_ID }

    fun bannerSignature(): String =
        "${bannerTitle.trim()}|${bannerText.trim()}|${bannerLink.trim()}|${bannerType.trim()}"

    fun promoSignature(): String =
        "${promoBadge.trim()}|${promoTitle.trim()}|${promoText.trim()}|${promoCtaText.trim()}|${promoLink.trim()}|${promoColor.trim()}"

    fun upcomingMaintenanceSignature(): String = upcomingMaintenanceText.trim()

    fun updateSignature(): String =
        "$forceUpdate|$showUpdatePopup|$showUpdateBanner|$minVersion|$latestVersion|$minVersionCode|$latestVersionCode|${blockedVersions.joinToString(",")}|${updateTitle.trim()}|${updateMessage.trim()}|${updateLink.trim()}"

    fun dynamicConfigValue(vararg keys: String): String? {
        if (dynamicConfig.isEmpty()) return null
        val normalizedTargets = keys.map(::normalizeConfigKey)
        for (target in normalizedTargets) {
            dynamicConfig.entries.firstOrNull { normalizeConfigKey(it.key) == target }?.let { match ->
                if (match.value.isNotBlank()) return match.value.trim()
            }
        }
        return null
    }

    fun dynamicHomeNotice(): String =
        dynamicConfigValue(
            "welcome_message",
            "home_notice",
            "couple_tip",
            "daily_note",
            "notice",
            "banner_note",
            "promo_note"
        ).orEmpty()

    fun effectiveUpdateLink(packageName: String): String =
        listOf(updateLink, promoLink, bannerLink)
            .firstOrNull { AppConfigUrlValidator.isSafeWebUrl(it) }
            ?: "https://play.google.com/store/apps/details?id=$packageName"

    companion object {
        private val RESERVED_DOC_META_KEYS = setOf(
            "id", "updatedAt", "updated_at", "createdAt", "created_at", "adminPackageId", "admin_package_id"
        )

        private fun normalizeConfigKey(raw: String): String =
            raw.trim().lowercase().replace(Regex("[^a-z0-9]"), "")

        private fun asStringMap(raw: Any?): Map<String, Any?>? {
            if (raw !is Map<*, *>) return null
            val out = LinkedHashMap<String, Any?>()
            for ((k, v) in raw) {
                if (k is String) out[k] = v
            }
            return out
        }

        private fun parseBoolValue(raw: Any?): Boolean? = when (raw) {
            is Boolean -> raw
            is Number -> raw.toLong() != 0L
            is String -> when (raw.trim().lowercase()) {
                "true", "1", "yes", "on", "enabled", "active", "live", "published" -> true
                "false", "0", "no", "off", "disabled", "inactive", "paused" -> false
                else -> null
            }
            else -> null
        }

        private fun parseIntValue(raw: Any?): Int? = when (raw) {
            is Int -> raw
            is Long -> raw.toInt()
            is Double -> raw.toInt()
            is Number -> raw.toInt()
            is String -> raw.trim().toIntOrNull()
            else -> null
        }

        private fun parseStringValue(raw: Any?): String = when (raw) {
            is String -> raw.trim()
            is Long -> raw.toString()
            is Int -> raw.toString()
            is Double -> if (raw % 1.0 == 0.0) raw.toLong().toString() else raw.toString()
            is Number -> raw.toString()
            is Boolean -> raw.toString()
            else -> ""
        }

        private fun parseVersionValue(raw: Any?): String = when (raw) {
            is Long -> raw.toString()
            is Int -> raw.toString()
            is Double -> if (raw % 1.0 == 0.0) raw.toLong().toString() else raw.toString()
            is Number -> raw.toString()
            is String -> raw.trim()
            else -> ""
        }

        private fun firstBoolInMaps(maps: List<Map<String, Any?>>, vararg keys: String): Boolean? {
            for (m in maps) {
                for (k in keys) {
                    if (m.containsKey(k)) {
                        val parsed = parseBoolValue(m[k])
                        if (parsed != null) return parsed
                    }
                }
            }
            return null
        }

        private fun firstStrInMaps(maps: List<Map<String, Any?>>, vararg keys: String): String {
            for (m in maps) {
                for (k in keys) {
                    if (m.containsKey(k)) {
                        val s = parseStringValue(m[k])
                        if (s.isNotBlank()) return s
                    }
                }
            }
            return ""
        }

        private fun firstIntInMaps(maps: List<Map<String, Any?>>, vararg keys: String): Int? {
            for (m in maps) {
                for (k in keys) {
                    if (m.containsKey(k)) {
                        val parsed = parseIntValue(m[k])
                        if (parsed != null) return parsed
                    }
                }
            }
            return null
        }

        private fun firstVersionInMaps(maps: List<Map<String, Any?>>, vararg keys: String): String {
            for (m in maps) {
                for (k in keys) {
                    if (m.containsKey(k)) {
                        val s = parseVersionValue(m[k])
                        if (s.isNotBlank()) return s
                    }
                }
            }
            return ""
        }

        private fun firstVersionListInMaps(maps: List<Map<String, Any?>>, vararg keys: String): List<String>? {
            for (m in maps) {
                for (k in keys) {
                    if (!m.containsKey(k)) continue
                    val raw = m[k] ?: continue
                    val parsed = when (raw) {
                        is Iterable<*> -> raw.mapNotNull { item ->
                            parseVersionValue(item).takeIf { it.isNotBlank() }
                        }
                        is String -> raw.split(Regex("[,;\\n\\r]+"))
                            .map { it.trim() }
                            .filter { it.isNotBlank() }
                        else -> emptyList()
                    }
                    return parsed
                }
            }
            return null
        }

        private fun subMaps(root: Map<String, Any?>, vararg keys: String): List<Map<String, Any?>> =
            keys.mapNotNull { asStringMap(root[it]) }

        private fun matchFlagSlot(rawName: String): String? {
            val norm = normalizeConfigKey(rawName)
            return when {
                norm in setOf("sharedjournal", "sharedjournalenabled", "journal", "journalenabled", "sharedmoments") ->
                    "sharedJournalEnabled"
                norm in setOf("dateplanner", "dateplannerenabled", "datesplanner", "planner") ->
                    "datePlannerEnabled"
                norm in setOf("moodsync", "moodsyncenabled", "moodtracker", "moodtracking") ->
                    "moodSyncEnabled"
                norm in setOf("dailyprompt", "dailypromptcheck", "dailypromptenabled", "dailypromptcheckenabled", "dailyquestion", "promptcheck") ->
                    "dailyPromptEnabled"
                else -> null
            }
        }

        fun fromSnapshot(snap: DocumentSnapshot, collectionHint: String = ""): LiveAppConfig =
            parseSnapshotWithPresence(snap, collectionHint).config

        fun parseSnapshotWithPresence(snap: DocumentSnapshot, collectionHint: String = ""): ParsedLiveConfigDoc {
            if (!snap.exists()) {
                return ParsedLiveConfigDoc(config = LiveAppConfig(id = snap.id), priority = 0)
            }
            val data = snap.data.orEmpty()
            val updatedTs = snap.getTimestamp("updatedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: snap.getTimestamp("updated_at", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            return parseMapWithPresence(
                docId = snap.id,
                data = data,
                updatedAt = updatedTs,
                collectionHint = collectionHint
            )
        }

        fun fromMap(
            docId: String,
            data: Map<String, Any?>,
            collectionHint: String = "",
            updatedAt: Timestamp? = null
        ): LiveAppConfig = parseMapWithPresence(docId, data, updatedAt, collectionHint).config

        fun parseMapWithPresence(
            docId: String,
            data: Map<String, Any?>,
            updatedAt: Timestamp? = null,
            collectionHint: String = ""
        ): ParsedLiveConfigDoc {
            val present = linkedSetOf<String>()
            val colNorm = collectionHint.trim().lowercase()
            val idNorm = docId.trim().lowercase()

            val maintMaps = subMaps(data, "maintenance", "maintenanceConfig", "maintenance_config", "maintenanceMode") + listOf(data)
            val isMaintDoc = colNorm in setOf("maintenance", "maintenance_mode", "app_status") ||
                colNorm.contains("maint") ||
                idNorm.contains("maint")

            val announcementSubMaps = subMaps(
                data,
                "announcement",
                "announcementBanner",
                "announcement_banner",
                "banner",
                "notice"
            )
            val promoSubMaps = subMaps(
                data,
                "promo",
                "promoBanner",
                "promo_banner",
                "promotion",
                "campaign"
            )
            val updateSubMaps = subMaps(
                data,
                "versionControl",
                "version_control",
                "version",
                "versions",
                "update",
                "updates",
                "appUpdate",
                "app_update"
            )
            val flagSubMaps = subMaps(
                data,
                "featureFlags",
                "feature_flags",
                "features",
                "flags",
                "modules",
                "toggles"
            )
            val dynamicSubMaps = subMaps(
                data,
                "dynamicConfig",
                "dynamic_config",
                "keyValueConfig",
                "key_value_config",
                "keyValues",
                "key_values",
                "kvConfig",
                "kv_config",
                "remoteConfig",
                "remote_config",
                "parameters",
                "entries",
                "values"
            )

            // Determine document intent flags
            val docType = firstStrInMaps(listOf(data), "type", "bannerType", "category", "kind").lowercase()
            val isPromoDoc = colNorm in setOf("promo_banners", "promos") ||
                idNorm.contains("promo") ||
                docType == "promo" ||
                parseBoolValue(data["isPromo"]) == true
            val isAnnouncementDoc = !isPromoDoc && (
                colNorm in setOf("banners", "announcements") ||
                    idNorm.contains("banner") ||
                    idNorm.contains("announce")
                )
            val isUpdateDoc = colNorm in setOf("version_control", "app_versions", "updates") ||
                idNorm.contains("version") ||
                idNorm.contains("update")
            val isFlagDoc = colNorm in setOf("feature_flags", "features") ||
                idNorm.contains("flag") ||
                idNorm.contains("feature")
            val isDynamicKvDoc = colNorm in setOf(
                "dynamic_config", "key_value_config", "remote_config", "kv_config", "config", "app_settings", "settings"
            ) || idNorm.contains("dynamic") || idNorm.contains("key_value") || idNorm.contains("kv") || idNorm.contains("remote")

            // 1. Maintenance fields
            val nestedMaintMaps = subMaps(data, "maintenance", "maintenanceConfig", "maintenance_config")
            val maintSearchMaps = if (isMaintDoc) maintMaps else listOf(data) + nestedMaintMaps
            val maintStatusRaw = firstStrInMaps(
                maintSearchMaps,
                "maintenanceStatus", "maintenance_status", "siteStatus", "appStatus", "app_status", "status", "mode"
            ).lowercase()
            val maintModeByStatus = when {
                maintStatusRaw in setOf("maintenance", "maint", "under_maintenance", "down") -> true
                isMaintDoc && maintStatusRaw in setOf("normal", "operational", "online", "live", "active", "off") -> false
                else -> null
            }
            val maintModeParsed = firstBoolInMaps(
                maintSearchMaps,
                "maintenanceMode", "maintenance_mode",
                "isMaintenance", "is_maintenance",
                "maintenance",
                "maintenanceEnabled", "maintenance_enabled",
                "isMaintenanceEnabled", "is_maintenance_enabled",
                "isMaintenanceMode", "is_maintenance_mode",
                "underMaintenance", "under_maintenance",
                "inMaintenance", "in_maintenance",
                "maintenanceActive", "maintenance_active",
                "maintenanceOn", "maintenance_on"
            ) ?: if (isMaintDoc) {
                firstBoolInMaps(maintMaps, "enabled", "isEnabled", "active", "isActive", "on", "mode")
            } else if (nestedMaintMaps.isNotEmpty()) {
                firstBoolInMaps(nestedMaintMaps, "enabled", "isEnabled", "active", "isActive", "on", "mode")
            } else null
                ?: maintModeByStatus
            val resolvedMaintModeParsed = maintModeParsed ?: maintModeByStatus
            if (resolvedMaintModeParsed != null) present += "maintenanceMode"

            val maintTitleParsed = firstStrInMaps(maintMaps, "maintenanceTitle", "maintenance_title")
                .ifBlank { if (isMaintDoc) firstStrInMaps(maintMaps, "title", "headline") else "" }
            if (maintTitleParsed.isNotBlank()) present += "maintenanceTitle"

            val maintMsgParsed = firstStrInMaps(
                maintMaps,
                "maintenanceMessage", "maintenance_message", "maintenanceReason", "maintenance_reason", "reason"
            ).ifBlank { if (isMaintDoc) firstStrInMaps(maintMaps, "message", "body", "text", "description") else "" }
            if (maintMsgParsed.isNotBlank()) present += "maintenanceMessage"

            val maintEtaParsed = firstStrInMaps(
                maintMaps,
                "maintenanceEta", "maintenance_eta", "estimatedEndTime", "estimated_end_time", "eta"
            )
            if (maintEtaParsed.isNotBlank()) present += "maintenanceEta"

            val upcomingMaintParsed = firstBoolInMaps(
                maintMaps,
                "upcomingMaintenance", "upcoming_maintenance", "scheduledMaintenance", "scheduled_maintenance", "showMaintenanceBanner"
            )
            if (upcomingMaintParsed != null) present += "upcomingMaintenance"

            val upcomingMaintTextParsed = firstStrInMaps(
                maintMaps,
                "upcomingMaintenanceText", "upcoming_maintenance_text", "scheduledMaintenanceMessage", "maintenanceNotice"
            )
            if (upcomingMaintTextParsed.isNotBlank()) present += "upcomingMaintenanceText"

            // 2. Announcement Banner fields (supports submaps + dedicated docs + prefixed flat keys)
            val announcementSearchMaps = if (isAnnouncementDoc) announcementSubMaps + listOf(data) else announcementSubMaps + listOf(data)
            val showBannerParsed = firstBoolInMaps(
                announcementSearchMaps,
                "showBanner", "show_banner", "showAnnouncement", "show_announcement",
                "bannerEnabled", "banner_enabled", "announcementEnabled", "announcement_enabled",
                "hasBanner", "has_banner", "announcementActive", "announcement_active", "isAnnouncementActive"
            ) ?: if (announcementSubMaps.isNotEmpty()) {
                firstBoolInMaps(announcementSubMaps, "enabled", "isEnabled", "active", "isActive", "visible", "isVisible", "show", "status")
            } else if (isAnnouncementDoc) {
                firstBoolInMaps(listOf(data), "enabled", "isEnabled", "active", "isActive", "visible", "isVisible", "show", "status")
            } else null
            if (showBannerParsed != null) present += "showBanner"

            val bannerTitleParsed = firstStrInMaps(
                announcementSearchMaps,
                "bannerTitle", "banner_title", "announcementTitle", "announcement_title"
            ).ifBlank {
                if (announcementSubMaps.isNotEmpty()) firstStrInMaps(announcementSubMaps, "title", "headline")
                else if (isAnnouncementDoc) firstStrInMaps(listOf(data), "title", "headline")
                else ""
            }
            if (bannerTitleParsed.isNotBlank()) present += "bannerTitle"

            val bannerTextParsed = firstStrInMaps(
                announcementSearchMaps,
                "bannerText", "banner_text", "bannerMessage", "banner_message",
                "announcementText", "announcement_text", "announcementMessage", "announcement_message", "announcementBody"
            ).ifBlank {
                if (announcementSubMaps.isNotEmpty()) firstStrInMaps(announcementSubMaps, "text", "message", "body", "description", "subtitle", "content")
                else if (isAnnouncementDoc) firstStrInMaps(listOf(data), "text", "message", "body", "description", "subtitle", "content")
                else ""
            }
            if (bannerTextParsed.isNotBlank()) present += "bannerText"

            val bannerLinkParsed = firstStrInMaps(
                announcementSearchMaps,
                "bannerLink", "banner_link", "bannerUrl", "banner_url",
                "announcementLink", "announcement_link", "announcementUrl", "announcement_url"
            ).ifBlank {
                if (announcementSubMaps.isNotEmpty()) firstStrInMaps(announcementSubMaps, "link", "url", "actionUrl", "ctaLink")
                else if (isAnnouncementDoc) firstStrInMaps(listOf(data), "link", "url", "actionUrl", "ctaLink")
                else ""
            }
            if (bannerLinkParsed.isNotBlank()) present += "bannerLink"

            val bannerTypeRaw = firstStrInMaps(
                announcementSearchMaps,
                "bannerType", "banner_type", "bannerStyle", "banner_style", "announcementType", "announcement_type"
            ).ifBlank {
                if (announcementSubMaps.isNotEmpty()) firstStrInMaps(announcementSubMaps, "type", "style", "severity")
                else if (isAnnouncementDoc) firstStrInMaps(listOf(data), "type", "style", "severity")
                else ""
            }.lowercase()
            if (bannerTypeRaw.isNotBlank()) present += "bannerType"

            // 3. Promo Banner fields
            val promoSearchMaps = promoSubMaps + listOf(data)
            val showPromoParsed = firstBoolInMaps(
                promoSearchMaps,
                "showPromoBanner", "show_promo_banner", "showPromo", "show_promo",
                "promoEnabled", "promo_enabled", "promoBannerEnabled", "promo_banner_enabled",
                "promoActive", "promo_active", "hasPromo", "has_promo", "hasPromoBanner"
            ) ?: if (promoSubMaps.isNotEmpty()) {
                firstBoolInMaps(promoSubMaps, "enabled", "isEnabled", "active", "isActive", "visible", "isVisible", "show", "status")
            } else if (isPromoDoc) {
                firstBoolInMaps(listOf(data), "enabled", "isEnabled", "active", "isActive", "visible", "isVisible", "show", "status")
            } else null
            if (showPromoParsed != null) present += "showPromoBanner"

            val promoTitleParsed = firstStrInMaps(
                promoSearchMaps,
                "promoTitle", "promo_title", "promoHeadline", "promo_headline"
            ).ifBlank {
                if (promoSubMaps.isNotEmpty()) firstStrInMaps(promoSubMaps, "title", "headline", "name")
                else if (isPromoDoc) firstStrInMaps(listOf(data), "title", "headline", "name")
                else ""
            }
            if (promoTitleParsed.isNotBlank()) present += "promoTitle"

            val promoTextParsed = firstStrInMaps(
                promoSearchMaps,
                "promoText", "promo_text", "promoMessage", "promo_message",
                "promoSubtitle", "promo_subtitle", "promoDescription", "promo_description", "promoBody"
            ).ifBlank {
                if (promoSubMaps.isNotEmpty()) firstStrInMaps(promoSubMaps, "text", "message", "body", "subtitle", "description", "content")
                else if (isPromoDoc) firstStrInMaps(listOf(data), "text", "message", "body", "subtitle", "description", "content")
                else ""
            }
            if (promoTextParsed.isNotBlank()) present += "promoText"

            val promoBadgeParsed = firstStrInMaps(
                promoSearchMaps,
                "promoBadge", "promo_badge", "promoTag", "promo_tag", "promoCode", "promo_code"
            ).ifBlank {
                if (promoSubMaps.isNotEmpty()) firstStrInMaps(promoSubMaps, "badge", "tag", "code", "label")
                else if (isPromoDoc) firstStrInMaps(listOf(data), "badge", "tag", "code", "label")
                else ""
            }
            if (promoBadgeParsed.isNotBlank()) present += "promoBadge"

            val promoCtaParsed = firstStrInMaps(
                promoSearchMaps,
                "promoCtaText", "promo_cta_text", "promoCta", "promo_cta", "promoButtonText", "promo_button_text"
            ).ifBlank {
                if (promoSubMaps.isNotEmpty()) firstStrInMaps(promoSubMaps, "ctaText", "cta_text", "cta", "buttonText", "button_text", "actionText")
                else if (isPromoDoc) firstStrInMaps(listOf(data), "ctaText", "cta_text", "cta", "buttonText", "button_text", "actionText")
                else ""
            }
            if (promoCtaParsed.isNotBlank()) present += "promoCtaText"

            val promoLinkParsed = firstStrInMaps(
                promoSearchMaps,
                "promoLink", "promo_link", "promoUrl", "promo_url", "promoCtaLink"
            ).ifBlank {
                if (promoSubMaps.isNotEmpty()) firstStrInMaps(promoSubMaps, "link", "url", "actionUrl", "ctaLink")
                else if (isPromoDoc) firstStrInMaps(listOf(data), "link", "url", "actionUrl", "ctaLink")
                else ""
            }
            if (promoLinkParsed.isNotBlank()) present += "promoLink"

            val promoColorParsed = firstStrInMaps(
                promoSearchMaps,
                "promoColor", "promo_color", "promoTheme", "promo_theme"
            ).ifBlank {
                if (promoSubMaps.isNotEmpty()) firstStrInMaps(promoSubMaps, "color", "theme", "accentColor")
                else if (isPromoDoc) firstStrInMaps(listOf(data), "color", "theme", "accentColor")
                else ""
            }
            if (promoColorParsed.isNotBlank()) present += "promoColor"

            // 4. Version Control & Updates fields
            val updateSearchMaps = updateSubMaps + listOf(data)
            val forceUpdateParsed = firstBoolInMaps(
                updateSearchMaps,
                "forceUpdate", "force_update", "isForceUpdate", "is_force_update",
                "mandatoryUpdate", "mandatory_update", "requireUpdate", "require_update", "force"
            )
            if (forceUpdateParsed != null) present += "forceUpdate"

            val showUpdateBannerParsed = firstBoolInMaps(
                updateSearchMaps,
                "showUpdateBanner", "show_update_banner", "updateBanner", "update_banner", "updateBannerEnabled"
            )
            if (showUpdateBannerParsed != null) present += "showUpdateBanner"

            val showUpdatePopupParsed = firstBoolInMaps(
                updateSearchMaps,
                "showUpdatePopup", "show_update_popup",
                "updatePopup", "update_popup",
                "optionalUpdate", "optional_update",
                "updateAvailable", "update_available",
                "isUpdateAvailable", "is_update_available",
                "hasUpdate", "has_update",
                "newUpdate", "new_update",
                "pushUpdate", "push_update",
                "updateEnabled", "update_enabled"
            ) ?: if (updateSubMaps.isNotEmpty()) {
                firstBoolInMaps(updateSubMaps, "enabled", "isEnabled", "active", "isActive")
            } else if (isUpdateDoc) {
                firstBoolInMaps(listOf(data), "enabled", "isEnabled", "active", "isActive")
            } else null
            if (showUpdatePopupParsed != null) present += "showUpdatePopup"

            val minVersionParsed = firstVersionInMaps(
                updateSearchMaps,
                "minVersion", "min_version", "minimumVersion", "minimum_version",
                "minAppVersion", "min_app_version", "requiredVersion", "required_version"
            )
            if (minVersionParsed.isNotBlank()) present += "minVersion"

            val latestVersionParsed = firstVersionInMaps(
                updateSearchMaps,
                "latestVersion", "latest_version", "newVersion", "new_version",
                "updateVersion", "update_version", "versionName", "version_name",
                "currentVersion", "targetVersion", "recommendedVersion"
            )
            if (latestVersionParsed.isNotBlank()) present += "latestVersion"

            val minVersionCodeParsed = firstIntInMaps(
                updateSearchMaps,
                "minVersionCode", "min_version_code", "minimumVersionCode", "minBuildNumber", "min_build"
            )
            if (minVersionCodeParsed != null) present += "minVersionCode"

            val latestVersionCodeParsed = firstIntInMaps(
                updateSearchMaps,
                "latestVersionCode", "latest_version_code", "versionCode", "version_code", "latestBuild"
            )
            if (latestVersionCodeParsed != null) present += "latestVersionCode"

            val blockedVersionsParsed = firstVersionListInMaps(
                updateSearchMaps,
                "blockedVersions", "blocked_versions", "disabledVersions", "deprecatedVersions"
            )
            if (blockedVersionsParsed != null) present += "blockedVersions"

            val updateTitleParsed = firstStrInMaps(
                updateSearchMaps,
                "updateTitle", "update_title"
            ).ifBlank { if (updateSubMaps.isNotEmpty() || isUpdateDoc) firstStrInMaps(updateSearchMaps, "title") else "" }
            if (updateTitleParsed.isNotBlank()) present += "updateTitle"

            val updateMsgParsed = firstStrInMaps(
                updateSearchMaps,
                "updateMessage", "update_message", "releaseNotes", "release_notes",
                "changelog", "updateDescription", "update_description"
            ).ifBlank { if (updateSubMaps.isNotEmpty() || isUpdateDoc) firstStrInMaps(updateSearchMaps, "message", "notes") else "" }
            if (updateMsgParsed.isNotBlank()) present += "updateMessage"

            val updateLinkParsed = firstStrInMaps(
                updateSearchMaps,
                "updateLink", "update_link", "updateUrl", "update_url",
                "downloadUrl", "download_url", "downloadLink", "download_link",
                "appUrl", "playStoreUrl", "apkUrl", "storeUrl", "store_url",
                "releaseUrl", "release_url", "ctaLink", "ctaUrl"
            ).ifBlank { if (updateSubMaps.isNotEmpty() || isUpdateDoc) firstStrInMaps(updateSearchMaps, "link", "url") else "" }
            if (updateLinkParsed.isNotBlank()) present += "updateLink"

            // 5. Dynamic Key-Value Config extraction
            val extractedKv = linkedMapOf<String, String>()
            fun putKv(k: String, v: Any?) {
                val cleanKey = k.trim()
                if (cleanKey.isBlank() || cleanKey in RESERVED_DOC_META_KEYS) return
                val strVal = parseStringValue(v)
                if (strVal.isNotEmpty() || v is String || v is Boolean || v is Number) {
                    extractedKv[cleanKey] = strVal
                }
            }
            for (subMap in dynamicSubMaps) {
                for ((k, v) in subMap) {
                    if (v !is Map<*, *> && v !is Iterable<*>) {
                        putKv(k, v)
                    }
                }
            }
            // Handle arrays of { key, value } entries (e.g. entries / items / pairs / configs)
            listOf("entries", "items", "pairs", "configs", "kvPairs", "keyValues").forEach { listKey ->
                val rawList = data[listKey]
                if (rawList is Iterable<*>) {
                    for (item in rawList) {
                        val m = asStringMap(item) ?: continue
                        val entryKey = firstStrInMaps(listOf(m), "key", "name", "id")
                        if (entryKey.isNotBlank() && m.containsKey("value")) {
                            putKv(entryKey, m["value"])
                        }
                    }
                }
            }
            // Handle single KV entry documents ({ key: "foo", value: "bar" }) or KV collections
            val explicitEntryKey = firstStrInMaps(listOf(data), "key", "configKey", "paramKey")
            if (explicitEntryKey.isNotBlank() && data.containsKey("value")) {
                putKv(explicitEntryKey, data["value"])
            } else if (isDynamicKvDoc) {
                for ((k, v) in data) {
                    if (v !is Map<*, *> && v !is Iterable<*>) {
                        putKv(k, v)
                    }
                }
            }
            if (extractedKv.isNotEmpty()) present += "dynamicConfig"

            // 6. Feature Flags extraction (supports flat fields, nested maps, lists, per-doc flags, and KV overrides)
            var sharedJournal = true
            var datePlanner = true
            var moodSync = true
            var dailyPrompt = true

            fun applyFlag(slot: String, value: Boolean) {
                present += slot
                when (slot) {
                    "sharedJournalEnabled" -> sharedJournal = value
                    "datePlannerEnabled" -> datePlanner = value
                    "moodSyncEnabled" -> moodSync = value
                    "dailyPromptEnabled" -> dailyPrompt = value
                }
            }

            // 6a. Keyed maps (submaps + top-level data)
            val flagMaps = flagSubMaps + listOf(data)
            firstBoolInMaps(
                flagMaps,
                "sharedJournalEnabled", "shared_journal_enabled",
                "sharedJournal", "shared_journal",
                "enableSharedJournal", "enable_shared_journal",
                "featureSharedJournal", "feature_shared_journal",
                "journalEnabled", "journal"
            )?.let { applyFlag("sharedJournalEnabled", it) }

            firstBoolInMaps(
                flagMaps,
                "datePlannerEnabled", "date_planner_enabled",
                "datePlanner", "date_planner",
                "enableDatePlanner", "enable_date_planner",
                "featureDatePlanner", "feature_date_planner",
                "plannerEnabled"
            )?.let { applyFlag("datePlannerEnabled", it) }

            firstBoolInMaps(
                flagMaps,
                "moodSyncEnabled", "mood_sync_enabled",
                "moodSync", "mood_sync",
                "enableMoodSync", "enable_mood_sync",
                "featureMoodSync", "feature_mood_sync"
            )?.let { applyFlag("moodSyncEnabled", it) }

            firstBoolInMaps(
                flagMaps,
                "dailyPromptEnabled", "daily_prompt_enabled",
                "dailyPromptCheckEnabled", "daily_prompt_check_enabled",
                "dailyPromptCheck", "daily_prompt_check",
                "dailyPrompt", "daily_prompt",
                "enableDailyPrompt", "enable_daily_prompt",
                "enableDailyPromptCheck", "featureDailyPrompt", "feature_daily_prompt"
            )?.let { applyFlag("dailyPromptEnabled", it) }

            // 6b. List of flag objects inside a doc: flags: [{ key/name, enabled/value }]
            listOf("flags", "featureFlags", "feature_flags", "features").forEach { listKey ->
                val rawList = data[listKey]
                if (rawList is Iterable<*>) {
                    for (item in rawList) {
                        val m = asStringMap(item) ?: continue
                        val name = firstStrInMaps(listOf(m), "key", "id", "name", "feature", "flag")
                        val slot = matchFlagSlot(name) ?: continue
                        val valBool = firstBoolInMaps(listOf(m), "enabled", "isEnabled", "active", "value", "state") ?: continue
                        applyFlag(slot, valBool)
                    }
                }
            }

            // 6c. Per-flag document in /feature_flags/{flagId} or /features/{flagId}
            if (isFlagDoc) {
                val docFlagName = firstStrInMaps(listOf(data), "key", "id", "name", "feature", "flag").ifBlank { docId }
                val matchedSlot = matchFlagSlot(docFlagName)
                if (matchedSlot != null) {
                    val b = firstBoolInMaps(listOf(data), "enabled", "isEnabled", "active", "isActive", "value", "state", "status")
                    if (b != null) applyFlag(matchedSlot, b)
                }
            }

            // 6d. Feature flag entries stored inside dynamicConfig map if not already explicitly parsed
            for ((k, v) in extractedKv) {
                val slot = matchFlagSlot(k) ?: continue
                if (slot !in present) {
                    parseBoolValue(v)?.let { applyFlag(slot, it) }
                }
            }

            val adminPkg = firstStrInMaps(listOf(data), "adminPackageId", "adminAppId", "sourcePackage")
            if (adminPkg.isNotBlank()) present += "adminPackageId"

            val config = LiveAppConfig(
                id = docId,
                maintenanceMode = resolvedMaintModeParsed ?: false,
                maintenanceTitle = maintTitleParsed,
                maintenanceMessage = maintMsgParsed,
                maintenanceEta = maintEtaParsed,
                upcomingMaintenance = upcomingMaintParsed ?: false,
                upcomingMaintenanceText = upcomingMaintTextParsed,
                showBanner = showBannerParsed ?: false,
                bannerTitle = bannerTitleParsed,
                bannerText = bannerTextParsed,
                bannerLink = bannerLinkParsed,
                bannerType = bannerTypeRaw.ifBlank { "info" },
                showPromoBanner = showPromoParsed ?: false,
                promoTitle = promoTitleParsed,
                promoText = promoTextParsed,
                promoBadge = promoBadgeParsed,
                promoCtaText = promoCtaParsed,
                promoLink = promoLinkParsed,
                promoColor = promoColorParsed.ifBlank { "rose" },
                sharedJournalEnabled = sharedJournal,
                datePlannerEnabled = datePlanner,
                moodSyncEnabled = moodSync,
                dailyPromptEnabled = dailyPrompt,
                forceUpdate = forceUpdateParsed ?: false,
                showUpdateBanner = showUpdateBannerParsed ?: false,
                showUpdatePopup = showUpdatePopupParsed ?: false,
                minVersion = minVersionParsed,
                latestVersion = latestVersionParsed,
                minVersionCode = minVersionCodeParsed,
                latestVersionCode = latestVersionCodeParsed,
                blockedVersions = blockedVersionsParsed ?: emptyList(),
                updateTitle = updateTitleParsed,
                updateMessage = updateMsgParsed,
                updateLink = updateLinkParsed,
                dynamicConfig = extractedKv,
                adminPackageId = adminPkg,
                updatedAt = updatedAt
            )

            // Priority gives dedicated domain docs slight preference when timestamps tie,
            // while newer updatedAt wins within each domain.
            val priority = when {
                colNorm != "app_config" && colNorm.isNotBlank() -> 3
                idNorm != "live" -> 2
                else -> 1
            }

            return ParsedLiveConfigDoc(
                config = config,
                presentFields = present,
                priority = priority,
                updatedAtMillis = updatedAt?.toDate()?.time ?: 0L,
                collectionHint = colNorm,
                isMaintenanceDomain = isMaintDoc,
                isUpdateDomain = isUpdateDoc
            )
        }

        /**
         * Sparse timestamp-ordered merge of any number of OTA configuration documents
         * across app_config and companion collections (feature_flags, banners, promo_banners,
         * version_control, maintenance, dynamic_config, etc.).
         * Dedicated domain docs (e.g. maintenance or version_control collections) are isolated so
         * unrelated documents carrying default false values cannot accidentally overwrite
         * active Maintenance Mode or Update gates.
         */
        fun mergeParsedDocs(docs: List<ParsedLiveConfigDoc>): LiveAppConfig {
            if (docs.isEmpty()) return LiveAppConfig()
            val sorted = docs.sortedWith(
                compareBy<ParsedLiveConfigDoc> { it.updatedAtMillis }
                    .thenBy { it.priority }
                    .thenBy { it.config.id }
            )

            var result = LiveAppConfig()
            val mergedKv = linkedMapOf<String, String>()
            val hasDedicatedMaintDocWithMode = sorted.any {
                it.isMaintenanceDomain && "maintenanceMode" in it.presentFields
            }
            val maintAuthoritativeCollections = setOf(
                "", "app_config", "maintenance", "maintenance_mode",
                "system_config", "app_status", "admin_config", "config", "app_settings", "settings"
            )
            val updateAuthoritativeCollections = setOf(
                "", "app_config", "version_control", "app_versions", "updates",
                "system_config", "admin_config", "config", "app_settings", "settings"
            )

            for (doc in sorted) {
                val c = doc.config
                val p = doc.presentFields
                val allowMaintOverride = if (hasDedicatedMaintDocWithMode) {
                    doc.isMaintenanceDomain
                } else {
                    doc.isMaintenanceDomain ||
                        doc.collectionHint in maintAuthoritativeCollections ||
                        c.maintenanceMode
                }
                val allowUpdateOverride = doc.isUpdateDomain ||
                    doc.collectionHint in updateAuthoritativeCollections ||
                    c.forceUpdate || c.showUpdatePopup || c.showUpdateBanner ||
                    c.latestVersion.isNotBlank() || c.minVersion.isNotBlank()

                result = result.copy(
                    maintenanceMode = if (allowMaintOverride && "maintenanceMode" in p) c.maintenanceMode else result.maintenanceMode,
                    maintenanceTitle = if ("maintenanceTitle" in p) c.maintenanceTitle else result.maintenanceTitle,
                    maintenanceMessage = if ("maintenanceMessage" in p) c.maintenanceMessage else result.maintenanceMessage,
                    maintenanceEta = if ("maintenanceEta" in p) c.maintenanceEta else result.maintenanceEta,
                    upcomingMaintenance = if ("upcomingMaintenance" in p) c.upcomingMaintenance else result.upcomingMaintenance,
                    upcomingMaintenanceText = if ("upcomingMaintenanceText" in p) c.upcomingMaintenanceText else result.upcomingMaintenanceText,
                    showBanner = if ("showBanner" in p) c.showBanner else result.showBanner,
                    bannerTitle = if ("bannerTitle" in p) c.bannerTitle else result.bannerTitle,
                    bannerText = if ("bannerText" in p) c.bannerText else result.bannerText,
                    bannerLink = if ("bannerLink" in p) c.bannerLink else result.bannerLink,
                    bannerType = if ("bannerType" in p) c.bannerType else result.bannerType,
                    showPromoBanner = if ("showPromoBanner" in p) c.showPromoBanner else result.showPromoBanner,
                    promoTitle = if ("promoTitle" in p) c.promoTitle else result.promoTitle,
                    promoText = if ("promoText" in p) c.promoText else result.promoText,
                    promoBadge = if ("promoBadge" in p) c.promoBadge else result.promoBadge,
                    promoCtaText = if ("promoCtaText" in p) c.promoCtaText else result.promoCtaText,
                    promoLink = if ("promoLink" in p) c.promoLink else result.promoLink,
                    promoColor = if ("promoColor" in p) c.promoColor else result.promoColor,
                    sharedJournalEnabled = if ("sharedJournalEnabled" in p) c.sharedJournalEnabled else result.sharedJournalEnabled,
                    datePlannerEnabled = if ("datePlannerEnabled" in p) c.datePlannerEnabled else result.datePlannerEnabled,
                    moodSyncEnabled = if ("moodSyncEnabled" in p) c.moodSyncEnabled else result.moodSyncEnabled,
                    dailyPromptEnabled = if ("dailyPromptEnabled" in p) c.dailyPromptEnabled else result.dailyPromptEnabled,
                    forceUpdate = if (allowUpdateOverride && "forceUpdate" in p) c.forceUpdate else result.forceUpdate,
                    showUpdateBanner = if (allowUpdateOverride && "showUpdateBanner" in p) c.showUpdateBanner else result.showUpdateBanner,
                    showUpdatePopup = if (allowUpdateOverride && "showUpdatePopup" in p) c.showUpdatePopup else result.showUpdatePopup,
                    minVersion = if ("minVersion" in p) c.minVersion else result.minVersion,
                    latestVersion = if ("latestVersion" in p) c.latestVersion else result.latestVersion,
                    minVersionCode = if ("minVersionCode" in p) c.minVersionCode else result.minVersionCode,
                    latestVersionCode = if ("latestVersionCode" in p) c.latestVersionCode else result.latestVersionCode,
                    blockedVersions = if ("blockedVersions" in p) c.blockedVersions else result.blockedVersions,
                    updateTitle = if ("updateTitle" in p) c.updateTitle else result.updateTitle,
                    updateMessage = if ("updateMessage" in p) c.updateMessage else result.updateMessage,
                    updateLink = if ("updateLink" in p) c.updateLink else result.updateLink,
                    adminPackageId = if ("adminPackageId" in p) c.adminPackageId else result.adminPackageId,
                    updatedAt = c.updatedAt ?: result.updatedAt
                )
                if (c.dynamicConfig.isNotEmpty()) {
                    mergedKv.putAll(c.dynamicConfig)
                }
            }

            // Re-apply dedicated maintenance docs on top if any exist with an explicit maintenanceMode,
            // ensuring a stale app_config/live (even with a newer banner timestamp) cannot override maintenance.
            if (hasDedicatedMaintDocWithMode) {
                val latestMaintDoc = sorted.lastOrNull {
                    it.isMaintenanceDomain && "maintenanceMode" in it.presentFields
                }
                if (latestMaintDoc != null) {
                    result = result.copy(
                        maintenanceMode = latestMaintDoc.config.maintenanceMode,
                        maintenanceTitle = latestMaintDoc.config.maintenanceTitle.ifBlank { result.maintenanceTitle },
                        maintenanceMessage = latestMaintDoc.config.maintenanceMessage.ifBlank { result.maintenanceMessage },
                        maintenanceEta = latestMaintDoc.config.maintenanceEta.ifBlank { result.maintenanceEta }
                    )
                }
            }

            // Check dynamic KV fallback for maintenance_mode if no doc explicitly set maintenanceMode
            val anyDocSetMaintMode = sorted.any { "maintenanceMode" in it.presentFields }
            if (!anyDocSetMaintMode) {
                val kvMaint = parseBoolValue(
                    mergedKv.entries.firstOrNull {
                        normalizeConfigKey(it.key) in setOf("maintenancemode", "maintenance", "undermaintenance")
                    }?.value
                )
                if (kvMaint != null) {
                    result = result.copy(maintenanceMode = kvMaint)
                }
            }

            return result.copy(dynamicConfig = mergedKv)
        }

        fun mergeConfigs(
            live: LiveAppConfig?,
            maintenance: LiveAppConfig?,
            updates: LiveAppConfig?,
            banners: LiveAppConfig?
        ): LiveAppConfig {
            val base = live ?: LiveAppConfig()
            val m = maintenance
            val u = updates
            val b = banners
            val mergedKv = linkedMapOf<String, String>().apply {
                putAll(base.dynamicConfig)
                m?.dynamicConfig?.let(::putAll)
                u?.dynamicConfig?.let(::putAll)
                b?.dynamicConfig?.let(::putAll)
            }
            return base.copy(
                maintenanceMode = if (live != null && live.maintenanceMode) true else (m?.maintenanceMode ?: false),
                maintenanceTitle = base.maintenanceTitle.ifBlank { m?.maintenanceTitle.orEmpty() },
                maintenanceMessage = base.maintenanceMessage.ifBlank { m?.maintenanceMessage.orEmpty() },
                maintenanceEta = base.maintenanceEta.ifBlank { m?.maintenanceEta.orEmpty() },
                upcomingMaintenance = base.upcomingMaintenance || (m?.upcomingMaintenance == true),
                upcomingMaintenanceText = base.upcomingMaintenanceText.ifBlank { m?.upcomingMaintenanceText.orEmpty() },
                showBanner = base.showBanner || (b?.showBanner == true),
                bannerTitle = base.bannerTitle.ifBlank { b?.bannerTitle.orEmpty() },
                bannerText = base.bannerText.ifBlank { b?.bannerText.orEmpty() },
                bannerLink = base.bannerLink.ifBlank { b?.bannerLink.orEmpty() },
                bannerType = if (base.bannerText.isNotBlank()) base.bannerType else (b?.bannerType ?: "info"),
                showPromoBanner = base.showPromoBanner || (b?.showPromoBanner == true),
                promoTitle = base.promoTitle.ifBlank { b?.promoTitle.orEmpty() },
                promoText = base.promoText.ifBlank { b?.promoText.orEmpty() },
                promoBadge = base.promoBadge.ifBlank { b?.promoBadge.orEmpty() },
                promoCtaText = base.promoCtaText.ifBlank { b?.promoCtaText.orEmpty() },
                promoLink = base.promoLink.ifBlank { b?.promoLink.orEmpty() },
                promoColor = if (base.promoText.isNotBlank() || base.promoTitle.isNotBlank()) base.promoColor else (b?.promoColor ?: "rose"),
                sharedJournalEnabled = base.sharedJournalEnabled && (b?.sharedJournalEnabled ?: true) && (u?.sharedJournalEnabled ?: true) && (m?.sharedJournalEnabled ?: true),
                datePlannerEnabled = base.datePlannerEnabled && (b?.datePlannerEnabled ?: true) && (u?.datePlannerEnabled ?: true) && (m?.datePlannerEnabled ?: true),
                moodSyncEnabled = base.moodSyncEnabled && (b?.moodSyncEnabled ?: true) && (u?.moodSyncEnabled ?: true) && (m?.moodSyncEnabled ?: true),
                dailyPromptEnabled = base.dailyPromptEnabled && (b?.dailyPromptEnabled ?: true) && (u?.dailyPromptEnabled ?: true) && (m?.dailyPromptEnabled ?: true),
                forceUpdate = base.forceUpdate || (u?.forceUpdate == true),
                showUpdateBanner = base.showUpdateBanner || (u?.showUpdateBanner == true),
                showUpdatePopup = base.showUpdatePopup || (u?.showUpdatePopup == true),
                minVersion = base.minVersion.ifBlank { u?.minVersion.orEmpty() },
                latestVersion = base.latestVersion.ifBlank { u?.latestVersion.orEmpty() },
                minVersionCode = base.minVersionCode ?: u?.minVersionCode,
                latestVersionCode = base.latestVersionCode ?: u?.latestVersionCode,
                blockedVersions = (base.blockedVersions + u?.blockedVersions.orEmpty()).distinct(),
                updateTitle = base.updateTitle.ifBlank { u?.updateTitle.orEmpty() },
                updateMessage = base.updateMessage.ifBlank { u?.updateMessage.orEmpty() },
                updateLink = base.updateLink.ifBlank { u?.updateLink.orEmpty() },
                dynamicConfig = mergedKv,
                adminPackageId = base.adminPackageId
                    .ifBlank { m?.adminPackageId.orEmpty() }
                    .ifBlank { u?.adminPackageId.orEmpty() }
                    .ifBlank { b?.adminPackageId.orEmpty() }
            )
        }
    }
}

object AppVersionComparator {
    private fun normalizeVersionTag(raw: String): String =
        raw.trim().removePrefix("v").removePrefix("V").lowercase()

    fun isVersionBlocked(
        currentVersionName: String,
        currentVersionCode: Int,
        blockedVersions: List<String>
    ): Boolean {
        if (blockedVersions.isEmpty()) return false
        val currentNameNorm = normalizeVersionTag(currentVersionName)
        val currentCodeNorm = currentVersionCode.toString()
        val isBuildNamedCurrent = parseVersionParts(currentVersionName).let { it.size == 1 && it[0] >= 100L }
        return blockedVersions.any { raw ->
            val candidate = normalizeVersionTag(raw)
            candidate.isNotBlank() && (
                candidate == currentNameNorm ||
                    candidate == currentCodeNorm ||
                    (isBuildNamedCurrent && candidate == "1.0.0")
                )
        }
    }

    fun isCurrentLowerThanMin(
        currentVersionName: String,
        currentVersionCode: Int,
        minVersionRaw: String
    ): Boolean {
        val minParts = parseVersionParts(minVersionRaw)
        if (minParts.isEmpty()) return false
        val parsedCurrent = parseVersionParts(currentVersionName)
            .ifEmpty { listOf(currentVersionCode.coerceAtLeast(0).toLong()) }

        // If the app uses a purely numeric build label (e.g. "1617") while Bondly Admin specifies
        // a dotted semantic version (e.g. "1.0.1", "1.2.0", "2.0.0"), compare against baseline 1.0.0.
        val effectiveCurrent = if (parsedCurrent.size == 1 && parsedCurrent[0] >= 100L &&
            minParts.size >= 2 && minParts[0] < 100L
        ) {
            listOf(1L, 0L, 0L)
        } else {
            parsedCurrent
        }

        val maxLen = maxOf(effectiveCurrent.size, minParts.size)
        for (i in 0 until maxLen) {
            val cur = effectiveCurrent.getOrElse(i) { 0L }
            val min = minParts.getOrElse(i) { 0L }
            if (cur < min) return true
            if (cur > min) return false
        }
        return false
    }

    internal fun parseVersionParts(raw: String): List<Long> {
        val cleaned = raw.trim().removePrefix("v").removePrefix("V")
        if (cleaned.isBlank()) return emptyList()
        return Regex("\\d+")
            .findAll(cleaned)
            .mapNotNull { it.value.toLongOrNull() }
            .toList()
    }
}

object AppConfigUrlValidator {
    fun isSafeWebUrl(raw: String?): Boolean {
        val candidate = raw?.trim().orEmpty()
        if (candidate.isBlank()) return false
        val uri = try {
            Uri.parse(candidate)
        } catch (_: Exception) {
            return false
        }
        val scheme = uri.scheme?.lowercase() ?: return false
        val host = uri.host?.trim().orEmpty()
        return (scheme == "https" || scheme == "http") && host.isNotBlank()
    }
}


