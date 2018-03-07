package com.example.android.tsunamiwarning;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * Created by zackdraper on 14/02/18.
 */

public class DisplaySMSLogActivity extends AppCompatActivity {

    private ProgressBar mLoadingIndicator;
    private TextView mTsunamiMessage;
    private RecyclerView mQuakeList;
    private QuakeEventAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sms_log_display);

        //Intent intent = getIntent();
        //String message = intent.getStringExtra(MainActivity.NTWC_MESSAGE);

        mTsunamiMessage = (TextView) findViewById(R.id.tsunami_messages_sms);
        mQuakeList = (RecyclerView) findViewById(R.id.quake_event_list_sms);
        mLoadingIndicator = (ProgressBar) findViewById(R.id.pb_tsunami_messages_sms);

        mTsunamiMessage.setMovementMethod(LinkMovementMethod.getInstance());

        mLoadingIndicator.setVisibility(View.INVISIBLE);
        mTsunamiMessage.setText(
                "Sign up for NTWC Warning via Twitter texts," +
                        " for those times when you don't have any data\n\n"+
                        "Text 'follow NWS_NTWC' to your region/providers Twitter short code\n\n"
        );

    }

    public void showErrorMessage() {
        mQuakeList.setVisibility(View.INVISIBLE);
        mTsunamiMessage.setVisibility(View.VISIBLE);
        mLoadingIndicator.setVisibility(View.INVISIBLE);

        mTsunamiMessage.setText(R.string.error);
    }

    public void showSMSMessages(String ntwcData) {
        mTsunamiMessage.setVisibility(View.INVISIBLE);
        mQuakeList.setVisibility(View.VISIBLE);

        //ArrayList<String[]> strArr = new ArrayList<String[]>();

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        mQuakeList.setLayoutManager(layoutManager);

        mQuakeList.setHasFixedSize(true);

        //mAdapter = new QuakeEventAdapter(strArr, this);
        //mQuakeList.setAdapter(mAdapter);

    }

    public class fetchSMSMessages extends AsyncTask<String, Void, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mLoadingIndicator.setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(String... params) {

            return "";
        }

        @Override
        protected void onPostExecute(String ntwcData) {
            mLoadingIndicator.setVisibility(View.INVISIBLE);

            if (ntwcData.length() > 5) {
                showSMSMessages(ntwcData);
            } else {
                showErrorMessage();
            }
        }

    }
}
