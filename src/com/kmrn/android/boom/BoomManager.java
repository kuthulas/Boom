package com.kmrn.android.boom;

import java.io.IOException;

import android.app.Activity;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class BoomManager extends Activity implements OnClickListener {
	SeekBar seek_bar;
	Button play_button, pause_button;
	Handler seekHandler = new Handler();
	MediaPlayer mp = new MediaPlayer();

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		getInit();
		seekUpdation();
	}

	public void getInit() {
		seek_bar = (SeekBar) findViewById(R.id.seek_bar);
		play_button = (Button) findViewById(R.id.next);
		pause_button = (Button) findViewById(R.id.pause);
		play_button.setOnClickListener(this);
		pause_button.setOnClickListener(this);
    	
    	
  	     Cursor mCursor = null;
  	     try {
  	    	String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";
  	         mCursor = getContentResolver().query(
  	             MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, selection, null, "_id");

  	         Toast.makeText(this, Integer.toString(mCursor.getCount())+ " songs found!",Toast.LENGTH_SHORT).show();
  	         String[] columns = {MediaStore.Audio.Media.TITLE};
  	         int[] to = new int[] {android.R.id.text1};
  	         
  	         SimpleCursorAdapter cursorAdapter = new SimpleCursorAdapter(this, R.layout.mylist, mCursor, columns, to, 0);

  	         if (mCursor.getCount() != 0) {
  	        	 ListView listView = (ListView) findViewById(R.id.mylist);
  	        	 listView.setAdapter(cursorAdapter);
  	        	listView.setOnItemClickListener(new OnItemClickListener() {
  	        	    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
  	        	    	TextView textViewItem = ((TextView) view.findViewById(android.R.id.text1));
  	        	    	String listItemText = textViewItem.getText().toString();
  	        	    	String s = MediaStore.Audio.Media.TITLE + " = '"+ listItemText +"'";
  	        	    	Cursor mc = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, s, null, null);
  	        	    	mc.moveToNext();
  	        	    	if(mp.isPlaying()){
  	        	         mp.stop();
  	        	         mp.reset();
  	        	    	}
  	        	    	try {
							mp.setDataSource(mc.getString(1));
						} catch (IllegalArgumentException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (SecurityException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (IllegalStateException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
 	        	         try {
							mp.prepare();
							seek_bar.setMax(mp.getDuration());
						} catch (IllegalStateException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
 	        	         mp.start();
  	        	     mp.setOnCompletionListener(new OnCompletionListener() {

  	        	         public void onCompletion(MediaPlayer mPlayer) {
  	        	             mp.release();
  	        	         }
  	        	     

  	        	     });
  	        	    }
  	        	});
  	         }
  	     } catch (Exception e) {e.printStackTrace();}
  	     

	}

	Runnable run = new Runnable() {

		@Override
		public void run() {
			seekUpdation();
		}
	};

	public void seekUpdation() {

		seek_bar.setProgress(mp.getCurrentPosition());
		seekHandler.postDelayed(run, 1000);
	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.next:
			mp.start();
			break;
		case R.id.pause:
			mp.pause();
		}

	}
}
