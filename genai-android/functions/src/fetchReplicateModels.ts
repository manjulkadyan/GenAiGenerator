/**
 * Fetch available models from Replicate API and seed Firestore
 *
 * This script:
 * 1. Fetches models from Replicate API
 * 2. Filters for video generation models
 * 3. Seeds Firestore with model data
 *
 * Usage:
 * - Set REPLICATE_API_TOKEN environment variable
 * - Run: npx ts-node src/fetchReplicateModels.ts
 */

import * as admin from "firebase-admin";

// Initialize Firebase Admin
if (!admin.apps.length) {
  admin.initializeApp();
}

const firestore = admin.firestore();

type ReplicateModel = {
  name: string;
  description?: string;
  visibility: string;
  github_url?: string;
  paper_url?: string;
  license_url?: string;
  cover_image_url?: string;
  default_example?: {
    input?: Record<string, unknown>;
    output?: string | string[];
  };
  latest_version?: {
    id: string;
    created_at: string;
  };
};

/**
 * Fetch models from Replicate API
 */
async function fetchReplicateModels(apiToken: string): Promise<ReplicateModel[]> {
  const response = await fetch("https://api.replicate.com/v1/models", {
    method: "GET",
    headers: {
      "Authorization": `Bearer ${apiToken}`,
      "Content-Type": "application/json",
    },
  });

  if (!response.ok) {
    const text = await response.text();
    throw new Error(`Replicate API error: ${response.status} - ${text}`);
  }

  const data = await response.json();
  return data.results || [];
}

/**
 * Filter video generation models
 * You can customize this filter based on your needs
 */
function isVideoModel(model: ReplicateModel): boolean {
  const name = model.name.toLowerCase();
  const description = (model.description || "").toLowerCase();

  // Filter for video-related models
  const videoKeywords = [
    "veo",
    "video",
    "animate",
    "motion",
    "flux",
    "runway",
    "pika",
    "kling",
    "zeroscope",
    "text-to-video",
    "image-to-video",
    "gen3",
    "gen-3",
  ];

  // Exclude non-video models
  const excludeKeywords = [
    "image",
    "upscale",
    "enhance",
    "remove",
    "background",
    "face",
    "detect",
    "classify",
  ];

  const hasVideoKeyword = videoKeywords.some(
    (keyword) => name.includes(keyword) || description.includes(keyword)
  );

  const hasExcludeKeyword = excludeKeywords.some(
    (keyword) => name.includes(keyword) || description.includes(keyword)
  );

  // Must have video keyword and NOT have exclude keyword
  return hasVideoKeyword && !hasExcludeKeyword;
}

/**
 * Convert Replicate model to Firestore document
 */
function convertToFirestoreModel(
  model: ReplicateModel,
  index: number,
): Record<string, unknown> {
  // Extract model name parts (e.g., "google/veo-2" -> "veo-2")
  const modelId = model.name.split("/").pop() || model.name.replace("/", "-");

  // Default values - customize based on model
  return {
    id: modelId,
    name: model.name.split("/").pop() || model.name, // Display name
    description: model.description || "AI video generation model",
    price_per_sec: 15, // Default pricing - adjust per model
    default_duration: 5,
    duration_options: [3, 5, 10], // Default options
    aspect_ratios: ["16:9", "9:16", "1:1"], // Default ratios
    requires_first_frame: false,
    requires_last_frame: false,
    preview_url: model.cover_image_url || model.default_example?.output as string || "",
    replicate_name: model.name, // Full Replicate model name
    index: index,
    trending: false,
    visibility: model.visibility,
    github_url: model.github_url,
    paper_url: model.paper_url,
    license_url: model.license_url,
  };
}

/**
 * Main function to fetch and seed models
 */
async function fetchAndSeedModels() {
  const apiToken = process.env.REPLICATE_API_TOKEN;

  if (!apiToken) {
    throw new Error(
      "REPLICATE_API_TOKEN environment variable is required. " +
      "Get your token from: https://replicate.com/account/api-tokens"
    );
  }

  console.log("Fetching models from Replicate API...");
  const allModels = await fetchReplicateModels(apiToken);

  console.log(`Found ${allModels.length} total models`);

  // Filter for video models
  const videoModels = allModels.filter(isVideoModel);
  console.log(`Found ${videoModels.length} video generation models`);

  if (videoModels.length === 0) {
    console.warn("No video models found. You may need to adjust the filter.");
    console.log("Available models:", allModels.slice(0, 10).map((m) => m.name));
    return;
  }

  console.log("\nSeeding models to Firestore...");

  let seeded = 0;
  for (let i = 0; i < videoModels.length; i++) {
    const model = videoModels[i];
    try {
      const firestoreModel = convertToFirestoreModel(model, i);
      await firestore
        .collection("video_features")
        .doc(firestoreModel.id as string)
        .set(firestoreModel, {merge: true});

      console.log(`✅ Seeded: ${firestoreModel.name} (${firestoreModel.replicate_name})`);
      seeded++;
    } catch (error) {
      console.error(`❌ Failed to seed ${model.name}:`, error);
    }
  }

  console.log(`\n✅ Successfully seeded ${seeded} models to video_features collection`);
}

// Run if called directly
if (require.main === module) {
  fetchAndSeedModels()
    .then(() => {
      console.log("Done!");
      process.exit(0);
    })
    .catch((error) => {
      console.error("Error:", error);
      process.exit(1);
    });
}

export {fetchAndSeedModels};

