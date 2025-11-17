/**
 * Script to seed Firestore with AI models from Replicate
 *
 * Run this script to populate the video_features collection:
 *
 * Option 1: Run as Firebase Function (temporary)
 * Option 2: Run locally with: npx ts-node src/seedModels.ts
 * Option 3: Use Firebase Admin SDK in a script
 */

import * as admin from "firebase-admin";

// Initialize Firebase Admin (if not already initialized)
if (!admin.apps.length) {
  admin.initializeApp();
}

const firestore = admin.firestore();

/**
 * Seed video_features collection with AI models
 *
 * These are example models - you should customize based on:
 * 1. What models Replicate actually offers
 * 2. What models you want to support
 * 3. Pricing and features
 */
async function seedModels() {
  const models = [
    {
      id: "veo-3-1",
      name: "Veo 3.1",
      description: "Google's state-of-the-art video generation with synchronized native audio, enhanced prompt adherence, and image-to-video capabilities",
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
      supports_audio: true,
      supports_reference_images: true,
      max_reference_images: 3,
    },
    {
      id: "kling-v2-5-turbo-pro",
      name: "Kling 2.5 Turbo Pro",
      description: "Pro-level text-to-video and image-to-video with smooth motion, cinematic depth, and remarkable prompt adherence",
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
    },
    {
      id: "veo-2",
      name: "Veo 2",
      description: "Google's high-quality video generation model",
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
    },
    {
      id: "flux-schnell",
      name: "FLUX.1 [schnell]",
      description: "Fast image-to-video generation from Black Forest Labs",
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
    },
    {
      id: "zeroscope-v2-xl",
      name: "Zeroscope v2 XL",
      description: "High-quality video generation model",
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
    },
    {
      id: "runway-gen3",
      name: "Runway Gen-3",
      description: "Runway's latest video generation model",
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
    },
    {
      id: "pika-labs",
      name: "Pika Labs",
      description: "Creative video generation with style control",
      price_per_sec: 14,
      default_duration: 4,
      duration_options: [3, 4, 5],
      aspect_ratios: ["16:9", "9:16", "1:1"],
      requires_first_frame: false,
      requires_last_frame: false,
      preview_url: "",
      replicate_name: "pika/pika",
      index: 6,
      trending: false,
    },
  ];

  console.log("Seeding models to Firestore...");

  for (const model of models) {
    try {
      await firestore.collection("video_features").doc(model.id).set(model);
      console.log(`✅ Seeded model: ${model.name} (${model.id})`);
    } catch (error) {
      console.error(`❌ Failed to seed ${model.id}:`, error);
    }
  }

  console.log(`\n✅ Seeded ${models.length} models to video_features collection`);
}

// Run if called directly
if (require.main === module) {
  seedModels()
    .then(() => {
      console.log("Done!");
      process.exit(0);
    })
    .catch((error) => {
      console.error("Error seeding models:", error);
      process.exit(1);
    });
}

export {seedModels};

