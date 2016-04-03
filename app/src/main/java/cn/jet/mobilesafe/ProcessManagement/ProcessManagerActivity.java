package cn.jet.mobilesafe.ProcessManagement;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.Formatter;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import cn.jet.mobilesafe.R;
import cn.jet.mobilesafe.ProcessManagement.adapter.ProcessManagerAdapter;
import cn.jet.mobilesafe.ProcessManagement.entity.TaskInfo;
import cn.jet.mobilesafe.ProcessManagement.utils.SystemInfoUtils;
import cn.jet.mobilesafe.ProcessManagement.utils.TaskInfoParser;

public class ProcessManagerActivity extends Activity implements OnClickListener {

	private TextView mRunProcessNumTV;
	private TextView mMemoryTV;
	private TextView mProcessNumTV;
	private ListView mListView;
	ProcessManagerAdapter mAdapter;
	private List<TaskInfo> mRunningTaskInfos;
	private List<TaskInfo> mUserTaskInfos = new ArrayList<TaskInfo>();
	private List<TaskInfo> mSysTaskInfo = new ArrayList<TaskInfo>();
	private ActivityManager mManager;
	private int mRunningPocessCount; //以包名区分
	private long mTotalMem;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_processmanager);
		initView();
		fillData();
	}
	
	@Override
	protected void onResume() {
		if (mAdapter != null) {
			mAdapter.notifyDataSetChanged();
		}
		super.onResume();
	}

	private void initView() {
		findViewById(R.id.rl_titlebar).setBackgroundColor(
				getResources().getColor(R.color.bright_green));
		ImageView mLeftImgv = (ImageView) findViewById(R.id.imgv_leftbtn);
		mLeftImgv.setOnClickListener(this);
		mLeftImgv.setImageResource(R.drawable.back);
		ImageView mRightImgv = (ImageView) findViewById(R.id.imgv_rightbtn);
		mRightImgv.setImageResource(R.drawable.processmanager_setting_icon);
		mRightImgv.setOnClickListener(this);
		((TextView) findViewById(R.id.tv_title)).setText("进程管理");
		mRunProcessNumTV = (TextView) findViewById(R.id.tv_runningprocess_num);
		mMemoryTV = (TextView) findViewById(R.id.tv_memory_processmanager);
		mProcessNumTV = (TextView) findViewById(R.id.tv_user_runningprocess);
		mRunningPocessCount = 0;
		mRunProcessNumTV.setText("运行中的进程： " + mRunningPocessCount + "个");
		long totalAvailMem = SystemInfoUtils.getAvailMem(this);
		mTotalMem = SystemInfoUtils.getTotalMem();
		mMemoryTV.setText("可用/总内存："
				+ Formatter.formatFileSize(this, totalAvailMem) + "/"
				+ Formatter.formatFileSize(this, mTotalMem));
		mListView = (ListView) findViewById(R.id.lv_runningapps);
		initListener();
		SystemInfoUtils.EnableUsageStatus(this);
	}

	private void initListener() {
		findViewById(R.id.btn_selectall).setOnClickListener(this);
		findViewById(R.id.btn_select_inverse).setOnClickListener(this);
		findViewById(R.id.btn_cleanprocess).setOnClickListener(this);
		mListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
			                        int position, long id) {
				Object object = mListView.getItemAtPosition(position);
				if (object != null & object instanceof TaskInfo) {
					TaskInfo info = (TaskInfo) object;
					if (info.packageName.equals(getPackageName())) {
						// 当前点击的条目是本应用程序
						return;
					}
					info.isChecked = !info.isChecked;
					//ToDo:显示详细信息界面

					mAdapter.notifyDataSetChanged();
				}
			}
		});
		mListView.setOnScrollListener(new OnScrollListener() {

			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
			}

			@Override
			public void onScroll(AbsListView view, int firstVisibleItem,
			                     int visibleItemCount, int totalItemCount) {
				if (firstVisibleItem >= mUserTaskInfos.size() + 1) {
					mProcessNumTV.setText("系统进程：" + mSysTaskInfo.size() + "个");
				} else {
					mProcessNumTV.setText("用户进程： " + mUserTaskInfos.size() + "个");
				}
			}
		});
	}

	private void fillData() {
		mUserTaskInfos.clear();
		mSysTaskInfo.clear();
		new Thread() {
			public void run() {
				mRunningTaskInfos = TaskInfoParser
						.getRunningTaskInfos(getApplicationContext());

				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						mRunningPocessCount = mRunningTaskInfos.size();
						mRunProcessNumTV.setText("运行中的进程： " + mRunningPocessCount + "个");
						for (TaskInfo taskInfo : mRunningTaskInfos) {
							if (taskInfo.isUserApp) {
								mUserTaskInfos.add(taskInfo);
							} else {
								mSysTaskInfo.add(taskInfo);
							}
						}
						if (mAdapter == null) {
							mAdapter = new ProcessManagerAdapter(
									getApplicationContext(), mUserTaskInfos,
									mSysTaskInfo);
							mListView.setAdapter(mAdapter);
						} else {
							mAdapter.notifyDataSetChanged();
						}
						if (mUserTaskInfos.size() > 0){
							mProcessNumTV.setText("用户进程： "
									+ mUserTaskInfos.size() + "个");
						}else{mProcessNumTV.setText("系统进程：" + mSysTaskInfo.size()
									+ "个");
						}
					}
				});
			};
		}.start();
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.imgv_leftbtn:
			finish();
			break;
		case R.id.imgv_rightbtn:
			//跳转至 进程管理设置页面
			startActivity(new Intent(this,ProcessManagerSettingActivity.class));
		break;
		case R.id.btn_selectall:
			selectAll();
			break;
		case R.id.btn_select_inverse:
			inverse();
			break;
		case R.id.btn_cleanprocess:
			cleanProcess();
			break;
		}
	}

	/**清理进程*/
	private void cleanProcess() {
		mManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
		int count=0;
		long saveMemory = 0;
		List<TaskInfo> killedtaskInfos = new ArrayList<TaskInfo>();

		//注意，遍历集合时不能改变集合大小
		for(TaskInfo info : mUserTaskInfos){
			if(info.isChecked){
				count++;
				saveMemory += info.appMemory;
				//会重启
				mManager.killBackgroundProcesses(info.packageName);
				SystemInfoUtils.stopApp(this, info.packageName);
				killedtaskInfos.add(info);
			}
		}
		for(TaskInfo info : mSysTaskInfo){
			if(info.isChecked){
				count++;
				saveMemory += info.appMemory;
				mManager.killBackgroundProcesses(info.packageName);
				SystemInfoUtils.stopApp(this, info.packageName);
				killedtaskInfos.add(info);
			}
		}

		for(TaskInfo info : killedtaskInfos){
			if(info.isUserApp){
				mUserTaskInfos.remove(info);
			}
			else{
				mSysTaskInfo.remove(info);
			}
			
		}
		mRunningPocessCount -=count;
		mRunProcessNumTV.setText("运行中的进程：" + mRunningPocessCount + "个");
		mMemoryTV.setText("可用/总内存："
				+ Formatter.formatFileSize(this, SystemInfoUtils.getAvailMem(this)) + "/"
				+ Formatter.formatFileSize(this, mTotalMem));
		Toast.makeText(this, "清理了" + count + "个进程,释放了"
				+ Formatter.formatFileSize(this, saveMemory) + "内存", 1).show();
		mProcessNumTV.setText("用户进程："+ mUserTaskInfos.size()+"个");
		mAdapter.notifyDataSetChanged();
	}

	/** 反选 */
	private void inverse() {
		for (TaskInfo taskInfo : mUserTaskInfos) {
			// 就是本应用程序
			if (taskInfo.packageName.equals(getPackageName())) {
				continue;
			}
			boolean checked = taskInfo.isChecked;
			taskInfo.isChecked = !checked;
		}
		for (TaskInfo taskInfo : mSysTaskInfo) {
			if (taskInfo.packageName.equals(getPackageName())) {
				continue;
			}
			boolean checked = taskInfo.isChecked;
			taskInfo.isChecked = !checked;
		}
		mAdapter.notifyDataSetChanged();
	}

	/** 全选 */
	private void selectAll() {
		for (TaskInfo taskInfo : mUserTaskInfos) {
			// 就是本应用程序
			if (taskInfo.packageName.equals(getPackageName())) {
				continue;
			}
			taskInfo.isChecked = true;
		}
		for (TaskInfo taskInfo : mSysTaskInfo) {
			taskInfo.isChecked = true;
		}
		mAdapter.notifyDataSetChanged();
	}

}
