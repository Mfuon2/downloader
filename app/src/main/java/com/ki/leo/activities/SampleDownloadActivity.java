package com.ki.leo.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.esafirm.rxdownloader.RxDownloader;
import com.ki.leo.R;
import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.TwitterApiClient;
import com.twitter.sdk.android.core.TwitterCore;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.models.Tweet;
import com.twitter.sdk.android.core.services.StatusesService;

import at.huber.youtubeExtractor.VideoMeta;
import at.huber.youtubeExtractor.YouTubeExtractor;
import at.huber.youtubeExtractor.YtFile;
import retrofit2.Call;

public class SampleDownloadActivity extends AppCompatActivity {

    private static String youtubeLink;
    private static String twitterLink;

    private LinearLayout mainLayout;
    private ProgressBar mainProgressBar;
    final Context c = this;
    String fname;

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String s = getIntent().getStringExtra("EXTRA_SESSION_ID");
        setContentView(R.layout.activity_sample_download);
        mainLayout = (LinearLayout) findViewById(R.id.main_layout);
        mainProgressBar = (ProgressBar) findViewById(R.id.prgrBar);
        // Check how it was started and if we can get the youtube link
        if (s != null && (s.contains("://youtu.be/") || s.contains("youtube.com/watch?v="))) {
            Toast.makeText(getApplicationContext(),"Saved instance TRUE"+s,Toast.LENGTH_LONG).show();
            youtubeLink = s;
            // We have a valid link
            getYoutubeDownloadUrl(youtubeLink);
        }

        if (savedInstanceState == null && Intent.ACTION_SEND.equals(getIntent().getAction())
                && getIntent().getType() != null && "text/plaiinstance nulln".equals(getIntent().getType())) {
            Toast.makeText(this, s, Toast.LENGTH_LONG).show();
            //getYoutubeDownloadUrl(youtubeLink);
            if (s != null && (s.contains("://youtu.be/") || s.contains("youtube.com/watch?v="))) {
                youtubeLink = s;
                // We have a valid link
                getYoutubeDownloadUrl(youtubeLink);
            }else if(s != null && (s.contains("twitter.com/"))){
                twitterLink = s;
                Long id = getTweetId(twitterLink);
                //Check if filename is set. If not, set the tweet Id as the filename
                if (twitterLink.length()>0) {
                    fname = twitterLink.trim();
                } else {
                    fname = String.valueOf(id);
                }
                //Call method to get tweet
                if (id !=null) {
                    getTweet(id, fname);
                }
            } else{
                Toast.makeText(this, R.string.error_no_yt_link, Toast.LENGTH_LONG).show();
                finish();
            }
        } else if (savedInstanceState != null && youtubeLink != null) {
            Toast.makeText(getApplicationContext(),"Saved instance TRUE"+s,Toast.LENGTH_LONG).show();
            getYoutubeDownloadUrl(youtubeLink);
        }else if (savedInstanceState != null && twitterLink != null) {
            Long id = getTweetId(twitterLink);
            //Check if filename is set. If not, set the tweet Id as the filename
            if (twitterLink.length()>0) {
                fname = twitterLink.trim();
            } else {
                fname = String.valueOf(id);
            }
            getTweet(id,fname);
        } else {
            finish();
        }
    }

    @SuppressLint("StaticFieldLeak")
    private void getYoutubeDownloadUrl(String youtubeLink) {
        new YouTubeExtractor(this) {

            @Override
            public void onExtractionComplete(SparseArray<YtFile> ytFiles, VideoMeta vMeta) {
                mainProgressBar.setVisibility(View.GONE);
                // Iterate over itags
                for (int i = 0, itag; i < ytFiles.size(); i++) {
                    itag = ytFiles.keyAt(i);
                    // ytFile represents one file with its url and meta data
                    YtFile ytFile = ytFiles.get(itag);
                    // Just add videos in a decent format => height -1 = audio
                    if (ytFile.getFormat().getHeight() == -1 || ytFile.getFormat().getHeight() >= 360) {
                        addButtonToMainLayout(vMeta.getTitle(), ytFile);
                    }
                }
            }
        }.extract(youtubeLink, true, false);
    }

    private void addButtonToMainLayout(final String videoTitle, final YtFile ytfile) {
        // Display some buttons and let the user choose the format
        String btnText = (ytfile.getFormat().getHeight() == -1) ? "Audio " +
                ytfile.getFormat().getAudioBitrate() + " kbit/s" :
                ytfile.getFormat().getHeight() + "p";
        btnText += (ytfile.getFormat().isDashContainer()) ? " dash" : "";
        Button btn = new Button(this);
        btn.setText(btnText);
        mainLayout.addView(btn);
        btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                // Code here executes on main thread after user presses button
                String filename;
                if (videoTitle.length() > 55) {
                    filename = videoTitle.substring(0, 55) + "." + ytfile.getFormat().getExt();
                } else {
                    filename = videoTitle + "." + ytfile.getFormat().getExt();
                }
                filename = filename.replaceAll("[\\\\><\"|*?%:#/]", "");
                //imRun(ytfile.getUrl(),videoTitle,filename);
                // downloadFromUrl(ytfile.getUrl(), videoTitle, filename);
                //Toast.makeText(getApplicationContext(), "TWO " + filename, Toast.LENGTH_LONG).show();
                //Uri uri = Uri.parse(ytfile.getUrl());


                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(ytfile.getUrl()));

                request.setTitle(videoTitle);

                request.allowScanningByMediaScanner();

                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);

                DownloadManager manager = (DownloadManager) c.getSystemService(Context.DOWNLOAD_SERVICE);
                manager.enqueue(request);
                finish();
            }
        });
    }

    public void getTweet(final Long id, final String fname){
        TwitterApiClient twitterApiClient= TwitterCore.getInstance().getApiClient();
        StatusesService statusesService=twitterApiClient.getStatusesService();
        Call<Tweet> tweetCall=statusesService.show(id,null,null,null);
        tweetCall.enqueue(new Callback<Tweet>() {
            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
            @Override
            public void success(Result<Tweet> result) {
                //Check if media is present
                if (result.data.extendedEtities==null && result.data.entities.media==null){
                    alertNoMedia();
                }
                else {
                    assert result.data.extendedEtities != null;
                    if (!(result.data.extendedEtities.media.get(0).type).equals("video") && !(result.data.extendedEtities.media.get(0).type).equals("animated_gif")){
                        alertNoVideo();
                    }
                    else {
                        String filename=fname;
                        String url;

                        //Set filename to gif or mp4
                        if ((result.data.extendedEtities.media.get(0).type).equals("video")) {
                            filename = filename + ".mp4";
                        }else {
                            filename = filename + ".gif";
                        }

                        int i=0;
                        url = result.data.extendedEtities.media.get(0).videoInfo.variants.get(i).url;
                        while (!url.contains(".mp4")){
                            try {
                                if (result.data.extendedEtities.media.get(0).videoInfo.variants.get(i) != null) {
                                    url = result.data.extendedEtities.media.get(0).videoInfo.variants.get(i).url;
                                    i += 1;
                                }
                            } catch (IndexOutOfBoundsException e) {
                                downloadVideo(url,filename);
                            }
                        }
                        downloadVideo(url,filename);
                    }
                }
            }

            @Override
            public void failure(TwitterException exception) {
                Toast.makeText(SampleDownloadActivity.this, "Request Failed: Check your internet connection", Toast.LENGTH_SHORT).show();
            }
        });
    }


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private void downloadVideo(String url, String fname) {
        //Check if External Storage permission js allowed
        if (!storageAllowed()) {
            ActivityCompat.requestPermissions(SampleDownloadActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
            ActivityCompat.requestPermissions(SampleDownloadActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            ActivityCompat.requestPermissions(SampleDownloadActivity.this, new String[]{Manifest.permission.READ_PHONE_STATE}, 1);
            ActivityCompat.requestPermissions(SampleDownloadActivity.this, new String[]{Manifest.permission.INTERNET}, 1);
            Toast.makeText(this, "Kindly grant the request and try again", Toast.LENGTH_SHORT).show();
        }else {
            RxDownloader.getInstance(this)
                    .download(url,fname, "video/*"); // url, filename, and mimeType

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
        Toast.makeText(this, "The url entered contains no video or gif file", Toast.LENGTH_LONG).show();
    }

    private void alertNoMedia() {
        Toast.makeText(this, "The url entered contains no media file", Toast.LENGTH_LONG).show();
    }

    private void alertNoUrl() {
        Toast.makeText(SampleDownloadActivity.this, "Enter a correct tweet url", Toast.LENGTH_LONG).show();
    }


    private Long getTweetId(String s) {
        try {
            String[] split = s.split("\\/");
            String id = split[5].split("\\?")[0];
            return Long.parseLong(id);
        }catch (Exception e){
            Log.d("TAG", "getTweetId: "+e.getLocalizedMessage());
            alertNoUrl();
            return null;
        }
    }
}
