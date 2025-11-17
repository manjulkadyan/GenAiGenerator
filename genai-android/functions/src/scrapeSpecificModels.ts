/**
 * Scrape specific Replicate models (curated list)
 * Uses API-first approach for better pricing extraction
 *
 * Usage:
 * npm run seed:specific
 */

import * as fs from "fs";
import * as path from "path";

// Import types and helper functions from the main scraper
type ModelInfo = {
  name: string;
  url: string;
  description?: string;
  owner?: string;
  modelName?: string;
  schema?: {
    input?: Record<string, unknown>;
    output?: unknown;
  };
  pricing?: {
    per_second?: number;
    per_minute?: number;
    hardware?: string;
    estimated_cost?: string;
  };
  aspectRatios?: string[];
  durations?: number[];
  parameters?: {
    supportsFirstFrame?: boolean;
    supportsLastFrame?: boolean;
    supportsReferenceImages?: boolean;
    maxReferenceImages?: number;
    supportsAudio?: boolean;
    [key: string]: unknown;
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
    apiReferencePath?: string;
    metadataPath?: string;
  };
};

// List of specific models to scrape
const SPECIFIC_MODELS = [
  "openai/sora-2-pro", // sora 2 pro
  "openai/sora-2", // sora 2
  "google/veo-3.1", // google veo 3.1
  "google/veo-3.1-fast", // google veo 3.1 fast
  "google/veo-3", // google veo 3
  "wan-video/wan-2.5-t2v-fast", // wan 2.5 t2v fast (wan-2.5-t2v-hd doesn't exist)
  "bytedance/seedance-1-lite", // seedance lite
  "wan-video/wan-2.5-i2v", // wan 2.5 - i2v
  "google/veo-3-fast", // veo-3-fast
  "minimax/hailuo-2.3-fast", // hailuo-02 fast
  "bytedance/seedance-1-pro", // seedance pro 1080p
  "minimax/hailuo-02", // hailuo-02 pro
  "kwaivgi/kling-v2.1-master", // kling 2.1 Master
  "wan-video/wan-2.2-t2v-fast", // wan spicy cheap (likely)
  "pixverse/pixverse-v5", // pixVerse v5
  "kwaivgi/kling-v2.5-turbo-pro", // kling 2.5
  "runwayml/gen4-image-turbo", // runway gen-4 image turbo
  "lightricks/ltx-2-fast", // LTX 2 Fast
  "leonardoai/motion-2.0", // Leonardo Motion 2.0
  "lightricks/ltx-2-pro", // LTX 2 Pro
  "character-ai/ovi-i2v", // Ovi I2V
  "luma/ray-2-720p", // luma Ray-2 720p
];

/**
 * Fetch with rate limit handling and exponential backoff
 */
async function fetchWithRetry(
  url: string,
  options: RequestInit,
  maxRetries = 3,
): Promise<Response | null> {
  let lastError: Error | null = null;

  for (let attempt = 0; attempt < maxRetries; attempt++) {
    try {
      const response = await fetch(url, options);

      if (response.status === 429) {
        const retryAfter = response.headers.get("Retry-After");
        const waitTime = retryAfter ?
          parseInt(retryAfter, 10) * 1000 :
          Math.min(1000 * Math.pow(2, attempt), 10000);

        console.warn(`      ⚠️  Rate limited. Waiting ${waitTime}ms before retry...`);
        await new Promise((resolve) => setTimeout(resolve, waitTime));
        continue;
      }

      return response;
    } catch (error) {
      lastError = error instanceof Error ? error : new Error(String(error));
      if (attempt < maxRetries - 1) {
        const waitTime = Math.min(1000 * Math.pow(2, attempt), 5000);
        await new Promise((resolve) => setTimeout(resolve, waitTime));
      }
    }
  }

  if (lastError) {
    console.warn(`      ⚠️  Failed after ${maxRetries} attempts: ${lastError.message}`);
  }
  return null;
}

/**
 * Save raw data to file
 */
function saveRawData(
  modelName: string,
  dataType: "html" | "schema" | "api-reference" | "metadata",
  content: string,
): string {
  const outputDir = path.join(__dirname, "..", "raw-data");

  if (!fs.existsSync(outputDir)) {
    fs.mkdirSync(outputDir, {recursive: true});
  }

  const sanitizedName = modelName.replace(/\//g, "_").replace(/[^a-zA-Z0-9_-]/g, "_");
  const extension = dataType === "html" ? "html" : "json";
  const filename = `${sanitizedName}_${dataType}.${extension}`;
  const filePath = path.join(outputDir, filename);

  fs.writeFileSync(filePath, content, "utf-8");

  return filePath;
}

/**
 * Fetch model metadata from API endpoint
 */
async function fetchModelMetadata(
  owner: string,
  modelName: string,
): Promise<{metadata?: unknown; rawPath?: string}> {
  try {
    const apiUrl = `https://replicate.com/api/models/${owner}/${modelName}`;
    const response = await fetchWithRetry(apiUrl, {
      headers: {
        "accept": "application/json",
        "accept-language": "en-US,en;q=0.9",
        "user-agent": "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36",
      },
    });

    if (response?.ok) {
      const rawText = await response.text();
      const rawPath = saveRawData(`${owner}_${modelName}`, "metadata", rawText);
      const metadata = JSON.parse(rawText);
      return {metadata, rawPath};
    }
  } catch (error) {
    // Silently fail
  }
  return {};
}

/**
 * Extract pricing from model metadata API response
 */
function extractPricingFromMetadata(metadata: unknown): ModelInfo["pricing"] | undefined {
  try {
    const data = metadata as {
      latest_version?: {
        pricing?: {
          per_second?: number;
          per_minute?: number;
          hardware?: string;
          [key: string]: unknown;
        };
      };
      pricing?: {
        per_second?: number;
        per_minute?: number;
        hardware?: string;
        [key: string]: unknown;
      };
      [key: string]: unknown;
    };

    if (data.latest_version?.pricing) {
      const pricing = data.latest_version.pricing;
      return {
        per_second: typeof pricing.per_second === "number" ? pricing.per_second : undefined,
        per_minute: typeof pricing.per_minute === "number" ? pricing.per_minute : undefined,
        hardware: typeof pricing.hardware === "string" ? pricing.hardware : undefined,
      };
    }

    if (data.pricing) {
      const pricing = data.pricing;
      return {
        per_second: typeof pricing.per_second === "number" ? pricing.per_second : undefined,
        per_minute: typeof pricing.per_minute === "number" ? pricing.per_minute : undefined,
        hardware: typeof pricing.hardware === "string" ? pricing.hardware : undefined,
      };
    }
  } catch (error) {
    // Ignore extraction errors
  }
  return undefined;
}

/**
 * Fetch and save raw schema from model's API schema endpoint
 */
async function fetchModelSchema(
  modelUrl: string,
  modelName: string,
): Promise<{schema?: ModelInfo["schema"]; rawPath?: string}> {
  try {
    const schemaUrl = `${modelUrl}/api/schema`;
    const response = await fetchWithRetry(schemaUrl, {
      headers: {
        "accept": "application/json",
        "user-agent": "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36",
      },
    });

    if (response?.ok) {
      const rawText = await response.text();
      const rawPath = saveRawData(modelName, "schema", rawText);

      // Try to parse as JSON first
      let schema: ModelInfo["schema"] | undefined;
      try {
        schema = JSON.parse(rawText) as ModelInfo["schema"];
      } catch {
        // If JSON parsing fails, try to extract from HTML
        // Look for JSON schema in script tags or __NEXT_DATA__
        const jsonMatch = rawText.match(/<script[^>]*id="__NEXT_DATA__"[^>]*>([\s\S]*?)<\/script>/);
        if (jsonMatch) {
          try {
            const nextData = JSON.parse(jsonMatch[1]);
            // Navigate to the schema in the Next.js data structure
            if (nextData?.props?.pageProps?.model?.latest_version?.openapi_schema) {
              schema = nextData.props.pageProps.model.latest_version.openapi_schema as ModelInfo["schema"];
            } else if (nextData?.props?.pageProps?.version?.openapi_schema) {
              schema = nextData.props.pageProps.version.openapi_schema as ModelInfo["schema"];
            } else if (nextData?.props?.pageProps?.example?._extras?.ran_on?.dereferenced_openapi_schema) {
              // Try example data structure (common in HTML responses)
              schema = nextData.props.pageProps.example._extras.ran_on.dereferenced_openapi_schema as ModelInfo["schema"];
            }
          } catch {
            // Try react component props
            const reactMatch = rawText.match(/<script[^>]*type="application\/json"[^>]*>([\s\S]*?)<\/script>/);
            if (reactMatch) {
              try {
                const reactData = JSON.parse(reactMatch[1]);
                if (reactData?.version?._extras?.dereferenced_openapi_schema) {
                  schema = reactData.version._extras.dereferenced_openapi_schema as ModelInfo["schema"];
                }
              } catch {}
            }
            // Try window variable
            const schemaMatch = rawText.match(/window\.__REPLICATE_MODEL_SCHEMA__\s*=\s*({[\s\S]*?});/);
            if (schemaMatch) {
              try {
                schema = JSON.parse(schemaMatch[1]) as ModelInfo["schema"];
              } catch {}
            }
          }
        }
      }

      if (schema) {
        return {schema, rawPath};
      }
    }
  } catch (error) {
    // Silently fail
  }
  return {};
}

/**
 * Extract aspect ratios and durations from schema
 */
function extractAspectRatiosAndDurations(schema: ModelInfo["schema"]): {
  aspectRatios?: string[];
  durations?: number[];
} {
  const result: { aspectRatios?: string[]; durations?: number[] } = {};

  if (!schema?.input) {
    return result;
  }

  const input = schema.input as Record<string, unknown>;

  if (input.aspect_ratio || input.aspectRatio) {
    const aspectRatio = input.aspect_ratio || input.aspectRatio;
    if (typeof aspectRatio === "string") {
      result.aspectRatios = [aspectRatio];
    } else if (Array.isArray(aspectRatio)) {
      result.aspectRatios = aspectRatio.filter((r): r is string => typeof r === "string");
    } else if (typeof aspectRatio === "object" && aspectRatio !== null) {
      const enumObj = aspectRatio as { enum?: string[]; default?: string };
      if (enumObj.enum) {
        result.aspectRatios = enumObj.enum;
      } else if (enumObj.default) {
        result.aspectRatios = [enumObj.default];
      }
    }
  }

  if (input.duration || input.duration_seconds || input.durationSeconds) {
    const duration = input.duration || input.duration_seconds || input.durationSeconds;
    if (typeof duration === "number") {
      result.durations = [duration];
    } else if (Array.isArray(duration)) {
      result.durations = duration.filter((d): d is number => typeof d === "number");
    } else if (typeof duration === "object" && duration !== null) {
      const enumObj = duration as { enum?: number[]; default?: number };
      if (enumObj.enum) {
        result.durations = enumObj.enum;
      } else if (enumObj.default) {
        result.durations = [enumObj.default];
      }
    }
  }

  return result;
}

/**
 * Extract parameters from schema
 */
function extractParameters(schema: ModelInfo["schema"]): ModelInfo["parameters"] {
  const params: ModelInfo["parameters"] = {};

  if (!schema?.input) {
    return params;
  }

  const input = schema.input as Record<string, unknown>;

  params.supportsFirstFrame = !!(
    input.first_frame ||
    input.firstFrame ||
    input.first_frame_url ||
    input.firstFrameUrl ||
    input.first_frame_image
  );

  params.supportsLastFrame = !!(
    input.last_frame ||
    input.lastFrame ||
    input.last_frame_url ||
    input.lastFrameUrl
  );

  params.supportsReferenceImages = !!(
    input.reference_image ||
    input.reference_images ||
    input.referenceImage ||
    input.referenceImages ||
    input.image ||
    input.images
  );

  if (input.reference_images || input.referenceImages) {
    const refImages = input.reference_images || input.referenceImages;
    if (Array.isArray(refImages)) {
      params.maxReferenceImages = refImages.length;
    }
  }

  params.supportsAudio = !!(
    input.audio ||
    input.enable_audio ||
    input.enableAudio ||
    input.with_audio ||
    input.withAudio
  );

  return params;
}

/**
 * Extract example video URLs from HTML
 */
function extractExampleVideoUrls(html: string): string[] {
  const videoUrls: string[] = [];

  try {
    // Method 1: Extract from __NEXT_DATA__ (most reliable)
    const nextDataMatch = html.match(/window\.__NEXT_DATA__\s*=\s*({[\s\S]*?});/);
    if (nextDataMatch) {
      try {
        const nextData = JSON.parse(nextDataMatch[1]);
        const modelData = nextData?.props?.pageProps?.model ||
                         nextData?.props?.pageProps?.data?.model ||
                         nextData?.query?.data?.model;

        // Look for examples or outputs in the model data
        if (modelData?.examples) {
          for (const example of modelData.examples) {
            if (example.output && Array.isArray(example.output)) {
              for (const output of example.output) {
                if (typeof output === "string" && (output.startsWith("http") || output.startsWith("https"))) {
                  videoUrls.push(output);
                } else if (output?.url && typeof output.url === "string") {
                  videoUrls.push(output.url);
                } else if (output?.video && typeof output.video === "string") {
                  videoUrls.push(output.video);
                }
              }
            } else if (example.output && typeof example.output === "string") {
              if (example.output.startsWith("http")) {
                videoUrls.push(example.output);
              }
            } else if (example.video && typeof example.video === "string") {
              videoUrls.push(example.video);
            } else if (example.url && typeof example.url === "string" && example.url.match(/\.(mp4|webm|mov)/i)) {
              videoUrls.push(example.url);
            }
          }
        }

        // Look for latest_version outputs
        if (modelData?.latest_version?.outputs) {
          for (const output of modelData.latest_version.outputs) {
            if (typeof output === "string" && output.startsWith("http")) {
              videoUrls.push(output);
            } else if (output?.url) {
              videoUrls.push(output.url);
            }
          }
        }
      } catch {
        // Ignore JSON parse errors
      }
    }

    // Method 2: Extract from video elements in HTML
    const videoElementRegex = /<video[^>]*src=["']([^"']+)["'][^>]*>/gi;
    let match;
    while ((match = videoElementRegex.exec(html)) !== null) {
      const url = match[1];
      if (url && url.startsWith("http") && !videoUrls.includes(url)) {
        videoUrls.push(url);
      }
    }

    // Method 3: Extract from source elements inside video tags
    const sourceRegex = /<source[^>]*src=["']([^"']+)["'][^>]*>/gi;
    while ((match = sourceRegex.exec(html)) !== null) {
      const url = match[1];
      if (url && url.startsWith("http") && !videoUrls.includes(url)) {
        videoUrls.push(url);
      }
    }

    // Method 4: Look for Replicate CDN URLs (replicate.delivery)
    const replicateUrlRegex = /https?:\/\/replicate\.delivery\/[^\s"']+/gi;
    while ((match = replicateUrlRegex.exec(html)) !== null) {
      const url = match[0];
      if (url && !videoUrls.includes(url)) {
        videoUrls.push(url);
      }
    }

    // Method 5: Look for common video URL patterns in data attributes
    const dataUrlRegex = /data-(?:video|src|url)=["']([^"']+)["']/gi;
    while ((match = dataUrlRegex.exec(html)) !== null) {
      const url = match[1];
      if (url && url.startsWith("http") && url.match(/\.(mp4|webm|mov)/i) && !videoUrls.includes(url)) {
        videoUrls.push(url);
      }
    }
  } catch (error) {
    // Ignore extraction errors
  }

  // Remove duplicates and filter to valid video URLs
  return Array.from(new Set(videoUrls))
    .filter((url) => url && url.startsWith("http"))
    .slice(0, 10); // Limit to first 10 videos
}

/**
 * Extract pricing from HTML (fallback)
 */
function extractPricing(html: string): ModelInfo["pricing"] | undefined {
  try {
    const pricing: ModelInfo["pricing"] = {};

    // Look for window.__NEXT_DATA__
    const nextDataMatch = html.match(/window\.__NEXT_DATA__\s*=\s*({[\s\S]*?});/);
    if (nextDataMatch) {
      try {
        const nextData = JSON.parse(nextDataMatch[1]);
        const modelData = nextData?.props?.pageProps?.model ||
                         nextData?.props?.pageProps?.data?.model ||
                         nextData?.query?.data?.model;
        if (modelData?.pricing) {
          if (typeof modelData.pricing === "number") {
            pricing.per_second = modelData.pricing;
          } else if (typeof modelData.pricing === "object") {
            pricing.per_second = modelData.pricing.per_second || modelData.pricing.perSecond;
            pricing.per_minute = modelData.pricing.per_minute || modelData.pricing.perMinute;
            pricing.hardware = modelData.pricing.hardware;
          }
        }
      } catch {
        // Ignore
      }
    }

    // Regex patterns
    const pricingPatterns = [
      /\$(\d+\.?\d*)\s*(?:per\s*)?(?:second|sec)/i,
      /(\d+\.?\d*)\s*cents?\s*(?:per\s*)?(?:second|sec)/i,
    ];

    for (const pattern of pricingPatterns) {
      const match = html.match(pattern);
      if (match && match[1]) {
        const price = parseFloat(match[1]);
        if (!isNaN(price) && price > 0) {
          if (pattern.source.includes("cent")) {
            pricing.per_second = price / 100;
          } else {
            pricing.per_second = price;
          }
          break;
        }
      }
    }

    if (pricing.per_second || pricing.per_minute || pricing.hardware) {
      return pricing;
    }
  } catch (error) {
    // Ignore
  }
  return undefined;
}

/**
 * Fetch details for a specific model
 */
async function fetchModelDetails(modelName: string, delayBetweenRequests = 100): Promise<ModelInfo> {
  const [owner, name] = modelName.split("/");
  const url = `https://replicate.com/${modelName}`;

  const model: ModelInfo = {
    name: modelName,
    url,
    owner,
    modelName: name,
    rawData: {},
  };

  // Ensure rawData is always initialized
  if (!model.rawData) {
    model.rawData = {};
  }

  try {
    // Step 1: Fetch metadata API (BEST SOURCE for pricing)
    console.log(`\n📊 [${modelName}] Fetching metadata API...`);
    const {metadata, rawPath: metadataPath} = await fetchModelMetadata(owner, name);
    if (metadata && metadataPath) {
      model.rawData!.metadataPath = metadataPath;
      console.log(`      💾 Saved metadata to: ${metadataPath}`);

      const pricingFromMetadata = extractPricingFromMetadata(metadata);
      if (pricingFromMetadata) {
        model.pricing = pricingFromMetadata;
        console.log(`      ✅ Pricing: $${pricingFromMetadata.per_second}/sec`);
      }
    }

    await new Promise((resolve) => setTimeout(resolve, delayBetweenRequests));

    // Step 2: Fetch schema
    console.log("      📋 Fetching schema...");
    const {schema, rawPath: schemaPath} = await fetchModelSchema(url, modelName);
    if (schema && schemaPath) {
      model.schema = schema;
      model.rawData!.schemaPath = schemaPath;

      model.parameters = extractParameters(schema);
      const {aspectRatios, durations} = extractAspectRatiosAndDurations(schema);
      if (aspectRatios) model.aspectRatios = aspectRatios;
      if (durations) model.durations = durations;

      console.log("      ✅ Schema extracted");
    }

    await new Promise((resolve) => setTimeout(resolve, delayBetweenRequests));

    // Step 3: Always fetch HTML to extract example videos (and fallback for pricing)
    console.log(`      🌐 Fetching HTML (for example videos${!model.pricing ? " and pricing" : ""})...`);
    const response = await fetchWithRetry(url, {
      headers: {
        "accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8",
        "accept-language": "en-US,en;q=0.9",
        "user-agent": "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36",
      },
    });

    if (!response) {
      console.warn("      ⚠️  Failed to fetch HTML (response is null)");
    } else if (!response.ok) {
      console.warn(`      ⚠️  HTML fetch failed with status: ${response.status} ${response.statusText}`);
    } else {
      const html = await response.text();
      const htmlPath = saveRawData(modelName, "html", html);
      model.rawData!.htmlPath = htmlPath;
      console.log(`      💾 Saved HTML to: ${htmlPath}`);

      // Extract example video URLs (always do this)
      const exampleVideos = extractExampleVideoUrls(html);
      if (exampleVideos.length > 0) {
        model.exampleVideoUrls = exampleVideos;
        console.log(`      🎬 Found ${exampleVideos.length} example video(s)`);
      } else {
        console.log("      ⚠️  No example videos found in HTML");
      }

      // Extract pricing from HTML only if not already found from metadata
      if (!model.pricing) {
        const pricing = extractPricing(html);
        if (pricing) {
          model.pricing = pricing;
          console.log(`      ✅ Pricing from HTML: $${pricing.per_second}/sec`);
        } else {
          console.log("      ⚠️  No pricing found in HTML");
        }
      }
    }
  } catch (error) {
    console.warn(`      ⚠️  Error: ${error instanceof Error ? error.message : String(error)}`);
  }

  return model;
}

/**
 * Save models to file
 */
function saveToFile(models: ModelInfo[], filename: string): void {
  const outputDir = path.join(__dirname, "..");
  const filePath = path.join(outputDir, filename);

  let content = "# Specific Text-to-Video Models from Replicate\n";
  content += `# Total models: ${models.length}\n`;
  content += `# Generated: ${new Date().toISOString()}\n\n`;
  content += "---\n\n";

  models.forEach((model, index) => {
    content += `${index + 1}. **${model.name}**\n`;
    content += `   URL: ${model.url}\n`;
    if (model.description) {
      content += `   Description: ${model.description}\n`;
    }
    if (model.aspectRatios && model.aspectRatios.length > 0) {
      content += `   Aspect Ratios: ${model.aspectRatios.join(", ")}\n`;
    }
    if (model.durations && model.durations.length > 0) {
      content += `   Durations: ${model.durations.join("s, ")}s\n`;
    }
    if (model.pricing) {
      content += "   Pricing: ";
      if (model.pricing.per_second) {
        content += `$${model.pricing.per_second}/sec`;
      }
      if (model.pricing.hardware) {
        content += ` (${model.pricing.hardware})`;
      }
      content += "\n";
    }
    if (model.parameters) {
      const params: string[] = [];
      if (model.parameters.supportsFirstFrame) params.push("First Frame");
      if (model.parameters.supportsLastFrame) params.push("Last Frame");
      if (model.parameters.supportsReferenceImages) {
        params.push(`Reference Images${model.parameters.maxReferenceImages ? ` (max ${model.parameters.maxReferenceImages})` : ""}`);
      }
      if (model.parameters.supportsAudio) params.push("Audio");
      if (params.length > 0) {
        content += `   Features: ${params.join(", ")}\n`;
      }
    }
    if (model.exampleVideoUrls && model.exampleVideoUrls.length > 0) {
      content += `   Example Videos: ${model.exampleVideoUrls.length} video(s)\n`;
      model.exampleVideoUrls.forEach((url, idx) => {
        content += `      ${idx + 1}. ${url}\n`;
      });
    }
    content += "\n";
  });

  const jsonPath = path.join(outputDir, filename.replace(".txt", ".json"));
  fs.writeFileSync(jsonPath, JSON.stringify(models, null, 2));
  fs.writeFileSync(filePath, content);

  console.log(`\n✅ Saved ${models.length} models to:`);
  console.log(`   📄 ${filePath}`);
  console.log(`   📄 ${jsonPath}`);
}

/**
 * Main scraping function for specific models
 */
async function scrapeSpecificModels() {
  console.log("🚀 Starting scrape of specific models...\n");
  console.log(`📋 Models to scrape: ${SPECIFIC_MODELS.length}\n`);

  const models: ModelInfo[] = [];
  const DELAY_BETWEEN_MODELS = 200; // ms

  for (let i = 0; i < SPECIFIC_MODELS.length; i++) {
    const modelName = SPECIFIC_MODELS[i];
    console.log(`[${i + 1}/${SPECIFIC_MODELS.length}] Processing: ${modelName}`);

    const model = await fetchModelDetails(modelName, 100);
    models.push(model);

    if (i < SPECIFIC_MODELS.length - 1) {
      await new Promise((resolve) => setTimeout(resolve, DELAY_BETWEEN_MODELS));
    }
  }

  saveToFile(models, "replicate-specific-models.txt");

  console.log(`\n🎉 Done! Scraped ${models.length} specific models.`);
}

// Run if called directly
if (require.main === module) {
  scrapeSpecificModels()
    .then(() => {
      process.exit(0);
    })
    .catch((error) => {
      console.error("💥 Fatal error:", error);
      process.exit(1);
    });
}

export {scrapeSpecificModels, fetchModelDetails};

