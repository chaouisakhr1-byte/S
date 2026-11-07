package com.sstar.video;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

final class Supabase {
  static final String BASE = "https://loysyodgqvfiskijzwqd.supabase.co";
  // Publishable client key only. Never place a service-role key in an Android app.
  static final String KEY = "sb_publishable_nA2mHOoOZoqLvAgr7s1BEw_vQYZmKTw";
  final Context context;
  String accessToken="", refreshToken="", userId="", email="";
  Supabase(Context c){ context=c; load(); }
  void load(){ SharedPreferences p=context.getSharedPreferences("s_session",0); accessToken=p.getString("token",""); refreshToken=p.getString("refresh",""); userId=p.getString("uid",""); email=p.getString("email",""); }
  void save(String token,String uid,String mail,String refresh){ accessToken=token;refreshToken=refresh==null?"":refresh;userId=uid;email=mail;context.getSharedPreferences("s_session",0).edit().putString("token",token).putString("refresh",refreshToken).putString("uid",uid).putString("email",mail).apply(); }
  void logout(){accessToken="";refreshToken="";userId="";email="";context.getSharedPreferences("s_session",0).edit().clear().apply();}
  JSONObject request(String method,String path,JSONObject body,String prefer) throws Exception {
    return requestInternal(method,path,body,prefer,true);
  }

  private JSONObject requestInternal(String method,String path,JSONObject body,String prefer,boolean allowRefresh) throws Exception {
    HttpURLConnection c=(HttpURLConnection)new URL(BASE+path).openConnection();
    c.setRequestMethod(method);c.setConnectTimeout(15000);c.setReadTimeout(20000);
    c.setRequestProperty("apikey",KEY);
    c.setRequestProperty("Authorization","Bearer "+(accessToken.isEmpty()?KEY:accessToken));
    c.setRequestProperty("Accept","application/json");
    if(prefer!=null)c.setRequestProperty("Prefer",prefer);
    if(body!=null){c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json");try(OutputStream o=c.getOutputStream()){o.write(body.toString().getBytes(StandardCharsets.UTF_8));}}
    int code=c.getResponseCode();
    InputStream in=code>=200&&code<300?c.getInputStream():c.getErrorStream();
    String response=read(in);c.disconnect();
    if(code==401&&allowRefresh&&!refreshToken.isEmpty()&&!path.startsWith("/auth/v1/token?grant_type=refresh_token")){
      if(refreshSession())return requestInternal(method,path,body,prefer,false);
    }
    if(code<200||code>=300)throw new IOException("HTTP "+code+": "+response);
    if(response==null||response.trim().isEmpty())return new JSONObject();
    if(response.trim().startsWith("[")){JSONObject wrap=new JSONObject();wrap.put("data",new JSONArray(response));return wrap;}
    return new JSONObject(response);
  }

  private synchronized boolean refreshSession() {
    HttpURLConnection c=null;
    try{
      JSONObject payload=new JSONObject();payload.put("refresh_token",refreshToken);
      c=(HttpURLConnection)new URL(BASE+"/auth/v1/token?grant_type=refresh_token").openConnection();
      c.setRequestMethod("POST");c.setDoOutput(true);c.setConnectTimeout(15000);c.setReadTimeout(20000);
      c.setRequestProperty("apikey",KEY);c.setRequestProperty("Authorization","Bearer "+KEY);
      c.setRequestProperty("Content-Type","application/json");c.setRequestProperty("Accept","application/json");
      try(OutputStream out=c.getOutputStream()){out.write(payload.toString().getBytes(StandardCharsets.UTF_8));}
      int code=c.getResponseCode();String response=read(code>=200&&code<300?c.getInputStream():c.getErrorStream());
      if(code<200||code>=300)return false;
      JSONObject data=new JSONObject(response);String token=data.optString("access_token","");
      String nextRefresh=data.optString("refresh_token",refreshToken);
      JSONObject user=data.optJSONObject("user");
      String uid=user==null?userId:user.optString("id",userId);
      String mail=user==null?email:user.optString("email",email);
      if(token.isEmpty()||uid.isEmpty())return false;
      save(token,uid,mail,nextRefresh);return true;
    }catch(Exception ignored){return false;}
    finally{if(c!=null)c.disconnect();}
  }

  String upload(String objectPath, android.net.Uri uri) throws Exception {
    return uploadInternal(objectPath,uri,true);
  }

  private String uploadInternal(String objectPath,android.net.Uri uri,boolean allowRefresh) throws Exception {
    String mime=context.getContentResolver().getType(uri);if(mime==null)mime="video/mp4";
    HttpURLConnection c=(HttpURLConnection)new URL(BASE+"/storage/v1/object/s-media/"+encodePath(objectPath)).openConnection();
    c.setRequestMethod("POST");c.setDoOutput(true);c.setConnectTimeout(20000);c.setReadTimeout(120000);
    c.setRequestProperty("apikey",KEY);c.setRequestProperty("Authorization","Bearer "+accessToken);
    c.setRequestProperty("Content-Type",mime);c.setRequestProperty("x-upsert","false");
    try(InputStream in=context.getContentResolver().openInputStream(uri);OutputStream out=c.getOutputStream()){
      byte[] buffer=new byte[65536];int n;while((n=in.read(buffer))!=-1)out.write(buffer,0,n);
    }
    int code=c.getResponseCode();String response=read(code>=200&&code<300?c.getInputStream():c.getErrorStream());c.disconnect();
    if(code==401&&allowRefresh&&!refreshToken.isEmpty()&&refreshSession())return uploadInternal(objectPath,uri,false);
    if(code<200||code>=300)throw new IOException("Upload HTTP "+code+": "+response);
    return BASE+"/storage/v1/object/public/s-media/"+encodePath(objectPath);
  }

  static String encodePath(String path)throws Exception{String[] parts=path.split("/");StringBuilder out=new StringBuilder();for(int i=0;i<parts.length;i++){if(i>0)out.append("/");out.append(java.net.URLEncoder.encode(parts[i],"UTF-8").replace("+","%20"));}return out.toString();}
  static String read(InputStream in)throws Exception{if(in==null)return "";ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] x=new byte[8192];int n;while((n=in.read(x))!=-1)b.write(x,0,n);return b.toString("UTF-8");}
}
