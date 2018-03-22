package com.example.android.tsunamiwarning;

import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.List;

/**
 * Created by zackdraper on 14/02/18.
 */

public class DisplaySMSLogActivity extends AppCompatActivity {

    private ProgressBar mLoadingIndicator;
    private TextView mTsunamiMessage;
    private RecyclerView mSMSList;
    private QuakeEventAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sms_log_display);

        showEntryMessage();

        if (true) {
            new fetchSMSMessages().execute();
        }
    }

    public void showEntryMessage() {
        mTsunamiMessage = (TextView) findViewById(R.id.tsunami_messages_sms);
        mSMSList = (RecyclerView) findViewById(R.id.quake_event_list_sms);
        mLoadingIndicator = (ProgressBar) findViewById(R.id.pb_tsunami_messages_sms);

        Spanned linkText = Html.fromHtml("Sign up for NTWC Warning via Twitter texts," +
                " just for those times when you don't have any data.<br><br>"+
                "Text 'follow NWS_NTWC' to your region & providers "+
                "<a href='https://help.twitter.com/en/using-twitter/supported-mobile-carriers'>" +
                "Twitter short code</a><br><br>"+
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

    public void showSMSMessages(String SMSData) {
        mTsunamiMessage.setVisibility(View.VISIBLE);
        mSMSList.setVisibility(View.INVISIBLE);

        JSONObject events = new JSONObject();

        try {
            events = new JSONObject(SMSData);
        } catch (JSONException e) {
            e.printStackTrace();
        };

        List<String> items = Arrays.asList(SMSData.split("\\s*,\\s*"));

        mTsunamiMessage.setText(items.toString());

        //ArrayList<String[]> strArr = new ArrayList<String[]>();

        //LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        //mSMSList.setLayoutManager(layoutManager);

        //mSMSList.setHasFixedSize(true);

        //mAdapter = new QuakeEventAdapter(strArr, this);
        //mSMSList.setAdapter(mAdapter);

    }

    public class fetchSMSMessages extends AsyncTask<String, Void, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            //mLoadingIndicator.setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(String... params) {

            StringBuilder smsBuilder = new StringBuilder();
            final String SMS_URI_INBOX = "content://sms/inbox";
            final String SMS_URI_ALL = "content://sms/";
            smsBuilder.append("[");
            try {
                Uri uri = Uri.parse(SMS_URI_INBOX);
                String[] projection = new String[]{"_id", "address", "person", "body", "date", "type"};
                Cursor cur = getContentResolver().query(uri, projection, "address='21212'", null, "date desc");
                if (cur.moveToFirst()) {
                    int index_Address = cur.getColumnIndex("address");
                    int index_Person = cur.getColumnIndex("person");
                    int index_Body = cur.getColumnIndex("body");
                    int index_Date = cur.getColumnIndex("date");
                    int index_Type = cur.getColumnIndex("type");
                    do {
                        String strAddress = cur.getString(index_Address);
                        int intPerson = cur.getInt(index_Person);
                        String strbody = cur.getString(index_Body);
                        long longDate = cur.getLong(index_Date);
                        int int_Type = cur.getInt(index_Type);


                        smsBuilder.append("\"" + strAddress + "\",");
                        smsBuilder.append("\"" + intPerson + "\",");
                        smsBuilder.append("\"" + strbody + "\",");
                        smsBuilder.append("\"" + longDate + "\",");
                        smsBuilder.append("\"" + int_Type + "\"");
                        smsBuilder.append("],[");
                    } while (cur.moveToNext());

                    if (!cur.isClosed()) {
                        cur.close();
                        cur = null;
                    }
                } else {
                    smsBuilder.append("no result!");
                } // end if
                smsBuilder.append("]");
                return smsBuilder.toString();
            } catch (SQLiteException ex) {
                Log.d("SQLiteException", ex.getMessage());
                return smsBuilder.toString();
            }

        }

        @Override
        protected void onPostExecute(String SMSData) {
            //mLoadingIndicator.setVisibility(View.INVISIBLE);

            showSMSMessages(SMSData);
        }

    }
}
