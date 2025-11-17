/**
 * Scrape Replicate search page for text-to-video models
 * Saves all models to a text file (no Firestore)
 *
 * Usage:
 * npm run seed:scrape
 */

import * as fs from "fs";
import * as path from "path";

type ModelInfo = {
  name: string;
  url: string;
  description?: string;
  owner?: string;
  modelName?: string;
  // Additional details from model pages
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
  // Raw data storage paths
  rawData?: {
    htmlPath?: string;
    schemaPath?: string;
    apiReferencePath?: string;
  };
};

/**
 * Save raw API response to file
 */
function saveRawApiResponse(query: string, rawData: string): string {
  const outputDir = path.join(__dirname, "..", "raw-data");

  // Create directory if it doesn't exist
  if (!fs.existsSync(outputDir)) {
    fs.mkdirSync(outputDir, {recursive: true});
  }

  // Sanitize query for filename
  const sanitizedQuery = query.replace(/[^a-zA-Z0-9_-]/g, "_");
  const timestamp = new Date().toISOString().replace(/[:.]/g, "-");
  const filename = `search-api_${sanitizedQuery}_${timestamp}.json`;
  const filePath = path.join(outputDir, filename);

  fs.writeFileSync(filePath, rawData, "utf-8");

  return filePath;
}

/**
 * Fetch models from Replicate search API (the actual API the page uses)
 * Saves ALL raw data first, then parses it
 */
async function scrapeSearchPage(query: string, limit = 200): Promise<ModelInfo[]> {
  // This is the actual API endpoint the search page uses
  const apiUrl = `https://replicate-search-prototype-production.replicate.workers.dev/query?q=${encodeURIComponent(query)}&limit=${limit}`;

  console.log(`📡 Fetching from API: ${apiUrl}`);

  const response: Response = await fetch(apiUrl, {
    headers: {
      "accept": "*/*",
      "accept-language": "en-US,en;q=0.9",
      "dnt": "1",
      "origin": "https://replicate.com",
      "priority": "u=1, i",
      "sec-ch-ua": "\"Chromium\";v=\"142\", \"Google Chrome\";v=\"142\", \"Not_A Brand\";v=\"99\"",
      "sec-ch-ua-mobile": "?0",
      "sec-ch-ua-platform": "\"macOS\"",
      "sec-fetch-dest": "empty",
      "sec-fetch-mode": "cors",
      "sec-fetch-site": "cross-site",
      "user-agent": "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36",
    },
  });

  if (!response.ok) {
    throw new Error(`Failed to fetch search API: ${response.status} - ${await response.text()}`);
  }

  // Save raw response first
  const rawText = await response.text();
  const rawFilePath = saveRawApiResponse(query, rawText);
  console.log(`💾 Saved raw API response to: ${rawFilePath}`);

  // Then parse it from the saved text
  const data = JSON.parse(rawText) as {
    query?: string;
    results?: Array<{
      name?: string; // e.g., "google/veo-3"
      description?: string; // Long description
      model_description?: string; // Short description from model.description
      model?: {
        url?: string;
        owner?: string;
        name?: string;
        description?: string;
      };
      cover_image_url?: string;
      tags?: string[];
      run_count?: number;
      [key: string]: unknown;
    }>;
    [key: string]: unknown;
  };

  const models: ModelInfo[] = [];
  const seen = new Set<string>();

  if (data.results) {
    for (const item of data.results) {
      // Get model name - prefer top-level "name" field (e.g., "google/veo-3")
      let fullName = typeof item.name === "string" ? item.name : undefined;

      // Fallback to constructing from model object
      if (!fullName && item.model) {
        const owner = typeof item.model.owner === "string" ? item.model.owner : undefined;
        const modelName = typeof item.model.name === "string" ? item.model.name : undefined;
        if (owner && modelName) {
          fullName = `${owner}/${modelName}`;
        }
      }

      if (!fullName || seen.has(fullName)) {
        continue;
      }

      seen.add(fullName);

      // Get URL - prefer model.url, fallback to constructing from name
      let url = item.model?.url;
      if (!url || typeof url !== "string") {
        url = `https://replicate.com/${fullName}`;
      }

      // Get description - prefer top-level "description" (longer), fallback to model_description or model.description
      const description = typeof item.description === "string" ?
        item.description :
        (typeof item.model_description === "string" ?
          item.model_description :
          (item.model?.description && typeof item.model.description === "string" ?
            item.model.description :
            undefined));

      // Split owner and model name
      const parts = fullName.split("/");
      const owner = parts[0];
      const modelName = parts[1] || fullName;

      models.push({
        name: fullName,
        url,
        description,
        owner,
        modelName,
        runCount: typeof item.run_count === "number" ? item.run_count : undefined,
        coverImageUrl: typeof item.cover_image_url === "string" ? item.cover_image_url : undefined,
        tags: Array.isArray(item.tags) ? item.tags.filter((t): t is string => typeof t === "string") : undefined,
      });
    }
  }

  console.log(`✅ Found ${models.length} models from search API\n`);
  return models;
}

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

      // Handle rate limiting (429)
      if (response.status === 429) {
        const retryAfter = response.headers.get("Retry-After");
        const waitTime = retryAfter ?
          parseInt(retryAfter, 10) * 1000 :
          Math.min(1000 * Math.pow(2, attempt), 10000); // Exponential backoff, max 10s

        console.warn(`      ⚠️  Rate limited. Waiting ${waitTime}ms before retry...`);
        await new Promise((resolve) => setTimeout(resolve, waitTime));
        continue; // Retry
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

  // Create directory if it doesn't exist
  if (!fs.existsSync(outputDir)) {
    fs.mkdirSync(outputDir, {recursive: true});
  }

  // Sanitize model name for filename
  const sanitizedName = modelName.replace(/\//g, "_").replace(/[^a-zA-Z0-9_-]/g, "_");
  const extension = dataType === "html" ? "html" : "json";
  const filename = `${sanitizedName}_${dataType}.${extension}`;
  const filePath = path.join(outputDir, filename);

  fs.writeFileSync(filePath, content, "utf-8");

  return filePath;
}

/**
 * Fetch model metadata from API endpoint
 * This is the best source for structured data including pricing
 */
async function fetchModelMetadata(
  owner: string,
  modelName: string,
): Promise<{metadata?: unknown; rawPath?: string}> {
  try {
    // Use the API endpoint: /api/models/{owner}/{model}
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

      // Save raw JSON first
      const rawPath = saveRawData(`${owner}_${modelName}`, "metadata", rawText);

      // Then parse it
      const metadata = JSON.parse(rawText);
      return {metadata, rawPath};
    }
  } catch (error) {
    // Silently fail - API might not be available
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

    // Try latest_version.pricing first (most common)
    if (data.latest_version?.pricing) {
      const pricing = data.latest_version.pricing;
      return {
        per_second: typeof pricing.per_second === "number" ? pricing.per_second : undefined,
        per_minute: typeof pricing.per_minute === "number" ? pricing.per_minute : undefined,
        hardware: typeof pricing.hardware === "string" ? pricing.hardware : undefined,
      };
    }

    // Try top-level pricing
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

      // Save raw JSON first
      const rawPath = saveRawData(modelName, "schema", rawText);

      // Then parse it
      const schema = JSON.parse(rawText) as ModelInfo["schema"];
      return {schema, rawPath};
    }
  } catch (error) {
    // Silently fail - schema might not be available
  }
  return {};
}

/**
 * Fetch and save raw API reference
 */
async function fetchApiReference(
  modelUrl: string,
  modelName: string,
): Promise<string | undefined> {
  try {
    const apiRefUrl = `${modelUrl}/api/api-reference`;
    const response = await fetchWithRetry(apiRefUrl, {
      headers: {
        "accept": "*/*",
        "user-agent": "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36",
      },
    });

    if (response?.ok) {
      const rawText = await response.text();
      const rawPath = saveRawData(modelName, "api-reference", rawText);
      return rawPath;
    }
  } catch (error) {
    // Silently fail - API reference might not be available
  }
  return undefined;
}

/**
 * Extract pricing information from model page HTML
 * Looks for pricing in multiple places: JSON-LD, data attributes, pricing sections, etc.
 */
function extractPricing(html: string): ModelInfo["pricing"] | undefined {
  try {
    const pricing: ModelInfo["pricing"] = {};

    // Method 1: Look for JSON-LD structured data
    const jsonLdMatches = html.match(/<script[^>]*type=["']application\/ld\+json["'][^>]*>([\s\S]*?)<\/script>/gi);
    if (jsonLdMatches) {
      for (const match of jsonLdMatches) {
        try {
          const jsonContent = match.replace(/<script[^>]*>/, "").replace(/<\/script>/, "");
          const jsonData = JSON.parse(jsonContent);
          // Look for pricing in structured data
          if (jsonData.offers?.price || jsonData.price) {
            const price = jsonData.offers?.price || jsonData.price;
            if (typeof price === "number") {
              pricing.per_second = price;
            }
          }
        } catch {
          // Ignore JSON parse errors
        }
      }
    }

    // Method 2: Look for window.__NEXT_DATA__ or similar React/Next.js data
    const nextDataMatch = html.match(/window\.__NEXT_DATA__\s*=\s*({[\s\S]*?});/);
    if (nextDataMatch) {
      try {
        const nextData = JSON.parse(nextDataMatch[1]);
        // Navigate through Next.js data structure to find pricing
        // Pricing might be in props.pageProps.model or similar
        const modelData = nextData?.props?.pageProps?.model ||
                         nextData?.props?.pageProps?.data?.model ||
                         nextData?.query?.data?.model;
        if (modelData) {
          // Look for pricing fields
          if (modelData.pricing) {
            if (typeof modelData.pricing === "number") {
              pricing.per_second = modelData.pricing;
            } else if (typeof modelData.pricing === "object") {
              pricing.per_second = modelData.pricing.per_second || modelData.pricing.perSecond;
              pricing.per_minute = modelData.pricing.per_minute || modelData.pricing.perMinute;
              pricing.hardware = modelData.pricing.hardware;
            }
          }
          // Look for cost fields
          if (modelData.cost && typeof modelData.cost === "number") {
            pricing.per_second = modelData.cost;
          }
          // Look for hardware
          if (modelData.hardware && typeof modelData.hardware === "string") {
            pricing.hardware = modelData.hardware;
          }
        }
      } catch {
        // Ignore JSON parse errors
      }
    }

    // Method 3: Look for pricing in data attributes
    const dataPricingMatch = html.match(/data-pricing=["']([^"']+)["']/i) ||
                            html.match(/data-cost=["']([^"']+)["']/i);
    if (dataPricingMatch) {
      const priceValue = parseFloat(dataPricingMatch[1]);
      if (!isNaN(priceValue)) {
        pricing.per_second = priceValue;
      }
    }

    // Method 4: Look for pricing section with regex patterns
    // Pattern: "$X.XX per second" or "$X/sec" or "X cents per second"
    const pricingPatterns = [
      /\$(\d+\.?\d*)\s*(?:per\s*)?(?:second|sec)/i,
      /(\d+\.?\d*)\s*cents?\s*(?:per\s*)?(?:second|sec)/i,
      /(\d+\.?\d*)\s*¢\s*(?:per\s*)?(?:second|sec)/i,
      /pricing[^>]*>[\s\S]*?\$(\d+\.?\d*)/i,
      /cost[^>]*>[\s\S]*?\$(\d+\.?\d*)/i,
    ];

    for (const pattern of pricingPatterns) {
      const match = html.match(pattern);
      if (match && match[1]) {
        const price = parseFloat(match[1]);
        if (!isNaN(price) && price > 0) {
          // If it's in cents, convert to dollars
          if (pattern.source.includes("cent") || pattern.source.includes("¢")) {
            pricing.per_second = price / 100;
          } else {
            pricing.per_second = price;
          }
          break;
        }
      }
    }

    // Method 5: Look for hardware information
    const hardwarePatterns = [
      /hardware[^>]*>([^<]+)/i,
      /data-hardware=["']([^"']+)["']/i,
      /GPU[^>]*>([^<]+)/i,
      /(A100|H100|T4|V100|RTX|GTX)[^<]*/i,
    ];

    for (const pattern of hardwarePatterns) {
      const match = html.match(pattern);
      if (match && match[1]) {
        pricing.hardware = match[1].trim();
        break;
      }
    }

    // Method 6: Look for pricing table or pricing section
    const pricingSectionMatch = html.match(/<[^>]*pricing[^>]*>([\s\S]{0,500})<\/[^>]*>/i);
    if (pricingSectionMatch) {
      const section = pricingSectionMatch[1];
      // Look for dollar amounts in the pricing section
      const dollarMatch = section.match(/\$(\d+\.?\d*)/);
      if (dollarMatch) {
        const price = parseFloat(dollarMatch[1]);
        if (!isNaN(price) && price > 0) {
          pricing.per_second = price;
        }
      }
    }

    // Return pricing if we found anything
    if (pricing.per_second || pricing.per_minute || pricing.hardware) {
      return pricing;
    }
  } catch (error) {
    // Ignore extraction errors
  }
  return undefined;
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

  // Check for first_frame, firstFrame, first_frame_url, etc.
  params.supportsFirstFrame = !!(
    input.first_frame ||
    input.firstFrame ||
    input.first_frame_url ||
    input.firstFrameUrl ||
    input.first_frame_image
  );

  // Check for last_frame, lastFrame, last_frame_url, etc.
  params.supportsLastFrame = !!(
    input.last_frame ||
    input.lastFrame ||
    input.last_frame_url ||
    input.lastFrameUrl
  );

  // Check for reference images
  params.supportsReferenceImages = !!(
    input.reference_image ||
    input.reference_images ||
    input.referenceImage ||
    input.referenceImages ||
    input.image ||
    input.images
  );

  // Extract max reference images if it's an array
  if (input.reference_images || input.referenceImages) {
    const refImages = input.reference_images || input.referenceImages;
    if (Array.isArray(refImages)) {
      params.maxReferenceImages = refImages.length;
    }
  }

  // Check for audio support
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

  // Extract aspect_ratio options
  if (input.aspect_ratio || input.aspectRatio) {
    const aspectRatio = input.aspect_ratio || input.aspectRatio;
    if (typeof aspectRatio === "string") {
      result.aspectRatios = [aspectRatio];
    } else if (Array.isArray(aspectRatio)) {
      result.aspectRatios = aspectRatio.filter((r): r is string => typeof r === "string");
    } else if (typeof aspectRatio === "object" && aspectRatio !== null) {
      // Might be an enum object
      const enumObj = aspectRatio as { enum?: string[]; default?: string };
      if (enumObj.enum) {
        result.aspectRatios = enumObj.enum;
      } else if (enumObj.default) {
        result.aspectRatios = [enumObj.default];
      }
    }
  }

  // Extract duration options
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
 * Fetch model details from individual model pages and API endpoints
 * Uses API-first approach: fetches structured JSON API, then falls back to HTML scraping
 * Saves ALL raw data first, then extracts specific fields
 */
async function fetchModelDetails(model: ModelInfo, delayBetweenRequests = 100): Promise<ModelInfo> {
  try {
    // Initialize raw data storage
    model.rawData = {};

    if (!model.owner || !model.modelName) {
      console.warn(`      ⚠️  Missing owner/modelName for ${model.name}, skipping API calls`);
      return model;
    }

    // Step 1: Fetch model metadata API (BEST SOURCE for pricing and structured data)
    console.log("      📊 Fetching model metadata API...");
    const {metadata, rawPath: metadataPath} = await fetchModelMetadata(model.owner, model.modelName);
    if (metadata && metadataPath) {
      console.log(`      💾 Saved metadata to: ${metadataPath}`);

      // Extract pricing from metadata (primary source)
      const pricingFromMetadata = extractPricingFromMetadata(metadata);
      if (pricingFromMetadata) {
        model.pricing = pricingFromMetadata;
        console.log(`      ✅ Found pricing in metadata: $${pricingFromMetadata.per_second}/sec`);
      }
    }

    // Small delay between requests
    if (delayBetweenRequests > 0) {
      await new Promise((resolve) => setTimeout(resolve, delayBetweenRequests));
    }

    // Step 2: Fetch and save raw schema JSON
    console.log("      📋 Fetching and saving raw schema...");
    const {schema, rawPath: schemaPath} = await fetchModelSchema(model.url, model.name);
    if (schema) {
      model.schema = schema;
      model.rawData.schemaPath = schemaPath;

      // Extract parameters from schema
      model.parameters = extractParameters(schema);

      // Extract aspect ratios and durations
      const {aspectRatios, durations} = extractAspectRatiosAndDurations(schema);
      if (aspectRatios) model.aspectRatios = aspectRatios;
      if (durations) model.durations = durations;
    }

    // Small delay between requests
    if (delayBetweenRequests > 0) {
      await new Promise((resolve) => setTimeout(resolve, delayBetweenRequests));
    }

    // Step 3: Fetch and save raw API reference
    console.log("      📚 Fetching and saving raw API reference...");
    const apiRefPath = await fetchApiReference(model.url, model.name);
    if (apiRefPath) {
      model.rawData.apiReferencePath = apiRefPath;
    }

    // Small delay between requests
    if (delayBetweenRequests > 0) {
      await new Promise((resolve) => setTimeout(resolve, delayBetweenRequests));
    }

    // Step 4: Fetch and save raw HTML page (FALLBACK for pricing if API didn't have it)
    console.log("      🌐 Fetching and saving raw HTML page...");
    const response = await fetchWithRetry(model.url, {
      headers: {
        "accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7",
        "accept-language": "en-US,en;q=0.9",
        "cache-control": "max-age=0",
        "dnt": "1",
        "priority": "u=0, i",
        "referer": `${model.url}/api`,
        "sec-ch-ua": "\"Chromium\";v=\"142\", \"Google Chrome\";v=\"142\", \"Not_A Brand\";v=\"99\"",
        "sec-ch-ua-mobile": "?0",
        "sec-ch-ua-platform": "\"macOS\"",
        "sec-fetch-dest": "document",
        "sec-fetch-mode": "navigate",
        "sec-fetch-site": "same-origin",
        "sec-fetch-user": "?1",
        "upgrade-insecure-requests": "1",
        "user-agent": "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36",
      },
    });

    if (response?.ok) {
      const html = await response.text();

      // Save raw HTML first
      const htmlPath = saveRawData(model.name, "html", html);
      model.rawData.htmlPath = htmlPath;
      console.log(`      💾 Saved raw HTML to: ${htmlPath}`);

      // Extract pricing from HTML ONLY if we didn't get it from metadata API
      if (!model.pricing) {
        const pricing = extractPricing(html);
        if (pricing) {
          model.pricing = pricing;
          console.log(`      ✅ Found pricing in HTML: $${pricing.per_second}/sec`);
        }
      }

      // Extract description if not already set
      if (!model.description) {
        const metaDescMatch = html.match(/<meta\s+name="description"\s+content="([^"]+)"/i);
        if (metaDescMatch) {
          model.description = metaDescMatch[1];
        }
      }

      // Extract GitHub, paper, license URLs
      const githubMatch = html.match(/github\.com\/([^"'\s<>]+)/i);
      if (githubMatch) {
        model.githubUrl = `https://github.com/${githubMatch[1]}`;
      }

      const paperMatch = html.match(/arxiv\.org\/(?:abs|pdf)\/([^"'\s<>]+)/i) ||
                          html.match(/papers\.[^"'\s<>]+/i);
      if (paperMatch) {
        model.paperUrl = paperMatch[0];
      }
    }

    console.log(`      ✅ Saved all raw data for ${model.name}`);
  } catch (error) {
    console.warn(`      ⚠️  Error fetching details: ${error instanceof Error ? error.message : String(error)}`);
  }

  return model;
}

/**
 * Save models to text file
 */
function saveToFile(models: ModelInfo[], filename: string): void {
  const outputDir = path.join(__dirname, "..");
  const filePath = path.join(outputDir, filename);

  let content = "# Text-to-Video Models from Replicate\n";
  content += "# Scraped from: https://replicate.com/search?query=text%20to%20video\n";
  content += `# Total models: ${models.length}\n`;
  content += `# Generated: ${new Date().toISOString()}\n\n`;
  content += "---\n\n";

  models.forEach((model, index) => {
    content += `${index + 1}. **${model.name}**\n`;
    content += `   URL: ${model.url}\n`;
    if (model.description) {
      content += `   Description: ${model.description}\n`;
    }
    if (model.owner && model.modelName) {
      content += `   Owner: ${model.owner}\n`;
      content += `   Model: ${model.modelName}\n`;
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
    if (model.githubUrl) {
      content += `   GitHub: ${model.githubUrl}\n`;
    }
    if (model.paperUrl) {
      content += `   Paper: ${model.paperUrl}\n`;
    }
    if (model.tags && model.tags.length > 0) {
      content += `   Tags: ${model.tags.join(", ")}\n`;
    }
    if (model.runCount) {
      content += `   Run Count: ${model.runCount.toLocaleString()}\n`;
    }
    if (model.coverImageUrl) {
      content += `   Cover Image: ${model.coverImageUrl}\n`;
    }
    content += "\n";
  });

  // Also save as JSON for easy parsing
  const jsonPath = path.join(outputDir, filename.replace(".txt", ".json"));
  fs.writeFileSync(jsonPath, JSON.stringify(models, null, 2));

  fs.writeFileSync(filePath, content);

  console.log(`\n✅ Saved ${models.length} models to:`);
  console.log(`   📄 ${filePath}`);
  console.log(`   📄 ${jsonPath}`);
}

/**
 * Main scraping function
 */
async function scrapeReplicateSearch() {
  console.log("🚀 Starting Replicate search page scrape...\n");

  try {
    // Scrape search page
    const models = await scrapeSearchPage("text to video");

    if (models.length === 0) {
      console.warn("⚠️  No models found. The page structure might have changed.");
      return;
    }

    console.log(`📦 Found ${models.length} models\n`);

    // Save to file immediately (API already has descriptions)
    saveToFile(models, "replicate-text-to-video-models.txt");

    // Optionally fetch additional details (slower but more complete)
    console.log("\n📥 Fetching additional details for each model...\n");
    console.log("⚡ Rate limiting: 200ms delay between models (300 req/min, well under 3000/min limit)\n");

    const modelsWithDetails: ModelInfo[] = [];
    const DELAY_BETWEEN_MODELS = 200; // ms - conservative rate limiting
    const DELAY_BETWEEN_REQUESTS = 100; // ms - delay between schema and page for same model

    for (let i = 0; i < models.length; i++) {
      const model = models[i];
      console.log(`[${i + 1}/${models.length}] ${model.name}`);

      const detailedModel = await fetchModelDetails(model, DELAY_BETWEEN_REQUESTS);
      modelsWithDetails.push(detailedModel);

      // Rate limiting - wait between models
      if (i < models.length - 1) {
        await new Promise((resolve) => setTimeout(resolve, DELAY_BETWEEN_MODELS));
      }
    }

    // Save detailed version
    saveToFile(modelsWithDetails, "replicate-text-to-video-models-detailed.txt");

    console.log(`\n🎉 Done! Scraped ${modelsWithDetails.length} models.`);
  } catch (error) {
    console.error("💥 Error scraping:", error);
    throw error;
  }
}

// Run if called directly
if (require.main === module) {
  scrapeReplicateSearch()
    .then(() => {
      process.exit(0);
    })
    .catch((error) => {
      console.error("💥 Fatal error:", error);
      process.exit(1);
    });
}

export {scrapeReplicateSearch};

