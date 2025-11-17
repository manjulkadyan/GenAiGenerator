/**
 * Check robots.txt and rate limit information for Replicate
 */

async function checkRobotsTxt() {
  console.log("🤖 Checking robots.txt files...\n");

  try {
    // Check main Replicate site
    console.log("📄 Checking https://replicate.com/robots.txt");
    const mainResponse = await fetch("https://replicate.com/robots.txt");
    if (mainResponse.ok) {
      const text = await mainResponse.text();
      console.log("✅ Main site robots.txt:");
      console.log(text);
      console.log("\n");
    } else {
      console.log(`❌ Could not fetch: ${mainResponse.status}\n`);
    }
  } catch (error) {
    console.error("❌ Error fetching main robots.txt:", error);
  }

  try {
    // Check search API (if it has one)
    console.log("📄 Checking search API robots.txt");
    const searchResponse = await fetch("https://replicate-search-prototype-production.replicate.workers.dev/robots.txt");
    if (searchResponse.ok) {
      const text = await searchResponse.text();
      console.log("✅ Search API robots.txt:");
      console.log(text);
    } else {
      console.log(`ℹ️  No robots.txt for search API (${searchResponse.status} - this is normal)\n`);
    }
  } catch (error) {
    console.log("ℹ️  Search API doesn't have robots.txt (this is normal)\n");
  }
}

async function checkRateLimits() {
  console.log("⚡ Rate Limit Information:\n");
  console.log("Based on Replicate API documentation:");
  console.log("  • Create Prediction Requests: 600 requests/minute");
  console.log("  • All Other Endpoints: 3,000 requests/minute");
  console.log("  • Returns 429 status code when throttled\n");

  console.log("📊 Current Scraper Rate Limiting:");
  console.log("  • Delay between model detail requests: 200ms");
  console.log("  • Requests per second: ~5 (300/minute)");
  console.log("  • Per model: 2 requests (schema + page)");
  console.log("  • For 50 models: ~100 requests in ~20 seconds");
  console.log("  • Well within limits ✅\n");

  console.log("💡 Recommendations:");
  console.log("  • Current 200ms delay is conservative and safe");
  console.log("  • Could reduce to 100ms if needed (600 req/min)");
  console.log("  • Add exponential backoff on 429 errors");
  console.log("  • Respect robots.txt directives\n");
}

async function main() {
  await checkRobotsTxt();
  await checkRateLimits();
}

if (require.main === module) {
  main()
    .then(() => process.exit(0))
    .catch((error) => {
      console.error("Error:", error);
      process.exit(1);
    });
}

export {checkRobotsTxt, checkRateLimits};

