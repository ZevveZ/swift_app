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
 * ��ʾ��ǰ���ֵ��豸�б����û����һ�������豸ʱ������Ӧ
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
		
		//��ʼʱ���ð�ť��������
		btnConnect.setEnabled(false);
		btnDisconnect.setEnabled(false);
		
		return mContentView;
	}

	class DeviceListButtonListener implements View.OnClickListener{
		WifiP2pConfig  config;	//�û���ѡ���豸��config
		
		public DeviceListButtonListener() {
			// TODO Auto-generated constructor stub
			
			//��ʼ��config�������������
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
		//����ListFragment��������
		this.setListAdapter(new WiFiPeerListAdapter(getActivity(),R.layout.device_list_item,peers));
	}
	
	//���豸��״̬�ı�ʱ����ô˷���
	@Override
	public void onPeersAvailable(WifiP2pDeviceList list) {
		Log.w(ChatroomActivity.TAG, "������onPeersAvailable");
		
		//���֮ǰ���豸�б�
		peers.clear();
		//����µĿ����豸�б�
		peers.addAll(list.getDeviceList());
		
		Log.w(ChatroomActivity.TAG,String.valueOf(peers.size()));
		//û�п��õ��豸
		if(peers.size()==0){
			Log.w(ChatroomActivity.TAG, "û�з��ֿ��õ��豸");
		}else{
			
			for(WifiP2pDevice device:peers){
				if(device.status==WifiP2pDevice.AVAILABLE){
					Toast.makeText(getActivity(), "���ֿ��õ��豸",Toast.LENGTH_SHORT).show();
				}
			}
			
		}
		//����list
		 ((WiFiPeerListAdapter)getListAdapter()).notifyDataSetChanged();
	}
	
	
	//���û���Ҫ����ĳ���豸ʱ����Ӧ�û��ĵ���¼�
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		/*
		//��ȡ�û���Ҫ�����豸����Ϣ
		WifiP2pDevice connectedDevice=peers.get(position);
		WifiP2pConfig config=new WifiP2pConfig();
		config.deviceAddress=connectedDevice.deviceAddress;
		config.wps.setup=WpsInfo.PBC;
		
		
		//�ص�����activity������������
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
	 * �Զ��������������ΪDeviceListFragment��������
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
				//�ڶ�������ҪΪnull������Ϊparent
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
	//��WifiP2pDevice�е�״̬��ת��Ϊ�ַ���
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
	//��������豸����Ϣ
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
	//fragment������activity֮���ͨѶ�ӿ�
	public interface DeviceActionListener{
		void createServerMember();
		void createClientMember(InetAddress hostAddress);
		void sendFile(String absoultPath);
		void connect(WifiP2pConfig config);
		void disconnect();
	}
	
	//�����ⲿ�豸����ʱ����ô˷���
	@Override
	public void onConnectionInfoAvailable(WifiP2pInfo info){
		Log.w(ChatroomActivity.TAG, "������onConnectionInfoAvailable");
		//�γ�group
		if(info.groupFormed){
			Log.w(ChatroomActivity.TAG, "�γ���group");
			if(info.isGroupOwner){
				//do something that owner should do
				Log.w(ChatroomActivity.TAG, "���� server member");
				((DeviceActionListener)getActivity()).createServerMember();
			}else{
				//do something that client should do
				Log.w(ChatroomActivity.TAG,"���� client member");
				((DeviceActionListener)getActivity()).createClientMember(info.groupOwnerAddress);
			}
		}else{
			Log.w(ChatroomActivity.TAG, "û���γ�group");
		}
	}
}
