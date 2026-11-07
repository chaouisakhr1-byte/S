package com.sstar.video;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.util.concurrent.*;

public final class LiveHubActivity extends Activity {
  private final ExecutorService io = Executors.newSingleThreadExecutor();
  private final Handler handler = new Handler(Looper.getMainLooper());
  private Supabase api;
  private LinearLayout list;
  private TextView notice;
  private Runnable refresh;
  private static final int DARK = Color.rgb(20,22,29);
  private static final int PURPLE = Color.rgb(105,78,240);

  @Override public void onCreate(Bundle state) {
    super.onCreate(state);
    api = new Supabase(this);
    LinearLayout root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setBackgroundColor(Color.rgb(247,247,250));
    TextView header = new TextView(this);
    header.setText("S  ·  LIVE");
    header.setTextSize(23);
    header.setTextColor(Color.WHITE);
    header.setPadding(20,20,16,20);
    header.setBackgroundColor(DARK);
    root.addView(header);
    LinearLayout actions = new LinearLayout(this);
    Button create = new Button(this); create.setText("Start a live");
    Button reload = new Button(this); reload.setText("Refresh");
    actions.addView(create,new LinearLayout.LayoutParams(0,-2,1));
    actions.addView(reload,new LinearLayout.LayoutParams(0,-2,1));
    root.addView(actions);
    notice = new TextView(this); notice.setTextColor(DARK); notice.setPadding(16,8,16,8);
    root.addView(notice);
    ScrollView scroll = new ScrollView(this);
    list = new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL); list.setPadding(14,8,14,24);
    scroll.addView(list); root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
    setContentView(root);
    create.setOnClickListener(v -> createStream());
    reload.setOnClickListener(v -> loadStreams());
    loadStreams();
    refresh = new Runnable(){ public void run(){ loadStreams(); handler.postDelayed(this,12000); }};
    handler.postDelayed(refresh,12000);
  }

  private TextView label(String value,int size,int color) {
    TextView t=new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color); t.setPadding(8,8,8,8); return t;
  }

  private void loadStreams() {
    notice.setText("Loading live broadcasts…");
    io.execute(() -> {
      try {
        JSONObject result=api.request("GET","/rest/v1/live_streams?select=id,host_id,title,description,status,playback_url,started_at,created_at&or=(status.eq.live,host_id.eq."+api.userId+")&order=created_at.desc&limit=50",null,null);
        JSONArray rows=result.optJSONArray("data");
        runOnUiThread(() -> render(rows==null?new JSONArray():rows));
      } catch(Exception e) { runOnUiThread(() -> notice.setText("Could not load live broadcasts: "+e.getMessage())); }
    });
  }

  private void render(JSONArray rows) {
    list.removeAllViews();
    notice.setText(rows.length()+" live / scheduled broadcast(s)");
    if(rows.length()==0) list.addView(label("No live broadcasts yet. Start the first broadcast.",16,DARK));
    for(int i=0;i<rows.length();i++) {
      JSONObject row=rows.optJSONObject(i); if(row==null) continue;
      String id=row.optString("id","");
      String title=row.optString("title","Live broadcast");
      String host=row.optString("host_id","");
      LinearLayout card=new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL);
      card.setPadding(14,12,14,12); card.setBackgroundColor(Color.WHITE);
      LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2); cp.bottomMargin=14; list.addView(card,cp);
      String status=row.optString("status","live");
      boolean mine=!host.isEmpty()&&host.equals(api.userId);
      boolean liveNow="live".equals(status);
      TextView badge=label(liveNow?"● LIVE":"◷ SCHEDULED",12,liveNow?Color.rgb(205,35,65):PURPLE); card.addView(badge);
      card.addView(label(title,19,DARK));
      card.addView(label(mine?"Your broadcast":"Host: "+host,12,Color.DKGRAY));
      if(liveNow){
        Button chat=new Button(this); chat.setText("Open live chat"); card.addView(chat);
        chat.setOnClickListener(v -> openChat(id,title));
      }
      if(mine&&!liveNow){
        Button resume=new Button(this); resume.setText("Continue studio"); card.addView(resume);
        resume.setOnClickListener(v -> {
          Intent intent=new Intent(this,LiveBroadcastActivity.class);
          intent.putExtra("stream_id",id); intent.putExtra("stream_title",title); startActivity(intent);
        });
      }
      String playback=row.optString("playback_url","");
      if(liveNow&&(playback.startsWith("https://")||playback.startsWith("http://"))) {
        Button watch=new Button(this); watch.setText("Watch stream"); card.addView(watch);
        watch.setOnClickListener(v -> {
          Intent intent=new Intent(this,VideoPlaybackActivity.class);
          intent.putExtra("url",playback); intent.putExtra("title",title); startActivity(intent);
        });
      }
    }
  }

  private void createStream() {
    EditText title=new EditText(this); title.setHint("Broadcast title");
    new AlertDialog.Builder(this).setTitle("Create live broadcast").setView(title)
      .setNegativeButton("Cancel",null).setPositiveButton("Continue",(d,w)->{
        String name=title.getText().toString().trim(); if(name.isEmpty()) name="Live with S";
        final String streamTitle=name;
        io.execute(()->{
          try {
            JSONObject row=new JSONObject(); row.put("host_id",api.userId); row.put("title",streamTitle);
            row.put("description",""); row.put("status","scheduled");
            JSONObject result=api.request("POST","/rest/v1/live_streams",row,"return=representation");
            JSONArray data=result.optJSONArray("data");
            String id=data!=null&&data.length()>0?data.getJSONObject(0).optString("id"):"";
            runOnUiThread(()->{
              Intent intent=new Intent(this,LiveBroadcastActivity.class);
              intent.putExtra("stream_id",id); intent.putExtra("stream_title",streamTitle);
              startActivity(intent);
            });
          } catch(Exception e) { runOnUiThread(()->new AlertDialog.Builder(this).setMessage("Could not create broadcast: "+e.getMessage()).setPositiveButton("OK",null).show()); }
        });
      }).show();
  }

  private void openChat(String streamId,String title) {
    LinearLayout panel=new LinearLayout(this); panel.setOrientation(LinearLayout.VERTICAL);
    TextView messages=new TextView(this); messages.setText("Loading messages…"); messages.setTextColor(DARK); messages.setPadding(8,8,8,8);
    ScrollView scroll=new ScrollView(this); scroll.addView(messages);
    panel.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
    EditText input=new EditText(this); input.setHint("Write a message");
    if(api.accessToken.isEmpty()){input.setEnabled(false);input.setHint("Sign in to send messages");}
    panel.addView(input);
    AlertDialog dialog=new AlertDialog.Builder(this).setTitle(title+" · Chat").setView(panel)
      .setNegativeButton("Close",null).setPositiveButton("Send",null).create();
    dialog.setOnShowListener(x -> dialog.getButton(-1).setOnClickListener(v -> {
      String body=input.getText().toString().trim(); if(body.isEmpty()) return;
      input.setText("");
      io.execute(()->{
        try {
          JSONObject row=new JSONObject(); row.put("stream_id",streamId); row.put("user_id",api.userId); row.put("body",body);
          api.request("POST","/rest/v1/live_chat_messages",row,"return=minimal");
          fetchChat(streamId,messages);
        } catch(Exception e) { runOnUiThread(()->Toast.makeText(this,"Message failed: "+e.getMessage(),Toast.LENGTH_LONG).show()); }
      });
    }));
    dialog.show();
    fetchChat(streamId,messages);
    final Runnable poll=new Runnable(){ public void run(){ if(dialog.isShowing()){fetchChat(streamId,messages);handler.postDelayed(this,3500);} }};
    handler.postDelayed(poll,3500);
    dialog.setOnDismissListener(x -> handler.removeCallbacks(poll));
  }

  private void fetchChat(String streamId,TextView target) {
    io.execute(()->{
      try {
        JSONObject result=api.request("GET","/rest/v1/live_chat_messages?select=user_id,body,created_at&stream_id=eq."+streamId+"&order=created_at.asc&limit=100",null,null);
        JSONArray rows=result.optJSONArray("data"); StringBuilder out=new StringBuilder();
        if(rows!=null) for(int i=0;i<rows.length();i++){JSONObject r=rows.optJSONObject(i);if(r!=null)out.append(r.optString("user_id","user").substring(0,Math.min(8,r.optString("user_id","user").length()))).append(": ").append(r.optString("body","")).append("\n");}
        runOnUiThread(()->target.setText(out.length()==0?"No messages yet. Say hello!":out.toString()));
      } catch(Exception e) { runOnUiThread(()->target.setText("Chat unavailable: "+e.getMessage())); }
    });
  }

  @Override protected void onDestroy(){handler.removeCallbacksAndMessages(null);io.shutdown();super.onDestroy();}
}
