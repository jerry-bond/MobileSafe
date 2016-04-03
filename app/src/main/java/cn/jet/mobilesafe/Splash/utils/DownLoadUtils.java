package cn.jet.mobilesafe.Splash.utils;

import java.io.File;

import com.lidroid.xutils.HttpUtils;
import com.lidroid.xutils.exception.HttpException;
import com.lidroid.xutils.http.ResponseInfo;
import com.lidroid.xutils.http.callback.RequestCallBack;

public class DownLoadUtils {

	/***
	 * 下载APK的方法
	 * @param url
	 * @param targerFile
	 * @param myCallBack
	 */
	public void downapk(String url,String targerFile, final IDownloadCallBack myCallBack){
		//创建HttpUtils对象
		HttpUtils httpUtils = new HttpUtils();
		//调用HttpUtils下载的方法下载指定文件
		httpUtils.download(url, targerFile, new RequestCallBack<File>() {


			@Override
			public void onSuccess(ResponseInfo<File> arg0) {
				myCallBack.onSuccess(arg0);
			}
			
			@Override
			public void onFailure(HttpException arg0, String arg1) {
				myCallBack.onFailure(arg0, arg1);
			}
			
			@Override
			public void onLoading(long total, long current, boolean isUploading) {
				super.onLoading(total, current, isUploading);
				myCallBack.onLoadding(total, current, isUploading);
			}
			public void onCancelled() {
				myCallBack.onCancelled();
			}
		});
	}
	
	/**
	 *接口，用于监听下载状态的接口
	 * */
	public interface IDownloadCallBack{
		/**下载成功时调用*/
		void onSuccess(ResponseInfo<File> arg0);
		/**下载失败时调用*/
		void onFailure(HttpException arg0, String arg1);
		/**下载中调用*/
		void onLoadding(long total, long current, boolean isUploading);
		void onCancelled();
	}
}
