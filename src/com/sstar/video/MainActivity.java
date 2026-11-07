package com.sstar.video;

import android.app.*;import android.os.*;import android.content.*;import android.database.Cursor;import android.net.Uri;import android.provider.OpenableColumns;import android.view.*;import android.widget.*;import android.graphics.Color;import android.graphics.Typeface;import android.graphics.drawable.GradientDrawable;import android.view.Gravity;
import org.json.*;import java.util.concurrent.*;import java.io.*;

public class MainActivity extends Activity {
  final ExecutorService io=Executors.newSingleThreadExecutor(); Supabase api; LinearLayout root,content,bar; TextView status; JSONArray videos=new JSONArray(); java.util.Map<String,String> creatorNames=new java.util.HashMap<>(); java.util.Set<String> likedVideoIds=new java.util.HashSet<>(), followedCreatorIds=new java.util.HashSet<>();
  static final int PICK_VIDEO=41; int dark=Color.rgb(22,24,29), accent=Color.rgb(110,76,255);
  public void onCreate(Bundle b){super.onCreate(b);api=new Supabase(this);getWindow().setStatusBarColor(dark);getWindow().setNavigationBarColor(dark);if(api.accessToken.isEmpty())showAuth();else showHome();handleGoogleCallback(getIntent());}
  protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);handleGoogleCallback(intent);}
  void startGoogleSignIn(){
    try{
      String redirect="com.sstar.video://auth/callback";
      byte[] random=new byte[32];new java.security.SecureRandom().nextBytes(random);
      String verifier=android.util.Base64.encodeToString(random,android.util.Base64.URL_SAFE|android.util.Base64.NO_WRAP|android.util.Base64.NO_PADDING);
      byte[] digest=java.security.MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(java.nio.charset.StandardCharsets.US_ASCII));
      String challenge=android.util.Base64.encodeToString(digest,android.util.Base64.URL_SAFE|android.util.Base64.NO_WRAP|android.util.Base64.NO_PADDING);
      byte[] stateBytes=new byte[24];new java.security.SecureRandom().nextBytes(stateBytes);
      String state=android.util.Base64.encodeToString(stateBytes,android.util.Base64.URL_SAFE|android.util.Base64.NO_WRAP|android.util.Base64.NO_PADDING);
      getSharedPreferences("s_oauth",0).edit().putString("pkce_verifier",verifier).putString("oauth_state",state).apply();
      String url=Supabase.BASE+"/auth/v1/authorize?provider=google&redirect_to="+java.net.URLEncoder.encode(redirect,"UTF-8")+"&code_challenge="+java.net.URLEncoder.encode(challenge,"UTF-8")+"&code_challenge_method=S256&state="+java.net.URLEncoder.encode(state,"UTF-8")+"&apikey="+java.net.URLEncoder.encode(Supabase.KEY,"UTF-8");
      startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));
    }catch(Exception ex){showAuth();showAuthError("Could not start Google sign-in: "+ex.getMessage());}
  }
  void showAuthError(String message){
    if(status==null)return;
    status.setText(message);
    status.setTextColor(Color.rgb(190,35,45));
    status.setVisibility(View.VISIBLE);
  }
  void handleGoogleCallback(Intent intent){
    Uri data=intent==null?null:intent.getData();
    if(data==null||!"com.sstar.video".equals(data.getScheme())||!"auth".equals(data.getHost()))return;
    String error=data.getQueryParameter("error_description");
    if(error==null)error=data.getQueryParameter("error");
    if(error!=null){showAuth();showAuthError("Google sign-in failed: "+error);return;}
    String returnedState=data.getQueryParameter("state");
    String expectedState=getSharedPreferences("s_oauth",0).getString("oauth_state","");
    if(expectedState.isEmpty()||returnedState==null||!expectedState.equals(returnedState)){
      showAuth();showAuthError("Google sign-in could not be verified. Please try again.");return;
    }
    String code=data.getQueryParameter("code");
    if(code==null||code.isEmpty()){
      showAuth();showAuthError("Google did not return an authorization code.");return;
    }
    String verifier=getSharedPreferences("s_oauth",0).getString("pkce_verifier","");
    if(verifier.isEmpty()){showAuth();showAuthError("The secure sign-in session expired. Please try again.");return;}
    showAuth();status.setText("Finishing Google sign-in…");status.setTextColor(Color.DKGRAY);status.setVisibility(View.VISIBLE);
    io.execute(()->{
      try{
        JSONObject payload=new JSONObject();payload.put("auth_code",code);payload.put("code_verifier",verifier);
        JSONObject session=api.request("POST","/auth/v1/token?grant_type=pkce",payload,null);
        String token=session.optString("access_token",""),refresh=session.optString("refresh_token","");
        JSONObject user=session.optJSONObject("user");
        String uid=user==null?"":user.optString("id",""),mail=user==null?"":user.optString("email","");
        if(token.isEmpty()||uid.isEmpty())throw new Exception("Supabase did not return a valid user session.");
        api.save(token,uid,mail,refresh);
        getSharedPreferences("s_oauth",0).edit().clear().apply();
        runOnUiThread(this::showHome);
      }catch(Exception ex){runOnUiThread(()->{showAuth();showAuthError("Google sign-in could not be completed: "+ex.getMessage());});}
    });
  }
  GradientDrawable shape(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(radius);return d;}
  TextView text(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setPadding(12,10,12,10);return t;}
  Button button(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextColor(Color.WHITE);b.setTextSize(13);b.setMinHeight(44);b.setPadding(14,4,14,4);b.setBackground(shape(accent,24));if(android.os.Build.VERSION.SDK_INT>=21)b.setStateListAnimator(null);return b;}
  void base(String title){root=new LinearLayout(this);root.setOrientation(1);root.setBackgroundColor(Color.rgb(12,14,20));setContentView(root);LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);header.setPadding(8,8,16,8);header.setBackgroundColor(Color.rgb(12,14,20));TextView brand=text("S",27,Color.WHITE);brand.setTypeface(null,Typeface.BOLD);header.addView(brand);TextView heading=text(title,17,Color.WHITE);heading.setTypeface(null,Typeface.BOLD);header.addView(heading);root.addView(header);ScrollView sc=new ScrollView(this);sc.setFillViewport(true);content=new LinearLayout(this);content.setPadding(16,14,16,24);content.setOrientation(1);content.setBackgroundColor(Color.rgb(245,246,250));sc.addView(content);root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));}
  boolean requireAuth(){if(!api.accessToken.isEmpty())return true;new AlertDialog.Builder(this).setTitle("Sign in required").setMessage("Create an account or sign in to upload videos, go live, comment and follow creators. You can browse public videos without an account.").setNegativeButton("Later",null).setPositiveButton("Sign in",(d,w)->showAuth()).show();return false;}
  void showAuth(){
    base("Welcome to S");
    LinearLayout panel=new LinearLayout(this);
    panel.setOrientation(LinearLayout.VERTICAL);
    panel.setGravity(Gravity.CENTER);
    panel.setPadding(20,42,20,42);
    panel.setBackground(shape(Color.WHITE,28));
    TextView mark=text("S",52,accent);mark.setTypeface(null,Typeface.BOLD);mark.setGravity(Gravity.CENTER);panel.addView(mark);
    TextView intro=text("Sign in or create your account with Google",18,dark);intro.setGravity(Gravity.CENTER);intro.setTypeface(null,Typeface.BOLD);panel.addView(intro);
    TextView detail=text("Continue securely using your Google account. No email or password registration.",14,Color.GRAY);detail.setGravity(Gravity.CENTER);panel.addView(detail);
    Button google=button("Continue with Google");google.setTextSize(16);google.setMinHeight(56);
    LinearLayout.LayoutParams gp=new LinearLayout.LayoutParams(-1,-2);gp.topMargin=24;panel.addView(google,gp);
    google.setOnClickListener(v->startGoogleSignIn());
    status=text("",14,Color.RED);status.setGravity(Gravity.CENTER);status.setVisibility(View.GONE);panel.addView(status);
    content.addView(panel,new LinearLayout.LayoutParams(-1,-2));
  }
  void showHome(){base("DISCOVER");status=text("Discover videos from the S community",13,Color.rgb(100,104,118));status.setBackgroundColor(Color.WHITE);root.addView(status);bar=new LinearLayout(this);bar.setGravity(Gravity.CENTER);bar.setPadding(5,8,5,8);bar.setBackgroundColor(Color.WHITE);Button feed=navButton("⌂","Home"),upload=navButton("+","Create"),live=navButton("◉","Live"),profile=navButton("◎","Profile"),account=navButton("⋯",api.accessToken.isEmpty()?"Sign in":"Sign out");for(Button b:new Button[]{feed,upload,live,profile,account})bar.addView(b,new LinearLayout.LayoutParams(0,58,1));root.addView(bar);feed.setOnClickListener(v->loadFeed());upload.setOnClickListener(v->{if(requireAuth())pickVideo();});live.setOnClickListener(v->{if(requireAuth())startActivity(new Intent(this,LiveHubActivity.class));});profile.setOnClickListener(v->{if(requireAuth())showProfile();});account.setOnClickListener(v->{if(api.accessToken.isEmpty())showAuth();else{api.logout();showHome();}});loadFeed();}
  Button navButton(String icon,String label){Button b=new Button(this);b.setText(icon+"\n"+label);b.setTextSize(11);b.setTextColor(dark);b.setAllCaps(false);b.setPadding(2,2,2,2);b.setBackgroundColor(Color.TRANSPARENT);if(android.os.Build.VERSION.SDK_INT>=21)b.setStateListAnimator(null);return b;}
  void loadFeed(){status.setText("Loading feed…");io.execute(()->{try{
    JSONObject r=api.request("GET","/rest/v1/videos?select=id,owner_id,title,description,video_url,created_at&visibility=eq.public&order=created_at.desc&limit=50",null,null);
    videos=r.optJSONArray("data");if(videos==null)videos=new JSONArray();
    creatorNames.clear();likedVideoIds.clear();followedCreatorIds.clear();
    java.util.LinkedHashSet<String> owners=new java.util.LinkedHashSet<>(), videoIds=new java.util.LinkedHashSet<>();
    for(int i=0;i<videos.length();i++){
      JSONObject item=videos.optJSONObject(i);if(item==null)continue;
      String owner=item.optString("owner_id",""),vid=item.optString("id","");
      if(!owner.isEmpty())owners.add(owner);if(!vid.isEmpty())videoIds.add(vid);
    }
    if(!videoIds.isEmpty()&&!api.userId.isEmpty()){
      StringBuilder filter=new StringBuilder();for(String id:videoIds){if(filter.length()>0)filter.append(",");filter.append(id);}
      try{
        JSONObject likes=api.request("GET","/rest/v1/video_likes?select=video_id&user_id=eq."+api.userId+"&video_id=in.("+filter+")",null,null);
        JSONArray rows=likes.optJSONArray("data");if(rows!=null)for(int i=0;i<rows.length();i++){JSONObject row=rows.optJSONObject(i);if(row!=null)likedVideoIds.add(row.optString("video_id",""));}
      }catch(Exception ignored){}
    }
    if(!owners.isEmpty()&&!api.userId.isEmpty()){
      StringBuilder filter=new StringBuilder();for(String id:owners){if(filter.length()>0)filter.append(",");filter.append(id);}
      try{
        JSONObject follows=api.request("GET","/rest/v1/follows?select=followed_id&follower_id=eq."+api.userId+"&followed_id=in.("+filter+")",null,null);
        JSONArray rows=follows.optJSONArray("data");if(rows!=null)for(int i=0;i<rows.length();i++){JSONObject row=rows.optJSONObject(i);if(row!=null)followedCreatorIds.add(row.optString("followed_id",""));}
      }catch(Exception ignored){}
    }
    if(!owners.isEmpty()){
      StringBuilder filter=new StringBuilder();for(String id:owners){if(filter.length()>0)filter.append(",");filter.append(id);}
      try{
        JSONObject profiles=api.request("GET","/rest/v1/profiles?select=id,username,display_name&id=in.("+filter+")",null,null);
        JSONArray rows=profiles.optJSONArray("data");if(rows!=null)for(int i=0;i<rows.length();i++){
          JSONObject profile=rows.optJSONObject(i);if(profile==null)continue;
          String name=profile.optString("display_name","").trim();if(name.isEmpty())name=profile.optString("username","").trim();
          if(!name.isEmpty())creatorNames.put(profile.optString("id",""),name);
        }
      }catch(Exception ignored){}
    }
    runOnUiThread(()->renderFeed());
  }catch(Exception e){runOnUiThread(()->status.setText("Feed error: "+e.getMessage()));}});}
  void renderFeed(){content.removeAllViews();status.setText("PUBLIC FEED  ·  "+videos.length()+" VIDEOS");TextView heading=text("For you",26,dark);heading.setTypeface(null,Typeface.BOLD);content.addView(heading);content.addView(text("The latest from creators on S",14,Color.GRAY));if(videos.length()==0){LinearLayout empty=new LinearLayout(this);empty.setOrientation(1);empty.setGravity(Gravity.CENTER);empty.setPadding(22,38,22,38);empty.setBackground(shape(Color.WHITE,28));TextView symbol=text("S",42,accent);symbol.setGravity(Gravity.CENTER);symbol.setTypeface(null,Typeface.BOLD);empty.addView(symbol);TextView title=text("Your feed starts here",20,dark);title.setGravity(Gravity.CENTER);title.setTypeface(null,Typeface.BOLD);empty.addView(title);TextView detail=text("There are no public videos yet. Be the first creator to share something with the community.",14,Color.GRAY);detail.setGravity(Gravity.CENTER);empty.addView(detail);Button create=button("＋  Upload a video");empty.addView(create);create.setOnClickListener(v->{if(requireAuth())pickVideo();});content.addView(empty);return;}for(int i=0;i<videos.length();i++){JSONObject item=videos.optJSONObject(i);if(item==null)continue;LinearLayout card=new LinearLayout(this);card.setOrientation(1);card.setBackground(shape(Color.WHITE,22));card.setPadding(10,10,10,16);LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.bottomMargin=18;content.addView(card,cp);String title=item.optString("title","Video");card.addView(text(title,18,dark));String owner=item.optString("owner_id","");String creator=owner.equals(api.userId)?"You":creatorNames.get(owner);if(creator==null||creator.isEmpty())creator=owner.length()>8?owner.substring(0,8):owner;card.addView(text("By "+creator,13,Color.GRAY));if(!owner.isEmpty()&&!owner.equals(api.userId)){Button follow=button(followedCreatorIds.contains(owner)?"Following":"Follow "+creator);card.addView(follow);follow.setOnClickListener(v->toggleFollow(owner,follow));}String videoId=item.optString("id","");Button like=button(likedVideoIds.contains(videoId)?"♥ Liked":"♥ Like");card.addView(like);like.setOnClickListener(v->toggleLike(videoId,like));card.addView(text(item.optString("description",""),14,Color.DKGRAY));String url=item.optString("video_url","");if(!url.isEmpty()){VideoView player=new VideoView(this);player.setVideoURI(Uri.parse(url));player.setBackground(shape(Color.BLACK,18));card.addView(player,new LinearLayout.LayoutParams(-1,560));Button play=button("Play video");card.addView(play);play.setOnClickListener(v->player.start());}Button comments=button("Comments");card.addView(comments);comments.setOnClickListener(v->showComments(item.optString("id")));}}
  void pickVideo(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("video/*");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,PICK_VIDEO);}
  protected void onActivityResult(int req,int res,Intent data){super.onActivityResult(req,res,data);if(req==PICK_VIDEO&&res==RESULT_OK&&data!=null&&data.getData()!=null){Uri uri=data.getData();getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);uploadVideo(uri);}}
  String displayName(Uri uri){try(Cursor c=getContentResolver().query(uri,null,null,null,null)){if(c!=null&&c.moveToFirst()){int n=c.getColumnIndex(OpenableColumns.DISPLAY_NAME);if(n>=0)return c.getString(n);}}return "video.mp4";}
  long fileSize(Uri uri){try(Cursor c=getContentResolver().query(uri,new String[]{OpenableColumns.SIZE},null,null,null)){if(c!=null&&c.moveToFirst()){int i=c.getColumnIndex(OpenableColumns.SIZE);if(i>=0)return c.getLong(i);}}catch(Exception ignored){}return -1L;}
  void uploadVideo(Uri uri){
    long size=fileSize(uri);if(size>50L*1024L*1024L){new AlertDialog.Builder(this).setTitle("Video too large").setMessage("The current S media bucket accepts files up to 50 MiB. Choose a smaller video.").setPositiveButton("OK",null).show();return;}
    String mime=getContentResolver().getType(uri);
    if(mime!=null&&!mime.equals("video/mp4")&&!mime.equals("video/quicktime")&&!mime.equals("video/webm")&&!mime.equals("video/3gpp")&&!mime.equals("video/x-m4v")){new AlertDialog.Builder(this).setTitle("Unsupported video format").setMessage("Supported formats: MP4, QuickTime, WebM, 3GPP and M4V.").setPositiveButton("OK",null).show();return;}
    final EditText title=new EditText(this);title.setHint("Video title");new AlertDialog.Builder(this).setTitle("Name your video").setMessage(size>0?"File size: "+(size/(1024*1024))+" MiB · Maximum 50 MiB":"Maximum upload size: 50 MiB").setView(title).setNegativeButton("Cancel",null).setPositiveButton("Upload",(d,w)->{String name=title.getText().toString().trim();if(name.isEmpty())name=displayName(uri);final String t=name;status.setText("Uploading…");io.execute(()->{try{String path=api.userId+"/"+System.currentTimeMillis()+"_"+displayName(uri).replaceAll("[^A-Za-z0-9._-]","_");String publicUrl=api.upload(path,uri);JSONObject row=new JSONObject();row.put("owner_id",api.userId);row.put("title",t);row.put("description","");row.put("video_url",publicUrl);row.put("visibility","public");api.request("POST","/rest/v1/videos",row,"return=minimal");runOnUiThread(()->{status.setText("Upload complete");loadFeed();});}catch(Exception e){runOnUiThread(()->status.setText("Upload failed: "+e.getMessage()));}});}).show();}
  void showComments(String videoId){
    LinearLayout panel=new LinearLayout(this);panel.setOrientation(1);
    TextView messages=text("Loading comments…",14,dark);
    ScrollView scroll=new ScrollView(this);scroll.addView(messages);
    panel.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
    EditText input=new EditText(this);input.setHint("Write a comment");panel.addView(input);
    AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Comments").setView(panel)
      .setNegativeButton("Close",null).setPositiveButton("Send",null).create();
    Runnable load=()->io.execute(()->{
      try{
        JSONObject result=api.request("GET","/rest/v1/comments?select=user_id,body,created_at&video_id=eq."+videoId+"&order=created_at.asc&limit=200",null,null);
        JSONArray rows=result.optJSONArray("data");StringBuilder out=new StringBuilder();
        if(rows!=null)for(int i=0;i<rows.length();i++){
          JSONObject row=rows.optJSONObject(i);if(row==null)continue;
          String uid=row.optString("user_id","user");
          out.append(uid.substring(0,Math.min(8,uid.length()))).append(": ")
            .append(row.optString("body","")).append("\n\n");
        }
        runOnUiThread(()->messages.setText(out.length()==0?"No comments yet. Be the first to comment.":out.toString()));
      }catch(Exception ex){runOnUiThread(()->messages.setText("Could not load comments: "+ex.getMessage()));}
    });
    dialog.setOnShowListener(x->dialog.getButton(-1).setOnClickListener(v->{
      String body=input.getText().toString().trim();if(body.isEmpty())return;
      if(!requireAuth())return;
      input.setText("");
      io.execute(()->{
        try{
          JSONObject row=new JSONObject();row.put("video_id",videoId);row.put("user_id",api.userId);row.put("body",body);
          api.request("POST","/rest/v1/comments",row,"return=minimal");
          runOnUiThread(()->Toast.makeText(this,"Comment posted",Toast.LENGTH_SHORT).show());
          load.run();
        }catch(Exception ex){runOnUiThread(()->Toast.makeText(this,"Comment failed: "+ex.getMessage(),Toast.LENGTH_LONG).show());}
      });
    }));
    dialog.show();load.run();
  }
  void showProfile(){
    base("Profile");
    content.addView(text("Your account",18,dark));
    EditText display=new EditText(this);display.setHint("Display name");content.addView(display);
    EditText username=new EditText(this);username.setHint("Username");content.addView(username);
    EditText bio=new EditText(this);bio.setHint("Bio");content.addView(bio);
    TextView profileStatus=text("Loading your saved profile…",13,dark);content.addView(profileStatus);
    io.execute(()->{
      try{
        JSONObject result=api.request("GET","/rest/v1/profiles?select=username,display_name,bio&id=eq."+api.userId+"&limit=1",null,null);
        JSONArray rows=result.optJSONArray("data");
        JSONObject saved=rows!=null&&rows.length()>0?rows.optJSONObject(0):null;
        runOnUiThread(()->{
          if(saved!=null){
            display.setText(saved.optString("display_name",""));
            username.setText(saved.optString("username",""));
            bio.setText(saved.optString("bio",""));
            profileStatus.setText("Profile loaded");
          }else profileStatus.setText("No profile saved yet. Complete the fields below.");
        });
      }catch(Exception ex){runOnUiThread(()->profileStatus.setText("Could not load profile: "+ex.getMessage()));}
    });
    Button save=button("Save profile");content.addView(save);
    save.setOnClickListener(v->{
      String handle=username.getText().toString().trim();
      if(handle.isEmpty()){profileStatus.setText("Choose a username before saving.");return;}
      try{
        JSONObject row=new JSONObject();row.put("id",api.userId);
        row.put("display_name",display.getText().toString().trim());
        row.put("username",handle);row.put("bio",bio.getText().toString().trim());
        profileStatus.setText("Saving profile…");
        io.execute(()->{
          try{
            api.request("POST","/rest/v1/profiles",row,"resolution=merge-duplicates,return=minimal");
            runOnUiThread(()->profileStatus.setText("Profile saved successfully."));
          }catch(Exception ex){runOnUiThread(()->profileStatus.setText("Profile save failed: "+ex.getMessage()));}
        });
      }catch(Exception ex){profileStatus.setText("Invalid profile data.");}
    });
    content.addView(text("Your videos",18,dark));
    TextView uploadsStatus=text("Loading your uploads…",13,dark);content.addView(uploadsStatus);
    LinearLayout uploads=new LinearLayout(this);uploads.setOrientation(LinearLayout.VERTICAL);content.addView(uploads);
    Button reloadUploads=button("Refresh my videos");content.addView(reloadUploads);
    Runnable loadUploads=()->io.execute(()->{
      try{
        JSONObject result=api.request("GET","/rest/v1/videos?select=id,title,description,video_url,visibility,created_at&owner_id=eq."+api.userId+"&order=created_at.desc&limit=30",null,null);
        JSONArray rows=result.optJSONArray("data");if(rows==null)rows=new JSONArray();
        JSONArray finalRows=rows;
        runOnUiThread(()->{
          uploads.removeAllViews();uploadsStatus.setText(finalRows.length()+" uploaded video(s)");
          if(finalRows.length()==0)uploads.addView(text("You have not uploaded a video yet.",14,dark));
          for(int i=0;i<finalRows.length();i++){
            JSONObject item=finalRows.optJSONObject(i);if(item==null)continue;
            String title=item.optString("title","Video"),url=item.optString("video_url","");
            String visibility=item.optString("visibility","public");
            LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.VERTICAL);row.setPadding(8,8,8,12);row.setBackgroundColor(Color.WHITE);
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=8;uploads.addView(row,lp);
            row.addView(text(title,16,dark));row.addView(text("Visibility: "+visibility,12,Color.GRAY));
            if(!url.isEmpty()){Button play=button("Play");row.addView(play);play.setOnClickListener(v->{Intent intent=new Intent(this,VideoPlaybackActivity.class);intent.putExtra("url",url);intent.putExtra("title",title);startActivity(intent);});}
          }
        });
      }catch(Exception ex){runOnUiThread(()->uploadsStatus.setText("Could not load your videos: "+ex.getMessage()));}
    });
    reloadUploads.setOnClickListener(v->loadUploads.run());loadUploads.run();
    Button out=button("Sign out");content.addView(out);out.setOnClickListener(v->{api.logout();showAuth();});
  }
  void toggleFollow(String creatorId,Button b){if(!requireAuth())return;io.execute(()->{try{String q="/rest/v1/follows?follower_id=eq."+api.userId+"&followed_id=eq."+creatorId;JSONObject found=api.request("GET",q,null,null);JSONArray a=found.optJSONArray("data");if(a!=null&&a.length()>0){api.request("DELETE",q,null,null);followedCreatorIds.remove(creatorId);runOnUiThread(()->b.setText("Follow creator"));}else{JSONObject row=new JSONObject();row.put("follower_id",api.userId);row.put("followed_id",creatorId);api.request("POST","/rest/v1/follows",row,"return=minimal");followedCreatorIds.add(creatorId);runOnUiThread(()->b.setText("Following"));}}catch(Exception e){runOnUiThread(()->Toast.makeText(this,"Follow failed: "+e.getMessage(),Toast.LENGTH_LONG).show());}});}
  void toggleLike(String videoId,Button b){if(!requireAuth())return;io.execute(()->{try{String q="/rest/v1/video_likes?user_id=eq."+api.userId+"&video_id=eq."+videoId;JSONObject found=api.request("GET",q,null,null);JSONArray a=found.optJSONArray("data");if(a!=null&&a.length()>0){api.request("DELETE",q,null,null);likedVideoIds.remove(videoId);runOnUiThread(()->b.setText("♥ Like"));}else{JSONObject row=new JSONObject();row.put("user_id",api.userId);row.put("video_id",videoId);api.request("POST","/rest/v1/video_likes",row,"return=minimal");likedVideoIds.add(videoId);runOnUiThread(()->b.setText("♥ Liked"));}}catch(Exception e){runOnUiThread(()->Toast.makeText(this,"Like failed: "+e.getMessage(),Toast.LENGTH_LONG).show());}});}
  protected void onDestroy(){super.onDestroy();io.shutdown();}
}
