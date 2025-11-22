import * as admin from "firebase-admin";
import {defineSecret} from "firebase-functions/params";
import {setGlobalOptions} from "firebase-functions/v2";
import {onCall, onRequest} from "firebase-functions/v2/https";

setGlobalOptions({maxInstances: 10});

admin.initializeApp();
const firestore = admin.firestore();
const replicateToken = defineSecret("REPLICATE_API_TOKEN");

type GenerateRequest = {
  modelId: string;
  replicateName: string;
  prompt: string;
  durationSeconds: number;
  aspectRatio: string;
  firstFrameUrl?: string;
  lastFrameUrl?: string;
  userId: string;
  cost?: number;
  usePromptOptimizer?: boolean;
  enableAudio?: boolean; // Audio support - doubles the cost
  negativePrompt?: string; // Negative prompt for models that support it
};

type EffectRequest = {
  imageUrl: string;
  effectId: string;
  effectPrompt: string;
  userId: string;
  appVersion: string;
  aspectRatio?: string;
};

type ReplicateResponse = {
  id: string;
  status: string;
  urls?: {get?: string};
};

type ReplicatePrediction = {
  id: string;
  status: "starting" | "processing" | "succeeded" | "failed" | "canceled";
  output?: string | string[];
  error?: string;
  urls?: {
    get?: string;
    cancel?: string;
  };
};

export const callReplicateVeoAPIV2 = onCall<GenerateRequest>(
  {secrets: [replicateToken]},
  async ({data, auth}) => {
    if (!auth) {
      throw new Error("Missing auth.");
    }
    const token = replicateToken.value();
    if (!token) {
      throw new Error("REPLICATE_API_TOKEN not set.");
    }

    const userId = data.userId || auth.uid;

    // 0. Fetch model document to get schema and validate duration
    let modelData: admin.firestore.DocumentData | undefined;
    let imageParamName: string | null = null;
    let firstFrameParamName: string | null = null;
    let lastFrameParamName: string | null = null;
    let audioParamName: string | null = null;

    if (data.modelId) {
      const modelDoc = await firestore
        .collection("models")
        .doc(data.modelId)
        .get();
      if (modelDoc.exists) {
        modelData = modelDoc.data();
        const allowedDurations =
          (modelData?.duration_options as number[]) || [];
        if (allowedDurations.length > 0 && data.durationSeconds) {
          if (!allowedDurations.includes(data.durationSeconds)) {
            throw new Error(
              `Invalid duration: ${data.durationSeconds}. ` +
                `Allowed durations: ${allowedDurations.join(", ")}`,
            );
          }
        }

        // Extract parameter names from schema_parameters
        const schemaParams = (modelData?.schema_parameters as Array<{
          name: string;
          type: string;
          format?: string;
        }>) || [];

        // Find image input parameter names
        // Priority: image > first_frame > input_image > firstFrameUrl
        const imageParam = schemaParams.find((p) => {
          const nameLower = p.name.toLowerCase();
          return (
            nameLower === "image" ||
            nameLower === "input_image" ||
            nameLower === "inputimage" ||
            (nameLower.includes("image") && p.type === "string" &&
              (p.format === "uri" || p.format === "url"))
          );
        });

        const firstFrameParam = schemaParams.find((p) => {
          const nameLower = p.name.toLowerCase();
          return (
            nameLower === "first_frame" ||
            nameLower === "firstframe" ||
            nameLower === "first_frame_url" ||
            nameLower === "firstframeurl" ||
            nameLower === "first_frame_image"
          );
        });

        const lastFrameParam = schemaParams.find((p) => {
          const nameLower = p.name.toLowerCase();
          return (
            nameLower === "last_frame" ||
            nameLower === "lastframe" ||
            nameLower === "last_frame_url" ||
            nameLower === "lastframeurl" ||
            nameLower === "last_frame_image"
          );
        });

        const audioParam = schemaParams.find((p) => {
          const nameLower = p.name.toLowerCase();
          return (
            nameLower === "audio" ||
            nameLower === "audio_file" ||
            nameLower === "audiofile" ||
            (nameLower.includes("audio") && p.type === "string" &&
              (p.format === "uri" || p.format === "url"))
          );
        });

        // Set parameter names (use actual schema name, not our internal name)
        imageParamName = imageParam?.name || null;
        firstFrameParamName = firstFrameParam?.name || null;
        lastFrameParamName = lastFrameParam?.name || null;
        audioParamName = audioParam?.name || null;

        console.log(
          `Model ${data.modelId} parameter mapping: ` +
            `image=${imageParamName}, ` +
            `first_frame=${firstFrameParamName}, ` +
            `last_frame=${lastFrameParamName}, ` +
            `audio=${audioParamName}`,
        );
      }
    }

    // 1. Ensure user document exists, then check and deduct credits
    const userRef = firestore.collection("users").doc(userId);
    let userDoc = await userRef.get();

    // Create user document if it doesn't exist
    if (!userDoc.exists) {
      await userRef.set({
        credits: 0,
        created_at: admin.firestore.FieldValue.serverTimestamp(),
      });
      // Re-fetch the document after creation
      userDoc = await userRef.get();
    }

    const currentCredits = (userDoc.data()?.credits as number) || 0;
    const cost = data.cost || 0;

    if (currentCredits < cost) {
      throw new Error(
        `Insufficient credits. Required: ${cost}, Available: ${currentCredits}`,
      );
    }

    // Deduct credits immediately (will refund if job fails)
    await userRef.update({
      credits: admin.firestore.FieldValue.increment(-cost),
    });

    console.log(
      `Deducted ${cost} credits from user ${userId}. ` +
        `Remaining: ${currentCredits - cost}`,
    );

    // Webhook URL - will be set after first deployment
    // Format: https://{region}-{project}.cloudfunctions.net/replicateWebhook
    // You can set this as environment variable: WEBHOOK_URL
    // Or it will be auto-detected from function URL after deployment
    const webhookUrl =
      process.env.WEBHOOK_URL ||
      `https://us-central1-${
        process.env.GCLOUD_PROJECT || "your-project-id"
      }.cloudfunctions.net/replicateWebhook`;

    // Build input payload - dynamically map parameters based on model schema
    const input: Record<string, unknown> = {
      prompt: data.prompt,
      duration: data.durationSeconds,
      aspect_ratio: data.aspectRatio,
    };

    // Map first frame URL to correct parameter name
    if (data.firstFrameUrl) {
      // Priority: image > first_frame > firstFrame (for models like wan-video)
      if (imageParamName) {
        // If model uses "image" parameter, use that
        // (e.g., wan-video/wan-2.5-i2v)
        input[imageParamName] = data.firstFrameUrl;
        console.log(
          `Mapped firstFrameUrl to parameter: ${imageParamName}`,
        );
      } else if (firstFrameParamName) {
        // Use the actual parameter name from schema
        input[firstFrameParamName] = data.firstFrameUrl;
        console.log(
          `Mapped firstFrameUrl to parameter: ${firstFrameParamName}`,
        );
      } else {
        // Fallback to default names
        input.first_frame = data.firstFrameUrl;
        input.firstFrame = data.firstFrameUrl;
        console.log("Using fallback: first_frame and firstFrame");
      }
    }

    // Map last frame URL to correct parameter name
    if (data.lastFrameUrl) {
      if (lastFrameParamName) {
        input[lastFrameParamName] = data.lastFrameUrl;
        console.log(`Mapped lastFrameUrl to parameter: ${lastFrameParamName}`);
      } else {
        // Fallback to default names
        input.last_frame = data.lastFrameUrl;
        input.lastFrame = data.lastFrameUrl;
        console.log("Using fallback: last_frame and lastFrame");
      }
    }

    // Map audio parameter
    if (data.enableAudio) {
      if (audioParamName) {
        // For audio file URI (if model supports audio file input)
        // Note: Currently we only support boolean enable_audio, not file upload
        // This is for future support
        input[audioParamName] = true;
      }
      // Support common audio parameter names
      input.generate_audio = true;
      input.enable_audio = true;
      input.with_audio = true;
    }

    // Add negative prompt if provided
    if (data.negativePrompt) {
      input.negative_prompt = data.negativePrompt;
      input.negativePrompt = data.negativePrompt;
    }

    const payload = {
      version: data.replicateName,
      input,
      webhook: webhookUrl,
      // Send webhook for all status changes (more reliable)
      webhook_events_filter: ["start", "output", "logs", "completed"],
    };

    const response = await fetch(
      "https://api.replicate.com/v1/predictions",
      {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "Authorization": `Bearer ${token}`,
        },
        body: JSON.stringify(payload),
      },
    );

    if (!response.ok) {
      const text = await response.text();
      // Refund credits if Replicate API call fails
      await userRef.update({
        credits: admin.firestore.FieldValue.increment(cost),
      });
      console.log(
        `Refunded ${cost} credits to user ${userId} ` +
          "due to Replicate API error",
      );
      throw new Error(`Replicate error: ${text}`);
    }

    const result = (await response.json()) as ReplicateResponse;

    // Duplicate check: Prevent creating the same job twice
    const jobRef = firestore
      .collection("users")
      .doc(userId)
      .collection("jobs")
      .doc(result.id);

    const existingJob = await jobRef.get();

    if (existingJob.exists) {
      console.log(`Job ${result.id} already exists, skipping creation`);
      // Return existing job info instead of creating duplicate
      return {
        predictionId: result.id,
        status: existingJob.data()?.status || "PROCESSING",
        webhook: result.urls?.get ?? "",
      };
    }

    // Create new job document
    await writeJobDocument({
      uid: userId,
      jobId: result.id,
      payload: {
        prompt: data.prompt,
        model_id: data.modelId,
        model_name: data.replicateName,
        duration_seconds: data.durationSeconds,
        aspect_ratio: data.aspectRatio,
        status: "PROCESSING", // Match app's VideoJobStatus
        replicate_prediction_id: result.id,
        cost: cost, // Store cost for tracking and potential refund
        credits_deducted: cost, // Track credits deducted
        enable_audio: data.enableAudio || false,
        first_frame_url: data.firstFrameUrl || null,
        last_frame_url: data.lastFrameUrl || null,
        created_at:
          admin.firestore.FieldValue.serverTimestamp(),
      },
    });

    // Create prediction lookup for webhook (avoids collectionGroup index)
    await firestore.collection("predictions").doc(result.id).set({
      user_id: userId,
      job_id: result.id,
      created_at: admin.firestore.FieldValue.serverTimestamp(),
    });

    return {
      predictionId: result.id,
      status: result.status,
      webhook: result.urls?.get ?? "",
    };
  },
);

/**
 * Example video URLs from replicate-specific-models.txt
 * These are real example videos from various AI models on Replicate
 */
const EXAMPLE_VIDEO_URLS = [
  "https://replicate.delivery/xezq/6AV71MwvhV7nINq376qBf27eo7k3KMBZtrM9KhejLz8uhfDWB/tmpkoheyvn2.mp4",
  "https://replicate.delivery/xezq/TjiHWxnCNgp2O5sJJJ7QjcVRQQUmg6WdoqOUuVuO3huwZiYF/tmp72b74bmb.mp4",
  "https://replicate.delivery/xezq/rf7jDpzBdvVoUCKMV5eavWwKmrtzbiX5YJ4jMdiCXH1if9BrA/tmpot52l9ao.mp4",
  "https://replicate.delivery/xezq/269LKn0Xu5LPHVjcELGzgNmIY0ujGLUAOjIXfF6gcjby5gwKA/tmp95uk29hk.mp4",
  "https://replicate.delivery/xezq/WoC3evx2EQQHLCHq9vEfjhIq9ZotfQhQWmBEd54iLhvVUleVB/tmpk07h98l4.mp4",
  "https://replicate.delivery/xezq/MDBpHsfnFuS2TiCNuXM0QGghQ83rAuHikI5x9heoWs2mqTfqA/tmp62g_gnbr.mp4",
  "https://replicate.delivery/xezq/69ok3gifKESec04GfPMaTU2YShWMSL37elrN7dQ3QJmeD56rC/tmpvruiwe2d.mp4",
  "https://replicate.delivery/xezq/hpxa0wn2og7aIRife9Xlvu0OGeS1qaz45bl43foW3rL58t9VB/tmpgnyhwssx.mp4",
  "https://replicate.delivery/xezq/aOYeXeCcnHhDiELUfr7GJR0IkfcGabYRUWiTShhvdJCTEQ9VB/tmpafbwmta5.mp4",
  "https://replicate.delivery/xezq/1B4tYHYfzWzDA6QU9CEDxkCnStf8YeysV0TlfnKktgG3la9VB/tmpyxaslbuk.mp4",
  "https://replicate.delivery/xezq/Z3Ix0cUxzkqcL18AfbOu0B5pXJFfsqfscQQzenO288McMb9VB/tmp2vogi5sn.mp4",
  "https://replicate.delivery/xezq/8ZOMlNOoESqDDZXc9SwJNselA7lTLS1MBx3HgSx0lTzwhrvKA/tmpyid0s5l4.mp4",
  "https://replicate.delivery/xezq/xfz0U06bqSw1AyiJsw7LjOjVXOBpmjJfe7K9XtRLifW26q7SB/tmpi29yh_f0.mp4",
  "https://replicate.delivery/xezq/Hlc4n4H0hJYsARpeGdaOLCdaWLyRUyaWNqsg5vosHdnvJgXKA/tmpsz1voe2j.mp4",
  "https://replicate.delivery/xezq/IZvJUH7VaYo9BBq9lhMjAHXCz1WJUuOWyXx2fFGcKXcrlaYKA/tmpxsomu815.mp4",
  "https://replicate.delivery/xezq/lZfr3rskBEVLDCytear4lUo92fOpAcoblFWzk6e06WEnC3PTB/tmpny051d_c.mp4",
  "https://replicate.delivery/xezq/mryc62SvSpp3NpOedDNQD7njZlmd5re5e8ClPltdEwWF7BopA/tmp7uoz8hjv.mp4",
  "https://replicate.delivery/xezq/ZdvQyoLLXgKNKtgU8Kdy5HBdIDaHVTl4Iws4zufK38e4WrGVA/tmpov6h3v3s.mp4",
  "https://replicate.delivery/xezq/G2SgiLJaDrYpGJnBezzhrqwdA7Z6cO4PHUKrkBbuFaaZwPsKA/tmpmb4xgltj.mp4",
  "https://replicate.delivery/xezq/L4f180LhnhSCHihiHQ1vX7ZfEG7XOK2WCKNlGGd81xGr7v4UA/tmpeywvj5rm.mp4",
  "https://replicate.delivery/xezq/TdE8PqxZMNKOABO3KIok1RVXuXxEBDJcDrWI9ewQhMAeDw4UA/tmpt6y3sy01.mp4",
  "https://replicate.delivery/xezq/BxhvfwlsVtXnByBxMw7lxS2hSPVZJ1Uc6urvVtQqx0eXIw4UA/tmpjj2pfqsg.mp4",
  "https://replicate.delivery/xezq/ucdza5hh5hJlA92SgLwAen9EQiSxXmuJj38FrvJdvxfnfR9pA/tmplwb84b03.mp4",
  "https://replicate.delivery/xezq/KUdYQBcySHanKZzAiCcz7DRqCZ59QLp6zKUvkzUBDPUueMsKA/tmpyol7xiil.mp4",
  "https://replicate.delivery/xezq/P33ke99FZTTWDKYainJQXizgxr7NxAMBYlK4QemCm0dsigYVA/tmp1l37lz4s.mp4",
  "https://replicate.delivery/xezq/nQec5ARx2fnWXE5dRUNwHizbSZ9uH5IXeZSsqF27M7XQWBxqA/tmpho4736hc.mp4",
  "https://replicate.delivery/xezq/AdX9KfAKEs1qMidbHzseSmefdOtfIEBg9HrulHeCf6tRzYQsKA/tmpvphmgf3e.mp4",
  "https://replicate.delivery/xezq/iY9PbAFJN2LJOBx1imI08Bhz4lJX1ZHBf5WakE1JGNtfKHfpA/tmp8ai_03z8.mp4",
  "https://replicate.delivery/xezq/VleZE1vtsJyLfUxuKcYzsngzUUFrgeeLlqsQdnLdG7sFNd8TB/tmpva5kimdq.mp4",
  "https://replicate.delivery/xezq/ecejocRJQkkUGUPfWnMf7AIAIlraKuSCd2bdkoeLdqflj1xPF/tmphuw78wy3.mp4",
  "https://replicate.delivery/xezq/xKos9IcrsrqrId3eA5bVZxD5m5jWRednYoECfsSaqASM9OeTB/tmpqa0xpc63.mp4",
  "https://replicate.delivery/xezq/l8u0QcK58uIQF5IUyYRQeMi0eAjISqU01o6XvY6PWqmEcrGVA/tmp9pdz1bfu.mp4",
  "https://replicate.delivery/xezq/NohwkQMfXYWKAS5QciHGFng9b9H0wVEbvol7WJAEmkN8MixKA/tmp5hxzqzw4.mp4",
  "https://replicate.delivery/xezq/xMBu3hve9jUTUyeijFeZNPBWT9sAxaQ0shKrsxltREUHdgGrA/tmp5teqjfsk.mp4",
  "https://replicate.delivery/xezq/OH4Ubefa0NiZwUneg3eS0LqcPYtNmxVJlSsZMFl6JpvFu4NWB/tmp6hdm26gd.mp4",
  "https://replicate.delivery/xezq/fXRgtIXwL2RfiEnoeaIZKfgVee8q8FUFqHxlcLCtrPonAl3YF/tmplcgybmlt.mp4",
  "https://replicate.delivery/xezq/ClgDLn4vlLosCtQeNCBMfAFxFefS9CwjWr6XkppFrQ2saYoTB/tmp4csnp1gw.mp4",
  "https://replicate.delivery/xezq/DfgnKlBwYW38OSSOyeeJ2seBqpDLjX1e7FxJxXFpahkDfhhOF/tmp0ht3v7vp.mp4",
  "https://replicate.delivery/xezq/NLybk0ySU6qKANmOJ5xzKJx28Q8x5rk4PZey7eeoC7qiIg5pA/tmpbufkswx1.mp4",
  "https://replicate.delivery/xezq/yAfwoReEDZl39EeInfkM7nufcc7eeAkB02POD28SGYX7wFYeUA/tmpjeapk4og.mp4",
  "https://replicate.delivery/xezq/CSPm6I9uOR45JFzXKX6wQKXaEUKE08x4wl0MhGoSDgWEDMPF/tmpeahvppel.mp4",
  "https://replicate.delivery/xezq/fjGfQQVNPvjCeJCp2eXRMqzdwBUxKmB4hQcQyBTpVH1NAKzTB/tmpm5vefpnt.mp4",
  "https://replicate.delivery/xezq/LEuAxRIjDJKtEBtjJqz7fys54ONyDRUAK3jNxNwCEkKAdZeUA/tmpy89a70ke.mp4",
  "https://replicate.delivery/xezq/eRwX7pg1AIUjFC148S2JpVn2hWS5r9dULdxm15dOMfxd54GVA/tmpou9td0s5.mp4",
  "https://replicate.delivery/xezq/RrB0vIXdP7YMHx6NJqV9plIbwJ85Rktes9R26txTnjIjCMoKA/tmpoo4_l9xh.mp4",
  "https://replicate.delivery/xezq/9uK7fv8Ahn3Kbam2dLehxXjxzsFiedPdWG086eeBRPdDZLEnC/tmp_07jswu0.mp4",
  "https://replicate.delivery/xezq/XYrfXEU8EJQjDqcHG7Pt6fDlIKQWyleZuRV58eEtX3IxaGiTB/tmpunco8ebh.mp4",
  "https://replicate.delivery/xezq/eOagYC8PA4zYRajq2EjDbTtO7120oD4AHFAlmhC3NFlLHRcKA/tmp5bp3oxgh.mp4",
  "https://replicate.delivery/xezq/D0S2kFge6F1KXahOjV77ifxqLb5PFp8y1vHayeCZhgJgmJxpA/tmpooyiogo4.mp4",
  "https://replicate.delivery/xezq/fLIizBaYWBWwBKFAL7JRKQsfmqSeHfI5fQH8RitXfQ7w65eiKA/output.mp4",
  "https://replicate.delivery/xezq/PNTe7b9VBLxFPKB35GDhzrMYaph7WKspkexwtxeRPxJqfuXUB/output.mp4",
  "https://replicate.delivery/xezq/j71Ii2YrWfRpGilHFUkqMSPhrV9rPu0ee4EiSV1H7CD3PhvqA/output_30fps.mp4",
  "https://replicate.delivery/xezq/FxmknjDK8SqlBRoWWFnfxsOMwnuUMnpj1w6joZljM3D6m4iKA/output.mp4",
  "https://replicate.delivery/xezq/cGWRn81blVZGKFocNncx5MHgLtvho90zLaTn3gtQpySLxzTF/tmppcyih9pj.mp4",
  "https://replicate.delivery/xezq/ljc6TxrXY1bOLhvnbL3ukFK6T2161ptYcIgLyiFXReNCknnKA/tmp3s3i7nns.mp4",
  "https://replicate.delivery/xezq/ZpcwMRwOww7iKJ6f7Tqv9XxXDS9liCOQokaIDbUFwIF7lnnKA/tmp2sgncxf1.mp4",
  "https://replicate.delivery/xezq/17c3JG1SzH6NCduMiKp1Cxyqvpad0GXRf507Pqq5GOqNsZsKA/tmpg4w3kyjz.mp4",
  "https://replicate.delivery/xezq/ssJoLMbvdLIgIRdWaDY26XEmUJmQfH1Af6He1Z2XHLtqIoxqA/tmpp3vqjumh.mp4",
  "https://replicate.delivery/xezq/Fez1ZQegP1tyC0f18Qr1WyVpVeGWEYS0GPemdGCI9ayvDhGrC/tmp4s6vgmpf.mp4",
  "https://replicate.delivery/xezq/EnlmUfQrjDTzGaMakDffU94SfsNnrSIcGdK9AlRGi0uDBRjVB/tmptifj8gul.mp4",
  "https://replicate.delivery/xezq/Lzq3zWbgAH7vCRFyf7xef9AEqfNOK1fYk2ZJfprfEdHaZUasKA/tmpmpzrg66r.mp4",
  "https://replicate.delivery/xezq/VUdJthWRZxJMOdVL2lBFR1K9J2nLUUjngDiyagPbjAejvehVA/tmplkv0_7wi.mp4",
  "https://replicate.delivery/xezq/OZDRKjW01V54BNhZll1lo9CVPIU2pbD8yGGSvFRy1MAAmfwKA/tmpoahhx6gd.mp4",
  "https://replicate.delivery/xezq/zsD7zCTyuL7eW68mhR2To8DZoS4OT2BQd2oXP3ZOK1U0MfhVA/tmp8un0w8yy.mp4",
  "https://replicate.delivery/xezq/ZuMzPe5GtOxtVK59EO2a4F8hDrDESMTPUshVB86dffS538DrA/tmp55nt3l_c.mp4",
  "https://replicate.delivery/xezq/qZJqNQUGOQZBL11GKYyMf6j9zebygo2QxsbmGztcezBQ48DrA/tmpycq8b_l0.mp4",
  "https://replicate.delivery/xezq/C5lKtwHSBloDJB5LmasPpsRZdVUgqTnwdK2BjASYOe4rRfhVA/tmp69__x443.mp4",
  "https://replicate.delivery/xezq/Xe3HUYeQMIlJzEmRW2RFdH6Vifeg8Z1PkAmhlc5eS0pqrLQsC/tmpzqz2e09l.mp4",
  "https://replicate.delivery/xezq/FvR9AwLdbzIKNRQ0y2pwEDf0BgF0Ez1sQE8eZteW1J7XcfGWB/tmpan00ukuq.mp4",
  "https://replicate.delivery/xezq/ONOR9TevnKSldi2E5yObpoByieYhuGDePh1Fl0DA0tiOoWipA/tmpwfxoip00.mp4",
  "https://replicate.delivery/xezq/Up5cK7j9ZgJALVZjTvCfQeDWibaoABcjh6qfGlzyXLEwoWipA/tmps5gv28ej.mp4",
  "https://replicate.delivery/xezq/M8krcmsXwRrSK1DuufUjOpLjfHs9KGF7zgespA2hugJ1V7DrA/tmpc_m94ikd.mp4",
  "https://replicate.delivery/xezq/GPXMuvvmsJp9P5zhA8iXvkFVETpel5tiMdhtUhWjsBD8RfhVA/tmp3_rwznmf.mp4",
  "https://replicate.delivery/xezq/SKUPcbemc2VNE6B3ykiKKIYn3w7S3BQaMg4YIf4kN0zHoeDrA/tmpjk5lvmjy.mp4",
  "https://replicate.delivery/xezq/FN4X6kFZhRKEMJNH8pDa1mtg3kcGdfDrINIg38FCqalbWfhVA/tmp3q56smse.mp4",
  "https://replicate.delivery/xezq/10DE4e0psL1EBiQqZA9rc7JdCBmdGKpc0NoeaJLfzuRic9DrA/tmpaxus5iln.mp4",
  "https://replicate.delivery/xezq/OdK1NDj7x6rwNdfLRGCHzrz3OS20dbBlW6dGI73Y09eYzeDrA/tmpujshdc0p.mp4",
  "https://replicate.delivery/xezq/7spwly6repRhAqQZw5FbNGkexFMxLIdJ83r0Q11sJ2Tw0acVA/output.mp4",
  "https://replicate.delivery/xezq/IgZWgrUkXsL3Ihf7ftHhOiaeE9K8Hcv6e7H1jLxOPYXVdrxVB/output.mp4",
  "https://replicate.delivery/xezq/vMy749nMtroXLVHYAfNTKXEruMVcCyEZSYockVN7j0yTcNuKA/output.mp4",
  "https://replicate.delivery/xezq/4ZbQqkJIuuLtCpMMz2Sfj0uFFytvCm562B1WZiPftamgwycVA/output.mp4",
  "https://replicate.delivery/xezq/oUGT0YvX1tqoGFPvBukW4DFSXvJEptXX8U6Kt36jkhaBrFXF/output.mp4",
  "https://replicate.delivery/czjl/QAYtRuTghFrPH1qgCx3C0a8WrQggBsGJ0yCCZLsSVuye8pEKA/tmpkq6t05vs.mp4",
  "https://replicate.delivery/czjl/DPQLyUb0KH6eKafJafajZJMYUkdK5NFpLrzHNKqE1Jv1MoSoA/tmp501exz8a.mp4",
  "https://replicate.delivery/czjl/qQJQ3L2pDgIaPBET4YjZergziQu776uXRJnllTJCOSynDqEKA/tmpmxs3uoat.mp4",
  "https://replicate.delivery/czjl/DW5uByT9o5K3EpK4aryN5lT0Le8o9wMrDdElpxYtx3FiFqEKA/tmp850eff5d.mp4",
  "https://replicate.delivery/czjl/Q6jWYYtvlAauJlWqS5oYvzaWZrBERcXTXb9Evyp3yH1sCVCF/tmpn8nwwjak.mp4",
];

/**
 * Get a random example video URL from the list
 * @return {string} A random example video URL
 */
function getRandomExampleVideoUrl(): string {
  const randomIndex = Math.floor(Math.random() * EXAMPLE_VIDEO_URLS.length);
  return EXAMPLE_VIDEO_URLS[randomIndex] || EXAMPLE_VIDEO_URLS[0] || "";
}

/**
 * TEST FUNCTION: Mimics credit deduction and job creation
 * without calling Replicate API. Use this for testing credit
 * charging functionality without incurring Replicate costs.
 *
 * Usage in Android app:
 * Change the function name from "callReplicateVeoAPIV2" to
 * "testCallReplicateVeoAPIV2" in FirebaseRepositories.kt
 */
export const testCallReplicateVeoAPIV2 = onCall<GenerateRequest>(
  async ({data, auth}) => {
    if (!auth) {
      throw new Error("Missing auth.");
    }

    const userId = data.userId || auth.uid;

    // 1. Ensure user document exists, then check and deduct credits
    const userRef = firestore.collection("users").doc(userId);
    let userDoc = await userRef.get();

    // Create user document if it doesn't exist
    if (!userDoc.exists) {
      await userRef.set({
        credits: 0,
        created_at: admin.firestore.FieldValue.serverTimestamp(),
      });
      userDoc = await userRef.get();
    }

    const currentCredits = (userDoc.data()?.credits as number) || 0;
    const cost = data.cost || 0;

    if (currentCredits < cost) {
      throw new Error(
        `Insufficient credits. Required: ${cost}, Available: ${currentCredits}`,
      );
    }

    // Deduct credits immediately (same as production)
    await userRef.update({
      credits: admin.firestore.FieldValue.increment(-cost),
    });

    console.log(
      `[TEST] Deducted ${cost} credits from user ${userId}. ` +
        `Remaining: ${currentCredits - cost}`,
    );

    // Generate a fake prediction ID for testing
    const fakePredictionId =
      `test_${Date.now()}_${Math.random().toString(36).substring(7)}`;

    // Create job document with TEST status (so you can identify test jobs)
    await writeJobDocument({
      uid: userId,
      jobId: fakePredictionId,
      payload: {
        prompt: data.prompt,
        model_id: data.modelId,
        model_name: data.replicateName,
        duration_seconds: data.durationSeconds,
        aspect_ratio: data.aspectRatio,
        status: "PROCESSING", // Will be updated to COMPLETE after delay
        replicate_prediction_id: fakePredictionId,
        cost: cost,
        credits_deducted: cost,
        enable_audio: data.enableAudio || false,
        first_frame_url: data.firstFrameUrl || null,
        last_frame_url: data.lastFrameUrl || null,
        negative_prompt: data.negativePrompt || null,
        created_at: admin.firestore.FieldValue.serverTimestamp(),
        is_test: true, // Flag to identify test jobs
      },
    });

    // Simulate video generation by updating status to COMPLETE
    // after 5 seconds. In production, this would be done by
    // Replicate webhook. Use a random example video URL.
    setTimeout(async () => {
      const randomVideoUrl = getRandomExampleVideoUrl();
      await firestore
        .collection("users")
        .doc(userId)
        .collection("jobs")
        .doc(fakePredictionId)
        .update({
          status: "COMPLETE",
          storage_url: randomVideoUrl,
          preview_url: randomVideoUrl,
          completed_at: admin.firestore.FieldValue.serverTimestamp(),
        });
      console.log(
        `[TEST] Job ${fakePredictionId} marked as COMPLETE ` +
          `with video: ${randomVideoUrl}`,
      );
    }, 5000); // 5 second delay to simulate generation

    return {
      predictionId: fakePredictionId,
      status: "PROCESSING",
      webhook: "",
      message:
        "TEST MODE: Credits deducted, job created. " +
        "Will complete in 5 seconds with random example video.",
    };
  },
);

/**
 * Helper function to add test credits to a user account
 *
 * Usage:
 * - Call from Android app or Firebase Console
 * - data: { userId: "user_id", credits: 1000 }
 */
export const addTestCredits = onCall<{userId?: string; credits: number}>(
  async ({data, auth}) => {
    if (!auth) {
      throw new Error("Missing auth.");
    }

    const userId = data.userId || auth.uid;
    const creditsToAdd = data.credits || 1000;

    const userRef = firestore.collection("users").doc(userId);
    let userDoc = await userRef.get();

    // Create user document if it doesn't exist
    if (!userDoc.exists) {
      await userRef.set({
        credits: 0,
        created_at: admin.firestore.FieldValue.serverTimestamp(),
      });
      userDoc = await userRef.get();
    }

    const currentCredits = (userDoc.data()?.credits as number) || 0;

    // Add credits
    await userRef.update({
      credits: admin.firestore.FieldValue.increment(creditsToAdd),
    });

    const newCredits = currentCredits + creditsToAdd;

    console.log(
      `[TEST] Added ${creditsToAdd} credits to user ${userId}. ` +
        `New balance: ${newCredits}`,
    );

    return {
      success: true,
      userId,
      creditsAdded: creditsToAdd,
      previousBalance: currentCredits,
      newBalance: newCredits,
    };
  },
);

export const generateVideoEffect = onCall(async ({data, auth}) => {
  if (!auth) {
    throw new Error("Missing auth.");
  }

  const required = [
    "imageUrl",
    "effectId",
    "effectPrompt",
    "userId",
    "appVersion",
  ];
  for (const key of required) {
    if (!data[key as keyof EffectRequest]) {
      throw new Error(`${key} is required`);
    }
  }

  const jobId = firestore
    .collection("users")
    .doc(data.userId)
    .collection("jobs")
    .doc().id;

  await writeJobDocument({
    uid: data.userId,
    jobId,
    payload: {
      prompt: data.effectPrompt,
      model_id: data.effectId,
      model_name: "effect",
      duration_seconds: 0,
      aspect_ratio: data.aspectRatio ?? "1:1",
      status: "QUEUED", // Match app's VideoJobStatus
      input_image: data.imageUrl,
      created_at: admin.firestore.FieldValue.serverTimestamp(),
    },
  });

  return {
    jobId,
    status: "QUEUED",
  };
});

/**
 * Persist a job document for the given user.
 */
async function writeJobDocument({
  uid,
  jobId,
  payload,
}: {
  uid: string;
  jobId: string;
  payload: Record<string, unknown>;
}) {
  await firestore
    .collection("users")
    .doc(uid)
    .collection("jobs")
    .doc(jobId)
    .set(payload, {merge: true});
}

/**
 * Process webhook update for a job.
 * Updates job status, handles credits refund on failure, and sends
 * notifications.
 * @param {string} userId - User ID who owns the job
 * @param {string} jobId - Job ID to update
 * @param {ReplicatePrediction} prediction - Prediction data from Replicate
 * @param {admin.firestore.Firestore} firestore - Firestore instance
 * @return {Promise<void>} Promise that resolves when update is complete
 */
async function processWebhookUpdate(
  userId: string,
  jobId: string,
  prediction: ReplicatePrediction,
  firestore: admin.firestore.Firestore,
): Promise<void> {
  const jobRef = firestore
    .collection("users")
    .doc(userId)
    .collection("jobs")
    .doc(jobId);

  const jobDoc = await jobRef.get();

  if (!jobDoc.exists) {
    console.warn(`Job ${jobId} not found for user ${userId}`);
    return;
  }

  const updateData: Record<string, unknown> = {
    updated_at: admin.firestore.FieldValue.serverTimestamp(),
  };

  if (prediction.status === "succeeded") {
    // Job completed successfully
    const outputUrl = Array.isArray(prediction.output) ?
      prediction.output[0] :
      prediction.output;

    if (outputUrl && typeof outputUrl === "string") {
      updateData.status = "COMPLETE";
      updateData.storage_url = outputUrl;
      updateData.preview_url = outputUrl;
      updateData.completed_at =
        admin.firestore.FieldValue.serverTimestamp();

      console.log(`Job ${jobId} completed. Video URL: ${outputUrl}`);

      // Send FCM notification
      await sendJobCompleteNotification(userId, jobId, outputUrl);
    } else {
      console.error(`Job ${jobId} succeeded but no output URL`);
      updateData.status = "FAILED";
      updateData.error_message = "Job completed but no output URL";
    }
  } else if (
    prediction.status === "failed" ||
    prediction.status === "canceled"
  ) {
    // Job failed - refund credits
    const jobData = jobDoc.data();
    const creditsDeducted = (jobData?.credits_deducted as number) || 0;

    if (creditsDeducted > 0 && userId) {
      const userRef = firestore.collection("users").doc(userId);
      await userRef.update({
        credits: admin.firestore.FieldValue.increment(creditsDeducted),
      });
      console.log(
        `Refunded ${creditsDeducted} credits to user ${userId} ` +
          `for failed job ${jobId}`,
      );
    }

    // Job failed
    updateData.status = "FAILED";
    updateData.error_message = prediction.error || "Job failed";
    updateData.failed_at = admin.firestore.FieldValue.serverTimestamp();
    updateData.credits_refunded = creditsDeducted;

    console.log(`Job ${jobId} failed: ${prediction.error}`);
  } else if (
    prediction.status === "starting" ||
    prediction.status === "processing"
  ) {
    // Still processing, update timestamp but keep status
    updateData.status = "PROCESSING";
    console.log(`Job ${jobId} still processing...`);
  }

  // Update Firestore document
  await jobRef.update(updateData);
}

/**
 * Webhook endpoint that Replicate calls when prediction status changes.
 * This is more efficient than polling - Replicate calls us directly.
 * Must allow unauthenticated invocations since Replicate calls it.
 */
export const replicateWebhook = onRequest(
  {
    secrets: [replicateToken],
    // Allow unauthenticated invocations for Replicate webhooks
    invoker: "public",
  },
  async (req, res) => {
    // Replicate sends POST requests to webhook
    if (req.method !== "POST") {
      res.status(405).send("Method not allowed");
      return;
    }

    try {
      const prediction = req.body as ReplicatePrediction;

      if (!prediction || !prediction.id) {
        console.error("Invalid webhook payload:", req.body);
        res.status(400).send("Invalid payload");
        return;
      }

      console.log(
        `Webhook received for prediction ${prediction.id}, ` +
          `status: ${prediction.status}`,
      );

      // Find the job using prediction lookup (avoids collectionGroup index)
      const predictionLookup = await firestore
        .collection("predictions")
        .doc(prediction.id)
        .get();

      if (!predictionLookup.exists) {
        console.warn(`No prediction lookup found for ${prediction.id}`);
        // Fallback: try collectionGroup (may fail if index missing)
        try {
          const jobsSnapshot = await firestore
            .collectionGroup("jobs")
            .where("replicate_prediction_id", "==", prediction.id)
            .limit(1)
            .get();

          if (jobsSnapshot.empty) {
            console.warn(`No job found for prediction ${prediction.id}`);
            res.status(200).send("OK");
            return;
          }

          const jobDoc = jobsSnapshot.docs[0];
          const userId = jobDoc.ref.parent.parent?.id;

          if (!userId) {
            console.error(`No userId found for job ${jobDoc.id}`);
            res.status(200).send("OK");
            return;
          }

          // Process with fallback method
          await processWebhookUpdate(
            userId,
            jobDoc.id,
            prediction,
            firestore,
          );
          res.status(200).send("OK");
          return;
        } catch (fallbackError) {
          console.error(
            `Fallback query also failed for ${prediction.id}:`,
            fallbackError,
          );
          res.status(200).send("OK");
          return;
        }
      }

      const lookupData = predictionLookup.data();
      const userId = lookupData?.user_id as string;
      const jobId = lookupData?.job_id as string;

      if (!userId || !jobId) {
        console.error(
          `Invalid prediction lookup data for ${prediction.id}`,
        );
        res.status(200).send("OK");
        return;
      }

      // Process webhook update
      await processWebhookUpdate(userId, jobId, prediction, firestore);

      // Always return 200 to Replicate (even if we had errors)
      // This prevents Replicate from retrying
      res.status(200).send("OK");
    } catch (error) {
      console.error("Error processing webhook:", error);
      // Still return 200 to prevent Replicate from retrying
      // Log the error for debugging
      res.status(200).send("OK");
    }
  },
);

/**
 * Send FCM notification when job completes.
 * @param {string} userId - User ID to send notification to
 * @param {string} jobId - Job ID that completed
 * @param {string} videoUrl - URL of the completed video
 * @return {Promise<void>} Promise that resolves when notification is sent
 */
async function sendJobCompleteNotification(
  userId: string,
  jobId: string,
  videoUrl: string,
): Promise<void> {
  try {
    // Get user's FCM token from Firestore (if stored)
    const userDoc = await firestore.collection("users").doc(userId).get();
    const fcmToken = userDoc.data()?.fcm_token as string | undefined;

    if (!fcmToken) {
      console.log(`No FCM token for user ${userId}`);
      return;
    }

    const message = {
      token: fcmToken,
      notification: {
        title: "Video Ready!",
        body: "Your video generation is complete.",
      },
      data: {
        type: "video_complete",
        job_id: jobId,
        video_url: videoUrl,
      },
      android: {
        priority: "high" as const,
      },
    };

    await admin.messaging().send(message);
    console.log(`Notification sent to user ${userId} for job ${jobId}`);
  } catch (error) {
    console.error("Failed to send notification:", error);
    // Don't throw - notification failure shouldn't break the workflow
  }
}
