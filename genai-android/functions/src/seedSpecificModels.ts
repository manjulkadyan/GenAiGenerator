/**
 * Seed Firestore with the 18 specific models from replicate-specific-models.json
 *
 * Usage:
 * npm run seed:specific:firestore
 */

import * as admin from "firebase-admin";
import * as fs from "fs";
import * as path from "path";
import fetch from "node-fetch";

// Initialize Firebase Admin (if not already initialized)
if (!admin.apps.length) {
  // Check if using Firebase Emulator
  const useEmulator = process.env.FIRESTORE_EMULATOR_HOST || process.env.FIREBASE_EMULATOR_HUB;

  if (useEmulator) {
    // Use Firebase Emulator
    const emulatorHost = process.env.FIRESTORE_EMULATOR_HOST || "localhost:8080";
    const [host, port] = emulatorHost.split(":");
    admin.initializeApp();
    admin.firestore().settings({
      host: host,
      port: parseInt(port, 10),
      ssl: false,
    });
    console.log(`✅ Using Firebase Emulator at ${emulatorHost}`);
  } else {
    // Try to use service account key if provided
    const serviceAccountPath = process.env.GOOGLE_APPLICATION_CREDENTIALS;
    if (serviceAccountPath && fs.existsSync(serviceAccountPath)) {
      try {
        const serviceAccount = JSON.parse(fs.readFileSync(serviceAccountPath, "utf-8"));
        admin.initializeApp({
          credential: admin.credential.cert(serviceAccount),
        });
        console.log("✅ Using service account credentials");
      } catch (error) {
        console.error("❌ Failed to load service account:", error);
        process.exit(1);
      }
    } else {
      // Try default initialization (works with Firebase CLI login or in Firebase Functions environment)
      try {
        admin.initializeApp();
        console.log("✅ Using Firebase credentials (Firebase CLI login)");
      } catch (error) {
        console.error("\n❌ Firebase authentication failed!");
        console.error("Error:", (error as Error).message);
        console.error("\nPlease authenticate using one of these methods:\n");
        console.error("Option 1: Firebase CLI (recommended):");
        console.error("  firebase login");
        console.error("  firebase use YOUR_PROJECT_ID\n");
        console.error("Option 2: Use Firebase Emulator (for local testing):");
        console.error("  firebase emulators:start --only firestore");
        console.error("  export FIRESTORE_EMULATOR_HOST=localhost:8080\n");
        console.error("Option 3: Use service account key:");
        console.error("  1. Download from Firebase Console → Project Settings → Service Accounts");
        console.error("  2. export GOOGLE_APPLICATION_CREDENTIALS=/path/to/service-account.json\n");
        process.exit(1);
      }
    }
  }
}

const firestore = admin.firestore();

type ScrapedModel = {
  name: string;
  url: string;
  owner?: string;
  modelName?: string;
  description?: string;
  pricing?: {
    per_second?: number;
    per_minute?: number;
    hardware?: string;
  };
  aspectRatios?: string[];
  durations?: number[];
  parameters?: {
    supportsFirstFrame?: boolean;
    supportsLastFrame?: boolean;
    supportsReferenceImages?: boolean;
    maxReferenceImages?: number;
    supportsAudio?: boolean;
  };
  runCount?: number;
  coverImageUrl?: string;
  tags?: string[];
  githubUrl?: string;
  paperUrl?: string;
  licenseUrl?: string;
  exampleVideoUrls?: string[]; // URLs of example videos from the model page
  rawData?: {
    htmlPath?: string;
    schemaPath?: string;
    metadataPath?: string;
  };
};

/**
 * Extract description from HTML file
 */
function extractDescriptionFromHtml(htmlPath: string): string | null {
  try {
    if (!fs.existsSync(htmlPath)) {
      return null;
    }
    const html = fs.readFileSync(htmlPath, "utf-8");

    // Try to extract from meta description
    const metaDescMatch = html.match(/<meta\s+name=["']description["']\s+content=["']([^"']+)["']/i);
    if (metaDescMatch) {
      return metaDescMatch[1];
    }

    // Try to extract from JSON-LD
    const jsonLdMatch = html.match(/<script\s+type=["']application\/ld\+json["']>([\s\S]*?)<\/script>/i);
    if (jsonLdMatch) {
      try {
        const jsonLd = JSON.parse(jsonLdMatch[1]);
        if (jsonLd.description) {
          return jsonLd.description;
        }
      } catch {
        // Ignore
      }
    }

    // Try to extract from __NEXT_DATA__
    const nextDataMatch = html.match(/window\.__NEXT_DATA__\s*=\s*({[\s\S]*?});/);
    if (nextDataMatch) {
      try {
        const nextData = JSON.parse(nextDataMatch[1]);
        const description = nextData?.props?.pageProps?.model?.description ||
                           nextData?.props?.pageProps?.data?.model?.description ||
                           nextData?.query?.data?.model?.description;
        if (description) {
          return description;
        }
      } catch {
        // Ignore
      }
    }
  } catch (error) {
    // Ignore errors
  }
  return null;
}

/**
 * Fetch schema JSON directly from API (not HTML)
 */
async function fetchSchemaFromAPI(
  owner: string,
  modelName: string,
): Promise<Record<string, unknown> | null> {
  try {
    const schemaUrl = `https://replicate.com/${owner}/${modelName}/api/schema`;
    const response = await fetch(schemaUrl, {
      headers: {
        "accept": "application/json",
        "user-agent": "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36",
      },
    });

    if (response.ok) {
      const contentType = response.headers.get("content-type");
      if (contentType?.includes("application/json")) {
        return await response.json() as Record<string, unknown>;
      }
    }
  } catch (error) {
    // Ignore errors
  }
  return null;
}

import {extractAllSchemaParameters, SchemaParameter} from "./extractAllSchemaParameters";

/**
 * Extract ALL input parameters from schema comprehensively
 */
function extractAllInputParameters(schema: Record<string, unknown> | null): {
  aspectRatios?: string[];
  durations?: number[];
  supportsFirstFrame?: boolean;
  requiresFirstFrame?: boolean; // True if field is required (not optional)
  supportsLastFrame?: boolean;
  requiresLastFrame?: boolean; // True if field is required (not optional)
  supportsReferenceImages?: boolean;
  maxReferenceImages?: number;
  supportsAudio?: boolean;
  supportsImageInput?: boolean;
  supportsVideoInput?: boolean;
  supportsNegativePrompt?: boolean;
  supportsSeed?: boolean;
  supportsGuidanceScale?: boolean;
  supportsMotionBucket?: boolean;
  supportsFps?: boolean;
  supportsNumFrames?: boolean;
  allInputParameters?: Record<string, unknown>; // Store ALL parameters for future use
  allSchemaParameters?: SchemaParameter[]; // ALL extracted parameters with metadata
  schemaMetadata?: {
    requiredFields: string[];
    categorized: {
      text: SchemaParameter[];
      numeric: SchemaParameter[];
      boolean: SchemaParameter[];
      enum: SchemaParameter[];
      file: SchemaParameter[];
    };
  };
} {
  const result: ReturnType<typeof extractAllInputParameters> = {
    allInputParameters: {},
  };

  if (!schema || typeof schema !== "object") {
    return result;
  }

  // Extract comprehensive schema information FIRST
  const extractedSchema = extractAllSchemaParameters(schema);
  result.allSchemaParameters = extractedSchema.parameters;
  result.schemaMetadata = {
    requiredFields: extractedSchema.requiredFields,
    categorized: extractedSchema.categorized,
  };

  // Build a flat object of all parameters for backward compatibility
  // Use extracted parameters to build the input object
  const input: Record<string, unknown> = {};
  if (extractedSchema.parameters && extractedSchema.parameters.length > 0) {
    // Build input from extracted parameters
    for (const param of extractedSchema.parameters) {
      input[param.name] = param.default ?? null;
    }
  } else {
    // Fallback to schema.input if extraction didn't work
    const schemaInput = (schema.input as Record<string, unknown>) || {};
    Object.assign(input, schemaInput);

    // Also try to get from components.schemas.Input
    const getNested = (obj: unknown, path: string[]): unknown => {
      let current: unknown = obj;
      for (const key of path) {
        if (current && typeof current === "object" && key in current) {
          current = (current as Record<string, unknown>)[key];
        } else {
          return undefined;
        }
      }
      return current;
    };

    const componentsSchemasInput = getNested(schema, ["components", "schemas", "Input"]);
    if (componentsSchemasInput && typeof componentsSchemasInput === "object") {
      const inputProps = (componentsSchemasInput as Record<string, unknown>).properties;
      if (inputProps && typeof inputProps === "object") {
        Object.assign(input, inputProps as Record<string, unknown>);
      }
    }
  }

  result.allInputParameters = input;

  // Extract aspect ratios - check both input object and extracted parameters
  const aspectRatioKeys = ["aspect_ratio", "aspectRatio", "aspect", "ratio"];
  for (const key of aspectRatioKeys) {
    if (input[key]) {
      const aspectRatio = input[key];
      if (typeof aspectRatio === "string") {
        result.aspectRatios = [aspectRatio];
        break;
      } else if (Array.isArray(aspectRatio)) {
        result.aspectRatios = aspectRatio.filter((r): r is string => typeof r === "string");
        break;
      } else if (typeof aspectRatio === "object" && aspectRatio !== null) {
        const enumObj = aspectRatio as { enum?: string[]; default?: string; oneOf?: Array<{const?: string}> };
        if (enumObj.enum) {
          result.aspectRatios = enumObj.enum;
          break;
        } else if (enumObj.oneOf) {
          result.aspectRatios = enumObj.oneOf
            .map((item) => item.const)
            .filter((r): r is string => typeof r === "string");
          break;
        } else if (enumObj.default) {
          result.aspectRatios = [enumObj.default];
          break;
        }
      }
    }
  }

  // Extract durations
  const durationKeys = ["duration", "duration_seconds", "durationSeconds", "video_length", "length"];
  for (const key of durationKeys) {
    if (input[key]) {
      const duration = input[key];
      if (typeof duration === "number") {
        result.durations = [duration];
        break;
      } else if (Array.isArray(duration)) {
        result.durations = duration.filter((d): d is number => typeof d === "number");
        break;
      } else if (typeof duration === "object" && duration !== null) {
        const enumObj = duration as { enum?: number[]; default?: number; minimum?: number; maximum?: number };
        if (enumObj.enum) {
          result.durations = enumObj.enum;
          break;
        } else if (enumObj.default) {
          result.durations = [enumObj.default];
          break;
        } else if (enumObj.minimum !== undefined && enumObj.maximum !== undefined) {
          // Generate common durations in range
          const min = enumObj.minimum;
          const max = enumObj.maximum;
          result.durations = [];
          for (let d = min; d <= max; d += Math.max(1, Math.floor((max - min) / 3))) {
            result.durations.push(d);
          }
          break;
        }
      }
    }
  }

  // Extract ALL feature flags comprehensively
  // Use extracted schema parameters to check for features (more reliable)
  const requiredFields = extractedSchema.requiredFields;

  // Check for first frame - use extracted parameters first, then fallback to input
  const firstFrameKeys = [
    "first_frame", "firstFrame", "first_frame_url", "firstFrameUrl",
    "first_frame_image", "firstFrameImage", "start_frame", "startFrame",
  ];

  // Check in extracted parameters first (case-insensitive)
  const firstFrameParam = extractedSchema.parameters.find((p) => {
    const paramNameLower = p.name.toLowerCase();
    return firstFrameKeys.some((key) => paramNameLower === key.toLowerCase());
  });

  if (firstFrameParam) {
    result.supportsFirstFrame = true;
    result.requiresFirstFrame = firstFrameParam.required && !firstFrameParam.nullable;
  } else {
    // Fallback to checking input object
    const firstFrameKey = firstFrameKeys.find((key) => !!input[key]);
    if (firstFrameKey) {
      result.supportsFirstFrame = true;
      const firstFrameProp = input[firstFrameKey] as { nullable?: boolean } | undefined;
      result.requiresFirstFrame = requiredFields.includes(firstFrameKey) && !firstFrameProp?.nullable;
    } else {
      result.supportsFirstFrame = false;
      result.requiresFirstFrame = false;
    }
  }

  // Check for last frame - use extracted parameters first
  const lastFrameKeys = [
    "last_frame", "lastFrame", "last_frame_url", "lastFrameUrl",
    "last_frame_image", "lastFrameImage", "end_frame", "endFrame",
  ];

  const lastFrameParam = extractedSchema.parameters.find((p) => {
    const paramNameLower = p.name.toLowerCase();
    return lastFrameKeys.some((key) => paramNameLower === key.toLowerCase());
  });

  if (lastFrameParam) {
    result.supportsLastFrame = true;
    result.requiresLastFrame = lastFrameParam.required && !lastFrameParam.nullable;
  } else {
    // Fallback to checking input object
    const lastFrameKey = lastFrameKeys.find((key) => !!input[key]);
    if (lastFrameKey) {
      result.supportsLastFrame = true;
      const lastFrameProp = input[lastFrameKey] as { nullable?: boolean } | undefined;
      result.requiresLastFrame = requiredFields.includes(lastFrameKey) && !lastFrameProp?.nullable;
    } else {
      result.supportsLastFrame = false;
      result.requiresLastFrame = false;
    }
  }

  // Check for reference images
  const referenceImageKeys = [
    "reference_image", "reference_images", "referenceImage", "referenceImages",
    "image", "images", "input_image", "inputImage", "conditioning_image", "conditioningImage",
  ];

  result.supportsReferenceImages = extractedSchema.parameters.some((p) =>
    referenceImageKeys.some((key) => p.name.toLowerCase() === key.toLowerCase())
  ) || referenceImageKeys.some((key) => !!input[key]);

  if (input.reference_images || input.referenceImages) {
    const refImages = input.reference_images || input.referenceImages;
    if (Array.isArray(refImages)) {
      result.maxReferenceImages = refImages.length;
    } else if (typeof refImages === "object" && refImages !== null) {
      const schemaObj = refImages as { maxItems?: number; maxLength?: number };
      result.maxReferenceImages = schemaObj.maxItems || schemaObj.maxLength;
    }
  }

  // Check for audio - use extracted parameters first
  const audioKeys = [
    "audio", "enable_audio", "enableAudio", "with_audio", "withAudio",
    "audio_file", "audioFile", "sound", "enable_sound",
  ];

  result.supportsAudio = extractedSchema.parameters.some((p) =>
    audioKeys.some((key) => p.name.toLowerCase() === key.toLowerCase())
  ) || audioKeys.some((key) => !!input[key]);

  const imageInputKeys = [
    "image", "images", "input_image", "inputImage", "image_url", "imageUrl",
    "image_file", "imageFile", "img", "photo",
  ];
  result.supportsImageInput = imageInputKeys.some((key) => !!input[key]);

  const videoInputKeys = [
    "video", "input_video", "inputVideo", "video_url", "videoUrl",
    "video_file", "videoFile",
  ];
  result.supportsVideoInput = videoInputKeys.some((key) => !!input[key]);

  result.supportsNegativePrompt = !!(
    input.negative_prompt || input.negativePrompt || input.negative_prompts || input.negativePrompts
  );

  result.supportsSeed = !!(
    input.seed || input.random_seed || input.randomSeed
  );

  result.supportsGuidanceScale = !!(
    input.guidance_scale || input.guidanceScale || input.cfg_scale || input.cfgScale
  );

  result.supportsMotionBucket = !!(
    input.motion_bucket || input.motionBucket || input.motion || input.motion_score || input.motionScore
  );

  result.supportsFps = !!(
    input.fps || input.frame_rate || input.frameRate || input.frames_per_second
  );

  result.supportsNumFrames = !!(
    input.num_frames || input.numFrames || input.frames || input.frame_count || input.frameCount
  );

  return result;
}

/**
 * Convert pricing from dollars to credits (multiply by 1000 for cents, then by 10 for credits)
 * Example: $0.0001/sec = 0.0001 * 1000 * 10 = 1 credit/sec
 */
function pricingToCredits(perSecond: number | undefined): number {
  if (!perSecond) return 10; // Default
  // Convert dollars to credits: $0.0001 = 1 credit
  return Math.max(1, Math.round(perSecond * 10000));
}

/**
 * Generate a friendly model name from replicate name
 */
function generateModelName(replicateName: string): string {
  const parts = replicateName.split("/");
  const modelPart = parts[parts.length - 1];

  // Convert kebab-case to Title Case
  return modelPart
    .split("-")
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(" ")
    .replace(/V(\d)/g, "v$1") // Keep version numbers lowercase
    .replace(/Pro/g, "Pro")
    .replace(/Turbo/g, "Turbo")
    .replace(/Hd/g, "HD")
    .replace(/I2v/g, "I2V")
    .replace(/T2v/g, "T2V");
}

/**
 * Generate model ID from replicate name
 */
function generateModelId(replicateName: string): string {
  return replicateName.replace(/\//g, "-").replace(/\./g, "-");
}

/**
 * Get default values for missing fields
 */
function getDefaults(replicateName: string): {
  aspectRatios: string[];
  durations: number[];
  defaultDuration: number;
} {
  // Default based on model type
  if (replicateName.includes("veo")) {
    return {
      aspectRatios: ["16:9", "9:16"],
      durations: [4, 6, 8],
      defaultDuration: 6,
    };
  }
  if (replicateName.includes("kling")) {
    return {
      aspectRatios: ["16:9", "9:16", "1:1"],
      durations: [3, 5, 10],
      defaultDuration: 5,
    };
  }
  if (replicateName.includes("sora")) {
    return {
      aspectRatios: ["16:9", "9:16", "1:1"],
      durations: [5, 10],
      defaultDuration: 5,
    };
  }
  // Generic defaults
  return {
    aspectRatios: ["16:9", "9:16"],
    durations: [3, 5, 8],
    defaultDuration: 5,
  };
}

/**
 * Seed models from scraped data
 */
async function seedSpecificModels() {
  const jsonPath = path.join(__dirname, "..", "replicate-specific-models.json");

  if (!fs.existsSync(jsonPath)) {
    console.error(`❌ File not found: ${jsonPath}`);
    console.error("   Run 'npm run seed:specific' first to scrape the models");
    process.exit(1);
  }

  // Check if new pricing file exists (with 20x multiplier)
  const newPricingPath = path.join(__dirname, "..", "replicate-specific-models-new-pricing.json");
  let useNewPricing = false;
  let newPricingModels: Array<{id: string; price_per_sec: number; [key: string]: unknown}> = [];

  if (fs.existsSync(newPricingPath)) {
    console.log("💰 Found new pricing file (20x multiplier)");
    console.log("   Using new pricing for all models\n");
    newPricingModels = JSON.parse(fs.readFileSync(newPricingPath, "utf-8"));
    useNewPricing = true;
  }

  const scrapedModels = JSON.parse(
    fs.readFileSync(jsonPath, "utf-8")
  ) as ScrapedModel[];

  console.log(`📦 Found ${scrapedModels.length} models to seed\n`);

  const modelsToSeed = [];

  for (let i = 0; i < scrapedModels.length; i++) {
    const scraped = scrapedModels[i];
    const replicateName = scraped.name;
    const modelId = generateModelId(replicateName);
    const modelName = generateModelName(replicateName);
    const [owner, name] = replicateName.split("/");

    console.log(`[${i + 1}/${scrapedModels.length}] Processing: ${replicateName}`);

    // Rate limiting: delay between models
    if (i > 0) {
      await new Promise((resolve) => setTimeout(resolve, 200));
    }

    // Extract description from HTML if available
    let description = scraped.description || "";
    if (!description && scraped.rawData?.htmlPath) {
      const htmlPath = scraped.rawData.htmlPath;
      description = extractDescriptionFromHtml(htmlPath) || "";
    }
    if (!description) {
      description = `${modelName} - High-quality text-to-video generation model`;
    }

    // Fetch schema directly from API (most reliable)
    console.log("      📋 Fetching schema from API...");
    let schemaData: ReturnType<typeof extractAllInputParameters> = {};
    const schema = await fetchSchemaFromAPI(owner, name);
    if (schema) {
      schemaData = extractAllInputParameters(schema);
      const paramCount = schemaData.allSchemaParameters?.length || Object.keys(schemaData.allInputParameters || {}).length;
      console.log(`      ✅ Schema extracted with ${paramCount} parameters`);
      if (schemaData.allSchemaParameters && schemaData.allSchemaParameters.length > 0) {
        const paramNames = schemaData.allSchemaParameters.map((p) => p.name).slice(0, 10);
        console.log(`         📝 Parameters: ${paramNames.join(", ")}${schemaData.allSchemaParameters.length > 10 ? "..." : ""}`);
      }
    } else if (scraped.rawData?.schemaPath) {
      // Fallback: try to parse saved schema file (might be HTML, so try JSON first)
      try {
        const schemaContent = fs.readFileSync(scraped.rawData.schemaPath, "utf-8");
        // Check if it's JSON
        if (schemaContent.trim().startsWith("{")) {
          const parsedSchema = JSON.parse(schemaContent);
          schemaData = extractAllInputParameters(parsedSchema);
        } else {
          // Try to extract from HTML (same logic as fetchModelSchema)
          const jsonMatch = schemaContent.match(/<script[^>]*id="__NEXT_DATA__"[^>]*>([\s\S]*?)<\/script>/);
          if (jsonMatch) {
            try {
              const nextData = JSON.parse(jsonMatch[1]);
              let htmlSchema: Record<string, unknown> | null = null;
              if (nextData?.props?.pageProps?.model?.latest_version?.openapi_schema) {
                htmlSchema = nextData.props.pageProps.model.latest_version.openapi_schema as Record<string, unknown>;
              } else if (nextData?.props?.pageProps?.version?.openapi_schema) {
                htmlSchema = nextData.props.pageProps.version.openapi_schema as Record<string, unknown>;
              } else if (nextData?.props?.pageProps?.example?._extras?.ran_on?.dereferenced_openapi_schema) {
                htmlSchema = nextData.props.pageProps.example._extras.ran_on.dereferenced_openapi_schema as Record<string, unknown>;
              }
              if (htmlSchema) {
                schemaData = extractAllInputParameters(htmlSchema);
                const paramCount = schemaData.allSchemaParameters?.length || Object.keys(schemaData.allInputParameters || {}).length;
                console.log(`      ✅ Schema extracted from HTML with ${paramCount} parameters`);
              }
            } catch {
              // Try react component props
              const reactMatch = schemaContent.match(/<script[^>]*type="application\/json"[^>]*>([\s\S]*?)<\/script>/);
              if (reactMatch) {
                try {
                  const reactData = JSON.parse(reactMatch[1]);
                  if (reactData?.version?._extras?.dereferenced_openapi_schema) {
                    const htmlSchema = reactData.version._extras.dereferenced_openapi_schema as Record<string, unknown>;
                    schemaData = extractAllInputParameters(htmlSchema);
                    const paramCount = schemaData.allSchemaParameters?.length || Object.keys(schemaData.allInputParameters || {}).length;
                    console.log(`      ✅ Schema extracted from React props with ${paramCount} parameters`);
                  }
                } catch {}
              }
            }
          }
        }
      } catch {
        // Ignore - might be HTML or invalid
      }
    }

    // Use defaults if not found
    const defaults = getDefaults(replicateName);
    const aspectRatios = scraped.aspectRatios ||
                         schemaData.aspectRatios ||
                         defaults.aspectRatios;
    const durations = scraped.durations ||
                      schemaData.durations ||
                      defaults.durations;
    const defaultDuration = durations[0] || defaults.defaultDuration;

    // Get pricing - use new pricing if available, otherwise calculate from scraped data
    let finalPrice: number;
    if (useNewPricing) {
      const newPricingModel = newPricingModels.find((m) => m.id === modelId);
      if (newPricingModel) {
        finalPrice = newPricingModel.price_per_sec;
        const multiplier = (newPricingModel as {price_multiplier?: number}).price_multiplier || 20;
        console.log(`      💰 Using new pricing: ${finalPrice} credits/sec (${multiplier}x multiplier)`);
      } else {
        finalPrice = pricingToCredits(scraped.pricing?.per_second);
        console.log(`      ⚠️  New pricing not found, using calculated: ${finalPrice} credits/sec`);
      }
    } else {
      finalPrice = pricingToCredits(scraped.pricing?.per_second);
    }

    // Build Firestore document
    const firestoreDoc: Record<string, unknown> = {
      id: modelId,
      name: modelName,
      description: description,
      price_per_sec: finalPrice,
      default_duration: defaultDuration,
      duration_options: durations,
      aspect_ratios: aspectRatios,
      // Normalized schema: distinguish between "supports" and "requires"
      supports_first_frame: schemaData.supportsFirstFrame ??
                            scraped.parameters?.supportsFirstFrame ??
                            false,
      requires_first_frame: schemaData.requiresFirstFrame ?? false, // Only true if field is actually required
      supports_last_frame: schemaData.supportsLastFrame ??
                           scraped.parameters?.supportsLastFrame ??
                           false,
      requires_last_frame: schemaData.requiresLastFrame ?? false, // Only true if field is actually required
      preview_url: scraped.coverImageUrl || "",
      replicate_name: replicateName,
      index: i,
      trending: i < 5, // First 5 are trending

      // Additional fields from schema
      supports_reference_images: scraped.parameters?.supportsReferenceImages ||
                                  schemaData.supportsReferenceImages ||
                                  false,
      max_reference_images: scraped.parameters?.maxReferenceImages ||
                           schemaData.maxReferenceImages ||
                           null,
      supports_audio: scraped.parameters?.supportsAudio ||
                     schemaData.supportsAudio ||
                     false,
      supports_image_input: schemaData.supportsImageInput || false,
      supports_video_input: schemaData.supportsVideoInput || false,
      supports_negative_prompt: schemaData.supportsNegativePrompt || false,
      supports_seed: schemaData.supportsSeed || false,
      supports_guidance_scale: schemaData.supportsGuidanceScale || false,
      supports_motion_bucket: schemaData.supportsMotionBucket || false,
      supports_fps: schemaData.supportsFps || false,
      supports_num_frames: schemaData.supportsNumFrames || false,

      // Store ALL schema parameters for dynamic UI rendering
      schema_parameters: schemaData.allSchemaParameters || [], // All parameters with full metadata
      schema_metadata: schemaData.schemaMetadata ? {
        required_fields: schemaData.schemaMetadata.requiredFields,
        categorized: {
          text: schemaData.schemaMetadata.categorized.text.map((p) => ({
            name: p.name,
            type: p.type,
            required: p.required,
            description: p.description,
            default: p.default,
          })),
          numeric: schemaData.schemaMetadata.categorized.numeric.map((p) => ({
            name: p.name,
            type: p.type,
            required: p.required,
            description: p.description,
            default: p.default,
            min: p.min,
            max: p.max,
          })),
          boolean: schemaData.schemaMetadata.categorized.boolean.map((p) => ({
            name: p.name,
            type: p.type,
            required: p.required,
            description: p.description,
            default: p.default,
          })),
          enum: schemaData.schemaMetadata.categorized.enum.map((p) => ({
            name: p.name,
            type: p.type,
            required: p.required,
            description: p.description,
            default: p.default,
            enum: p.enum,
          })),
          file: schemaData.schemaMetadata.categorized.file.map((p) => ({
            name: p.name,
            type: p.type,
            required: p.required,
            description: p.description,
            format: p.format,
          })),
        },
      } : null,

      // Store complete raw schema for future reference (as JSON string to avoid Firestore size limits)
      input_schema: schemaData.allInputParameters ?
        JSON.stringify(schemaData.allInputParameters) : null,
      hardware: scraped.pricing?.hardware || null,
      run_count: scraped.runCount || null,
      tags: scraped.tags || [],
      github_url: scraped.githubUrl || null,
      paper_url: scraped.paperUrl || null,
      license_url: scraped.licenseUrl || null,
      cover_image_url: scraped.coverImageUrl || null,
      example_video_urls: scraped.exampleVideoUrls && scraped.exampleVideoUrls.length > 0 ?
        scraped.exampleVideoUrls :
        null, // Array of example video URLs
    };

    // Remove null values
    Object.keys(firestoreDoc).forEach((key) => {
      if (firestoreDoc[key] === null) {
        delete firestoreDoc[key];
      }
      // Remove empty example_video_urls array
      if (key === "example_video_urls" && Array.isArray(firestoreDoc[key]) && (firestoreDoc[key] as unknown[]).length === 0) {
        delete firestoreDoc[key];
      }
    });

    modelsToSeed.push(firestoreDoc);
    console.log(`   ✅ Prepared: ${modelName}`);
  }

  console.log(`\n🔥 Seeding ${modelsToSeed.length} models to Firestore...\n`);

  // Seed to Firestore
  for (let i = 0; i < modelsToSeed.length; i++) {
    const model = modelsToSeed[i];
    try {
      await firestore.collection("video_features").doc(model.id as string).set(model);
      console.log(`✅ [${i + 1}/${modelsToSeed.length}] Seeded: ${model.name}`);
    } catch (error) {
      console.error(`❌ [${i + 1}/${modelsToSeed.length}] Failed to seed ${model.id}:`, error);
    }
  }

  console.log(`\n🎉 Done! Seeded ${modelsToSeed.length} models to video_features collection`);
}

// Run if called directly
if (require.main === module) {
  seedSpecificModels()
    .then(() => {
      console.log("\n✅ Seeding complete!");
      process.exit(0);
    })
    .catch((error) => {
      console.error("💥 Error seeding models:", error);
      process.exit(1);
    });
}

export {seedSpecificModels};

