package com.example.android.tsunamiwarning;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.android.tsunamiwarning.utilities.NetworkUtils;

import java.net.URL;

/**
 * Created by zackdraper on 14/02/18.
 */

public class DisplayMessageActivity extends AppCompatActivity {

    private static final String ntwc_domain = "https://www.tsunami.gov";

    private TextView mTsunamiMessageFull;

    private ProgressBar mProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message_display);
        Intent intent = getIntent();
        String message = intent.getStringExtra(MainActivity.NTWC_MESSAGE);

        mTsunamiMessageFull = (TextView) findViewById(R.id.detailedMessage);
        mProgressBar = (ProgressBar) findViewById(R.id.pb_tsunami_gram);

        new fetchTsunamiGram().execute(ntwc_domain+message);
    }

    public void displayMessage(String message) {
        mProgressBar.setVisibility(View.INVISIBLE);
        mTsunamiMessageFull.setVisibility(View.VISIBLE);

        mTsunamiMessageFull.setText(message);
    }

    public void showErrorMessage() {
        mProgressBar.setVisibility(View.INVISIBLE);
        mTsunamiMessageFull.setVisibility(View.VISIBLE);

        mTsunamiMessageFull.setText(R.string.error);
    }

    public class fetchTsunamiGram extends AsyncTask<String, Void, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(String... messageURL) {

            URL urlMessage = NetworkUtils.getStringUrl(messageURL[0]);

            try {
                String NtwcResponse = NetworkUtils
                        .getResponseFromHttpUrl(urlMessage);
                //String jsonNtwcResponse = "TEst String";
                return NtwcResponse;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String ntwcData) {
            mProgressBar.setVisibility(View.INVISIBLE);
            if (ntwcData.length() > 5) {
                displayMessage(ntwcData);
            } else {
                showErrorMessage();
            }
        }

    }

}
