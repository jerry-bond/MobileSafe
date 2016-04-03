package cn.jet.mobilesafe.Splash.utils;

import java.io.File;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;

import android.net.Uri;

public class MyUtils {
    private static String TAG = "Install";
	/**
	 * 获取版本号
	 * 
	 * @param context
	 * @return 返回版本号
	 */
	public static String getVersion(Context context) {
		// PackageManager 可以获取清单文件中的所有信息
		PackageManager manager = context.getPackageManager();
		try {
			// 获取到一个应用程序的信息
			// getPackageName() 获取到当前程序的包名
			PackageInfo packageInfo = manager.getPackageInfo(
					context.getPackageName(), 0);
			return packageInfo.versionName;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
			return "";
		}
	}

	/**
	 * 安装新版本
	 * @param activity
	 */
	public static void installApk(Activity activity, String localFile) {

		Intent intent = new Intent(Intent.ACTION_VIEW);
		//intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		// 添加默认分类
		intent.addCategory(Intent.CATEGORY_DEFAULT);
		// 设置数据和类型 在文件中
		intent.setDataAndType(
				Uri.fromFile(new File(localFile)),
				"application/vnd.android.package-archive");
		// 如果开启的activity 退出的时候 会回调当前activity的onActivityResult
		activity.startActivity(intent);
	}

	/*
	public static void installOnBackground(Activity activity, String localFile ){
		File file = new File(localFile);
		Log.i(TAG, "installOnBackground()");
		int installFlags = 0;
		Uri packageUri = Uri.fromFile(file);//file是要安装的apk文件

		PackageManager pm = activity.getPackageManager();
		installFlags |= PackageManager.INSTALL_REPLACE_EXISTING;

		MyPackageInstallObserver observer = new MyPackageInstallObserver();
		pm.installPackage(packageUri, observer, installFlags, "cn.jet.mobilesafe");
	}

	private static class MyPackageInstallObserver extends IPackageInstallObserver.Stub{
		@Override
		public void packageInstalled(String packageName, int returnCode)
				throws RemoteException {
			//returnCode=1表示安装成功
			Log.e(TAG, "packageInstalled()");
			Log.i(TAG, "packageName = " + packageName);
			Log.i(TAG, "returnCode = " + returnCode);
		}
	}
	*/
}
