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

    // 0. Validate duration against model's allowed durations
    if (data.modelId) {
      const modelDoc = await firestore
        .collection("models")
        .doc(data.modelId)
        .get();
      if (modelDoc.exists) {
        const modelData = modelDoc.data();
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

    // Build input payload - include audio if enabled
    const input: Record<string, unknown> = {
      prompt: data.prompt,
      duration: data.durationSeconds,
      aspect_ratio: data.aspectRatio,
    };

    if (data.firstFrameUrl) {
      input.first_frame = data.firstFrameUrl;
    }
    if (data.lastFrameUrl) {
      input.last_frame = data.lastFrameUrl;
    }
    if (data.enableAudio) {
      // Support different audio parameter names
      input.generate_audio = true;
      input.enable_audio = true;
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
