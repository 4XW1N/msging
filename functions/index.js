const { initializeApp } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");
const { onDocumentCreated } = require("firebase-functions/v2/firestore");

initializeApp();

exports.sendMessageNotification = onDocumentCreated(
  "rooms/{roomId}/messages/{messageId}",
  async (event) => {
    const snap = event.data;
    if (!snap) return;

    const roomId = event.params.roomId;
    const data = snap.data();
    const senderId = data.sender;
    const text = data.text;

    if (!senderId || !text) return;

    const db = getFirestore();
    const messaging = getMessaging();

    let senderName = "Someone";
    const senderDoc = await db.collection("users").doc(senderId).get();
    if (senderDoc.exists) {
      senderName = senderDoc.data().name || "Someone";
    }

    let recipientIds = [];
    const isGroup = roomId.length > 30 && !roomId.includes("_");

    if (isGroup) {
      const groupDoc = await db.collection("groups").doc(roomId).get();
      if (groupDoc.exists) {
        const members = groupDoc.data().members || [];
        recipientIds = members.filter((uid) => uid !== senderId);
      }
    } else {
      const parts = roomId.split("_");
      recipientIds = parts.filter((uid) => uid !== senderId);
    }

    if (recipientIds.length === 0) return;

    const tokens = [];
    for (const uid of recipientIds) {
      const userDoc = await db.collection("users").doc(uid).get();
      if (userDoc.exists) {
        const fcmToken = userDoc.data().fcmToken;
        if (fcmToken) tokens.push(fcmToken);
      }
    }

    if (tokens.length === 0) return;

    const roomName = isGroup
      ? (await db.collection("groups").doc(roomId).get()).data()?.name || "Group"
      : senderName;

    const truncatedText = text.length > 100 ? text.substring(0, 100) + "..." : text;

    const message = {
      notification: {
        title: isGroup ? `${roomName} - ${senderName}` : senderName,
        body: truncatedText,
      },
      data: {
        roomId: roomId,
        roomName: roomName,
        isGroup: String(isGroup),
      },
      tokens: tokens,
    };

    try {
      const response = await messaging.sendEachForMulticast(message);
      const failedTokens = [];

      response.responses.forEach((res, idx) => {
        if (!res.success) {
          failedTokens.push(tokens[idx]);
        }
      });

      if (failedTokens.length > 0) {
        const batch = db.batch();
        for (const token of failedTokens) {
          const userSnapshot = await db
            .collection("users")
            .where("fcmToken", "==", token)
            .get();
          userSnapshot.forEach((doc) => {
            batch.update(doc.ref, { fcmToken: null });
          });
        }
        await batch.commit();
      }
    } catch (error) {
      console.error("Error sending notification:", error);
    }
  }
);
