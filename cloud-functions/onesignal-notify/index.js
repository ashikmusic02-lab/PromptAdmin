/**
 * PROMPTADMIN — PRIVILEGED FIREBASE CLOUD FUNCTIONS (GEN 2)
 *
 * Deployment Instructions:
 * 1. Note: Firebase Cloud Functions (Gen 2) and Google Cloud Secret Manager require
 *    the Firebase Blaze (pay-as-you-go) billing plan.
 * 2. Set your private OneSignal REST API Key in Secret Manager (NEVER in the Android app):
 *    firebase functions:secrets:set ONESIGNAL_REST_API_KEY
 * 3. Deploy functions:
 *    firebase deploy --only functions
 */

const { onRequest } = require("firebase-functions/v2/https");
const { defineSecret } = require("firebase-functions/params");
const admin = require("firebase-admin");

admin.initializeApp();
const db = admin.firestore();
const ONESIGNAL_REST_API_KEY = defineSecret("ONESIGNAL_REST_API_KEY");

/**
 * Verifies the caller's Firebase ID Token and checks that their adminUsers/{uid}
 * record is active and belongs to one of the allowed roles.
 */
async function verifyAdminCaller(req, allowedRoles) {
  const authHeader = req.headers.authorization || "";
  if (!authHeader.startsWith("Bearer ")) {
    throw new Error("Missing or invalid Authorization Bearer token.");
  }
  const idToken = authHeader.substring("Bearer ".length).trim();
  const decoded = await admin.auth().verifyIdToken(idToken);
  const adminSnap = await db.collection("adminUsers").doc(decoded.uid).get();
  if (!adminSnap.exists || adminSnap.data().active !== true) {
    throw new Error("Caller is not an active administrator.");
  }
  const role = adminSnap.data().role;
  if (!allowedRoles.includes(role)) {
    throw new Error(`Insufficient role (${role}). Required: ${allowedRoles.join(", ")}`);
  }
  return { uid: decoded.uid, email: decoded.email || "", role };
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
      const caller = await verifyAdminCaller(req, ["OWNER", "ADMINISTRATOR"]);
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
        adminUid: caller.uid,
        adminEmail: caller.email,
        adminRole: caller.role,
        actionType: "ONESIGNAL_PUSH_DISPATCH",
        targetEntity: `notifications/${notificationId || "direct"}`,
        summary: `Dispatched push "${title}" -> ${deepLink} (${status})`,
        createdAt: admin.firestore.FieldValue.serverTimestamp()
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
    const caller = await verifyAdminCaller(req, ["OWNER", "ADMINISTRATOR"]);
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
      adminUid: caller.uid,
      adminEmail: caller.email,
      adminRole: caller.role,
      actionType: disableAccount ? "USER_ACCOUNT_DISABLED" : "USER_ACCOUNT_RESTORED",
      targetEntity: `users/${targetUserId}`,
      summary: `${disableAccount ? "Disabled" : "Restored"} user ${targetUserId}. Reason: ${reason || "Admin action"}`,
      createdAt: admin.firestore.FieldValue.serverTimestamp()
    });

    return res.status(200).json({ success: true, accountStatus: newStatus });
  } catch (err) {
    return res.status(403).json({ error: err.message || "Operation failed" });
  }
});
