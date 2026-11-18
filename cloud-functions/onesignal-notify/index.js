/**
 * PROMPTADMIN — PRIVILEGED FIREBASE CLOUD FUNCTIONS (GEN 2)
 *
 * Deployment Instructions:
 * 1. Set your private OneSignal REST API Key in Secret Manager (NEVER in the Android app):
 *    firebase functions:secrets:set ONESIGNAL_REST_API_KEY
 * 2. Deploy functions:
 *    firebase deploy --only functions
 */

const { onRequest } = require("firebase-functions/v2/https");
const { defineSecret } = require("firebase-functions/params");
const admin = require("firebase-admin");

admin.initializeApp();
const db = admin.firestore();
const ONESIGNAL_REST_API_KEY = defineSecret("ONESIGNAL_REST_API_KEY");

const ADMIN_EMAIL_ALLOWLIST = ["sfforhadvai@gmail.com"];

/**
 * Verifies the caller's Firebase ID Token and checks that they signed in with Google,
 * have a verified email, and are in ADMIN_EMAIL_ALLOWLIST.
 */
async function verifyAdminCaller(req) {
  const authHeader = req.headers.authorization || "";
  if (!authHeader.startsWith("Bearer ")) {
    throw new Error("Missing or invalid Authorization Bearer token.");
  }
  const idToken = authHeader.substring("Bearer ".length).trim();
  const decoded = await admin.auth().verifyIdToken(idToken);
  const provider = decoded.firebase && decoded.firebase.sign_in_provider;
  const email = (decoded.email || "").toLowerCase();
  if (
    provider !== "google.com" ||
    decoded.email_verified !== true ||
    !ADMIN_EMAIL_ALLOWLIST.includes(email)
  ) {
    throw new Error("Access denied: caller is not an allowlisted Google administrator.");
  }
  return { uid: decoded.uid, email: decoded.email || "", role: "OWNER" };
}

/**
 * Secure Server-Side OneSignal Push Notification Dispatcher
 */
exports.sendOneSignalNotification = onRequest(
  { secrets: [ONESIGNAL_REST_API_KEY], cors: true },
  async (req, res) => {
    try {
      if (req.method !== "POST") {
        return res.status(405).json({ error: "Method Not Allowed" });
      }
      const caller = await verifyAdminCaller(req);
      const {
        notificationId,
        oneSignalAppId,
        title,
        message,
        targetAudience,
        imageId,
        imageThumbnailUrl,
        deepLink,
        scheduledIso
      } = req.body;

      if (!oneSignalAppId || !title || !message || !deepLink) {
        return res.status(400).json({ error: "Missing required notification fields." });
      }

      const segmentMap = {
        ALL_SUBSCRIBED: ["Subscribed Users"],
        ACTIVE_USERS: ["Active Users"],
        HIGH_ENGAGEMENT: ["Engaged Users"],
        NEW_USERS: ["New Users"]
      };

      const payload = {
        app_id: oneSignalAppId,
        headings: { en: title },
        contents: { en: message },
        included_segments: segmentMap[targetAudience] || ["Subscribed Users"],
        app_url: deepLink,
        data: {
          imageId: imageId || "",
          deepLink: deepLink
        }
      };

      if (imageThumbnailUrl && /^https?:\/\//.test(imageThumbnailUrl)) {
        payload.big_picture = imageThumbnailUrl;
        payload.ios_attachments = { id1: imageThumbnailUrl };
      }

      if (scheduledIso) {
        payload.send_after = scheduledIso;
      }

      const response = await fetch("https://api.onesignal.com/notifications", {
        method: "POST",
        headers: {
          "Content-Type": "application/json; charset=utf-8",
          "Authorization": `Key ${ONESIGNAL_REST_API_KEY.value()}`
        },
        body: JSON.stringify(payload)
      });

      const resultJson = await response.json();
      const status = response.ok ? "DISPATCHED_PROVIDER_CONFIRMED" : "FAILED";
      const deliveryDetails = response.ok
        ? `Provider ID: ${resultJson.id || "N/A"} | Recipients: ${resultJson.recipients ?? 0}`
        : `OneSignal Error: ${JSON.stringify(resultJson.errors || resultJson)}`;

      if (notificationId) {
        await db.collection("notifications").doc(notificationId).set(
          {
            sendingStatus: status,
            deliveryDetails,
            providerNotificationId: resultJson.id || "",
            recipientsCount: resultJson.recipients || 0,
            updatedAt: admin.firestore.FieldValue.serverTimestamp()
          },
          { merge: true }
        );
      }

      await db.collection("adminActivityLogs").add({
        actorUid: caller.uid,
        adminEmail: caller.email,
        adminRole: caller.role,
        actionType: "ONESIGNAL_PUSH_DISPATCH",
        targetEntity: `notifications/${notificationId || "direct"}`,
        summary: `Dispatched push "${title}" -> ${deepLink} (${status})`,
        createdAtMillis: Date.now()
      });

      return res.status(response.ok ? 200 : 502).json({
        status,
        deliveryDetails,
        providerResponse: resultJson
      });
    } catch (err) {
      return res.status(403).json({
        status: "FAILED",
        error: err.message || "Unauthorized or failed OneSignal dispatch"
      });
    }
  }
);

/**
 * Secure Firebase Authentication Account Administration (Disable / Restore User)
 */
exports.manageUserAccount = onRequest({ cors: true }, async (req, res) => {
  try {
    if (req.method !== "POST") {
      return res.status(405).json({ error: "Method Not Allowed" });
    }
    const caller = await verifyAdminCaller(req);
    const { targetUserId, disableAccount, reason } = req.body;
    if (!targetUserId) {
      return res.status(400).json({ error: "targetUserId is required" });
    }

    await admin.auth().updateUser(targetUserId, { disabled: Boolean(disableAccount) });
    const newStatus = disableAccount ? "DISABLED" : "ACTIVE";

    await db.collection("users").doc(targetUserId).set(
      {
        accountStatus: newStatus,
        moderationNote: reason || "",
        updatedAt: admin.firestore.FieldValue.serverTimestamp()
      },
      { merge: true }
    );

    await db.collection("adminActivityLogs").add({
      actorUid: caller.uid,
      adminEmail: caller.email,
      adminRole: caller.role,
      actionType: disableAccount ? "USER_ACCOUNT_DISABLED" : "USER_ACCOUNT_RESTORED",
      targetEntity: `users/${targetUserId}`,
      summary: `${disableAccount ? "Disabled" : "Restored"} user ${targetUserId}. Reason: ${reason || "Admin action"}`,
      createdAtMillis: Date.now()
    });

    return res.status(200).json({ success: true, accountStatus: newStatus });
  } catch (err) {
    return res.status(403).json({ error: err.message || "Operation failed" });
  }
});
