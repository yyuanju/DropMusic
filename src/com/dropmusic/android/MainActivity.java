package com.dropmusic.android;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.Session.AccessType;

import android.os.Bundle;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;
import android.view.Menu;

public class MainActivity extends Activity {
	
	final static private String APP_KEY = "wh6ezdpjqs5f3h1";
	final static private String APP_SECRET = "17ewc7d7nx4ry2x";
	final static private AccessType ACCESS_TYPE = AccessType.APP_FOLDER;
	
	final static private String ACCOUNT_PREFS_NAME = "prefs";
    final static private String ACCESS_KEY_NAME = "ACCESS_KEY";
    final static private String ACCESS_SECRET_NAME = "ACCESS_SECRET";
	
	// In the class declaration section:
	private DropboxAPI<AndroidAuthSession> mDBApi;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		AppKeyPair appKeys = new AppKeyPair(APP_KEY, APP_SECRET);
		AndroidAuthSession session = new AndroidAuthSession(appKeys, ACCESS_TYPE);
		mDBApi = new DropboxAPI<AndroidAuthSession>(session);
		
		// MyActivity below should be your activity class name
		mDBApi.getSession().startAuthentication(MainActivity.this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	protected void onResume() {
	    super.onResume();
	    AndroidAuthSession session = mDBApi.getSession();

	    if (session.authenticationSuccessful()) {
	        try {
	            // Required to complete auth, sets the access token on the session
	        	session.finishAuthentication();

	            AccessTokenPair tokens = session.getAccessTokenPair();
	            storeKeys(tokens.key, tokens.secret);
                setLoggedIn(true);
	            
	        } catch (IllegalStateException e) {
	            Log.i("DbAuthLog", "Error authenticating", e);
	        }
	    }
	}
}
