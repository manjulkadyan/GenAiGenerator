const admin = require('firebase-admin');
const fs = require('fs');

// Initialize Firebase Admin (uses default credentials)
try {
  admin.initializeApp();
} catch (e) {
  // Already initialized
}

const db = admin.firestore();

async function fetchVideoFeatures() {
  try {
    console.log('Fetching from Firestore collection: video_features...');
    
    const collectionRef = db.collection('video_features');
    const snapshot = await collectionRef.get();
    
    const models = [];
    snapshot.forEach(doc => {
      const data = doc.data();
      data._firestore_id = doc.id;
      models.push(data);
    });
    
    console.log(`Fetched ${models.length} models`);
    
    // Save to file
    fs.writeFileSync('firestore_video_features.json', JSON.stringify(models, null, 2));
    console.log('✓ Saved to firestore_video_features.json');
    
    // Analyze
    if (models.length > 0) {
      console.log('\n=== First Model ===');
      const first = models[0];
      console.log(`ID: ${first.id || 'N/A'}`);
      console.log(`Keys: ${Object.keys(first).slice(0, 15).join(', ')}`);
      
      // Check duplicates
      const byId = {};
      models.forEach(m => {
        const id = m.id || '';
        if (id) {
          if (!byId[id]) byId[id] = [];
          byId[id].push(m);
        }
      });
      
      const duplicates = Object.entries(byId).filter(([k, v]) => v.length > 1);
      console.log(`\nDuplicate IDs: ${duplicates.length}`);
      if (duplicates.length > 0) {
        console.log('Examples:');
        duplicates.slice(0, 3).forEach(([id, models]) => {
          console.log(`  ${id}: ${models.length} entries`);
        });
      }
    }
    
    process.exit(0);
  } catch (error) {
    console.error('Error:', error.message);
    process.exit(1);
  }
}

fetchVideoFeatures();

