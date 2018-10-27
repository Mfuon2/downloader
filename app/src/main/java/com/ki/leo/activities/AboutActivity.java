/*
 * Copyright (C) 2018  ~LMfuon
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

import android.app.ActionBar;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.ki.leo.R;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_about);


        /*TextView txt_link= (TextView) findViewById(R.id.txt_link);
        TextView txt_link_facebook= (TextView) findViewById(R.id.txt_link_facebook);*/
        TextView txt_link_review= (TextView) findViewById(R.id.txt_link_review);

//        txt_link.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Intent urlIntent=new Intent(Intent.ACTION_VIEW, Uri.parse("https://twitter.com/trapa___"));
//                urlIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                startActivity(urlIntent);

    /*    *.iml
                .gradle
                /local.properties
                /.idea/workspace.xml
                /.idea/libraries
                .DS_Store
                /build
                /captures
                .externalNativeBuild*/
//            }
//        });
//
//        txt_link_facebook.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Intent urlIntent=new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.facebook.com/mfuon"));
//                urlIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                startActivity(urlIntent);
//            }
//        });

        txt_link_review.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent urlIntent=new Intent(Intent.ACTION_VIEW, Uri.parse("https://goo.gl/forms/zN6CHfD2X9xojKeO2"));
                urlIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(urlIntent);
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId()==android.R.id.home){
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

}
