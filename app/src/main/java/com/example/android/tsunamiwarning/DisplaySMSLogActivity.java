package com.example.android.tsunamiwarning;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.android.tsunamiwarning.utilities.DividerItemDecoration;
import com.example.android.tsunamiwarning.utilities.NetworkUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by zackdraper on 14/02/18.
 */

public class DisplaySMSLogActivity extends AppCompatActivity
        implements QuakeEventAdapter.ListItemClickListener {

    public static final String NTWC_MESSAGE = "com.example.MESSAGE";

    private ProgressBar mLoadingIndicator;
    private TextView mTsunamiMessage;
    private RecyclerView mSMSList;
    private QuakeEventAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sms_log_display);

        View mDescription = findViewById(R.id.description_item_sms);

        showEntryMessage();

        SharedPreferences twittercode = PreferenceManager.getDefaultSharedPreferences(this);
        String twitcode = twittercode.getString("twitter_code", null);

        final String SMS_URI_INBOX = "content://sms/inbox";
        Uri uri = Uri.parse(SMS_URI_INBOX);
        Cursor cur = getContentResolver().query(uri, null, "address='"+twitcode+"'", null, null);

        if (cur.getCount() > 0) {
            quakeListDiscription(mDescription);
            new fetchSMSMessages().execute(twitcode);
        }
    }

    public void quakeListDiscription(View mDescription) {

        TextView event_discrip = (TextView) mDescription.findViewById(R.id.tv_event_description);
        event_discrip.setText("Event Description");

        TextView event_mag = (TextView) mDescription.findViewById(R.id.tv_event_mag);
        event_mag.setText("Magnitude");

        TextView event_dist = (TextView) mDescription.findViewById(R.id.tv_event_dist);
        event_dist.setText("Distance");

        TextView event_time = (TextView) mDescription.findViewById(R.id.tv_event_time);
        event_time.setText("Time Since Event");
    }

    @Override
    public void onListItemClick(int clickedItemIndex, String tweet) {

        if (NetworkUtils.isNetworkAvailable(this)) {

            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(tweet));
            startActivity(browserIntent);

        } else {
            mTsunamiMessage = (TextView) findViewById(R.id.tsunami_messages_sms);
            mLoadingIndicator = (ProgressBar) findViewById(R.id.pb_tsunami_messages_sms);

            mLoadingIndicator.setVisibility(View.INVISIBLE);
            mTsunamiMessage.setText("No Internet Connection");

        }
    }

    public void showEntryMessage() {
        mTsunamiMessage = (TextView) findViewById(R.id.tsunami_messages_sms);
        mSMSList = (RecyclerView) findViewById(R.id.quake_event_list_sms);
        mLoadingIndicator = (ProgressBar) findViewById(R.id.pb_tsunami_messages_sms);

        Spanned linkText = Html.fromHtml("Sign up for NTWC Warnings via sms texts," +
                " for those times when you don't have any data, but still have cell reception.<br><br>"+
                "Text 'follow NWS_NTWC' to your region & providers "+
                "<a href='https://help.twitter.com/en/using-twitter/supported-mobile-carriers'>" +
                "Twitter short code</a> and then update your settings.<br><br>"+
                "<img src='ca.png' /> 21212<br>"+
                "<img src='us.png' /> 40404", new ImageGetter(), null
        );

        mLoadingIndicator.setVisibility(View.INVISIBLE);
        mTsunamiMessage.setText(linkText);

        mTsunamiMessage.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private class ImageGetter implements Html.ImageGetter {

        public Drawable getDrawable(String source) {
            int id;

            if (source.equals("us.png")) {
                id = R.drawable.us;
            }
            else if (source.equals("ca.png")) {
                id = R.drawable.ca;
            }
            else {
                return null;
            }

            Drawable d = getResources().getDrawable(id);
            d.setBounds(0,0,d.getIntrinsicWidth(),d.getIntrinsicHeight());
            return d;
        }
    };

    public void showSMSMessages(ArrayList<String[]> SMSData) {
        mTsunamiMessage.setVisibility(View.INVISIBLE);
        mSMSList.setVisibility(View.VISIBLE);

        //mTsunamiMessage.setText(SMSData.toString());

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        mSMSList.setLayoutManager(layoutManager);

        // add the decoration to the recyclerView
        com.example.android.tsunamiwarning.utilities.DividerItemDecoration decoration =
                new DividerItemDecoration(this, R.color.colorPrimaryDark, 2f);
        mSMSList.addItemDecoration(decoration);

        mSMSList.setHasFixedSize(true);

        mAdapter = new QuakeEventAdapter(SMSData, this);
        mSMSList.setAdapter(mAdapter);

        //save last know location for alarm
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("smsLastEvent", SMSData.get(0).toString() );
        editor.apply();

    }

    public class fetchSMSMessages extends AsyncTask<String, Void, ArrayList<String[]>> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mLoadingIndicator.setVisibility(View.VISIBLE);
        }

        @Override
        protected ArrayList<String[]> doInBackground(String... twitcode) {

            StringBuilder smsBuilder = new StringBuilder();
            final String SMS_URI_INBOX = "content://sms/inbox";
            //final String SMS_URI_ALL = "content://sms/";
            smsBuilder.append("{");

            String[] datalist = new String[] {};
            ArrayList<String[]> data = new ArrayList<String[]>();

            try {
                Uri uri = Uri.parse(SMS_URI_INBOX);
                String[] projection = new String[]{"_id", "address", "person", "body", "date", "type"};

                //twitcode = new String[] {"21212"};
                Cursor cur = getContentResolver().query(uri, projection, "address='"+twitcode[0]+"'", null, "date desc");
                if (cur.moveToFirst()) {
                    int index_Address = cur.getColumnIndex("address");
                    int index_Person = cur.getColumnIndex("person");
                    int index_Body = cur.getColumnIndex("body");
                    int index_Date = cur.getColumnIndex("date");
                    int index_Type = cur.getColumnIndex("type");
                    do {
                        //String strAddress = cur.getString(index_Address);
                        //int intPerson = cur.getInt(index_Person);
                        String strbody = cur.getString(index_Body);
                        long longDate = cur.getLong(index_Date);
                        //int int_Type = cur.getInt(index_Type);
                        String message_tag = strbody.substring(0,9);

                        if (message_tag.equalsIgnoreCase("@nws_ntwc") ) {
                            int idx_mag = strbody.indexOf(" M",9);
                            String magnitude = strbody.substring(idx_mag+2, idx_mag + 5);

                            int idx_end = strbody.indexOf("NTWC",10);
                            String location = strbody.substring(idx_mag + 6, idx_end-2);

                            String urlFull = "m.twitter.com/NWS_NTWC";
                            String latitude = "-99";
                            String longitude = "-99";

                            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                            Date smsTime = new Date(longDate);

                            String smsTime_str;

                            smsTime_str = df.format(smsTime);

                            datalist = new String[]{magnitude, smsTime_str, location, urlFull, latitude, longitude};

                            data.add(datalist);
                        }

                    } while (cur.moveToNext());

                    if (!cur.isClosed()) {
                        cur.close();
                        cur = null;
                    }
                } else {
                    datalist = new String[] {"","","No Result","","",""};
                    data.add(datalist);
                } // end if

                return data;
            } catch (SQLiteException ex) {
                Log.d("SQLiteException", ex.getMessage());
                return data;
            }

        }

        @Override
        protected void onPostExecute(ArrayList<String[]> SMSData) {
            mLoadingIndicator.setVisibility(View.INVISIBLE);

            showSMSMessages(SMSData);

        }

    }
}
