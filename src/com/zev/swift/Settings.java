package com.zev.swift;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

public class Settings extends Activity {
	private EditText edtNickname;
	private EditText edtServerPort;
	private EditText edtStorePath;
	private SharedPreferences preferences;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);
		
		preferences=getSharedPreferences("settings", MODE_PRIVATE);
		
		edtNickname=(EditText) findViewById(R.id.edtNickname);
		edtServerPort=(EditText) findViewById(R.id.edtServerPort);
		edtStorePath=(EditText) findViewById(R.id.edtStorePath);
		
		edtNickname.setText(preferences.getString("nickname", "client"));
		edtServerPort.setText(preferences.getString("server_port", "8080"));
		edtStorePath.setText(preferences.getString("store_path", Environment.getExternalStorageDirectory().getPath()+"/swift_files"));
		
		//定位光标
		edtNickname.setSelection(edtNickname.getText().toString().length());
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.settings, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
		
		SharedPreferences.Editor editor=preferences.edit();
		
		editor.putString("nickname", edtNickname.getText().toString());
		editor.putString("server_port",edtServerPort.getText().toString());
		editor.putString("store_path", edtStorePath.getText().toString());		
		editor.commit();
		
	}
}
