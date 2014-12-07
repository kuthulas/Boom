package com.kmrn.android.boom;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

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
	WiFiDirectActivity activity = (WiFiDirectActivity) getActivity();
	private static boolean async_running = false;
	private static boolean device_state = false;
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
				if(!device_state){
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
				else {
					((DeviceActionListener) getActivity()).disconnect();
					Button btn = (Button)mContentView.findViewById(R.id.btn_connect);
					btn.setText("Connect");
				}
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

		if (info.groupFormed && info.isGroupOwner) {
			((WiFiDirectActivity)getActivity()).set_group_owner(true);

		} else if (info.groupFormed) {
			Thread lis = new Thread(runner);
			lis.start();
		}
		Button btn = (Button)mContentView.findViewById(R.id.btn_connect);
		btn.setText("Disconnect");
	}

	/**
	 * Updates the UI with device data
	 * 
	 * @param device the device to be displayed
	 */
	public void showDetails(WifiP2pDevice device) {
		this.device = device;
		this.getView().setVisibility(View.VISIBLE);
	}

	/**
	 * Clears the UI fields after a disconnect or direct mode disable operation.
	 */
	public void resetViews() {
		Button btn = (Button)mContentView.findViewById(R.id.btn_connect);
		btn.setText("Connect");
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
				//String[] parts = new String(packet.getData()).split("[:]");
				//((WiFiDirectActivity)context).set_go_offset(System.currentTimeMillis() - Long.parseLong(parts[0]));
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
			String[] parts = packet.split("[:]");
			((WiFiDirectActivity)context).set_go_offset(System.currentTimeMillis() - Long.parseLong(parts[0]));
			long s_time = Long.parseLong(parts[1]);
			int s_position = Integer.parseInt(parts[2]);
			String s_file = parts[3];
			int s_play = Integer.parseInt(parts[4]);

			try {
				((WiFiDirectActivity)context).sync_play(s_time, s_position, s_file, s_play);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (IllegalStateException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
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

	public void state_update(WifiP2pDevice device) {
		if(device.status != WifiP2pDevice.CONNECTED) 
			device_state = false;
		else device_state = true;
	}
}
