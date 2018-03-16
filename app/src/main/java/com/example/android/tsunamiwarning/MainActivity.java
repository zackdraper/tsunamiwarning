package com.example.android.tsunamiwarning;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.android.tsunamiwarning.TsunamiAlarm.TsunamiAlarmService;
import com.example.android.tsunamiwarning.utilities.DividerItemDecoration;
import com.example.android.tsunamiwarning.utilities.NetworkUtils;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity
        implements QuakeEventAdapter.ListItemClickListener {

    public static final String NTWC_MESSAGE = "com.example.MESSAGE";

    private ProgressBar mLoadingIndicator;
    private TextView mTsunamiMessage;
    private RecyclerView mQuakeList;
    private QuakeEventAdapter mAdapter;

    private FusedLocationProviderClient mFusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTsunamiMessage = (TextView) findViewById(R.id.tsunami_messages);
        mQuakeList = (RecyclerView) findViewById(R.id.quake_event_list);
        mLoadingIndicator = (ProgressBar) findViewById(R.id.pb_tsunami_messages);

        getSupportActionBar().setIcon(R.drawable.iso_tsunami_icon);

        //getLocationData();

        if (isNetworkAvailable()) {
            loadNtwcMessages();
            startTsunamiAlarm();
        } else {
            showErrorMessage();
        }

    }

    public void getLocationData() {
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        Location lastKnownLocation = mFusedLocationClient.getLastLocation().getResult();

        if (lastKnownLocation != null) {
            lastKnownLocation.setLatitude( 0 );
            lastKnownLocation.setLongitude( 0 );
        }

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("lastKnownLocation",lastKnownLocation.toString());
        editor.apply();
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int itemId = item.getItemId();

        switch (itemId) {

            case R.id.action_refresh:

                if (isNetworkAvailable()) {
                    loadNtwcMessages();
                    startTsunamiAlarm();
                } else {
                    showErrorMessage();
                }
                return true;

            case R.id.action_sms:

                Intent intent = new Intent(this, DisplaySMSLogActivity.class);
                //intent.putExtra(NTWC_MESSAGE, message);
                startActivity(intent);

                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onListItemClick(int clickedItemIndex, String message) {

        //if (mToast != null) {
        //    mToast.cancel();
        //}

        //String toastMessage = "Item #" + clickedItemIndex + " clicked.";
        //mToast = Toast.makeText(this, toastMessage, Toast.LENGTH_LONG);
        //mToast.show();

        Intent intent = new Intent(this, DisplayMessageActivity.class);
        intent.putExtra(NTWC_MESSAGE, message);
        startActivity(intent);
    }

    public void loadNtwcMessages() {
        mTsunamiMessage.setVisibility(View.VISIBLE);
        mQuakeList.setVisibility(View.INVISIBLE);

        new fetchNtwcMessages().execute();
    }

    public void showNtwcMessages(String ntwcData) {
        mTsunamiMessage.setVisibility(View.INVISIBLE);
        mQuakeList.setVisibility(View.VISIBLE);

        JSONObject json = new JSONObject();

        try {
            json = new JSONObject(ntwcData);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        ArrayList<String[]> strArr = parseNtwcJson(json);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        mQuakeList.setLayoutManager(layoutManager);

        // add the decoration to the recyclerView
        com.example.android.tsunamiwarning.utilities.DividerItemDecoration decoration =
                new DividerItemDecoration(this, R.color.colorPrimaryDark, 2f);
        mQuakeList.addItemDecoration(decoration);

        mQuakeList.setHasFixedSize(true);

        mAdapter = new QuakeEventAdapter(strArr, this);
        mQuakeList.setAdapter(mAdapter);

    }

    public ArrayList<String[]> parseNtwcJson(JSONObject json) {

        JSONArray events = new JSONArray();

        JSONObject event = new JSONObject();

        try {
            events = json.getJSONArray("event");
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }

        String[] datalist = new String[] {};
        ArrayList<String[]> data = new ArrayList<String[]>();

        for (int i = 0; i < events.length(); i++ ) {
            try {
                event = events.getJSONObject(i);

                String magnitude = event.getString("magnitude");
                String datestamp = event.getString("issueTime");
                String location = event.getString("quakeLocation");

                String latitude = event.getString("lat");
                String longitude = event.getString("lon");

                String wmoid = event.getString("WMOID");
                String url = event.getString("URL");

                String urlFull = url+"/"+wmoid+"/"+wmoid+".txt";

                datalist = new String[] {magnitude,datestamp,location,urlFull,latitude,longitude};

                //Log.d("ADebugTag", "Value: " + magnitude.toString()+datestamp.toString()+location.toString()+urlFull.toString());

                data.add(datalist);

            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }

        }


        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("jsonNtwcEvents",events.toString());
        editor.apply();

        return data;

    }

    public void showErrorMessage() {
        mQuakeList.setVisibility(View.INVISIBLE);
        mTsunamiMessage.setVisibility(View.VISIBLE);
        mLoadingIndicator.setVisibility(View.INVISIBLE);

        mTsunamiMessage.setText(R.string.error);
    }

    public class fetchNtwcMessages extends AsyncTask<String, Void, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mLoadingIndicator.setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(String... params) {

            URL ntwc_pre40 = NetworkUtils.getNTWCurl();

            try {
                String jsonNtwcResponse = NetworkUtils
                        .getResponseFromHttpUrl(ntwc_pre40);
                //String jsonNtwcResponse = "TEst String";
                return "{\n"+jsonNtwcResponse;
            } catch (Exception e) {
                e.printStackTrace();
                return null;

            }
        }

        @Override
        protected void onPostExecute(String ntwcData) {
            mLoadingIndicator.setVisibility(View.INVISIBLE);

            if (ntwcData.length() > 5) {
                showNtwcMessages(ntwcData);
            } else {
                showErrorMessage();
            }
        }

    }

    public void startTsunamiAlarm() {
        String TAG = TsunamiAlarm.TsunamiAlarmService.class.getSimpleName();

        ComponentName componentName = new ComponentName(this, TsunamiAlarmService.class);
        JobInfo jobInfo = new JobInfo.Builder(12, componentName)
                //.setPeriodic(5000)
                .setPersisted(true)
                .setRequiresCharging(false)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                //.setTriggerContentMaxDelay(5000)
                .setMinimumLatency(5000)
                .build();

        JobScheduler jobScheduler = (JobScheduler)getSystemService(JOB_SCHEDULER_SERVICE);
        int resultCode = jobScheduler.schedule(jobInfo);
        if (resultCode == JobScheduler.RESULT_SUCCESS) {
            //Log.d(TAG, "Job scheduled!");
        } else {
            //Log.d(TAG, "Job not scheduled");
        }
    }


}
