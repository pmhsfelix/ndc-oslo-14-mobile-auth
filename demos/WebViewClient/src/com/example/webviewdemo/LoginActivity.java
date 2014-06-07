package com.example.webviewdemo;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

public class LoginActivity extends Activity {
	
	private String _clientId;
	private String _clientSecret;
	private String _redirectUri;

	private void d(String fmt, Object...args){
		Log.d("WEB_VIEW_DEMO",String.format(fmt,args));
	}

	@SuppressLint("SetJavaScriptEnabled")
	// beware of the XSS!
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		_clientId = this.getString(R.string.client_id);
		_clientSecret = this.getString(R.string.client_secret);
		_redirectUri = this.getString(R.string.redirect_uri);
		if(TextUtils.isEmpty(_clientId) || TextUtils.isEmpty(_clientSecret) || TextUtils.isEmpty(_redirectUri)){
			Toast.makeText(this, "Please configure client (see readme.md)", Toast.LENGTH_LONG).show();
			finish();
			return;
		}		
		
		d("------- starting login -------");
		
		FrameLayout.LayoutParams frameParams = new FrameLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.MATCH_PARENT);

		LinearLayout mainLayout = new LinearLayout(this);
		mainLayout.setPadding(0, 0, 0, 0);

		WebView wv = new WebView(this);
		// wv.setVisibility(View.INVISIBLE);
		wv.setVerticalScrollBarEnabled(false);
		wv.setHorizontalScrollBarEnabled(false);
		wv.getSettings().setJavaScriptEnabled(true);
		wv.setLayoutParams(frameParams);
		wv.setWebViewClient(new CustomWebViewClient(_redirectUri));
		
		String authRequest = 
				String.format("https://github.com/login/oauth/authorize?client_id=%s&scope=user&redirect_uri=%s&state=some.state", 
				_clientId,
				Uri.encode(_redirectUri));
		
		// Removing cookies
		CookieManager cookieManager = CookieManager.getInstance();
		cookieManager.removeAllCookie();
		cookieManager.setAcceptCookie(true);		
		
		d("Loading '%s'",authRequest);
		wv.loadUrl(authRequest);
		

		mainLayout.addView(wv);

		setContentView(mainLayout, new LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.MATCH_PARENT));

	}
	
	private class ExchangeCodeForToken extends AsyncTask<String,Void,String>{

		@Override
		protected String doInBackground(String... params) {
			try {
				URL tokenEndpoint = new URL("https://github.com/login/oauth/access_token");
				d("Exchanging code for token at '%s'", tokenEndpoint);
				HttpURLConnection conn = (HttpURLConnection) tokenEndpoint.openConnection();
				conn.addRequestProperty("Accept", "application/json");
		        conn.setRequestMethod("POST");
		        conn.setRequestProperty("Content-Type", 
		               "application/x-www-form-urlencoded");
		        String body = String.format("client_id=%s&client_secret=%s&code=%s&redirect_uri=&s",
		        		_clientId,
		        		_clientSecret,
		        		params[0],
		        		Uri.encode(_redirectUri));
		        OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());
		        writer.write(body);
		        writer.close();
		        Scanner s = new Scanner(conn.getInputStream());
				try{
					s.useDelimiter("\\A");
					return s.hasNext() ? deserialize(s.next()) : null;
				}finally{
					s.close();
				}
			} catch (MalformedURLException e) {			
				d("Exception %s",e.getMessage());
				return null;
			} catch (IOException e) {
				d("Exception %s",e.getMessage());
				return null;
			}
		}
		
		@Override
		protected void onPostExecute(String token){
			if(token != null){
				Intent intent = new Intent();
				intent.putExtra("ACCESS_TOKEN", token);
				LoginActivity.this.setResult(RESULT_OK,intent);
			}else{
				LoginActivity.this.setResult(RESULT_CANCELED);				
			}
			LoginActivity.this.finish();			
		}
		
		private String deserialize(String value){
			try {
				d("deserializing %s", value);
				JSONObject jo = new JSONObject(value);
				String token = jo.getString("access_token");
				d("Access token is %s",token);
				return token;				
			} catch (JSONException e) {
				return null;
			}
		}		
	}

	private class CustomWebViewClient extends WebViewClient {

		private Uri _redirectUri; 
		public CustomWebViewClient(String redirectUri) {
			_redirectUri = Uri.parse(redirectUri);
		}

		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {

			d("Should load '%s'?",url);
			Uri uri = Uri.parse(url);
			if(uri.getAuthority().equals(_redirectUri.getAuthority()) &&
				uri.getPath().equals(_redirectUri.getPath()))
			{
				d("No, because it is the redirect uri",url);
				String code = uri.getQueryParameter("code");				
				new ExchangeCodeForToken().execute(code);
				return true;
			}
			d("Yes, it isn't the redirect URI");
			return false;
		}		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
}
