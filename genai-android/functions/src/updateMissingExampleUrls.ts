/**
 * Update missing example_video_urls for models in Firestore
 */

import * as admin from "firebase-admin";
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

if (!require("fs").existsSync(serviceAccountPath)) {
  console.error(`❌ Service account key not found: ${serviceAccountPath}`);
  process.exit(1);
}

const serviceAccount = JSON.parse(
  require("fs").readFileSync(serviceAccountPath, "utf-8"),
);

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

// Example URLs from replicate-specific-models copy.txt
const missingUrls: Record<string, string[]> = {
  "ltx-2-fast": [
    "https://replicate.delivery/xezq/VUdJthWRZxJMOdVL2lBFR1K9J2nLUUjngDiyagPbjAejvehVA/tmplkv0_7wi.mp4",
    "https://replicate.delivery/xezq/OZDRKjW01V54BNhZll1lo9CVPIU2pbD8yGGSvFRy1MAAmfwKA/tmpoahhx6gd.mp4",
    "https://replicate.delivery/xezq/zsD7zCTyuL7eW68mhR2To8DZoS4OT2BQd2oXP3ZOK1U0MfhVA/tmp8un0w8yy.mp4",
    "https://replicate.delivery/xezq/ZuMzPe5GtOxtVK59EO2a4F8hDrDESMTPUshVB86dffS538DrA/tmp55nt3l_c.mp4",
    "https://replicate.delivery/xezq/qZJqNQUGOQZBL11GKYyMf6j9zebygo2QxsbmGztcezBQ48DrA/tmpycq8b_l0.mp4",
    "https://replicate.delivery/xezq/C5lKtwHSBloDJB5LmasPpsRZdVUgqTnwdK2BjASYOe4rRfhVA/tmp69__x443.mp4",
    "https://replicate.delivery/xezq/Xe3HUYeQMIlJzEmRW2RFdH6Vifeg8Z1PkAmhlc5eS0pqrLQsC/tmpzqz2e09l.mp4",
    "https://replicate.delivery/xezq/FvR9AwLdbzIKNRQ0y2pwEDf0BgF0Ez1sQE8eZteW1J7XcfGWB/tmpan00ukuq.mp4",
    "https://replicate.delivery/pbxt/Nvp4bHMZRVeafvUBJKIfzlqkqRjLl8Fyn3JMuWUlUWEVhzuT/replicate-prediction-pwnbqza6f9rm80ct1sgtfgvwdr.png",
  ],
  "ltx-2-pro": [
    "https://replicate.delivery/xezq/M8krcmsXwRrSK1DuufUjOpLjfHs9KGF7zgespA2hugJ1V7DrA/tmpc_m94ikd.mp4",
    "https://replicate.delivery/xezq/GPXMuvvmsJp9P5zhA8iXvkFVETpel5tiMdhtUhWjsBD8RfhVA/tmp3_rwznmf.mp4",
    "https://replicate.delivery/xezq/SKUPcbemc2VNE6B3ykiKKIYn3w7S3BQaMg4YIf4kN0zHoeDrA/tmpjk5lvmjy.mp4",
    "https://replicate.delivery/xezq/FN4X6kFZhRKEMJNH8pDa1mtg3kcGdfDrINIg38FCqalbWfhVA/tmp3q56smse.mp4",
    "https://replicate.delivery/xezq/10DE4e0psL1EBiQqZA9rc7JdCBmdGKpc0NoeaJLfzuRic9DrA/tmpaxus5iln.mp4",
    "https://replicate.delivery/xezq/OdK1NDj7x6rwNdfLRGCHzrz3OS20dbBlW6dGI73Y09eYzeDrA/tmpujshdc0p.mp4",
    "https://replicate.delivery/pbxt/NvqakTNQAu10qQBXiS00K2Z4pDgWafgrVrZjsFU5ipAEBqp5/replicate-prediction-hq3n9jetmxrmc0ct1sj9k38jpg.png",
  ],
};

async function updateMissingExampleUrls() {
  console.log("🔄 Updating missing example_video_urls in Firestore...\n");

  const collection = firestore.collection("video_features");
  let updatedCount = 0;

  for (const [modelId, urls] of Object.entries(missingUrls)) {
    try {
      const docRef = collection.doc(modelId);
      const doc = await docRef.get();

      if (!doc.exists) {
        console.log(`⚠️  Document ${modelId} not found, skipping...`);
        continue;
      }

      console.log(`📝 Updating ${modelId}...`);
      console.log(`   Adding ${urls.length} example video URLs`);

      await docRef.update({
        example_video_urls: urls,
      });

      updatedCount++;
      console.log(`   ✅ Updated ${modelId}\n`);
    } catch (error) {
      console.error(`   ❌ Error updating ${modelId}:`, error);
    }
  }

  console.log(`\n✅ Successfully updated ${updatedCount} documents`);
  process.exit(0);
}

updateMissingExampleUrls().catch((error) => {
  console.error("❌ Error:", error);
  process.exit(1);
});

