package com.hackydesk.thesaviour;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class FcmNotificationsSender {

    private static final String SHARED_PREFS_NAME = "FcmPrefs";
    private static final String FCM_KEY_PREF = "FcmServerKey";

    private String userFcmToken;
    private String title;
    private String body;
    private Context mContext;

    private RequestQueue requestQueue;
    private final String postUrl = "https://fcm.googleapis.com/fcm/send";
    private String fcmServerKey;

    public FcmNotificationsSender(String userFcmToken, String title, String body, Context mContext) {
        this.userFcmToken = userFcmToken;
        this.title = title;
        this.body = body;
        this.mContext = mContext;
        requestQueue = Volley.newRequestQueue(mContext);
    }

    public void SendNotifications() {
        fcmServerKey = getFcmServerKeyFromPrefs();
        if (fcmServerKey != null) {
            // Use the cached key
            sendNotificationWithKey();
        } else {
            // Fetch the key from Firestore and then send notification
            fetchFcmServerKey();
        }
    }

    private void fetchFcmServerKey() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference docRef = db.collection("apikey").document("tqj1osj3ZbcnltmYKEGQ");

        docRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (documentSnapshot.exists()) {
                    fcmServerKey = documentSnapshot.getString("fcmkey");
                    saveFcmServerKeyToPrefs(fcmServerKey);
                    sendNotificationWithKey();
                } else {
                    Log.d("FcmNotificationsSender", "Document does not exist");
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                Log.e("FcmNotificationsSender", "Error fetching document", e);
            }
        });
    }

    private void sendNotificationWithKey() {
        JSONObject mainObj = new JSONObject();
        try {
            mainObj.put("to", userFcmToken);
            JSONObject notiObject = new JSONObject();
            notiObject.put("title", title);
            notiObject.put("body", body);
            notiObject.put("icon", "mainlog_transparent_bg"); // enter icon that exists in drawable only
            mainObj.put("notification", notiObject);
            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, postUrl, mainObj, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    // code run when response is received
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    // code run when an error is received
                }
            }) {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    Map<String, String> header = new HashMap<>();
                    header.put("content-type", "application/json");
                    header.put("authorization", "key=" + fcmServerKey);
                    return header;
                }
            };
            requestQueue.add(request);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private String getFcmServerKeyFromPrefs() {
        SharedPreferences prefs = mContext.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(FCM_KEY_PREF, null);
    }

    private void saveFcmServerKeyToPrefs(String key) {
        SharedPreferences prefs = mContext.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(FCM_KEY_PREF, key);
        editor.apply();
    }
}
