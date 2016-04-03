package cn.jet.mobilesafe.AdvancedTools;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import cn.jet.mobilesafe.R;
import cn.jet.mobilesafe.AdvancedTools.utils.SmsBackUpUtils;
import cn.jet.mobilesafe.AdvancedTools.utils.UIUtils;
import cn.jet.mobilesafe.AdvancedTools.utils.SmsBackUpUtils.BackupStatusCallback;
import cn.jet.mobilesafe.AdvancedTools.widget.MyCircleProgress;
/**短信备份**/
public class SMSBackupActivity extends Activity implements OnClickListener{

	private MyCircleProgress mProgressButton;
	/**标识符，用来标识备份状态的*/
	private boolean flag = false;
	private int status = BackupStatus.START_BACKUP;
	private SmsBackUpUtils smsBackUpUtils;
	private static final int CHANGE_BUTTON_TEXT = 100;
	private static final int BACKUPOK_BUTTON_TEXT = 101;
	private static final int BACKUPFAIL_BUTTON_TEXT = 102;
	private Handler handler = new Handler(){
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
				case CHANGE_BUTTON_TEXT: {
					flag = false;
					mProgressButton.setText("一键备份");
					break;
				}
				case BACKUPOK_BUTTON_TEXT: {
					mProgressButton.setText("备份成功");
					status = BackupStatus.DONE_BACKUP;
					break;
				}
				case BACKUPFAIL_BUTTON_TEXT: {
					mProgressButton.setText("备份失败");
					status = BackupStatus.DONE_BACKUP;
					break;
				}
			}
		};
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_smsbackup);
		smsBackUpUtils = new SmsBackUpUtils();
		initView();
	}

	private void initView() {
		findViewById(R.id.rl_titlebar).setBackgroundColor(
				getResources().getColor(R.color.bright_red));
		ImageView mLeftImgv = (ImageView) findViewById(R.id.imgv_leftbtn);
		((TextView) findViewById(R.id.tv_title)).setText("短信备份");
		mLeftImgv.setOnClickListener(this);
		mLeftImgv.setImageResource(R.drawable.back);
		
		mProgressButton = (MyCircleProgress) findViewById(R.id.mcp_smsbackup);
		mProgressButton.setOnClickListener(this);
	}

	
	@Override
	protected void onDestroy() {
		flag = false;
		smsBackUpUtils.setFlag(flag);
		super.onDestroy();
	}
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.imgv_leftbtn:
			finish();
			break;
		case R.id.mcp_smsbackup:
			switch(status) {
				case BackupStatus.START_BACKUP: {
					status = BackupStatus.BEING_BACKUP;
					flag = true;
					mProgressButton.setText("取消备份");
					break;
				}
				case BackupStatus.BEING_BACKUP: {
					flag = false;
					break;
				}
				case BackupStatus.DONE_BACKUP: {
					flag = false;
					mProgressButton.setProcess(0);
					mProgressButton.setText("一键备份");
					status = BackupStatus.START_BACKUP;
					return;
				}
			}

			smsBackUpUtils.setFlag(flag);
			new Thread(){
				Date mLastDate = null;
				//SimpleDateFormat mSDF =  new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				Calendar mCalendar = Calendar.getInstance();
				int mTotalSize = 0;
				public void run() {
					try {
						boolean backUpSms = smsBackUpUtils.backUpSms(SMSBackupActivity.this, new BackupStatusCallback() {
							@Override
							public void onSmsBackup(int process) {
								try {
									Date nowDate = new Date();
									if (null == mLastDate) {
										mLastDate = nowDate;
										return;
									}
									mCalendar.setTime(mLastDate);
									long lastTime = mCalendar.getTimeInMillis();
									mCalendar.setTime(nowDate);
									long nowTime = mCalendar.getTimeInMillis();
									long betweenTime = nowTime - lastTime;

									if ((betweenTime > 50) || (mTotalSize == process)) {
										mLastDate = nowDate;
										mProgressButton.setProcess(process);
									}
								} catch (Exception e) {
									e.printStackTrace();
								}
							}
							
							@Override
							public void beforeSmsBackup(int size) {
								if(size <= 0){
									flag = false;
									smsBackUpUtils.setFlag(flag);
									UIUtils.showToast(SMSBackupActivity.this, "您还没有短信！");
									handler.sendEmptyMessage(CHANGE_BUTTON_TEXT);
								}else{
									mProgressButton.setMax(size);
									mTotalSize = size;
								}
							}
						});
						if(backUpSms){
							handler.sendEmptyMessage(BACKUPOK_BUTTON_TEXT);
							UIUtils.showToast(SMSBackupActivity.this, "备份成功");
						}else{
							handler.sendEmptyMessage(BACKUPFAIL_BUTTON_TEXT);
							UIUtils.showToast(SMSBackupActivity.this, "备份失败");
						}

					} catch (FileNotFoundException e) {
						e.printStackTrace();
						UIUtils.showToast(SMSBackupActivity.this, "文件生成失败");
					} catch (IllegalStateException e) {
						e.printStackTrace();
						UIUtils.showToast(SMSBackupActivity.this, "SD卡不可用或SD卡内存不足");
					} catch (IOException e) {
						e.printStackTrace();
						UIUtils.showToast(SMSBackupActivity.this, "读写错误");
					}
				};
			}.start();
			break;
		}
	}

	class BackupStatus {
		static final int START_BACKUP = 1;
		static final int BEING_BACKUP = 2;
		static final int DONE_BACKUP = 3;
	}
}
