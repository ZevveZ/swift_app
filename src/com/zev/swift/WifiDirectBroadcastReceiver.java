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
			//��ע��㲥���һ���յ������Ϣ
			//��鱾����wifi p2p״̬�Ƿ����
			Log.w(ChatroomActivity.TAG, "WIFI_P2P_STATE_CHANGED_ACTION");
			//��ȡ������wifi p2p״̬
			int state =intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
			//֪ͨ��activity wifi p2p��״̬
			if(state==WifiP2pManager.WIFI_P2P_STATE_ENABLED){
				mActivity.setWiFiState(true);
			}else{
				mActivity.setWiFiState(false);
			}
		}else if(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)){
			//ע��㲥���յ��ĵڶ�����Ϣ�����ֱ����豸����
			//����������ⲿ�豸״̬�ı�ʱ���յ�����Ϣ
			Log.w(ChatroomActivity.TAG, "WIFI_P2P_PEERS_CHANGED_ACTION");
			if(mManager!=null){
				//����device list
				mManager.requestPeers(mChannel, (WifiP2pManager.PeerListListener)mActivity.getFragmentManager().findFragmentById(R.id.device_list));
			}
			
		}else if(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)){
			//ע��㲥���յ��ĵ�������Ϣ
			//�������豸���ⲿ�豸����������ı�ʱ�յ�����Ϣ
			Log.w(ChatroomActivity.TAG,"WIFI_P2P_CONNECTION_CHANGED_ACTION");
			if(mManager==null){
				Log.w(ChatroomActivity.TAG, "mManager==null");
				return;
			}
			//��ȡ������Ϣ
			NetworkInfo info=(NetworkInfo)intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
			if(info.isConnected()){
				//���ӳɹ�
				Log.w(ChatroomActivity.TAG, "����ⲿ�豸����");
				mManager.requestConnectionInfo(mChannel, (ConnectionInfoListener)mActivity.getFragmentManager().findFragmentById(R.id.device_list));
				
				/*
				//��ChatroomActivity�ӹ�
				//���·��Ͱ�ť
				mActivity.findViewById(R.id.btnSend).setEnabled(true);
				*/
			}else{
				Log.w(ChatroomActivity.TAG, "ʧȥ�ⲿ�豸����");
				//����ʧ�ܣ��������Ϊ�Է��豸�����õĻ��������յ��㲥WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION�����ﲻ�öԴ�������
				
				/*
				//��ChatroomActivity�ӹ�
				//���·��Ͱ�ť
				mActivity.findViewById(R.id.btnSend).setEnabled(false);
				*/
			}
			
		}else if(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)){
			//ע��㲥���յ��ĵ��ĸ���Ϣ
			//�������豸��ϸ�ڸı�ʱ�յ�����Ϣ
			Log.w(ChatroomActivity.TAG, "WIFI_P2P_THIS_DEVICE_CHANGED_ACTION");
			
			DeviceListFragment fragment=(DeviceListFragment) mActivity.getFragmentManager().findFragmentById(R.id.device_list);
			fragment.updateThisDevice(((WifiP2pDevice)intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE)));
		}
	}

}
