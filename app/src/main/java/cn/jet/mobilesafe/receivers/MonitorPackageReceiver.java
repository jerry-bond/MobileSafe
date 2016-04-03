package cn.jet.mobilesafe.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
/**
 * Created by jerry on 2/25/2016.
 */

public class MonitorPackageReceiver extends BroadcastReceiver{

    private static String TAG = "MonitorPackageReceiver";
    @Override
    public void onReceive(Context context, Intent intent){
        //接收安装广播
        Log.d(TAG, "onReceive action: "
                + intent.getAction() + " dataString: " +  intent.getDataString());

        //都收不到ACTION_PACKAGE_ADDED ACTION_PACKAGE_REPLACED,只收到ACTION_PACKAGE_REMOVED,原因未知??
        if (intent.getAction().equals("android.intent.action.PACKAGE_ADDED")) {

            String packageName = intent.getDataString();
            Log.d(TAG, "PACKAGE_ADDED " + packageName);
            if (packageName.equals(context.getPackageName())) {
                android.os.Process.killProcess(android.os.Process.myPid());
            }
        }
        else if (intent.getAction().equals(Intent.ACTION_PACKAGE_REMOVED)) {
            String packageName = intent.getDataString();
            if (packageName.equals(context.getPackageName())) {
                Log.d(TAG, "PACKAGE_REMOVED " + packageName);
            }
        }
    }
}