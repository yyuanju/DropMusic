package com.dropmusic.android;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.exception.DropboxIOException;
import com.dropbox.client2.exception.DropboxServerException;
import com.dropbox.client2.exception.DropboxUnlinkedException;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.Session.AccessType;

public class MainActivity extends Activity {
	private static final String TAG = "MainActivity";
	final static private String APP_KEY = "wh6ezdpjqs5f3h1";
	final static private String APP_SECRET = "17ewc7d7nx4ry2x";
	final static private AccessType ACCESS_TYPE = AccessType.APP_FOLDER;
	
	final static private String ACCOUNT_PREFS_NAME = "prefs";
    final static private String ACCESS_KEY_NAME = "ACCESS_KEY";
    final static private String ACCESS_SECRET_NAME = "ACCESS_SECRET";
	
	private DropboxAPI<AndroidAuthSession> mDBApi;
	private boolean mLoggedIn;
	private Button mSubmit;
	private LinearLayout mDisplay;
	private ImageView mImage;
	public ProgressDialog myDialog = null;
	private Handler monitorUI_handler = new Handler();
	private Handler monitorThreadHandler;
    private HandlerThread monitorThread;
    
    private String FolderName = null;
    private int folder_depth = 0;
    private String[] folder_return;
    private String[] FileURL;
    List<Map<String, Object>> menu_list= new ArrayList<Map<String, Object>>();
    private ListView folder_list;
    private SimpleAdapter my_adapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		AndroidAuthSession session = buildSession();
		mDBApi = new DropboxAPI<AndroidAuthSession>(session);
		
		//try {
			//Log.i("userinfo_log", "displayName: " + mDBApi.accountInfo().displayName);
			//Log.i("userinfo_log", "uid: " + mDBApi.accountInfo().uid);
		//} catch (DropboxException e) {
		//	Log.e("userinfo_log", "Something went wrong while getting userinfo.");
		//}
		mSubmit = (Button) findViewById(R.id.auth_button);

		mSubmit.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				// This logs you out if you're logged in, or vice versa
				if (mLoggedIn) {
					logOut();
				} else {
					// Start the remote authentication
					mDBApi.getSession().startAuthentication(MainActivity.this);
				}
			}
		});
		
		mDisplay = (LinearLayout) findViewById(R.id.logged_in_display);
		mImage = (ImageView) findViewById(R.id.image_view);
		myDialog = ProgressDialog.show(MainActivity.this, "讀取中", "loading", true);
		
		monitorThread = new HandlerThread("listview_display");
		monitorThread.start();

		monitorThreadHandler = new Handler(monitorThread.getLooper());
		monitorThreadHandler.post(task1);
		
		setLoggedIn(mDBApi.getSession().isLinked());

	}

	// get folder data list we need to store the data to List now
	private Runnable task1 = new Runnable() {
		public void run() {
			Log.i("thread", "task1");
			String path;
			try {
				if (FolderName == null)
					path = "/";
				else
					path = FolderName;

				Entry contact = mDBApi.metadata(path, 0, null, true, null);
				List<Entry> CFolder = contact.contents;
				int i = 0;
				//folder_return = new String[CFolder.size()];
				FileURL = new String[CFolder.size()];
				menu_list.clear();
				for (Entry entry : CFolder) { /* 		
        		    //old version: only file_name
        			if(entry.isDir){
        				folder_return[i++]="F_" + entry.fileName();
        			}
        			else{// is file
        				folder_return[i]=entry.fileName().toString();
            			FileURL[i++]= mApi.media(entry.path, false).url.toString();
            			//Log.i("DbExampleLog", "e_path: " + entry.path);
            			//Log.i("DbExampleLog", "e_media: " + mApi.media(entry.path, false).url);
        			} */
					// new version: image & file_name
					Map<String, Object> map = new HashMap<String, Object>();
					if (entry.isDir) {
						map.put("info", entry.fileName());
						map.put("title", "Folder");
						map.put("img", R.drawable.icon);
						menu_list.add(map);
					} else {
						map.put("info", entry.fileName().toString());
						if (entry.icon.equals("page_white_sound")) {
							map.put("title", "MUSIC");
							// add file URL to playlist
							FileURL[i++] = mDBApi.media(entry.path, false).url.toString();
						} else if (entry.icon.equals("page_white_picture")) {
							map.put("title", "PICTURE");
						} else
							map.put("title", "FILE");
						// map.put("title", entry.icon);
						map.put("img", R.drawable.file);
						menu_list.add(map);
						// FileURL[i++]= mApi.media(entry.path,
						// false).url.toString();//add file URL to playlist
					}
				}
				i = 0;
			}
			catch (DropboxUnlinkedException e) {
				Log.e("DbExampleLog", "you have not set an access token pair "
						+ "on the session, or if the user has revoked access.");
			} catch (DropboxServerException e) {
				Log.e("DbExampleLog", "server responds with an error code.");
				Log.e("DbExampleLog", e.reason);
			} catch (DropboxIOException e) {
				Log.e("DbExampleLog", "network-related error occurs.");
			} catch (DropboxException e) {
				Log.e("DbExampleLog", "Something went wrong while getting metadata.");
			} finally {
				myDialog.dismiss();
			}
			
			setContentView(R.layout.activity_main);
			
			monitorUI_handler.post(task2);
		}
	};
	
	private Runnable task2 = new Runnable() {
		public void run() {
			Log.i("thread", "run2");

			folder_list = (ListView) findViewById(R.id.folder_contents);

			// FIX ME: lost .img .title hahahahaha....
			my_adapter = new SimpleAdapter(MainActivity.this, menu_list,
					R.layout.listview_contents, new String[] { "title", "info",
							"img"/* , "checkBox1" */}, new int[] { R.id.title,
							R.id.info, R.id.img /* , R.id.checkBox1 */});
			folder_list.setAdapter(my_adapter);
			folder_list.setOnItemClickListener(
				new AdapterView.OnItemClickListener() {
					@Override
					public void onItemClick(AdapterView<?> arg0, View arg1, 
							int position, long arg3) {

						// new version: get clicked item data path
						if (menu_list.get(position).get("title") == "Folder") {
							myDialog = ProgressDialog.show(MainActivity.this,
									"讀取中", "loading", true);
							if (folder_depth != 0) {
								FolderName = FolderName + "/"
										+ menu_list.get(position).get("info");
								folder_depth++;
							} else {
								FolderName = "/"
										+ menu_list.get(position).get("info");
								folder_depth++;
							}
							monitorThreadHandler.post(task1);
						}
					}
				}
			);
    	}
    };
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
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
	
	private AndroidAuthSession buildSession() {
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
	
	private void logOut() {
		mDBApi.getSession().unlink();
		clearKeys();
		setLoggedIn(false);
	}
	
	/**
	 * Convenience function to change UI state based on being logged in
	 */
	private void setLoggedIn(boolean loggedIn) {
		mLoggedIn = loggedIn;
		if (loggedIn) {
			mSubmit.setText("Unlink from Dropbox");
			mDisplay.setVisibility(View.VISIBLE);
		} else {
			mSubmit.setText("Link with Dropbox");
			mDisplay.setVisibility(View.GONE);
			//mImage.setImageDrawable(null);
		}
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
