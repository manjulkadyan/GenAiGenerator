/**
 * Fetch ALL text-to-video models from Replicate API and seed Firestore
 *
 * This script:
 * 1. Fetches all models from Replicate API
 * 2. Filters for text-to-video models
 * 3. Fetches detailed info (schema, pricing) for each model
 * 4. Seeds Firestore with complete model data
 *
 * Usage:
 * export REPLICATE_API_TOKEN=r8_your_token
 * npm run seed:all
 */

import * as admin from "firebase-admin";

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

type ModelVersion = {
  id: string;
  created_at: string;
  cog_version?: string;
  openapi_schema?: {
    components?: {
      schemas?: {
        Input?: {
          properties?: Record<string, unknown>;
          required?: string[];
        };
      };
    };
  };
};

type ModelPricing = {
  hardware?: string;
  price_per_second?: number;
  min_time?: number;
};

/**
 * Fetch all models from Replicate API (with pagination)
 */
async function fetchAllReplicateModels(apiToken: string): Promise<ReplicateModel[]> {
  const allModels: ReplicateModel[] = [];
  let nextUrl: string | null = "https://api.replicate.com/v1/models";

  console.log("📡 Fetching all models from Replicate API...");

  while (nextUrl) {
    const response: Response = await fetch(nextUrl, {
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

    const data = (await response.json()) as {
      results?: ReplicateModel[];
      next?: string | null;
    };
    allModels.push(...(data.results || []));

    // Check for next page
    nextUrl = data.next || null;

    if (nextUrl) {
      console.log(`   Fetched ${allModels.length} models so far...`);
    }
  }

  console.log(`✅ Fetched ${allModels.length} total models\n`);
  return allModels;
}

/**
 * Filter for text-to-video models
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
    "wan",
    "t2v",
    "text2video",
    "video-gen",
    "video-generation",
    "animate-image",
    "img2vid",
    "image2video",
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
    "segment",
    "caption",
    "describe",
    "embed",
    "translate",
    "transcribe",
    "audio",
    "music",
    "speech",
    "tts",
    "text-to-speech",
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
 * Fetch model version details (schema, pricing)
 */
async function fetchModelDetails(
  apiToken: string,
  modelName: string,
): Promise<{schema?: ModelVersion["openapi_schema"]; pricing?: ModelPricing}> {
  try {
    // Fetch model details
    const modelResponse: Response = await fetch(
      `https://api.replicate.com/v1/models/${modelName}`,
      {
        headers: {
          "Authorization": `Bearer ${apiToken}`,
          "Content-Type": "application/json",
        },
      },
    );

    if (!modelResponse.ok) {
      return {};
    }

    const modelData = (await modelResponse.json()) as {
      latest_version?: {id: string};
    };
    const latestVersion = modelData.latest_version;

    if (!latestVersion) {
      return {};
    }

    // Fetch version details for schema
    const versionResponse: Response = await fetch(
      `https://api.replicate.com/v1/models/${modelName}/versions/${latestVersion.id}`,
      {
        headers: {
          "Authorization": `Bearer ${apiToken}`,
          "Content-Type": "application/json",
        },
      },
    );

    if (!versionResponse.ok) {
      return {};
    }

    const versionData = (await versionResponse.json()) as ModelVersion;

    return {
      schema: versionData.openapi_schema,
      pricing: {
        // Pricing info is usually on the model page, not in API
        // We'll estimate based on model type
      },
    };
  } catch (error) {
    console.warn(`   ⚠️  Could not fetch details for ${modelName}:`, error);
    return {};
  }
}

/**
 * Extract model parameters from schema
 */
function extractModelParams(schema?: ModelVersion["openapi_schema"]): {
  aspectRatios: string[];
  durations: number[];
  supportsFirstFrame: boolean;
  supportsLastFrame: boolean;
  supportsImage: boolean;
} {
  const defaults = {
    aspectRatios: ["16:9", "9:16", "1:1"],
    durations: [3, 5, 10],
    supportsFirstFrame: false,
    supportsLastFrame: false,
    supportsImage: false,
  };

  if (!schema?.components?.schemas?.Input?.properties) {
    return defaults;
  }

  const properties = schema.components.schemas.Input.properties;
  const aspectRatios: string[] = [];
  const durations: number[] = [];
  let supportsFirstFrame = false;
  let supportsLastFrame = false;
  let supportsImage = false;

  // Check for aspect ratio parameter
  if (properties.aspect_ratio || properties.aspectRatio) {
    const aspectProp = (properties.aspect_ratio || properties.aspectRatio) as {
      enum?: string[];
      default?: string;
    };
    if (aspectProp.enum) {
      aspectRatios.push(...aspectProp.enum);
    } else if (aspectProp.default) {
      aspectRatios.push(aspectProp.default);
    }
  }

  // Check for duration parameter
  if (properties.duration || properties.durationSeconds) {
    const durationProp = (properties.duration || properties.durationSeconds) as {
      enum?: number[];
      default?: number;
      minimum?: number;
      maximum?: number;
    };
    if (durationProp.enum) {
      durations.push(...durationProp.enum);
    } else if (durationProp.default) {
      durations.push(durationProp.default);
    } else if (durationProp.minimum && durationProp.maximum) {
      // Generate common durations in range
      for (let i = durationProp.minimum; i <= durationProp.maximum; i += 1) {
        if (i <= 15) durations.push(i);
      }
    }
  }

  // Check for image input parameters
  supportsFirstFrame = !!(properties.first_frame || properties.firstFrame || properties.image);
  supportsLastFrame = !!(properties.last_frame || properties.lastFrame);
  supportsImage = !!(properties.image || properties.input_image);

  return {
    aspectRatios: aspectRatios.length > 0 ? aspectRatios : defaults.aspectRatios,
    durations: durations.length > 0 ? durations : defaults.durations,
    supportsFirstFrame,
    supportsLastFrame,
    supportsImage,
  };
}

/**
 * Estimate pricing based on model name and type
 */
function estimatePricing(modelName: string): number {
  const name = modelName.toLowerCase();

  // Premium models
  if (name.includes("veo-3") || name.includes("veo3")) return 20;
  if (name.includes("kling") && name.includes("pro")) return 18;
  if (name.includes("veo-2") || name.includes("veo2")) return 15;
  if (name.includes("runway") && name.includes("gen3")) return 16;

  // Mid-tier models
  if (name.includes("kling")) return 14;
  if (name.includes("flux")) return 12;
  if (name.includes("runway")) return 14;
  if (name.includes("pika")) return 13;

  // Budget models
  if (name.includes("zeroscope")) return 10;
  if (name.includes("wan")) return 11;
  if (name.includes("fast") || name.includes("turbo")) return 10;

  // Default
  return 12;
}

/**
 * Convert Replicate model to Firestore document
 */
async function convertToFirestoreModel(
  model: ReplicateModel,
  index: number,
  apiToken: string,
): Promise<Record<string, unknown> | null> {
  try {
    // Extract model ID
    const modelId = model.name.split("/").pop() || model.name.replace("/", "-");
    const displayName = model.name.split("/").pop() || model.name;

    // Fetch model details
    const details = await fetchModelDetails(apiToken, model.name);
    const params = extractModelParams(details.schema);

    // Build Firestore document
    const firestoreModel: Record<string, unknown> = {
      id: modelId,
      name: displayName,
      description: model.description || "AI video generation model",
      price_per_sec: estimatePricing(model.name),
      default_duration: params.durations[0] || 5,
      duration_options: params.durations.length > 0 ? params.durations : [3, 5, 10],
      aspect_ratios: params.aspectRatios,
      requires_first_frame: params.supportsFirstFrame,
      requires_last_frame: params.supportsLastFrame,
      preview_url: model.cover_image_url || "",
      replicate_name: model.name,
      index: index,
      trending: false,
      official: model.visibility === "public",
      github_url: model.github_url || "",
      paper_url: model.paper_url || "",
      license_url: model.license_url || "",
      supports_image_input: params.supportsImage,
      created_at: admin.firestore.FieldValue.serverTimestamp(),
    };

    return firestoreModel;
  } catch (error) {
    console.error(`   ❌ Error converting ${model.name}:`, error);
    return null;
  }
}

/**
 * Main function to fetch and seed all video models
 */
async function fetchAndSeedAllModels() {
  const apiToken = process.env.REPLICATE_API_TOKEN;

  if (!apiToken) {
    throw new Error(
      "REPLICATE_API_TOKEN environment variable is required. " +
      "Get your token from: https://replicate.com/account/api-tokens"
    );
  }

  console.log("🚀 Starting comprehensive model fetch and seed...\n");

  // Step 1: Fetch all models
  const allModels = await fetchAllReplicateModels(apiToken);

  // Step 2: Filter for video models
  console.log("🔍 Filtering for text-to-video models...");
  const videoModels = allModels.filter(isVideoModel);
  console.log(`✅ Found ${videoModels.length} text-to-video models\n`);

  if (videoModels.length === 0) {
    console.warn("⚠️  No video models found. Check your filter criteria.");
    return;
  }

  // Step 3: Process and seed each model
  console.log("📦 Processing models and seeding to Firestore...\n");

  let successCount = 0;
  let errorCount = 0;
  const errors: Array<{model: string; error: string}> = [];

  for (let i = 0; i < videoModels.length; i++) {
    const model = videoModels[i];
    console.log(`[${i + 1}/${videoModels.length}] Processing: ${model.name}`);

    try {
      const firestoreModel = await convertToFirestoreModel(model, i, apiToken);

      if (!firestoreModel) {
        errorCount++;
        errors.push({model: model.name, error: "Conversion failed"});
        console.log("   ⚠️  Skipped (conversion failed)\n");
        continue;
      }

      // Seed to Firestore
      await firestore
        .collection("video_features")
        .doc(firestoreModel.id as string)
        .set(firestoreModel, {merge: true});

      console.log(`   ✅ Seeded: ${firestoreModel.name}`);
      successCount++;

      // Small delay to avoid rate limiting
      if (i < videoModels.length - 1) {
        await new Promise((resolve) => setTimeout(resolve, 500));
      }
    } catch (error) {
      errorCount++;
      const errorMsg = error instanceof Error ? error.message : String(error);
      errors.push({model: model.name, error: errorMsg});
      console.error(`   ❌ Error: ${errorMsg}\n`);
    }
  }

  // Summary
  console.log("\n" + "=".repeat(60));
  console.log("📊 SEEDING SUMMARY");
  console.log("=".repeat(60));
  console.log(`✅ Successfully seeded: ${successCount} models`);
  console.log(`❌ Errors: ${errorCount} models`);
  console.log(`📦 Total processed: ${videoModels.length} models`);

  if (errors.length > 0) {
    console.log("\n⚠️  Errors encountered:");
    errors.slice(0, 10).forEach(({model, error}) => {
      console.log(`   - ${model}: ${error}`);
    });
    if (errors.length > 10) {
      console.log(`   ... and ${errors.length - 10} more errors`);
    }
  }

  console.log("\n🎉 Done! All models are now available in your app.");
  console.log("\n💡 Tip: Check Firestore collection 'video_features' to see all seeded models.");
}

// Run if called directly
if (require.main === module) {
  fetchAndSeedAllModels()
    .then(() => {
      process.exit(0);
    })
    .catch((error) => {
      console.error("💥 Fatal error:", error);
      process.exit(1);
    });
}

export {fetchAndSeedAllModels};

