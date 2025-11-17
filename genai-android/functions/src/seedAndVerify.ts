/**
 * Seed Firestore and then verify the data
 *
 * Usage:
 * npm run seed:and:verify
 */

import {seedSpecificModels} from "./seedSpecificModels";
import {verifyFirestoreData} from "./verifyFirestoreData";

async function seedAndVerify() {
  console.log("=".repeat(80));
  console.log("STEP 1: SEEDING MODELS TO FIRESTORE");
  console.log("=".repeat(80));
  console.log();

  try {
    await seedSpecificModels();
    console.log();
    console.log("=".repeat(80));
    console.log("STEP 2: VERIFYING DATA IN FIRESTORE");
    console.log("=".repeat(80));
    console.log();

    await verifyFirestoreData();

    console.log();
    console.log("=".repeat(80));
    console.log("✅ SUCCESS! All models seeded and verified.");
    console.log("=".repeat(80));
    console.log();
    console.log("🌐 Open the Emulator UI at: http://127.0.0.1:4000");
    console.log("   Navigate to Firestore → video_features collection");
    console.log("   You should see all 18 models there!");
  } catch (error) {
    console.error("❌ Error:", error);
    process.exit(1);
  }
}

// Run if called directly
if (require.main === module) {
  seedAndVerify()
    .then(() => {
      process.exit(0);
    })
    .catch((error) => {
      console.error("💥 Fatal error:", error);
      process.exit(1);
    });
}

export {seedAndVerify};

