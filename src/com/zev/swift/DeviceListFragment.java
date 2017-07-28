package com.zev.swift;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import android.app.ListFragment;
import android.content.Context;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/*
 * 显示当前发现的设备列表，当用户点击一个可用设备时进行响应
 */
public class DeviceListFragment extends ListFragment implements PeerListListener,ConnectionInfoListener{
	private List<WifiP2pDevice> peers=new ArrayList<WifiP2pDevice>();
	private View mContentView;
	private Button btnConnect;
	private Button btnDisconnect;
	private DeviceListButtonListener mButtonListener;
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		mContentView=inflater.inflate(R.layout.device_list, container);
		btnConnect=(Button) mContentView.findViewById(R.id.btnConnect);
		btnDisconnect=(Button) mContentView.findViewById(R.id.btnDisconnect);
		
		mButtonListener=new DeviceListButtonListener();
		btnConnect.setOnClickListener(mButtonListener);
		btnDisconnect.setOnClickListener(mButtonListener);
		
		//初始时设置按钮都不可用
		btnConnect.setEnabled(false);
		btnDisconnect.setEnabled(false);
		
		return mContentView;
	}

	class DeviceListButtonListener implements View.OnClickListener{
		WifiP2pConfig  config;	//用户所选择设备的config
		
		public DeviceListButtonListener() {
			// TODO Auto-generated constructor stub
			
			//初始化config，后面可以重用
			config=new WifiP2pConfig();
			config.wps.setup=WpsInfo.PBC;
		}
		
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			switch(v.getId()){
			case R.id.btnConnect:
				((DeviceActionListener)getActivity()).connect(config);
				break;
			case R.id.btnDisconnect:
				((DeviceActionListener)getActivity()).disconnect();
				break;
			}
			v.setEnabled(false);
		}
		
	}
	
	@Override	
	public void onActivityCreated(Bundle savedInstanceState) {
		
		super.onActivityCreated(savedInstanceState);
		//设置ListFragment的适配器
		this.setListAdapter(new WiFiPeerListAdapter(getActivity(),R.layout.device_list_item,peers));
	}
	
	//当设备的状态改变时会调用此方法
	@Override
	public void onPeersAvailable(WifiP2pDeviceList list) {
		Log.w(ChatroomActivity.TAG, "调用了onPeersAvailable");
		
		//清空之前的设备列表
		peers.clear();
		//添加新的可用设备列表
		peers.addAll(list.getDeviceList());
		
		Log.w(ChatroomActivity.TAG,String.valueOf(peers.size()));
		//没有可用的设备
		if(peers.size()==0){
			Log.w(ChatroomActivity.TAG, "没有发现可用的设备");
		}else{
			
			for(WifiP2pDevice device:peers){
				if(device.status==WifiP2pDevice.AVAILABLE){
					Toast.makeText(getActivity(), "发现可用的设备",Toast.LENGTH_SHORT).show();
				}
			}
			
		}
		//更新list
		 ((WiFiPeerListAdapter)getListAdapter()).notifyDataSetChanged();
	}
	
	
	//当用户想要连接某个设备时，响应用户的点击事件
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		/*
		//获取用户想要连接设备的信息
		WifiP2pDevice connectedDevice=peers.get(position);
		WifiP2pConfig config=new WifiP2pConfig();
		config.deviceAddress=connectedDevice.deviceAddress;
		config.wps.setup=WpsInfo.PBC;
		
		
		//回掉宿主activity方法进行连接
		((DeviceActionListener)getActivity()).connect(config);
		*/
		
		WifiP2pDevice selectedDevice=peers.get(position);
		
		mButtonListener.config.deviceAddress=selectedDevice.deviceAddress;
		
		switch(selectedDevice.status){
		case WifiP2pDevice.AVAILABLE:
			btnConnect.setEnabled(true);
			break;
		case WifiP2pDevice.CONNECTED:
			btnDisconnect.setEnabled(true);
			break;
		case WifiP2pDevice.FAILED:
		case WifiP2pDevice.INVITED:
		case WifiP2pDevice.UNAVAILABLE:
			btnDisconnect.setEnabled(true);
		}
		
		super.onListItemClick(l, v, position, id);
	}



	/*
	 * 自定义的适配器，作为DeviceListFragment的适配器
	 */
	private class WiFiPeerListAdapter extends ArrayAdapter<WifiP2pDevice>{
		private List<WifiP2pDevice> items;
		public WiFiPeerListAdapter(Context context, int resource, List<WifiP2pDevice> objects) {
			super(context, resource, objects);
			items=objects;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v=convertView;
			if(v==null){
				LayoutInflater inflater=(LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				//第二个参数要为null，不能为parent
				v=inflater.inflate(R.layout.device_list_item,null);
			}
			
			WifiP2pDevice device=items.get(position);
			if(device!=null){
				TextView tvName=(TextView) v.findViewById(R.id.tvName);
				if(tvName!=null){
					tvName.setText(device.deviceName);
				}
				TextView tvStatus=(TextView)v.findViewById(R.id.tvStatus);
				if(tvStatus!=null){
					tvStatus.setText(getDeviceStatus(device.status));
				}
			}
			
			return v;
		}
		
		
	}
	//将WifiP2pDevice中的状态码转化为字符串
	private static String getDeviceStatus(int status){
		switch(status){
			case WifiP2pDevice.AVAILABLE:
				return "Available";
			case WifiP2pDevice.INVITED:
				return "Invited";
			case WifiP2pDevice.UNAVAILABLE:
				return "Unavailable";
			case WifiP2pDevice.CONNECTED:
				return "Connected";
			case WifiP2pDevice.FAILED:
				return "Failed";
			default:
				return "Unknown";
		}
	}
	//更新这个设备的信息
	public void updateThisDevice(WifiP2pDevice device){
		TextView selfName=(TextView) mContentView.findViewById(R.id.tvSelfName);
		if(selfName!=null){
			selfName.setText(device.deviceName);
		}
		TextView selfStatus=(TextView) mContentView.findViewById(R.id.tvSelfStatus);
		if(selfStatus!=null){
			selfStatus.setText(getDeviceStatus(device.status));
		}
	}
	//fragment与宿主activity之间的通讯接口
	public interface DeviceActionListener{
		void createServerMember();
		void createClientMember(InetAddress hostAddress);
		void sendFile(String absoultPath);
		void connect(WifiP2pConfig config);
		void disconnect();
	}
	
	//当有外部设备连接时会调用此方法
	@Override
	public void onConnectionInfoAvailable(WifiP2pInfo info){
		Log.w(ChatroomActivity.TAG, "调用了onConnectionInfoAvailable");
		//形成group
		if(info.groupFormed){
			Log.w(ChatroomActivity.TAG, "形成了group");
			if(info.isGroupOwner){
				//do something that owner should do
				Log.w(ChatroomActivity.TAG, "创建 server member");
				((DeviceActionListener)getActivity()).createServerMember();
			}else{
				//do something that client should do
				Log.w(ChatroomActivity.TAG,"创建 client member");
				((DeviceActionListener)getActivity()).createClientMember(info.groupOwnerAddress);
			}
		}else{
			Log.w(ChatroomActivity.TAG, "没有形成group");
		}
	}
}
