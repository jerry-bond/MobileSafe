package cn.jet.mobilesafe.AdvancedTools;

import java.io.IOException;

import org.xmlpull.v1.XmlPullParserException;

import cn.jet.mobilesafe.R;
import cn.jet.mobilesafe.AdvancedTools.utils.SmsRestoreUtils;
import cn.jet.mobilesafe.AdvancedTools.utils.UIUtils;
import cn.jet.mobilesafe.AdvancedTools.utils.SmsRestoreUtils.SmsRestoreCallBack;
import cn.jet.mobilesafe.AdvancedTools.widget.MyCircleProgress;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
/**短信还原**/
public class SMSRestoreActivity extends Activity implements OnClickListener{

	private MyCircleProgress mProgressButton;
	private boolean flag = false;
	private int restoreStatus = RestoreStatus.START_RESTORE;
	private SmsRestoreUtils smsRestoreUtils;

	private static final int MSG_RESTORE_OK = 1;
	private static final int MSG_RESTORE_FAIL = 2;

	private Handler handler = new Handler(){
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
				case MSG_RESTORE_OK: {
					mProgressButton.setText("还原成功");
					restoreStatus = RestoreStatus.DONE_RESTORE;
					break;
				}
				case MSG_RESTORE_FAIL: {
					mProgressButton.setText("短信还原中断");
					restoreStatus = RestoreStatus.DONE_RESTORE;
					break;
				}
			}
		};
	};
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_reducition);
		initView();
		smsRestoreUtils = new SmsRestoreUtils();
	}

	private void initView() {
		findViewById(R.id.rl_titlebar).setBackgroundColor(
				getResources().getColor(R.color.bright_red));
		ImageView mLeftImgv = (ImageView) findViewById(R.id.imgv_leftbtn);
		((TextView) findViewById(R.id.tv_title)).setText("短信还原");
		mLeftImgv.setOnClickListener(this);
		mLeftImgv.setImageResource(R.drawable.back);
		
		mProgressButton = (MyCircleProgress) findViewById(R.id.mcp_reducition);
		mProgressButton.setOnClickListener(this);
	}
	
	@Override
	protected void onDestroy() {
		flag = false;
		smsRestoreUtils.setFlag(flag);
		super.onDestroy();
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.imgv_leftbtn:
			finish();
			break;
		case R.id.mcp_reducition:
			switch(restoreStatus) {
				case RestoreStatus.START_RESTORE: {
					restoreStatus = RestoreStatus.BEING_RESTORE;
					flag = true;
					mProgressButton.setText("取消还原");
					break;
				}
				case RestoreStatus.BEING_RESTORE: {
					flag = false;
					break;
				}
				case RestoreStatus.DONE_RESTORE: {
					flag = false;
					mProgressButton.setProcess(0);
					mProgressButton.setText("一键还原");
					restoreStatus = RestoreStatus.START_RESTORE;
					return;
				}
			}

			smsRestoreUtils.setFlag(flag);
			new Thread(){
				public void run() {
					try {
						boolean restoreSmsResult = smsRestoreUtils.restoreSms(SMSRestoreActivity.this, new SmsRestoreCallBack() {
							@Override
							public void onSmsRestore(int process) {
								mProgressButton.setProcess(process);
							}

							@Override
							public void beforeSmsRestore(int size) {
								mProgressButton.setMax(size);
							}
						});
						if(restoreSmsResult){
							handler.sendEmptyMessage(MSG_RESTORE_OK);
						}else{
							handler.sendEmptyMessage(MSG_RESTORE_FAIL);
						}
					} catch (XmlPullParserException e) {
						e.printStackTrace();
						UIUtils.showToast(SMSRestoreActivity.this, "文件格式错误");
					} catch (IOException e) {
						e.printStackTrace();
						UIUtils.showToast(SMSRestoreActivity.this, "读写错误");
					}
				}	
			}.start();
			break;
		}
	}

	class RestoreStatus {
		static final int START_RESTORE = 1;
		static final int BEING_RESTORE = 2;
		static final int DONE_RESTORE = 3;
	}
}
