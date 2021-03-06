/**
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.traccar.client;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

import static org.traccar.client.MainActivity.KEY_EMERGENCY;
import static org.traccar.client.MainActivity.KEY_INTERVAL;
import static org.traccar.client.MainActivity.KEY_STATUS;

public class WaynikFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "WaynikFirebaseMsgSrv";

    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    // [START receive_message]
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // [START_EXCLUDE]
        // There are two types of messages data messages and notification messages. Data messages are handled
        // here in onMessageReceived whether the app is in the foreground or background. Data messages are the type
        // traditionally used with GCM. Notification messages are only received here in onMessageReceived when the app
        // is in the foreground. When the app is in the background an automatically generated notification is displayed.
        // When the user taps on the notification they are returned to the app. Messages containing both notification
        // and data payloads are treated as notification messages. The Firebase console always sends notification
        // messages. For more see: https://firebase.google.com/docs/cloud-messaging/concept-options
        // [END_EXCLUDE]

        // TODO(developer): Handle FCM messages here.
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());

//            if (/* Check if data needs to be processed by long running job */ true) {
//                // For long-running tasks (10 seconds or more) use Firebase Job Dispatcher.
//                scheduleJob();
//            } else {
            // Handle message within 10 seconds
            handleNow(remoteMessage);
//            }

        }

        // Check if message contains a notification payload.
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
            // Also if you intend on generating your own notifications as a result of a received FCM
            // message, here is where that should be initiated. See sendNotification method below.
            // /sendNotification(remoteMessage.getNotification().getBody());

        }


    }
    // [END receive_message]

    /**
     * Schedule a job using FirebaseJobDispatcher.
     */
//    private void scheduleJob() {
//        // [START dispatch_job]
//        FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(this));
//        Job myJob = dispatcher.newJobBuilder()
//                .setService(MyJobService.class)
//                .setTag("my-job-tag")
//                .build();
//        dispatcher.schedule(myJob);
//        // [END dispatch_job]
//    }

    /**
     * Handle time allotted to BroadcastReceivers.
     */
    private void handleNow(RemoteMessage remoteMessage) {
        Map<String, String> data = remoteMessage.getData();
        final String frequency = data.get("frequency");
        final String emergency = data.get("emergency");
        String message = data.get("message_for_android");
        final boolean emergencyValue = Boolean.parseBoolean(emergency);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (frequency != null) {
            Log.d(TAG, "setting frequency to" + frequency);
            //set both the preference and update the ui.
            sharedPreferences.edit().putString(KEY_INTERVAL, frequency).commit();
            if (MainActivity.instance != null) {
                MainActivity.instance.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        EditTextPreference frequencyPreference = (EditTextPreference) MainActivity.instance.findPreference(KEY_INTERVAL);
                        frequencyPreference.setText(frequency);
                    }
                });
            }

        }
        if (emergency != null) {
            Log.d(TAG, "setting emergency to" + emergencyValue);
            sharedPreferences.edit().putBoolean(KEY_EMERGENCY, emergencyValue).commit();

            if (MainActivity.instance != null) {
                MainActivity.instance.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        SwitchPreference emergencyPreference = (SwitchPreference) MainActivity.instance.findPreference(KEY_EMERGENCY);
                        emergencyPreference.setChecked(emergencyValue);
                    }
                });
            }
        }

        // always turn it on at the end
        sharedPreferences.edit().putBoolean(KEY_STATUS, true).commit();
        if (MainActivity.instance != null) {
            MainActivity.instance.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    SwitchPreference statusPreference = (SwitchPreference) MainActivity.instance.findPreference(KEY_STATUS);
                    statusPreference.setChecked(true);
                    MainActivity.instance.stopTrackingService();
                    MainActivity.instance.startTrackingService(true, false);
                }
            });
        } else {
            // app is killed, just start the services!
            stopService(new Intent(this, TrackingService.class));
            startService(new Intent(this, TrackingService.class));
        }

        if (message != null) {
            sendNotification(message);
        }
        Log.d(TAG, "Short lived task is done.");
    }

    /**
     * Create and show a simple notification containing the received FCM message.
     *
     * @param messageBody FCM message body received.
     */
    private void sendNotification(String messageBody) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT);

        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Waynik")
                .setContentText(messageBody)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());
    }
}