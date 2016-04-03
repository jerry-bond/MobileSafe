package cn.jet.mobilesafe.TrafficMonitor.service;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import cn.jet.mobilesafe.TrafficMonitor.db.dao.TrafficDao;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.TrafficStats;
import android.os.IBinder;
import android.util.Log;

public class TrafficMonitoringService extends Service {
	private static final String TAG = "TrafficMonitoringService";
	private long mOldRxBytes;
	private long mOldTxBytes;
	private TrafficDao dao;
	private SharedPreferences mSp;
	private long usedFlow;
	boolean flag = true;
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	@Override
	public void onCreate() {
		super.onCreate();
		mOldRxBytes = TrafficStats.getMobileRxBytes();
		mOldTxBytes = TrafficStats.getMobileTxBytes();
		dao = new TrafficDao(this);
		mSp = getSharedPreferences("config", MODE_PRIVATE);
		mThread.start();
	}
	
	private Thread mThread = new Thread() {
		public void run() {
			while (flag) {
				try {
					Thread.sleep(2000 * 60);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				updateTodayGPRS();
			}
		}
		private void updateTodayGPRS() {
			// 获取已经使用了的流量
			usedFlow = mSp.getLong("usedflow", 0);
			Date date = new Date();
			Calendar calendar = Calendar.getInstance(); // 得到日历
			calendar.setTime(date);// 把当前时间赋给日历
			if (calendar.DAY_OF_MONTH == 1 & calendar.HOUR_OF_DAY == 0
					& calendar.MINUTE < 1 & calendar.SECOND < 30) {
				usedFlow = 0;
			}
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			String dataString = sdf.format(date);
			long moblieGPRS = dao.getMoblieGPRS(dataString);
			long mobileRxBytes = TrafficStats.getMobileRxBytes();
			long mobileTxBytes = TrafficStats.getMobileTxBytes();
			// 新产生的流量
			long newGprs = (mobileRxBytes + mobileTxBytes) - mOldRxBytes
					- mOldTxBytes;
			Log.i(TAG, "moblieGPRS " + moblieGPRS + " mobileRxBytes " + mobileRxBytes
					+ " mobileTxBytes " + mobileTxBytes + " newGprs " + newGprs);
			mOldRxBytes = mobileRxBytes;
			mOldTxBytes = mobileTxBytes;
			if (newGprs < 0) {
				// 网络切换过
				newGprs = mobileRxBytes + mobileTxBytes;
				Log.i(TAG, "网络切换过 " + newGprs);
			}
			if (moblieGPRS == -1) {
				Log.i(TAG, "第一次统计 " + newGprs);
				dao.insertTodayGPRS(newGprs);
			} else {
				if (moblieGPRS < 0) {
					moblieGPRS = 0;
				}
				Log.i(TAG, "累计流量: " + (moblieGPRS + newGprs));
				dao.UpdateTodayGPRS(moblieGPRS + newGprs);
			}
			usedFlow = usedFlow + newGprs;
			Editor edit = mSp.edit();
			edit.putLong("usedflow", usedFlow);
			edit.commit();
		};
	};
	
	@Override
	public void onDestroy() {
		if (mThread != null & !mThread.interrupted()) {
			flag = false;
			mThread.interrupt();
			mThread = null;
		}
		super.onDestroy();
	}
}
