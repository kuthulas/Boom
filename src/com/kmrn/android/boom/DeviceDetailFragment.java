package com.kmrn.android.boom;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.kmrn.android.boom.Broadcaster;
import com.kmrn.android.boom.DeviceListFragment.DeviceActionListener;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

/**
 * A fragment that manages a particular peer and allows interaction with device
 * i.e. setting up network connection and transferring data.
 */
@SuppressLint("InflateParams") public class DeviceDetailFragment extends Fragment implements ConnectionInfoListener {

	protected static final int CHOOSE_FILE_RESULT_CODE = 20;
	private View mContentView = null;
	private WifiP2pDevice device;
	ProgressDialog progressDialog = null;

	private static boolean async_running = false;

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		mContentView = inflater.inflate(R.layout.device_detail, null);
		mContentView.findViewById(R.id.btn_connect).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				WifiP2pConfig config = new WifiP2pConfig();
				config.deviceAddress = device.deviceAddress;
				config.groupOwnerIntent = 15;
				config.wps.setup = WpsInfo.PBC;
				if (progressDialog != null && progressDialog.isShowing()) {
					progressDialog.dismiss();
				}
				progressDialog = new ProgressDialog(getActivity());
		        progressDialog.setMessage("Requesting Connection!");
		        progressDialog.setCancelable(true);
		        progressDialog.show();
				((DeviceActionListener) getActivity()).connect(config);

			}
		});

		mContentView.findViewById(R.id.btn_disconnect).setOnClickListener(
				new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						((DeviceActionListener) getActivity()).disconnect();
					}
				});

		mContentView.findViewById(R.id.btn_start_client).setOnClickListener(
				new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						// kmrn
						Intent serviceIntent = new Intent(getActivity(), Broadcaster.class);
						// serviceIntent.setAction(Broadcaster.ACTION_SEND_FILE);
						getActivity().startService(serviceIntent);
					}
				});

		return mContentView;
	}

	@Override
	public void onConnectionInfoAvailable(final WifiP2pInfo info) {
		if (progressDialog != null && progressDialog.isShowing()) {
			progressDialog.dismiss();
		}
		this.getView().setVisibility(View.VISIBLE);

		// The owner IP is now known.
		TextView view = (TextView) mContentView.findViewById(R.id.group_owner);
		view.setText(getResources().getString(R.string.group_owner_text)
				+ ((info.isGroupOwner == true) ? getResources().getString(R.string.yes)
						: getResources().getString(R.string.no)));

		// InetAddress from WifiP2pInfo struct.
		view = (TextView) mContentView.findViewById(R.id.device_info);
		view.setText("Group Owner IP - " + info.groupOwnerAddress.getHostAddress());

		if (info.groupFormed && info.isGroupOwner) {
			mContentView.findViewById(R.id.btn_start_client).setVisibility(View.VISIBLE);
		} else if (info.groupFormed) {
			Thread lis = new Thread(runner);     
			lis.start();
		}

		mContentView.findViewById(R.id.btn_connect).setVisibility(View.GONE);
	}

	/**
	 * Updates the UI with device data
	 * 
	 * @param device the device to be displayed
	 */
	public void showDetails(WifiP2pDevice device) {
		this.device = device;
		this.getView().setVisibility(View.VISIBLE);
		TextView view = (TextView) mContentView.findViewById(R.id.device_info);
		view.setText(device.toString());

	}

	/**
	 * Clears the UI fields after a disconnect or direct mode disable operation.
	 */
	public void resetViews() {
		mContentView.findViewById(R.id.btn_connect).setVisibility(View.VISIBLE);
		TextView view = (TextView) mContentView.findViewById(R.id.device_info);
		view.setText(R.string.empty);
		view = (TextView) mContentView.findViewById(R.id.group_owner);
		view.setText(R.string.empty);
		mContentView.findViewById(R.id.btn_start_client).setVisibility(View.GONE);
		this.getView().setVisibility(View.GONE);
	}

	public static class AsyncListenTask extends AsyncTask<Void, Void, String> {
		private Context context;
		public AsyncListenTask(Context context) {
			this.context = context;
		}

		@Override
		protected String doInBackground(Void... params) {
			DatagramSocket socket = null;
			try {
				Log.d("kmrn", "Async started!");
				socket = new DatagramSocket(8988);
				socket.setReuseAddress(true);
				byte[] buf = new byte[1024];
				DatagramPacket packet = new DatagramPacket(buf, buf.length);
				socket.receive(packet);
				Log.d("kmrn", "Async received!");
				socket.close();
				async_running = false;
				return new String(packet.getData());
			} catch (IOException e) {
				Log.e("kmrn", e.getMessage());
				e.printStackTrace();
				return null;
			}
		}

		@Override
		protected void onPostExecute(String packet) {
			// kmrn
			if (packet != null) Toast.makeText(context, packet ,Toast.LENGTH_SHORT).show();
			String[] parts = packet.split("[:]");
			int s_time = Integer.parseInt(parts[0]); 
			long s_position = Long.parseLong(parts[1]);
			long my_time = System.currentTimeMillis();
			String s_file = parts[2];
			Cursor mc = context.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, s_file, null, null);
			mc.moveToNext();
			//mp.setDataSource(mc.getString(1));
			//mp.prepare();
			//mp.start();
			//mp.seekto(s_position + s_time - my_time);
		}

		@Override
		protected void onPreExecute() {}
	}

	Runnable runner = new Runnable() {      
	    @Override
	    public void run() {
	        while(true){
	        	if(!async_running) {
	        		new AsyncListenTask(getActivity()).execute();
	        		async_running = true;
	        	}
	        	}
	        }           
	    };
}
