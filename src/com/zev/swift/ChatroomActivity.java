package com.zev.swift;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import com.zev.swift.DeviceListFragment.DeviceActionListener;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

/*
 * 还需要改进的地方：
 * 1.在activity生命周期中变量的保存、线程的管理
 * 2.ui界面的设计
 * 3.考虑要不要加入多人模式
 */

public class ChatroomActivity extends Activity implements DeviceActionListener{

	//wifi direct所需要的变量
	private WifiP2pManager mManager;	
	private Channel mChannel;
	
	//通过接受广播来获取周围设备的情况
	private BroadcastReceiver mReceiver;
	private IntentFilter mIntentFilter;
	
	private boolean wifiIsEnable;
	private Member me;
	private Button btnSend;
	private EditText edtMessage;
	private TextView tvMessage;
	public static final String TAG="com.zev.swift";
	private Handler handler;
	//private NotificationManager mNManager;
	private InetAddress globalInetAddress;
	private String sendFilePath;
	private String receiveFilename;
	private boolean isConnected;	//默认初始化为false
	private int fileSize;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_chatroom);
		
		//隐藏actionbar的图标
		getActionBar().setDisplayShowHomeEnabled(false);
		
		//初始化变量
		mManager=(WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
		mChannel=mManager.initialize(this, getMainLooper(), null);
		mReceiver=new WifiDirectBroadcastReceiver(this,mManager,mChannel);
		
		btnSend=(Button) findViewById(R.id.btnSend);
		edtMessage=(EditText)findViewById(R.id.edtMessage);
		tvMessage=(TextView)findViewById(R.id.tvMessage);
		
		//设置mReceiver的过滤器
		mIntentFilter=new IntentFilter();
		mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
		mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
		mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
		mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
		
		//此时还没有实例化me，disable'发送'按钮
		btnSend.setEnabled(false);
		
		//设置handler处理对应的事件
		handler=new Handler(){

			@Override
			public void handleMessage(Message msg) {
				// TODO Auto-generated method stub
				switch(msg.what){
				case Member.CONNECTED:
					btnSend.setEnabled(true);
					isConnected=true;
					break;
				case Member.DISCONNECTED:
					btnSend.setEnabled(false);
					isConnected=false;
					break;
				case Member.RECEIVE_MESSAGE:
					String line=(String) msg.obj;
					tvMessage.append("From:"+line+'\n');
					edtMessage.setText("");
					break;
				case Member.RECEIVE_FILE:
					//询问用户是否接受文件
					//Log.w(ChatroomActivity.TAG, "dialog之前filename="+(String)msg.obj);
					AlertDialog.Builder builder=new AlertDialog.Builder(ChatroomActivity.this);
					builder.setMessage("是否接收文件:\n"+receiveFilename).setPositiveButton("是",new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub
							//向服务器发送同意接收文件的信息
							me.sendMessage(String.valueOf(Member.ALLOW_RECEIVE_FILE));
						}
					}).setNegativeButton("否", new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							//向服务器发送不同意接收文件的信息
							me.sendMessage(String.valueOf(Member.NOT_ALLOW_RECEIVE_FILE));
						}
					});
					builder.show();
					
					break;
					
				case Member.ALLOW_RECEIVE_FILE:
					//me.sendFile(filepath);
					//启动发送文件的任务
					new SendFileAsyncTask().execute(sendFilePath);
					break;
				case Member.NOT_ALLOW_RECEIVE_FILE:
					Toast.makeText(ChatroomActivity.this, "对方拒绝接受文件", Toast.LENGTH_SHORT).show();
					break;
				case Member.SENDER_READY:
					Log.w(ChatroomActivity.TAG,"receiver know sender ready");
					new ReceiveFileAsyncTask().execute(receiveFilename);
					break;
				}
				super.handleMessage(msg);
			}
			
		};
		
		//设置发送按钮的监听器
		btnSend.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View arg0) {
				String msg=edtMessage.getText().toString();
				me.sendMessage(msg);
				//更新tvMessage
				tvMessage.append("To:"+msg+'\n');
				//更新edtMessage
				edtMessage.setText("");
			}
			
		});
		
		//初始化NotificationManager
		//mNManager=(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.chatroom, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.discover_peers) {
			if(wifiIsEnable){
				//发现周围的peers,如果成功mReceiver将会收到WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION
				mManager.discoverPeers(mChannel, new ActionListener(){

					@Override
					public void onFailure(int arg0) {
						Toast.makeText(ChatroomActivity.this, "Fail to start discovery", Toast.LENGTH_SHORT).show();
					}

					@Override
					public void onSuccess() {
						Toast.makeText(ChatroomActivity.this,"Discovering...",Toast.LENGTH_SHORT).show();
					}
					
				});
			}else{
				//启动WiFiConnector来连接
				new WifiConnector(this).startSystemWifiActivity();
				
			}
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public void connect(WifiP2pConfig config) {
		Log.w(ChatroomActivity.TAG, "调用了connect");
		
		mManager.connect(mChannel, config, new ActionListener(){
			
			@Override
			public void onFailure(int arg0) {
				//Toast.makeText(ChatroomActivity.this, "初始化连接失败", Toast.LENGTH_SHORT).show();
				Log.w(ChatroomActivity.TAG, "初始化连接失败");
			}

			@Override
			public void onSuccess() {
				Log.w(ChatroomActivity.TAG, "初始化连接成功");
				//Toast.makeText(ChatroomActivity.this, "初始化连接成功", Toast.LENGTH_SHORT).show();
				//将会触发WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION的广播
				//WifiDirectBroadcastReceiver会做出相应的处理，这里不用做什么
				
			}
			
		});
	}
	
	
	
	private class ServerMember extends Member{
		private ServerSocket mServer;
		private Socket client;
		
		public ServerMember(){
				//初始化Member的成员
				create();
				
				//启动线程
				mThread.start();
				Log.w(ChatroomActivity.TAG, "服务器聊天线程启动");
		}

		@Override
		protected void create() {
			// TODO Auto-generated method stub
			mThread=new Thread(new Runnable(){
				/*
				@Override
				public void run() {
					// TODO Auto-generated method stub
					while(true){
						Log.w(ChatroomActivity.TAG, "I am the server thread");
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
				*/
				@Override
				public void run() {
					try {
						mServer=new ServerSocket(CHAT_PORT);
						Log.w(ChatroomActivity.TAG, "服务器已建立");
						
						//接受一个客户端的连接
						client=mServer.accept();
						Log.w(ChatroomActivity.TAG, "服务器接收到客户端的连接");
						globalInetAddress=client.getInetAddress();
						
						//初始化br和pw
						br=new BufferedReader(new InputStreamReader(client.getInputStream(),"UTF-8"));
						pw=new PrintWriter(new OutputStreamWriter(client.getOutputStream(),"UTF-8"),true);
						
						Message msg;
						
						//发送消息启动btnSend
						msg=Message.obtain();
						msg.what=CONNECTED;
						handler.sendMessage(msg);
						/*
						while(true){
							Log.w(ChatroomActivity.TAG, "连接中");
							
							try {
								Thread.sleep(1000);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
						
						*/
						//监听客户端的消息
						String line;
						while((line=br.readLine())!=null){
							msg=Message.obtain();
							if(line.length()==0){
								msg.what=RECEIVE_MESSAGE;
								msg.obj=line;
							}else{
								
								//处理信息的类型
								switch(line.charAt(0)){
								case ALLOW_RECEIVE_FILE:
									msg.what=ALLOW_RECEIVE_FILE;
									break;
								case NOT_ALLOW_RECEIVE_FILE:
									msg.what=NOT_ALLOW_RECEIVE_FILE;
									break;
								case SEND_FILE_FLAG:
									msg.what=RECEIVE_FILE;
									int end=line.lastIndexOf(SEND_FILE_FLAG);
									receiveFilename=line.substring(1,end);
									Log.w(TAG, receiveFilename);
									
									fileSize=Integer.parseInt(line.substring(end+1,line.length()));
									Log.w(TAG, String.valueOf(fileSize));
									
									break;
								case SENDER_READY:
									msg.what=SENDER_READY;
									break;
								default:
									msg.what=RECEIVE_MESSAGE;
									msg.obj=line;
									
								}
							}
						
							handler.sendMessage(msg);
						}
						
					} catch (IOException e) {
						e.printStackTrace();
					}finally{
						try {
							if(mServer!=null){
								mServer.close();
								Log.w(ChatroomActivity.TAG, "销毁ServerSocket");
								
							}
							//处理断开连接
							Message msg=Message.obtain();
							msg.what=DISCONNECTED;
							handler.sendMessage(msg);
							
						} catch (IOException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
						Log.w(ChatroomActivity.TAG, "销毁服务器聊天线程");
					}
				}
			});
		}

		@Override
		public void destroy() {
			// TODO Auto-generated method stub
			try {
				if(mServer!=null&&client!=null){
					client.shutdownInput();
					client.shutdownOutput();
					mServer.close();
					Log.w(ChatroomActivity.TAG, "mServer is closed");
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	private class ClientMember extends Member{
		private Socket mClient;
		
		public ClientMember(InetAddress hostAddress){
			globalInetAddress=hostAddress;//保存服务器的地址，方便传送文件时连接
			
			//执行客户端的连接以及监听服务器的消息
			create();
			
			mThread.start();
			Log.w(ChatroomActivity.TAG, "客户端聊天线程启动");
		}
		
		@Override
		protected void create() {
			// TODO Auto-generated method stub
			mThread=new Thread(new Runnable() {
				/*
				@Override
				public void run() {
					// TODO Auto-generated method stub
					while(true){
						Log.w(ChatroomActivity.TAG, "I am the client thread");
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
				*/
				@Override
				public void run() {
					try{
						mClient=new Socket();
						
						//连接服务器
						mClient.bind(null);
						mClient.connect(new InetSocketAddress(globalInetAddress,CHAT_PORT),5000);
						Log.w(ChatroomActivity.TAG, "客户端连接到服务器");
						
						//初始化输入输出流
						br=new BufferedReader(new InputStreamReader(mClient.getInputStream(),"UTF-8"));
						pw=new PrintWriter(new OutputStreamWriter(mClient.getOutputStream(),"UTF-8"),true);
						
						Message msg;	//msg每次使用的时候都要重新获取实例
						
						//发送消息通知handler已经连接服务器
						msg=Message.obtain();
						msg.what=CONNECTED;
						handler.sendMessage(msg);
						/*
						while(true){
							Log.w(ChatroomActivity.TAG, "连接中");
							
							try {
								Thread.sleep(1000);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
						
						*/
						//监听服务器的消息
						String line;
						while((line=br.readLine())!=null){
							msg=Message.obtain();
							//解析消息
							switch(line.charAt(0)){
							case ALLOW_RECEIVE_FILE:
								msg.what=ALLOW_RECEIVE_FILE;
								break;
							case NOT_ALLOW_RECEIVE_FILE:
								msg.what=NOT_ALLOW_RECEIVE_FILE;
								break;
							case SEND_FILE_FLAG:
								msg.what=RECEIVE_FILE;
								int end=line.lastIndexOf(SEND_FILE_FLAG);
								receiveFilename=line.substring(1,end);
								Log.w(TAG, receiveFilename);
								
								fileSize=Integer.parseInt(line.substring(end+1,line.length()));
								Log.w(TAG, String.valueOf(fileSize));
								
								break;
							case SENDER_READY:
								msg.what=SENDER_READY;
								break;
							default:
								msg.what=RECEIVE_MESSAGE;
								msg.obj=line;
								
							}
							handler.sendMessage(msg);
						}
					
					}catch(IOException e){
						e.printStackTrace();
					}finally{
						try{
							if(mClient!=null){
								mClient.close();
								Log.w(ChatroomActivity.TAG, "销毁Socket");
							}
							
							//处理断开连接
							Message msg=Message.obtain();
							msg.what=DISCONNECTED;
							handler.sendMessage(msg);
						}catch(IOException e){
							e.printStackTrace();
						}
						Log.w(ChatroomActivity.TAG, "销毁客户端聊天线程");
					}
					
				}
				
			});
		}

		@Override
		public void destroy() {
			// TODO Auto-generated method stub
			try {
				if(mClient!=null){
					mClient.shutdownInput();
					mClient.shutdownOutput();
					mClient.close();
					Log.w(ChatroomActivity.TAG, "mClient is closed");
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}
	
	
	//当广播收到获得外部设备连接的信号时，会调用此方法，注意当另一部设备建立了连接，如果这时候又
	//按了返回键，广播会被注销，但是下一次又注册广播的时候，又会收到与外部设备连接的信号，于是要
	//注意不能够重复创建对象
	@Override
	public void createServerMember() {
		// TODO Auto-generated method stub
		//注意不要重复创建对象
		//if(me==null){
			Log.w(ChatroomActivity.TAG, "new ServerMember");
			me=new ServerMember();
		//}
	}
	//由DeviceListFragment调用
	@Override
	public void createClientMember(InetAddress hostAddress) {
		// TODO Auto-generated method stub
		//if(me==null){
			Log.w(ChatroomActivity.TAG, "new ClientMember"); 
			me=new ClientMember(hostAddress);
		//}
	}
	
	//空间已经准备好了发送文件的功能
	private class SendFileAsyncTask extends AsyncTask<String, Integer, String>{
		
		//private Notification mNotify;
		//private int handleSize;	//记录已经处理的文件大小
		
		public SendFileAsyncTask() {
//			mNotify=new Notification();
//			mNotify.icon=R.drawable.file_icon;
//			mNotify.tickerText="正在发送文件";
//			mNotify.contentView=new RemoteViews(getPackageName(),R.layout.notification_item);
//			
//			mNManager.notify(0, mNotify);
		}
		//执行耗时的后台任务
		@Override
		protected String doInBackground(String... params) {
			// TODO Auto-generated method stub
			
			try {
				ServerSocket sendSocket=new ServerSocket(Member.SEND_FILE_PORT);
				
				//向接收者发送准备好了的信号
				me.sendMessage(String.valueOf(Member.SENDER_READY));
				
				Log.w(ChatroomActivity.TAG, "发送者在等待接收者");
				
				//设置等待接收者的时间
				sendSocket.setSoTimeout(5000);
				Socket receiveSocket=sendSocket.accept();
				Log.w(ChatroomActivity.TAG, "发送者连接上接收者");
				
				File file=new File(params[0]);
				
				//handleSize=0;
				
				Log.w(ChatroomActivity.TAG,"发送的文件大小="+String.valueOf(file.length()));
				
				
				transferFile(new FileInputStream(file),receiveSocket.getOutputStream());
				sendSocket.close();
				
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return "发送失败";
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return "发送失败";
			}
			return "发送成功";
		}
		
		//后台任务执行完成之后调用
		@Override
		protected void onPostExecute(String result) {
			// TODO Auto-generated method stub
			Toast.makeText(ChatroomActivity.this, result, Toast.LENGTH_SHORT).show();
			super.onPostExecute(result);
		}

		//任务执行之前调用
		@Override
		protected void onPreExecute() {
			// TODO Auto-generated method stub
			//通知栏显示下载通知
			//mNManager.notify(0,mNotify);
			Toast.makeText(ChatroomActivity.this, "正在发送文件", Toast.LENGTH_SHORT).show();
			super.onPreExecute();
		}
		
		//更新进度
		@Override
		protected void onProgressUpdate(Integer... values) {
			// TODO Auto-generated method stub
			Log.w(TAG, values[0].toString());
			
			//mNotify.contentView.setProgressBar(R.id.pb, 100, values[0], false);
//			mNotify.contentView.setProgressBar(R.id.pb, 100,values[0], false);
//			mNManager.notify(0,mNotify);
			//super.onProgressUpdate(values);
		}
		
		private void transferFile(InputStream in,OutputStream out){
			byte[] buf=new byte[1024];
			int readLength;
			try {
				while((readLength=in.read(buf))!=-1){
					out.write(buf,0,readLength);
					
//					handleSize+=readLength;
//					publishProgress(handleSize*100/fileSize);
				}
				out.flush();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}finally{
				//关闭输入流输出流
				try {
					in.close();
					out.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
	
	}
	
	//接收文件的功能
	private class ReceiveFileAsyncTask extends AsyncTask<String, Integer, String>{
		
		//private Notification mNotify;
		//private int handleSize;	//记录已经处理的文件大小
		
		public ReceiveFileAsyncTask() {
			// TODO Auto-generated constructor stub
			
//			mNotify=new Notification();
//			mNotify.icon=R.drawable.file_icon;
//			mNotify.tickerText="文件发送中";
//			mNotify.contentView=new RemoteViews(getPackageName(),R.layout.notification_item);
//			//省略设置点击通知栏是进入app
//			
//			mNManager.notify(0, mNotify);
		}
		
		@Override
		protected String doInBackground(String... params) {
			// TODO Auto-generated method stub
			
			File path=new File("/storage/sdcard0/files");
			if(!path.exists()){
				path.mkdirs();
			}
			File file=new File(path.getAbsolutePath()+'/'+params[0]);
			
			try {
				file.createNewFile();
				//Socket receiveSocket=new Socket(globalInetAddress,Member.SEND_FILE_PORT);
				Socket receiveSocket=new Socket();
				receiveSocket.bind(null);
				receiveSocket.connect(new InetSocketAddress(globalInetAddress,Member.SEND_FILE_PORT));
				
				Log.w(ChatroomActivity.TAG, "接收者连接到发送者");
				transferFile(receiveSocket.getInputStream(), new FileOutputStream(file));
				
				Log.w(ChatroomActivity.TAG, "收到的文件大小="+String.valueOf(file.length()));
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onPostExecute(String result) {
			// TODO Auto-generated method stub
			Toast.makeText(ChatroomActivity.this, "文件传送成功", Toast.LENGTH_SHORT).show();
			super.onPostExecute(result);
		}

		@Override
		protected void onPreExecute() {
			// TODO Auto-generated method stub
			
			
			Toast.makeText(ChatroomActivity.this, "开始传送文件", Toast.LENGTH_SHORT).show();
			super.onPreExecute();
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			// TODO Auto-generated method stub
			Log.w(TAG, values[0].toString());
			
//			mNotify.contentView.setProgressBar(R.id.pb, 100, values[0], false);
			//mNManager.notify(0,mNotify);
			
			super.onProgressUpdate(values);
		}
		
		private void transferFile(InputStream in,OutputStream out){
			byte[] buf=new byte[1024];
			int readLength;
			try {
				while((readLength=in.read(buf))!=-1){
					out.write(buf,0,readLength);
					
					//handleSize+=readLength;
					//publishProgress(handleSize*100/fileSize);
				}
				out.flush();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}finally{
				//关闭输入流输出流
				try {
					in.close();
					out.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	//由FileListFragment在选中一个文件时调用
	@Override
	public void sendFile(String absoultPath) {
		// TODO Auto-generated method stub
		//去掉路径
		File sendedFile=new File(absoultPath);
		fileSize=(int) sendedFile.length();
		me.confirmSendFile(sendedFile.getName(),(int)sendedFile.length());
		sendFilePath=absoultPath;
	}

	@Override
	public void disconnect() {
		// TODO Auto-generated method stub
		mManager.removeGroup(mChannel, new ActionListener(){

			@Override
			public void onFailure(int arg0) {
				// TODO Auto-generated method stub
				Toast.makeText(ChatroomActivity.this, "断开连接失败:"+arg0, Toast.LENGTH_SHORT).show();
				Log.w(ChatroomActivity.TAG, "断开连接失败："+arg0);
			}

			@Override
			public void onSuccess() {
				// TODO Auto-generated method stub
				Toast.makeText(ChatroomActivity.this, "断开连接成功", Toast.LENGTH_SHORT).show();
				
				Log.w(ChatroomActivity.TAG, "断开连接成功");
				
			}
			
		});
	}
	
	//返回当前的连接状态
	public boolean isConnected(){
		return isConnected;
	}

	@Override
	protected void onRestart() {
		// TODO Auto-generated method stub
		Log.w(ChatroomActivity.TAG, "onRestart");
		super.onRestart();
	}

	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		Log.w(ChatroomActivity.TAG, "onStart");
		super.onStart();
	}

	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		Log.w(ChatroomActivity.TAG, "onStop");
		//me.close();
		super.onStop();
	}
	
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		//销毁线程
		if(me!=null){
			me.destroy();
		}
		super.onDestroy();
		
		Log.w(ChatroomActivity.TAG, "onDestroy");
	}
	
	//撤销广播
	@Override
	protected void onPause() {
		super.onPause();
		Log.w(ChatroomActivity.TAG, "onPause");
		unregisterReceiver(mReceiver);
	}
	//注册广播
	@Override
	protected void onResume() {
		super.onResume();
		Log.w(ChatroomActivity.TAG, "onResume");
		registerReceiver(mReceiver,mIntentFilter);
	}
	//设置WiFi的状态
	void setWiFiState(boolean state){
		wifiIsEnable=state;
	}
}
