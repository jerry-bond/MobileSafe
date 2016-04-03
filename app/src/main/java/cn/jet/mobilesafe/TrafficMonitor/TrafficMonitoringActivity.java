package cn.jet.mobilesafe.TrafficMonitor;

import java.text.SimpleDateFormat;
import java.util.Date;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import cn.jet.mobilesafe.R;
import cn.jet.mobilesafe.TrafficMonitor.db.dao.TrafficDao;
import cn.jet.mobilesafe.TrafficMonitor.service.TrafficMonitoringService;
import cn.jet.mobilesafe.TrafficMonitor.utils.SystemInfoUtils;

public class TrafficMonitoringActivity extends Activity implements
		OnClickListener {
	private SharedPreferences mSP;
	private Button mCorrectFlowBtn;
	private TextView mTotalTV;
	private TextView mUsedTV;
	private TextView mToDayTV;
	private TrafficDao dao;
	private ImageView mRemindIMGV;
	private TextView mRemindTV;
	private CorrectFlowReceiver receiver;

	private static final String ChinaTelecom = "10001";
	private static final String ChinaUnicom = "10010";
	private static final String ChinaMobile = "10086";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_trafficmonitoring);
		mSP = getSharedPreferences("config", MODE_PRIVATE);
		boolean flag = mSP.getBoolean("isset_operator", false);
		// 如果没有设置运营商信息则进入信息设置页面
		if (!flag) {
			startActivity(new Intent(this, OperatorSetActivity.class));
			finish();
		}
		if (!SystemInfoUtils
				.isServiceRunning(this,
						"cn.jet.mobilesafe.TrafficMonitor.service.TrafficMonitoringService")) {
			startService(new Intent(this, TrafficMonitoringService.class));
		}
		initView();
		registReceiver();
		initData();
	}

	private void initView() {
		findViewById(R.id.rl_titlebar).setBackgroundColor(
				getResources().getColor(R.color.light_green));
		ImageView mLeftImgv = (ImageView) findViewById(R.id.imgv_leftbtn);
		((TextView) findViewById(R.id.tv_title)).setText("流量监控");
		mLeftImgv.setOnClickListener(this);
		mLeftImgv.setImageResource(R.drawable.back);
		mCorrectFlowBtn = (Button) findViewById(R.id.btn_correction_flow);
		mCorrectFlowBtn.setOnClickListener(this);
		mTotalTV = (TextView) findViewById(R.id.tv_month_totalgprs);
		mUsedTV = (TextView) findViewById(R.id.tv_month_usedgprs);
		mToDayTV = (TextView) findViewById(R.id.tv_today_gprs);
		mRemindIMGV = (ImageView) findViewById(R.id.imgv_traffic_remind);
		mRemindTV = (TextView) findViewById(R.id.tv_traffic_remind);
	}

	private void initData() {
		long totalflow = mSP.getLong("totalflow", 0);
		long usedflow = mSP.getLong("usedflow", 0);
		if (totalflow > 0 & usedflow >= 0) {
			float scale = usedflow / totalflow;
			if (scale > 0.9) {
				mRemindIMGV.setEnabled(false);
				mRemindTV.setText("您的套餐流量即将用完！");
			} else {
				mRemindIMGV.setEnabled(true);
				mRemindTV.setText("本月流量充足请放心使用");
			}
		}
		mTotalTV.setText("本月流量：" + Formatter.formatFileSize(this, totalflow));
		mUsedTV.setText("本月已用：" + Formatter.formatFileSize(this, usedflow));
		dao = new TrafficDao(this);
		Date date = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		String dataString = sdf.format(date);
		long moblieGPRS = dao.getMoblieGPRS(dataString);
		if (moblieGPRS < 0) {
			moblieGPRS = 0;
		}
		mToDayTV.setText("本日已用：" + Formatter.formatFileSize(this, moblieGPRS));
	}

	private void registReceiver() {
		receiver = new CorrectFlowReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction("android.provider.Telephony.SMS_RECEIVED");
		registerReceiver(receiver, filter);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.imgv_leftbtn:
			finish();
			break;
		case R.id.btn_correction_flow:
			// 首先判断是哪个运营商，
			int i = mSP.getInt("operator", 0);
			SmsManager smsManager = SmsManager.getDefault();
			switch (i) {
			case 0:
				// 没有设置运营商
				Toast.makeText(this, "您还没有设置运营商信息", 0).show();
				break;
			case 1:
				// 中国移动
				smsManager.sendTextMessage(ChinaMobile, null, "CXGLL", null, null);
				break;
			case 2:
				// 中国联通
				// 发送LLCX至10010
				// 获取系统默认的短信管理器
				smsManager.sendTextMessage(ChinaUnicom, null, "LLCX", null, null);
				break;
			case 3:
				// 中国电信
				smsManager.sendTextMessage(ChinaTelecom, null, "108", null, null);
				break;
			}
		}
	}

	public class FlowData {
		long mLeft = 0;// 本月剩余流量
		long mUsed = 0;// 本月已用流量
		long mBeyond = 0;// 本月超出流量
	}
	class CorrectFlowReceiver extends BroadcastReceiver {
		private final String TAG = "CorrectFlowReceiver";

		@Override
		public void onReceive(Context context, Intent intent) {
			Object[] objs = (Object[]) intent.getExtras().get("pdus");
			FlowData flowData  = new FlowData();
			for (Object obj : objs) {
				SmsMessage smsMessage = SmsMessage.createFromPdu((byte[]) obj);
				String body = smsMessage.getMessageBody();
				String address = smsMessage.getOriginatingAddress();

				// 以下短信分割只针对联通3G用户
				if (address.equals(ChinaUnicom)) {
					parseChinaUnicom(body,flowData);
				} else if(address.equals(ChinaMobile)) {
					parseChinaMobile(body, flowData);
				} else if(address.equals(ChinaTelecom)) {
					parseChinaTelecom(body, flowData);
				} else {
					Log.i(TAG, "Unknown number type");
				}
			}

			Editor edit = mSP.edit();
			edit.putLong("totalflow", flowData.mUsed + flowData.mLeft);
			edit.putLong("usedflow", flowData.mUsed + flowData.mBeyond);
			edit.commit();
			mTotalTV.setText("本月流量："
					+ Formatter.formatFileSize(context, (flowData.mUsed + flowData.mLeft)));
			mUsedTV.setText("本月已用："
					+ Formatter.formatFileSize(context, (flowData.mUsed + flowData.mBeyond)));

		}
	}

	private boolean parseChinaUnicom(String body, FlowData flowData) {
		String[] split = body.split("，");

		for (int i = 0; i < split.length; i++) {
			if (split[i].contains("本月流量已使用")) {
				// 套餐总量
				String usedflow = split[i].substring(7,
						split[i].length());
				flowData.mUsed = getStringTofloat(usedflow);
			} else if (split[i].contains("剩余流量")) {
				String leftflow = split[i].substring(4,
						split[i].length());
				flowData.mLeft = getStringTofloat(leftflow);
			} else if (split[i].contains("套餐外流量")) {
				String beyondflow = split[i].substring(5,
						split[i].length());
				flowData.mBeyond = getStringTofloat(beyondflow);
			}
		}
		return true;
	}

	private boolean parseChinaTelecom(String body, FlowData flowData) {
		String[] split = body.split("，");
		for (int i = 0; i < split.length; i++) {
		}
		return true;
	}

	private boolean parseChinaMobile(String body, FlowData flowData) {
		String[] split = body.split("，");
		int nIndex = 0;
		for (int i = 0; i < split.length; i++) {
			if (split[i].contains("您已使用数据流量")) {
				// 套餐总量
				nIndex = split[i].indexOf("您已使用数据流量");
				nIndex += "您已使用数据流量".length();
				String usedflow = split[i].substring(nIndex,
						split[i].length());
				flowData.mUsed = getStringTofloat(usedflow);
			} else if (split[i].contains("剩余流量")) {
				nIndex = split[i].indexOf("剩余流量");
				nIndex += "剩余流量".length();
				String leftflow = split[i].substring(nIndex,
						split[i].length());
				flowData.mLeft = getStringTofloat(leftflow);
			} else if (split[i].contains("套餐外流量")) {
				nIndex = split[i].indexOf("套餐外流量");
				nIndex += "套餐外流量".length();
				String beyondflow = split[i].substring(nIndex,
						split[i].length());
				flowData.mBeyond = getStringTofloat(beyondflow);
			}
		}
		return true;
	}

	/** 将字符串转化成Float类型数据 **/
	private long getStringTofloat(String str) {
		long flow = 0;
		if (!TextUtils.isEmpty(str)) {
			if (str.contains("KB")) {
				String[] split = str.split("KB");
				float m = Float.parseFloat(split[0]);
				flow = (long) (m * 1024);
			} else if (str.contains("MB")) {
				String[] split = str.split("MB");
				float m = Float.parseFloat(split[0]);
				flow = (long) (m * 1024 * 1024);
			} else if (str.contains("GB")) {
				String[] split = str.split("GB");
				float m = Float.parseFloat(split[0]);
				flow = (long) (m * 1024 * 1024 * 1024);
			}
		}
		return flow;
	}

	@Override
	public void onDestroy() {
		if (receiver != null) {
			unregisterReceiver(receiver);
			receiver = null;
		}
		super.onDestroy();
	}
}
