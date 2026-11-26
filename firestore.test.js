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

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
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
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("projects").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("characters").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("generationHistory").get());
});

test("Owner can create and update UserProfile, while credit inflation and cross-user reads fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const past = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Director",
      email: "alice@example.com",
      creditsRemaining: 50,
      totalVideosGenerated: 0,
      totalScenesRendered: 0,
      preferredStyle: "3D_CARTOON",
      preferredLanguage: "ENGLISH",
      createdAt: past,
      updatedAt: past,
    })
  );

  // Valid credit deduction
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      creditsRemaining: 45,
      totalVideosGenerated: 1,
      totalScenesRendered: 3,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject arbitrary credit inflation when credits >= 10
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      creditsRemaining: 999,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject cross-user read
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
});

test("Owner can create project, scenes, characters, and immutable history; cross-user and ghost fields fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const past = new Date(Date.now() - 5000);

  // Create project (NO @DocumentId id field in payload)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("projects").doc("proj_1").set({
      userId: ALICE_UID,
      title: "Robo-Fox Adventure",
      prompt: "A clever robot fox explores a glowing crystal forest.",
      style: "3D_CARTOON",
      durationOption: "SEC_30",
      language: "ENGLISH",
      aspectRatio: "RATIO_16_9",
      resolution: "RES_1080P",
      status: "GENERATING",
      progressPercent: 10,
      sceneCount: 3,
      completedScenes: 0,
      durationSeconds: 30,
      createdAt: past,
      updatedAt: past,
    })
  );

  // Create scene under existing project
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("projects").doc("proj_1")
      .collection("scenes").doc("scene_0").set({
        userId: ALICE_UID,
        projectId: "proj_1",
        sceneIndex: 0,
        title: "Entering the Crystal Woods",
        visualPrompt: "Cute 3D cartoon orange robot fox wearing blue scarf in glowing purple forest",
        cameraMotion: "ZOOM_IN",
        moodMusic: "ADVENTURE",
        sfxCue: "SPARKLE",
        characterNames: ["Bolt"],
        dialogues: [{ speaker: "Bolt", gender: "MALE", expression: "EXCITED", line: "Whoa, look at those crystals!", durationSec: 4 }],
        status: "COMPLETED",
        retryCount: 0,
        createdAt: past,
        updatedAt: new Date(Date.now() - 1000),
      })
  );

  // Reject orphaned scene under missing project
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("projects").doc("missing_proj")
      .collection("scenes").doc("scene_0").set({
        userId: ALICE_UID,
        projectId: "missing_proj",
        sceneIndex: 0,
        title: "Orphan",
        visualPrompt: "Test",
        cameraMotion: "STATIC",
        moodMusic: "PLAYFUL",
        sfxCue: "NONE",
        characterNames: [],
        dialogues: [],
        status: "PENDING",
        retryCount: 0,
        createdAt: past,
        updatedAt: past,
      })
  );

  // Create saved character
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("characters").doc("char_bolt").set({
      userId: ALICE_UID,
      name: "Bolt",
      role: "Hero Explorer",
      appearance: "Small bright orange cartoon robot fox with glowing cyan eyes",
      outfit: "Cobalt blue explorer scarf and silver backpack",
      voiceGender: "MALE",
      voicePitch: 1.15,
      defaultExpression: "EXCITED",
      style: "3D_CARTOON",
      createdAt: past,
      updatedAt: past,
    })
  );

  // Reject ghost field on character create
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("characters").doc("char_ghost").set({
      id: "char_ghost",
      userId: ALICE_UID,
      name: "Ghost",
      role: "Hero",
      appearance: "Blue robot",
      outfit: "Red cape",
      voiceGender: "MALE",
      voicePitch: 1.0,
      defaultExpression: "HAPPY",
      style: "2D_CARTOON",
      createdAt: past,
      updatedAt: past,
    })
  );

  // Create immutable history entry & verify update fails
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("generationHistory").doc("hist_1").set({
      userId: ALICE_UID,
      projectId: "proj_1",
      projectTitle: "Robo-Fox Adventure",
      actionType: "FULL_MOVIE",
      creditsUsed: 5,
      status: "COMPLETED",
      durationLabel: "30s",
      language: "ENGLISH",
      style: "3D_CARTOON",
      details: "3 scenes rendered",
      createdAt: past,
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("generationHistory").doc("hist_1").update({
      creditsUsed: 0,
    })
  );

  // List queries succeed for owner, fail for Bob
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("projects").get());
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("projects").doc("proj_1").collection("scenes").get());
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("characters").get());
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("generationHistory").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("projects").get());
});
