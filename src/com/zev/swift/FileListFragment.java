package com.zev.swift;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.zev.swift.DeviceListFragment.DeviceActionListener;

import android.app.ListFragment;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class FileListFragment extends ListFragment {
	private View contentView;
	private TextView tvDir;	//��ʾ��ǰ��·��
	private List<String> fileList=new ArrayList<String>();
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		contentView=inflater.inflate(R.layout.file_list,container);
		tvDir=(TextView) contentView.findViewById(R.id.tvDir);
		
		return contentView;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		this.setListAdapter(new FileListAdapter(getActivity(),R.layout.file_list_item,fileList));
		
		/*
		//���÷�����һ����Ŀ¼
		LayoutInflater inflater=(LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View v=inflater.inflate(R.layout.file_list_item, null);
		((TextView)v.findViewById(R.id.tvFileName)).setText("..");
		((ImageView)v.findViewById(R.id.ivIcon)).setImageResource(R.drawable.folder_icon);
		this.getListView().addHeaderView(v);
		*/
		
		//��ʼ��Ŀ¼Ϊ����sdcardĿ¼
		updateFileList(new File(Environment.getExternalStorageDirectory().getPath()));
		
		super.onActivityCreated(savedInstanceState);
	}
	
	
	
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		// TODO Auto-generated method stub
		
		//��������һ��Ŀ¼������
		if(position==0){
			File parentFile=new File(getDir()).getParentFile();
			//��Ŀ¼����
			if(parentFile!=null){
				updateFileList(parentFile);
			}else{
				Toast.makeText(getActivity(), "�ѵ����Ŀ¼", Toast.LENGTH_SHORT).show();
			}
		}else{
			
			//��ȡѡ����ļ�
			File selectedFile=new File(tvDir.getText().toString()+'/'+fileList.get(position));
			//�����Ŀ¼
			if(selectedFile.isDirectory()){
				updateFileList(selectedFile);
			}else{
				//û�����ӵ��豸ʱ�����Խ���Ŀ¼�����ǲ�����ѡ���ļ�
				if(((ChatroomActivity)getActivity()).isConnected()==false){
					Toast.makeText(getActivity(), "û�����ӵ��豸", Toast.LENGTH_SHORT).show();
				}else{
					//�����ļ�
					((DeviceActionListener)getActivity()).sendFile(selectedFile.getAbsolutePath());
				}
			}
		}
		
		
		super.onListItemClick(l, v, position, id);
	}
	
	//������һ���µ�Ŀ¼�ǻ���ô˷�������tvDir�Լ�list view
	private void updateFileList(File selectedDir){
		//�ж��Ƿ���Ȩ�޽���
		if(!selectedDir.canRead()){
			Toast.makeText(getActivity(), "Ȩ�޲���", Toast.LENGTH_SHORT).show();
			return;
		}
		//����tvDir
		tvDir.setText(selectedDir.getAbsolutePath());
		//����list view
		fileList.clear();	//�����һ��Ŀ¼������
		
		fileList.add("..");	//�����ϼ�Ŀ¼
		
		//�������ݵ�fileList
		for(String str:selectedDir.list()){
			fileList.add(str);
		}
		
		//��������
		Collections.sort(fileList,new Comparator<String>(){
			@Override
			public int compare(String str0,String str1) {
				return str0.compareTo(str1);
			}
			
		});
		
		//����list view
		((FileListAdapter)this.getListAdapter()).notifyDataSetChanged();
		
		//����list view��λ��
		this.getListView().setSelection(0);
	}
	
	private class FileListAdapter extends ArrayAdapter<String>{
		private List<String> fileList;	//����������fileList������
		
		public FileListAdapter(Context context, int resource, List<String> objects) {
			super(context, resource, objects);
			// TODO Auto-generated constructor stub
			fileList=objects;
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if(convertView==null){
				LayoutInflater inflater =(LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				convertView=inflater.inflate(R.layout.file_list_item,null);
			}
			//�����ļ���
			TextView tvFileName=(TextView) convertView.findViewById(R.id.tvFileName);
			String filename=fileList.get(position);
			if(tvFileName!=null){
				tvFileName.setText(filename);
			}
			//����ͼ��
			ImageView ivIcon=(ImageView)convertView.findViewById(R.id.ivIcon);
			if(ivIcon!=null){
				//�����ļ��л����ļ�ѡ��ͬ��icon
				File file=new File(tvDir.getText().toString()+'/'+filename);
				if(file.isDirectory()){
					ivIcon.setImageResource(R.drawable.folder_icon);
				}else{
					ivIcon.setImageResource(R.drawable.file_icon);
				}
				
			}
			return convertView;
			
		}
		
	}
	private String getDir(){
		return tvDir.getText().toString();
	}
	
}
