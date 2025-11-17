/**
 * Preview the 18 specific models that will be seeded to Firestore
 * Shows all extracted data without actually writing to Firestore
 *
 * Usage:
 * npm run preview:specific
 */

import * as fs from "fs";
import * as path from "path";
import fetch from "node-fetch";

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

/**
 * Extract ALL input parameters from schema comprehensively
 */
function extractAllInputParameters(schema: Record<string, unknown> | null): {
  aspectRatios?: string[];
  durations?: number[];
  supportsFirstFrame?: boolean;
  supportsLastFrame?: boolean;
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
  allInputParameters?: Record<string, unknown>;
} {
  const result: ReturnType<typeof extractAllInputParameters> = {
    allInputParameters: {},
  };

  if (!schema || typeof schema !== "object") {
    return result;
  }

  const input = (schema.input as Record<string, unknown>) || {};
  result.allInputParameters = input;

  // Extract aspect ratios
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
  const firstFrameKeys = [
    "first_frame", "firstFrame", "first_frame_url", "firstFrameUrl",
    "first_frame_image", "firstFrameImage", "start_frame", "startFrame",
  ];
  result.supportsFirstFrame = firstFrameKeys.some((key) => !!input[key]);

  const lastFrameKeys = [
    "last_frame", "lastFrame", "last_frame_url", "lastFrameUrl",
    "end_frame", "endFrame",
  ];
  result.supportsLastFrame = lastFrameKeys.some((key) => !!input[key]);

  const referenceImageKeys = [
    "reference_image", "reference_images", "referenceImage", "referenceImages",
    "image", "images", "input_image", "inputImage", "conditioning_image", "conditioningImage",
  ];
  result.supportsReferenceImages = referenceImageKeys.some((key) => !!input[key]);

  if (input.reference_images || input.referenceImages) {
    const refImages = input.reference_images || input.referenceImages;
    if (Array.isArray(refImages)) {
      result.maxReferenceImages = refImages.length;
    } else if (typeof refImages === "object" && refImages !== null) {
      const schemaObj = refImages as { maxItems?: number; maxLength?: number };
      result.maxReferenceImages = schemaObj.maxItems || schemaObj.maxLength;
    }
  }

  const audioKeys = [
    "audio", "enable_audio", "enableAudio", "with_audio", "withAudio",
    "audio_file", "audioFile", "sound", "enable_sound",
  ];
  result.supportsAudio = audioKeys.some((key) => !!input[key]);

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
 * Convert pricing from dollars to credits
 */
function pricingToCredits(perSecond: number | undefined): number {
  if (!perSecond) return 10; // Default
  return Math.max(1, Math.round(perSecond * 10000));
}

/**
 * Generate a friendly model name from replicate name
 */
function generateModelName(replicateName: string): string {
  const parts = replicateName.split("/");
  const modelPart = parts[parts.length - 1];

  return modelPart
    .split("-")
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(" ")
    .replace(/V(\d)/g, "v$1")
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
  return {
    aspectRatios: ["16:9", "9:16"],
    durations: [3, 5, 8],
    defaultDuration: 5,
  };
}

/**
 * Preview models that will be seeded
 */
async function previewSpecificModels() {
  const jsonPath = path.join(__dirname, "..", "replicate-specific-models.json");

  if (!fs.existsSync(jsonPath)) {
    console.error(`❌ File not found: ${jsonPath}`);
    console.error("   Run 'npm run seed:specific' first to scrape the models");
    process.exit(1);
  }

  const scrapedModels = JSON.parse(
    fs.readFileSync(jsonPath, "utf-8")
  ) as ScrapedModel[];

  console.log(`📦 Found ${scrapedModels.length} models to preview\n`);
  console.log("=" .repeat(80));
  console.log("PREVIEW: Models that will be seeded to Firestore");
  console.log("=" .repeat(80));
  console.log();

  const modelsToSeed = [];

  for (let i = 0; i < scrapedModels.length; i++) {
    const scraped = scrapedModels[i];
    const replicateName = scraped.name;
    const modelId = generateModelId(replicateName);
    const modelName = generateModelName(replicateName);
    const [owner, name] = replicateName.split("/");

    console.log(`\n[${i + 1}/${scrapedModels.length}] ${modelName} (${replicateName})`);
    console.log("-".repeat(80));

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

    // Fetch schema directly from API
    console.log("  📋 Fetching schema from API...");
    let schemaData: ReturnType<typeof extractAllInputParameters> = {};
    const schema = await fetchSchemaFromAPI(owner, name);
    if (schema) {
      schemaData = extractAllInputParameters(schema);
      console.log(`  ✅ Schema extracted with ${Object.keys(schemaData.allInputParameters || {}).length} parameters`);
    } else {
      console.log("  ⚠️  Could not fetch schema from API");
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

    // Build Firestore document
    const firestoreDoc: Record<string, unknown> = {
      id: modelId,
      name: modelName,
      description: description,
      price_per_sec: pricingToCredits(scraped.pricing?.per_second),
      default_duration: defaultDuration,
      duration_options: durations,
      aspect_ratios: aspectRatios,
      requires_first_frame: scraped.parameters?.supportsFirstFrame ||
                            schemaData.supportsFirstFrame ||
                            false,
      requires_last_frame: scraped.parameters?.supportsLastFrame ||
                           schemaData.supportsLastFrame ||
                           false,
      preview_url: scraped.coverImageUrl || "",
      replicate_name: replicateName,
      index: i,
      trending: i < 5,

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

      hardware: scraped.pricing?.hardware || null,
      run_count: scraped.runCount || null,
      tags: scraped.tags || [],
      github_url: scraped.githubUrl || null,
      paper_url: scraped.paperUrl || null,
      license_url: scraped.licenseUrl || null,
      cover_image_url: scraped.coverImageUrl || null,
    };

    // Remove null values for display
    const displayDoc: Record<string, unknown> = {};
    Object.keys(firestoreDoc).forEach((key) => {
      if (firestoreDoc[key] !== null && firestoreDoc[key] !== undefined) {
        displayDoc[key] = firestoreDoc[key];
      }
    });

    // Display key information
    console.log(`  📝 ID: ${modelId}`);
    console.log(`  💰 Price: ${displayDoc.price_per_sec} credits/sec`);
    console.log(`  ⏱️  Durations: ${(displayDoc.duration_options as number[])?.join(", ")}s`);
    console.log(`  📐 Aspect Ratios: ${(displayDoc.aspect_ratios as string[])?.join(", ")}`);
    console.log(`  🎬 First Frame: ${displayDoc.requires_first_frame ? "✅" : "❌"}`);
    console.log(`  🎬 Last Frame: ${displayDoc.requires_last_frame ? "✅" : "❌"}`);
    console.log(`  🖼️  Reference Images: ${displayDoc.supports_reference_images ? "✅" : "❌"}`);
    if (displayDoc.max_reference_images) {
      console.log(`     Max: ${displayDoc.max_reference_images}`);
    }
    console.log(`  🔊 Audio: ${displayDoc.supports_audio ? "✅" : "❌"}`);
    console.log(`  🖼️  Image Input: ${displayDoc.supports_image_input ? "✅" : "❌"}`);
    console.log(`  🎥 Video Input: ${displayDoc.supports_video_input ? "✅" : "❌"}`);
    console.log(`  ❌ Negative Prompt: ${displayDoc.supports_negative_prompt ? "✅" : "❌"}`);
    console.log(`  🎲 Seed: ${displayDoc.supports_seed ? "✅" : "❌"}`);
    console.log(`  📊 Guidance Scale: ${displayDoc.supports_guidance_scale ? "✅" : "❌"}`);
    console.log(`  🎯 Motion Bucket: ${displayDoc.supports_motion_bucket ? "✅" : "❌"}`);
    console.log(`  🎞️  FPS: ${displayDoc.supports_fps ? "✅" : "❌"}`);
    console.log(`  📹 Num Frames: ${displayDoc.supports_num_frames ? "✅" : "❌"}`);

    if (schemaData.allInputParameters) {
      const paramCount = Object.keys(schemaData.allInputParameters).length;
      console.log(`  📋 Total Input Parameters: ${paramCount}`);
    }

    modelsToSeed.push(firestoreDoc);
  }

  // Save preview to JSON file
  const previewPath = path.join(__dirname, "..", "replicate-specific-models-preview.json");
  fs.writeFileSync(previewPath, JSON.stringify(modelsToSeed, null, 2));

  console.log("\n" + "=".repeat(80));
  console.log(`✅ Preview complete! ${modelsToSeed.length} models prepared`);
  console.log(`📄 Preview saved to: ${previewPath}`);
  console.log("=".repeat(80));
  console.log("\nTo actually seed to Firestore, run:");
  console.log("  npm run seed:specific:firestore");
}

// Run if called directly
if (require.main === module) {
  previewSpecificModels()
    .then(() => {
      process.exit(0);
    })
    .catch((error) => {
      console.error("💥 Error previewing models:", error);
      process.exit(1);
    });
}

export {previewSpecificModels};

