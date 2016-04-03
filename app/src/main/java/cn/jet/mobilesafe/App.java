package cn.jet.mobilesafe;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.app.Activity;
import android.os.Bundle;
import java.lang.ref.WeakReference;

import cn.jet.mobilesafe.Subject;
import cn.jet.mobilesafe.ApplicationSubject;


public class App extends Application {
	private final static String TAG = "App";
	private static ApplicationSubject applicationSubject;

	public ApplicationSubject getApplicationSubject() {
		if(null == applicationSubject) {
			applicationSubject = new ApplicationSubject();
		}
		return applicationSubject;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
			@Override
			public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
				Log.i(TAG, "onActivityCreated " + activity.toString());

			}

			@Override
			public void onActivityStarted(Activity activity) {
				//Log.i(TAG, "onActivityStarted " + activity.toString());
			}

			@Override
			public void onActivityResumed(Activity activity) {
				//Log.i(TAG, "onActivityResumed "  + activity.toString());
				MyActivityManager.getInstance().setCurrentActivity(activity);
			}

			@Override
			public void onActivityPaused(Activity activity) {
				//Log.i(TAG, "onActivityPaused " + activity.toString());
			}

			@Override
			public void onActivityStopped(Activity activity) {
				//Log.i(TAG, "onActivityStopped " + activity.toString());
			}

			@Override
			public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
				//Log.i(TAG, "onActivitySaveInstanceState " + activity.toString());
			}

			@Override
			public void onActivityDestroyed(Activity activity) {
				Log.i(TAG, "onActivityDestroyed " + activity.toString());
			}
		});
		correctSIM();
	}

	@Override
	public void onTerminate() {
		applicationSubject.exit();
		super.onTerminate();
	}
	public void correctSIM() {
		// 检查sim卡是否发生变化
		SharedPreferences sp = getSharedPreferences("config",
				Context.MODE_PRIVATE);
		// 获取防盗保护的状态
		boolean protecting = sp.getBoolean("protecting", true);
		if (protecting) {
			// 得到绑定的sim卡串号
			String bindsim = sp.getString("sim", "");
			// 得到手机现在的sim卡串号
			TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
			// 为了测试在手机序列号上dafa 已模拟SIM卡被更换的情况
			String realsim = tm.getSimSerialNumber();
			if (bindsim.equals(realsim)) {
				Log.i("", "sim卡未发生变化，还是您的手机");
			} else {
				Log.i("", "SIM卡变化了");
				// 由于系统版本的原因，这里的发短信可能与其他手机版本不兼容
				String safenumber = sp.getString("safephone", "");
				if (!TextUtils.isEmpty(safenumber)) {
					SmsManager smsManager = SmsManager.getDefault();
					smsManager.sendTextMessage(safenumber, null,
							"你的亲友手机的SIM卡已经被更换！", null, null);
				}
			}
		}
	}
	public static class MyActivityManager {
		private static MyActivityManager mInstance = new MyActivityManager();
		private WeakReference<Activity> mCurrentActivityWeakRef;


		private MyActivityManager() {

		}

		public static MyActivityManager getInstance() {
			return mInstance;
		}

		public Activity getCurrentActivity() {
			Activity currentActivity = null;
			if (mCurrentActivityWeakRef != null) {
				currentActivity = mCurrentActivityWeakRef.get();
			}
			return currentActivity;
		}

		public void setCurrentActivity(Activity activity) {
			mCurrentActivityWeakRef = new WeakReference<Activity>(activity);
		}
	}
}
