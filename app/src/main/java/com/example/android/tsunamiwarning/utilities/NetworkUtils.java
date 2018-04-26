/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.tsunamiwarning.utilities;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;



/**
 * These utilities will be used to read JSON from the web
 */
public final class NetworkUtils {

    private static final String NTWC_PRE40 =
            "https://www.tsunami.gov/events/js/previous.js";

    final static String QUERY_PARAM = "q";

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null;
    }

    /**
     * Builds the url for the NTWC previous 40 list
     *
     * @return The URL to use to query the weather server.
     */
    public static URL getNTWCurl() {

        Uri builtURI = Uri.parse(NTWC_PRE40).buildUpon().build();

        URL url = null;
        try {
            url = new URL(builtURI.toString());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        return url;
    }

    public static URL getStringUrl(String string) {

        Uri builtURI = Uri.parse(string).buildUpon().build();

        URL url = null;
        try {
            url = new URL(builtURI.toString());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        return url;
    }

    /**
     * This method returns the entire result from the HTTP response.
     *
     * @param url The URL to fetch the HTTP response from.
     * @return The contents of the HTTP response.
     * @throws IOException Related to network and stream reading
     */
    public static String getResponseFromHttpUrl(URL url) throws IOException {

        String str = "";

        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        try {
            InputStream is=urlConnection.getInputStream();

            BufferedReader br=new BufferedReader(new InputStreamReader(is));

            String line;

            int i = 0;

            while ((line = br.readLine()) != null) {
                i+=1;
                if ( i > 1) {
                    str+=line+'\n';
                }
            }

            br.close();

            return str;

        } finally {
            urlConnection.disconnect();
        }
    }
}