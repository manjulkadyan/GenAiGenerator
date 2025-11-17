/**
 * Merge duplicate models and flag missing pairs
 * - Takes example_video_urls and description from bytedance-seedance-1-lite
 * - Takes rest from seedance-1-lite
 * - Deletes the duplicate with company prefix
 * - Flags all duplicate pairs
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

/**
 * Extract company prefix from replicate_name
 */
function extractCompanyPrefix(replicateName: string): string | null {
  const parts = replicateName.split("/");
  if (parts.length === 2) {
    return parts[0];
  }
  return null;
}

/**
 * Extract model name without company prefix
 */
function extractModelName(replicateName: string): string {
  const parts = replicateName.split("/");
  if (parts.length === 2) {
    return parts[1];
  }
  return replicateName;
}

/**
 * Normalize ID for comparison (handles dots, dashes, underscores)
 */
function normalizeIdForComparison(id: string): string {
  return id.toLowerCase().replace(/[._-]/g, "");
}

/**
 * Generate ID without company prefix
 */
function generateIdWithoutPrefix(replicateName: string): string {
  return extractModelName(replicateName).replace(/\//g, "-").toLowerCase();
}

/**
 * Generate ID with company prefix
 */
function generateIdWithPrefix(replicateName: string): string {
  return replicateName.replace(/\//g, "-").toLowerCase();
}

/**
 * Find duplicate pairs
 */
function findDuplicatePairs(
  documents: Array<{id: string; data: Record<string, unknown>}>,
): Array<{
  withPrefix: {id: string; data: Record<string, unknown>} | null;
  withoutPrefix: {id: string; data: Record<string, unknown>} | null;
  replicateName: string;
}> {
  const pairs: Array<{
    withPrefix: {id: string; data: Record<string, unknown>} | null;
    withoutPrefix: {id: string; data: Record<string, unknown>} | null;
    replicateName: string;
  }> = [];

  // Group by replicate_name
  const byReplicateName = new Map<
    string,
    Array<{id: string; data: Record<string, unknown>}>
  >();

  documents.forEach((doc) => {
    const replicateName = doc.data.replicate_name as string;
    if (!replicateName) return;

    if (!byReplicateName.has(replicateName)) {
      byReplicateName.set(replicateName, []);
    }
    byReplicateName.get(replicateName)!.push(doc);
  });

  // Find pairs - use normalized comparison to catch different ID formats
  byReplicateName.forEach((docs, replicateName) => {
    const companyPrefix = extractCompanyPrefix(replicateName);
    if (!companyPrefix) return;

    const idWithPrefix = generateIdWithPrefix(replicateName);
    const idWithoutPrefix = generateIdWithoutPrefix(replicateName);

    // Try exact match first
    let withPrefix = docs.find((d) => d.id === idWithPrefix) || null;
    let withoutPrefix = docs.find((d) => d.id === idWithoutPrefix) || null;

    // If not found, try normalized comparison (handles dots vs dashes)
    if (!withPrefix) {
      withPrefix =
        docs.find(
          (d) =>
            normalizeIdForComparison(d.id) ===
              normalizeIdForComparison(idWithPrefix) &&
            normalizeIdForComparison(d.id) !==
              normalizeIdForComparison(idWithoutPrefix),
        ) || null;
    }

    if (!withoutPrefix) {
      withoutPrefix =
        docs.find(
          (d) =>
            normalizeIdForComparison(d.id) ===
              normalizeIdForComparison(idWithoutPrefix) &&
            normalizeIdForComparison(d.id) !==
              normalizeIdForComparison(idWithPrefix),
        ) || null;
    }

    if (withPrefix || withoutPrefix) {
      pairs.push({
        withPrefix,
        withoutPrefix,
        replicateName,
      });
    }
  });

  return pairs;
}

async function mergeAndDeduplicate() {
  console.log("🔄 Merging duplicates and flagging missing pairs...\n");

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

  // Find all duplicate pairs
  const pairs = findDuplicatePairs(documents);

  console.log(`📊 Found ${pairs.length} potential duplicate pairs\n`);

  // Process all duplicate pairs that have both versions
  console.log("🔀 Processing duplicate pairs...\n");

  let mergedCount = 0;
  let deletedCount = 0;

  for (const pair of pairs) {
    if (!pair.withPrefix || !pair.withoutPrefix) {
      continue; // Skip if one is missing
    }

    console.log(`\n📌 Processing: ${pair.replicateName}`);

    // Get example_video_urls and description from version with prefix
    const exampleVideoUrls =
      pair.withPrefix.data.example_video_url ||
      pair.withPrefix.data.example_video_urls ||
      pair.withoutPrefix.data.example_video_url ||
      pair.withoutPrefix.data.example_video_urls ||
      null;

    const description =
      (pair.withPrefix.data.description as string) ||
      (pair.withoutPrefix.data.description as string) ||
      "";

    // Merge: use version without prefix as base, add example_video_urls and description
    const mergedData = {
      ...pair.withoutPrefix.data,
      example_video_url: exampleVideoUrls,
      example_video_urls: exampleVideoUrls,
      description: description || pair.withoutPrefix.data.description,
    };

    // Update version without prefix
    await collection.doc(pair.withoutPrefix.id).set(mergedData);
    console.log(`   ✅ Updated ${pair.withoutPrefix.id} with merged data`);

    // Delete version with prefix
    await collection.doc(pair.withPrefix.id).delete();
    console.log(`   🗑️  Deleted ${pair.withPrefix.id}`);

    mergedCount++;
    deletedCount++;
  }

  console.log(`\n✅ Processed ${mergedCount} duplicate pairs`);
  console.log(`   Merged: ${mergedCount}`);
  console.log(`   Deleted: ${deletedCount}\n`);

  // Find potential duplicates by comparing model names (case-insensitive)
  console.log("\n🔍 Finding additional potential duplicates by name...\n");

  const nameMap = new Map<string, Array<{id: string; data: Record<string, unknown>}>>();

  documents.forEach((doc) => {
    const name = (doc.data.name as string)?.toLowerCase().trim();
    if (!name) return;

    if (!nameMap.has(name)) {
      nameMap.set(name, []);
    }
    nameMap.get(name)!.push(doc);
  });

  const nameDuplicates: Array<{
    name: string;
    documents: Array<{id: string; replicateName: string}>;
  }> = [];

  nameMap.forEach((docs, name) => {
    if (docs.length > 1) {
      nameDuplicates.push({
        name,
        documents: docs.map((d) => ({
          id: d.id,
          replicateName: d.data.replicate_name as string,
        })),
      });
    }
  });

  // Flag all duplicate pairs
  console.log("🏷️  Duplicate Pairs Analysis:\n");
  console.log("=".repeat(80));

  const flaggedPairs: Array<{
    replicateName: string;
    status: string;
    withPrefixId?: string;
    withoutPrefixId?: string;
  }> = [];

  pairs.forEach((pair) => {
    const status = pair.withPrefix && pair.withoutPrefix ?
      "✅ Both exist" :
      pair.withPrefix ?
        "⚠️  Only with prefix" :
        pair.withoutPrefix ?
          "⚠️  Only without prefix" :
          "❌ Neither found";

    flaggedPairs.push({
      replicateName: pair.replicateName,
      status,
      withPrefixId: pair.withPrefix?.id,
      withoutPrefixId: pair.withoutPrefix?.id,
    });

    console.log(`\n📌 ${pair.replicateName}`);
    console.log(`   Status: ${status}`);
    if (pair.withPrefix) {
      console.log(`   With prefix: ${pair.withPrefix.id}`);
    }
    if (pair.withoutPrefix) {
      console.log(`   Without prefix: ${pair.withoutPrefix.id}`);
    }
  });

  // Show name-based duplicates that weren't caught
  if (nameDuplicates.length > 0) {
    console.log("\n\n⚠️  Additional duplicates found by name (not by replicate_name):");
    console.log("=".repeat(80));
    nameDuplicates.forEach((dup) => {
      const replicateNames = dup.documents.map((d) => d.replicateName);
      const allSameReplicate = new Set(replicateNames).size === 1;

      if (!allSameReplicate) {
        console.log(`\n📌 Name: "${dup.name}"`);
        dup.documents.forEach((doc) => {
          console.log(`   - ${doc.id} (${doc.replicateName})`);
        });
      }
    });
  }

  // Save flagged pairs to file
  const outputPath = path.join(
    __dirname,
    "..",
    "duplicate_pairs_analysis.json",
  );
  fs.writeFileSync(
    outputPath,
    JSON.stringify(
      {
        analyzed_at: new Date().toISOString(),
        total_pairs: flaggedPairs.length,
        pairs: flaggedPairs,
        name_based_duplicates: nameDuplicates.filter((dup) => {
          const replicateNames = dup.documents.map((d) => d.replicateName);
          return new Set(replicateNames).size > 1;
        }),
        summary: {
          both_exist: flaggedPairs.filter((p) => p.status === "✅ Both exist")
            .length,
          only_with_prefix: flaggedPairs.filter(
            (p) => p.status === "⚠️  Only with prefix",
          ).length,
          only_without_prefix: flaggedPairs.filter(
            (p) => p.status === "⚠️  Only without prefix",
          ).length,
          merged: mergedCount,
          deleted: deletedCount,
        },
      },
      null,
      2,
    ),
  );

  console.log("\n" + "=".repeat(80));
  console.log(`\n✅ Analysis complete!`);
  console.log(`   Total pairs found: ${flaggedPairs.length}`);
  console.log(`   Merged: ${mergedCount}`);
  console.log(`   Deleted: ${deletedCount}`);
  console.log(`   Additional name-based duplicates: ${nameDuplicates.filter((dup) => {
    const replicateNames = dup.documents.map((d) => d.replicateName);
    return new Set(replicateNames).size > 1;
  }).length}`);
  console.log(`   Results saved to: ${outputPath}\n`);

  process.exit(0);
}

mergeAndDeduplicate().catch((error) => {
  console.error("❌ Error:", error);
  process.exit(1);
});

