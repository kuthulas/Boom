/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kmrn.android.boom;

import java.io.IOException;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ChannelListener;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.kmrn.android.boom.DeviceListFragment.DeviceActionListener;

/**
 * An activity that uses WiFi Direct APIs to discover and connect with available
 * devices. WiFi Direct APIs are asynchronous and rely on callback mechanism
 * using interfaces to notify the application of operation success or failure.
 * The application should also register a BroadcastReceiver for notification of
 * WiFi state related events.
 */
public class WiFiDirectActivity extends Activity implements ChannelListener, DeviceActionListener {

	public static final String TAG = "boom";
	private WifiP2pManager manager;
	private boolean isWifiP2pEnabled = false;
	private boolean retryChannel = false;

	private final IntentFilter intentFilter = new IntentFilter();
	private Channel channel;
	private BroadcastReceiver receiver = null;
	private MediaPlayer mp = new MediaPlayer();
	/**
	 * @param isWifiP2pEnabled the isWifiP2pEnabled to set
	 */
	public void setIsWifiP2pEnabled(boolean isWifiP2pEnabled) {
		this.isWifiP2pEnabled = isWifiP2pEnabled;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		// add necessary intent values to be matched.

		intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
		intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
		intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
		intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

		manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
		channel = manager.initialize(this, getMainLooper(), null);
	}

	/** register the BroadcastReceiver with the intent values to be matched */
	@Override
	public void onResume() {
		super.onResume();
		receiver = new WiFiDirectBroadcastReceiver(manager, channel, this);
		registerReceiver(receiver, intentFilter);
	}

	@Override
	public void onPause() {
		super.onPause();
		unregisterReceiver(receiver);
	}

	/**
	 * Remove all peers and clear all fields. This is called on
	 * BroadcastReceiver receiving a state change event.
	 */
	public void resetData() {
		DeviceListFragment fragmentList = (DeviceListFragment) getFragmentManager()
				.findFragmentById(R.id.frag_list);
		DeviceDetailFragment fragmentDetails = (DeviceDetailFragment) getFragmentManager()
				.findFragmentById(R.id.frag_detail);
		if (fragmentList != null) {
			fragmentList.clearPeers();
		}
		if (fragmentDetails != null) {
			fragmentDetails.resetViews();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.action_items, menu);
		return true;
	}

	/*
	 * (non-Javadoc)
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.pause:
			if(mp.isPlaying()){ mp.pause();}
			else{ mp.start(); }
	
		case R.id.show_music:
			playmanager();
			return true;
		case R.id.atn_direct_discover:
			if (!isWifiP2pEnabled) {
				Toast.makeText(WiFiDirectActivity.this, R.string.p2p_off_warning,
						Toast.LENGTH_SHORT).show();
				return true;
			}
			final DeviceListFragment fragment = (DeviceListFragment) getFragmentManager()
					.findFragmentById(R.id.frag_list);
			fragment.onInitiateDiscovery();
			manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {

				@Override
				public void onSuccess() {
					Toast.makeText(WiFiDirectActivity.this, "Discovery Initiated",
							Toast.LENGTH_SHORT).show();
				}

				@Override
				public void onFailure(int reasonCode) {
					Toast.makeText(WiFiDirectActivity.this, "Discovery Failed : " + reasonCode,
							Toast.LENGTH_SHORT).show();
				}
			});
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void showDetails(WifiP2pDevice device) {
		DeviceDetailFragment fragment = (DeviceDetailFragment) getFragmentManager()
				.findFragmentById(R.id.frag_detail);
		fragment.showDetails(device);

	}

	@Override
	public void connect(WifiP2pConfig config) {
		manager.connect(channel, config, new ActionListener() {

			@Override
			public void onSuccess() {}

			@Override
			public void onFailure(int reason) {
				Toast.makeText(WiFiDirectActivity.this, "Connect failed. Retry.",
						Toast.LENGTH_SHORT).show();
			}
		});
	}

	@Override
	public void disconnect() {
		final DeviceDetailFragment fragment = (DeviceDetailFragment) getFragmentManager()
				.findFragmentById(R.id.frag_detail);
		fragment.resetViews();
		manager.removeGroup(channel, new ActionListener() {

			@Override
			public void onFailure(int reasonCode) {
				Log.d(TAG, "Disconnect failed. Reason :" + reasonCode);

			}

			@Override
			public void onSuccess() {
				fragment.getView().setVisibility(View.GONE);
			}

		});
	}

	@Override
	public void onChannelDisconnected() {
		// we will try once more
		if (manager != null && !retryChannel) {
			Toast.makeText(this, "Channel lost. Trying again", Toast.LENGTH_LONG).show();
			resetData();
			retryChannel = true;
			manager.initialize(this, getMainLooper(), this);
		} else {
			Toast.makeText(this,
					"Severe! Channel is probably lost premanently. Try Disable/Re-Enable P2P.",
					Toast.LENGTH_LONG).show();
		}
	}

	@Override
	public void cancelDisconnect() {

		/*
		 * A cancel abort request by user. Disconnect i.e. removeGroup if
		 * already connected. Else, request WifiP2pManager to abort the ongoing
		 * request
		 */
		if (manager != null) {
			final DeviceListFragment fragment = (DeviceListFragment) getFragmentManager()
					.findFragmentById(R.id.frag_list);
			if (fragment.getDevice() == null
					|| fragment.getDevice().status == WifiP2pDevice.CONNECTED) {
				disconnect();
			} else if (fragment.getDevice().status == WifiP2pDevice.AVAILABLE
					|| fragment.getDevice().status == WifiP2pDevice.INVITED) {

				manager.cancelConnect(channel, new ActionListener() {

					@Override
					public void onSuccess() {
						Toast.makeText(WiFiDirectActivity.this, "Aborting connection",
								Toast.LENGTH_SHORT).show();
					}

					@Override
					public void onFailure(int reasonCode) {
						Toast.makeText(WiFiDirectActivity.this,
								"Connect abort request failed. Reason Code: " + reasonCode,
								Toast.LENGTH_SHORT).show();
					}
				});
			}
		}

	}

	private void playmanager(){
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
						if(mp!=null) {
							mp.release();
							mp = new MediaPlayer();
						}
						TextView textViewItem = ((TextView) view.findViewById(android.R.id.text1));
						String listItemText = textViewItem.getText().toString();
						String s = MediaStore.Audio.Media.TITLE + " = '"+ listItemText.replace("'", "\'\'") +"'";
						Cursor mc = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, s, null, null);
						mc.moveToNext();
						if(mp.isPlaying()){
							mp.stop();
							mp.reset();
						}
						try {
							mp.setDataSource(mc.getString(1));
							mp.prepare();
						} catch (IllegalArgumentException e) {
							e.printStackTrace();
						} catch (SecurityException e) {
							e.printStackTrace();
						} catch (IllegalStateException e) {
							e.printStackTrace();
						} catch (IOException e) {
							e.printStackTrace();
						}

						mp.start();
						Thread runn = new Thread(new seekupdate(mp));     
						runn.start();
						SeekBar seek_bar = (SeekBar) findViewById(R.id.seek_bar);
						seek_bar.setOnSeekBarChangeListener(new seekbarread());
						ImageButton pause_button = (ImageButton) findViewById(R.id.pause);
						pause_button.setOnClickListener(new OnClickListener() {
				            public void onClick(View v) {
				            	if(mp.isPlaying()) mp.pause();
				            	else mp.start();
				            }
				        });
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

	class seekupdate implements Runnable {
        MediaPlayer mp;
        seekupdate(MediaPlayer m) { mp = m; }
        public void run() {
        	while(mp.isPlaying()){
				SeekBar seek_bar = (SeekBar) findViewById(R.id.seek_bar);
				int currentPosition = 0;
				int total = mp.getDuration();
				seek_bar.setMax(total);
				while (mp != null && currentPosition < total) {
					try {
						Thread.sleep(100);
						currentPosition = mp.getCurrentPosition();
					} catch (InterruptedException e) {
						return;
					} catch (Exception e) {
						return;
					}
					seek_bar.setProgress(currentPosition);
				}
			}
        }
    }
	
	class seekbarread implements SeekBar.OnSeekBarChangeListener{
		@Override
	    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
	        if (fromUser) {
	            mp.seekTo(progress);
	        }
	    }

		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
		}

		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
		}
	}

}
