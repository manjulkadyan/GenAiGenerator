/**
 * List the 4 pairs that were NOT matched/merged
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
  app = admin.app();
  firestore = admin.firestore();
}

async function listUnmatchedPairs() {
  console.log("🔍 Finding unmatched pairs...\n");

  const collection = firestore.collection("video_features");
  const snapshot = await collection.get();

  if (snapshot.empty) {
    console.error("❌ No documents found");
    process.exit(1);
  }

  const documents = snapshot.docs.map((doc) => ({
    id: doc.id,
    data: doc.data(),
  }));

  // Read the analysis file
  const analysisPath = path.join(
    __dirname,
    "..",
    "duplicate_pairs_analysis.json",
  );
  if (!fs.existsSync(analysisPath)) {
    console.error("❌ Analysis file not found. Run merge:duplicates first.");
    process.exit(1);
  }

  const analysis = JSON.parse(fs.readFileSync(analysisPath, "utf-8"));

  // Find pairs that have issues
  const unmatched: Array<{
    replicateName: string;
    status: string;
    withPrefixId?: string;
    withoutPrefixId?: string;
    issue: string;
  }> = [];

  analysis.pairs.forEach((pair: {
    replicateName: string;
    status: string;
    withPrefixId?: string;
    withoutPrefixId?: string;
  }) => {
    // Only with prefix - missing without prefix version
    if (pair.status === "⚠️  Only with prefix") {
      unmatched.push({
        ...pair,
        issue: "Missing version WITHOUT prefix - should create or keep as-is",
      });
    }

    // Only without prefix - these are already cleaned (prefix version was deleted)
    // We'll focus on the ones that are truly problematic
  });

  // The 4 unmatched pairs are the ones that have issues
  // 1. lightricks/ltx-2-fast - only without prefix (no version with prefix exists)
  // 2. lightricks/ltx-2-pro - only without prefix (no version with prefix exists)
  // 3. wan-video/wan-2.5-t2v-hd - only with prefix (no version without prefix exists)
  // 4. Check for one more...

  console.log("📋 The 4 Unmatched Pairs:\n");
  console.log("=".repeat(80));

  // Get the specific 4
  const top4 = [
    analysis.pairs.find((p: {replicateName: string}) =>
      p.replicateName === "lightricks/ltx-2-fast",
    ),
    analysis.pairs.find((p: {replicateName: string}) =>
      p.replicateName === "lightricks/ltx-2-pro",
    ),
    analysis.pairs.find((p: {replicateName: string}) =>
      p.replicateName === "wan-video/wan-2.5-t2v-hd",
    ),
    // Find one more that's truly unmatched
    analysis.pairs.find(
      (p: {status: string; replicateName: string}) =>
        p.status === "⚠️  Only without prefix" &&
        !p.replicateName.includes("lightricks") &&
        !p.replicateName.includes("wan-video"),
    ),
  ].filter((p) => p !== undefined);
  top4.forEach((pair, index) => {
    console.log(`\n${index + 1}. ${pair.replicateName}`);
    console.log(`   Status: ${pair.status}`);
    console.log(`   Issue: ${pair.issue}`);
    if (pair.withPrefixId) {
      console.log(`   With prefix ID: ${pair.withPrefixId}`);
      const doc = documents.find((d) => d.id === pair.withPrefixId);
      console.log(`   Exists: ${doc ? "✅ Yes" : "❌ No"}`);
    }
    if (pair.withoutPrefixId) {
      console.log(`   Without prefix ID: ${pair.withoutPrefixId}`);
      const doc = documents.find((d) => d.id === pair.withoutPrefixId);
      console.log(`   Exists: ${doc ? "✅ Yes" : "❌ No"}`);
    }
  });

  // Also check for lightricks models specifically
  console.log("\n\n🔍 Checking Lightricks models specifically:\n");
  const lightricksModels = documents.filter((d) =>
    (d.data.replicate_name as string)?.includes("lightricks"),
  );
  lightricksModels.forEach((doc) => {
    console.log(`   - ${doc.id} (${doc.data.replicate_name})`);
  });

  // Check wan-video-wan-2-5-t2v-hd
  console.log("\n\n🔍 Checking wan-video-wan-2-5-t2v-hd:\n");
  const wanHd = documents.find((d) => d.id === "wan-video-wan-2-5-t2v-hd");
  if (wanHd) {
    console.log(`   Found: ${wanHd.id}`);
    console.log(`   Replicate Name: ${wanHd.data.replicate_name}`);
    const expectedWithoutPrefix = "wan-2.5-t2v-hd";
    const existsWithoutPrefix = documents.some((d) => d.id === expectedWithoutPrefix);
    console.log(`   Expected without prefix: ${expectedWithoutPrefix}`);
    console.log(`   Exists without prefix: ${existsWithoutPrefix ? "✅ Yes" : "❌ No"}`);
  }

  process.exit(0);
}

listUnmatchedPairs().catch((error) => {
  console.error("❌ Error:", error);
  process.exit(1);
});

