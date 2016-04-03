package cn.jet.mobilesafe.AdvancedTools.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import cn.jet.mobilesafe.AdvancedTools.EnterPswActivity;
import cn.jet.mobilesafe.AdvancedTools.db.dao.AppLockDao;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

/***
 * 程序锁服务
 * 
 * @author admin
 */
public class AppLockService extends Service {
	/** 是否开启程序锁服务的标志 */
	private boolean flag = false;
	private AppLockDao dao;
	private Uri uri = Uri.parse("content://cn.jet.mobilesafe.applock");
	private List<String> packagenames;
	private Intent intent;
	private ActivityManager am;
	private List<RunningTaskInfo> taskInfos;
	private RunningTaskInfo taskInfo;
	private String pacagekname;
	private Set<String> tempStopProtectPacks;
	private AppLockReceiver receiver;
	private MyObserver observer;
	private List<String> mBlockingPackages;


	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		// 创建AppLockDao实例
		dao = new AppLockDao(this);
		observer = new MyObserver(new Handler());
		getContentResolver().registerContentObserver(uri, true,
				observer);
		// 获取数据库中的所有包名
		packagenames = dao.findAll();
		receiver = new AppLockReceiver();
		IntentFilter filter = new IntentFilter("cn.jet.mobilesafe.applock");
		filter.addAction(Intent.ACTION_SCREEN_ON);
		filter.addAction(Intent.ACTION_SCREEN_OFF);
		registerReceiver(receiver, filter);
		// 创建Intent实例，用来打开输入密码页面
		intent = new Intent(AppLockService.this, EnterPswActivity.class);
		//服务没有任务栈，如果要开启一个在任务栈中运行的activity，需要为其创建一个任务栈
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		// 获取ActivityManager对象
		am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
		tempStopProtectPacks = Collections.synchronizedSet(new HashSet<String>());
		mBlockingPackages = Collections.synchronizedList(new ArrayList<String>());
		startApplockService();

		super.onCreate();
	}

	/***
	 * 开启监控程序服务
	 */
	private void startApplockService() {

		new Thread() {
			public void run() {
				flag = true;
				while (flag) {
					// 监视任务栈的情况。 最近使用的打开的任务栈在集合的最前面
					taskInfos = am.getRunningTasks(1);
					//获取当前正在栈顶运行的activity
					taskInfo = taskInfos.get(0);
					pacagekname = taskInfo.topActivity.getPackageName();

					intent.putExtra("packagename", pacagekname);
                    // 判断这个包名是否需要被保护。
					// 判断当前应用程序是否需要临时停止保护（输入了正确的密码）
					if (packagenames.contains(pacagekname) &&
							(!tempStopProtectPacks.contains(pacagekname))) {

						// 需要保护
						// 弹出一个输入密码的界面。
						startActivity(intent);
						Log.i("startApplockService", "after startActivity " + pacagekname);
						if (!mBlockingPackages.contains(pacagekname)) {
							mBlockingPackages.add(pacagekname);
						}
					}
					try {
						Thread.sleep(50);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			};
		}.start();
	}

	// 广播接收者
	class AppLockReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if ("cn.jet.mobilesafe.applock".equals(intent.getAction())) {
				String pkgName = intent.getStringExtra("packagename");
				tempStopProtectPacks.add(pkgName);
				mBlockingPackages.remove(pkgName);
			} else if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
				tempStopProtectPacks.clear();
				// 停止监控程序
				flag = false;
			} else if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
				// 开启监控程序
				if (flag == false) {
					startApplockService();
				}
			}
		}
	}

	// 内容观察者
	class MyObserver extends ContentObserver {

		public MyObserver(Handler handler) {
			super(handler);
		}

		@Override
		public void onChange(boolean selfChange) {
			packagenames = dao.findAll();
			super.onChange(selfChange);
		}
	}
	
	@Override
	public void onDestroy() {
		flag = false;
		unregisterReceiver(receiver);
		receiver = null;
		getContentResolver().unregisterContentObserver(observer);
		observer = null;
		super.onDestroy();
	}
}
