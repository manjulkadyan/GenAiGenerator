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

    // Webhook URL - will be set after first deployment
    // Format: https://{region}-{project}.cloudfunctions.net/replicateWebhook
    // You can set this as environment variable: WEBHOOK_URL
    // Or it will be auto-detected from function URL after deployment
    const webhookUrl = process.env.WEBHOOK_URL ||
      `https://us-central1-${process.env.GCLOUD_PROJECT || "your-project-id"}.cloudfunctions.net/replicateWebhook`;

    const payload = {
      version: data.replicateName,
      input: {
        prompt: data.prompt,
        duration: data.durationSeconds,
        aspect_ratio: data.aspectRatio,
        first_frame: data.firstFrameUrl,
        last_frame: data.lastFrameUrl,
      },
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
      throw new Error(`Replicate error: ${text}`);
    }

    const result = (await response.json()) as ReplicateResponse;
    await writeJobDocument({
      uid: data.userId,
      jobId: result.id,
      payload: {
        prompt: data.prompt,
        model_id: data.modelId,
        model_name: data.replicateName,
        duration_seconds: data.durationSeconds,
        aspect_ratio: data.aspectRatio,
        status: "PROCESSING", // Match app's VideoJobStatus
        replicate_prediction_id: result.id,
        created_at: admin.firestore.FieldValue.serverTimestamp(),
      },
    });

    return {
      predictionId: result.id,
      status: result.status,
      webhook: result.urls?.get ?? "",
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
 * Webhook endpoint that Replicate calls when prediction status changes.
 * This is more efficient than polling - Replicate calls us directly.
 */
export const replicateWebhook = onRequest(
  {secrets: [replicateToken]},
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

      console.log(`Webhook received for prediction ${prediction.id}, status: ${prediction.status}`);

      // Find the job document by replicate_prediction_id
      const jobsSnapshot = await firestore
        .collectionGroup("jobs")
        .where("replicate_prediction_id", "==", prediction.id)
        .limit(1)
        .get();

      if (jobsSnapshot.empty) {
        console.warn(`No job found for prediction ${prediction.id}`);
        res.status(200).send("OK"); // Still return 200 to Replicate
        return;
      }

      const jobDoc = jobsSnapshot.docs[0];
      const userId = jobDoc.ref.parent.parent?.id;

      if (!userId) {
        console.error(`No userId found for job ${jobDoc.id}`);
        res.status(200).send("OK");
        return;
      }

      // Update Firestore based on Replicate status
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
          updateData.completed_at = admin.firestore.FieldValue.serverTimestamp();

          console.log(`Job ${jobDoc.id} completed. Video URL: ${outputUrl}`);

          // Send FCM notification
          await sendJobCompleteNotification(userId, jobDoc.id, outputUrl);
        } else {
          console.error(`Job ${jobDoc.id} succeeded but no output URL`);
          updateData.status = "FAILED";
          updateData.error_message = "Job completed but no output URL";
        }
      } else if (
        prediction.status === "failed" ||
        prediction.status === "canceled"
      ) {
        // Job failed
        updateData.status = "FAILED";
        updateData.error_message = prediction.error || "Job failed";
        updateData.failed_at = admin.firestore.FieldValue.serverTimestamp();

        console.log(`Job ${jobDoc.id} failed: ${prediction.error}`);
      } else if (
        prediction.status === "starting" ||
        prediction.status === "processing"
      ) {
        // Still processing, update timestamp but keep status
        updateData.status = "PROCESSING";
        console.log(`Job ${jobDoc.id} still processing...`);
      }

      // Update Firestore document
      await jobDoc.ref.update(updateData);

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
