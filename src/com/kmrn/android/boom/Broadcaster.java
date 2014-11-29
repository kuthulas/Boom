package com.kmrn.android.boom;

import android.app.IntentService;
import android.content.Intent;
import android.widget.Toast;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Broadcaster extends IntentService {
	// public static final String ACTION_SEND_FILE = "com.example.android.wifidirect.SEND_FILE";

	public Broadcaster(String name) {
		super(name);
	}

	public Broadcaster() {
		super("Broadcaster");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		DatagramSocket socket = null;

		try {
			InetAddress host = InetAddress.getByName("192.168.49.255");
			socket = new DatagramSocket();
			socket.setBroadcast(true);
			String data = "KMRN";
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
