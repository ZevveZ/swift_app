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
	private TextView tvDir;	//显示当前的路径
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
		//设置返回上一级的目录
		LayoutInflater inflater=(LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View v=inflater.inflate(R.layout.file_list_item, null);
		((TextView)v.findViewById(R.id.tvFileName)).setText("..");
		((ImageView)v.findViewById(R.id.ivIcon)).setImageResource(R.drawable.folder_icon);
		this.getListView().addHeaderView(v);
		*/
		
		//初始化目录为外置sdcard目录
		updateFileList(new File(Environment.getExternalStorageDirectory().getPath()));
		
		super.onActivityCreated(savedInstanceState);
	}
	
	
	
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		// TODO Auto-generated method stub
		
		//处理返回上一级目录的请求
		if(position==0){
			File parentFile=new File(getDir()).getParentFile();
			//父目录存在
			if(parentFile!=null){
				updateFileList(parentFile);
			}else{
				Toast.makeText(getActivity(), "已到达根目录", Toast.LENGTH_SHORT).show();
			}
		}else{
			
			//获取选择的文件
			File selectedFile=new File(tvDir.getText().toString()+'/'+fileList.get(position));
			//如果是目录
			if(selectedFile.isDirectory()){
				updateFileList(selectedFile);
			}else{
				//没有连接到设备时，可以进入目录，但是不可以选择文件
				if(((ChatroomActivity)getActivity()).isConnected()==false){
					Toast.makeText(getActivity(), "没有连接到设备", Toast.LENGTH_SHORT).show();
				}else{
					//发送文件
					((DeviceActionListener)getActivity()).sendFile(selectedFile.getAbsolutePath());
				}
			}
		}
		
		
		super.onListItemClick(l, v, position, id);
	}
	
	//当进入一个新的目录是会调用此方法更新tvDir以及list view
	private void updateFileList(File selectedDir){
		//判断是否有权限进入
		if(!selectedDir.canRead()){
			Toast.makeText(getActivity(), "权限不够", Toast.LENGTH_SHORT).show();
			return;
		}
		//更新tvDir
		tvDir.setText(selectedDir.getAbsolutePath());
		//更新list view
		fileList.clear();	//清空上一个目录的数据
		
		fileList.add("..");	//返回上级目录
		
		//更新数据到fileList
		for(String str:selectedDir.list()){
			fileList.add(str);
		}
		
		//升序排序
		Collections.sort(fileList,new Comparator<String>(){
			@Override
			public int compare(String str0,String str1) {
				return str0.compareTo(str1);
			}
			
		});
		
		//更新list view
		((FileListAdapter)this.getListAdapter()).notifyDataSetChanged();
		
		//设置list view的位置
		this.getListView().setSelection(0);
	}
	
	private class FileListAdapter extends ArrayAdapter<String>{
		private List<String> fileList;	//保留对外面fileList的引用
		
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
			//设置文件名
			TextView tvFileName=(TextView) convertView.findViewById(R.id.tvFileName);
			String filename=fileList.get(position);
			if(tvFileName!=null){
				tvFileName.setText(filename);
			}
			//设置图标
			ImageView ivIcon=(ImageView)convertView.findViewById(R.id.ivIcon);
			if(ivIcon!=null){
				//根据文件夹还是文件选择不同的icon
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
