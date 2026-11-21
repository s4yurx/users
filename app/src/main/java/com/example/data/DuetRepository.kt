package com.example.data

import android.content.Context
import com.example.R
import com.example.crypto.EncryptedEnvelope
import com.example.telemetry.LocalDeviceTelemetry
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.snapshots
import java.security.SecureRandom
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.tasks.await

class DuetRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val secureRandom = SecureRandom()

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

    // --- LIVE APP CONFIG (BONDLY ADMIN OTA REALTIME CONTROL) ---

    private fun observeOtaCollection(collectionName: String): Flow<List<ParsedLiveConfigDoc>> =
        db.collection(collectionName)
            .snapshots()
            .map { snap ->
                snap.documents.mapNotNull { doc ->
                    if (doc.exists()) {
                        LiveAppConfig.parseSnapshotWithPresence(doc, collectionName)
                    } else {
                        null
                    }
                }
            }
            .onStart { emit(emptyList()) }
            .catch { error ->
                if (error is CancellationException) throw error
                logFlowErrorIfSignedIn(error, OperationType.GET, collectionName, auth.currentUser?.uid.orEmpty())
                emit(emptyList())
            }

    fun observeLiveAppConfig(): Flow<LiveAppConfig> =
        combine(
            listOf(
                observeOtaCollection("app_config"),
                observeOtaCollection("maintenance"),
                observeOtaCollection("maintenance_mode"),
                observeOtaCollection("system_config"),
                observeOtaCollection("app_status"),
                observeOtaCollection("admin_config"),
                observeOtaCollection("updates"),
                observeOtaCollection("feature_flags"),
                observeOtaCollection("features"),
                observeOtaCollection("banners"),
                observeOtaCollection("announcements"),
                observeOtaCollection("promo_banners"),
                observeOtaCollection("promos"),
                observeOtaCollection("version_control"),
                observeOtaCollection("app_versions"),
                observeOtaCollection("dynamic_config"),
                observeOtaCollection("key_value_config"),
                observeOtaCollection("remote_config"),
                observeOtaCollection("kv_config"),
                observeOtaCollection("config"),
                observeOtaCollection("app_settings"),
                observeOtaCollection("settings")
            )
        ) { lists ->
            LiveAppConfig.mergeParsedDocs(lists.flatMap { it })
        }

    // --- USER PROFILE ---

    fun observeUserProfile(userId: String): Flow<UserProfile?> =
        db.collection("users").document(userId)
            .snapshots()
            .map { snap -> UserProfile.fromSnapshot(snap) }
            .catch { error ->
                logFlowErrorIfSignedIn(error, OperationType.GET, "users/$userId", userId)
                throw error
            }

    fun observePartnerUserProfile(partnerUid: String): Flow<UserProfile?> =
        db.collection("users").document(partnerUid)
            .snapshots()
            .map { snap -> UserProfile.fromSnapshot(snap) }
            .catch { error ->
                if (error is CancellationException) throw error
                emit(null)
            }

    suspend fun fetchUserProfileFromServer(userId: String): UserProfile? {
        return try {
            val snap = db.collection("users").document(userId).get(Source.SERVER).await()
            UserProfile.fromSnapshot(snap)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            fetchUserProfileOnce(userId)
        }
    }

    suspend fun fetchUserProfileOnce(userId: String): UserProfile? {
        return try {
            val snap = db.collection("users").document(userId).get().await()
            UserProfile.fromSnapshot(snap)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    suspend fun fetchCoupleOnce(coupleId: String): CoupleSpace? {
        return try {
            val snap = db.collection("couples").document(coupleId).get().await()
            if (snap.exists()) {
                snap.toObject(CoupleSpace::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            } else null
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    suspend fun ensureUserProfile(
        displayName: String,
        publicKey: String
    ): Result<UserProfile> {
        val uid = auth.currentUser?.uid
        val docPath = "users/${uid ?: "unauthenticated"}"
        return try {
            val activeUid = requireUserId()
            val cleanName = displayName.trim().take(60).ifBlank { "Sweetheart" }
            val initials = deriveInitials(cleanName)
            val ref = db.collection("users").document(activeUid)
            val snap = ref.get().await()
            if (!snap.exists()) {
                val payload = mapOf(
                    "userId" to activeUid,
                    "displayName" to cleanName,
                    "avatarInitials" to initials,
                    "publicKey" to publicKey,
                    "shareDeviceStatus" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                ref.set(payload).await()
                val fresh = UserProfile.fromSnapshot(ref.get().await())
                    ?: UserProfile(id = activeUid, userId = activeUid, displayName = cleanName, avatarInitials = initials, publicKey = publicKey)
                Result.success(fresh)
            } else {
                val existing = UserProfile.fromSnapshot(snap)
                if (existing?.isAccountBanned() == true) {
                    return Result.success(existing)
                }
                val raw = snap.data.orEmpty()
                val repairs = linkedMapOf<String, Any>()
                val storedUid = (raw["userId"] as? String)?.trim().orEmpty()
                if (storedUid != activeUid) {
                    repairs["userId"] = activeUid
                }
                val storedName = (raw["displayName"] as? String)?.trim().orEmpty()
                if (storedName.isBlank()) {
                    repairs["displayName"] = cleanName
                }
                val storedInitials = (raw["avatarInitials"] as? String)?.trim().orEmpty()
                if (storedInitials.isBlank() || storedInitials.length > 4) {
                    repairs["avatarInitials"] = deriveInitials(storedName.ifBlank { cleanName })
                }
                val storedPubKey = (raw["publicKey"] as? String)?.trim().orEmpty()
                if (storedPubKey != publicKey) {
                    repairs["publicKey"] = publicKey
                }
                if (raw["createdAt"] !is Timestamp) {
                    repairs["createdAt"] = FieldValue.serverTimestamp()
                }
                if (raw["shareDeviceStatus"] !is Boolean) {
                    repairs["shareDeviceStatus"] = true
                }
                if (repairs.isNotEmpty()) {
                    repairs["updatedAt"] = FieldValue.serverTimestamp()
                    ref.update(repairs).await()
                }
                val updatedSnap = if (repairs.isNotEmpty()) ref.get().await() else snap
                Result.success(
                    UserProfile.fromSnapshot(updatedSnap)
                        ?: existing?.copy(publicKey = publicKey)
                        ?: UserProfile(id = activeUid, userId = activeUid, displayName = cleanName, avatarInitials = initials, publicKey = publicKey)
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    private suspend fun prepareUserDocForPairing(
        uid: String,
        cleanName: String,
        myPublicKey: String
    ): String? {
        val userRef = db.collection("users").document(uid)
        val snap = userRef.get().await()
        if (!snap.exists()) {
            userRef.set(
                mapOf(
                    "userId" to uid,
                    "displayName" to cleanName,
                    "avatarInitials" to deriveInitials(cleanName),
                    "publicKey" to myPublicKey,
                    "shareDeviceStatus" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            return null
        }

        val existing = UserProfile.fromSnapshot(snap)
        if (existing?.isAccountBanned() == true) {
            val reason = existing.resolvedBanReason()?.let { ": $it" }.orEmpty()
            throw IllegalStateException("Your account is suspended$reason")
        }

        val raw = snap.data.orEmpty()
        val repairs = linkedMapOf<String, Any>()
        val storedUid = (raw["userId"] as? String)?.trim().orEmpty()
        if (storedUid != uid) {
            repairs["userId"] = uid
        }
        val storedName = (raw["displayName"] as? String)?.trim().orEmpty()
        if (storedName.isBlank()) {
            repairs["displayName"] = cleanName
        }
        val storedInitials = (raw["avatarInitials"] as? String)?.trim().orEmpty()
        if (storedInitials.isBlank() || storedInitials.length > 4) {
            repairs["avatarInitials"] = deriveInitials(storedName.ifBlank { cleanName })
        }
        val storedPubKey = (raw["publicKey"] as? String)?.trim().orEmpty()
        if (storedPubKey.isBlank() || storedPubKey != myPublicKey) {
            repairs["publicKey"] = myPublicKey
        }
        if (raw["createdAt"] !is Timestamp) {
            repairs["createdAt"] = FieldValue.serverTimestamp()
        }
        if (raw["shareDeviceStatus"] !is Boolean) {
            repairs["shareDeviceStatus"] = true
        }
        if (repairs.isNotEmpty()) {
            repairs["updatedAt"] = FieldValue.serverTimestamp()
            userRef.update(repairs).await()
        }
        return (raw["coupleId"] as? String)?.trim()?.takeIf { it.isNotBlank() }
    }

    private suspend fun cleanupPreviousWaitingCoupleIfOwned(uid: String, previousCoupleId: String?) {
        if (previousCoupleId.isNullOrBlank()) return
        runCatching {
            val prevRef = db.collection("couples").document(previousCoupleId)
            val prevSnap = prevRef.get().await()
            if (prevSnap.exists()) {
                val prevCouple = prevSnap.toObject(CoupleSpace::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                if (prevCouple != null &&
                    prevCouple.creatorUid == uid &&
                    prevCouple.status == "waiting" &&
                    prevCouple.memberUids.size <= 1
                ) {
                    val prevCode = prevCouple.inviteCode.filter { it.isDigit() }
                    if (prevCode.length == 6) {
                        runCatching { db.collection("pair_codes").document(prevCode).delete().await() }
                    }
                    prevRef.delete().await()
                }
            }
        }
    }

    suspend fun updateShareDeviceStatus(enabled: Boolean): Result<Unit> {
        val docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            db.collection("users").document(uid)
                .update(
                    mapOf(
                        "shareDeviceStatus" to enabled,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateMyDisplayName(couple: CoupleSpace?, newName: String): Result<Unit> {
        val docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            val clean = newName.trim().take(60)
            require(clean.isNotEmpty()) { "Name cannot be blank" }
            val batch = db.batch()
            batch.update(
                db.collection("users").document(uid),
                mapOf(
                    "displayName" to clean,
                    "avatarInitials" to deriveInitials(clean),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            if (couple != null && couple.id.isNotBlank()) {
                val nameField = if (couple.creatorUid == uid) "creatorName" else "partnerName"
                batch.update(
                    db.collection("couples").document(couple.id),
                    mapOf(
                        nameField to clean,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updatePartnerNickname(nickname: String): Result<Unit> {
        val docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            val clean = nickname.trim().take(40)
            val value: Any? = clean.ifBlank { null }
            db.collection("users").document(uid)
                .update(
                    mapOf(
                        "partnerNickname" to value,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    // --- 6-DIGIT PAIRING & COUPLE SPACE ---

    suspend fun createCoupleSpace(
        nickname: String,
        myPublicKey: String,
        excludeInviteCode: String? = null,
        vaultKeyCreator: (inviteCode: String, coupleId: String) -> String
    ): Result<CoupleSpace> {
        var docPath = "couples"
        return try {
            val uid = requireUserId()
            val cleanName = nickname.trim().take(60).ifBlank { "Sweetheart" }
            docPath = "users/$uid"
            val previousCoupleId = prepareUserDocForPairing(uid, cleanName, myPublicKey)
            cleanupPreviousWaitingCoupleIfOwned(uid, previousCoupleId)

            val coupleId = "cpl_" + UUID.randomUUID().toString().replace("-", "").take(20)
            val inviteCode = generateUniqueSixDigitCode(excludeCode = excludeInviteCode)
            val wrappedVaultKey = vaultKeyCreator(inviteCode, coupleId)

            val coupleRef = db.collection("couples").document(coupleId)
            val codeRef = db.collection("pair_codes").document(inviteCode)
            val userRef = db.collection("users").document(uid)
            docPath = coupleRef.path

            val batch = db.batch()
            batch.set(
                coupleRef,
                mapOf(
                    "creatorUid" to uid,
                    "memberUids" to listOf(uid),
                    "inviteCode" to inviteCode,
                    "status" to "waiting",
                    "creatorName" to cleanName,
                    "creatorPublicKey" to myPublicKey,
                    "wrappedVaultKey" to wrappedVaultKey,
                    "lovePulseCount" to 0,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                codeRef,
                mapOf(
                    "code" to inviteCode,
                    "coupleId" to coupleId,
                    "creatorUid" to uid,
                    "creatorName" to cleanName,
                    "status" to "open",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                userRef,
                mapOf(
                    "displayName" to cleanName,
                    "avatarInitials" to deriveInitials(cleanName),
                    "coupleId" to coupleId,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()

            val created = coupleRef.get().await()
                .toObject(CoupleSpace::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: CoupleSpace(
                    id = coupleId,
                    creatorUid = uid,
                    memberUids = listOf(uid),
                    inviteCode = inviteCode,
                    status = "waiting",
                    creatorName = cleanName,
                    creatorPublicKey = myPublicKey,
                    wrappedVaultKey = wrappedVaultKey
                )
            Result.success(created)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun joinCoupleByCode(
        rawCode: String,
        nickname: String,
        myPublicKey: String
    ): Result<String> {
        val code = rawCode.filter { it.isDigit() }
        var docPath = "pair_codes/$code"
        return try {
            val uid = requireUserId()
            require(code.length == 6) { "Please enter a valid 6-digit invite code." }
            val cleanName = nickname.trim().take(60).ifBlank { "Sweetheart" }
            docPath = "users/$uid"
            val previousCoupleId = prepareUserDocForPairing(uid, cleanName, myPublicKey)
            cleanupPreviousWaitingCoupleIfOwned(uid, previousCoupleId)

            docPath = "pair_codes/$code"
            val codeRef = db.collection("pair_codes").document(code)
            val snap = codeRef.get().await()
            if (!snap.exists()) {
                return Result.failure(IllegalArgumentException("No couple space found for code $code."))
            }
            val pairDoc = snap.toObject(PairCodeDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: return Result.failure(IllegalStateException("Invalid invite code record."))

            if (pairDoc.status != "open") {
                return Result.failure(IllegalStateException("This 6-digit invite code has already been used."))
            }
            if (pairDoc.creatorUid == uid) {
                return Result.failure(IllegalStateException("That's your own invite code — share it with your partner!"))
            }

            val coupleRef = db.collection("couples").document(pairDoc.coupleId)
            val userRef = db.collection("users").document(uid)
            docPath = coupleRef.path

            val batch = db.batch()
            batch.update(
                codeRef,
                mapOf(
                    "status" to "claimed",
                    "claimedByUid" to uid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                coupleRef,
                mapOf(
                    "partnerUid" to uid,
                    "partnerName" to cleanName,
                    "partnerPublicKey" to myPublicKey,
                    "memberUids" to listOf(pairDoc.creatorUid, uid),
                    "status" to "paired",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                userRef,
                mapOf(
                    "displayName" to cleanName,
                    "avatarInitials" to deriveInitials(cleanName),
                    "coupleId" to pairDoc.coupleId,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(pairDoc.coupleId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    fun observeCoupleSpace(coupleId: String, uid: String): Flow<CoupleSpace?> =
        db.collection("couples").document(coupleId)
            .snapshots()
            .map { snap ->
                if (snap.exists()) {
                    snap.toObject(CoupleSpace::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                } else {
                    null
                }
            }
            .catch { error ->
                logFlowErrorIfSignedIn(error, OperationType.GET, "couples/$coupleId", uid)
                throw error
            }

    suspend fun syncMyPublicKeyOnCouple(couple: CoupleSpace, myPublicKey: String): Result<Unit> {
        val docPath = "couples/${couple.id}"
        return try {
            val uid = requireUserId()
            val field = when (uid) {
                couple.creatorUid -> if (couple.creatorPublicKey != myPublicKey) "creatorPublicKey" else null
                couple.partnerUid -> if (couple.partnerPublicKey != myPublicKey) "partnerPublicKey" else null
                else -> null
            } ?: return Result.success(Unit)
            db.collection("couples").document(couple.id)
                .update(mapOf(field to myPublicKey, "updatedAt" to FieldValue.serverTimestamp()))
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    /**
     * Backfills denormalized memberUids on documents created by creator while waiting for partner,
     * so the newly joined partner can immediately observe all pre-pairing history.
     */
    suspend fun syncPrePairingSubcollections(couple: CoupleSpace): Result<Unit> {
        val docPath = "couples/${couple.id}"
        return try {
            val uid = requireUserId()
            if (couple.status != "paired" || couple.memberUids.size != 2 || couple.creatorUid != uid) {
                return Result.success(Unit)
            }
            val coupleRef = db.collection("couples").document(couple.id)
            for (col in ALL_COUPLE_SUBCOLLECTIONS) {
                val snap = coupleRef.collection(col)
                    .whereArrayContains("memberUids", uid)
                    .get()
                    .await()
                val batch = db.batch()
                var updates = 0
                for (doc in snap.documents) {
                    val existingMembers = (doc.get("memberUids") as? List<*>)?.filterIsInstance<String>().orEmpty()
                    if (existingMembers != couple.memberUids) {
                        batch.update(
                            doc.reference,
                            mapOf(
                                "memberUids" to couple.memberUids,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                        updates++
                    }
                }
                if (updates > 0) batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    /**
     * Initiator phase of "Remove as Partner":
     * 1. Permanently wipes all documents across all 13 subcollections of this couple space
     *    (sent chats, shared HD moments, moods, countdowns, daily questions, bucket list,
     *     love letters, love jar, song dedications, care check-ins, doodles, promises, presence).
     * 2. Deletes & invalidates the old 6-digit linking code in /pair_codes/{oldCode}.
     * 3. If paired with a second user, marks /couples/{coupleId} as "dissolved" with a rotated
     *    6-digit code and cleared dates/pulses so the removed partner is automatically notified,
     *    reset, and transferred to the 6-digit code screen; if waiting alone, deletes the couple doc.
     * 4. Resets the initiator's /users/{uid} (coupleId = null, partnerNickname = null, fresh publicKey).
     */
    suspend fun removePartnerAndWipeCouple(
        couple: CoupleSpace,
        myDisplayName: String,
        newPublicKey: String
    ): Result<Unit> {
        var docPath = "couples/${couple.id}"
        return try {
            val uid = requireUserId()
            val cleanName = myDisplayName.trim().take(60).ifBlank { "Your partner" }
            val coupleRef = db.collection("couples").document(couple.id)

            // 1. Delete all shared moments, sent chats, moods, presence, and all 13 couple subcollections
            wipeAllCoupleSubcollections(couple.id, uid)

            // 2. Permanently invalidate the old 6-digit partner linking code
            val oldCode = couple.inviteCode.filter { it.isDigit() }
            if (oldCode.length == 6) {
                docPath = "pair_codes/$oldCode"
                runCatching {
                    db.collection("pair_codes").document(oldCode).delete().await()
                }
            }

            // 3. Dissolve (if paired with partner so partner's device auto-resets) or delete (if solo waiting)
            docPath = "couples/${couple.id}"
            if (couple.status == "paired" && couple.memberUids.size >= 2) {
                val rotatedCode = generateUniqueSixDigitCode(excludeCode = oldCode)
                coupleRef.update(
                    mapOf(
                        "status" to "dissolved",
                        "inviteCode" to rotatedCode,
                        "dissolvedByUid" to uid,
                        "dissolvedByName" to cleanName,
                        "dissolvedAt" to FieldValue.serverTimestamp(),
                        "anniversaryYear" to null,
                        "anniversaryMonth" to null,
                        "anniversaryDay" to null,
                        "creatorBirthdayMonth" to null,
                        "creatorBirthdayDay" to null,
                        "partnerBirthdayMonth" to null,
                        "partnerBirthdayDay" to null,
                        "lovePulseCount" to 0,
                        "lastPulseByUid" to null,
                        "lastPulseAt" to null,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                coupleRef.delete().await()
            }

            // 4. Reset initiator's user profile and publish rotated ECDH public key
            docPath = "users/$uid"
            db.collection("users").document(uid)
                .update(
                    mapOf(
                        "coupleId" to null,
                        "partnerNickname" to null,
                        "publicKey" to newPublicKey,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    /**
     * Removed-partner phase (runs automatically when partner observes couple.status == "dissolved"):
     * Sweeps any leftover subcollection docs, deletes the dissolved couple tombstone so zero
     * remnants stay in Firestore, and resets /users/{uid} back to unpaired with a fresh public key.
     */
    suspend fun completePartnerRemovalForRemovedUser(
        couple: CoupleSpace,
        newPublicKey: String
    ): Result<Unit> {
        var docPath = "couples/${couple.id}"
        return try {
            val uid = requireUserId()

            runCatching { wipeAllCoupleSubcollections(couple.id, uid) }

            val code = couple.inviteCode.filter { it.isDigit() }
            if (code.length == 6) {
                runCatching { db.collection("pair_codes").document(code).delete().await() }
            }

            runCatching {
                db.collection("couples").document(couple.id).delete().await()
            }

            docPath = "users/$uid"
            db.collection("users").document(uid)
                .update(
                    mapOf(
                        "coupleId" to null,
                        "partnerNickname" to null,
                        "publicKey" to newPublicKey,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    /**
     * Safety net if a user's profile still points at a coupleId whose document was already removed.
     */
    suspend fun clearDanglingCoupleReference(newPublicKey: String): Result<Unit> {
        val docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            db.collection("users").document(uid)
                .update(
                    mapOf(
                        "coupleId" to null,
                        "partnerNickname" to null,
                        "publicKey" to newPublicKey,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    private suspend fun wipeAllCoupleSubcollections(coupleId: String, uid: String) {
        val coupleRef = db.collection("couples").document(coupleId)
        for (col in ALL_COUPLE_SUBCOLLECTIONS) {
            val snap = coupleRef.collection(col)
                .whereArrayContains("memberUids", uid)
                .get()
                .await()
            if (snap.documents.isEmpty()) continue
            snap.documents.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { doc -> batch.delete(doc.reference) }
                batch.commit().await()
            }
        }
    }

    // --- LIVE PRESENCE & DEVICE TELEMETRY ---

    fun observePresences(coupleId: String, uid: String): Flow<List<PartnerPresence>> =
        db.collection("couples").document(coupleId).collection("presence")
            .whereArrayContains("memberUids", uid)
            .snapshots()
            .map { snap ->
                snap.toObjects(PartnerPresence::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                logFlowErrorIfSignedIn(error, OperationType.LIST, "couples/$coupleId/presence", uid)
                throw error
            }

    suspend fun upsertMyPresence(
        couple: CoupleSpace,
        displayName: String,
        telemetry: LocalDeviceTelemetry,
        moodKey: String,
        moodNote: String,
        shareStatus: Boolean,
        markLastSeenNow: Boolean = false
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: "unauthenticated"
        val docPath = "couples/${couple.id}/presence/$uid"
        return try {
            val activeUid = requireUserId()
            val ref = db.collection("couples").document(couple.id)
                .collection("presence").document(activeUid)
            val battery = if (shareStatus) telemetry.batteryPercent.coerceIn(0, 100) else -1
            val charging = if (shareStatus) telemetry.charging else false
            val network = if (shareStatus) telemetry.networkType else "HIDDEN"
            val devStatus = if (shareStatus) telemetry.deviceStatus else "HIDDEN"
            val activeApp = if (shareStatus) telemetry.activeAppName.trim().take(60) else ""
            val deviceName = if (shareStatus) telemetry.deviceName.trim().take(100) else ""
            val deviceSpecsJson = if (shareStatus) telemetry.deviceSpecsJson.take(4096) else ""
            val cleanName = displayName.trim().take(60).ifBlank { "You" }
            val cleanNote = moodNote.trim().take(140)
            val shouldUpdateLastSeen = markLastSeenNow || devStatus == "ACTIVE_IN_DUET"

            val snap = ref.get().await()
            if (snap.exists()) {
                val updates = mutableMapOf<String, Any>(
                    "displayName" to cleanName,
                    "batteryPercent" to battery,
                    "charging" to charging,
                    "networkType" to network,
                    "deviceStatus" to devStatus,
                    "activeAppName" to activeApp,
                    "deviceName" to deviceName,
                    "deviceSpecsJson" to deviceSpecsJson,
                    "moodKey" to moodKey.take(32),
                    "moodNote" to cleanNote,
                    "statusShared" to shareStatus,
                    "memberUids" to couple.memberUids,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (shouldUpdateLastSeen) {
                    updates["lastSeenAt"] = FieldValue.serverTimestamp()
                }
                ref.update(updates).await()
            } else {
                ref.set(
                    mapOf(
                        "userId" to activeUid,
                        "coupleId" to couple.id,
                        "memberUids" to couple.memberUids,
                        "displayName" to cleanName,
                        "batteryPercent" to battery,
                        "charging" to charging,
                        "networkType" to network,
                        "deviceStatus" to devStatus,
                        "activeAppName" to activeApp,
                        "deviceName" to deviceName,
                        "deviceSpecsJson" to deviceSpecsJson,
                        "moodKey" to moodKey.take(32),
                        "moodNote" to cleanNote,
                        "statusShared" to shareStatus,
                        "typingActive" to false,
                        "lastSeenAt" to FieldValue.serverTimestamp(),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun setTypingStatus(coupleId: String, typingActive: Boolean): Result<Unit> {
        val uid = auth.currentUser?.uid ?: "unauthenticated"
        val docPath = "couples/$coupleId/presence/$uid"
        return try {
            val activeUid = requireUserId()
            val ref = db.collection("couples").document(coupleId)
                .collection("presence").document(activeUid)
            ref.update(
                mapOf(
                    "typingActive" to typingActive,
                    "typingUpdatedAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Presence doc may not exist yet on very first boot; ignore quietly if NOT_FOUND
            if ((e as? FirebaseFirestoreException)?.code != FirebaseFirestoreException.Code.NOT_FOUND) {
                handleFirestoreError(e, OperationType.UPDATE, docPath)
            }
            Result.failure(e)
        }
    }

    // --- E2EE MESSAGES ---

    fun observeMessages(coupleId: String, uid: String): Flow<List<EncryptedMessage>> =
        db.collection("couples").document(coupleId).collection("messages")
            .whereArrayContains("memberUids", uid)
            .snapshots()
            .map { snap ->
                snap.toObjects(EncryptedMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                logFlowErrorIfSignedIn(error, OperationType.LIST, "couples/$coupleId/messages", uid)
                throw error
            }

    suspend fun sendEncryptedMessage(
        couple: CoupleSpace,
        senderName: String,
        messageType: String,
        envelope: EncryptedEnvelope
    ): Result<Unit> {
        var docPath = "couples/${couple.id}/messages"
        return try {
            val uid = requireUserId()
            require(envelope.ciphertext.length <= 880_000) { "HD photo is too large after compression." }
            val ref = db.collection("couples").document(couple.id).collection("messages").document()
            docPath = ref.path
            ref.set(
                mapOf(
                    "coupleId" to couple.id,
                    "senderUid" to uid,
                    "senderName" to senderName.trim().take(60).ifBlank { "You" },
                    "memberUids" to couple.memberUids,
                    "messageType" to messageType,
                    "keyMode" to envelope.keyMode,
                    "ciphertext" to envelope.ciphertext,
                    "iv" to envelope.iv,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun reactToMessage(coupleId: String, messageId: String, reactionKey: String?): Result<Unit> {
        val docPath = "couples/$coupleId/messages/$messageId"
        return try {
            val uid = requireUserId()
            require(messageId.isNotBlank())
            val cleanKey = reactionKey?.trim()?.take(16).orEmpty()
            val fieldValue: Any = if (cleanKey.isBlank()) FieldValue.delete() else cleanKey
            db.collection("couples").document(coupleId)
                .collection("messages").document(messageId)
                .update(
                    mapOf(
                        "reactions.$uid" to fieldValue,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteMessage(coupleId: String, messageId: String): Result<Unit> {
        val docPath = "couples/$coupleId/messages/$messageId"
        return try {
            requireUserId()
            require(messageId.isNotBlank())
            db.collection("couples").document(coupleId)
                .collection("messages").document(messageId)
                .delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun markMessagesSeen(coupleId: String, messageIds: List<String>): Result<Unit> {
        val cleanIds = messageIds.filter { it.isNotBlank() }.distinct().take(40)
        if (cleanIds.isEmpty()) return Result.success(Unit)
        val docPath = "couples/$coupleId/messages/${cleanIds.first()}"
        return try {
            val uid = requireUserId()
            val messagesCol = db.collection("couples").document(coupleId).collection("messages")
            val batch = db.batch()
            cleanIds.forEach { msgId ->
                batch.update(
                    messagesCol.document(msgId),
                    mapOf(
                        "seenByUid" to uid,
                        "seenAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    // --- SHARED MOOD TRACKER ---

    fun observeMoods(coupleId: String, uid: String): Flow<List<MoodEntry>> =
        db.collection("couples").document(coupleId).collection("moods")
            .whereArrayContains("memberUids", uid)
            .snapshots()
            .map { snap ->
                snap.toObjects(MoodEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                logFlowErrorIfSignedIn(error, OperationType.LIST, "couples/$coupleId/moods", uid)
                throw error
            }

    suspend fun logMood(
        couple: CoupleSpace,
        authorName: String,
        mood: MoodOption,
        note: String
    ): Result<Unit> {
        var docPath = "couples/${couple.id}/moods"
        return try {
            val uid = requireUserId()
            val ref = db.collection("couples").document(couple.id).collection("moods").document()
            docPath = ref.path
            ref.set(
                mapOf(
                    "coupleId" to couple.id,
                    "userId" to uid,
                    "authorName" to authorName.trim().take(60).ifBlank { "You" },
                    "memberUids" to couple.memberUids,
                    "moodKey" to mood.key,
                    "note" to note.trim().take(160),
                    "energyLevel" to mood.energyLevel,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun sendComfortHug(coupleId: String, moodId: String): Result<Unit> {
        val docPath = "couples/$coupleId/moods/$moodId"
        return try {
            val uid = requireUserId()
            db.collection("couples").document(coupleId)
                .collection("moods").document(moodId)
                .update(
                    mapOf(
                        "comfortSentByUid" to uid,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    // --- SHARED ENCRYPTED PHOTO MOMENTS ---

    fun observeMoments(coupleId: String, uid: String): Flow<List<SharedMoment>> =
        db.collection("couples").document(coupleId).collection("moments")
            .whereArrayContains("memberUids", uid)
            .snapshots()
            .map { snap ->
                snap.toObjects(SharedMoment::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                logFlowErrorIfSignedIn(error, OperationType.LIST, "couples/$coupleId/moments", uid)
                throw error
            }

    suspend fun uploadEncryptedMoment(
        couple: CoupleSpace,
        uploaderName: String,
        envelope: EncryptedEnvelope
    ): Result<Unit> {
        var docPath = "couples/${couple.id}/moments"
        return try {
            val uid = requireUserId()
            require(envelope.ciphertext.length <= 880_000) { "HD photo is too large to upload." }
            val ref = db.collection("couples").document(couple.id).collection("moments").document()
            docPath = ref.path
            ref.set(
                mapOf(
                    "coupleId" to couple.id,
                    "uploaderUid" to uid,
                    "uploaderName" to uploaderName.trim().take(60).ifBlank { "You" },
                    "memberUids" to couple.memberUids,
                    "keyMode" to envelope.keyMode,
                    "ciphertext" to envelope.ciphertext,
                    "iv" to envelope.iv,
                    "lovedByUids" to listOf(uid),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun toggleMomentLove(coupleId: String, moment: SharedMoment): Result<Unit> {
        val docPath = "couples/$coupleId/moments/${moment.id}"
        return try {
            val uid = requireUserId()
            val updatedLoves = if (uid in moment.lovedByUids) {
                moment.lovedByUids.filterNot { it == uid }
            } else {
                (moment.lovedByUids + uid).distinct().take(2)
            }
            db.collection("couples").document(coupleId)
                .collection("moments").document(moment.id)
                .update(
                    mapOf(
                        "lovedByUids" to updatedLoves,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteMoment(coupleId: String, momentId: String): Result<Unit> {
        val docPath = "couples/$coupleId/moments/$momentId"
        return try {
            requireUserId()
            db.collection("couples").document(coupleId)
                .collection("moments").document(momentId)
                .delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    // --- DATES, ANNIVERSARY, BIRTHDAYS & PULSES ---

    suspend fun updateAnniversary(coupleId: String, year: Int, month: Int, day: Int): Result<Unit> {
        val docPath = "couples/$coupleId"
        return try {
            requireUserId()
            db.collection("couples").document(coupleId)
                .update(
                    mapOf(
                        "anniversaryYear" to year.coerceIn(1900, 2100),
                        "anniversaryMonth" to month.coerceIn(1, 12),
                        "anniversaryDay" to day.coerceIn(1, 31),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateBirthday(
        couple: CoupleSpace,
        forPartner: Boolean,
        month: Int,
        day: Int
    ): Result<Unit> {
        val docPath = "couples/${couple.id}"
        return try {
            val uid = requireUserId()
            val editingCreator = if (couple.creatorUid == uid) !forPartner else forPartner
            val monthKey = if (editingCreator) "creatorBirthdayMonth" else "partnerBirthdayMonth"
            val dayKey = if (editingCreator) "creatorBirthdayDay" else "partnerBirthdayDay"
            db.collection("couples").document(couple.id)
                .update(
                    mapOf(
                        monthKey to month.coerceIn(1, 12),
                        dayKey to day.coerceIn(1, 31),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun sendLovePulse(couple: CoupleSpace): Result<Unit> {
        val docPath = "couples/${couple.id}"
        return try {
            val uid = requireUserId()
            db.collection("couples").document(couple.id)
                .update(
                    mapOf(
                        "lovePulseCount" to FieldValue.increment(1L),
                        "lastPulseByUid" to uid,
                        "lastPulseAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    fun observeEvents(coupleId: String, uid: String): Flow<List<SpecialDateEvent>> =
        db.collection("couples").document(coupleId).collection("events")
            .whereArrayContains("memberUids", uid)
            .snapshots()
            .map { snap ->
                snap.toObjects(SpecialDateEvent::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                logFlowErrorIfSignedIn(error, OperationType.LIST, "couples/$coupleId/events", uid)
                throw error
            }

    suspend fun addSpecialEvent(
        couple: CoupleSpace,
        title: String,
        category: String,
        year: Int,
        month: Int,
        day: Int,
        repeatsYearly: Boolean
    ): Result<Unit> {
        var docPath = "couples/${couple.id}/events"
        return try {
            val uid = requireUserId()
            val cleanTitle = title.trim().take(60)
            require(cleanTitle.isNotEmpty()) { "Event title cannot be empty" }
            val ref = db.collection("couples").document(couple.id).collection("events").document()
            docPath = ref.path
            ref.set(
                mapOf(
                    "coupleId" to couple.id,
                    "creatorUid" to uid,
                    "memberUids" to couple.memberUids,
                    "title" to cleanTitle,
                    "category" to category,
                    "year" to year.coerceIn(1900, 2200),
                    "month" to month.coerceIn(1, 12),
                    "day" to day.coerceIn(1, 31),
                    "repeatsYearly" to repeatsYearly,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteSpecialEvent(coupleId: String, eventId: String): Result<Unit> {
        val docPath = "couples/$coupleId/events/$eventId"
        return try {
            requireUserId()
            db.collection("couples").document(coupleId)
                .collection("events").document(eventId)
                .delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    // --- DAILY COUPLE QUESTION ---

    fun observeDailyPrompt(coupleId: String, dateKey: String, uid: String): Flow<DailyPromptEntry?> =
        db.collection("couples").document(coupleId).collection("daily_prompts")
            .whereArrayContains("memberUids", uid)
            .snapshots()
            .map { snap ->
                snap.toObjects(DailyPromptEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .firstOrNull { it.dateKey == dateKey }
            }
            .catch { error ->
                logFlowErrorIfSignedIn(error, OperationType.LIST, "couples/$coupleId/daily_prompts", uid)
                throw error
            }

    suspend fun submitDailyPromptAnswer(
        couple: CoupleSpace,
        dateKey: String,
        promptIndex: Int,
        answer: String
    ): Result<Unit> {
        val docPath = "couples/${couple.id}/daily_prompts/$dateKey"
        return try {
            val uid = requireUserId()
            val cleanAnswer = answer.trim().take(400)
            require(cleanAnswer.isNotEmpty()) { "Answer cannot be empty" }
            val isCreator = couple.creatorUid == uid
            val ref = db.collection("couples").document(couple.id)
                .collection("daily_prompts").document(dateKey)
            val snap = ref.get().await()
            if (!snap.exists()) {
                val base = mutableMapOf<String, Any>(
                    "coupleId" to couple.id,
                    "dateKey" to dateKey,
                    "promptIndex" to promptIndex,
                    "memberUids" to couple.memberUids,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (isCreator) {
                    base["creatorAnswer"] = cleanAnswer
                    base["creatorAnsweredUid"] = uid
                } else {
                    base["partnerAnswer"] = cleanAnswer
                    base["partnerAnsweredUid"] = uid
                }
                ref.set(base).await()
            } else {
                val updateMap = if (isCreator) {
                    mapOf(
                        "creatorAnswer" to cleanAnswer,
                        "creatorAnsweredUid" to uid,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                } else {
                    mapOf(
                        "partnerAnswer" to cleanAnswer,
                        "partnerAnsweredUid" to uid,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                }
                ref.update(updateMap).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    // --- SHARED COUPLE BUCKET LIST ---

    fun observeBucketItems(coupleId: String, uid: String): Flow<List<BucketListItem>> =
        db.collection("couples").document(coupleId).collection("bucket_items")
            .whereArrayContains("memberUids", uid)
            .snapshots()
            .map { snap ->
                snap.toObjects(BucketListItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedWith(
                        compareBy<BucketListItem> { it.completed }
                            .thenByDescending { it.createdAt?.seconds ?: 0L }
                    )
            }
            .catch { error ->
                logFlowErrorIfSignedIn(error, OperationType.LIST, "couples/$coupleId/bucket_items", uid)
                throw error
            }

    suspend fun addBucketItem(
        couple: CoupleSpace,
        creatorName: String,
        title: String,
        category: String
    ): Result<Unit> {
        var docPath = "couples/${couple.id}/bucket_items"
        return try {
            val uid = requireUserId()
            val cleanTitle = title.trim().take(120)
            require(cleanTitle.isNotEmpty()) { "Dream title cannot be empty" }
            val validCat = BucketListCatalog.byKey(category).key
            val ref = db.collection("couples").document(couple.id).collection("bucket_items").document()
            docPath = ref.path
            ref.set(
                mapOf(
                    "coupleId" to couple.id,
                    "creatorUid" to uid,
                    "creatorName" to creatorName.trim().take(60).ifBlank { "Sweetheart" },
                    "memberUids" to couple.memberUids,
                    "title" to cleanTitle,
                    "category" to validCat,
                    "completed" to false,
                    "completedByUid" to null,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun toggleBucketItemCompleted(coupleId: String, item: BucketListItem): Result<Unit> {
        val docPath = "couples/$coupleId/bucket_items/${item.id}"
        return try {
            val uid = requireUserId()
            val nextCompleted = !item.completed
            db.collection("couples").document(coupleId)
                .collection("bucket_items").document(item.id)
                .update(
                    mapOf(
                        "completed" to nextCompleted,
                        "completedByUid" to if (nextCompleted) uid else null,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteBucketItem(coupleId: String, itemId: String): Result<Unit> {
        val docPath = "couples/$coupleId/bucket_items/$itemId"
        return try {
            requireUserId()
            db.collection("couples").document(coupleId)
                .collection("bucket_items").document(itemId)
                .delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    // --- 1. SECRET LOVE LETTERS & TIME CAPSULES ---

    fun observeLoveLetters(coupleId: String, uid: String): Flow<List<LoveLetter>> =
        db.collection("couples").document(coupleId).collection("love_letters")
            .whereArrayContains("memberUids", uid)
            .snapshots()
            .map { snap ->
                snap.toObjects(LoveLetter::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                logFlowErrorIfSignedIn(error, OperationType.LIST, "couples/$coupleId/love_letters", uid)
                throw error
            }

    suspend fun sendLoveLetter(
        couple: CoupleSpace,
        senderName: String,
        title: String,
        body: String,
        sealStyle: String,
        unlockAtMillis: Long
    ): Result<Unit> {
        var docPath = "couples/${couple.id}/love_letters"
        return try {
            val uid = requireUserId()
            val cleanTitle = title.trim().take(80)
            val cleanBody = body.trim().take(1500)
            require(cleanTitle.isNotEmpty() && cleanBody.isNotEmpty()) { "Letter title and message are required" }
            val validSeal = LetterSealCatalog.byKey(sealStyle).key
            val ref = db.collection("couples").document(couple.id).collection("love_letters").document()
            docPath = ref.path
            ref.set(
                mapOf(
                    "coupleId" to couple.id,
                    "senderUid" to uid,
                    "senderName" to senderName.trim().take(60).ifBlank { "Sweetheart" },
                    "memberUids" to couple.memberUids,
                    "title" to cleanTitle,
                    "body" to cleanBody,
                    "sealStyle" to validSeal,
                    "unlockAtMillis" to unlockAtMillis.coerceAtLeast(0L),
                    "openedByPartner" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun markLoveLetterOpened(coupleId: String, letterId: String): Result<Unit> {
        val docPath = "couples/$coupleId/love_letters/$letterId"
        return try {
            requireUserId()
            db.collection("couples").document(coupleId)
                .collection("love_letters").document(letterId)
                .update(
                    mapOf(
                        "openedByPartner" to true,
                        "openedAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteLoveLetter(coupleId: String, letterId: String): Result<Unit> {
        val docPath = "couples/$coupleId/love_letters/$letterId"
        return try {
            requireUserId()
            db.collection("couples").document(coupleId).collection("love_letters").document(letterId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    // --- 2. SHARED WISH & LOVE JAR ---

    fun observeLoveJarNotes(coupleId: String, uid: String): Flow<List<LoveJarNote>> =
        db.collection("couples").document(coupleId).collection("love_jar")
            .whereArrayContains("memberUids", uid)
            .snapshots()
            .map { snap ->
                snap.toObjects(LoveJarNote::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                logFlowErrorIfSignedIn(error, OperationType.LIST, "couples/$coupleId/love_jar", uid)
                throw error
            }

    suspend fun addLoveJarNote(
        couple: CoupleSpace,
        authorName: String,
        kind: String,
        text: String
    ): Result<Unit> {
        var docPath = "couples/${couple.id}/love_jar"
        return try {
            val uid = requireUserId()
            val cleanText = text.trim().take(240)
            require(cleanText.isNotEmpty()) { "Note text cannot be empty" }
            val validKind = LoveJarKindCatalog.byKey(kind).key
            val ref = db.collection("couples").document(couple.id).collection("love_jar").document()
            docPath = ref.path
            ref.set(
                mapOf(
                    "coupleId" to couple.id,
                    "authorUid" to uid,
                    "authorName" to authorName.trim().take(60).ifBlank { "Sweetheart" },
                    "memberUids" to couple.memberUids,
                    "kind" to validKind,
                    "text" to cleanText,
                    "granted" to false,
                    "grantedByUid" to null,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun toggleLoveJarGranted(coupleId: String, note: LoveJarNote): Result<Unit> {
        val docPath = "couples/$coupleId/love_jar/${note.id}"
        return try {
            val uid = requireUserId()
            val next = !note.granted
            db.collection("couples").document(coupleId)
                .collection("love_jar").document(note.id)
                .update(
                    mapOf(
                        "granted" to next,
                        "grantedByUid" to if (next) uid else null,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteLoveJarNote(coupleId: String, noteId: String): Result<Unit> {
        val docPath = "couples/$coupleId/love_jar/$noteId"
        return try {
            requireUserId()
            db.collection("couples").document(coupleId).collection("love_jar").document(noteId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    // --- 3. SHARED MUSIC & VIBE DEDICATIONS ---

    fun observeVibeDedications(coupleId: String, uid: String): Flow<List<VibeDedication>> =
        db.collection("couples").document(coupleId).collection("vibe_dedications")
            .whereArrayContains("memberUids", uid)
            .snapshots()
            .map { snap ->
                snap.toObjects(VibeDedication::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                logFlowErrorIfSignedIn(error, OperationType.LIST, "couples/$coupleId/vibe_dedications", uid)
                throw error
            }

    suspend fun sendVibeDedication(
        couple: CoupleSpace,
        senderName: String,
        songTitle: String,
        artistName: String,
        vibeEmoji: String,
        dedicationNote: String
    ): Result<Unit> {
        var docPath = "couples/${couple.id}/vibe_dedications"
        return try {
            val uid = requireUserId()
            val cleanSong = songTitle.trim().take(100)
            val cleanArtist = artistName.trim().take(80).ifBlank { "Favorite Artist" }
            require(cleanSong.isNotEmpty()) { "Song title cannot be empty" }
            val ref = db.collection("couples").document(couple.id).collection("vibe_dedications").document()
            docPath = ref.path
            ref.set(
                mapOf(
                    "coupleId" to couple.id,
                    "senderUid" to uid,
                    "senderName" to senderName.trim().take(60).ifBlank { "Sweetheart" },
                    "memberUids" to couple.memberUids,
                    "songTitle" to cleanSong,
                    "artistName" to cleanArtist,
                    "vibeEmoji" to vibeEmoji.trim().take(16).ifBlank { "🎶" },
                    "dedicationNote" to dedicationNote.trim().take(220),
                    "lovedByPartner" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun toggleVibeLoved(coupleId: String, vibe: VibeDedication): Result<Unit> {
        val docPath = "couples/$coupleId/vibe_dedications/${vibe.id}"
        return try {
            requireUserId()
            db.collection("couples").document(coupleId)
                .collection("vibe_dedications").document(vibe.id)
                .update(
                    mapOf(
                        "lovedByPartner" to !vibe.lovedByPartner,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteVibeDedication(coupleId: String, vibeId: String): Result<Unit> {
        val docPath = "couples/$coupleId/vibe_dedications/$vibeId"
        return try {
            requireUserId()
            db.collection("couples").document(coupleId).collection("vibe_dedications").document(vibeId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    // --- 4. CARE & WELLNESS CHECK-INS ---

    fun observeCareCheckIns(coupleId: String, uid: String): Flow<List<CareCheckIn>> =
        db.collection("couples").document(coupleId).collection("care_checkins")
            .whereArrayContains("memberUids", uid)
            .snapshots()
            .map { snap ->
                snap.toObjects(CareCheckIn::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                logFlowErrorIfSignedIn(error, OperationType.LIST, "couples/$coupleId/care_checkins", uid)
                throw error
            }

    suspend fun sendCareCheckIn(
        couple: CoupleSpace,
        senderName: String,
        careType: String,
        message: String
    ): Result<Unit> {
        var docPath = "couples/${couple.id}/care_checkins"
        return try {
            val uid = requireUserId()
            val preset = CareCheckInCatalog.byKey(careType)
            val cleanMsg = message.trim().take(180).ifBlank { preset.defaultMessage }
            val ref = db.collection("couples").document(couple.id).collection("care_checkins").document()
            docPath = ref.path
            ref.set(
                mapOf(
                    "coupleId" to couple.id,
                    "senderUid" to uid,
                    "senderName" to senderName.trim().take(60).ifBlank { "Sweetheart" },
                    "memberUids" to couple.memberUids,
                    "careType" to preset.key,
                    "message" to cleanMsg,
                    "acknowledged" to false,
                    "acknowledgedByUid" to null,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun acknowledgeCareCheckIn(coupleId: String, checkInId: String): Result<Unit> {
        val docPath = "couples/$coupleId/care_checkins/$checkInId"
        return try {
            val uid = requireUserId()
            db.collection("couples").document(coupleId)
                .collection("care_checkins").document(checkInId)
                .update(
                    mapOf(
                        "acknowledged" to true,
                        "acknowledgedByUid" to uid,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    // --- 5. SHARED LOVE DOODLE CANVAS ---

    fun observeLoveDoodles(coupleId: String, uid: String): Flow<List<LoveDoodle>> =
        db.collection("couples").document(coupleId).collection("love_doodles")
            .whereArrayContains("memberUids", uid)
            .snapshots()
            .map { snap ->
                snap.toObjects(LoveDoodle::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                logFlowErrorIfSignedIn(error, OperationType.LIST, "couples/$coupleId/love_doodles", uid)
                throw error
            }

    suspend fun sendLoveDoodle(
        couple: CoupleSpace,
        artistName: String,
        title: String,
        imageBase64: String
    ): Result<Unit> {
        var docPath = "couples/${couple.id}/love_doodles"
        return try {
            val uid = requireUserId()
            require(imageBase64.isNotBlank() && imageBase64.length <= 360_000) { "Doodle image is invalid or too large" }
            val ref = db.collection("couples").document(couple.id).collection("love_doodles").document()
            docPath = ref.path
            ref.set(
                mapOf(
                    "coupleId" to couple.id,
                    "artistUid" to uid,
                    "artistName" to artistName.trim().take(60).ifBlank { "Sweetheart" },
                    "memberUids" to couple.memberUids,
                    "title" to title.trim().take(80),
                    "imageBase64" to imageBase64,
                    "lovedByPartner" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun toggleDoodleLoved(coupleId: String, doodle: LoveDoodle): Result<Unit> {
        val docPath = "couples/$coupleId/love_doodles/${doodle.id}"
        return try {
            requireUserId()
            db.collection("couples").document(coupleId)
                .collection("love_doodles").document(doodle.id)
                .update(
                    mapOf(
                        "lovedByPartner" to !doodle.lovedByPartner,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteLoveDoodle(coupleId: String, doodleId: String): Result<Unit> {
        val docPath = "couples/$coupleId/love_doodles/$doodleId"
        return try {
            requireUserId()
            db.collection("couples").document(coupleId).collection("love_doodles").document(doodleId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    // --- 6. COUPLE PROMISE & PINKY-SWEAR VAULT ---

    fun observeCouplePromises(coupleId: String, uid: String): Flow<List<CouplePromise>> =
        db.collection("couples").document(coupleId).collection("couple_promises")
            .whereArrayContains("memberUids", uid)
            .snapshots()
            .map { snap ->
                snap.toObjects(CouplePromise::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                logFlowErrorIfSignedIn(error, OperationType.LIST, "couples/$coupleId/couple_promises", uid)
                throw error
            }

    suspend fun addCouplePromise(
        couple: CoupleSpace,
        creatorName: String,
        title: String,
        category: String
    ): Result<Unit> {
        var docPath = "couples/${couple.id}/couple_promises"
        return try {
            val uid = requireUserId()
            val cleanTitle = title.trim().take(160)
            require(cleanTitle.isNotEmpty()) { "Promise cannot be empty" }
            val validCat = PromiseCatalog.byKey(category).key
            val ref = db.collection("couples").document(couple.id).collection("couple_promises").document()
            docPath = ref.path
            ref.set(
                mapOf(
                    "coupleId" to couple.id,
                    "creatorUid" to uid,
                    "creatorName" to creatorName.trim().take(60).ifBlank { "Sweetheart" },
                    "memberUids" to couple.memberUids,
                    "title" to cleanTitle,
                    "category" to validCat,
                    "signedByUids" to listOf(uid),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun coSignPromise(coupleId: String, promise: CouplePromise): Result<Unit> {
        val docPath = "couples/$coupleId/couple_promises/${promise.id}"
        return try {
            val uid = requireUserId()
            val nextSigners = (promise.signedByUids + uid).distinct().take(2)
            db.collection("couples").document(coupleId)
                .collection("couple_promises").document(promise.id)
                .update(
                    mapOf(
                        "signedByUids" to nextSigners,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteCouplePromise(coupleId: String, promiseId: String): Result<Unit> {
        val docPath = "couples/$coupleId/couple_promises/$promiseId"
        return try {
            requireUserId()
            db.collection("couples").document(coupleId).collection("couple_promises").document(promiseId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    // --- HELPERS ---

    private suspend fun generateUniqueSixDigitCode(excludeCode: String? = null): String {
        val cleanExclude = excludeCode?.filter { it.isDigit() }?.takeIf { it.length == 6 }
        repeat(64) {
            val candidate = (100_000 + secureRandom.nextInt(900_000)).toString()
            if (candidate == cleanExclude) return@repeat
            val existing = db.collection("pair_codes").document(candidate).get().await()
            if (!existing.exists()) return candidate
        }
        throw IllegalStateException("Could not allocate a fresh 6-digit invite code. Please try again.")
    }

    companion object {
        private val ALL_COUPLE_SUBCOLLECTIONS = listOf(
            "messages",
            "moments",
            "moods",
            "events",
            "daily_prompts",
            "bucket_items",
            "love_letters",
            "love_jar",
            "vibe_dedications",
            "care_checkins",
            "love_doodles",
            "couple_promises",
            "presence"
        )
    }

    private fun deriveInitials(name: String): String {
        val parts = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        return when {
            parts.size >= 2 -> "${parts[0].first().uppercaseChar()}${parts[1].first().uppercaseChar()}"
            parts.size == 1 -> parts[0].take(2).uppercase()
            else -> "DU"
        }
    }

    private fun logFlowErrorIfSignedIn(
        error: Throwable,
        operationType: OperationType,
        path: String,
        expectedUid: String
    ) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) return
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == expectedUid) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
    }
}
