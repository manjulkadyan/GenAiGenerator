/**
 * Create pricing mapping: shows current prices and creates new version with 20x multiplier
 *
 * Usage:
 * npm run pricing:mapping
 */

import * as fs from "fs";
import * as path from "path";

type ModelPreview = {
  id: string;
  name: string;
  replicate_name: string;
  price_per_sec: number;
  [key: string]: unknown;
};

/**
 * Create pricing mapping and new pricing file
 */
function createPricingMapping() {
  const previewPath = path.join(__dirname, "..", "replicate-specific-models-preview.json");

  if (!fs.existsSync(previewPath)) {
    console.error(`❌ File not found: ${previewPath}`);
    console.error("   Run 'npm run preview:specific' first");
    process.exit(1);
  }

  const models = JSON.parse(
    fs.readFileSync(previewPath, "utf-8")
  ) as ModelPreview[];

  console.log("=".repeat(80));
  console.log("PRICING MAPPING: Current vs New (20x Multiplier)");
  console.log("=".repeat(80));
  console.log();

  const PRICING_MULTIPLIER = 20;

  const pricingMapping: Array<{
    modelId: string;
    modelName: string;
    replicateName: string;
    currentPrice: number;
    newPrice: number;
    multiplier: number;
  }> = [];

  const modelsWithNewPricing: ModelPreview[] = [];

  for (const model of models) {
    const currentPrice = model.price_per_sec || 1;
    const newPrice = currentPrice * PRICING_MULTIPLIER;

    pricingMapping.push({
      modelId: model.id,
      modelName: model.name as string,
      replicateName: model.replicate_name as string,
      currentPrice: currentPrice,
      newPrice: newPrice,
      multiplier: PRICING_MULTIPLIER,
    });

    // Create new model with updated pricing
    const newModel = {
      ...model,
      price_per_sec: newPrice,
      original_price_per_sec: currentPrice, // Keep original for reference
      price_multiplier: PRICING_MULTIPLIER,
    };

    modelsWithNewPricing.push(newModel);

    // Display
    console.log(`${model.name}`);
    console.log(`  ID: ${model.id}`);
    console.log(`  Replicate: ${model.replicate_name}`);
    console.log(`  Current Price: ${currentPrice} credits/sec`);
    console.log(`  New Price (20x): ${newPrice} credits/sec`);
    console.log(`  Example: 5 sec video = ${currentPrice * 5} → ${newPrice * 5} credits`);
    console.log();
  }

  // Save pricing mapping
  const mappingPath = path.join(__dirname, "..", "pricing-mapping.json");
  fs.writeFileSync(mappingPath, JSON.stringify(pricingMapping, null, 2));
  console.log(`✅ Pricing mapping saved to: ${mappingPath}`);

  // Save models with new pricing
  const newPricingPath = path.join(__dirname, "..", "replicate-specific-models-new-pricing.json");
  fs.writeFileSync(newPricingPath, JSON.stringify(modelsWithNewPricing, null, 2));
  console.log(`✅ Models with new pricing saved to: ${newPricingPath}`);

  // Create summary
  const totalCurrent = pricingMapping.reduce((sum, m) => sum + m.currentPrice, 0);
  const totalNew = pricingMapping.reduce((sum, m) => sum + m.newPrice, 0);
  const avgCurrent = totalCurrent / pricingMapping.length;
  const avgNew = totalNew / pricingMapping.length;

  console.log();
  console.log("=".repeat(80));
  console.log("PRICING SUMMARY");
  console.log("=".repeat(80));
  console.log(`Total Models: ${pricingMapping.length}`);
  console.log(`Average Current Price: ${avgCurrent.toFixed(2)} credits/sec`);
  console.log(`Average New Price: ${avgNew.toFixed(2)} credits/sec`);
  console.log(`Total Current (all models): ${totalCurrent} credits/sec`);
  console.log(`Total New (all models): ${totalNew} credits/sec`);
  console.log();
  console.log("Price Distribution (Current):");
  const priceGroups = pricingMapping.reduce((acc, m) => {
    const price = m.currentPrice;
    acc[price] = (acc[price] || 0) + 1;
    return acc;
  }, {} as Record<number, number>);
  Object.entries(priceGroups)
    .sort(([a], [b]) => Number(a) - Number(b))
    .forEach(([price, count]) => {
      console.log(`  ${price} credits/sec: ${count} models`);
    });
  console.log();
  console.log(`Price Distribution (New - ${PRICING_MULTIPLIER}x):`);
  const newPriceGroups = pricingMapping.reduce((acc, m) => {
    const price = m.newPrice;
    acc[price] = (acc[price] || 0) + 1;
    return acc;
  }, {} as Record<number, number>);
  Object.entries(newPriceGroups)
    .sort(([a], [b]) => Number(a) - Number(b))
    .forEach(([price, count]) => {
      console.log(`  ${price} credits/sec: ${count} models`);
    });
  console.log();
  console.log("=".repeat(80));
}

// Run if called directly
if (require.main === module) {
  createPricingMapping();
  process.exit(0);
}

export {createPricingMapping};

