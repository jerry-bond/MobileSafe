package cn.jet.mobilesafe.BlackNumber.reciever;

import cn.jet.mobilesafe.BlackNumber.db.DBOperator.BlackNumberDBOperator;
import cn.jet.mobilesafe.common.Constants;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.telephony.SmsMessage;
import android.util.Log;
import android.net.Uri;
import android.database.Cursor;

/*
	ToDo: 1. >=Android4.4系统短信的拦截 彩信 多媒体短信， InterceptSmsReciever 设置为Default?
		   2. 广播被其它优先级高的BroadcastReceiver.abortBroadcast android:priority>="2147483647"截断处理?
 */

public class InterceptSmsReciever extends BroadcastReceiver {
	private final static String TAG = "InterceptSmsReciever";
	@Override
	public void onReceive(Context context, Intent intent) {
		SharedPreferences mSP = context.getSharedPreferences("config",
				Context.MODE_PRIVATE);
		boolean BlackNumStatus = mSP.getBoolean("BlackNumStatus", true);
		if (!BlackNumStatus) {
			// 黑名单拦截关闭
			return;
		}
		// 如果是黑名单 则终止广播
		BlackNumberDBOperator dao = new BlackNumberDBOperator(context);
		Object[] objs = (Object[]) intent.getExtras().get("pdus");
		for (Object obj : objs) {
			SmsMessage smsMessage = SmsMessage.createFromPdu((byte[]) obj);
			String sender = smsMessage.getOriginatingAddress();
			String body = smsMessage.getMessageBody();
			if (sender.startsWith("+86")) {
				sender = sender.substring(3, sender.length());
			}
			int mode = dao.getBlackContactMode(sender);
			if ((Constants.BLACK_NUM_SMS == mode) ||
					(Constants.BLACK_NUM_ALL == mode)) {
				Log.i(TAG, "拦截短信");
				// 需要拦截短信，拦截广播
				abortBroadcast();
				deleteSMS(context, sender, body);
			}
		}
	}

	public void deleteSMS(Context context, String phoneNumber, String smsContent)	{
		try		{
			//两条相同知信??
			// 准备系统短信收信箱的uri地址
			Uri uri = Uri.parse("content://sms/inbox");// 收信箱
			// 查询收信箱里所有的短信
			Cursor cursorSMS =
					context.getContentResolver().query(uri, null, "read=" + 0,
							null, null);
			while (cursorSMS.moveToNext())			{
				 String phone =
						 cursorSMS.getString(cursorSMS.getColumnIndex("address")).trim();//获取发信人
				String body =
						cursorSMS.getString(cursorSMS.getColumnIndex("body")).trim();// 获取信息内容
				Log.i(TAG, phone + " " + body);
				if (phone.equals(phoneNumber) && body.equals(smsContent))				{
					int id = cursorSMS.getInt(cursorSMS.getColumnIndex("_id"));
					context.getContentResolver().delete(
							Uri.parse("content://sms"), "_id=" + id, null);
					Log.i(TAG, "Delete " + phone + " " + body);
				}
			}
		}
		catch (Exception e)		{
			e.printStackTrace();
		}
	}
}