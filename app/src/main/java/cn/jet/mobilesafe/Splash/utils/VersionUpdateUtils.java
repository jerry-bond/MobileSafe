package cn.jet.mobilesafe.Splash.utils;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.widget.Toast;
import cn.jet.mobilesafe.HomeActivity;
import cn.jet.mobilesafe.R;
import cn.jet.mobilesafe.Splash.entity.VersionEntity;

import android.content.res.Resources;

import cn.jet.mobilesafe.ProcessManagement.utils.SystemInfoUtils;
import cn.jet.mobilesafe.Splash.services.UpdateService;
import cn.jet.mobilesafe.common.Constants;
import android.util.Log;

/** 更新提醒工具类 */
public class VersionUpdateUtils {
	private static final int MESSAGE_NET_EEOR = 101;
	private static final int MESSAGE_IO_EEOR = 102;
	private static final int MESSAGE_JSON_EEOR = 103;
	private static final int MESSAGE_SHOEW_DIALOG = 104;
	private static final int MESSAGE_ENTERHOME = 105;
	private static final int MESSAGE_UPDATA_SERVICE_STARTED = 106;
	private static final int MESSAGE_NO_UPDATE = 107;
	/** 本地版本号 */
	private String mVersion;
	private Activity mContext;
	private VersionEntity mVersionEntity;
	private ConnectionService mConnectionService = null;
	private final String TAG = "VersionUpdateUtils";

	/** 用于更新UI */
	private Handler handler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case MESSAGE_IO_EEOR:
				Toast.makeText(mContext, "IO异常", 0).show();
				enterHome();
				break;
			case MESSAGE_JSON_EEOR:
				Toast.makeText(mContext, "JSON解析异常", 0).show();
				enterHome();
				break;
			case MESSAGE_NET_EEOR:
				Toast.makeText(mContext, "网络异常", 0).show();
				enterHome();
				break;
			case MESSAGE_SHOEW_DIALOG:
				showUpdateDialog(mVersionEntity);
				break;
			case MESSAGE_ENTERHOME:
				Intent intent = new Intent(mContext,HomeActivity.class);
				mContext.startActivity(intent);
				mContext.finish();
				break;
			case MESSAGE_UPDATA_SERVICE_STARTED:
				enterHome();
				break;
			case MESSAGE_NO_UPDATE:
				enterHome();
				break;
			}
		}
	};
	
	public VersionUpdateUtils(String Version, Activity activity) {
		mVersion = Version;
		mContext = activity;
	}


	public void tryUpdate() {
		if (!isUpdateServiceWorking()) {
			getCloudVersion();
		}
		else {
			enterHome();
		}
	}
	/**
	 * 获取服务器版本号
	 */
	private void getCloudVersion(){
		int StatusCode = MESSAGE_ENTERHOME;

		try {
			//ToDo:单独存在的配置文件，读取、打包
			SharedPreferences msharedPreferences =
					mContext.getSharedPreferences("config", mContext.MODE_PRIVATE);
			String updataUrl = msharedPreferences.getString("updateUrl",
					Constants.SERVER_VERSION_URL);
			HttpClient client = new DefaultHttpClient();
			/*连接超时*/
			HttpConnectionParams.setConnectionTimeout(client.getParams(), 5000);
			/*请求超时*/
			HttpConnectionParams.setSoTimeout(client.getParams(), 5000);
			//ToDo:支持DNS ？
			HttpGet httpGet = new HttpGet(updataUrl);

			HttpResponse execute = client.execute(httpGet);
			if (execute.getStatusLine().getStatusCode() == 200) {
				// 请求和响应都成功了
				HttpEntity entity = execute.getEntity();
				String result = EntityUtils.toString(entity, "utf8");
				// 创建jsonObject对象
				JSONObject jsonObject = new JSONObject(result);
				mVersionEntity = new VersionEntity();
				String code = jsonObject.getString("code");
				mVersionEntity.versioncode = code;
				String des = jsonObject.getString("des");
				mVersionEntity.description = des;
				String apkurl = jsonObject.getString("apkurl");
				mVersionEntity.apkurl = apkurl;
				//ToDo:版本号检查规则
				if (!mVersion.equals(mVersionEntity.versioncode)) {
					// 版本号不一致
					StatusCode = MESSAGE_SHOEW_DIALOG;
				}
			}
		} catch (ClientProtocolException e) {
			StatusCode = MESSAGE_NET_EEOR;
			e.printStackTrace();
		}catch (org.apache.http.conn.HttpHostConnectException e) {
		}
		catch (IOException e) {
			StatusCode = MESSAGE_IO_EEOR;
			e.printStackTrace();
		} catch (JSONException e) {
			StatusCode = MESSAGE_JSON_EEOR;
			e.printStackTrace();
		}
		handler.sendEmptyMessage(StatusCode);
	}

	/**
	 * 弹出更新提示对话框
	 * 
	 * @param versionEntity
	 */
	private void showUpdateDialog(final VersionEntity versionEntity) {
		// 创建dialog
		Resources res = mContext.getResources();
		AlertDialog.Builder builder = new Builder(mContext);
		builder.setTitle(res.getText(R.string.check_new_ver)
				+ versionEntity.versioncode);// 设置标题
		builder.setMessage(versionEntity.description);// 根据服务器返回描述,设置升级描述信息
		builder.setCancelable(false);// 设置不能点击手机返回按钮隐藏对话框
		builder.setIcon(R.drawable.ic_launcher);// 设置对话框图标
		// 设置立即升级按钮点击事件  
		builder.setPositiveButton(res.getText(R.string.update_now),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						//initDownloadNotification();
						//downloadNewApk(versionEntity.apkurl);
						Intent updateIntent = new Intent(mContext,
								UpdateService.class);
						updateIntent.putExtra("apkurl", versionEntity.apkurl);
						mContext.startService(updateIntent);
						/*
						if (null == mConnectionService) {
							mConnectionService = new ConnectionService();
						}
						mContext.bindService(updateIntent, mConnectionService, 1);
						*/
						handler.sendEmptyMessage(MESSAGE_UPDATA_SERVICE_STARTED);
					}
				});
		// 设置暂不升级按钮点击事件
		builder.setNegativeButton(res.getText(R.string.update_no),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						handler.sendEmptyMessage(MESSAGE_NO_UPDATE);
					}
				});
		// 对话框必须调用show方法 否则不显示
		builder.show();
	}

	public void enterHome() {
		//handler.sendEmptyMessageDelayed(MESSAGE_ENTERHOME, 1000);
		handler.sendEmptyMessage(MESSAGE_ENTERHOME);
	}

	public void release() {
		Log.i(TAG, "release");
		if (mConnectionService != null) {
			mContext.unbindService(mConnectionService);
		}
	}


	public boolean isUpdateServiceWorking() {
		String serviceName = mContext.getPackageName() + "."
				+ UpdateService.class.getName();
		return SystemInfoUtils.isServiceRunning(mContext, serviceName);
	}

	class ConnectionService  implements ServiceConnection
	{
		private UpdateService mServiceRef;
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mServiceRef =((UpdateService.UpdateServiceBinder)service).getUpdateService();
			mServiceRef.setMainActivity(mContext);
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			Log.i(TAG, "ConnectionService onServiceDisconnected");
			mServiceRef = null;
		}
	}
}
