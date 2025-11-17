/**
 * Seed Firestore with popular text-to-video models from Replicate
 *
 * This script seeds the most popular and reliable text-to-video models
 * based on actual Replicate availability and usage.
 *
 * Run: npm run seed:models
 */

import * as admin from "firebase-admin";

if (!admin.apps.length) {
  admin.initializeApp();
}

const firestore = admin.firestore();

/**
 * Popular text-to-video models from Replicate
 * Based on: https://replicate.com/search?query=text%20to%20video
 */
async function seedPopularModels() {
  const models = [
    // Google Veo 3.1 - Latest and most advanced
    {
      id: "veo-3-1",
      name: "Veo 3.1",
      description: "Google's state-of-the-art video generation with synchronized native audio, enhanced prompt adherence, and image-to-video capabilities. Supports reference images and frame-to-frame generation.",
      price_per_sec: 20,
      default_duration: 6,
      duration_options: [4, 6, 8],
      aspect_ratios: ["16:9", "9:16"],
      requires_first_frame: false,
      requires_last_frame: false,
      preview_url: "",
      replicate_name: "google/veo-3.1",
      index: 0,
      trending: true,
      official: true,
    },

    // Kling 2.5 Turbo Pro - Popular pro model
    {
      id: "kling-v2-5-turbo-pro",
      name: "Kling 2.5 Turbo Pro",
      description: "Pro-level text-to-video and image-to-video creation with smooth motion, cinematic depth, and remarkable prompt adherence. Better prompt understanding and realistic motion.",
      price_per_sec: 18,
      default_duration: 5,
      duration_options: [3, 5, 10],
      aspect_ratios: ["16:9", "9:16", "1:1"],
      requires_first_frame: false,
      requires_last_frame: false,
      preview_url: "",
      replicate_name: "kwaivgi/kling-v2.5-turbo-pro",
      index: 1,
      trending: true,
      official: false,
    },

    // Google Veo 2 - Previous generation
    {
      id: "veo-2",
      name: "Veo 2",
      description: "Google's high-quality video generation model with support for multiple aspect ratios",
      price_per_sec: 15,
      default_duration: 5,
      duration_options: [3, 5, 10],
      aspect_ratios: ["16:9", "9:16", "1:1", "21:9"],
      requires_first_frame: false,
      requires_last_frame: false,
      preview_url: "",
      replicate_name: "google/veo-2",
      index: 2,
      trending: false,
      official: true,
    },

    // FLUX.1 schnell - Fast generation
    {
      id: "flux-schnell",
      name: "FLUX.1 [schnell]",
      description: "Fast image-to-video generation from Black Forest Labs. Quick generation times with good quality.",
      price_per_sec: 12,
      default_duration: 4,
      duration_options: [3, 4, 5],
      aspect_ratios: ["16:9", "9:16", "1:1"],
      requires_first_frame: false,
      requires_last_frame: false,
      preview_url: "",
      replicate_name: "black-forest-labs/flux-schnell",
      index: 3,
      trending: true,
      official: false,
    },

    // Zeroscope v2 XL - Community favorite
    {
      id: "zeroscope-v2-xl",
      name: "Zeroscope v2 XL",
      description: "High-quality video generation model popular in the community",
      price_per_sec: 10,
      default_duration: 3,
      duration_options: [2, 3, 4],
      aspect_ratios: ["16:9", "9:16"],
      requires_first_frame: false,
      requires_last_frame: false,
      preview_url: "",
      replicate_name: "anotherjesse/zeroscope-v2-xl",
      index: 4,
      trending: false,
      official: false,
    },

    // Runway Gen-3 - Professional model
    {
      id: "runway-gen3",
      name: "Runway Gen-3",
      description: "Runway's latest professional video generation model with advanced controls",
      price_per_sec: 16,
      default_duration: 5,
      duration_options: [3, 5, 10],
      aspect_ratios: ["16:9", "9:16", "1:1"],
      requires_first_frame: false,
      requires_last_frame: false,
      preview_url: "",
      replicate_name: "runway/gen3",
      index: 5,
      trending: true,
      official: false,
    },
  ];

  console.log("🌱 Seeding popular text-to-video models to Firestore...\n");

  let successCount = 0;
  let errorCount = 0;

  for (const model of models) {
    try {
      await firestore.collection("video_features").doc(model.id).set(model, {merge: true});
      console.log(`✅ Seeded: ${model.name} (${model.replicate_name})`);
      successCount++;
    } catch (error) {
      console.error(`❌ Failed to seed ${model.id}:`, error);
      errorCount++;
    }
  }

  console.log("\n📊 Summary:");
  console.log(`   ✅ Success: ${successCount}`);
  console.log(`   ❌ Errors: ${errorCount}`);
  console.log(`   📦 Total: ${models.length}`);
  console.log("\n🎉 Done! Models are now available in your app.");
}

// Run if called directly
if (require.main === module) {
  seedPopularModels()
    .then(() => {
      process.exit(0);
    })
    .catch((error) => {
      console.error("💥 Error seeding models:", error);
      process.exit(1);
    });
}

export {seedPopularModels};

