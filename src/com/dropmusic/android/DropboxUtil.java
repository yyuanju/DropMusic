package com.dropmusic.android;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.util.Log;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.Session.AccessType;

public class DropboxUtil extends Activity {

	final static private String APP_KEY = "wh6ezdpjqs5f3h1";
	final static private String APP_SECRET = "17ewc7d7nx4ry2x";
	final static private AccessType ACCESS_TYPE = AccessType.APP_FOLDER;
	
	final static private String ACCOUNT_PREFS_NAME = "prefs";
    final static private String ACCESS_KEY_NAME = "ACCESS_KEY";
    final static private String ACCESS_SECRET_NAME = "ACCESS_SECRET";
	
 	private DropboxAPI<AndroidAuthSession> mDBApi;

 	@Override
 	protected void onCreate(Bundle savedInstanceState) {
 		super.onCreate(savedInstanceState);
 		setContentView(R.layout.activity_main);
 		
 		AppKeyPair appKeys = new AppKeyPair(APP_KEY, APP_SECRET);
 		AndroidAuthSession session = new AndroidAuthSession(appKeys, ACCESS_TYPE);
 		mDBApi = new DropboxAPI<AndroidAuthSession>(session);
 		
 		// MyActivity below should be your activity class name
 		mDBApi.getSession().startAuthentication(DropboxUtil.this);
 	}
    
    /**
     * Shows keeping the access keys returned from Trusted Authenticator in a local
     * store, rather than storing user name & password, and re-authenticating each
     * time (which is not to be done, ever).
     *
     * @return Array of [access_key, access_secret], or null if none stored
     */
	private String[] getKeys() {
		SharedPreferences prefs = getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
		String key = prefs.getString(ACCESS_KEY_NAME, null);
		String secret = prefs.getString(ACCESS_SECRET_NAME, null);
		if (key != null && secret != null) {
			String[] ret = new String[2];
			ret[0] = key;
			ret[1] = secret;
			return ret;
		} else {
			return null;
		}
	}
	
    /**
     * Shows keeping the access keys returned from Trusted Authenticator in a local
     * store, rather than storing user name & password, and re-authenticating each
     * time (which is not to be done, ever).
     */
    public void storeKeys(String key, String secret) {
        // Save the access key for later
        SharedPreferences prefs = getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
        Editor edit = prefs.edit();
        edit.putString(ACCESS_KEY_NAME, key);
        edit.putString(ACCESS_SECRET_NAME, secret);
        edit.commit();
    }

    private void clearKeys() {
        SharedPreferences prefs = getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
        Editor edit = prefs.edit();
        edit.clear();
        edit.commit();
    }
	
	private AndroidAuthSession buildSession(String APP_KEY, String APP_SECRET, AccessType ACCESS_TYPE) {
		AppKeyPair appKeyPair = new AppKeyPair(APP_KEY, APP_SECRET);
		AndroidAuthSession session;

		String[] stored = getKeys();
		if (stored != null) {
			AccessTokenPair accessToken = new AccessTokenPair(stored[0], stored[1]);
			session = new AndroidAuthSession(appKeyPair, ACCESS_TYPE, accessToken);
		} else {
			session = new AndroidAuthSession(appKeyPair, ACCESS_TYPE);
		}

		return session;
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
