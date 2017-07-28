package com.zev.swift;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

import android.util.Log;

public abstract class Member {
	public static final int CONNECTED=0;
	public static final int RECEIVE_MESSAGE=1;
	public static final int RECEIVE_FILE=2;
	public static final int DISCONNECTED=3;
	public static final int SEND_FILE_PORT=8999;
	public static final int CHAT_PORT=8988;
	public static final char SEND_FILE_FLAG='\u001f';
	public static final char ALLOW_RECEIVE_FILE='\u001e';
	public static final char NOT_ALLOW_RECEIVE_FILE='\u001d';
	public static final char SENDER_READY='\u001c';
	
	protected BufferedReader br;
	protected PrintWriter pw;
	protected Thread mThread;
	
	protected abstract void create();
	
	public void sendMessage(String msg) {
		// TODO Auto-generated method stub
		if(pw!=null){
			Log.w(ChatroomActivity.TAG,"server pw");
			pw.println(msg);
		}
		
	}
	
	public void confirmSendFile(String filename,int fileSize) {
		// TODO Auto-generated method stub
		
		//向对方发送发送文件的请求
		Log.w(ChatroomActivity.TAG,"send file name="+filename);
		sendMessage(SEND_FILE_FLAG+filename+SEND_FILE_FLAG+String.valueOf(fileSize));
	}
	
	public BufferedReader getReader(){
		return br;
	}
	public PrintWriter getWriter(){
		return pw;
	}

	public abstract void destroy();
	
}
