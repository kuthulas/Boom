package com.kmrn.android.boom;

import android.app.IntentService;
import android.content.Intent;
import android.widget.Toast;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Broadcaster extends IntentService {

	public static final String ACTION_SYNC_PLAY = "com.kmrn.android.boom.SYNC_PLAY";
	public static final String EXTRAS_CURRENT_TIME = "time";
	public static final String EXTRAS_PLAY_POSITION = "position";
	public static final String EXTRAS_PLAY_TITLE = "file";
	public static final String EXTRAS_PLAY_PAUSE = "playpause";

	public Broadcaster(String name) {
		super(name);
	}

	public Broadcaster() {
		super("Broadcaster");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		DatagramSocket socket = null;
		String data = null;

		long time = intent.getExtras().getLong(EXTRAS_CURRENT_TIME);
		int position = intent.getExtras().getInt(EXTRAS_PLAY_POSITION);
		String file = intent.getExtras().getString(EXTRAS_PLAY_TITLE);
		int playpause = intent.getExtras().getInt(EXTRAS_PLAY_PAUSE);

		try {
			InetAddress host = InetAddress.getByName("192.168.49.255");
			socket = new DatagramSocket();
			socket.setBroadcast(true);
			data = String.valueOf(System.currentTimeMillis()) + ":" + time + ":" + position + ":" + file + ":" + playpause + ":";
			DatagramPacket packet = new DatagramPacket(data .getBytes(), data.length(),
					host, 8988);
			socket.send(packet);
		} catch (IOException e) {
			Toast.makeText(getApplicationContext(), e.getMessage() ,Toast.LENGTH_SHORT).show();
		} finally {
			if (socket != null) {
				socket.close();
			}
		}
	}
}
