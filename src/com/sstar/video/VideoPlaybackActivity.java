package com.sstar.video;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.*;
import android.graphics.Color;

public final class VideoPlaybackActivity extends Activity {
  @Override public void onCreate(Bundle state) {
    super.onCreate(state);
    String url=getIntent().getStringExtra("url");
    String title=getIntent().getStringExtra("title");
    LinearLayout root=new LinearLayout(this); root.setOrientation(1); root.setBackgroundColor(Color.BLACK);
    TextView heading=new TextView(this); heading.setText(title==null?"S Video":title); heading.setTextColor(Color.WHITE); heading.setTextSize(19); heading.setPadding(16,16,16,16); root.addView(heading);
    VideoView video=new VideoView(this); root.addView(video,new LinearLayout.LayoutParams(-1,0,1));
    if(url!=null&&!url.isEmpty()){video.setVideoURI(Uri.parse(url));video.setOnPreparedListener(mp->{mp.setLooping(false);video.start();});video.setOnErrorListener((mp,what,extra)->{Toast.makeText(this,"Unable to play this stream or video URL.",Toast.LENGTH_LONG).show();return true;});}
    Button close=new Button(this);close.setText("Close");close.setOnClickListener(v->finish());root.addView(close);
    setContentView(root);
  }
}
