package com.example.android.tsunamiwarning;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.example.android.tsunamiwarning.utilities.NetworkUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URL;
import java.util.Calendar;

import static android.content.ContentValues.TAG;

/**
 * Created by zackdraper on 04/04/18.
 */

public class TsunamiAlarm_beta extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent intent)
    {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "");
        wl.acquire();

        Log.d(TAG, "Alarm Executed");

        try {
            //Retrieve last quake location
            SharedPreferences ntwcevents = PreferenceManager.getDefaultSharedPreferences(context);
            String savedEventsString = ntwcevents.getString("jsonNtwcEvents", null);

            // Get old events list
            JSONArray oldEvents = new JSONArray(savedEventsString);
            JSONObject oldEvent = oldEvents.getJSONObject(0);
            String locationOld = oldEvent.getString("quakeLocation");

            String locationNew = "";
            String magnitude = "";
            String datestamp = "";

            // Get new events list from data if network available
            if (NetworkUtils.isNetworkAvailable(context)) {
                URL ntwc_pre40 = NetworkUtils.getNTWCurl();

                String jsonNtwcResponse = "";
                try {
                    jsonNtwcResponse = NetworkUtils
                            .getResponseFromHttpUrl(ntwc_pre40);;
                } catch (Exception e) {
                    e.printStackTrace();
                    jsonNtwcResponse = "";

                }

                if (jsonNtwcResponse.length() > 5) {
                    JSONObject newJson = new JSONObject("{\n" + jsonNtwcResponse);
                    JSONArray newEvents = newJson.getJSONArray("event");
                    JSONObject newEvent = newEvents.getJSONObject(0);
                    locationNew = newEvent.getString("quakeLocation");

                    magnitude = newEvent.getString("magnitude");
                    datestamp = newEvent.getString("issueTime");

                    boolean new_quake_data = (!locationOld.equalsIgnoreCase(locationNew));

                    if (new_quake_data) {
                        raiseAlarm(context, intent, magnitude, locationNew, datestamp);
                    }
                }
            }

            //check sms list
            boolean new_quake_sms = false;

            SharedPreferences ntwcevents_sms = PreferenceManager.getDefaultSharedPreferences(context);
            String smslastevent = ntwcevents_sms.getString("smsLastEvent", null);
            JSONArray oldSMSEvent = new JSONArray(smslastevent);

            SharedPreferences twittercode = PreferenceManager.getDefaultSharedPreferences(context);
            String twitcode = twittercode.getString("twitter_code", null);

            magnitude = oldSMSEvent.get(0).toString();
            locationNew = oldSMSEvent.get(1).toString();
            datestamp = oldSMSEvent.get(2).toString();

            Log.d(TAG, magnitude+locationNew+datestamp);

            //smsdata = DisplaySMSLogActivity.fetchSMSMessages().execute(twitcode);

            if ( new_quake_sms ) {
                raiseAlarm(context,intent,magnitude,locationNew,datestamp);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        this.cancelAlarm(context);
        // Put here YOUR code.
        //Toast.makeText(context, "Alarm !!!!!!!!!!", Toast.LENGTH_LONG).show(); // For example

        wl.release();
    }

    public void raiseAlarm(Context context,Intent intent, String magnitude,String locationNew,String datestamp) {

        Log.d(TAG, "New Quake!");

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_warning)
                .setContentTitle("NEW MESSAGE FROM NTWC")
                .setContentText(magnitude + " " + locationNew + " " + datestamp)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(0, mBuilder.build());

        if (Float.parseFloat(magnitude) > 4.0) {
            //Log.d(TAG, "Push Alarm");
            //Create an offset from the current time in which the activity_receiver_alarm will go off.
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.SECOND, 1);

            String tsunami_info = magnitude + "\n" + locationNew;

            //Create a new PendingIntent and add it to the AlarmManager
            Intent intent_alert = new Intent(context, AlarmReceiverActivity.class);
            intent_alert.putExtra("tsunami_info", tsunami_info);
            PendingIntent pendingIntent = PendingIntent.getActivity(context,
                    12345, intent, PendingIntent.FLAG_CANCEL_CURRENT);
            AlarmManager am =
                    (AlarmManager) context.getSystemService (Activity.ALARM_SERVICE);
            am.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(),
                    pendingIntent);

        }

    }

    public void setAlarm(Context context)
    {
        Log.d(TAG, "Alarm Started");
        AlarmManager am =( AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(context, TsunamiAlarm_beta.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);
        am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 1000 * 60, pi); // Millisec * Second * Minute
    }

    public void cancelAlarm(Context context)
    {
        Intent intent = new Intent(context, TsunamiAlarm_beta.class);
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(sender);
    }
}
