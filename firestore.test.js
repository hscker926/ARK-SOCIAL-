const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

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

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("posts").where("visibility", "==", "public").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("posts").doc("post_1").set({
      authorId: ALICE_UID,
      authorUsername: "alice",
      authorDisplayName: "Alice",
      caption: "Hi",
      mediaType: "photo",
      mediaKey: "preset_arch",
      visibility: "public",
      likesCount: 0,
      commentsCount: 0,
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("User profile create, partial update, PII isolation, and ghost-field rejection", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      username: "alice_studio",
      displayName: "Alice Studio",
      bio: "Visual storyteller",
      avatarKey: "preset_arch",
      discoverable: true,
      postsCount: 0,
      followersCount: 0,
      followingCount: 0,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Private PII doc succeeds for owner and fails for Bob
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("private").doc("account").set({
      userId: ALICE_UID,
      email: "alice@example.com",
      privateAccount: false,
      pushLikes: true,
      pushComments: true,
      pushFollows: true,
      showActivityStatus: true,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("private").doc("account").get()
  );

  // Ghost field rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Bob cannot modify Alice's profile
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      bio: "Hacked",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Filtered discoverable users query succeeds; unfiltered list fails
  await assertSucceeds(aliceDb.collection("users").where("discoverable", "==", true).get());
  await assertFails(aliceDb.collection("users").get());
});

test("Posts create, filtered list query, engagement counter, comment, and save", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb.collection("posts").doc("post_1").set({
      authorId: ALICE_UID,
      authorUsername: "alice_studio",
      authorDisplayName: "Alice Studio",
      authorAvatarKey: "preset_arch",
      caption: "Golden hour courtyard",
      mediaType: "photo",
      mediaKey: "preset_arch",
      location: "Lisbon",
      visibility: "public",
      likesCount: 0,
      commentsCount: 0,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Reject @DocumentId "id" injection or future timestamp
  await assertFails(
    aliceDb.collection("posts").doc("post_bad").set({
      id: "post_bad",
      authorId: ALICE_UID,
      authorUsername: "alice_studio",
      authorDisplayName: "Alice Studio",
      caption: "Bad",
      mediaType: "photo",
      mediaKey: "preset_arch",
      visibility: "public",
      likesCount: 0,
      commentsCount: 0,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(
    aliceDb.collection("posts").doc("post_future").set({
      authorId: ALICE_UID,
      authorUsername: "alice_studio",
      authorDisplayName: "Alice Studio",
      caption: "Future",
      mediaType: "photo",
      mediaKey: "preset_arch",
      visibility: "public",
      likesCount: 0,
      commentsCount: 0,
      createdAt: new Date(Date.now() + 60000),
      updatedAt: new Date(Date.now() + 60000),
    })
  );

  // Filtered queries succeed; unfiltered fails
  await assertSucceeds(bobDb.collection("posts").where("visibility", "==", "public").get());
  await assertSucceeds(aliceDb.collection("posts").where("authorId", "==", ALICE_UID).get());
  await assertFails(bobDb.collection("posts").get());

  // Bob can like (+1) and add a comment on Alice's public post, but cannot edit caption
  await assertSucceeds(
    bobDb.collection("posts").doc("post_1").update({
      likesCount: 1,
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    bobDb.collection("posts").doc("post_1").update({
      caption: "Tampered",
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertSucceeds(
    bobDb.collection("posts").doc("post_1").collection("comments").doc("c_1").set({
      postId: "post_1",
      postVisibility: "public",
      authorId: BOB_UID,
      authorUsername: "bob_frames",
      text: "Clean framing!",
      createdAt: new Date(Date.now() - 500),
    })
  );
  await assertSucceeds(
    bobDb
      .collection("posts")
      .doc("post_1")
      .collection("comments")
      .where("postVisibility", "==", "public")
      .get()
  );

  // Saved posts isolated to owner
  await assertSucceeds(
    bobDb.collection("users").doc(BOB_UID).collection("savedPosts").doc("post_1").set({
      userId: BOB_UID,
      postId: "post_1",
      authorUsername: "alice_studio",
      caption: "Golden hour courtyard",
      mediaKey: "preset_arch",
      mediaType: "photo",
      createdAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("savedPosts").get()
  );
});

test("Direct messages and stories enforce participant and visibility rules", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 1000);

  await assertSucceeds(
    aliceDb.collection("stories").doc("story_1").set({
      authorId: ALICE_UID,
      authorUsername: "alice_studio",
      mediaKey: "preset_coast",
      caption: "Morning surf",
      visibility: "public",
      createdAt: now,
    })
  );
  await assertSucceeds(bobDb.collection("stories").where("visibility", "==", "public").get());
  await assertFails(bobDb.collection("stories").get());

  const convId = `${ALICE_UID}_${BOB_UID}`;
  await assertSucceeds(
    aliceDb.collection("conversations").doc(convId).set({
      participantIds: [ALICE_UID, BOB_UID],
      participantLabel: "Alice & Bob",
      lastMessage: "Hey!",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    bobDb.collection("conversations").where("participantIds", "array-contains", BOB_UID).get()
  );
  await assertFails(bobDb.collection("conversations").get());
});
