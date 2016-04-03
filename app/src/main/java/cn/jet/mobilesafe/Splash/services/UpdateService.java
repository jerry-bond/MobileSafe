package cn.jet.mobilesafe.Splash.services;

/**
 * Created by jerry on 2/28/2016.
 */

import java.io.File;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.RemoteViews;
import android.app.Activity;

import com.lidroid.xutils.exception.HttpException;
import com.lidroid.xutils.http.ResponseInfo;

import cn.jet.mobilesafe.R;
import cn.jet.mobilesafe.Splash.utils.MyUtils;
import cn.jet.mobilesafe.Splash.utils.DownLoadUtils;
import cn.jet.mobilesafe.App;

public class UpdateService extends Service {
    private final static String TAG = "UpdateService";
    // 标题
    private String mApkurl = null;

    // 文件存储
    private File updateDir = null;
    private File mLocalFile = null;

    // 下载状态
    private final static int DOWNLOAD_COMPLETE = 0;
    private final static int DOWNLOAD_FAIL = 1;
    private final static int DOWNLOAD_SERVICE_EXIT = 2;

    // 通知栏
    private NotificationManager mUpdateNotificationManager = null;
    private Notification mUpdateNotification = null;
    private Notification.Builder mUpdateNotiBuilder = null;
    // 通知栏跳转Intent
    //private Intent updateIntent = null;
    //private PendingIntent updatePendingIntent = null;
    private Activity mContext = null;
    private UpdateService mUpdateService = null;
    /***
     * 创建通知栏
     */
    RemoteViews contentView;

    //private ProgressDialog mProgressDialog;


    private int mNotificationId = 0;
    private int mTotal = 0;
    private int mCurrent = 0;

    @Override
    public void onCreate() {
        mUpdateService = this;
    }

    // 在onStartCommand()方法中准备相关的下载工作：
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int nFlag = START_NOT_STICKY;
        try {
            mUpdateNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (null == intent) {
                mUpdateHandler.sendEmptyMessage(DOWNLOAD_SERVICE_EXIT);
                return nFlag;
            }
            // 获取传值
            mApkurl = intent.getStringExtra("apkurl");

            // 创建文件
            if (android.os.Environment.MEDIA_MOUNTED.equals(android.os.Environment
                    .getExternalStorageState())) {
                Uri.Builder builder = Uri.parse(mApkurl).buildUpon();
                Uri uri = builder.build();
                String fileName = uri.getLastPathSegment();
                updateDir = Environment.getExternalStorageDirectory();
                mLocalFile = new File(updateDir, fileName);

            }
            mNotificationId = 0;
            mUpdateNotiBuilder = new Notification.Builder(this);

            mUpdateNotiBuilder.setSmallIcon(R.drawable.ic_launcher);
            mUpdateNotiBuilder.setTicker("开始下载");
            mUpdateNotiBuilder.setContentTitle("手机卫士");
            mUpdateNotiBuilder.setContentText("0%");
            mUpdateNotification = mUpdateNotiBuilder.build();

            // 发出通知
            mUpdateNotificationManager.notify(mNotificationId, mUpdateNotification);

            nFlag = START_STICKY;
            // 开启一个新的线程下载，如果使用Service同步下载，会导致ANR问题，Service本身也会阻塞
            new Thread(new updateRunnable()).start();// 这个是下载的重点，是下载的过程
        } catch (Exception ex) {
            Log.i(TAG, ex.toString());
            ex.printStackTrace();
        }
        return nFlag;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy");
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
        //return new UpdateServiceBinder();
    }

    @Override
    public boolean onUnbind(Intent arg0) {
        return false;
    }

    private Handler mUpdateHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case DOWNLOAD_COMPLETE:
                    // 点击安装PendingIntent
                    Uri uri = Uri.fromFile(mLocalFile);
                    Intent installIntent = new Intent(Intent.ACTION_VIEW);
                    installIntent.setDataAndType(uri,
                            "application/vnd.android.package-archive");

                    PendingIntent updatePendingIntent = PendingIntent.getActivity(
                            UpdateService.this, 0, installIntent, 0);

                    mUpdateNotification.defaults = Notification.DEFAULT_SOUND;// 铃声提醒

					/*

                    mUpdateNotification.contentIntent = updatePendingIntent;
                    mUpdateNotification.contentView.setTextViewText(
                            R.id.notificationTitle, "下载完成,点击安装。");
                    mUpdateNotification.contentView.setProgressBar(
                            R.id.notificationProgress, mTotal, mCurrent, false);
                    mUpdateNotification.contentView.setTextViewText(
                            R.id.notificationPercent, "100%");
                    mUpdateNotificationManager.notify(mNotificationId, mUpdateNotification);
					*/

                    Activity activity = App.MyActivityManager.getInstance().getCurrentActivity();
                    MyUtils.installApk(activity, mLocalFile.getAbsolutePath());

                    sendEmptyMessage(DOWNLOAD_SERVICE_EXIT);
                    break;
                case DOWNLOAD_FAIL:
                    // 下载失败
	               //ToDo:退出
                    mUpdateNotification.contentView.setTextViewText(
                            R.id.notificationTitle, "下载失败");
                    mUpdateNotification.contentView.setProgressBar(
                            R.id.notificationProgress, mTotal, mCurrent, false);
                    mUpdateNotification.contentView.setTextViewText(
                            R.id.notificationPercent, mCurrent * 100 / mTotal + "%");
                    mUpdateNotificationManager.notify(mNotificationId, mUpdateNotification);
                    sendEmptyMessage(DOWNLOAD_SERVICE_EXIT);
                    break;

                case DOWNLOAD_SERVICE_EXIT:
                    ServicedExit();
                    break;
                default:
                    mUpdateNotification.contentView.setTextViewText(
                            R.id.notificationTitle, "未知错误 ");
                    mUpdateNotification.contentView.setProgressBar(
                            R.id.notificationProgress, mTotal, mCurrent, false);
                    mUpdateNotification.contentView.setTextViewText(
                            R.id.notificationPercent, mCurrent * 100 / mTotal + "%");
                    mUpdateNotificationManager.notify(mNotificationId, mUpdateNotification);
                    sendEmptyMessage(DOWNLOAD_SERVICE_EXIT);
                    break;
            }
        }
        private  void ServicedExit() {
            Log.i(TAG,"ServicedExit");
            if (mUpdateNotificationManager != null) {
                mUpdateNotificationManager.cancel(mNotificationId);
            }
            if (mUpdateService != null) {
                mUpdateService.stopSelf();
                mUpdateService = null;
            }
        }
    };

    class updateRunnable implements Runnable {
        Message mMessage = mUpdateHandler.obtainMessage();

        public void run() {
            mMessage.what = DOWNLOAD_FAIL;
            try {
                // 增加权限<USES-PERMISSION
                // android:name="android.permission.WRITE_EXTERNAL_STORAGE">;
                if (!updateDir.exists()) {
                    boolean ret = updateDir.mkdirs();
                }

                DownLoadUtils downLoadUtils = new DownLoadUtils();
                downLoadUtils.downapk(mApkurl, mLocalFile.getAbsolutePath(), new DownLoadUtils.IDownloadCallBack() {
                    @Override
                    public void onSuccess(ResponseInfo<File> arg0) {
                        mUpdateHandler.sendEmptyMessage(DOWNLOAD_COMPLETE);
                    }

                    @Override
                    public void onLoadding(long total,
                                           long current, boolean isUploading) {

                        mTotal = (int)total;
                        mCurrent = (int)current;
                        mUpdateNotification.contentView = new RemoteViews(
                                getPackageName(), R.layout.notification_item);
                        mUpdateNotification.contentView.setTextViewText(
                                R.id.notificationTitle, "正在下载");
                        mUpdateNotification.contentView.setProgressBar(
                                R.id.notificationProgress, mTotal, (int) mCurrent, false);
                        mUpdateNotification.contentView.setTextViewText(
                                R.id.notificationPercent, "" + mCurrent * 100 / mTotal);
                        mUpdateNotificationManager.notify(mNotificationId, mUpdateNotification);
                    }

                    @Override
                    public void onFailure(HttpException arg0, String arg1) {
                        mUpdateHandler.sendEmptyMessage(DOWNLOAD_FAIL);
                    }
                    public void onCancelled() {
                        Log.i(TAG, "onCancelled");
                    }
                });

            } catch (Exception ex) {
                ex.printStackTrace();
                mMessage.what = DOWNLOAD_FAIL;
                // 下载失败
                mUpdateHandler.sendMessage(mMessage);
            }
        }
    }

    public void setMainActivity(Activity activity)
    {
        this.mContext = activity;
    }

    public class UpdateServiceBinder extends Binder
    {
        public UpdateService getUpdateService()
        {
            return UpdateService.this;
        }
    }
}
