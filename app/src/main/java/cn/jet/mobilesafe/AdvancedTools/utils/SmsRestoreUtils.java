package cn.jet.mobilesafe.AdvancedTools.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.util.Xml;
import cn.jet.mobilesafe.AdvancedTools.entity.SmsInfo;

/***
 * 短信还原的工具类
 * 
 * @author admin
 */
public class SmsRestoreUtils {

	public interface SmsRestoreCallBack {
		/**
		 * 在短信还原之前调用的方法
		 * 
		 * @param size
		 *            总的短信的个数
		 */
		public void beforeSmsRestore(int size);

		/**
		 * 当sms短信还原过程中调用的方法
		 * 
		 * @param process
		 *            当前的进度
		 */
		public void onSmsRestore(int process);
	}

	private boolean flag = true;

	public void setFlag(boolean flag) {
		this.flag = flag;
	}

	//ToDo: >=android 4.4 默认短信以外的其它应用不能修改content://sms/数据库
	public boolean restoreSms(Activity context,
	                          SmsRestoreCallBack callBack) throws XmlPullParserException,
			IOException {
		File file = new File(Environment.getExternalStorageDirectory(),
				"backup.xml");
		if (!file.exists()) {
			// 如果backup.xml文件不存在，则说明没有备份短信
			UIUtils.showToast(context, "您还没有备份短信！");
			return false;
		}

		FileInputStream is = new FileInputStream(file);
		XmlPullParser parser = Xml.newPullParser();
		parser.setInput(is, "utf-8");
		SmsInfo smsInfo = null;
		int eventType = parser.getEventType();
		Integer max = null;
		int progress = 0;
		ContentResolver resolver = context.getContentResolver();
		Uri uri = Uri.parse("content://sms/");
		while (eventType != XmlPullParser.END_DOCUMENT & flag) {
			switch (eventType) {
			// 一个节点的开始
			case XmlPullParser.START_TAG:
				if ("smss".equals(parser.getName())) {
					String maxStr = parser.getAttributeValue(0);
					max = new Integer(maxStr);
					callBack.beforeSmsRestore(max);
				} else if ("sms".equals(parser.getName())) {
					smsInfo = new SmsInfo();
				} else if ("body".equals(parser.getName())) {
					try {
						String text = parser.nextText();
						smsInfo.body = Crypto.decrypt("21fcde8796dbd25088db32118dd73687", text);
					} catch (Exception e) {
						e.printStackTrace();
						// 此条短信还原失败
						smsInfo.body = "短信还原失败";
					}
				} else if ("address".equals(parser.getName())) {
					smsInfo.address = parser.nextText();
				} else if ("type".equals(parser.getName())) {
					smsInfo.type = parser.nextText();
				} else if ("date".equals(parser.getName())) {
					smsInfo.date = parser.nextText();
				}
				break;
			// 一个节点的结束
			case XmlPullParser.END_TAG:
				if ("sms".equals(parser.getName())) {
					// 向短信数据库中插入一条数据
					ContentValues values = new ContentValues();
					values.put("address", smsInfo.address);
					values.put("type", smsInfo.type);
					values.put("date", smsInfo.date);
					values.put("body", smsInfo.body);
					resolver.insert(uri, values);
					smsInfo = null;
					progress++;
					callBack.onSmsRestore(progress);
				}
				break;
			}
			// 得到下一个节点的事件类型，此行代码一定不能忘否则会成死循环
			eventType = parser.next();
		}
		/*
		// 防止出现在备份未完成的情况下，还原短信??
		if (eventType == XmlPullParser.END_DOCUMENT & max != null) {
			if (progress < max) {
				callBack.onSmsRestore(max);
			}
		}
		*/

		Cursor cursor = resolver.query(uri, new String[] { "address",
				"body", "type", "date" }, null, null, null);
		// 得到总的条目的个数
		int size = cursor.getCount();
		cursor.close();

		return flag;
	}
}
