package com.sstar.video;

import android.app.*;
import android.os.*;
import android.Manifest;
import android.content.pm.PackageManager;
import android.view.SurfaceView;
import android.view.SurfaceHolder;
import android.view.ViewGroup;
import android.widget.*;
import android.graphics.Color;
import com.pedro.common.ConnectChecker;
import com.pedro.library.rtmp.RtmpCamera1;
import org.json.*;
import java.util.concurrent.*;

public final class LiveBroadcastActivity extends Activity implements ConnectChecker, SurfaceHolder.Callback {
  private static final int PERMISSIONS=701;
  private final ExecutorService io=Executors.newSingleThreadExecutor();
  private final Handler handler=new Handler(Looper.getMainLooper());
  private Supabase api; private String streamId="",streamTitle="S Live";
  private RtmpCamera1 streamer; private SurfaceView preview; private EditText streamUrl;
  private TextView state; private Button start; private boolean surfaceReady=false, finishingStream=false;
  private static final int DARK=Color.rgb(18,20,27);

  @Override public void onCreate(Bundle saved) {
    super.onCreate(saved); api=new Supabase(this);
    streamId=getIntent().getStringExtra("stream_id"); if(streamId==null)streamId="";
    String t=getIntent().getStringExtra("stream_title"); if(t!=null&&!t.isEmpty())streamTitle=t;
    LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(14,14,14,14); root.setBackgroundColor(DARK);
    TextView heading=new TextView(this); heading.setText("S · LIVE STUDIO\n"+streamTitle); heading.setTextColor(Color.WHITE); heading.setTextSize(20); root.addView(heading);
    preview=new SurfaceView(this); preview.getHolder().addCallback(this); root.addView(preview,new LinearLayout.LayoutParams(-1,0,1));
    streamUrl=new EditText(this); streamUrl.setSingleLine(true); streamUrl.setTextColor(Color.WHITE); streamUrl.setHintTextColor(Color.LTGRAY); streamUrl.setHint("RTMP/RTMPS publishing URL"); root.addView(streamUrl);
    state=new TextView(this); state.setTextColor(Color.WHITE); state.setText("Allow camera and microphone to prepare your studio."); root.addView(state);
    LinearLayout controls=new LinearLayout(this);
    start=new Button(this); start.setText("Start broadcast");
    Button flip=new Button(this); flip.setText("Switch camera");
    Button chat=new Button(this); chat.setText("Live chat");
    Button close=new Button(this); close.setText("End live");
    controls.addView(start,new LinearLayout.LayoutParams(0,-2,1)); controls.addView(flip,new LinearLayout.LayoutParams(0,-2,1));
    controls.addView(chat,new LinearLayout.LayoutParams(0,-2,1)); controls.addView(close,new LinearLayout.LayoutParams(0,-2,1)); root.addView(controls);
    setContentView(root);
    start.setOnClickListener(v->startOrStop()); flip.setOnClickListener(v->{if(streamer!=null)streamer.switchCamera();});
    chat.setOnClickListener(v->openChat()); close.setOnClickListener(v->endLive());
    ensurePermissions();
  }

  private void ensurePermissions(){
    if(checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED||checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED)
      requestPermissions(new String[]{Manifest.permission.CAMERA,Manifest.permission.RECORD_AUDIO},PERMISSIONS);
    else initStreamer();
  }
  @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] results){
    super.onRequestPermissionsResult(requestCode,permissions,results);
    if(requestCode==PERMISSIONS){boolean ok=true;for(int r:results)if(r!=PackageManager.PERMISSION_GRANTED)ok=false;if(ok)initStreamer();else state.setText("Camera and microphone permissions are required.");}
  }
  private void initStreamer(){
    if(streamer!=null)return; streamer=new RtmpCamera1(preview,this);
    boolean video=streamer.prepareVideo(1280,720,30,1800*1024,0); boolean audio=streamer.prepareAudio(128*1024,44100,true);
    if(!video||!audio){state.setText("This device could not prepare camera/audio encoders.");return;}
    if(surfaceReady)streamer.startPreview(); state.setText("Studio ready · enter your RTMP publishing URL.");
  }
  private void startOrStop(){
    if(streamer==null){ensurePermissions();return;}
    if(streamer.isStreaming()){streamer.stopStream();start.setText("Start broadcast");state.setText("Broadcast stopped");return;}
    String url=streamUrl.getText().toString().trim();
    if(!url.startsWith("rtmp://")&&!url.startsWith("rtmps://")){state.setText("Enter a valid RTMP or RTMPS publishing URL.");return;}
    if(!streamer.isOnPreview())streamer.startPreview(); streamer.startStream(url); start.setText("Stop broadcast");state.setText("Connecting to streaming server…");
  }
  private void openChat(){
    if(streamId.isEmpty()){new AlertDialog.Builder(this).setMessage("This live session has no database chat room. Start it from the Live Hub.").setPositiveButton("OK",null).show();return;}
    LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);
    TextView messages=new TextView(this);messages.setTextColor(Color.WHITE);messages.setPadding(8,8,8,8);
    ScrollView scroll=new ScrollView(this);scroll.addView(messages);panel.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
    EditText input=new EditText(this);input.setHint("Write to viewers");panel.addView(input);
    AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Live chat").setView(panel).setNegativeButton("Close",null).setPositiveButton("Send",null).create();
    dialog.setOnShowListener(x->dialog.getButton(-1).setOnClickListener(v->{
      String body=input.getText().toString().trim();if(body.isEmpty())return;input.setText("");
      io.execute(()->{try{JSONObject row=new JSONObject();row.put("stream_id",streamId);row.put("user_id",api.userId);row.put("body",body);api.request("POST","/rest/v1/live_chat_messages",row,"return=minimal");fetchChat(messages);}catch(Exception e){runOnUiThread(()->Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show());}});
    }));
    dialog.show();fetchChat(messages);
    final Runnable poll=new Runnable(){public void run(){if(dialog.isShowing()){fetchChat(messages);handler.postDelayed(this,3500);}}};
    handler.postDelayed(poll,3500);dialog.setOnDismissListener(x->handler.removeCallbacks(poll));
  }
  private void fetchChat(TextView target){
    io.execute(()->{try{JSONObject r=api.request("GET","/rest/v1/live_chat_messages?select=user_id,body&stream_id=eq."+streamId+"&order=created_at.asc&limit=100",null,null);JSONArray a=r.optJSONArray("data");StringBuilder b=new StringBuilder();if(a!=null)for(int i=0;i<a.length();i++){JSONObject m=a.optJSONObject(i);if(m!=null){String id=m.optString("user_id","user");b.append(id.substring(0,Math.min(8,id.length()))).append(": ").append(m.optString("body","")).append("\n");}}runOnUiThread(()->target.setText(b.length()==0?"No messages yet.":b.toString()));}catch(Exception e){runOnUiThread(()->target.setText(e.getMessage()));}});
  }
  private void endLive(){
    if(streamer!=null){if(streamer.isStreaming())streamer.stopStream();if(streamer.isOnPreview())streamer.stopPreview();}
    if(!streamId.isEmpty()&&!finishingStream){finishingStream=true;io.execute(()->{try{JSONObject row=new JSONObject();row.put("status","ended");api.request("PATCH","/rest/v1/live_streams?id=eq."+streamId,row,"return=minimal");}catch(Exception ignored){}runOnUiThread(this::finish);});}
    else finish();
  }
  @Override public void surfaceCreated(SurfaceHolder holder){surfaceReady=true;if(streamer!=null&&!streamer.isOnPreview())streamer.startPreview();}
  @Override public void surfaceChanged(SurfaceHolder holder,int format,int width,int height){}
  @Override public void surfaceDestroyed(SurfaceHolder holder){surfaceReady=false;if(streamer!=null&&!streamer.isStreaming())streamer.stopPreview();}
  @Override public void onConnectionStarted(String url){runOnUiThread(()->state.setText("Connecting…"));}
  @Override public void onConnectionSuccess(){runOnUiThread(()->state.setText("LIVE · Connected"));}
  @Override public void onConnectionFailed(String reason){runOnUiThread(()->{state.setText("Broadcast failed: "+reason);start.setText("Start broadcast");});}
  @Override public void onDisconnect(){runOnUiThread(()->{state.setText("Disconnected");start.setText("Start broadcast");});}
  @Override public void onAuthError(){runOnUiThread(()->{state.setText("Streaming authentication failed");start.setText("Start broadcast");});}
  @Override public void onAuthSuccess(){runOnUiThread(()->state.setText("Streaming server authenticated"));}
  @Override protected void onDestroy(){handler.removeCallbacksAndMessages(null);if(streamer!=null){if(streamer.isStreaming())streamer.stopStream();if(streamer.isOnPreview())streamer.stopPreview();streamer.release();streamer=null;}io.shutdown();super.onDestroy();}
}
