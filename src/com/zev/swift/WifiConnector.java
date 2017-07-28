package com.zev.swift;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

public class WifiConnector {
	private Context context;	//�������activity��������
	//private ConnectivityManager connManager;
	private WifiManager wifiManager;
	
	public WifiConnector(Context context){
		this.context=context;
		//connManager=(ConnectivityManager) this.context.getSystemService(Context.CONNECTIVITY_SERVICE);
		wifiManager=(WifiManager)this.context.getSystemService(Context.WIFI_SERVICE);
	}
	//���wifi�Ƿ��Ѿ����ӣ��Ƿ���true���񷵻�false
	public boolean checkWifiState(){
		//State wifiState=connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState();
		WifiInfo wifiInfo=wifiManager.getConnectionInfo();
//		if(wifiInfo.getNetworkId()==-1) w("û������");
//		else w("�Ѿ�����");
		
		//w(wifiState.toString());
		if(wifiInfo.getNetworkId()>=0||isWifiAPEnabled()) return true;
		else return false;
	}
	//��ת��ϵͳ��wifi����activity
	public void startSystemWifiActivity(){
		Intent intent=new Intent("android.net.wifi.PICK_WIFI_NETWORK");	//��ʽ����
		//����wifi
		enableWifi();

		context.startActivity(intent);
	}
	//����wifi�ȵ�
	public void createWifiAP(){
		//�ر�wifi
		disableWifi();
		//�ر�wifiAP
		disableWifiAP();
		//����wifiAP
		try {
			Method method=wifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
			WifiConfiguration netConfig=new WifiConfiguration();
			netConfig.SSID="MySwift";
			netConfig.preSharedKey="123456";
			
			netConfig.allowedAuthAlgorithms
					.set(WifiConfiguration.AuthAlgorithm.OPEN);
			netConfig.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
			netConfig.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
			netConfig.allowedKeyManagement
					.set(WifiConfiguration.KeyMgmt.WPA_PSK);
			netConfig.allowedPairwiseCiphers
					.set(WifiConfiguration.PairwiseCipher.CCMP);
			netConfig.allowedPairwiseCiphers
					.set(WifiConfiguration.PairwiseCipher.TKIP);
			netConfig.allowedGroupCiphers
					.set(WifiConfiguration.GroupCipher.CCMP);
			netConfig.allowedGroupCiphers
					.set(WifiConfiguration.GroupCipher.TKIP);
			
			method.invoke(wifiManager, netConfig,true);
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	private void enableWifi(){
		if(!wifiManager.isWifiEnabled())
			wifiManager.setWifiEnabled(true);
	}
	private void disableWifi(){
		if(wifiManager.isWifiEnabled())
			wifiManager.setWifiEnabled(false);
	}
	private void disableWifiAP(){
		if(isWifiAPEnabled()){
			try {
				Method method=wifiManager.getClass().getMethod("getWifiApConfiguration");
				method.setAccessible(true);
				WifiConfiguration config=(WifiConfiguration) method.invoke(wifiManager);
				
				Method method2=wifiManager.getClass().getMethod("setWifiApEnabled",WifiConfiguration.class,boolean.class);
				method2.setAccessible(true);
				method2.invoke(wifiManager, config,false);
			} catch (NoSuchMethodException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
	}
	private boolean isWifiAPEnabled(){
		try {
			Method method=wifiManager.getClass().getMethod("isWifiApEnabled");
			method.setAccessible(true);
			return (Boolean) method.invoke(wifiManager);
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}
}
