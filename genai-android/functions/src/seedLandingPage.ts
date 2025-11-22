import * as fs from "fs";
import * as path from "path";
import {initializeApp, cert, App} from "firebase-admin/app";
import {getFirestore} from "firebase-admin/firestore";

/**
 * Landing page configuration structure
 */
interface LandingPageConfig {
  backgroundVideoUrl: string;
  features: Array<{
    title: string;
    description: string;
    icon: string;
  }>;
  subscriptionPlans: Array<{
    credits: number;
    price: string;
    isPopular: boolean;
    productId: string;
    period: string;
  }>;
  testimonials: Array<{
    username: string;
    rating: number;
    text: string;
  }>;
}

/**
 * Seed landing page configuration to Firestore
 */
async function seedLandingPage() {
  // Path to landing page config JSON
  const configPath = path.join(__dirname, "..", "..", "landingPageConfig.json");

  if (!fs.existsSync(configPath)) {
    console.error(`❌ Landing page config not found: ${configPath}`);
    console.error(`   Please create landingPageConfig.json in the genai-android directory`);
    process.exit(1);
  }

  console.log(`📖 Reading landing page config from: ${configPath}\n`);

  let config: LandingPageConfig;
  try {
    const configContent = fs.readFileSync(configPath, "utf-8");
    config = JSON.parse(configContent) as LandingPageConfig;
  } catch (error) {
    console.error(`❌ Failed to read or parse config file:`, error);
    process.exit(1);
  }

  // Validate config
  if (!config.features || !Array.isArray(config.features)) {
    console.error(`❌ Invalid config: features array is required`);
    process.exit(1);
  }

  if (!config.subscriptionPlans || !Array.isArray(config.subscriptionPlans)) {
    console.error(`❌ Invalid config: subscriptionPlans array is required`);
    process.exit(1);
  }

  console.log(`✅ Config loaded successfully:`);
  console.log(`   - Background video: ${config.backgroundVideoUrl || "Not set"}`);
  console.log(`   - Features: ${config.features.length}`);
  console.log(`   - Subscription plans: ${config.subscriptionPlans.length}`);
  console.log(`   - Testimonials: ${config.testimonials?.length || 0}\n`);

  // Initialize Firebase
  const serviceAccountPath = path.join(__dirname, "..", "service-account-key.json");
  if (!fs.existsSync(serviceAccountPath)) {
    console.error(`❌ Service account key not found: ${serviceAccountPath}`);
    console.error(`   Please ensure service-account-key.json exists in the functions directory`);
    process.exit(1);
  }

  console.log(`🔐 Initializing Firebase Admin...`);

  const serviceAccount = JSON.parse(fs.readFileSync(serviceAccountPath, "utf-8"));
  let app: App;
  try {
    app = initializeApp({
      credential: cert(serviceAccount),
    });
    console.log(`✅ Firebase Admin initialized\n`);
  } catch (error) {
    // App might already be initialized
    try {
      app = initializeApp();
      console.log(`✅ Firebase Admin initialized (using existing app)\n`);
    } catch (initError) {
      console.error(`❌ Failed to initialize Firebase Admin:`, initError);
      process.exit(1);
    }
  }

  const firestore = getFirestore(app);
  firestore.settings({ignoreUndefinedProperties: true});

  // Upload to app/landingPage
  const docRef = firestore.collection("app").doc("landingPage");

  console.log(`📤 Uploading landing page config to Firestore...`);
  console.log(`   Collection: app`);
  console.log(`   Document: landingPage\n`);

  try {
    await docRef.set(config, {merge: false});
    console.log(`✅ Successfully uploaded landing page config!\n`);

    // Verify by reading back
    const snapshot = await docRef.get();
    if (snapshot.exists) {
      const data = snapshot.data();
      console.log(`✅ Verification successful:`);
      console.log(`   - Document exists: ✅`);
      console.log(`   - Features count: ${data?.features?.length || 0}`);
      console.log(`   - Plans count: ${data?.subscriptionPlans?.length || 0}`);
      console.log(`\n🎉 Landing page is ready! Your app will now use this configuration.`);
    } else {
      console.error(`❌ Verification failed: Document was not created`);
      process.exit(1);
    }
  } catch (error) {
    console.error(`❌ Failed to upload landing page config:`, error);
    process.exit(1);
  }
}

// Run if called directly
if (require.main === module) {
  seedLandingPage()
    .then(() => {
      console.log(`\n✅ Done!`);
      process.exit(0);
    })
    .catch((error) => {
      console.error(`\n❌ Error:`, error);
      process.exit(1);
    });
}

export {seedLandingPage};

