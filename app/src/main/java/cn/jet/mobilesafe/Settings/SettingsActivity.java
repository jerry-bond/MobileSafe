package cn.jet.mobilesafe.Settings;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import cn.jet.mobilesafe.R;
import cn.jet.mobilesafe.AdvancedTools.service.AppLockService;
import cn.jet.mobilesafe.Settings.utils.SystemInfoUtils;
import cn.jet.mobilesafe.Settings.widget.SettingView;
import cn.jet.mobilesafe.Settings.widget.SettingView.OnCheckedStatusIsChanged;

public class SettingsActivity extends Activity implements OnClickListener, OnCheckedStatusIsChanged {

	private SettingView mBlackNumSV;
	private SettingView mAppLockSV;
	private SharedPreferences mSP;
	private boolean running;
	private Intent intent;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_settings);
		mSP = getSharedPreferences("config", MODE_PRIVATE);
		initView();
	}

	private void initView() {
		findViewById(R.id.rl_titlebar).setBackgroundColor(
				getResources().getColor(R.color.bright_blue));
		ImageView mLeftImgv = (ImageView) findViewById(R.id.imgv_leftbtn);
		((TextView) findViewById(R.id.tv_title)).setText("设置中心");
		mLeftImgv.setOnClickListener(this);
		mLeftImgv.setImageResource(R.drawable.back);
		mBlackNumSV = (SettingView) findViewById(R.id.sv_blacknumber_set);
		mAppLockSV = (SettingView) findViewById(R.id.sv_applock_set);
		mBlackNumSV.setOnCheckedStatusIsChanged(this);
		mAppLockSV.setOnCheckedStatusIsChanged(this);
	}
	@Override
	protected void onStart() {
		running = SystemInfoUtils.isServiceRunning(this, "cn.jet.mobilesafe.AdvancedTools.service.AppLockService");
		mAppLockSV.setChecked(running);
		mBlackNumSV.setChecked(mSP.getBoolean("BlackNumStatus", true));
		super.onStart();
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.imgv_leftbtn:
			finish();
			break;
		}
	}

	@Override
	public void onCheckedChanged(View view, boolean isChecked) {
		switch (view.getId()) {
		case R.id.sv_blacknumber_set:
			saveStatus("BlackNumStatus",isChecked);
			break;
		case R.id.sv_applock_set:
			saveStatus("AppLockStatus", isChecked);
			//开启或者关闭看门狗服务
			if(isChecked){
				intent = new Intent(this,AppLockService.class);
				startService(intent);
			}else{
				stopService(intent);
			}
			break;
		}
	}
	private void saveStatus(String keyname, boolean isChecked) {
		if(!TextUtils.isEmpty(keyname)){
			Editor edit = mSP.edit();
			edit.putBoolean(keyname, isChecked);
			edit.commit();
		}
	}
}
