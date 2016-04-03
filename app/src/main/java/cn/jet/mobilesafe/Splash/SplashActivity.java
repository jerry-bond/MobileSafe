package cn.jet.mobilesafe.Splash;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;
import android.widget.TextView;

import cn.jet.mobilesafe.R;
import cn.jet.mobilesafe.Splash.utils.MyUtils;
import cn.jet.mobilesafe.Splash.utils.VersionUpdateUtils;
import android.util.Log;
import android.content.res.Resources;
import cn.jet.mobilesafe.Subject;
import cn.jet.mobilesafe.Observer;
import cn.jet.mobilesafe.App;

/**
 * 欢迎页面
 * 
 * @author admin
 */
public class SplashActivity extends Activity implements Observer {

	private String TAG = "SplashActivity";
	/** 应用版本号 */
	private TextView mVersionTV;
	/** 本地版本号 */
	private String mVersion;

	private VersionUpdateUtils mUpdateUtils;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        App app = (App) this.getApplication();
        app.getApplicationSubject().attach(this);

		// 设置没有标题栏 在加载布局之前调用
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_splash);
		mVersion = MyUtils.getVersion(getApplicationContext());
		initView();

		mUpdateUtils = new VersionUpdateUtils(mVersion,
				SplashActivity.this);
		new Thread() {
			public void run() {
				// 获取服务器版本号
				mUpdateUtils.tryUpdate();
			};
		}.start();
	}

	/** 初始化控件 */
	private void initView() {
		mVersionTV = (TextView) findViewById(R.id.tv_splash_version);
		Resources res = getResources();
		mVersionTV.setText(res.getText(R.string.version_num) + mVersion);
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.d(TAG, "onResume");
	}

	@Override
	protected void onPause() {
		super.onPause();
		Log.d(TAG, "onPause");
	}

	@Override
	protected void onRestart() {
		super.onRestart();
		Log.d(TAG, "onRestart");
	}
	@Override
	protected void onStop() {
		super.onStop();
		Log.d(TAG, "onStop");
	}

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
		mUpdateUtils.release();
    }

    @Override
    public void notify(Subject subject, int nType) {
        Log.d(TAG, String.format("SplashActivity notify %d", nType));
        if (Observer.mExitType == nType) {
            mUpdateUtils.release();
        }
    }
}
