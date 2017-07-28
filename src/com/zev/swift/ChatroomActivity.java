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
 * ����Ҫ�Ľ��ĵط���
 * 1.��activity���������б����ı��桢�̵߳Ĺ���
 * 2.ui��������
 * 3.����Ҫ��Ҫ�������ģʽ
 */

public class ChatroomActivity extends Activity implements DeviceActionListener{

	//wifi direct����Ҫ�ı���
	private WifiP2pManager mManager;	
	private Channel mChannel;
	
	//ͨ�����ܹ㲥����ȡ��Χ�豸�����
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
	private boolean isConnected;	//Ĭ�ϳ�ʼ��Ϊfalse
	private int fileSize;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_chatroom);
		
		//����actionbar��ͼ��
		getActionBar().setDisplayShowHomeEnabled(false);
		
		//��ʼ������
		mManager=(WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
		mChannel=mManager.initialize(this, getMainLooper(), null);
		mReceiver=new WifiDirectBroadcastReceiver(this,mManager,mChannel);
		
		btnSend=(Button) findViewById(R.id.btnSend);
		edtMessage=(EditText)findViewById(R.id.edtMessage);
		tvMessage=(TextView)findViewById(R.id.tvMessage);
		
		//����mReceiver�Ĺ�����
		mIntentFilter=new IntentFilter();
		mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
		mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
		mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
		mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
		
		//��ʱ��û��ʵ����me��disable'����'��ť
		btnSend.setEnabled(false);
		
		//����handler�����Ӧ���¼�
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
					//ѯ���û��Ƿ�����ļ�
					//Log.w(ChatroomActivity.TAG, "dialog֮ǰfilename="+(String)msg.obj);
					AlertDialog.Builder builder=new AlertDialog.Builder(ChatroomActivity.this);
					builder.setMessage("�Ƿ�����ļ�:\n"+receiveFilename).setPositiveButton("��",new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub
							//�����������ͬ������ļ�����Ϣ
							me.sendMessage(String.valueOf(Member.ALLOW_RECEIVE_FILE));
						}
					}).setNegativeButton("��", new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							//����������Ͳ�ͬ������ļ�����Ϣ
							me.sendMessage(String.valueOf(Member.NOT_ALLOW_RECEIVE_FILE));
						}
					});
					builder.show();
					
					break;
					
				case Member.ALLOW_RECEIVE_FILE:
					//me.sendFile(filepath);
					//���������ļ�������
					new SendFileAsyncTask().execute(sendFilePath);
					break;
				case Member.NOT_ALLOW_RECEIVE_FILE:
					Toast.makeText(ChatroomActivity.this, "�Է��ܾ������ļ�", Toast.LENGTH_SHORT).show();
					break;
				case Member.SENDER_READY:
					Log.w(ChatroomActivity.TAG,"receiver know sender ready");
					new ReceiveFileAsyncTask().execute(receiveFilename);
					break;
				}
				super.handleMessage(msg);
			}
			
		};
		
		//���÷��Ͱ�ť�ļ�����
		btnSend.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View arg0) {
				String msg=edtMessage.getText().toString();
				me.sendMessage(msg);
				//����tvMessage
				tvMessage.append("To:"+msg+'\n');
				//����edtMessage
				edtMessage.setText("");
			}
			
		});
		
		//��ʼ��NotificationManager
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
				//������Χ��peers,����ɹ�mReceiver�����յ�WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION
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
				//����WiFiConnector������
				new WifiConnector(this).startSystemWifiActivity();
				
			}
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public void connect(WifiP2pConfig config) {
		Log.w(ChatroomActivity.TAG, "������connect");
		
		mManager.connect(mChannel, config, new ActionListener(){
			
			@Override
			public void onFailure(int arg0) {
				//Toast.makeText(ChatroomActivity.this, "��ʼ������ʧ��", Toast.LENGTH_SHORT).show();
				Log.w(ChatroomActivity.TAG, "��ʼ������ʧ��");
			}

			@Override
			public void onSuccess() {
				Log.w(ChatroomActivity.TAG, "��ʼ�����ӳɹ�");
				//Toast.makeText(ChatroomActivity.this, "��ʼ�����ӳɹ�", Toast.LENGTH_SHORT).show();
				//���ᴥ��WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION�Ĺ㲥
				//WifiDirectBroadcastReceiver��������Ӧ�Ĵ������ﲻ����ʲô
				
			}
			
		});
	}
	
	
	
	private class ServerMember extends Member{
		private ServerSocket mServer;
		private Socket client;
		
		public ServerMember(){
				//��ʼ��Member�ĳ�Ա
				create();
				
				//�����߳�
				mThread.start();
				Log.w(ChatroomActivity.TAG, "�����������߳�����");
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
						Log.w(ChatroomActivity.TAG, "�������ѽ���");
						
						//����һ���ͻ��˵�����
						client=mServer.accept();
						Log.w(ChatroomActivity.TAG, "���������յ��ͻ��˵�����");
						globalInetAddress=client.getInetAddress();
						
						//��ʼ��br��pw
						br=new BufferedReader(new InputStreamReader(client.getInputStream(),"UTF-8"));
						pw=new PrintWriter(new OutputStreamWriter(client.getOutputStream(),"UTF-8"),true);
						
						Message msg;
						
						//������Ϣ����btnSend
						msg=Message.obtain();
						msg.what=CONNECTED;
						handler.sendMessage(msg);
						/*
						while(true){
							Log.w(ChatroomActivity.TAG, "������");
							
							try {
								Thread.sleep(1000);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
						
						*/
						//�����ͻ��˵���Ϣ
						String line;
						while((line=br.readLine())!=null){
							msg=Message.obtain();
							if(line.length()==0){
								msg.what=RECEIVE_MESSAGE;
								msg.obj=line;
							}else{
								
								//������Ϣ������
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
								Log.w(ChatroomActivity.TAG, "����ServerSocket");
								
							}
							//����Ͽ�����
							Message msg=Message.obtain();
							msg.what=DISCONNECTED;
							handler.sendMessage(msg);
							
						} catch (IOException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
						Log.w(ChatroomActivity.TAG, "���ٷ����������߳�");
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
			globalInetAddress=hostAddress;//����������ĵ�ַ�����㴫���ļ�ʱ����
			
			//ִ�пͻ��˵������Լ���������������Ϣ
			create();
			
			mThread.start();
			Log.w(ChatroomActivity.TAG, "�ͻ��������߳�����");
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
						
						//���ӷ�����
						mClient.bind(null);
						mClient.connect(new InetSocketAddress(globalInetAddress,CHAT_PORT),5000);
						Log.w(ChatroomActivity.TAG, "�ͻ������ӵ�������");
						
						//��ʼ�����������
						br=new BufferedReader(new InputStreamReader(mClient.getInputStream(),"UTF-8"));
						pw=new PrintWriter(new OutputStreamWriter(mClient.getOutputStream(),"UTF-8"),true);
						
						Message msg;	//msgÿ��ʹ�õ�ʱ��Ҫ���»�ȡʵ��
						
						//������Ϣ֪ͨhandler�Ѿ����ӷ�����
						msg=Message.obtain();
						msg.what=CONNECTED;
						handler.sendMessage(msg);
						/*
						while(true){
							Log.w(ChatroomActivity.TAG, "������");
							
							try {
								Thread.sleep(1000);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
						
						*/
						//��������������Ϣ
						String line;
						while((line=br.readLine())!=null){
							msg=Message.obtain();
							//������Ϣ
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
								Log.w(ChatroomActivity.TAG, "����Socket");
							}
							
							//����Ͽ�����
							Message msg=Message.obtain();
							msg.what=DISCONNECTED;
							handler.sendMessage(msg);
						}catch(IOException e){
							e.printStackTrace();
						}
						Log.w(ChatroomActivity.TAG, "���ٿͻ��������߳�");
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
	
	
	//���㲥�յ�����ⲿ�豸���ӵ��ź�ʱ������ô˷�����ע�⵱��һ���豸���������ӣ������ʱ����
	//���˷��ؼ����㲥�ᱻע����������һ����ע��㲥��ʱ���ֻ��յ����ⲿ�豸���ӵ��źţ�����Ҫ
	//ע�ⲻ�ܹ��ظ���������
	@Override
	public void createServerMember() {
		// TODO Auto-generated method stub
		//ע�ⲻҪ�ظ���������
		//if(me==null){
			Log.w(ChatroomActivity.TAG, "new ServerMember");
			me=new ServerMember();
		//}
	}
	//��DeviceListFragment����
	@Override
	public void createClientMember(InetAddress hostAddress) {
		// TODO Auto-generated method stub
		//if(me==null){
			Log.w(ChatroomActivity.TAG, "new ClientMember"); 
			me=new ClientMember(hostAddress);
		//}
	}
	
	//�ռ��Ѿ�׼�����˷����ļ��Ĺ���
	private class SendFileAsyncTask extends AsyncTask<String, Integer, String>{
		
		//private Notification mNotify;
		//private int handleSize;	//��¼�Ѿ�������ļ���С
		
		public SendFileAsyncTask() {
//			mNotify=new Notification();
//			mNotify.icon=R.drawable.file_icon;
//			mNotify.tickerText="���ڷ����ļ�";
//			mNotify.contentView=new RemoteViews(getPackageName(),R.layout.notification_item);
//			
//			mNManager.notify(0, mNotify);
		}
		//ִ�к�ʱ�ĺ�̨����
		@Override
		protected String doInBackground(String... params) {
			// TODO Auto-generated method stub
			
			try {
				ServerSocket sendSocket=new ServerSocket(Member.SEND_FILE_PORT);
				
				//������߷���׼�����˵��ź�
				me.sendMessage(String.valueOf(Member.SENDER_READY));
				
				Log.w(ChatroomActivity.TAG, "�������ڵȴ�������");
				
				//���õȴ������ߵ�ʱ��
				sendSocket.setSoTimeout(5000);
				Socket receiveSocket=sendSocket.accept();
				Log.w(ChatroomActivity.TAG, "�����������Ͻ�����");
				
				File file=new File(params[0]);
				
				//handleSize=0;
				
				Log.w(ChatroomActivity.TAG,"���͵��ļ���С="+String.valueOf(file.length()));
				
				
				transferFile(new FileInputStream(file),receiveSocket.getOutputStream());
				sendSocket.close();
				
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return "����ʧ��";
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return "����ʧ��";
			}
			return "���ͳɹ�";
		}
		
		//��̨����ִ�����֮�����
		@Override
		protected void onPostExecute(String result) {
			// TODO Auto-generated method stub
			Toast.makeText(ChatroomActivity.this, result, Toast.LENGTH_SHORT).show();
			super.onPostExecute(result);
		}

		//����ִ��֮ǰ����
		@Override
		protected void onPreExecute() {
			// TODO Auto-generated method stub
			//֪ͨ����ʾ����֪ͨ
			//mNManager.notify(0,mNotify);
			Toast.makeText(ChatroomActivity.this, "���ڷ����ļ�", Toast.LENGTH_SHORT).show();
			super.onPreExecute();
		}
		
		//���½���
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
				//�ر������������
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
	
	//�����ļ��Ĺ���
	private class ReceiveFileAsyncTask extends AsyncTask<String, Integer, String>{
		
		//private Notification mNotify;
		//private int handleSize;	//��¼�Ѿ�������ļ���С
		
		public ReceiveFileAsyncTask() {
			// TODO Auto-generated constructor stub
			
//			mNotify=new Notification();
//			mNotify.icon=R.drawable.file_icon;
//			mNotify.tickerText="�ļ�������";
//			mNotify.contentView=new RemoteViews(getPackageName(),R.layout.notification_item);
//			//ʡ�����õ��֪ͨ���ǽ���app
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
				
				Log.w(ChatroomActivity.TAG, "���������ӵ�������");
				transferFile(receiveSocket.getInputStream(), new FileOutputStream(file));
				
				Log.w(ChatroomActivity.TAG, "�յ����ļ���С="+String.valueOf(file.length()));
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onPostExecute(String result) {
			// TODO Auto-generated method stub
			Toast.makeText(ChatroomActivity.this, "�ļ����ͳɹ�", Toast.LENGTH_SHORT).show();
			super.onPostExecute(result);
		}

		@Override
		protected void onPreExecute() {
			// TODO Auto-generated method stub
			
			
			Toast.makeText(ChatroomActivity.this, "��ʼ�����ļ�", Toast.LENGTH_SHORT).show();
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
				//�ر������������
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

	//��FileListFragment��ѡ��һ���ļ�ʱ����
	@Override
	public void sendFile(String absoultPath) {
		// TODO Auto-generated method stub
		//ȥ��·��
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
				Toast.makeText(ChatroomActivity.this, "�Ͽ�����ʧ��:"+arg0, Toast.LENGTH_SHORT).show();
				Log.w(ChatroomActivity.TAG, "�Ͽ�����ʧ�ܣ�"+arg0);
			}

			@Override
			public void onSuccess() {
				// TODO Auto-generated method stub
				Toast.makeText(ChatroomActivity.this, "�Ͽ����ӳɹ�", Toast.LENGTH_SHORT).show();
				
				Log.w(ChatroomActivity.TAG, "�Ͽ����ӳɹ�");
				
			}
			
		});
	}
	
	//���ص�ǰ������״̬
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
		//�����߳�
		if(me!=null){
			me.destroy();
		}
		super.onDestroy();
		
		Log.w(ChatroomActivity.TAG, "onDestroy");
	}
	
	//�����㲥
	@Override
	protected void onPause() {
		super.onPause();
		Log.w(ChatroomActivity.TAG, "onPause");
		unregisterReceiver(mReceiver);
	}
	//ע��㲥
	@Override
	protected void onResume() {
		super.onResume();
		Log.w(ChatroomActivity.TAG, "onResume");
		registerReceiver(mReceiver,mIntentFilter);
	}
	//����WiFi��״̬
	void setWiFiState(boolean state){
		wifiIsEnable=state;
	}
}
