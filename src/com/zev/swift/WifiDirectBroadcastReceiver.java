package com.zev.swift;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.util.Log;

/*
 * A BroadcastReceiver that notifies of important WiFi p2p events  
 */
public class WifiDirectBroadcastReceiver extends BroadcastReceiver {
	private ChatroomActivity mActivity;
	private WifiP2pManager mManager;
	private Channel mChannel;
	
	public WifiDirectBroadcastReceiver(ChatroomActivity mActivity,WifiP2pManager mManager,Channel mChannel) {
		this.mActivity=mActivity;
		this.mManager=mManager;
		this.mChannel=mChannel;
	}
	
	@Override
	public void onReceive(Context context, Intent intent) {
		String action=intent.getAction();
		
		if(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)){
			//当注册广播后第一个收到这个消息
			//检查本机的wifi p2p状态是否可用
			Log.w(ChatroomActivity.TAG, "WIFI_P2P_STATE_CHANGED_ACTION");
			//获取本机的wifi p2p状态
			int state =intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
			//通知主activity wifi p2p的状态
			if(state==WifiP2pManager.WIFI_P2P_STATE_ENABLED){
				mActivity.setWiFiState(true);
			}else{
				mActivity.setWiFiState(false);
			}
		}else if(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)){
			//注册广播后收到的第二个消息，发现本身设备可用
			//当本身或者外部设备状态改变时会收到此消息
			Log.w(ChatroomActivity.TAG, "WIFI_P2P_PEERS_CHANGED_ACTION");
			if(mManager!=null){
				//更新device list
				mManager.requestPeers(mChannel, (WifiP2pManager.PeerListListener)mActivity.getFragmentManager().findFragmentById(R.id.device_list));
			}
			
		}else if(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)){
			//注册广播后收到的第三个消息
			//当本身设备与外部设备的连接情况改变时收到此消息
			Log.w(ChatroomActivity.TAG,"WIFI_P2P_CONNECTION_CHANGED_ACTION");
			if(mManager==null){
				Log.w(ChatroomActivity.TAG, "mManager==null");
				return;
			}
			//获取连接信息
			NetworkInfo info=(NetworkInfo)intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
			if(info.isConnected()){
				//连接成功
				Log.w(ChatroomActivity.TAG, "获得外部设备连接");
				mManager.requestConnectionInfo(mChannel, (ConnectionInfoListener)mActivity.getFragmentManager().findFragmentById(R.id.device_list));
				
				/*
				//由ChatroomActivity接管
				//更新发送按钮
				mActivity.findViewById(R.id.btnSend).setEnabled(true);
				*/
			}else{
				Log.w(ChatroomActivity.TAG, "失去外部设备连接");
				//连接失败，如果是因为对方设备不可用的话，将会收到广播WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION，这里不用对此做处理
				
				/*
				//由ChatroomActivity接管
				//更新发送按钮
				mActivity.findViewById(R.id.btnSend).setEnabled(false);
				*/
			}
			
		}else if(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)){
			//注册广播后收到的第四个消息
			//当本机设备的细节改变时收到此消息
			Log.w(ChatroomActivity.TAG, "WIFI_P2P_THIS_DEVICE_CHANGED_ACTION");
			
			DeviceListFragment fragment=(DeviceListFragment) mActivity.getFragmentManager().findFragmentById(R.id.device_list);
			fragment.updateThisDevice(((WifiP2pDevice)intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE)));
		}
	}

}
