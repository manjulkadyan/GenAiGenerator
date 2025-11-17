/**
 * Verify what data is in Firestore
 *
 * Usage:
 * npm run verify:firestore
 */

import * as admin from "firebase-admin";
import * as fs from "fs";

// Initialize Firebase Admin
if (!admin.apps.length) {
  const useEmulator = process.env.FIRESTORE_EMULATOR_HOST;

  if (useEmulator) {
    const emulatorHost = process.env.FIRESTORE_EMULATOR_HOST || "localhost:8080";
    const [host, port] = emulatorHost.split(":");
    admin.initializeApp();
    admin.firestore().settings({
      host: host,
      port: parseInt(port, 10),
      ssl: false,
    });
    console.log(`✅ Connected to Firebase Emulator at ${emulatorHost}\n`);
  } else {
    const serviceAccountPath = process.env.GOOGLE_APPLICATION_CREDENTIALS;
    if (serviceAccountPath && fs.existsSync(serviceAccountPath)) {
      const serviceAccount = JSON.parse(fs.readFileSync(serviceAccountPath, "utf-8"));
      admin.initializeApp({
        credential: admin.credential.cert(serviceAccount),
      });
      console.log("✅ Using service account credentials\n");
    } else {
      admin.initializeApp();
      console.log("✅ Using default credentials\n");
    }
  }
}

const firestore = admin.firestore();

async function verifyFirestoreData() {
  console.log("=".repeat(80));
  console.log("VERIFYING FIRESTORE DATA");
  console.log("=".repeat(80));
  console.log();

  try {
    // Check video_features collection
    console.log("📦 Checking video_features collection...");
    const modelsSnapshot = await firestore.collection("video_features").get();

    if (modelsSnapshot.empty) {
      console.log("❌ No models found in video_features collection");
      console.log("   Collection is empty or doesn't exist\n");
    } else {
      console.log(`✅ Found ${modelsSnapshot.size} models in video_features collection\n`);

      console.log("Models in Firestore:");
      console.log("-".repeat(80));

      modelsSnapshot.docs.forEach((doc, index) => {
        const data = doc.data();
        console.log(`\n[${index + 1}] ${data.name || doc.id}`);
        console.log(`   ID: ${doc.id}`);
        console.log(`   Replicate Name: ${data.replicate_name || "N/A"}`);
        console.log(`   Price: ${data.price_per_sec || 0} credits/sec`);
        console.log(`   Durations: ${JSON.stringify(data.duration_options || [])}`);
        console.log(`   Aspect Ratios: ${JSON.stringify(data.aspect_ratios || [])}`);
        console.log(`   First Frame: ${data.requires_first_frame ? "✅" : "❌"}`);
        console.log(`   Last Frame: ${data.requires_last_frame ? "✅" : "❌"}`);
        console.log(`   Audio: ${data.supports_audio ? "✅" : "❌"}`);
        console.log(`   Reference Images: ${data.supports_reference_images ? "✅" : "❌"}`);
        if (data.original_price_per_sec) {
          console.log(`   Original Price: ${data.original_price_per_sec} credits/sec (${data.price_multiplier || 1}x)`);
        }
      });

      console.log("\n" + "=".repeat(80));
      console.log(`Total: ${modelsSnapshot.size} models`);
      console.log("=".repeat(80));
    }

    // Check if there are any users/jobs
    console.log("\n📦 Checking users collection...");
    const usersSnapshot = await firestore.collection("users").limit(5).get();
    console.log(`   Found ${usersSnapshot.size} user(s) (showing first 5)`);
  } catch (error) {
    console.error("❌ Error verifying Firestore:", error);
    process.exit(1);
  }
}

// Run if called directly
if (require.main === module) {
  verifyFirestoreData()
    .then(() => {
      console.log("\n✅ Verification complete!");
      process.exit(0);
    })
    .catch((error) => {
      console.error("💥 Error:", error);
      process.exit(1);
    });
}

export {verifyFirestoreData};

