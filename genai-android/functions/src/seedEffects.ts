import * as admin from 'firebase-admin';
import fetch from 'node-fetch';

// Initialize Firebase Admin if not already done
if (!admin.apps.length) {
  admin.initializeApp();
}

const db = admin.firestore();

interface PixverseTemplate {
  template_id: number;
  display_name: string;
  display_prompt: string;
  duration: number;
  web_thumbnail_gif_url: string;
  web_thumbnail_video_url: string;
  web_thumbnail_url: string;
  marker: string;
  effect_type: string;
}

export async function fetchAndSeedEffects(): Promise<void> {
  console.log('Fetching templates from Pixverse API...');
  
  const response = await fetch(
    'https://app-api.pixverse.ai/creative_platform/content/template/list?primary_category=3&platform=web&limit=200&offset=0',
    {
      headers: {
        'accept': 'application/json',
        'origin': 'https://app.pixverse.ai',
        'x-platform': 'Web',
      },
    }
  );
  
  const data = await response.json() as { Resp: { data: PixverseTemplate[] } };
  const templates = data.Resp.data;
  
  console.log(`Fetched ${templates.length} templates`);
  
  // Batch write to Firestore
  const batch = db.batch();
  const effectsRef = db.collection('video_effects');
  
  for (const t of templates) {
    const docRef = effectsRef.doc(t.template_id.toString());
    batch.set(docRef, {
      templateId: t.template_id,
      name: t.display_name,
      prompt: t.display_prompt || '',
      duration: t.duration || 5,
      previewGif: t.web_thumbnail_gif_url || '',
      previewVideo: t.web_thumbnail_video_url || '',
      previewImage: t.web_thumbnail_url || '',
      marker: t.marker || 'default',
      effectType: t.effect_type || '1',
      credits: 45, // Default credits for 5s 540p
      isActive: true,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
    });
  }
  
  await batch.commit();
  console.log(`✅ Seeded ${templates.length} effects to Firestore!`);
}

// Run if called directly
if (require.main === module) {
  fetchAndSeedEffects()
    .then(() => process.exit(0))
    .catch((err) => {
      console.error(err);
      process.exit(1);
    });
}
