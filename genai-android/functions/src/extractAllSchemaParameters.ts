/**
 * Comprehensive schema parameter extraction
 * Extracts ALL input parameters from model schemas dynamically
 */

export interface SchemaParameter {
  name: string; // Parameter name (e.g., "prompt", "seed", "guidance_scale")
  type: "string" | "number" | "boolean" | "array" | "object" | "enum"; // Parameter type
  required: boolean; // Is this parameter required?
  nullable: boolean; // Can this parameter be null?
  description?: string; // Parameter description
  default?: unknown; // Default value
  enum?: unknown[]; // Enum values (if type is enum)
  min?: number; // Minimum value (for numbers)
  max?: number; // Maximum value (for numbers)
  format?: string; // Format hint (e.g., "uri" for URLs)
  title?: string; // Human-readable title
}

export interface ExtractedSchema {
  parameters: SchemaParameter[]; // All input parameters
  requiredFields: string[]; // List of required field names
  categorized: {
    text: SchemaParameter[]; // Text inputs (prompt, negative_prompt, etc.)
    numeric: SchemaParameter[]; // Numeric inputs (seed, guidance_scale, duration, etc.)
    boolean: SchemaParameter[]; // Boolean flags (enable_audio, camera_fixed, etc.)
    enum: SchemaParameter[]; // Enum selections (aspect_ratio, resolution, etc.)
    file: SchemaParameter[]; // File/URL inputs (image, video, audio, frames, etc.)
  };
}

/**
 * Extract ALL input parameters from a schema
 */
export function extractAllSchemaParameters(schema: Record<string, unknown> | null): ExtractedSchema {
  const result: ExtractedSchema = {
    parameters: [],
    requiredFields: [],
    categorized: {
      text: [],
      numeric: [],
      boolean: [],
      enum: [],
      file: [],
    },
  };

  if (!schema || typeof schema !== "object") {
    return result;
  }

  // Get input schema from various possible locations
  let inputSchema: Record<string, unknown> | null = null;

  // Type-safe access helper
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

  // Try different schema structures (OpenAPI 3.0 format)
  const componentsSchemasInput = getNested(schema, ["components", "schemas", "Input"]);
  if (componentsSchemasInput && typeof componentsSchemasInput === "object") {
    inputSchema = componentsSchemasInput as Record<string, unknown>;
  }
  // Try direct input property
  else if (schema.input && typeof schema.input === "object") {
    inputSchema = schema.input as Record<string, unknown>;
  }
  // Try OpenAPI paths structure
  else {
    const pathsPredictions = getNested(schema, ["paths", "/predictions", "post", "requestBody", "content", "application/json", "schema", "properties", "input"]);
    if (pathsPredictions && typeof pathsPredictions === "object") {
      inputSchema = pathsPredictions as Record<string, unknown>;
    }
    // Try dereferenced_openapi_schema structure (from HTML)
    else if (schema.dereferenced_openapi_schema && typeof schema.dereferenced_openapi_schema === "object") {
      const derefSchema = schema.dereferenced_openapi_schema as Record<string, unknown>;
      const derefComponentsInput = getNested(derefSchema, ["components", "schemas", "Input"]);
      if (derefComponentsInput && typeof derefComponentsInput === "object") {
        inputSchema = derefComponentsInput as Record<string, unknown>;
      } else {
        const derefPathsInput = getNested(derefSchema, ["paths", "/predictions", "post", "requestBody", "content", "application/json", "schema", "properties", "input"]);
        if (derefPathsInput && typeof derefPathsInput === "object") {
          inputSchema = derefPathsInput as Record<string, unknown>;
        }
      }
    }
  }

  if (!inputSchema || !inputSchema.properties) {
    return result;
  }

  const properties = inputSchema.properties as Record<string, unknown>;
  const required = (inputSchema.required as string[]) || [];
  result.requiredFields = required;

  // Extract all parameters
  for (const [key, value] of Object.entries(properties)) {
    if (!value || typeof value !== "object") continue;

    const param = value as Record<string, unknown>;
    const paramType = param.type as string;
    const isRequired = required.includes(key);
    const isNullable = param.nullable === true;

    // Determine parameter type
    let type: SchemaParameter["type"] = "string";
    if (paramType === "integer" || paramType === "number") {
      type = "number";
    } else if (paramType === "boolean") {
      type = "boolean";
    } else if (paramType === "array") {
      type = "array";
    } else if (paramType === "object") {
      type = "object";
    } else if (param.enum || param.oneOf) {
      type = "enum";
    }

    const schemaParam: SchemaParameter = {
      name: key,
      type,
      required: isRequired,
      nullable: isNullable,
      description: param.description as string | undefined,
      default: param.default,
      format: param.format as string | undefined,
      title: param.title as string | undefined,
    };

    // Extract enum values
    if (param.enum) {
      schemaParam.enum = param.enum as unknown[];
    } else if (param.oneOf) {
      schemaParam.enum = (param.oneOf as Array<{ const?: unknown }>)
        .map((item) => item.const)
        .filter((v) => v !== undefined);
    }

    // Extract min/max for numbers
    if (type === "number") {
      if (param.minimum !== undefined) schemaParam.min = param.minimum as number;
      if (param.maximum !== undefined) schemaParam.max = param.maximum as number;
    }

    result.parameters.push(schemaParam);

    // Categorize parameter
    const lowerKey = key.toLowerCase();

    // File/URL inputs
    if (
      lowerKey.includes("image") ||
      lowerKey.includes("video") ||
      lowerKey.includes("audio") ||
      lowerKey.includes("frame") ||
      lowerKey.includes("file") ||
      lowerKey.includes("url") ||
      schemaParam.format === "uri" ||
      schemaParam.format === "binary"
    ) {
      result.categorized.file.push(schemaParam);
    }
    // Boolean flags
    else if (type === "boolean") {
      result.categorized.boolean.push(schemaParam);
    }
    // Enum selections
    else if (type === "enum" || schemaParam.enum) {
      result.categorized.enum.push(schemaParam);
    }
    // Numeric inputs
    else if (type === "number") {
      result.categorized.numeric.push(schemaParam);
    }
    // Text inputs (default)
    else {
      result.categorized.text.push(schemaParam);
    }
  }

  return result;
}

/**
 * Get a normalized parameter name (handles different naming conventions)
 */
export function normalizeParameterName(name: string): string {
  // Convert snake_case to camelCase for consistency
  return name.replace(/_([a-z])/g, (_, letter) => letter.toUpperCase());
}

/**
 * Check if a parameter is a "core" parameter that should always be shown
 */
export function isCoreParameter(name: string): boolean {
  const coreParams = [
    "prompt",
    "duration",
    "aspect_ratio",
    "aspectRatio",
    "resolution",
    "first_frame",
    "firstFrame",
    "first_frame_image",
    "last_frame",
    "lastFrame",
    "last_frame_image",
  ];
  return coreParams.includes(name.toLowerCase());
}

