/*
 * Copyright (C) 2017 Emmanuel Kehinde
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

package com.ki.leo.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.esafirm.rxdownloader.RxDownloader;
import com.ki.leo.R;
import com.ki.leo.receivers.AutoListenService;
import com.ki.leo.utils.Constant;
import com.twitter.sdk.android.Twitter;
import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.TwitterApiClient;
import com.twitter.sdk.android.core.TwitterAuthConfig;
import com.twitter.sdk.android.core.TwitterCore;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.models.Tweet;
import com.twitter.sdk.android.core.services.StatusesService;

import at.huber.youtubeExtractor.VideoMeta;
import at.huber.*;
import at.huber.youtubeExtractor.YouTubeExtractor;
import at.huber.youtubeExtractor.YtFile;
import io.fabric.sdk.android.Fabric;
import retrofit2.Call;
import rx.Observer;

import static com.ki.leo.R.color.colorAccent;

public class MainActivity extends AppCompatActivity {

    private ProgressDialog progressDialog;
    private Button btn_download;
    private TextView txt_tweet_url;
    private TextView txt_filename;
    private Switch swt_autolisten;
    private LinearLayout mainLayout;
    private ProgressBar mainProgressBar;
    final Context c = this;

    private SharedPreferences sharedPreferences;
    private Button btn_about_app;

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TwitterAuthConfig authConfig = new TwitterAuthConfig(Constant.TWITTER_KEY, Constant.TWITTER_SECRET);
        Fabric.with(this, new Twitter(authConfig));
        setContentView(R.layout.activity_main);
        sharedPreferences = this.getSharedPreferences("com.ki.leo", Context.MODE_PRIVATE);

        btn_download = findViewById(R.id.btn_download);
        btn_about_app = findViewById(R.id.btn_about_app);
        txt_tweet_url = findViewById(R.id.txt_tweet_url);
        txt_filename = findViewById(R.id.txt_filename);
        txt_tweet_url.setEnabled(false);
        txt_filename.setEnabled(false);
        swt_autolisten = findViewById(R.id.swt_autolisten);


        if (getIntent().getBooleanExtra("service_on", false)) {
            swt_autolisten.setChecked(true);
        } else
            swt_autolisten.setChecked(false);


        if (!storageAllowed()) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(this, Constant.PERMISSIONS_STORAGE, Constant.REQUEST_EXTERNAL_STORAGE);
        }

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Fetching Data...");
        progressDialog.setCancelable(false);
        progressDialog.setIndeterminate(true);


        // Get intent, action and MIME type
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if ("text/plain".equals(type)) {
                handleSharedText(intent); // Handle text being sent
            }
        }
        //Method call to handle AutoListen feature
        handleAutoListen();
        btn_about_app.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("ResourceAsColor")
            @Override
            public void onClick(View view) {
                Intent in = new Intent(getApplicationContext(),AboutActivity.class);
                startActivity(in);
            }});

        btn_download.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("ResourceAsColor")
            @Override
            public void onClick(View view) {
                String fname;
                //Check if the tweet url field has text containing twitter.com/...
                if (txt_tweet_url.getText().length() > 0 && txt_tweet_url.getText().toString().contains("twitter.com/")) {
                    Long id = getTweetId(txt_tweet_url.getText().toString());
                    //Check if filename is set. If not, set the tweet Id as the filename
                    if (txt_filename.getText().length() > 0) {
                        fname = txt_filename.getText().toString().trim();
                    } else {
                        fname = String.valueOf(id);
                    }
                    //Call method to get tweet
                    if (id != null) {
                        getTweet(id, fname);
                    }
                } else if (txt_tweet_url.getText().length() > 0 && (txt_tweet_url.getText().toString().contains("://youtu.be/") || txt_tweet_url.getText().toString().contains("youtube.com/watch?v="))) {
                   String  youtubeLink = txt_tweet_url.getText().toString();
                    // We have a valid link
                    Intent intent = new Intent(getApplicationContext(), DownloadYoutube.class);
                    intent.putExtra("EXTRA_SESSION_ID", youtubeLink);
                    startActivity(intent);
                   // getYoutubeDownloadUrl(youtubeLink);
                }else{
                    alertNoUrl();
                }
            }
        });

    }

    private void handleAutoListen() {
        swt_autolisten.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @SuppressLint("ResourceAsColor")
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                TextView yourTexView  = (TextView) findViewById(R.id.textView2 );
                if (isChecked) {
                    yourTexView.setTextColor(R.color.tw__composer_red);
                    yourTexView.setText(R.string.listening_mode);
                    startAutoService();
                } else {
                    yourTexView.setTextColor(colorAccent);
                    yourTexView.setText(R.string.listening_mode_off);
                    stopAutoService();
                }
            }
        });

    }

    private void stopAutoService() {
        this.stopService(new Intent(this, AutoListenService.class));
    }

    private void startAutoService() {
        this.startService(new Intent(this, AutoListenService.class));
    }


    //Method handling pasting the tweet url into the field when Sharing the tweet url from the twitter app
    private void handleSharedText(Intent intent) {
        String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (sharedText != null) {
            try {
                if (sharedText.split("\\ ").length > 1) {
                    txt_tweet_url.setText(sharedText.split("\\ ")[4]);

                } else {
                    txt_tweet_url.setText(sharedText.split("\\ ")[0]);
                }

                if(txt_tweet_url.getText().toString().contains("twitter.com/")){
                    txt_filename.setTextColor(colorAccent);
                    txt_filename.setText(R.string.twitterLink);
                }else if(txt_tweet_url.getText().toString().contains("://youtu.be/") || txt_tweet_url.getText().toString().contains("youtube.com/watch?v=")){
                    txt_filename.setTextColor(R.color.tw__composer_red);
                    txt_filename.setText(R.string.youtubeLink);
                }else {
                    txt_filename.setText(R.string.invalidLink);
                }
            } catch (Exception e) {
                Log.d("TAG", "handleSharedText: " + e);
            }
        }
    }


    public void getTweet(final Long id, final String fname) {
        progressDialog.show();
        TwitterApiClient twitterApiClient = TwitterCore.getInstance().getApiClient();
        StatusesService statusesService = twitterApiClient.getStatusesService();
        Call<Tweet> tweetCall = statusesService.show(id, null, null, null);
        tweetCall.enqueue(new Callback<Tweet>() {
            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
            @Override
            public void success(Result<Tweet> result) {

                //Check if media is present
                if (result.data.extendedEtities == null && result.data.entities.media == null) {
                    alertNoMedia();
                }
                //Check if gif or mp4 present in the file
                else if (!(result.data.extendedEtities.media.get(0).type).equals("video") && !(result.data.extendedEtities.media.get(0).type).equals("animated_gif")) {
                    alertNoVideo();
                } else {
                    String filename = fname;
                    String url;

                    //Set filename to gif or mp4
                    if ((result.data.extendedEtities.media.get(0).type).equals("video")) {
                        filename = filename + ".mp4";
                    } else {
                        filename = filename + ".gif";
                    }

                    int i = 0;
                    url = result.data.extendedEtities.media.get(0).videoInfo.variants.get(i).url;
                    while (!url.contains(".mp4")) {
                        try {
                            if (result.data.extendedEtities.media.get(0).videoInfo.variants.get(i) != null) {
                                url = result.data.extendedEtities.media.get(0).videoInfo.variants.get(i).url;
                                i += 1;
                            }
                        } catch (IndexOutOfBoundsException e) {
                            downloadVideo(url, filename);
                        }
                    }

                    downloadVideo(url, filename);
                }
            }
            @Override
            public void failure(TwitterException exception) {
                progressDialog.hide();
                Toast.makeText(MainActivity.this, "Request Failed: Check your internet connection", Toast.LENGTH_SHORT).show();
            }
        });
    }


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private void downloadVideo(String url, String fname) {

        if (fname.endsWith(".mp4")) {
            progressDialog.setMessage("Fetching video...");
        } else {
            progressDialog.setMessage("Fetching gif...");
        }

        //Check if External Storage permission js allowed
        if (!storageAllowed()) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(this, Constant.PERMISSIONS_STORAGE, Constant.REQUEST_EXTERNAL_STORAGE);
            progressDialog.hide();
            Toast.makeText(this, "Kindly grant the request and try again", Toast.LENGTH_SHORT).show();
        } else {
            RxDownloader.getInstance(this)
                    .download(url, fname, "video/*") // url, filename, and mimeType
                    .subscribe(new Observer<String>() {
                        @Override
                        public void onCompleted() {
                            Toast.makeText(MainActivity.this, "Download Complete", Toast.LENGTH_SHORT).show();
                            loadLikeDialog();
                        }

                        @Override
                        public void onError(Throwable e) {
                            Toast.makeText(MainActivity.this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onNext(String s) {

                        }
                    });

            progressDialog.hide();
            Toast.makeText(this, "Download Started: Check Notification", Toast.LENGTH_LONG).show();
        }
    }

    private boolean storageAllowed() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int permission = ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
            return permission == PackageManager.PERMISSION_GRANTED;

        }

        return true;
    }

    private void alertNoVideo() {
        progressDialog.hide();
        Toast.makeText(this, "The url entered contains no video or gif file", Toast.LENGTH_LONG).show();
    }

    private void alertNoMedia() {
        progressDialog.hide();
        new AlertDialog.Builder(this)
                .setTitle("No Media File")
                .setMessage("Make sure the URL points to a vaild Twitter or Youtube Video or Gif")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                    }
                }).show();
        Toast.makeText(this, "The url entered contains no media file", Toast.LENGTH_LONG).show();
    }

    private void alertNoUrl() {
        progressDialog.hide();
        new AlertDialog.Builder(this)
                .setTitle("Empty Or Invalid URL")
                .setMessage("Please Share Video With lolo from Twitter or Youtube for it to Grab the URL")
                .setIcon(android.R.drawable.ic_dialog_info)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                }).show();
    }


    private Long getTweetId(String s) {
        try {
            String[] split = s.split("\\/");
            String id = split[5].split("\\?")[0];
            return Long.parseLong(id);
        } catch (Exception e) {
            Log.d("TAG", "getTweetId: " + e.getLocalizedMessage());
            alertNoUrl();
            return null;
        }
    }

    //Shows this only the first time
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private void loadLikeDialog() {

        if (sharedPreferences.getString(Constant.FIRSTRUN, "").equals("")) {

            AlertDialog.Builder builder = new AlertDialog.Builder(this)
                    .setCancelable(true)
                    .setView(R.layout.like_layout)
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    })
                    .setPositiveButton("Like", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {

                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setData(Uri.parse("https://goo.gl/forms/zN6CHfD2X9xojKeO2"));
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        }
                    });

            AlertDialog alertDialog = builder.create();
            alertDialog.show();

            sharedPreferences.edit().putString(Constant.FIRSTRUN, "no").apply();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /*@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.about) {
            startActivity(new Intent(this, AboutActivity.class));
        }
        if (item.getItemId() == R.id.web) {
            Intent urlIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://twittasave.net"));
            urlIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(urlIntent);
        }
        return super.onOptionsItemSelected(item);
    }*/
}
