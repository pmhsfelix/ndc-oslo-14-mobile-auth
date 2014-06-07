package com.example.googleclient;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Scanner;

import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;

import com.example.googleclient.R;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.GooglePlayServicesAvailabilityException;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.AccountPicker;
import com.google.android.gms.common.GooglePlayServicesUtil;

public class MainActivity extends Activity {

	// the scopes
	private static final String TOKEN_SCOPE = 
			"oauth2:openid email profile";
	private static final String ID_TOKEN_SCOPE_FMT = 
			"audience:server:client_id:%s";
	private static final String CODE_SCOPE_FMT = 
			"oauth2:server:client_id:%s:api_scope:email https://www.googleapis.com/auth/tasks.readonly https://www.googleapis.com/auth/plus.login";
	
	// request ids
	private static final int CHOOSE_ACTIVITY = 2;
	private static final int CONTINUE_GET_TOKEN = 3;
	private static final int CONTINUE_GET_ID_TOKEN = 4;
	private static final int CONTINUE_GET_CODE = 5;
	
	private Button _loginBtn;
	private TextView _userInfoTextView;
	private Button _idTokenBtn;
	private String _userEmail;
	private Button _codeBtn;
	private String _serverSideClientId;
	private String _idTokenScope;
	private String _codeScope;

	private void d(String fmt, Object... args) {
		Log.d("GOOGLE_CLIENT", String.format(fmt, args));
	}

	private void show(String fmt, Object... args) {
		d(fmt, args);
		_userInfoTextView.setText(_userInfoTextView.getText() + "\n"
				+ String.format(fmt, args));
	}
	
	private void clear(){
		_userInfoTextView.setText("");
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);		
		_serverSideClientId = this.getString(R.string.server_side_client_id);
		_loginBtn = (Button) findViewById(R.id.loginBtn);
		if(TextUtils.isEmpty(_serverSideClientId) ){
			Toast.makeText(this, "Please configure client (see readme.md)", Toast.LENGTH_LONG).show();
			_loginBtn.setEnabled(false);
			return;
		}
		
		_idTokenScope = String.format(ID_TOKEN_SCOPE_FMT, _serverSideClientId);
		_codeScope = String.format(CODE_SCOPE_FMT, _serverSideClientId);		
		
		
		
		_userInfoTextView = (TextView) findViewById(R.id.textView1);
		_loginBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				clear();
				Intent chooseAccountIntent = AccountPicker
						.newChooseAccountIntent(
								null,
								null,
								new String[] { GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE },
								true, null, null, null, null);
				startActivityForResult(chooseAccountIntent, CHOOSE_ACTIVITY);
			}
		});

		_idTokenBtn = (Button) findViewById(R.id.idTokenBtn);
		_idTokenBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				clear();
				getTokenFor(_userEmail, _idTokenScope, CONTINUE_GET_ID_TOKEN);
			}
		});
		
		_codeBtn = (Button) findViewById(R.id.codeBtn);
		_codeBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				clear();
				getTokenFor(_userEmail, _codeScope, CONTINUE_GET_CODE);
			}
		});

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_OK) {
			if (requestCode == CHOOSE_ACTIVITY) {
				String email = data
						.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
				_userEmail = email;								
				show("Account = %s, getting token...", email);
				_idTokenBtn.setEnabled(true);
				_codeBtn.setEnabled(true);
				getTokenFor(email, TOKEN_SCOPE, CONTINUE_GET_TOKEN);
			} else if (requestCode == CONTINUE_GET_TOKEN) {
				String email = data
						.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
				show("still getting the token...");
				getTokenFor(email, TOKEN_SCOPE, CONTINUE_GET_TOKEN);
			} else if (requestCode == CONTINUE_GET_ID_TOKEN) {
				String email = data
						.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
				show("still getting the id_token...");
				getTokenFor(email, _idTokenScope, CONTINUE_GET_ID_TOKEN);
			} else if (requestCode == CONTINUE_GET_CODE) {
			String email = data
					.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
			show("still getting the code ...");
			getTokenFor(email, _codeScope, CONTINUE_GET_CODE);
		}
		}else{
			Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show();
		}
	}

	private void getTokenFor(String email, final String scope,
			final int continueRequestType) {
		new AsyncTask<String, Void, ErrorOrResult<String>>() {
			@Override
			protected ErrorOrResult<String> doInBackground(String... params) {
				String token = null;
				String email = params[0];
				d("getTokenFor email = %s, scope = %s, requestType", email, scope, continueRequestType);
				try {
					token = GoogleAuthUtil.getToken(MainActivity.this,
							email, scope, null);
					return new ErrorOrResult<String>(token);
				} catch (GooglePlayServicesAvailabilityException playEx) {
					Dialog dialog = GooglePlayServicesUtil.getErrorDialog(
							playEx.getConnectionStatusCode(),
							MainActivity.this, 0);
					dialog.show();
					return new ErrorOrResult<String>(playEx);
				} catch (UserRecoverableAuthException recoverableException) {
					Intent recoveryIntent = recoverableException.getIntent();
					MainActivity.this.startActivityForResult(recoveryIntent,
							continueRequestType);
					return new ErrorOrResult<String>(recoverableException);
				} catch (GoogleAuthException authEx) {
					d("Unrecoverable authentication exception: "
							+ authEx.getMessage(), authEx);					
					return new ErrorOrResult<String>(authEx);
				} catch (IOException ioEx) {
					d("transient error encountered: %s", ioEx.getMessage());
					return new ErrorOrResult<String>(ioEx);
				}
			}

			protected void onPostExecute(ErrorOrResult<String> res) {
				if(res.isError()){
					Exception ex = res.getException();
					if(!(ex instanceof UserRecoverableAuthException)){
						Toast.makeText(MainActivity.this, "An error ocurred while obtaining token", Toast.LENGTH_LONG).show();
					}
					return;
				}				
				String token= res.getValue();
				if (continueRequestType == CONTINUE_GET_TOKEN) {
					show("We have a token, let's get the user info...");
					getUserInfo(token);
				} else if (continueRequestType == CONTINUE_GET_ID_TOKEN) {
					show("Here is the id token:");
					show(token);
				} else if (continueRequestType == CONTINUE_GET_CODE) {
					show("Here is the code:");
					show(token);
				}					
			}
		}.execute(email);
	}

	private void getUserInfo(String accessToken) {
		new AsyncTask<String, Void, ErrorOrResult<String>>() {
			@Override
			protected ErrorOrResult<String> doInBackground(String... params) {
				URL url;
				BufferedInputStream in = null;
				Scanner s = null;
				try {
					url = new URL(
							"https://www.googleapis.com/plus/v1/people/me/openIdConnect");
					URLConnection urlConnection = url.openConnection();
					urlConnection.addRequestProperty("Authorization", "Bearer "
							+ params[0]);
					in = new BufferedInputStream(urlConnection.getInputStream());
					s = new Scanner(in);
					s.useDelimiter("\\A");
					String body = s.hasNext() ? s.next() : "<nothing>";
					return new ErrorOrResult<String>(body);
				} catch (MalformedURLException e) {
					d("MalformedURLException = %s", e.getMessage());
					return new ErrorOrResult<String>(e);
				} catch (IOException e) {
					d("IOException = %s", e.getMessage());
					return new ErrorOrResult<String>(e);
				} finally {
					if (s != null) {
						s.close();
					}
					try {
						if (in != null) {
							in.close();
						}
					} catch (IOException e) {
						d("exception = %s", e.getMessage());
						return new ErrorOrResult<String>(e);
					}
				}
			}

			@Override
			protected void onPostExecute(ErrorOrResult<String> result) {
				if(result.isError()){
					Exception e = result.getException();
					Toast.makeText(MainActivity.this, "Exception: "+e.getMessage(), Toast.LENGTH_LONG).show();
				}
				String info = result.getValue();
				show("Here is the user info:");
				show(info);				
			}
		}.execute(accessToken);
	}
	
	class ErrorOrResult<T>{
		private final Exception _e;
		private final T _value;
		ErrorOrResult(Exception e){
			_e = e;
			_value = null;
		}
		Exception getException(){
			return _e;
		}
		ErrorOrResult(T value){
			_e = null;
			_value = value;
		}
		T getValue(){
			return _value;
		}
		boolean isError(){
			return _e != null;
		}
	}

}
