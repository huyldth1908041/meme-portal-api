package com.t1908e.memeportalapi.util;

import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;
import com.t1908e.memeportalapi.dto.NotificationDTO;


public class FirebaseUtil {
    public static void sendNotification(String to, NotificationDTO content) {
        Firestore firestore = FirestoreClient.getFirestore();
        firestore.collection(to).add(content);
    }
}
