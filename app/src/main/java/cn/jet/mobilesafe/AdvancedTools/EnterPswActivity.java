package cn.jet.mobilesafe.AdvancedTools;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import cn.jet.mobilesafe.R;
import cn.jet.mobilesafe.AntiTheft.utils.MD5Utils;

public class EnterPswActivity extends Activity implements OnClickListener{
	private ImageView mAppIcon;
	private TextView mAppNameTV;
	private EditText mPswET;
	private ImageView mGoImgv;
	private LinearLayout mEnterPswLL;
	private SharedPreferences sp;
	private String password;
	private String mPackageName;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_enterpsw);
		sp = getSharedPreferences("config", MODE_PRIVATE);
		password = sp.getString("PhoneAntiTheftPWD", null);
		Intent intent = getIntent();
		mPackageName = intent.getStringExtra("packagename");
		PackageManager pm = getPackageManager();
		initView();
		setIcon();
		Log.i("Applock Activity", "onCreate " + mPackageName);
	}

	@Override
	protected void onStart() {
		super.onStart();
	}

	protected void onRestart() {
		Log.i("Applock Activity", "onRestart");
		Intent intent = getIntent();
		if (intent != null) {
			String pkgName = intent.getStringExtra("packagename");
			Log.i("Applock Activity", "onRestart pkgName: " + pkgName
					+ " original pkg " + mPackageName);
			if (!mPackageName.equals(pkgName)) {
				mPackageName = pkgName;
				setIcon();
			}
		} else {
			finish();
		}
		super.onRestart();
	}

	protected void onResume() {
		Log.i("Applock Activity", "onResume");
		super.onResume();
	}

	protected void onPause() {
		Log.i("Applock Activity", "onPause");
		super.onPause();
		finish();
	}
	protected void onStop() {
		Log.i("Applock Activity", "onStop");
		super.onStop();
	}
	protected void onDestroy() {
		Log.i("Applock Activity", "onDestroy");
		super.onDestroy();
	}
	/**
	 * 初始化控件
	 */
	private void initView() {
		mAppIcon = (ImageView) findViewById(R.id.imgv_appicon_enterpsw);
		mAppNameTV = (TextView) findViewById(R.id.tv_appname_enterpsw);
		mPswET = (EditText) findViewById(R.id.et_psw_enterpsw);
		mGoImgv = (ImageView) findViewById(R.id.imgv_go_enterpsw);
		mEnterPswLL = (LinearLayout) findViewById(R.id.ll_enterpsw);
		mGoImgv.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.imgv_go_enterpsw:
			//比较密码
			String inputpsw = mPswET.getText().toString().trim();
			if(TextUtils.isEmpty(inputpsw)){
				startAnim();
				Toast.makeText(this, "请输入密码！", 0).show();
				return;
			}else{
				if(!TextUtils.isEmpty(password)){
					if(MD5Utils.encode(inputpsw).equals(password)){
						//发送自定义的广播消息。
						Intent intent = new Intent();
						intent.setAction("cn.jet.mobilesafe.applock");
						intent.putExtra("packagename", mPackageName);
						sendBroadcast(intent);
						Log.i("Applock Activity", "after sendBroadcast");
						finish();
						Log.i("Applock Activity", "after finish");
					}else{
						startAnim();  
						Toast.makeText(this, "密码不正确！", 0).show();
						return;
					}
				}
			}
			break;
		}
	}

	private void startAnim() {
		Animation animation =AnimationUtils.loadAnimation(this, R.anim.shake);
		mEnterPswLL.startAnimation(animation);
	}

	private void setIcon() {
		try {
			PackageManager pm = getPackageManager();
			mAppIcon.setImageDrawable(pm.getApplicationInfo(mPackageName, 0).loadIcon(pm));
			mAppNameTV.setText(pm.getApplicationInfo(mPackageName, 0).loadLabel(pm).toString());
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
	}

}
