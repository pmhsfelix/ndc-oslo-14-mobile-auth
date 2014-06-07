package com.example.webviewdemo;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Scanner;

import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

	protected static final int LOGIN_REQUEST = 2;
	private TextView _userInfoTextView;
	
	private void d(String fmt, Object...args){
		Log.d("WEB_VIEW_DEMO",String.format(fmt,args));
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		Button btn = (Button) findViewById(R.id.button1);
		btn.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(MainActivity.this, LoginActivity.class);
				startActivityForResult(intent, LOGIN_REQUEST);				
			}			
		});
		
		_userInfoTextView = (TextView) findViewById(R.id.textView1);		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data){
		if(requestCode == LOGIN_REQUEST){
			if(resultCode == RESULT_OK){
				getAndShowUserInfo(data.getStringExtra("ACCESS_TOKEN"));
			}else{
				Toast.makeText(this, "Unable to get token", Toast.LENGTH_LONG).show();
			}
		}
	}
	
	private void getAndShowUserInfo(String accessToken){
		new AsyncTask<String,Void,String>(){
			@Override
			protected String doInBackground(String... params) {
				URL url;
				BufferedInputStream in = null;
				Scanner s = null;
				d("Getting userinfo with token=%s",params[0]);
				try {
					url = new URL("https://api.github.com/user");	
					URLConnection urlConnection = url.openConnection();
					urlConnection.addRequestProperty("Authorization", "Bearer "+params[0]);
					urlConnection.addRequestProperty("User-Agent","android-app");
					in = new BufferedInputStream(urlConnection.getInputStream());			
					s = new Scanner(in);			
					s.useDelimiter("\\A");
					String body =  s.hasNext() ? s.next() : "<nothing>";
					return body;								
				} catch (MalformedURLException e) {
					d("exception = %s",e.getMessage());	
					return "<error>";
				} catch (IOException e) {
					d("exception = %s",e.getMessage());
					return "<error>";
				}finally {
					if(s != null){
						s.close();
					}
					try {
						if(in != null){
							in.close();
						}
					} catch (IOException e) {
						d("exception = %s",e.getMessage());
						return "<error>";
					}
				}
			}
			
			@Override
			protected void onPostExecute(String result) {
		         _userInfoTextView.setText(result);
		     }
		}.execute(accessToken);
	}

}
