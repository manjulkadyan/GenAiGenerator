/**
 * Script to analyze all model schemas and determine:
 * 1. Which models support first_frame/last_frame
 * 2. Whether they're optional or required
 * 3. What field names are used
 */

import * as fs from "fs";
import * as path from "path";

interface FrameSupport {
  model: string;
  firstFrame?: {
    fieldName: string;
    required: boolean;
    nullable: boolean;
  };
  lastFrame?: {
    fieldName: string;
    required: boolean;
    nullable: boolean;
  };
}

function extractSchemaFromFile(filePath: string): any {
  try {
    const content = fs.readFileSync(filePath, "utf-8");

    // Try JSON first
    try {
      return JSON.parse(content);
    } catch {
      // Extract from HTML/__NEXT_DATA__
      const jsonMatch = content.match(/<script[^>]*id="__NEXT_DATA__"[^>]*>([\s\S]*?)<\/script>/);
      if (jsonMatch) {
        try {
          const nextData = JSON.parse(jsonMatch[1]);
          if (nextData?.props?.pageProps?.model?.latest_version?.openapi_schema) {
            return nextData.props.pageProps.model.latest_version.openapi_schema;
          } else if (nextData?.props?.pageProps?.version?.openapi_schema) {
            return nextData.props.pageProps.version.openapi_schema;
          }
        } catch {}
      }

      // Try react component props
      const reactMatch = content.match(/<script[^>]*type="application\/json"[^>]*>([\s\S]*?)<\/script>/);
      if (reactMatch) {
        try {
          const reactData = JSON.parse(reactMatch[1]);
          if (reactData?.version?._extras?.dereferenced_openapi_schema) {
            return reactData.version._extras.dereferenced_openapi_schema;
          }
        } catch {}
      }
    }
  } catch (error) {
    return null;
  }
  return null;
}

function analyzeSchema(schema: any, modelName: string): FrameSupport {
  const result: FrameSupport = {model: modelName};

  // Get input schema
  let inputSchema: any = null;
  if (schema?.components?.schemas?.Input) {
    inputSchema = schema.components.schemas.Input;
  } else if (schema?.input) {
    inputSchema = schema.input;
  } else if (schema?.paths?.["/predictions"]?.post?.requestBody?.content?.["application/json"]?.schema?.properties?.input) {
    inputSchema = schema.paths["/predictions"].post.requestBody.content["application/json"].schema.properties.input;
  }

  if (!inputSchema || !inputSchema.properties) {
    return result;
  }

  const required = inputSchema.required || [];
  const properties = inputSchema.properties;

  // Check for first frame fields
  const firstFrameKeys = [
    "first_frame", "firstFrame", "first_frame_image", "firstFrameImage",
    "start_frame", "startFrame", "first_frame_url", "firstFrameUrl",
  ];

  for (const key of firstFrameKeys) {
    if (properties[key]) {
      const prop = properties[key];
      result.firstFrame = {
        fieldName: key,
        required: required.includes(key),
        nullable: prop.nullable === true,
      };
      break;
    }
  }

  // Check for last frame fields
  const lastFrameKeys = [
    "last_frame", "lastFrame", "last_frame_image", "lastFrameImage",
    "end_frame", "endFrame", "last_frame_url", "lastFrameUrl",
  ];

  for (const key of lastFrameKeys) {
    if (properties[key]) {
      const prop = properties[key];
      result.lastFrame = {
        fieldName: key,
        required: required.includes(key),
        nullable: prop.nullable === true,
      };
      break;
    }
  }

  return result;
}

function main() {
  const rawDataDir = path.join(__dirname, "..", "raw-data");
  const schemaFiles = fs.readdirSync(rawDataDir)
    .filter((f) => f.endsWith("_schema.json"))
    .sort();

  const results: FrameSupport[] = [];

  for (const file of schemaFiles) {
    const modelName = file.replace("_schema.json", "").replace(/_/g, "/");
    const filePath = path.join(rawDataDir, file);
    const schema = extractSchemaFromFile(filePath);

    if (schema) {
      const analysis = analyzeSchema(schema, modelName);
      results.push(analysis);
    }
  }

  // Print results
  console.log("=".repeat(80));
  console.log("FRAME SUPPORT ANALYSIS");
  console.log("=".repeat(80));
  console.log();

  const withFirstFrame = results.filter((r) => r.firstFrame);
  const withLastFrame = results.filter((r) => r.lastFrame);
  const withBoth = results.filter((r) => r.firstFrame && r.lastFrame);
  const withNone = results.filter((r) => !r.firstFrame && !r.lastFrame);

  console.log(`Total models analyzed: ${results.length}`);
  console.log(`Models with first frame support: ${withFirstFrame.length}`);
  console.log(`Models with last frame support: ${withLastFrame.length}`);
  console.log(`Models with both: ${withBoth.length}`);
  console.log(`Models with neither: ${withNone.length}`);
  console.log();

  console.log("=".repeat(80));
  console.log("MODELS WITH FIRST FRAME SUPPORT");
  console.log("=".repeat(80));
  for (const r of withFirstFrame) {
    const f = r.firstFrame!;
    const status = f.required ? "REQUIRED" : f.nullable ? "OPTIONAL (nullable)" : "OPTIONAL";
    console.log(`${r.model.padEnd(40)} ${f.fieldName.padEnd(20)} ${status}`);
  }
  console.log();

  console.log("=".repeat(80));
  console.log("MODELS WITH LAST FRAME SUPPORT");
  console.log("=".repeat(80));
  for (const r of withLastFrame) {
    const f = r.lastFrame!;
    const status = f.required ? "REQUIRED" : f.nullable ? "OPTIONAL (nullable)" : "OPTIONAL";
    console.log(`${r.model.padEnd(40)} ${f.fieldName.padEnd(20)} ${status}`);
  }
  console.log();

  console.log("=".repeat(80));
  console.log("MODELS WITH NO FRAME SUPPORT");
  console.log("=".repeat(80));
  for (const r of withNone) {
    console.log(r.model);
  }
  console.log();

  // Generate normalized schema summary
  console.log("=".repeat(80));
  console.log("NORMALIZED SCHEMA SUMMARY");
  console.log("=".repeat(80));
  console.log();
  console.log("// Normalized frame support structure:");
  console.log("interface FrameSupport {");
  console.log("  supportsFirstFrame: boolean;  // Model has first_frame field");
  console.log("  requiresFirstFrame: boolean;   // Field is required (not optional)");
  console.log("  supportsLastFrame: boolean;    // Model has last_frame field");
  console.log("  requiresLastFrame: boolean;    // Field is required (not optional)");
  console.log("}");
  console.log();

  // Export JSON for programmatic use
  const exportData = results.map((r) => ({
    model: r.model,
    supportsFirstFrame: !!r.firstFrame,
    requiresFirstFrame: r.firstFrame?.required || false,
    firstFrameFieldName: r.firstFrame?.fieldName || null,
    supportsLastFrame: !!r.lastFrame,
    requiresLastFrame: r.lastFrame?.required || false,
    lastFrameFieldName: r.lastFrame?.fieldName || null,
  }));

  const exportPath = path.join(__dirname, "..", "frame-support-analysis.json");
  fs.writeFileSync(exportPath, JSON.stringify(exportData, null, 2));
  console.log(`✅ Analysis exported to: ${exportPath}`);
}

main();

