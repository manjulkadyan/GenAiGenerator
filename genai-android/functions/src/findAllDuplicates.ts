/**
 * Find ALL duplicates including those with different ID formats
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
 * Normalize ID for comparison
 */
function normalizeId(id: string): string {
  return id.toLowerCase().replace(/[_-]/g, "-");
}

/**
 * Extract model name from replicate_name
 */
function extractModelName(replicateName: string): string {
  const parts = replicateName.split("/");
  return parts.length === 2 ? parts[1] : replicateName;
}

async function findAllDuplicates() {
  console.log("🔍 Finding ALL duplicates...\n");

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

  // Find all duplicates
  const duplicates: Array<{
    replicateName: string;
    documents: Array<{id: string; name: string}>;
    status: string;
  }> = [];

  byReplicateName.forEach((docs, replicateName) => {
    if (docs.length > 1) {
      const modelName = extractModelName(replicateName);
      const normalizedModelName = normalizeId(modelName);

      // Check if IDs match expected patterns
      const hasWithPrefix = docs.some((d) =>
        normalizeId(d.id).includes(normalizeId(replicateName.split("/")[0])),
      );
      const hasWithoutPrefix = docs.some((d) =>
        normalizeId(d.id) === normalizedModelName ||
        normalizeId(d.id).endsWith(`-${normalizedModelName}`),
      );

      let status = "";
      if (hasWithPrefix && hasWithoutPrefix) {
        status = "✅ Both patterns exist";
      } else if (hasWithPrefix) {
        status = "⚠️  Only with prefix pattern";
      } else if (hasWithoutPrefix) {
        status = "⚠️  Only without prefix pattern";
      } else {
        status = "❓ Different ID patterns";
      }

      duplicates.push({
        replicateName,
        documents: docs.map((d) => ({
          id: d.id,
          name: d.data.name as string,
        })),
        status,
      });
    }
  });

  console.log(`📊 Found ${duplicates.length} models with duplicates\n`);
  console.log("=".repeat(80));

  duplicates.forEach((dup) => {
    console.log(`\n📌 ${dup.replicateName}`);
    console.log(`   Status: ${dup.status}`);
    dup.documents.forEach((doc) => {
      console.log(`   - ${doc.id} (${doc.name})`);
    });
  });

  // Save to file
  const outputPath = path.join(
    __dirname,
    "..",
    "all_duplicates_analysis.json",
  );
  fs.writeFileSync(
    outputPath,
    JSON.stringify(
      {
        analyzed_at: new Date().toISOString(),
        total_duplicates: duplicates.length,
        duplicates: duplicates,
        summary: {
          both_patterns: duplicates.filter(
            (d) => d.status === "✅ Both patterns exist",
          ).length,
          only_with_prefix: duplicates.filter(
            (d) => d.status === "⚠️  Only with prefix pattern",
          ).length,
          only_without_prefix: duplicates.filter(
            (d) => d.status === "⚠️  Only without prefix pattern",
          ).length,
          different_patterns: duplicates.filter(
            (d) => d.status === "❓ Different ID patterns",
          ).length,
        },
      },
      null,
      2,
    ),
  );

  console.log("\n" + "=".repeat(80));
  console.log("\n✅ Analysis complete!");
  console.log(`   Total duplicates found: ${duplicates.length}`);
  console.log(`   Results saved to: ${outputPath}\n`);

  process.exit(0);
}

findAllDuplicates().catch((error) => {
  console.error("❌ Error:", error);
  process.exit(1);
});

