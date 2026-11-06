package com.sstar.video;

import android.app.Activity;
import android.os.Bundle;
import android.Manifest;
import android.content.pm.PackageManager;
import android.view.SurfaceView;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.*;
import android.graphics.Color;
import android.view.ViewGroup;
import com.pedro.common.ConnectChecker;
import com.pedro.library.rtmp.RtmpCamera1;

/** Native camera-to-RTMP broadcaster. Requires camera and microphone permission. */
public final class LiveBroadcastActivity extends Activity implements ConnectChecker, SurfaceHolder.Callback {
  private static final int PERMISSIONS = 701;
  private RtmpCamera1 streamer;
  private SurfaceView preview;
  private EditText streamUrl;
  private TextView state;
  private Button start;
  private boolean surfaceReady = false;

  @Override public void onCreate(Bundle saved) {
    super.onCreate(saved);
    LinearLayout root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(16,16,16,16);
    root.setBackgroundColor(Color.rgb(18,20,27));
    TextView heading = new TextView(this);
    heading.setText("S · LIVE STUDIO");
    heading.setTextColor(Color.WHITE);
    heading.setTextSize(22);
    root.addView(heading);
    preview = new SurfaceView(this);
    root.addView(preview,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1));
    preview.getHolder().addCallback(this);
    streamUrl = new EditText(this);
    streamUrl.setSingleLine(true);
    streamUrl.setTextColor(Color.WHITE);
    streamUrl.setHintTextColor(Color.LTGRAY);
    streamUrl.setHint("rtmp://server/app/stream-key");
    root.addView(streamUrl);
    state = new TextView(this);
    state.setTextColor(Color.WHITE);
    state.setText("Waiting for camera permission");
    root.addView(state);
    LinearLayout controls = new LinearLayout(this);
    start = new Button(this); start.setText("Start broadcast");
    Button flip = new Button(this); flip.setText("Switch camera");
    Button close = new Button(this); close.setText("Close");
    controls.addView(start,new LinearLayout.LayoutParams(0,-2,1));
    controls.addView(flip,new LinearLayout.LayoutParams(0,-2,1));
    controls.addView(close,new LinearLayout.LayoutParams(0,-2,1));
    root.addView(controls);
    setContentView(root);
    start.setOnClickListener(v -> startOrStop());
    flip.setOnClickListener(v -> { if(streamer!=null) streamer.switchCamera(); });
    close.setOnClickListener(v -> finish());
    ensurePermissions();
  }

  private void ensurePermissions() {
    if(checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED ||
       checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED) {
      requestPermissions(new String[]{Manifest.permission.CAMERA,Manifest.permission.RECORD_AUDIO},PERMISSIONS);
    } else initStreamer();
  }

  @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] results) {
    super.onRequestPermissionsResult(requestCode,permissions,results);
    if(requestCode==PERMISSIONS) {
      boolean ok=true; for(int r:results) if(r!=PackageManager.PERMISSION_GRANTED) ok=false;
      if(ok) initStreamer(); else state.setText("Camera and microphone permissions are required.");
    }
  }

  private void initStreamer() {
    if(streamer!=null) return;
    streamer=new RtmpCamera1(preview,this);
    boolean video=streamer.prepareVideo(1280,720,30,1800*1024,0);
    boolean audio=streamer.prepareAudio(128*1024,44100,true);
    if(!video || !audio) { state.setText("This device could not prepare camera/audio encoders."); return; }
    if(surfaceReady) streamer.startPreview();
    state.setText("Preview ready · enter your RTMP URL");
  }

  private void startOrStop() {
    if(streamer==null) { ensurePermissions(); return; }
    if(streamer.isStreaming()) {
      streamer.stopStream();
      start.setText("Start broadcast");
      state.setText("Broadcast stopped");
      return;
    }
    String url=streamUrl.getText().toString().trim();
    if(!url.startsWith("rtmp://") && !url.startsWith("rtmps://")) {
      state.setText("Enter a valid RTMP or RTMPS publishing URL.");
      return;
    }
    if(!streamer.isOnPreview()) streamer.startPreview();
    streamer.startStream(url);
    start.setText("Stop broadcast");
    state.setText("Connecting to streaming server…");
  }

  @Override public void surfaceCreated(SurfaceHolder holder) { surfaceReady=true; if(streamer!=null&&!streamer.isOnPreview()) streamer.startPreview(); }
  @Override public void surfaceChanged(SurfaceHolder holder,int format,int width,int height) {}
  @Override public void surfaceDestroyed(SurfaceHolder holder) { surfaceReady=false; if(streamer!=null&&!streamer.isStreaming()) streamer.stopPreview(); }
  @Override public void onConnectionStarted(String url) { runOnUiThread(()->state.setText("Connecting…")); }
  @Override public void onConnectionSuccess() { runOnUiThread(()->state.setText("LIVE · Connected")); }
  @Override public void onConnectionFailed(String reason) { runOnUiThread(()->{state.setText("Broadcast failed: "+reason);start.setText("Start broadcast");}); }
  @Override public void onDisconnect() { runOnUiThread(()->{state.setText("Disconnected");start.setText("Start broadcast");}); }
  @Override public void onAuthError() { runOnUiThread(()->{state.setText("Streaming authentication failed");start.setText("Start broadcast");}); }
  @Override public void onAuthSuccess() { runOnUiThread(()->state.setText("Streaming server authenticated")); }
  @Override protected void onDestroy() { if(streamer!=null){if(streamer.isStreaming())streamer.stopStream();if(streamer.isOnPreview())streamer.stopPreview();streamer.release();streamer=null;}super.onDestroy(); }
}
