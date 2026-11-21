const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_101";
const BOB_UID = "bob_uid_202";
const CHARLIE_UID = "charlie_uid_303";
const COUPLE_ID = "couple_abc123";
const INVITE_CODE = "482917";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

const pastDate = (offsetMs = 2000) => new Date(Date.now() - offsetMs);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

async function seedPairedCouple() {
  const created = pastDate(10000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("couples").doc(COUPLE_ID).set({
      creatorUid: ALICE_UID,
      partnerUid: BOB_UID,
      memberUids: [ALICE_UID, BOB_UID],
      inviteCode: INVITE_CODE,
      status: "paired",
      creatorName: "Maya",
      partnerName: "Liam",
      creatorPublicKey: "ALICE_PUB_KEY_BASE64",
      partnerPublicKey: "BOB_PUB_KEY_BASE64",
      wrappedVaultKey: "WRAPPED_VAULT_KEY_BASE64",
      lovePulseCount: 2,
      createdAt: created,
      updatedAt: created,
    });
  });
}

test("Unauthenticated users are rejected across all collections", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauth.collection("users").doc(ALICE_UID).get());
  await assertFails(unauth.collection("pair_codes").doc(INVITE_CODE).get());
  await assertFails(unauth.collection("couples").doc(COUPLE_ID).get());
  await assertFails(
    unauth.collection("couples").doc(COUPLE_ID).collection("messages").get()
  );
});

test("User profiles enforce owner isolation and block list queries and ghost fields", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = pastDate(5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Maya",
      avatarInitials: "MY",
      publicKey: "ALICE_PUB_KEY_BASE64",
      shareDeviceStatus: true,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Ghost field rejected (Shadow Update test)
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: pastDate(1000),
    })
  );

  // Valid partial update succeeds (including partnerNickname)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      coupleId: COUPLE_ID,
      birthdayMonth: 5,
      birthdayDay: 14,
      partnerNickname: "My Sunshine",
      updatedAt: pastDate(1000),
    })
  );

  // Oversized partnerNickname (>40 chars) rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      partnerNickname: "A".repeat(41),
      updatedAt: pastDate(800),
    })
  );

  // Cross-user read & list rejected
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").get());
  await assertFails(aliceDb.collection("users").get());
});

test("6-digit pair code creation and atomic partner join flow", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const created = pastDate(10000);

  // Alice atomically creates couple space and 6-digit pair code
  const createBatch = aliceDb.batch();
  createBatch.set(aliceDb.collection("couples").doc(COUPLE_ID), {
    creatorUid: ALICE_UID,
    memberUids: [ALICE_UID],
    inviteCode: INVITE_CODE,
    status: "waiting",
    creatorName: "Maya",
    creatorPublicKey: "ALICE_PUB_KEY_BASE64",
    wrappedVaultKey: "WRAPPED_VAULT_KEY_BASE64",
    lovePulseCount: 0,
    createdAt: created,
    updatedAt: created,
  });
  createBatch.set(aliceDb.collection("pair_codes").doc(INVITE_CODE), {
    code: INVITE_CODE,
    coupleId: COUPLE_ID,
    creatorUid: ALICE_UID,
    creatorName: "Maya",
    status: "open",
    createdAt: created,
    updatedAt: created,
  });
  await assertSucceeds(createBatch.commit());

  // Pair codes cannot be listed (prevents scraping)
  await assertFails(bobDb.collection("pair_codes").get());

  // Bob can exact-lookup the 6-digit code
  await assertSucceeds(bobDb.collection("pair_codes").doc(INVITE_CODE).get());

  // Bob cannot read the couple before joining
  await assertFails(bobDb.collection("couples").doc(COUPLE_ID).get());

  // Bob atomically claims pair code and joins couple
  const joinTime = pastDate(2000);
  const joinBatch = bobDb.batch();
  joinBatch.update(bobDb.collection("pair_codes").doc(INVITE_CODE), {
    status: "claimed",
    claimedByUid: BOB_UID,
    updatedAt: joinTime,
  });
  joinBatch.update(bobDb.collection("couples").doc(COUPLE_ID), {
    partnerUid: BOB_UID,
    partnerName: "Liam",
    partnerPublicKey: "BOB_PUB_KEY_BASE64",
    memberUids: [ALICE_UID, BOB_UID],
    status: "paired",
    updatedAt: joinTime,
  });
  await assertSucceeds(joinBatch.commit());

  // Now Bob can read the couple, and filtered couple query works
  await assertSucceeds(bobDb.collection("couples").doc(COUPLE_ID).get());
  await assertSucceeds(
    bobDb.collection("couples").where("memberUids", "array-contains", BOB_UID).get()
  );

  // Unfiltered couples list rejected
  await assertFails(bobDb.collection("couples").get());

  // Third party Charlie cannot claim already-claimed code or read couple
  await assertFails(
    charlieDb.collection("pair_codes").doc(INVITE_CODE).update({
      status: "claimed",
      claimedByUid: CHARLIE_UID,
      updatedAt: pastDate(500),
    })
  );
  await assertFails(charlieDb.collection("couples").doc(COUPLE_ID).get());
});

test("Couple love pulse, dates update, presence telemetry, and subcollection isolation", async () => {
  await seedPairedCouple();

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const now = pastDate(2000);

  // Alice updates anniversary and birthday dates
  await assertSucceeds(
    aliceDb.collection("couples").doc(COUPLE_ID).update({
      anniversaryYear: 2024,
      anniversaryMonth: 6,
      anniversaryDay: 14,
      creatorBirthdayMonth: 3,
      creatorBirthdayDay: 22,
      partnerBirthdayMonth: 9,
      partnerBirthdayDay: 8,
      updatedAt: now,
    })
  );

  // Bob sends a Love Pulse (increments lovePulseCount by +1)
  await assertSucceeds(
    bobDb.collection("couples").doc(COUPLE_ID).update({
      lovePulseCount: 3,
      lastPulseByUid: BOB_UID,
      lastPulseAt: now,
      updatedAt: now,
    })
  );

  // Future tampered timestamp fails
  await assertFails(
    bobDb.collection("couples").doc(COUPLE_ID).update({
      lovePulseCount: 4,
      lastPulseByUid: BOB_UID,
      lastPulseAt: new Date(Date.now() + 60000),
      updatedAt: new Date(Date.now() + 60000),
    })
  );

  // First-time get() on non-existent presence doc succeeds for owner, fails for Charlie
  await assertSucceeds(
    aliceDb
      .collection("couples")
      .doc(COUPLE_ID)
      .collection("presence")
      .doc(ALICE_UID)
      .get()
  );
  await assertFails(
    charlieDb
      .collection("couples")
      .doc(COUPLE_ID)
      .collection("presence")
      .doc(CHARLIE_UID)
      .get()
  );

  // Presence: Alice writes her real battery, network, lock status & mood
  await assertSucceeds(
    aliceDb
      .collection("couples")
      .doc(COUPLE_ID)
      .collection("presence")
      .doc(ALICE_UID)
      .set({
        userId: ALICE_UID,
        coupleId: COUPLE_ID,
        memberUids: [ALICE_UID, BOB_UID],
        displayName: "Maya",
        batteryPercent: 86,
        charging: false,
        networkType: "WIFI",
        deviceStatus: "IN_USE",
        activeAppName: "Instagram",
        deviceName: "Google Pixel 9 Pro",
        deviceSpecsJson: JSON.stringify({ deviceName: "Google Pixel 9 Pro", androidVersion: "Android 15", securityPatch: "2026-10-05" }),
        moodKey: "MISSING_YOU",
        moodNote: "Can't wait for tonight",
        statusShared: true,
        lastSeenAt: now,
        createdAt: now,
        updatedAt: now,
      })
  );

  // Alice updates active foreground app to WhatsApp, device specs, typing indicator & lastSeenAt; oversized values rejected
  await assertSucceeds(
    aliceDb
      .collection("couples")
      .doc(COUPLE_ID)
      .collection("presence")
      .doc(ALICE_UID)
      .update({
        activeAppName: "WhatsApp",
        deviceName: "Samsung Galaxy S25 Ultra",
        deviceSpecsJson: JSON.stringify({ deviceName: "Samsung Galaxy S25 Ultra", androidVersion: "Android 16", securityPatch: "2026-11-05" }),
        typingActive: true,
        typingUpdatedAt: pastDate(1000),
        lastSeenAt: pastDate(1000),
        updatedAt: pastDate(1000),
      })
  );
  await assertFails(
    aliceDb
      .collection("couples")
      .doc(COUPLE_ID)
      .collection("presence")
      .doc(ALICE_UID)
      .update({
        activeAppName: "X".repeat(61),
        updatedAt: pastDate(500),
      })
  );
  await assertFails(
    aliceDb
      .collection("couples")
      .doc(COUPLE_ID)
      .collection("presence")
      .doc(ALICE_UID)
      .update({
        deviceSpecsJson: "D".repeat(4097),
        updatedAt: pastDate(500),
      })
  );

  // Bob can observe presence with memberUids filter; Charlie cannot
  await assertSucceeds(
    bobDb
      .collection("couples")
      .doc(COUPLE_ID)
      .collection("presence")
      .where("memberUids", "array-contains", BOB_UID)
      .get()
  );
  await assertFails(
    charlieDb
      .collection("couples")
      .doc(COUPLE_ID)
      .collection("presence")
      .doc(ALICE_UID)
      .get()
  );

  // Bob cannot overwrite Alice's presence
  await assertFails(
    bobDb
      .collection("couples")
      .doc(COUPLE_ID)
      .collection("presence")
      .doc(ALICE_UID)
      .update({
        batteryPercent: 10,
        updatedAt: pastDate(500),
      })
  );
});

test("E2EE messages, shared moods, moments, events, and daily prompt parity", async () => {
  await seedPairedCouple();

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const now = pastDate(3000);

  // 1. E2EE Message create + partner reaction
  const msgRef = aliceDb
    .collection("couples")
    .doc(COUPLE_ID)
    .collection("messages")
    .doc("msg_1");
  await assertSucceeds(
    msgRef.set({
      coupleId: COUPLE_ID,
      senderUid: ALICE_UID,
      senderName: "Maya",
      memberUids: [ALICE_UID, BOB_UID],
      messageType: "TEXT",
      keyMode: "ECDH_P256",
      ciphertext: "ENCRYPTED_BASE64_CIPHERTEXT",
      iv: "RANDOM_12B_IV",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Bob reacts to Alice's message using per-user reactions map
  await assertSucceeds(
    bobDb
      .collection("couples")
      .doc(COUPLE_ID)
      .collection("messages")
      .doc("msg_1")
      .update({
        [`reactions.${BOB_UID}`]: "HEART",
        updatedAt: pastDate(1000),
      })
  );

  // Alice adds her own reaction concurrently without overwriting Bob's reaction
  await assertSucceeds(
    aliceDb
      .collection("couples")
      .doc(COUPLE_ID)
      .collection("messages")
      .doc("msg_1")
      .update({
        [`reactions.${ALICE_UID}`]: "FIRE",
        updatedAt: pastDate(950),
      })
  );

  // Bob CANNOT spoof or alter Alice's reaction entry
  await assertFails(
    bobDb
      .collection("couples")
      .doc(COUPLE_ID)
      .collection("messages")
      .doc("msg_1")
      .update({
        [`reactions.${ALICE_UID}`]: "SAD",
        updatedAt: pastDate(920),
      })
  );

  // Bob can remove his own reaction cleanly without touching Alice's reaction
  await assertSucceeds(
    bobDb
      .collection("couples")
      .doc(COUPLE_ID)
      .collection("messages")
      .doc("msg_1")
      .update({
        reactions: { [ALICE_UID]: "FIRE" },
        updatedAt: pastDate(910),
      })
  );

  // Charlie cannot delete couple's message, while partner Bob CAN delete message
  await assertFails(
    charlieDb
      .collection("couples")
      .doc(COUPLE_ID)
      .collection("messages")
      .doc("msg_1")
      .delete()
  );

  // Bob (recipient) marks Alice's message as Seen; Alice (sender) cannot mark her own message as Seen
  await assertSucceeds(
    bobDb
      .collection("couples")
      .doc(COUPLE_ID)
      .collection("messages")
      .doc("msg_1")
      .update({
        seenByUid: BOB_UID,
        seenAt: pastDate(900),
        updatedAt: pastDate(900),
      })
  );
  await assertFails(
    aliceDb
      .collection("couples")
      .doc(COUPLE_ID)
      .collection("messages")
      .doc("msg_1")
      .update({
        seenByUid: ALICE_UID,
        seenAt: pastDate(800),
        updatedAt: pastDate(800),
      })
  );
  await assertFails(
    bobDb
      .collection("couples")
      .doc(COUPLE_ID)
      .collection("messages")
      .doc("msg_1")
      .update({
        seenByUid: ALICE_UID,
        seenAt: pastDate(800),
        updatedAt: pastDate(800),
      })
  );

  // Bob cannot alter Alice's ciphertext
  await assertFails(
    bobDb
      .collection("couples")
      .doc(COUPLE_ID)
      .collection("messages")
      .doc("msg_1")
      .update({
        ciphertext: "TAMPERED",
        updatedAt: pastDate(500),
      })
  );

  // Filtered messages query succeeds; unfiltered fails
  await assertSucceeds(
    bobDb
      .collection("couples")
      .doc(COUPLE_ID)
      .collection("messages")
      .where("memberUids", "array-contains", BOB_UID)
      .get()
  );
  await assertFails(
    bobDb.collection("couples").doc(COUPLE_ID).collection("messages").get()
  );

  // Couple member Bob can delete a message in the private chat
  await assertSucceeds(
    bobDb
      .collection("couples")
      .doc(COUPLE_ID)
      .collection("messages")
      .doc("msg_1")
      .delete()
  );

  // 2. Shared Mood Entry + partner comfort hug
  await assertSucceeds(
    aliceDb
      .collection("couples")
      .doc(COUPLE_ID)
      .collection("moods")
      .doc("mood_1")
      .set({
        coupleId: COUPLE_ID,
        userId: ALICE_UID,
        authorName: "Maya",
        memberUids: [ALICE_UID, BOB_UID],
        moodKey: "NEED_HUGS",
        note: "Long day at work",
        energyLevel: 2,
        createdAt: now,
        updatedAt: now,
      })
  );
  await assertSucceeds(
    bobDb
      .collection("couples")
      .doc(COUPLE_ID)
      .collection("moods")
      .doc("mood_1")
      .update({
        comfortSentByUid: BOB_UID,
        updatedAt: pastDate(1000),
      })
  );

  // 3. Shared Encrypted Photo Moment + love toggle
  await assertSucceeds(
    aliceDb
      .collection("couples")
      .doc(COUPLE_ID)
      .collection("moments")
      .doc("mom_1")
      .set({
        coupleId: COUPLE_ID,
        uploaderUid: ALICE_UID,
        uploaderName: "Maya",
        memberUids: [ALICE_UID, BOB_UID],
        keyMode: "ECDH_P256",
        ciphertext: "ENCRYPTED_PHOTO_BLOB",
        iv: "RANDOM_IV_2",
        lovedByUids: [ALICE_UID],
        createdAt: now,
        updatedAt: now,
      })
  );
  await assertSucceeds(
    bobDb
      .collection("couples")
      .doc(COUPLE_ID)
      .collection("moments")
      .doc("mom_1")
      .update({
        lovedByUids: [ALICE_UID, BOB_UID],
        updatedAt: pastDate(1000),
      })
  );

  // 4. Custom Special Date Event
  await assertSucceeds(
    bobDb
      .collection("couples")
      .doc(COUPLE_ID)
      .collection("events")
      .doc("evt_1")
      .set({
        coupleId: COUPLE_ID,
        creatorUid: BOB_UID,
        memberUids: [ALICE_UID, BOB_UID],
        title: "Kyoto Autumn Trip",
        category: "TRIP",
        year: 2027,
        month: 10,
        day: 12,
        repeatsYearly: false,
        createdAt: now,
        updatedAt: now,
      })
  );

  // 5. Daily Couple Prompt: non-existent get() succeeds for couple member, fails for Charlie
  await assertSucceeds(
    aliceDb
      .collection("couples")
      .doc(COUPLE_ID)
      .collection("daily_prompts")
      .doc("2026-11-19")
      .get()
  );
  await assertFails(
    charlieDb
      .collection("couples")
      .doc(COUPLE_ID)
      .collection("daily_prompts")
      .doc("2026-11-19")
      .get()
  );

  // Alice answers first, Bob unlocks with his answer
  await assertSucceeds(
    aliceDb
      .collection("couples")
      .doc(COUPLE_ID)
      .collection("daily_prompts")
      .doc("2026-11-19")
      .set({
        coupleId: COUPLE_ID,
        dateKey: "2026-11-19",
        promptIndex: 7,
        memberUids: [ALICE_UID, BOB_UID],
        creatorAnswer: "When you brought me warm tea this morning.",
        creatorAnsweredUid: ALICE_UID,
        createdAt: now,
        updatedAt: now,
      })
  );
  await assertSucceeds(
    bobDb
      .collection("couples")
      .doc(COUPLE_ID)
      .collection("daily_prompts")
      .doc("2026-11-19")
      .update({
        partnerAnswer: "Listening to rain with you on the balcony.",
        partnerAnsweredUid: BOB_UID,
        updatedAt: pastDate(1000),
      })
  );

  // Bob cannot overwrite Alice's prompt answer
  await assertFails(
    bobDb
      .collection("couples")
      .doc(COUPLE_ID)
      .collection("daily_prompts")
      .doc("2026-11-19")
      .update({
        creatorAnswer: "Overwritten",
        updatedAt: pastDate(500),
      })
  );

  // Charlie cannot read Bob/Alice moment, run unfiltered list, query Alice's UID, or write
  await assertFails(
    charlieDb
      .collection("couples")
      .doc(COUPLE_ID)
      .collection("moments")
      .doc("mom_1")
      .get()
  );
  await assertFails(
    charlieDb
      .collection("couples")
      .doc(COUPLE_ID)
      .collection("moments")
      .get()
  );
  await assertFails(
    charlieDb
      .collection("couples")
      .doc(COUPLE_ID)
      .collection("moments")
      .where("memberUids", "array-contains", ALICE_UID)
      .get()
  );
  await assertFails(
    charlieDb
      .collection("couples")
      .doc(COUPLE_ID)
      .collection("moments")
      .doc("mom_spoof")
      .set({
        coupleId: COUPLE_ID,
        uploaderUid: CHARLIE_UID,
        uploaderName: "Charlie",
        memberUids: [CHARLIE_UID],
        keyMode: "ECDH_P256",
        ciphertext: "SPOOF",
        iv: "IV",
        lovedByUids: [],
        createdAt: now,
        updatedAt: now,
      })
  );

  // 6. Shared Couple Bucket List (create, filtered list, partner completion toggle, delete, deny outsider & invalid schema)
  const bucketRef = aliceDb
    .collection("couples")
    .doc(COUPLE_ID)
    .collection("bucket_items")
    .doc("dream_1");

  await assertSucceeds(
    bucketRef.set({
      coupleId: COUPLE_ID,
      creatorUid: ALICE_UID,
      creatorName: "Maya",
      memberUids: [ALICE_UID, BOB_UID],
      title: "Watch sunrise together on a beach",
      category: "TRAVEL",
      completed: false,
      completedByUid: null,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Filtered query works for Bob; unfiltered list fails
  await assertSucceeds(
    bobDb
      .collection("couples")
      .doc(COUPLE_ID)
      .collection("bucket_items")
      .where("memberUids", "array-contains", BOB_UID)
      .get()
  );
  await assertFails(
    bobDb.collection("couples").doc(COUPLE_ID).collection("bucket_items").get()
  );

  // Partner Bob marks dream completed
  await assertSucceeds(
    bobDb
      .collection("couples")
      .doc(COUPLE_ID)
      .collection("bucket_items")
      .doc("dream_1")
      .update({
        completed: true,
        completedByUid: BOB_UID,
        updatedAt: pastDate(800),
      })
  );

  // Invalid category & spoofed completedByUid fail
  await assertFails(
    bobDb
      .collection("couples")
      .doc(COUPLE_ID)
      .collection("bucket_items")
      .doc("dream_1")
      .update({
        category: "INVALID_CAT",
        updatedAt: pastDate(700),
      })
  );
  await assertFails(
    bobDb
      .collection("couples")
      .doc(COUPLE_ID)
      .collection("bucket_items")
      .doc("dream_1")
      .update({
        completedByUid: ALICE_UID,
        updatedAt: pastDate(650),
      })
  );

  // Outsider Charlie cannot read or delete bucket_items
  await assertFails(
    charlieDb
      .collection("couples")
      .doc(COUPLE_ID)
      .collection("bucket_items")
      .doc("dream_1")
      .get()
  );
  await assertFails(
    charlieDb
      .collection("couples")
      .doc(COUPLE_ID)
      .collection("bucket_items")
      .doc("dream_1")
      .delete()
  );

  // Couple member Bob can delete a bucket list item
  await assertSucceeds(
    bobDb
      .collection("couples")
      .doc(COUPLE_ID)
      .collection("bucket_items")
      .doc("dream_1")
      .delete()
  );
});

test("HD ciphertext sizing and 6 new couple facilities enforce zero-trust isolation", async () => {
  await seedPairedCouple();

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const now = pastDate(2500);

  // 0. HD encrypted photo message (~620KB ciphertext) succeeds; oversized (>880KB) fails
  await assertSucceeds(
    aliceDb
      .collection("couples")
      .doc(COUPLE_ID)
      .collection("messages")
      .doc("hd_img_1")
      .set({
        coupleId: COUPLE_ID,
        senderUid: ALICE_UID,
        senderName: "Maya",
        memberUids: [ALICE_UID, BOB_UID],
        messageType: "IMAGE",
        keyMode: "ECDH_P256",
        ciphertext: "H".repeat(620000),
        iv: "RANDOM_12B_IV",
        createdAt: now,
        updatedAt: now,
      })
  );
  await assertFails(
    aliceDb
      .collection("couples")
      .doc(COUPLE_ID)
      .collection("messages")
      .doc("hd_img_too_big")
      .set({
        coupleId: COUPLE_ID,
        senderUid: ALICE_UID,
        senderName: "Maya",
        memberUids: [ALICE_UID, BOB_UID],
        messageType: "IMAGE",
        keyMode: "ECDH_P256",
        ciphertext: "H".repeat(880001),
        iv: "RANDOM_12B_IV",
        createdAt: now,
        updatedAt: now,
      })
  );

  // 1. Secret Love Letters & Time Capsules
  const letterRef = aliceDb.collection("couples").doc(COUPLE_ID).collection("love_letters").doc("let_1");
  await assertSucceeds(
    letterRef.set({
      coupleId: COUPLE_ID,
      senderUid: ALICE_UID,
      senderName: "Maya",
      memberUids: [ALICE_UID, BOB_UID],
      title: "Open on our Anniversary",
      body: "You make every single day warmer and brighter.",
      sealStyle: "WAX_ROSE",
      unlockAtMillis: Date.now() - 1000,
      openedByPartner: false,
      createdAt: now,
      updatedAt: now,
    })
  );
  // Sender cannot mark own letter opened before partner opens it
  await assertFails(
    letterRef.update({ openedByPartner: true, openedAt: pastDate(600), updatedAt: pastDate(600) })
  );
  await assertSucceeds(
    bobDb.collection("couples").doc(COUPLE_ID).collection("love_letters").doc("let_1").update({
      openedByPartner: true,
      openedAt: pastDate(500),
      updatedAt: pastDate(500),
    })
  );
  // Outsider Charlie and unauth cannot read
  await assertFails(
    charlieDb.collection("couples").doc(COUPLE_ID).collection("love_letters").doc("let_1").get()
  );
  await assertFails(
    unauthDb.collection("couples").doc(COUPLE_ID).collection("love_letters").doc("let_1").get()
  );

  // 2. Shared Love Jar
  const jarRef = aliceDb.collection("couples").doc(COUPLE_ID).collection("love_jar").doc("jar_1");
  await assertSucceeds(
    jarRef.set({
      coupleId: COUPLE_ID,
      authorUid: ALICE_UID,
      authorName: "Maya",
      memberUids: [ALICE_UID, BOB_UID],
      kind: "WISH",
      text: "Midnight chai drive when it rains",
      granted: false,
      grantedByUid: null,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    bobDb.collection("couples").doc(COUPLE_ID).collection("love_jar").doc("jar_1").update({
      granted: true,
      grantedByUid: BOB_UID,
      updatedAt: pastDate(500),
    })
  );
  await assertFails(
    charlieDb.collection("couples").doc(COUPLE_ID).collection("love_jar").where("memberUids", "array-contains", ALICE_UID).get()
  );

  // 3. Shared Music & Vibe Dedications
  const vibeRef = bobDb.collection("couples").doc(COUPLE_ID).collection("vibe_dedications").doc("vibe_1");
  await assertSucceeds(
    vibeRef.set({
      coupleId: COUPLE_ID,
      senderUid: BOB_UID,
      senderName: "Liam",
      memberUids: [ALICE_UID, BOB_UID],
      songTitle: "Raabta",
      artistName: "Arijit Singh",
      vibeEmoji: "🎶",
      dedicationNote: "Heard this on my drive and immediately missed you.",
      lovedByPartner: false,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    aliceDb.collection("couples").doc(COUPLE_ID).collection("vibe_dedications").doc("vibe_1").update({
      lovedByPartner: true,
      updatedAt: pastDate(500),
    })
  );
  await assertFails(
    charlieDb.collection("couples").doc(COUPLE_ID).collection("vibe_dedications").doc("vibe_1").get()
  );

  // 4. Care & Wellness Check-Ins
  const careRef = aliceDb.collection("couples").doc(COUPLE_ID).collection("care_checkins").doc("care_1");
  await assertSucceeds(
    careRef.set({
      coupleId: COUPLE_ID,
      senderUid: ALICE_UID,
      senderName: "Maya",
      memberUids: [ALICE_UID, BOB_UID],
      careType: "WATER",
      message: "Drink a big glass of water right now!",
      acknowledged: false,
      acknowledgedByUid: null,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    bobDb.collection("couples").doc(COUPLE_ID).collection("care_checkins").doc("care_1").update({
      acknowledged: true,
      acknowledgedByUid: BOB_UID,
      updatedAt: pastDate(500),
    })
  );
  await assertFails(
    charlieDb.collection("couples").doc(COUPLE_ID).collection("care_checkins").doc("care_1").get()
  );

  // 5. Shared Love Doodle Canvas
  const doodleRef = aliceDb.collection("couples").doc(COUPLE_ID).collection("love_doodles").doc("dood_1");
  await assertSucceeds(
    doodleRef.set({
      coupleId: COUPLE_ID,
      artistUid: ALICE_UID,
      artistName: "Maya",
      memberUids: [ALICE_UID, BOB_UID],
      title: "Little heart for you",
      imageBase64: "R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7",
      lovedByPartner: false,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    bobDb.collection("couples").doc(COUPLE_ID).collection("love_doodles").doc("dood_1").update({
      lovedByPartner: true,
      updatedAt: pastDate(500),
    })
  );
  await assertFails(
    charlieDb.collection("couples").doc(COUPLE_ID).collection("love_doodles").doc("dood_1").get()
  );

  // 6. Couple Promise Vault (create + partner co-sign)
  const promiseRef = aliceDb.collection("couples").doc(COUPLE_ID).collection("couple_promises").doc("prom_1");
  await assertSucceeds(
    promiseRef.set({
      coupleId: COUPLE_ID,
      creatorUid: ALICE_UID,
      creatorName: "Maya",
      memberUids: [ALICE_UID, BOB_UID],
      title: "Never go to sleep angry at each other",
      category: "TRUST",
      signedByUids: [ALICE_UID],
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    bobDb.collection("couples").doc(COUPLE_ID).collection("couple_promises").doc("prom_1").update({
      signedByUids: [ALICE_UID, BOB_UID],
      updatedAt: pastDate(500),
    })
  );
  await assertFails(
    charlieDb.collection("couples").doc(COUPLE_ID).collection("couple_promises").doc("prom_1").delete()
  );
  await assertSucceeds(
    bobDb.collection("couples").doc(COUPLE_ID).collection("couple_promises").doc("prom_1").delete()
  );
});

test("Remove as Partner: full couple data wipe, 6-digit code invalidation, dissolution, and outsider denial", async () => {
  await seedPairedCouple();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const now = pastDate(3000);

  // Seed claimed pair code + Alice-authored docs across subcollections (including presence, moods, moments, messages, daily_prompts)
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("pair_codes").doc(INVITE_CODE).set({
      code: INVITE_CODE,
      coupleId: COUPLE_ID,
      creatorUid: ALICE_UID,
      creatorName: "Maya",
      status: "claimed",
      claimedByUid: BOB_UID,
      createdAt: now,
      updatedAt: now,
    });
    const coupleRef = db.collection("couples").doc(COUPLE_ID);
    await coupleRef.collection("presence").doc(ALICE_UID).set({
      userId: ALICE_UID,
      coupleId: COUPLE_ID,
      memberUids: [ALICE_UID, BOB_UID],
      displayName: "Maya",
      batteryPercent: 78,
      charging: false,
      networkType: "WIFI",
      deviceStatus: "ACTIVE_IN_DUET",
      moodKey: "IN_LOVE",
      moodNote: "",
      statusShared: true,
      createdAt: now,
      updatedAt: now,
    });
    await coupleRef.collection("messages").doc("m_erase").set({
      coupleId: COUPLE_ID,
      senderUid: ALICE_UID,
      senderName: "Maya",
      memberUids: [ALICE_UID, BOB_UID],
      messageType: "TEXT",
      keyMode: "ECDH_P256",
      ciphertext: "SECRET_CHAT_BLOB",
      iv: "IV_1",
      createdAt: now,
      updatedAt: now,
    });
    await coupleRef.collection("moments").doc("mom_erase").set({
      coupleId: COUPLE_ID,
      uploaderUid: ALICE_UID,
      uploaderName: "Maya",
      memberUids: [ALICE_UID, BOB_UID],
      keyMode: "ECDH_P256",
      ciphertext: "SECRET_PHOTO_BLOB",
      iv: "IV_2",
      lovedByUids: [ALICE_UID],
      createdAt: now,
      updatedAt: now,
    });
    await coupleRef.collection("moods").doc("mood_erase").set({
      coupleId: COUPLE_ID,
      userId: ALICE_UID,
      authorName: "Maya",
      memberUids: [ALICE_UID, BOB_UID],
      moodKey: "HAPPY",
      note: "Sunny morning",
      energyLevel: 5,
      createdAt: now,
      updatedAt: now,
    });
    await coupleRef.collection("daily_prompts").doc("2026-11-20").set({
      coupleId: COUPLE_ID,
      dateKey: "2026-11-20",
      promptIndex: 4,
      memberUids: [ALICE_UID, BOB_UID],
      creatorAnswer: "Coffee walks together",
      creatorAnsweredUid: ALICE_UID,
      createdAt: now,
      updatedAt: now,
    });
  });

  // Outsider Charlie CANNOT delete any subcollection doc, pair code, or dissolve couple
  await assertFails(charlieDb.collection("pair_codes").doc(INVITE_CODE).delete());
  await assertFails(charlieDb.collection("couples").doc(COUPLE_ID).collection("moments").doc("mom_erase").delete());
  await assertFails(charlieDb.collection("couples").doc(COUPLE_ID).collection("daily_prompts").doc("2026-11-20").delete());
  await assertFails(
    charlieDb.collection("couples").doc(COUPLE_ID).update({
      status: "dissolved",
      inviteCode: "918273",
      dissolvedByUid: CHARLIE_UID,
      dissolvedByName: "Charlie",
      dissolvedAt: pastDate(1000),
      lovePulseCount: 0,
      updatedAt: pastDate(1000),
    })
  );

  // Partner Bob (non-creator) wipes all shared moments, sent chats, moods, presence, daily prompts & invalidates 6-digit code
  const bobCouple = bobDb.collection("couples").doc(COUPLE_ID);
  await assertSucceeds(bobCouple.collection("presence").doc(ALICE_UID).delete());
  await assertSucceeds(bobCouple.collection("messages").doc("m_erase").delete());
  await assertSucceeds(bobCouple.collection("moments").doc("mom_erase").delete());
  await assertSucceeds(bobCouple.collection("moods").doc("mood_erase").delete());
  await assertSucceeds(bobCouple.collection("daily_prompts").doc("2026-11-20").delete());
  await assertSucceeds(bobDb.collection("pair_codes").doc(INVITE_CODE).delete());

  // Dissolution without changing 6-digit inviteCode MUST fail
  await assertFails(
    bobCouple.update({
      status: "dissolved",
      inviteCode: INVITE_CODE,
      dissolvedByUid: BOB_UID,
      dissolvedByName: "Liam",
      dissolvedAt: pastDate(1000),
      lovePulseCount: 0,
      updatedAt: pastDate(1000),
    })
  );

  // Partner Bob validly dissolves couple space (rotates 6-digit code & clears dates/pulse count)
  const dissolveTime = pastDate(800);
  await assertSucceeds(
    bobCouple.update({
      status: "dissolved",
      inviteCode: "918273",
      dissolvedByUid: BOB_UID,
      dissolvedByName: "Liam",
      dissolvedAt: dissolveTime,
      anniversaryYear: null,
      anniversaryMonth: null,
      anniversaryDay: null,
      creatorBirthdayMonth: null,
      creatorBirthdayDay: null,
      partnerBirthdayMonth: null,
      partnerBirthdayDay: null,
      lovePulseCount: 0,
      lastPulseByUid: null,
      lastPulseAt: null,
      updatedAt: dissolveTime,
    })
  );

  // Presence writes are blocked once couple space is dissolved
  await assertFails(
    aliceDb.collection("couples").doc(COUPLE_ID).collection("presence").doc(ALICE_UID).set({
      userId: ALICE_UID,
      coupleId: COUPLE_ID,
      memberUids: [ALICE_UID, BOB_UID],
      displayName: "Maya",
      batteryPercent: 75,
      charging: false,
      networkType: "WIFI",
      deviceStatus: "ACTIVE_IN_DUET",
      moodKey: "IN_LOVE",
      moodNote: "",
      statusShared: true,
      createdAt: pastDate(400),
      updatedAt: pastDate(400),
    })
  );

  // Alice can observe dissolved status & delete the dissolved tombstone; Charlie cannot
  await assertSucceeds(aliceDb.collection("couples").doc(COUPLE_ID).get());
  await assertFails(charlieDb.collection("couples").doc(COUPLE_ID).delete());
  await assertSucceeds(aliceDb.collection("couples").doc(COUPLE_ID).delete());
});

test("User ban immutability and live app_config/live OTA read-only governance", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = pastDate(5000);

  // Seed app_config/live via Admin SDK (rules bypassed)
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("app_config").doc("live").set({
      maintenanceMode: false,
      maintenanceMessage: "Scheduled maintenance at midnight IST.",
      showBanner: true,
      bannerText: "Duet 2.0 is rolling out now!",
      bannerLink: "https://example.com/duet-release",
      forceUpdate: true,
      minVersion: "1618",
      updateLink: "https://play.google.com/store/apps/details?id=com.aistudio.duet.kvrxqm",
      updatedAt: created,
    });
  });

  // Unauthenticated and signed-in clients can read/list OTA app_config directly on startup
  await assertSucceeds(unauthDb.collection("app_config").doc("live").get());
  await assertSucceeds(aliceDb.collection("app_config").doc("live").get());
  await assertSucceeds(unauthDb.collection("app_config").get());
  await assertSucceeds(aliceDb.collection("app_config").get());
  await assertSucceeds(aliceDb.collection("app_config").doc("feature_flags").get());

  // Clients cannot tamper with app_config/live
  await assertFails(
    aliceDb.collection("app_config").doc("live").update({ maintenanceMode: true })
  );
  await assertFails(
    aliceDb.collection("app_config").doc("live").delete()
  );

  // Client cannot create a profile with pre-populated ban fields
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Maya",
      avatarInitials: "MY",
      publicKey: "ALICE_PUB_KEY_BASE64",
      isBanned: true,
      status: "banned",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Alice creates valid normal profile
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Maya",
      avatarInitials: "MY",
      publicKey: "ALICE_PUB_KEY_BASE64",
      shareDeviceStatus: true,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Alice cannot modify admin-only ban fields via update
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isBanned: false,
      status: "active",
      updatedAt: pastDate(1000),
    })
  );

  // Admin bans Alice via Admin SDK
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(ALICE_UID).update({
      status: "banned",
      isBanned: true,
      banReason: "Repeated policy violation",
      bannedAt: pastDate(500),
    });
  });

  // Alice can still read her own user document to observe the real-time ban & reason
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());

  // Alice CANNOT update or delete her profile once banned (cannot evade ban)
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      displayName: "New Maya",
      updatedAt: pastDate(200),
    })
  );
  await assertFails(aliceDb.collection("users").doc(ALICE_UID).delete());
});

test("Bondly Admin (com.aistudio.bondlyadmin.vzrqnp) operator permissions for app_config and user bans", async () => {
  const adminContext = testEnv.authenticatedContext("admin_uid_999", {
    email: "sahiloriginal16@gmail.com",
    email_verified: true,
  });
  const unverifiedAdminContext = testEnv.authenticatedContext("fake_admin_uid", {
    email: "sahiloriginal16@gmail.com",
    email_verified: false,
  });
  const regularUserContext = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  });

  const adminDb = adminContext.firestore();
  const unverifiedAdminDb = unverifiedAdminContext.firestore();
  const aliceDb = regularUserContext.firestore();
  const created = pastDate(5000);

  // Regular user creates her profile
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Maya",
      avatarInitials: "MY",
      publicKey: "ALICE_PUB_KEY_BASE64",
      shareDeviceStatus: true,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Verified Bondly Admin can publish OTA config across live, maintenance, updates, and banners
  await assertSucceeds(
    adminDb.collection("app_config").doc("live").set({
      maintenanceMode: false,
      showUpdatePopup: true,
      showUpdateBanner: true,
      latestVersion: "1620",
      updateTitle: "Bondly Spring Release",
      updateMessage: "Faster encrypted sync and new banners!",
      updateLink: "https://play.google.com/store/apps/details?id=com.aistudio.duet.kvrxqm",
      adminPackageId: "com.aistudio.bondlyadmin.vzrqnp",
      updatedAt: created,
    })
  );
  await assertSucceeds(
    adminDb.collection("app_config").doc("maintenance").set({
      maintenanceMode: true,
      maintenanceTitle: "Database Upgrade",
      maintenanceMessage: "Back in 15 minutes",
      maintenanceEta: "11:30 PM IST",
      updatedAt: created,
    })
  );
  await assertSucceeds(
    adminDb.collection("app_config").doc("updates").set({
      showUpdatePopup: true,
      latestVersion: "1621",
      updatedAt: created,
    })
  );
  await assertSucceeds(
    adminDb.collection("app_config").doc("banners").set({
      showBanner: true,
      bannerTitle: "Bondly Notice",
      bannerText: "Update ready!",
      bannerType: "update",
      updatedAt: created,
    })
  );
  await assertSucceeds(
    adminDb.collection("feature_flags").doc("shared_journal").set({
      key: "shared_journal",
      enabled: false,
      updatedAt: created,
    })
  );
  await assertSucceeds(
    adminDb.collection("promo_banners").doc("spring").set({
      active: true,
      title: "Spring Event",
      text: "Unlock spring themes!",
      badge: "PROMO",
      updatedAt: created,
    })
  );
  await assertSucceeds(
    adminDb.collection("version_control").doc("android").set({
      minVersion: "2.0.0",
      latestVersion: "2.1.0",
      forceUpdate: true,
      blockedVersions: ["1617"],
      updatedAt: created,
    })
  );
  await assertSucceeds(
    adminDb.collection("dynamic_config").doc("welcome_message").set({
      key: "welcome_message",
      value: "Welcome back lovebirds",
      updatedAt: created,
    })
  );

  // Clients can read OTA collections, but regular users & unverified email impersonators CANNOT write
  await assertSucceeds(aliceDb.collection("feature_flags").get());
  await assertSucceeds(aliceDb.collection("promo_banners").get());
  await assertSucceeds(aliceDb.collection("version_control").get());
  await assertSucceeds(aliceDb.collection("dynamic_config").get());

  await assertFails(
    unverifiedAdminDb.collection("app_config").doc("live").set({
      maintenanceMode: true,
      updatedAt: created,
    })
  );
  await assertFails(
    aliceDb.collection("app_config").doc("updates").set({
      showUpdatePopup: true,
      latestVersion: "9999",
    })
  );
  await assertFails(
    aliceDb.collection("feature_flags").doc("shared_journal").set({ enabled: true })
  );
  await assertFails(
    aliceDb.collection("promo_banners").doc("spring").set({ active: true })
  );
  await assertFails(
    aliceDb.collection("version_control").doc("android").set({ minVersion: "1.0.0" })
  );
  await assertFails(
    aliceDb.collection("dynamic_config").doc("welcome_message").set({ value: "hacked" })
  );
  await assertFails(unverifiedAdminDb.collection("users").get());
  await assertFails(aliceDb.collection("users").get());

  // Verified Bondly Admin can read/list users and ban/unban strictly via ban fields
  await assertSucceeds(adminDb.collection("users").get());
  await assertSucceeds(
    adminDb.collection("users").doc(ALICE_UID).update({
      status: "banned",
      isBanned: true,
      banReason: "Suspended via Bondly Admin",
      bannedAt: pastDate(1000),
      updatedAt: pastDate(1000),
    })
  );

  // Admin cannot overwrite user's personal E2EE publicKey or displayName
  await assertFails(
    adminDb.collection("users").doc(ALICE_UID).update({
      publicKey: "TAMPERED_KEY",
      updatedAt: pastDate(500),
    })
  );

  // Clients cannot tamper with /admins registry
  await assertFails(
    aliceDb.collection("admins").doc(ALICE_UID).set({ role: "owner" })
  );

  // Dedicated maintenance & updates collections are public-read, admin-write only
  await assertSucceeds(
    adminDb.collection("maintenance").doc("live").set({
      enabled: true,
      message: "Quick backend upgrade",
      updatedAt: created,
    })
  );
  await assertSucceeds(
    adminDb.collection("updates").doc("android").set({
      updateAvailable: true,
      latestVersion: "1625",
      downloadLink: "https://bondly.example.com/duet.apk",
      updatedAt: created,
    })
  );
  await assertSucceeds(aliceDb.collection("maintenance").get());
  await assertSucceeds(aliceDb.collection("updates").get());
  await assertFails(aliceDb.collection("maintenance").doc("live").set({ enabled: false }));
  await assertFails(aliceDb.collection("updates").doc("android").set({ updateAvailable: false }));
});

test("Legacy or partial user profile can self-heal missing userId/createdAt/keys during 6-digit code creation without bypassing ban protection", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = pastDate(5000);

  // Seed legacy/partial user profile missing userId, avatarInitials, publicKey, and createdAt
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(ALICE_UID).set({
      displayName: "Maya",
      email: "maya@example.com",
    });
  });

  // Owner heals missing schema fields cleanly
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      userId: ALICE_UID,
      displayName: "Maya",
      avatarInitials: "MA",
      publicKey: "ALICE_PUB_KEY_BASE64",
      shareDeviceStatus: true,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Non-owner still cannot touch Alice's profile
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      displayName: "Hacked",
      updatedAt: created,
    })
  );

  // Atomic 6-digit code creation batch succeeds immediately after profile healing
  const batch = aliceDb.batch();
  batch.set(aliceDb.collection("couples").doc(COUPLE_ID), {
    creatorUid: ALICE_UID,
    memberUids: [ALICE_UID],
    inviteCode: INVITE_CODE,
    status: "waiting",
    creatorName: "Maya",
    creatorPublicKey: "ALICE_PUB_KEY_BASE64",
    wrappedVaultKey: "WRAPPED_VAULT_KEY_BASE64",
    lovePulseCount: 0,
    createdAt: created,
    updatedAt: created,
  });
  batch.set(aliceDb.collection("pair_codes").doc(INVITE_CODE), {
    code: INVITE_CODE,
    coupleId: COUPLE_ID,
    creatorUid: ALICE_UID,
    creatorName: "Maya",
    status: "open",
    createdAt: created,
    updatedAt: created,
  });
  batch.update(aliceDb.collection("users").doc(ALICE_UID), {
    displayName: "Maya",
    avatarInitials: "MA",
    coupleId: COUPLE_ID,
    updatedAt: created,
  });
  await assertSucceeds(batch.commit());
});

test("Paired partner can read partner ban status/reason while outsider cannot, and non-banned partner wipe leaves banned partner intact", async () => {
  await seedPairedCouple();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const adminDb = testEnv.authenticatedContext("admin_uid_999", {
    email: "sahiloriginal16@gmail.com",
    email_verified: true,
  }).firestore();
  const created = pastDate(8000);

  // Seed user docs for Alice and Bob
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Maya",
      avatarInitials: "MY",
      publicKey: "ALICE_PUB_KEY_BASE64",
      coupleId: COUPLE_ID,
      shareDeviceStatus: true,
      createdAt: created,
      updatedAt: created,
    });
    await db.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      displayName: "Liam",
      avatarInitials: "LI",
      publicKey: "BOB_PUB_KEY_BASE64",
      coupleId: COUPLE_ID,
      shareDeviceStatus: true,
      createdAt: created,
      updatedAt: created,
    });
    await db.collection("couples").doc(COUPLE_ID).collection("messages").doc("msg1").set({
      coupleId: COUPLE_ID,
      senderUid: ALICE_UID,
      senderName: "Maya",
      memberUids: [ALICE_UID, BOB_UID],
      messageType: "TEXT",
      keyMode: "ECDH_P256",
      ciphertext: "ENCRYPTED",
      iv: "IV123",
      createdAt: created,
      updatedAt: created,
    });
  });

  // Admin bans Alice
  await assertSucceeds(
    adminDb.collection("users").doc(ALICE_UID).update({
      status: "banned",
      isBanned: true,
      banReason: "Abusive behavior reported",
      bannedAt: pastDate(1000),
      updatedAt: pastDate(1000),
    })
  );

  // Paired partner Bob CAN read Alice's user doc to observe ban status & reason
  const aliceSnapForBob = await assertSucceeds(bobDb.collection("users").doc(ALICE_UID).get());
  if (aliceSnapForBob.data().banReason !== "Abusive behavior reported") {
    throw new Error("Expected Bob to see Alice's banReason");
  }

  // Outsider Charlie CANNOT read Alice's user doc, and listing /users remains denied
  await assertFails(charlieDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").get());

  // Non-banned Bob wipes shared subcollections, rotates & invalidates pair code, dissolves couple, and resets only HIS profile
  const dissolveTime = pastDate(500);
  await assertSucceeds(
    bobDb.collection("couples").doc(COUPLE_ID).collection("messages").doc("msg1").delete()
  );
  await assertSucceeds(
    bobDb.collection("couples").doc(COUPLE_ID).update({
      status: "dissolved",
      inviteCode: "918273",
      dissolvedByUid: BOB_UID,
      dissolvedByName: "Liam",
      dissolvedAt: dissolveTime,
      anniversaryYear: null,
      anniversaryMonth: null,
      anniversaryDay: null,
      creatorBirthdayMonth: null,
      creatorBirthdayDay: null,
      partnerBirthdayMonth: null,
      partnerBirthdayDay: null,
      lovePulseCount: 0,
      lastPulseByUid: null,
      lastPulseAt: null,
      updatedAt: dissolveTime,
    })
  );
  await assertSucceeds(
    bobDb.collection("users").doc(BOB_UID).update({
      coupleId: null,
      partnerNickname: null,
      updatedAt: dissolveTime,
    })
  );

  // Alice's user doc STILL keeps her ban status & banReason untouched (zero impact on banned partner)
  const aliceSnapAfterBobWipe = await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
  if (!aliceSnapAfterBobWipe.data().isBanned || aliceSnapAfterBobWipe.data().banReason !== "Abusive behavior reported") {
    throw new Error("Banned partner's suspension must remain intact after partner wipes data");
  }

  // Admin unbans Alice cleanly
  await assertSucceeds(
    adminDb.collection("users").doc(ALICE_UID).update({
      status: "active",
      isBanned: false,
      banReason: null,
      bannedAt: null,
      updatedAt: pastDate(200),
    })
  );

  // Once unbanned, even if the old couple doc was completely deleted, Alice can observe resource == null cleanly without PERMISSION_DENIED
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("couples").doc(COUPLE_ID).delete();
  });
  const missingCoupleSnap = await assertSucceeds(aliceDb.collection("couples").doc(COUPLE_ID).get());
  if (missingCoupleSnap.exists) {
    throw new Error("Expected deleted couple doc to report exists === false");
  }
});
