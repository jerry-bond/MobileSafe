package cn.jet.mobilesafe.ProcessManagement.test;

import android.app.ActivityManager;
import android.content.Context;
import android.test.AndroidTestCase;
import android.util.Log;

import java.util.Hashtable;
import java.util.List;

import cn.jet.mobilesafe.ProcessManagement.utils.SystemInfoUtils;

/**
 * Created by jerry on 3/16/2016.
 */
public class TestSystemInfoUtils extends AndroidTestCase {
    private static final String TAG = "TestSystemInfoUtils";
    private Context context;

    @Override
    protected void setUp() throws Exception {
        context = getContext();
        super.setUp();
    }

    /**
     * 测试添加
     *
     * @throws Exception
     */
    public void testgetRunningPocessCount() throws Exception {
        int nCount = SystemInfoUtils.getRunningPocessCount(context);
    }

    /**
     * 测试添加
     *
     * @throws Exception
     */
    public void teststopApp() throws Exception {
        Hashtable<String, List<ActivityManager.RunningServiceInfo>> htServices
                = new Hashtable<String, List<ActivityManager.RunningServiceInfo>>();
        int nCount = SystemInfoUtils.getRunningPocessByService(context, htServices);
        String strPackName;

        strPackName = "com.qiyi.video";
        boolean bResult = SystemInfoUtils.stopApp(context, strPackName);
        if (!bResult) {
            Log.i(TAG, "stopApp " + strPackName + " failed.");
        }
    }

    public void testgetRunningPackages() throws Exception {
        List<String> activePackagesCompat = SystemInfoUtils.getActivePackagesCompat(context);
        List<String> activePackages = SystemInfoUtils.getActivePackages(context);
        List<String> runningPackages = SystemInfoUtils.getRunningApp(context);
    }
}
