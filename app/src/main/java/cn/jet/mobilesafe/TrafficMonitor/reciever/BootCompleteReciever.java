package cn.jet.mobilesafe.TrafficMonitor.reciever;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import cn.jet.mobilesafe.TrafficMonitor.service.TrafficMonitoringService;
import cn.jet.mobilesafe.TrafficMonitor.utils.SystemInfoUtils;

/**监听开机的广播该类，更新数据库，开启服务*/
public class BootCompleteReciever extends BroadcastReceiver{
	@Override
	public void onReceive(Context context, Intent intent) {
		//开机广播
		//判断流量监控服务是否开启，如果没开启则开启
		if(!SystemInfoUtils.isServiceRunning(context,"cn.jet.mobilesafe.TrafficMonitor.service.TrafficMonitoringService")){
			//开启服务
			context.startService(new Intent(context, TrafficMonitoringService.class));
		}
	}
}
