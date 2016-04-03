package cn.jet.mobilesafe.ClearCache;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.IPackageStatsObserver;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageStats;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.text.format.Formatter;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.util.Log;
import cn.jet.mobilesafe.R;
import cn.jet.mobilesafe.ClearCache.adapter.CacheCleanAdapter;
import cn.jet.mobilesafe.ClearCache.entity.CacheInfo;

public class CacheClearListActivity extends Activity implements OnClickListener {

	private static final String TAG = "CacheClearListActivity";
	protected static final int SCANNING = 100;
	protected static final int FINISH = 101;
	private AnimationDrawable animation;
	/** 建议清理 */
	private TextView mRecomandTV;
	/** 可清理 */
	private TextView mCanCleanTV;
	private long mCacheMemory;
	private List<CacheInfo> mCacheInfosObserver = new ArrayList<CacheInfo>();
	private List<CacheInfo> mCacheInfosMainThread = new ArrayList<CacheInfo>();
	private PackageManager pm;
	private CacheCleanAdapter adapter;
	private ListView mCacheLV;
	private Button mCacheBtn;
	//调用顺序及线程
	private Handler handler = new Handler(){
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case SCANNING:
				PackageInfo info = (PackageInfo) msg.obj;
				mRecomandTV.setText("正在扫描： "+info.packageName);
				mCanCleanTV.setText("已扫描缓存 ："+Formatter.formatFileSize(CacheClearListActivity.this, mCacheMemory));
				//在主线程添加变化后集合
				mCacheInfosMainThread.clear();
				mCacheInfosMainThread.addAll(mCacheInfosObserver);
				//ListView  刷新
				adapter.notifyDataSetChanged();
				mCacheLV.setSelection(mCacheInfosMainThread.size());
				break;
			case FINISH:
				//扫描完了，动画停止
				animation.stop();

				if(mCacheMemory >0){
					mRecomandTV.setText("扫描完成，请清理");
					mCacheBtn.setEnabled(true);
				}else{
					mCacheBtn.setEnabled(false);
					mRecomandTV.setText("扫描完成，您的手机洁净如新");
				}
				break;
			}
		};
	};
	private Thread mThread;
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_cacheclearlist);
		pm = getPackageManager();
		initView();
	}

	/***
	 * 初始化控件
	 */
	private void initView() {
		findViewById(R.id.rl_titlebar).setBackgroundColor(
				getResources().getColor(R.color.rose_red));
		ImageView mLeftImgv = (ImageView) findViewById(R.id.imgv_leftbtn);
		mLeftImgv.setOnClickListener(this);
		mLeftImgv.setImageResource(R.drawable.back);
		((TextView) findViewById(R.id.tv_title)).setText("缓存扫描");
		mRecomandTV = (TextView) findViewById(R.id.tv_recommend_clean);
		mCanCleanTV = (TextView) findViewById(R.id.tv_can_clean);
		mCacheLV = (ListView) findViewById(R.id.lv_scancache);
		mCacheBtn = (Button) findViewById(R.id.btn_cleanall);
		mCacheBtn.setOnClickListener(this);
		animation = (AnimationDrawable) findViewById(R.id.imgv_broom)
				.getBackground();
		animation.setOneShot(false);
		animation.start();
		adapter = new CacheCleanAdapter(this, mCacheInfosMainThread);
		mCacheLV.setAdapter(adapter);
		fillData();
	}

	/***
	 * 填充数据
	 */
	private void fillData() {
		mThread = new Thread() {
			
			public void run() {
				// 遍历手机里面的所有的应用程序。
				mCacheInfosObserver.clear();
				List<PackageInfo> infos = pm.getInstalledPackages(0);
				for (PackageInfo info : infos) {
					getCacheSize(info);
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					Message msg = Message.obtain();
					msg.obj = info;
					msg.what = SCANNING;
					handler.sendMessage(msg);
				}
				Message msg = Message.obtain();
				msg.what = FINISH;
				handler.sendMessage(msg);
			};
		};
		mThread.start();
	}

	/**
	 * 获取某个包名对应的应用程序的缓存大小
	 * 
	 * @param info
	 *            应用程序的包信息
	 */
	public void getCacheSize(PackageInfo info) {
		try {
			Method method = PackageManager.class.getDeclaredMethod(
					"getPackageSizeInfo", String.class,
					IPackageStatsObserver.class);
			method.invoke(pm, info.packageName, new MyPackObserver(info));
			Log.d(TAG, "getCacheSize after getPackageSizeInfo:" + mCacheMemory
					+ " pid:" + android.os.Process.myPid());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		animation.stop();
		if(mThread != null){
			mThread.interrupt();
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.imgv_leftbtn:
			finish();
			break;
		case R.id.btn_cleanall:
			if(mCacheMemory >0){
				//跳转至清理缓存的页面的Activity
				Intent intent = new Intent(this,CleanCacheActivity.class);
				//将要清理的垃圾大小传递至另一个页面
				intent.putExtra("cacheMemory", mCacheMemory);
				startActivity(intent);
				finish();
			}
			break;
		}
	}

	//oneway IPackageStatsObserver 回调
	private class MyPackObserver extends
			android.content.pm.IPackageStatsObserver.Stub {
		private PackageInfo mPackageInfo;

		public MyPackObserver(PackageInfo info) {
			this.mPackageInfo = info;
		}

		@Override
		public void onGetStatsCompleted(PackageStats pStats, boolean succeeded)
				throws RemoteException {
			// <uses-permission android:name="android.permission.GET_PACKAGE_SIZE" />
			long cachesize = pStats.cacheSize;
			if (cachesize >= 0) {
				CacheInfo cacheInfo = new CacheInfo();
				cacheInfo.cacheSize = cachesize;
				cacheInfo.packagename = mPackageInfo.packageName;
				cacheInfo.appName = mPackageInfo.applicationInfo.loadLabel(pm)
						.toString();
				cacheInfo.appIcon = mPackageInfo.applicationInfo.loadIcon(pm);
				mCacheInfosObserver.add(cacheInfo);
				mCacheMemory += cachesize;
				Log.d(TAG, "onGetStatsCompleted cacheMemory:" + mCacheMemory
						+ " pid:" + android.os.Process.myPid());
			}
		}
	}
}
