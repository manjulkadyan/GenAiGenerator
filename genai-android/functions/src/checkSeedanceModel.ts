/**
 * Check seedance-1-lite model in Firestore
 */

import * as admin from "firebase-admin";
import * as fs from "fs";
import * as path from "path";
import {initializeApp, cert, App} from "firebase-admin/app";
import {getFirestore} from "firebase-admin/firestore";

// Initialize Firebase
let app: App;
let firestore: admin.firestore.Firestore;

const serviceAccountPath = path.join(
  __dirname,
  "..",
  "service-account-key.json",
);
if (!fs.existsSync(serviceAccountPath)) {
  console.error(`❌ Service account key not found: ${serviceAccountPath}`);
  process.exit(1);
}

const serviceAccount = JSON.parse(fs.readFileSync(serviceAccountPath, "utf-8"));
try {
  app = initializeApp({
    credential: cert(serviceAccount),
  });
  firestore = getFirestore(app);
} catch (error) {
  // App might already be initialized
  app = admin.app();
  firestore = admin.firestore();
}

async function checkSeedanceModel() {
  console.log("🔍 Checking seedance-1-lite model in Firestore...\n");

  const collection = firestore.collection("video_features");
  const doc = await collection.doc("seedance-1-lite").get();

  if (!doc.exists) {
    console.error("❌ Document does not exist!");
    process.exit(1);
  }

  const data = doc.data();
  console.log("📊 Current Firestore Document:");
  console.log(JSON.stringify(data, null, 2));
  console.log("\n");

  // Check specific fields
  console.log("📋 Field Verification:");
  console.log(`   ID: ${data?.id}`);
  console.log(`   Name: ${data?.name}`);
  console.log(`   Aspect Ratios: ${JSON.stringify(data?.aspect_ratios)}`);
  console.log(`   Durations: ${JSON.stringify(data?.duration_options)}`);
  console.log(`   Resolutions: ${JSON.stringify(data?.resolutions)}`);
  console.log(`   Supports Reference Images: ${data?.supports_reference_images}`);
  console.log(`   Max Reference Images: ${data?.max_reference_images}`);
  console.log(`   Price per sec: ${data?.price_per_sec}`);

  process.exit(0);
}

checkSeedanceModel().catch((error) => {
  console.error("❌ Error:", error);
  process.exit(1);
});

