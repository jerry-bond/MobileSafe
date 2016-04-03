package cn.jet.mobilesafe.ProcessManagement.utils;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;

import android.app.usage.UsageStats;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.os.Debug.MemoryInfo;
import cn.jet.mobilesafe.R;
import cn.jet.mobilesafe.ProcessManagement.entity.TaskInfo;

/**
 * 任务信息 & 进程信息的解析器
 * 
 * @author Administrator
 * 
 */
public class TaskInfoParser {

	/**
	 * 获取正在运行的所有的进程的信息。
	 * @param context 上下文
	 * @return 进程信息的集合
	 ToDo: 得到运行中的进程信息
	 */
	public static List<TaskInfo> getRunningTaskInfos(Context context) {
		List<TaskInfo> taskInfos = new ArrayList<TaskInfo>();
		ActivityManager am = (ActivityManager) context
				.getSystemService(Context.ACTIVITY_SERVICE);

		Hashtable<String, List<RunningServiceInfo>> runningInfos
				= new Hashtable<String, List<RunningServiceInfo>>();
		int nCount = SystemInfoUtils.getRunningPocessByService(context, runningInfos);
		Enumeration<String> entry = runningInfos.keys();
		String packname = "";

		while(entry.hasMoreElements()) {
			TaskInfo taskInfo = new TaskInfo();
			packname = entry.nextElement();;
			taskInfo.packageName = packname;
			List<RunningServiceInfo> servicesInfo = runningInfos.get(packname);
            Set pidsSet = new HashSet();
			for (RunningServiceInfo serviceInfo : servicesInfo) {
				pidsSet.add(serviceInfo.pid);
			}
			int i = 0;
			int [] pids = new int[pidsSet.size()];
			Iterator<Object> iter = pidsSet.iterator();
			while (iter.hasNext()) {
				Object pid = iter.next();
				pids[i++] = (Integer)pid;
			}

			MemoryInfo[]  memroyinfos =
					am.getProcessMemoryInfo(pids);
			long appMemory = 0;
			long[] pidsMemory = new long[pids.length];
			i = 0;
			for (MemoryInfo memInfo : memroyinfos) {
				long memsize = memInfo.getTotalPrivateDirty()*1024;
				appMemory += memsize;
				pidsMemory[i++] = memsize;
			}
			taskInfo.pids = pids;
			taskInfo.pidsMemory = pidsMemory;
			taskInfo.appMemory = appMemory;

			setTaskInfoByPackageInfo(context, taskInfo);
			taskInfos.add(taskInfo);
		}

		List<UsageStats> usgStsLst = SystemInfoUtils.getUsageStatsList(context);
		for (UsageStats usageStats : usgStsLst) {
			packname = usageStats.getPackageName();
			if (runningInfos.get(packname) != null) {
				continue;
			}
			TaskInfo taskInfo = new TaskInfo();
			taskInfo.packageName = packname;
			setTaskInfoByPackageInfo(context, taskInfo);
			/*ToDo :
			 taskInfo.pids = pids;
			 taskInfo.pidsMemory = pidsMemory;
			 taskInfo.appMemory = appMemory;
			 */
			taskInfos.add(taskInfo);
		}


		return taskInfos;
	}

	private static int setTaskInfoByPackageInfo(Context context,
	                                            TaskInfo taskInfo) {
		int nRet = -1;
		if (null == taskInfo) {
			return nRet;
		}
		try {
			PackageManager pm = context.getPackageManager();
			PackageInfo packInfo = pm.getPackageInfo(taskInfo.packageName, 0);
			Drawable icon = packInfo.applicationInfo.loadIcon(pm);
			taskInfo.appIcon = icon;
			String appname = packInfo.applicationInfo.loadLabel(pm).toString();
			taskInfo.appName = appname;
			if ((ApplicationInfo.FLAG_SYSTEM & packInfo.applicationInfo.flags) != 0) {
				//系统进程
				taskInfo.isUserApp = false;
			} else {
				//用户进程
				taskInfo.isUserApp = true;
			}
			nRet = 0;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
			taskInfo.appName = taskInfo.packageName;
			taskInfo.appIcon = context.getResources().getDrawable(R.drawable.ic_default);
		}
		return nRet;
	}
}
