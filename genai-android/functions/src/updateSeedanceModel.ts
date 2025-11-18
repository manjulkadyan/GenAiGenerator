/**
 * Update seedance-1-lite model in Firestore with correct parameters
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
 * Extract durations from min/max range
 * @param {number} min - Minimum duration value
 * @param {number} max - Maximum duration value
 * @return {number[]} Array of duration options
 */
function extractDurationsFromRange(
  min: number,
  max: number,
): number[] {
  const durations: number[] = [];
  // Generate reasonable options: include min, max, and evenly spaced values
  const step = Math.max(1, Math.floor((max - min) / 4));
  for (let d = min; d <= max; d += step) {
    durations.push(d);
  }
  // Always include max if not already included
  if (!durations.includes(max)) {
    durations.push(max);
  }
  // Sort and remove duplicates
  return [...new Set(durations)].sort((a, b) => a - b);
}

/**
 * Extract resolution options from schema
 * @param {Object} schema - Schema object with properties
 * @return {string[]} Array of resolution options
 */
function extractResolutions(schema: {
  properties?: Record<string, unknown>;
}): string[] {
  const resolutionProp = schema.properties?.resolution;
  if (!resolutionProp) return [];

  if (Array.isArray((resolutionProp as {enum?: string[]}).enum)) {
    return (resolutionProp as {enum: string[]}).enum.filter(
      (r): r is string => typeof r === "string",
    );
  }
  return [];
}

/**
 * Extract reference images support
 * @param {Object} schema - Schema object with properties
 * @return {Object} Object with supports flag and maxCount
 */
function extractReferenceImagesSupport(schema: {
  properties?: Record<string, unknown>;
}): {supports: boolean; maxCount?: number} {
  const refImagesProp = schema.properties?.reference_images;
  if (!refImagesProp) {
    return {supports: false};
  }

  // Check if it's an array type
  const isArray = (refImagesProp as {type?: string}).type === "array";
  if (!isArray) {
    return {supports: false};
  }

  // Check for maxItems constraint
  const maxItems = (refImagesProp as {maxItems?: number}).maxItems;
  const description =
    (refImagesProp as {description?: string}).description || "";

  // Try to extract max count from description (e.g., "1-4 images")
  let maxCount = maxItems;
  if (!maxCount && description) {
    const match = description.match(/(\d+)[-\s]+(\d+)\s+images?/i);
    if (match) {
      maxCount = parseInt(match[2], 10);
    }
  }

  return {
    supports: true,
    maxCount: maxCount || undefined,
  };
}

/**
 * Update seedance-1-lite model in Firestore
 * @return {Promise<void>} Promise that resolves when update is complete
 */
async function updateSeedanceModel(): Promise<void> {
  console.log("🔄 Updating seedance-1-lite model in Firestore...\n");

  // Read normalized schema
  const normalizedPath = path.join(
    __dirname,
    "..",
    "normalized_models_schema.json",
  );
  const normalizedModels = JSON.parse(
    fs.readFileSync(normalizedPath, "utf-8"),
  ) as Array<{
    id: string;
    name: string;
    replicate_name: string;
    input_schema: {
      properties?: Record<string, unknown>;
      required?: string[];
    };
    pricing: {
      variants: Array<{price_per_second: number}>;
    };
  }>;

  const model = normalizedModels.find((m) => m.id === "seedance-1-lite");
  if (!model) {
    console.error("❌ Model seedance-1-lite not found in normalized schema");
    process.exit(1);
  }

  const schema = model.input_schema;
  const required = schema.required || [];

  // Extract duration (min: 2, max: 12, default: 5)
  const durationProp = schema.properties?.duration as {
    minimum?: number;
    maximum?: number;
    default?: number;
  } | undefined;

  const minDuration = durationProp?.minimum || 2;
  const maxDuration = durationProp?.maximum || 12;
  const defaultDuration = durationProp?.default || 5;
  const durations = extractDurationsFromRange(
    minDuration,
    maxDuration,
  );

  // Extract aspect ratios
  const aspectRatioProp = schema.properties?.aspect_ratio as {
    enum?: string[];
    default?: string;
  } | undefined;
  const aspectRatios = aspectRatioProp?.enum || ["16:9", "9:16"];

  // Extract resolutions
  const resolutions = extractResolutions(schema);

  // Extract feature support
  const supportsImage = !!schema.properties?.image;
  const requiresImage = supportsImage && required.includes("image");
  const supportsLastFrame = !!schema.properties?.last_frame_image;
  const requiresLastFrame =
    supportsLastFrame && required.includes("last_frame_image");

  const refImagesSupport = extractReferenceImagesSupport(schema);

  // Get pricing (use 720p as base since it's the default)
  const basePrice =
    model.pricing.variants.find(
      (v) => (v as {variant?: string}).variant === "720p",
    )?.price_per_second ||
    model.pricing.variants[0]?.price_per_second ||
    0.036;
  const pricePerSec = Math.max(1, Math.round(basePrice * 100)); // 1:100 ratio

  console.log(`📋 Model: ${model.name}`);
  console.log(`   📐 Aspect Ratios: ${aspectRatios.join(", ")}`);
  console.log(
    `   ⏱️  Durations: ${durations.join(", ")}s ` +
      `(default: ${defaultDuration}s)`,
  );
  console.log(`   📺 Resolutions: ${resolutions.join(", ")}`);
  console.log(`   🖼️  Supports Image Input: ${supportsImage}`);
  console.log(`   🖼️  Supports Last Frame: ${supportsLastFrame}`);
  const refImagesText = refImagesSupport.maxCount ?
    `(max: ${refImagesSupport.maxCount})` :
    "";
  console.log(
    `   🖼️  Supports Reference Images: ${refImagesSupport.supports} ` +
      `${refImagesText}`,
  );
  console.log(
    `   💰 Base price: $${basePrice}/sec → ${pricePerSec} credits/sec\n`,
  );

  // Build Firestore document
  const firestoreDoc: Record<string, unknown> = {
    id: model.id,
    name: model.name,
    description: `${model.name} - High-quality video generation model`,
    price_per_sec: pricePerSec,
    default_duration: defaultDuration,
    duration_options: durations,
    aspect_ratios: aspectRatios,
    resolutions: resolutions, // Add resolutions
    supports_first_frame: supportsImage,
    requires_first_frame: requiresImage,
    supports_last_frame: supportsLastFrame,
    requires_last_frame: requiresLastFrame,
    supports_reference_images: refImagesSupport.supports,
    max_reference_images: refImagesSupport.maxCount || null,
    supports_audio: false,
    preview_url: "",
    replicate_name: model.replicate_name,
    trending: true, // Mark as trending
    updated_at: admin.firestore.FieldValue.serverTimestamp(),
  };

  // Update Firestore - use set without merge to ensure all fields are updated
  const collection = firestore.collection("video_features");
  const docRef = collection.doc(model.id);

  // First check if document exists to preserve any existing fields we want to keep
  const existingDoc = await docRef.get();
  if (existingDoc.exists) {
    const existingData = existingDoc.data();
    // Preserve some fields that might exist
    if (existingData?.index !== undefined) {
      firestoreDoc.index = existingData.index;
    }
    if (existingData?.created_at) {
      firestoreDoc.created_at = existingData.created_at;
    }
  }

  // Use set to completely replace the document with our new data
  await docRef.set(firestoreDoc);

  console.log(`✅ Successfully updated ${model.id} in Firestore!`);
  console.log("   Collection: video_features");
  console.log(`   Document ID: ${model.id}\n`);

  // Verify the update
  const doc = await collection.doc(model.id).get();
  if (doc.exists) {
    const data = doc.data();
    console.log("📊 Verification:");
    console.log(`   Aspect Ratios: ${JSON.stringify(data?.aspect_ratios)}`);
    console.log(`   Durations: ${JSON.stringify(data?.duration_options)}`);
    console.log(`   Resolutions: ${JSON.stringify(data?.resolutions)}`);
    console.log(
      `   Supports Reference Images: ${data?.supports_reference_images}`,
    );
    console.log(
      `   Max Reference Images: ${data?.max_reference_images}`,
    );
  }

  process.exit(0);
}

updateSeedanceModel().catch((error) => {
  console.error("❌ Error:", error);
  process.exit(1);
});

