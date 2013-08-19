package com.dropbox.android.sample;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.audiofx.Equalizer;
import android.media.audiofx.Visualizer;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

@TargetApi(16)
public class Playmusic extends Activity implements OnClickListener, OnTouchListener, OnCompletionListener, OnBufferingUpdateListener {

	private ImageButton buttonPlayPause;
	private ImageButton buttonNext;
	private SeekBar seekBarProgress;
	public TextView editTextSongURL;
	private Button buttonPlayStop_b;
	
	private String []PlayList;
	private int next;
	private int mediaFileLengthInMilliseconds;
	
	private MediaPlayer mediaPlayer;
	
	//=====audiofx visualizerview
    private Visualizer mVisualizer;
    private VisualizerView mVisualizerView;
    private static final float VISUALIZER_HEIGHT_DIP = 50f;
    
    //=====audiofx equalizer relative setting
    private Equalizer mEqualizer;
    //private LinearLayout mLinearLayout = (LinearLayout) findViewById(R.id.equalizer_layout);
    
	private final Handler handler = new Handler();//usage?
	
	private Handler musicUI_handler = new Handler();
    private Handler musicThreadHandler;
    private HandlerThread musicThread;
    
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.layout_player);
		
		initView();	    
	}
	
	private void initView() {
		//create visualizerview 
		mVisualizerView = (VisualizerView)findViewById(R.id.visualizerView);	
		
		buttonPlayPause = (ImageButton)findViewById(R.id.play_pause_p);
		buttonPlayPause.setImageResource(R.drawable.pause);
		buttonPlayPause.setOnClickListener(this);
		
		buttonNext = (ImageButton)findViewById(R.id.next);
		buttonNext.setBackground(null);//we don't want the gray background
		buttonNext.setOnClickListener(this);
		
		buttonPlayStop_b = (Button)findViewById(R.id.stop_p);
		buttonPlayStop_b.setOnClickListener(this);
				
		seekBarProgress = (SeekBar)findViewById(R.id.seekBar1);
		seekBarProgress.setMax(99);
		seekBarProgress.setOnTouchListener(this);
		
		Intent GetFileList = this.getIntent();
		Bundle bundle = GetFileList.getExtras();
		
		next = 0;
		
		PlayList = bundle.getStringArray("PlayList");
		editTextSongURL = (TextView)findViewById(R.id.textView1);
		editTextSongURL.setText(PlayList[next]);
		
		//error log: Only the original thread that created a view hierarchy can touch its views
		/*musicThread = new HandlerThread("play_music_thread");
		musicThread.start();
        
        musicThreadHandler = new Handler(musicThread.getLooper());
        musicThreadHandler.post(taskMusic);*/
		initMediaPlayer();
		
		initVisualizer();
	}
	
	private Runnable taskMusic = new Runnable(){
	    	//implement runnable interface is better than expand Thread Class directly
	    	public void run()
	    	{
	    		initMediaPlayer();
	    	}
	};
	
	private void initMediaPlayer()
	{
		mediaPlayer = new MediaPlayer();
		mediaPlayer.setOnBufferingUpdateListener(this);
		mediaPlayer.setOnCompletionListener(this);	
		try{
			mediaPlayer.setDataSource(editTextSongURL.getText().toString());
			
			//mediaPlayer.prepare();
			
			//
			mediaPlayer.prepareAsync();
			/*OnPreparedListener preListener = new OnPreparedListener() {  
				 public void onPrepared(MediaPlayer arg0) { 
					 buttonPlayPause.setImageResource(R.drawable.pause);
						//mVisualizer.setEnabled(true);
						//setupEqualizerFxAndUI();
						mediaPlayer.start();					
				 }	
			};*/
			mediaPlayer.setOnPreparedListener(new OnPreparedListener(){
				@Override
				public void onPrepared(MediaPlayer arg0) {
					// TODO Auto-generated method stub
					mediaFileLengthInMilliseconds = mediaPlayer.getDuration();
				}	
			});
			//FIXME: Attempt to call getDuration without a valid mediaplayer....
			//moved to onPrepared
			//mediaFileLengthInMilliseconds = mediaPlayer.getDuration();
		}catch(Exception e){
			e.printStackTrace();//e.printStackTrace()?
		}	
		
		mediaPlayer.setOnCompletionListener(new OnCompletionListener(){
	         public void onCompletion(MediaPlayer arg0) {
	        	 mediaPlayer.reset();
	        	 //mVisualizer.setEnabled(false);
	        	 change_music();
	        	 Log.i("ch", "onCompletion. music");
	         }
	     });		
	}
	
	private void initVisualizer()
	{
		// Create the Visualizer object and attach it to our media player.
	    mVisualizer = new Visualizer(mediaPlayer.getAudioSessionId());
	    mVisualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[1]);
	    mVisualizer.setDataCaptureListener(new Visualizer.OnDataCaptureListener() {
	        public void onWaveFormDataCapture(Visualizer visualizer, byte[] bytes,
	                int samplingRate) {
	            mVisualizerView.updateVisualizer(bytes);
	        }

	        public void onFftDataCapture(Visualizer visualizer, byte[] bytes, int samplingRate) {}
	    }, Visualizer.getMaxCaptureRate() / 2, true, false);
	    
		Button back = (Button) findViewById(R.id.back);
		back.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
		 //回傳caller，然後結束自己回到啟動者
		 Intent intent = new Intent();
		 setResult(RESULT_OK, intent);
		 mediaPlayer.stop();
		 finish();
		 }
		});		
	}
	private void primarySeekBarProgressUpdater(){
		seekBarProgress.setProgress((int)
				(((float)mediaPlayer.getCurrentPosition()/mediaFileLengthInMilliseconds)*100));
		if(mediaPlayer.isPlaying()){
			Runnable notification = new Runnable(){
				public void run(){	
					primarySeekBarProgressUpdater();
				}
			};
			handler.postDelayed(notification, 1000);
		}
	}
	
	private void change_music() {
		// TODO Auto-generated method stub
			editTextSongURL.setText(PlayList[++next]);
			if((next+1) == PlayList.length){
				next = -1;
			}
			try{
				mediaPlayer.setDataSource(editTextSongURL.getText().toString());
				mediaPlayer.prepare();
				mVisualizer.setEnabled(true);
				mediaPlayer.start();
				//below code could directly play music, but will have a error message said that 
				//getDuration is not valid
				//and the listener may cause a infinite loop
				/*mediaPlayer.prepareAsync();
				OnPreparedListener preListener = new OnPreparedListener() {  
					 public void onPrepared(MediaPlayer arg0) {  
						 buttonPlayPause.setImageResource(R.drawable.pause);
							mVisualizer.setEnabled(true);
							//setupEqualizerFxAndUI();
							mediaPlayer.start();
					 }
				};*/
			}catch(Exception e){
				e.printStackTrace();//e.printStackTrace()?
			}
			mediaFileLengthInMilliseconds = mediaPlayer.getDuration();	
			seekBarProgress.setProgress(0);
			primarySeekBarProgressUpdater();
	}
	
	@Override
	public void onClick(View v){
		if(v.getId() == R.id.play_pause_p){
			if(!mediaPlayer.isPlaying()){
				buttonPlayPause.setImageResource(R.drawable.pause);
				//mVisualizer.setEnabled(true);
				//setupEqualizerFxAndUI();
				mediaPlayer.start();
				
			}else{
				buttonPlayPause.setImageResource(R.drawable.play);
				mediaPlayer.pause();				
			}
			primarySeekBarProgressUpdater();
			//Log.i("mp", "play/pause onClick be called");
		}
		if(v.getId() == R.id.stop_p){
			mediaPlayer.stop();
			seekBarProgress.setProgress(0);
			//Log.i("mp", "stop play" +" button clicked.");
			primarySeekBarProgressUpdater();
		}
		if(v.getId() == R.id.next){
			//case1: music is playing
			//case2: music is pause
			//case3: music is stop
			mediaPlayer.stop();
			seekBarProgress.setProgress(0);
			//Log.i("mp", "stop play" +" button clicked.");
			primarySeekBarProgressUpdater();
			mediaPlayer.reset();
			change_music();
			if(!mediaPlayer.isPlaying()){
				buttonPlayPause.setImageResource(R.drawable.pause);
				//mVisualizer.setEnabled(true);
				mediaPlayer.start();	
			}
		}
	}
	@Override
	public void onBufferingUpdate(MediaPlayer arg0, int percent) {
		// TODO Auto-generated method stub
		seekBarProgress.setSecondaryProgress(percent);
	}
	@Override
	public void onCompletion(MediaPlayer mp) {
		// TODO Auto-generated method stub
		buttonPlayPause.setImageResource(R.drawable.play);
	}
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		if(v.getId() == R.id.seekBar1){
			//if(mediaPlayer.isPlaying()){
				SeekBar sb= (SeekBar)v;
				int playPosititonInMilliseconds = (mediaFileLengthInMilliseconds/100)*
						sb.getProgress();
				mediaPlayer.seekTo(playPosititonInMilliseconds);
			//}
		}
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public void onBackPressed() {
		mediaPlayer.stop();
		seekBarProgress.setProgress(0);
		primarySeekBarProgressUpdater();
		super.onBackPressed();
	}
	
	/*private void setupEqualizerFxAndUI() {
	    // Create the Equalizer object (an AudioEffect subclass) and attach it to our media player,
	    // with a default priority (0).
	    mEqualizer = new Equalizer(0, mediaPlayer.getAudioSessionId());
	    mEqualizer.setEnabled(true);


	    short bands = mEqualizer.getNumberOfBands();

	    final short minEQLevel = mEqualizer.getBandLevelRange()[0];
	    final short maxEQLevel = mEqualizer.getBandLevelRange()[1];

	    for (short i = 0; i < bands; i++) {
	        final short band = i;

	        TextView freqTextView = new TextView(this);
	        freqTextView.setLayoutParams(new ViewGroup.LayoutParams(
	                ViewGroup.LayoutParams.FILL_PARENT,
	                ViewGroup.LayoutParams.WRAP_CONTENT));
	        freqTextView.setGravity(Gravity.CENTER_HORIZONTAL);
	        freqTextView.setText((mEqualizer.getCenterFreq(band) / 1000) + " Hz");
	        mLinearLayout.addView(freqTextView);

	        LinearLayout row = new LinearLayout(this);
	        row.setOrientation(LinearLayout.HORIZONTAL);

	        TextView minDbTextView = new TextView(this);
	        minDbTextView.setLayoutParams(new ViewGroup.LayoutParams(
	                ViewGroup.LayoutParams.WRAP_CONTENT,
	                ViewGroup.LayoutParams.WRAP_CONTENT));
	        minDbTextView.setText((minEQLevel / 100) + " dB");

	        TextView maxDbTextView = new TextView(this);
	        maxDbTextView.setLayoutParams(new ViewGroup.LayoutParams(
	                ViewGroup.LayoutParams.WRAP_CONTENT,
	                ViewGroup.LayoutParams.WRAP_CONTENT));
	        maxDbTextView.setText((maxEQLevel / 100) + " dB");

	        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
	                ViewGroup.LayoutParams.FILL_PARENT,
	                ViewGroup.LayoutParams.WRAP_CONTENT);
	        layoutParams.weight = 1;
	        SeekBar bar = new SeekBar(this);
	        bar.setLayoutParams(layoutParams);
	        bar.setMax(maxEQLevel - minEQLevel);
	        bar.setProgress(mEqualizer.getBandLevel(band));

	        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
	            public void onProgressChanged(SeekBar seekBar, int progress,
	                    boolean fromUser) {
	                mEqualizer.setBandLevel(band, (short) (progress + minEQLevel));
	            }

	            public void onStartTrackingTouch(SeekBar seekBar) {}
	            public void onStopTrackingTouch(SeekBar seekBar) {}
	        });

	        row.addView(minDbTextView);
	        row.addView(bar);
	        row.addView(maxDbTextView);

	        mLinearLayout.addView(row);
	    }
	}*/
}
