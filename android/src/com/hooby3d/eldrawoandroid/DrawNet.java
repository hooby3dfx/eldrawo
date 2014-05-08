package com.hooby3d.eldrawoandroid;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;


import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;


public class DrawNet {
	
	public static final String STORE_URL = "http://play.google.com/store/apps/details?id=com.hooby3d.eldrawoandroid";

	public static void post(File pic, String token, String desc) {
		HttpPost filePost = new HttpPost("https://graph.facebook.com/me/photos");
		
		
		filePost.getParams().setBooleanParameter("http.protocol.expect-continue", false);
	    
	    HttpClient client = new DefaultHttpClient();
	    
	    try {
		      
		      MultipartEntity me = new MultipartEntity();
		      me.addPart("source", new FileBody(pic));
		      me.addPart("access_token", new StringBody(token));
		      me.addPart("message", new StringBody(desc+"\n"+STORE_URL));
		      filePost.setEntity(me);
		      
		      
		      HttpResponse resp = client.execute(filePost);
		      if(resp.getStatusLine()==null){
		    	  Log.d("DN", "no status");
		      }else{
		    	  Log.d("DN", resp.getStatusLine().toString());
		      }
		      

		      /*
		      //get photo id from response
		      InputStream is = resp.getEntity().getContent();		      
		      JSONObject jresp = getJson(is);
		      String id = jresp.getString("id");
		      
		      //get the thumb url and full url
		      HttpGet getPhoto = new HttpGet("https://graph.facebook.com/"+id+"?access_token="+token);
		      resp = client.execute(getPhoto);
		      is = resp.getEntity().getContent();	
		      jresp = getJson(is);
		      String link = jresp.getString("link");
		      
		      //then post to group
		      HttpPost groupPost = new HttpPost("https://graph.facebook.com/387551321277700/feed"+
		    		  "?link="+link+
		    		  "?access_token="+token);
				//thumb

		      resp = client.execute(groupPost);
		      jresp = getJson(is);
		      
		      */

		    } catch (IOException e) {	    	
		    	//e.printStackTrace();
		    } 
//		    catch(JSONException e){
//		    	e.printStackTrace();
//		    }
		    
		    finally {
		    	//TODO this ok?
		    	client.getConnectionManager().shutdown();
		    }
		
	}
	
	private static JSONObject getJson(InputStream is) throws JSONException, IOException{
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		String all = "";
		for(String line = reader.readLine(); line!=null; line = reader.readLine()){
			//System.out.println(line);
			all+=line;
		}
		return new JSONObject(all);
	}
	


}
