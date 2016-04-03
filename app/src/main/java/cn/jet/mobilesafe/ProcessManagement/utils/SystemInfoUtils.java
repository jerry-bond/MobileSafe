package cn.jet.mobilesafe.ProcessManagement.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.List;
import java.util.ArrayList;
import java.util.Hashtable;

import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import java.lang.reflect.Method;

import android.content.ComponentName;


public class SystemInfoUtils {
	private static final SimpleDateFormat dateFormat = new SimpleDateFormat("M-d-yyyy HH:mm:ss");
	public static final String TAG = SystemInfoUtils.class.getSimpleName();
	/**
	 * 判断一个服务是否处于运行状态
	 *
	 * @param context 上下文
	 * @return
	 */
	public static boolean isServiceRunning(Context context, String className) {
		ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		List<RunningServiceInfo> infos = am.getRunningServices(200);
		for (RunningServiceInfo info : infos) {
			String serviceClassName = info.service.getClassName();
			if (className.equals(serviceClassName)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 获取手机的总内存大小 单位byte
	 *
	 * @return
	 */
	public static long getTotalMem() {
		try {
			FileInputStream fis = new FileInputStream(new File("/proc/meminfo"));
			BufferedReader br = new BufferedReader(new InputStreamReader(fis));
			String totalInfo = br.readLine();
			//MemTotal:         513000 kB
			StringBuffer sb = new StringBuffer();
			for (char c : totalInfo.toCharArray()) {
				if (c >= '0' && c <= '9') {
					sb.append(c);
				}
			}
			long bytesize = Long.parseLong(sb.toString()) * 1024;
			return bytesize;
		} catch (Exception e) {
			e.printStackTrace();
			return 0;
		}
	}

	/**
	 * 获取可用的内存信息。
	 *
	 * @param context
	 * @return
	 */
	public static long getAvailMem(Context context) {
		ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		//获取内存大小
		MemoryInfo outInfo = new ActivityManager.MemoryInfo();
		am.getMemoryInfo(outInfo);
		long availMem = outInfo.availMem;
		return availMem;
	}

	/**
	 * 得到正在运行的进程的数量
	 *
	 * @param context
	 * @return
	 */
	public static int getRunningPocessCount(Context context) {
		Hashtable<String, List<RunningServiceInfo>> htServices
				= new Hashtable<String, List<RunningServiceInfo>>();
		int nCount = getRunningPocessByService(context, htServices);
		htServices.clear();
		htServices = null;
		return nCount;
	}

	/**
	 * 得到正在运行的进程
	 * @param context
	 * @return
	 */
	public static int getRunningPocessByService(Context context,
	                                            Hashtable<String, List<RunningServiceInfo>> htServices){
		int count = 0;

		ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		List<RunningServiceInfo> svrList = am.getRunningServices(Integer.MAX_VALUE);

		for (RunningServiceInfo rsi : svrList) {
			String pkgName = rsi.service.getPackageName();
			if (htServices.get(pkgName) == null) {
				List<RunningServiceInfo> list = new ArrayList<RunningServiceInfo>();
				list.add(rsi);
				htServices.put(pkgName, list);
				count++;
			} else {
				htServices.get(pkgName).add(rsi);
			}
		}

		return count;
	}
	
	// 强行停止一个app 停止服务
	public static boolean stopApp(Context context, String pkgname) {
		boolean flag = false;
		ActivityManager am = (ActivityManager)
				context.getSystemService(Context.ACTIVITY_SERVICE);
		try {
			Method forceStopPackage;
			// 反射得到隐藏方法(hide)
			forceStopPackage = am.getClass().getDeclaredMethod("forceStopPackage",
					String.class);
			//获取私有成员变量的值
			forceStopPackage.setAccessible(true);
			forceStopPackage.invoke(am, pkgname);
			flag = true;
		} catch (Exception e) {
			Log.d(TAG, "Kill " + pkgname + " failed " + e.toString());
			e.printStackTrace();
		}
		return flag;
	}

	//只返回一个值
	public static List<String> getRunningPackages(Context context) {
		List<String> activePackages = null;
		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT_WATCH) {
			activePackages = getActivePackages(context);
		} else {
			activePackages = getActivePackagesCompat(context);
		}
		return activePackages;
	}


	//只返回一个值com.android.launcher3
	 public static List<String> getActivePackagesCompat(Context context) {
		 ActivityManager am = (ActivityManager)
				context.getSystemService(Context.ACTIVITY_SERVICE);
		 final List<ActivityManager.RunningTaskInfo> taskInfos = am.getRunningTasks(1);
		 List<String> activePackages = new ArrayList<String>();
		 for (ActivityManager.RunningTaskInfo info : taskInfos) {
			 ComponentName componentName = info.topActivity;
			 activePackages.add(componentName.getPackageName());
		 }

		return activePackages;
	}

	//只返回一个值cn.jet.mobilesafe
	public static List<String> getActivePackages(Context context) {
		ActivityManager am = (ActivityManager)
				context.getSystemService(Context.ACTIVITY_SERVICE);
		List<String> activePackages = new ArrayList<String>();
		final List<ActivityManager.RunningAppProcessInfo> processInfos = am.getRunningAppProcesses();
		for (ActivityManager.RunningAppProcessInfo processInfo : processInfos) {
			activePackages.addAll(Arrays.asList(processInfo.pkgList));
		}
		return activePackages;
	}

	//
	public static List<String> getRunningApp(Context context) {
		//android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
		List<String> activePackages = new ArrayList<String>();

		UsageStatsManager usm =
					(UsageStatsManager)context.getSystemService("usagestats");//Context.USAGE_STATS_SERVICE
		long time = System.currentTimeMillis();
		List<UsageStats> appList = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY,
				time - 1000 * 1000 * 60, time);
		for (UsageStats use : appList) {
			activePackages.add(use.getPackageName());
		}

		List<String> eventPackages = new ArrayList<String>();
		UsageEvents queryEvents = usm.queryEvents(System.currentTimeMillis() - 10000 * 60,
				System.currentTimeMillis());

		if (queryEvents != null) {
			UsageEvents.Event event = new UsageEvents.Event();
			while (queryEvents.hasNextEvent())	{
				UsageEvents.Event eventAux = new UsageEvents.Event();
				queryEvents.getNextEvent(eventAux);
				eventPackages.add(event.getPackageName());
			}
		}
		return activePackages;
	}

	private static void getStartEndTime(long startTime, long endTime) {

	}

	//ToDo:得到当前正在运行的package
	public static List<UsageStats> getUsageStatsList(Context context){
		List<UsageStats> usgStatsLst = new ArrayList<UsageStats>();
		UsageStatsManager usm
				= (UsageStatsManager) context.getSystemService("usagestats");

		Calendar calendar = Calendar.getInstance();
		long endTime = calendar.getTimeInMillis();
		calendar.add(Calendar.MINUTE, -1);
		long startTime = calendar.getTimeInMillis();
		List<UsageStats> usageStatsList =
				usm.queryUsageStats(UsageStatsManager.INTERVAL_BEST, startTime, endTime);

		Hashtable<String, UsageStats> ut = new Hashtable<String, UsageStats>();
		ActivityManager am = (ActivityManager)
				context.getSystemService(Context.ACTIVITY_SERVICE);
		PackageManager pm = context.getPackageManager();
		for (UsageStats u : usageStatsList){
			String pkgName = u.getPackageName();
			PackageInfo packInfo = null;
			try {
				packInfo = pm.getPackageInfo(pkgName, 0);
				String appname = packInfo.applicationInfo.loadLabel(pm).toString();
				if ((null == appname) || (appname.equals(""))) {
					Log.d(TAG, pkgName + " empty");
				} else {
					ut.put(u.getPackageName(), u);
				}
			} catch (Exception e) {
			}
		}
		Log.i(TAG, "count: " + ut.size());
		Enumeration<UsageStats> e = ut.elements();
		while(e.hasMoreElements()) {
			UsageStats u = e.nextElement();
			usgStatsLst.add(u);
		}
		return usgStatsLst;
	}

	public static void EnableUsageStatus(Context context) {
		//Check if permission enabled
		Calendar calendar = Calendar.getInstance();
		long endTime = calendar.getTimeInMillis();
		calendar.add(Calendar.YEAR, -1);
		long startTime = calendar.getTimeInMillis();
		UsageStatsManager usm = (UsageStatsManager) context.getSystemService("usagestats");
		List<UsageStats> usageStatsList =
				usm.queryUsageStats(UsageStatsManager.INTERVAL_YEARLY, startTime, endTime);

		if(usageStatsList.isEmpty()) {
			Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
			context.startActivity(intent);
		}
	}

}
