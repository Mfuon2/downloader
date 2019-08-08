package com.ki.leo.activities;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.ki.leo.R;

import at.huber.youtubeExtractor.VideoMeta;
import at.huber.youtubeExtractor.YouTubeExtractor;
import at.huber.youtubeExtractor.YtFile;

import static com.ki.leo.R.drawable.*;

public class DownloadYoutube extends AppCompatActivity {

    ProgressBar mainProgressBar;
    private LinearLayout mainLayout;
    final Context c = this;
    Button btn_download;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download_youtube);
        String s = getIntent().getStringExtra("EXTRA_SESSION_ID");
        mainLayout = findViewById(R.id.main_layout);
        mainProgressBar = findViewById(R.id.prgrBar);
        btn_download = findViewById(R.id.btn_download);
        if(s.length() > 0){
            mainProgressBar.setActivated(true);
            getYoutubeDownloadUrl(s);
        }


    }

    @SuppressLint("StaticFieldLeak")
    private void getYoutubeDownloadUrl(String youtubeLink) {
        new YouTubeExtractor(this) {
            @Override
            public void onExtractionComplete(SparseArray<YtFile> ytFiles, VideoMeta vMeta) {
                mainProgressBar.setVisibility(View.GONE);
                for (int i = 0, itag; i < ytFiles.size(); i++) {
                    itag = ytFiles.keyAt(i);
                    YtFile ytFile = ytFiles.get(itag);
                    if (ytFile.getFormat().getHeight() == -1 || ytFile.getFormat().getHeight() >= 20) {
                        addButtonToMainLayout(vMeta.getVideoId(), ytFile);
                    }
                }
            }
        }.extract(youtubeLink, true, false);
    }

    @SuppressLint("ResourceAsColor")
    private void addButtonToMainLayout(final String videoTitle, final YtFile ytfile) {
        // Display some buttons and let the user choose the format
        String btnText = "";

        if (!ytfile.getFormat().isDashContainer()) {
            btnText = (ytfile.getFormat().getHeight() == -1) ? "Audio " +
                    ytfile.getFormat().getAudioBitrate() + " kbit/s" :
                    ytfile.getFormat().getHeight() + "p";
            Button btn = new Button(this);
            btn.setPadding(16, 1, 16, 1);
            btn.setTextColor(R.color.colorPrimary);
            //btn.setBackgroundColor(R.color.colorWhite);
            btn.setText(btnText);
            mainLayout.addView(btn);

            final String finalBtnText = btnText;
            btn.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    new AlertDialog.Builder(c)
                            .setTitle("Info")
                            .setMessage("Download File " + videoTitle + " ~ " + finalBtnText)
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setPositiveButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    finish();
                                }
                            })
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    // Code here executes on main thread after user presses button
                                    String filename;
                                    if (videoTitle.length() > 55) {
                                        filename = videoTitle.substring(0, 55) + "." + ytfile.getFormat().getExt();
                                    } else {
                                        filename = videoTitle + "." + ytfile.getFormat().getExt();
                                    }
                                    filename = filename.replaceAll("[\\\\><\"|*?%:#/]", "");
                                    DownloadManager.Request request = new DownloadManager.Request(Uri.parse(ytfile.getUrl()));
                                    request.setTitle(videoTitle);
                                    request.allowScanningByMediaScanner();

                                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                                    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);

                                    DownloadManager manager = (DownloadManager) c.getSystemService(Context.DOWNLOAD_SERVICE);
                                    manager.enqueue(request);
                                    Toast.makeText(c, "Download Started..! Check Notifications", Toast.LENGTH_LONG).show();
                                    finish();
                                }
                            }).show();
                }
            });
        }
    }
}
