package com.example.android.tsunamiwarning;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.RequiresApi;
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
 * Created by zackdraper on 22/02/18.
 */

public final class TsunamiAlarm {

    //Background job for Tsunami Alarm

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static class TsunamiAlarmService extends JobService {
        private final String TAG = TsunamiAlarm.class.getSimpleName();

        boolean isWorking = false;
        boolean jobCancelled = false;

        // Called by the Android system when it's time to run the job
        @Override
        public boolean onStartJob(JobParameters jobParameters) {
            Log.d(TAG, "Job started!");
            isWorking = true;
            // We need 'jobParameters' so we can call 'jobFinished'
            startWorkOnNewThread(jobParameters); // Services do NOT run on a separate thread

            return isWorking;
        }

        private void startWorkOnNewThread(final JobParameters jobParameters) {
            new Thread(new Runnable() {
                public void run() {
                    doWork(jobParameters);
                }
            }).start();
        }

        private void doWork(JobParameters jobParameters) {

            try {
                //Retrieve last location
                SharedPreferences ntwcevents = PreferenceManager.getDefaultSharedPreferences(this);
                String savedEventsString = ntwcevents.getString("jsonNtwcEvents", null);

                // Get old events list
                JSONArray oldEvents = new JSONArray(savedEventsString);
                JSONObject oldEvent = oldEvents.getJSONObject(0);
                String locationOld = oldEvent.getString("quakeLocation");

                // Get new events list
                URL ntwc_pre40 = NetworkUtils.getNTWCurl();

                String jsonNtwcResponse = NetworkUtils
                        .getResponseFromHttpUrl(ntwc_pre40);

                JSONObject newJson = new JSONObject("{\n" + jsonNtwcResponse);
                JSONArray newEvents = newJson.getJSONArray("event");
                JSONObject newEvent = newEvents.getJSONObject(0);
                String locationNew = newEvent.getString("quakeLocation");
                String magnitude = newEvent.getString("magnitude");
                String datestamp = newEvent.getString("issueTime");

                Log.d(TAG, locationNew);

                if (! locationOld.equalsIgnoreCase(locationNew)) {
                    raiseAlarm(locationNew,magnitude,datestamp);
                }

                //check sms list
                SharedPreferences ntwcevents_sms = PreferenceManager.getDefaultSharedPreferences(context);
                String smslastevent = ntwcevents_sms.getString("smsLastEvent", null);
                JSONArray oldSMSEvent = new JSONArray(smslastevent);

                SharedPreferences twittercode = PreferenceManager.getDefaultSharedPreferences(context);
                String twitcode = twittercode.getString("twitter_code", null);

                locationOld = oldSMSEvent.get(1).toString();

                Log.d(TAG, locationOld);

                final String SMS_URI_INBOX = "content://sms/inbox";
                Uri uri = Uri.parse(SMS_URI_INBOX);
                Cursor cur = getContentResolver().query(uri, null, "address='"+twitcode[0]+"'", null, "date desc");

                cur.moveToFirst();

                int index_Body = cur.getColumnIndex("body");
                int index_Date = cur.getColumnIndex("date");

                String strbody = cur.getString(index_Body);
                String message_tag = strbody.substring(0,9);

                locationNew_sms = "";

                if (message_tag.equalsIgnoreCase("@nws_ntwc") ) {
                    int idx_mag = strbody.indexOf(" M",9);
                    String magnitude_sms = strbody.substring(idx_mag+2, idx_mag + 5);

                    int idx_end = strbody.indexOf("NTWC",10);
                    String locationNew_sms = strbody.substring(idx_mag + 6, idx_end-2);

                }

                if (! locationOld.equalsIgnoreCase(locationNew_sms)) {
                    raiseAlarm(locationNew_sms,magnitude_sms,datestamp_sms);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

            //Log.d(TAG, "Job finished!");
            isWorking = false;
            boolean needsReschedule = false;
            jobFinished(jobParameters, needsReschedule);
        }

        public void raiseAlarm(String locationNew, String magnitude,String datestamp) {

            Log.d(TAG, "New Quake!");

            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                    .setSmallIcon(R.drawable.ic_warning)
                    .setContentTitle("NEW MESSAGE FROM NTWC")
                    .setContentText(magnitude+" "+locationNew+" "+datestamp)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
            notificationManager.notify(0, mBuilder.build());

            if (Float.parseFloat(magnitude) > 4.0) {
                Log.d(TAG, "Push Alarm");
                //Create an offset from the current time in which the activity_receiver_alarm will go off.
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.SECOND, 1);

                String tsunami_info = magnitude+"\n"+locationNew;

                //Create a new PendingIntent and add it to the AlarmManager
                Intent intent = new Intent(this, AlarmReceiverActivity.class);
                intent.putExtra("tsunami_info", tsunami_info);
                PendingIntent pendingIntent = PendingIntent.getActivity(this,
                        12345, intent, PendingIntent.FLAG_CANCEL_CURRENT);
                AlarmManager am =
                        (AlarmManager)getSystemService(Activity.ALARM_SERVICE);
                am.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(),
                        pendingIntent);
            }

        }

        // Called if the job was cancelled before being finished
        @Override
        public boolean onStopJob(JobParameters jobParameters) {
            //Log.d(TAG, "Job cancelled before being completed.");
            jobCancelled = true;
            boolean needsReschedule = isWorking;
            jobFinished(jobParameters, needsReschedule);
            return needsReschedule;
        }
    }
}
