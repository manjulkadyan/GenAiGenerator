/**
 * Test scraping for specific models that are missing data
 */

import {fetchModelDetails} from "./scrapeSpecificModels";

async function testModels() {
  const testModels = [
    "wan-video/wan-2.5-t2v-hd",
    "runway/gen-4-turbo",
  ];

  for (const modelName of testModels) {
    console.log(`\n${"=".repeat(80)}`);
    console.log(`Testing: ${modelName}`);
    console.log("=".repeat(80));

    const model = await fetchModelDetails(modelName, 200);

    console.log(`\n📊 Results for ${modelName}:`);
    console.log(`   Pricing: ${model.pricing ? `$${model.pricing.per_second}/sec` : "NOT FOUND"}`);
    console.log(`   Example Videos: ${model.exampleVideoUrls?.length || 0}`);
    console.log(`   HTML Path: ${model.rawData?.htmlPath || "NOT FOUND"}`);

    if (model.exampleVideoUrls && model.exampleVideoUrls.length > 0) {
      console.log("   Video URLs:");
      model.exampleVideoUrls.forEach((url, idx) => {
        console.log(`      ${idx + 1}. ${url}`);
      });
    }
  }
}

testModels()
  .then(() => {
    console.log("\n✅ Test complete!");
    process.exit(0);
  })
  .catch((error) => {
    console.error("💥 Error:", error);
    process.exit(1);
  });

