import * as fs from "fs";
import * as path from "path";
import {initializeApp, cert, App} from "firebase-admin/app";
import {getFirestore} from "firebase-admin/firestore";
import {extractAllSchemaParameters} from "./extractAllSchemaParameters";

/**
 * Normalized model schema from normalized_models_schema.json
 */
interface NormalizedModel {
  id: string;
  name: string;
  replicate_name: string;
  url: string;
  schema_url: string;
  pricing_url: string;
  input_schema: {
    type: string;
    title: string;
    required?: string[];
    properties: Record<string, {
      type: string;
      title?: string;
      description?: string;
      enum?: unknown[];
      default?: unknown;
      nullable?: boolean;
      format?: string;
      minimum?: number;
      maximum?: number;
      [key: string]: unknown;
    }>;
  };
  output_schema: unknown;
  pricing: {
    variants: Array<{
      variant: string;
      variant_name: string | null;
      price_per_second: number;
      description: string;
      metric: string;
    }>;
    billing_config: unknown;
    raw_data: unknown;
  };
}

/**
 * Convert pricing from dollars to credits
 * Simple 1:100 ratio: $0.4/sec = 40 credits/sec
 * Formula: credits = dollars_per_second * 100
 */
function pricingToCredits(perSecond: number | undefined): number {
  if (!perSecond) return 10; // Default: 10 credits/sec
  // Convert dollars to credits: $0.4/sec = 0.4 * 100 = 40 credits/sec
  return Math.max(1, Math.round(perSecond * 100));
}

/**
 * Extract aspect ratios from schema
 */
function extractAspectRatios(schema: NormalizedModel["input_schema"]): string[] {
  const aspectRatioProp = schema.properties.aspect_ratio || schema.properties.aspectRatio;
  if (!aspectRatioProp) return ["16:9", "9:16"]; // Default

  if (Array.isArray(aspectRatioProp.enum)) {
    return aspectRatioProp.enum.filter((r): r is string => typeof r === "string");
  }
  if (typeof aspectRatioProp.default === "string") {
    return [aspectRatioProp.default];
  }
  return ["16:9", "9:16"]; // Default
}

/**
 * Extract durations from schema
 */
function extractDurations(schema: NormalizedModel["input_schema"]): number[] {
  const durationProp = schema.properties.duration ||
                       schema.properties.duration_seconds ||
                       schema.properties.seconds;
  if (!durationProp) return [5]; // Default

  if (Array.isArray(durationProp.enum)) {
    return durationProp.enum.filter((d): d is number => typeof d === "number");
  }
  if (typeof durationProp.default === "number") {
    return [durationProp.default];
  }
  if (typeof durationProp.minimum === "number" && typeof durationProp.maximum === "number") {
    const min = durationProp.minimum;
    const max = durationProp.maximum;
    const durations: number[] = [];
    for (let d = min; d <= max; d += Math.max(1, Math.floor((max - min) / 3))) {
      durations.push(d);
    }
    return durations.length > 0 ? durations : [min];
  }
  return [5]; // Default
}

/**
 * Extract feature support from schema properties
 */
function extractFeatureSupport(schema: NormalizedModel["input_schema"]): {
  supportsFirstFrame: boolean;
  requiresFirstFrame: boolean;
  supportsLastFrame: boolean;
  requiresLastFrame: boolean;
  supportsAudio: boolean;
} {
  const required = schema.required || [];
  const properties = schema.properties;

  // Check for first frame
  const firstFrameKeys = [
    "first_frame", "firstFrame", "first_frame_image", "firstFrameImage",
    "input_reference", "start_image", "startImage", "image",
  ];
  const firstFrameKey = firstFrameKeys.find((key) => properties[key]);
  const supportsFirstFrame = !!firstFrameKey;
  const requiresFirstFrame = firstFrameKey ? required.includes(firstFrameKey) : false;

  // Check for last frame
  const lastFrameKeys = [
    "last_frame", "lastFrame", "last_frame_image", "lastFrameImage",
    "end_image", "endImage",
  ];
  const lastFrameKey = lastFrameKeys.find((key) => properties[key]);
  const supportsLastFrame = !!lastFrameKey;
  const requiresLastFrame = lastFrameKey ? required.includes(lastFrameKey) : false;

  // Check for audio
  const audioKeys = [
    "generate_audio", "generateAudio", "audio", "enable_audio", "enableAudio",
  ];
  const supportsAudio = audioKeys.some((key) => !!properties[key]);

  return {
    supportsFirstFrame,
    requiresFirstFrame,
    supportsLastFrame,
    requiresLastFrame,
    supportsAudio,
  };
}

/**
 * Seed models from normalized schema
 */
async function seedNormalizedModels() {
  const normalizedPath = path.join(__dirname, "..", "normalized_models_schema.json");

  if (!fs.existsSync(normalizedPath)) {
    console.error(`❌ File not found: ${normalizedPath}`);
    process.exit(1);
  }

  const normalizedModels = JSON.parse(
    fs.readFileSync(normalizedPath, "utf-8")
  ) as NormalizedModel[];

  console.log(`📦 Found ${normalizedModels.length} models to seed\n`);

  // Initialize Firebase
  const serviceAccountPath = path.join(__dirname, "..", "service-account-key.json");
  if (!fs.existsSync(serviceAccountPath)) {
    console.error(`❌ Service account key not found: ${serviceAccountPath}`);
    process.exit(1);
  }

  const serviceAccount = JSON.parse(fs.readFileSync(serviceAccountPath, "utf-8"));
  let app: App;
  try {
    app = initializeApp({
      credential: cert(serviceAccount),
    });
  } catch (error) {
    // App might already be initialized
    app = initializeApp();
  }

  const firestore = getFirestore(app);
  // Enable ignoreUndefinedProperties to handle undefined values gracefully
  firestore.settings({ignoreUndefinedProperties: true});
  const collection = firestore.collection("video_features");

  const modelsToSeed = [];

  for (let i = 0; i < normalizedModels.length; i++) {
    const model = normalizedModels[i];
    console.log(`[${i + 1}/${normalizedModels.length}] Processing: ${model.replicate_name}`);

    const inputSchema = model.input_schema;

    // Extract schema parameters using the extraction function
    // The normalized schema format matches what extractAllSchemaParameters expects
    const extractedSchema = extractAllSchemaParameters({
      input: inputSchema,
      components: {
        schemas: {
          Input: inputSchema,
        },
      },
    } as unknown as Record<string, unknown>);

    // Extract basic info
    const aspectRatios = extractAspectRatios(inputSchema);
    const durations = extractDurations(inputSchema);
    const defaultDuration = durations[0] || 5;

    // Extract feature support
    const features = extractFeatureSupport(inputSchema);

    // Get pricing - use first variant as base price
    const basePrice = model.pricing.variants[0]?.price_per_second || 0.1;
    const pricePerSec = pricingToCredits(basePrice);

    console.log(`   💰 Base price: $${basePrice}/sec → ${pricePerSec} credits/sec (1:100 ratio)`);

    // Build Firestore document
    const firestoreDoc: Record<string, unknown> = {
      id: model.id,
      name: model.name,
      description: `${model.name} - High-quality video generation model`,
      price_per_sec: pricePerSec,
      default_duration: defaultDuration,
      duration_options: durations,
      aspect_ratios: aspectRatios,
      supports_first_frame: features.supportsFirstFrame,
      requires_first_frame: features.requiresFirstFrame,
      supports_last_frame: features.supportsLastFrame,
      requires_last_frame: features.requiresLastFrame,
      supports_audio: features.supportsAudio,
      preview_url: "",
      replicate_name: model.replicate_name,
      index: i,
      trending: i < 5, // First 5 are trending

      // Store ALL schema parameters for dynamic UI rendering
      // Remove undefined values to avoid Firestore errors
      schema_parameters: (extractedSchema.parameters || []).map((p) => {
        const param: Record<string, unknown> = {
          name: p.name,
          type: p.type,
          required: p.required,
          nullable: p.nullable,
        };
        if (p.description !== undefined) param.description = p.description;
        if (p.default !== undefined) param.default = p.default;
        if (p.enum !== undefined) param.enum = p.enum;
        if (p.min !== undefined) param.min = p.min;
        if (p.max !== undefined) param.max = p.max;
        if (p.format !== undefined) param.format = p.format;
        if (p.title !== undefined) param.title = p.title;
        return param;
      }),
      schema_metadata: extractedSchema.parameters.length > 0 ? {
        required_fields: extractedSchema.requiredFields,
        categorized: {
          text: extractedSchema.categorized.text.map((p) => {
            const param: Record<string, unknown> = {
              name: p.name,
              type: p.type,
              required: p.required,
            };
            if (p.description !== undefined) param.description = p.description;
            if (p.default !== undefined) param.default = p.default;
            return param;
          }),
          numeric: extractedSchema.categorized.numeric.map((p) => {
            const param: Record<string, unknown> = {
              name: p.name,
              type: p.type,
              required: p.required,
            };
            if (p.description !== undefined) param.description = p.description;
            if (p.default !== undefined) param.default = p.default;
            if (p.min !== undefined) param.min = p.min;
            if (p.max !== undefined) param.max = p.max;
            return param;
          }),
          boolean: extractedSchema.categorized.boolean.map((p) => {
            const param: Record<string, unknown> = {
              name: p.name,
              type: p.type,
              required: p.required,
            };
            if (p.description !== undefined) param.description = p.description;
            if (p.default !== undefined) param.default = p.default;
            return param;
          }),
          enum: extractedSchema.categorized.enum.map((p) => {
            const param: Record<string, unknown> = {
              name: p.name,
              type: p.type,
              required: p.required,
            };
            if (p.description !== undefined) param.description = p.description;
            if (p.default !== undefined) param.default = p.default;
            if (p.enum !== undefined) param.enum = p.enum;
            return param;
          }),
          file: extractedSchema.categorized.file.map((p) => {
            const param: Record<string, unknown> = {
              name: p.name,
              type: p.type,
              required: p.required,
            };
            if (p.description !== undefined) param.description = p.description;
            if (p.format !== undefined) param.format = p.format;
            return param;
          }),
        },
      } : null,

      // Store complete raw schema
      input_schema: JSON.stringify(inputSchema),
    };

    // Remove null and undefined values (Firestore doesn't allow undefined)
    Object.keys(firestoreDoc).forEach((key) => {
      if (firestoreDoc[key] === null || firestoreDoc[key] === undefined) {
        delete firestoreDoc[key];
      }
    });

    // Recursively remove undefined values from nested objects
    function removeUndefined(obj: unknown): unknown {
      if (obj === null || obj === undefined) {
        return null;
      }
      if (Array.isArray(obj)) {
        return obj.map(removeUndefined).filter((item) => item !== null && item !== undefined);
      }
      if (typeof obj === "object") {
        const cleaned: Record<string, unknown> = {};
        for (const [key, value] of Object.entries(obj)) {
          if (value !== undefined) {
            const cleanedValue = removeUndefined(value);
            if (cleanedValue !== null && cleanedValue !== undefined) {
              cleaned[key] = cleanedValue;
            }
          }
        }
        return cleaned;
      }
      return obj;
    }

    // Clean the entire document
    const cleanedDoc = removeUndefined(firestoreDoc) as Record<string, unknown>;

    modelsToSeed.push(cleanedDoc);
    console.log(`   ✅ Prepared: ${model.name}`);
    console.log(`      - Supports first frame: ${features.supportsFirstFrame}`);
    console.log(`      - Supports last frame: ${features.supportsLastFrame}`);
    console.log(`      - Supports audio: ${features.supportsAudio}`);
    console.log(`      - Parameters: ${extractedSchema.parameters.length}`);
  }

  console.log(`\n🔥 Seeding ${modelsToSeed.length} models to Firestore...\n`);

  // Seed to Firestore
  for (let i = 0; i < modelsToSeed.length; i++) {
    const model = modelsToSeed[i];
    try {
      await collection.doc(model.id as string).set(model);
      console.log(`[${i + 1}/${modelsToSeed.length}] ✅ Seeded: ${model.name}`);
    } catch (error) {
      console.error(`[${i + 1}/${modelsToSeed.length}] ❌ Failed to seed ${model.name}:`, error);
    }
  }

  console.log(`\n✅ Successfully seeded ${modelsToSeed.length} models!`);
}

// Run if called directly
if (require.main === module) {
  seedNormalizedModels()
    .then(() => {
      console.log("\n✨ Done!");
      process.exit(0);
    })
    .catch((error) => {
      console.error("\n❌ Error:", error);
      process.exit(1);
    });
}

export {seedNormalizedModels};

