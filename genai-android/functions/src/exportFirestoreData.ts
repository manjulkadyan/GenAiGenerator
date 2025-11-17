/**
 * Export all video_features data from Firestore to a JSON file
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

/**
 * Convert Firestore Timestamp to ISO string
 */
function convertTimestamps(obj: unknown): unknown {
  if (obj === null || obj === undefined) {
    return obj;
  }

  if (obj instanceof admin.firestore.Timestamp) {
    return obj.toDate().toISOString();
  }

  if (Array.isArray(obj)) {
    return obj.map(convertTimestamps);
  }

  if (typeof obj === "object") {
    const converted: Record<string, unknown> = {};
    for (const [key, value] of Object.entries(obj)) {
      converted[key] = convertTimestamps(value);
    }
    return converted;
  }

  return obj;
}

async function exportFirestoreData() {
  console.log("📥 Exporting video_features data from Firestore...\n");

  const collection = firestore.collection("video_features");
  const snapshot = await collection.get();

  if (snapshot.empty) {
    console.error("❌ No documents found in video_features collection");
    process.exit(1);
  }

  console.log(`📊 Found ${snapshot.size} documents\n`);

  const documents: Array<{id: string; data: Record<string, unknown>}> = [];

  snapshot.docs.forEach((doc) => {
    const data = doc.data();
    // Convert Firestore Timestamps to ISO strings
    const convertedData = convertTimestamps(data) as Record<string, unknown>;
    documents.push({
      id: doc.id,
      data: convertedData,
    });
    console.log(`   ✓ ${doc.id} - ${(convertedData.name as string) || "N/A"}`);
  });

  // Create output file
  const outputPath = path.join(
    __dirname,
    "..",
    "firestore_video_features_export.json",
  );

  const exportData = {
    exported_at: new Date().toISOString(),
    collection: "video_features",
    total_documents: documents.length,
    documents: documents,
  };

  fs.writeFileSync(outputPath, JSON.stringify(exportData, null, 2));

  console.log(`\n✅ Exported ${documents.length} documents to:`);
  console.log(`   ${outputPath}\n`);

  // Also create a summary file
  const summaryPath = path.join(
    __dirname,
    "..",
    "firestore_video_features_summary.json",
  );

  const summary = documents.map((doc) => ({
    id: doc.id,
    name: doc.data.name,
    replicate_name: doc.data.replicate_name,
    aspect_ratios: doc.data.aspect_ratios,
    duration_options: doc.data.duration_options,
    resolutions: doc.data.resolutions,
    price_per_sec: doc.data.price_per_sec,
    supports_first_frame: doc.data.supports_first_frame,
    supports_last_frame: doc.data.supports_last_frame,
    supports_reference_images: doc.data.supports_reference_images,
    max_reference_images: doc.data.max_reference_images,
  }));

  fs.writeFileSync(summaryPath, JSON.stringify(summary, null, 2));

  console.log(`📋 Summary file created:`);
  console.log(`   ${summaryPath}\n`);

  process.exit(0);
}

exportFirestoreData().catch((error) => {
  console.error("❌ Error:", error);
  process.exit(1);
});

