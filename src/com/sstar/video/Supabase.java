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
  String accessToken="", userId="", email="";
  Supabase(Context c){ context=c; load(); }
  void load(){ SharedPreferences p=context.getSharedPreferences("s_session",0); accessToken=p.getString("token",""); userId=p.getString("uid",""); email=p.getString("email",""); }
  void save(String token,String uid,String mail){ accessToken=token;userId=uid;email=mail;context.getSharedPreferences("s_session",0).edit().putString("token",token).putString("uid",uid).putString("email",mail).apply(); }
  void logout(){accessToken="";userId="";email="";context.getSharedPreferences("s_session",0).edit().clear().apply();}
  JSONObject request(String method,String path,JSONObject body,String prefer) throws Exception {
    HttpURLConnection c=(HttpURLConnection)new URL(BASE+path).openConnection(); c.setRequestMethod(method); c.setConnectTimeout(15000);c.setReadTimeout(20000);
    c.setRequestProperty("apikey",KEY); c.setRequestProperty("Authorization","Bearer "+(accessToken.isEmpty()?KEY:accessToken)); c.setRequestProperty("Accept","application/json");
    if(prefer!=null)c.setRequestProperty("Prefer",prefer);
    if(body!=null){c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json");try(OutputStream o=c.getOutputStream()){o.write(body.toString().getBytes(StandardCharsets.UTF_8));}}
    int code=c.getResponseCode(); InputStream in=code>=200&&code<300?c.getInputStream():c.getErrorStream(); String text=read(in); c.disconnect();
    if(code<200||code>=300)throw new IOException("HTTP "+code+": "+text);
    if(text==null||text.trim().isEmpty())return new JSONObject();
    if(text.trim().startsWith("[")){JSONObject wrap=new JSONObject();wrap.put("data",new JSONArray(text));return wrap;} return new JSONObject(text);
  }
  String upload(String objectPath, android.net.Uri uri) throws Exception {
    String mime=context.getContentResolver().getType(uri); if(mime==null)mime="video/mp4";
    HttpURLConnection c=(HttpURLConnection)new URL(BASE+"/storage/v1/object/s-media/"+encodePath(objectPath)).openConnection();
    c.setRequestMethod("POST");c.setDoOutput(true);c.setConnectTimeout(20000);c.setReadTimeout(120000);c.setRequestProperty("apikey",KEY);c.setRequestProperty("Authorization","Bearer "+accessToken);c.setRequestProperty("Content-Type",mime);c.setRequestProperty("x-upsert","false");
    try(InputStream in=context.getContentResolver().openInputStream(uri);OutputStream out=c.getOutputStream()){byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1)out.write(b,0,n);}
    int code=c.getResponseCode();String res=read(code>=200&&code<300?c.getInputStream():c.getErrorStream());c.disconnect();if(code<200||code>=300)throw new IOException("Upload HTTP "+code+": "+res);return BASE+"/storage/v1/object/public/s-media/"+objectPath;
  }
  static String encodePath(String path)throws Exception{String[] parts=path.split("/");StringBuilder out=new StringBuilder();for(int i=0;i<parts.length;i++){if(i>0)out.append("/");out.append(java.net.URLEncoder.encode(parts[i],"UTF-8").replace("+","%20"));}return out.toString();}
  static String read(InputStream in)throws Exception{if(in==null)return "";ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] x=new byte[8192];int n;while((n=in.read(x))!=-1)b.write(x,0,n);return b.toString("UTF-8");}
}
