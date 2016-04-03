package cn.jet.mobilesafe.AdvancedTools.utils;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.util.Xml;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by jerry on 16-3-21.
 */
public class SmsImportUtil {

    private static String TAG = "SmsImportUtil";
    private Context context;

    private List<SmsItem> smsItems;
    private ContentResolver conResolver;
    private static Uri mUri;

    public SmsImportUtil(Context context) {
        this.context = context;
        conResolver = context.getContentResolver();
        mUri = Uri.parse("content://sms");
    }

    @SuppressWarnings("unchecked")
    public void ImportSMSFromXml() {
        /**
         * 放一个解析xml文件的模块
         */
        smsItems = this.getSmsItemsFromXml();

        for (SmsItem item : smsItems) {
            // 判断短信数据库中是否已包含该条短信，如果有，则不需要恢复
            Cursor cursor = conResolver.query(mUri, new String[] { SmsField.DATE }, SmsField.DATE + "=?",
                    new String[] { item.getDate() }, null);
            while(cursor.moveToNext()) {
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String dateStr = cursor.getString(cursor.getColumnIndex(SmsField.DATE ));
                Date date = new Date();
                long lData = Long.parseLong(dateStr);
                date.setTime(lData);
                String timeValue = format.format(date);
                Log.d(TAG, timeValue);
            }

            if (!cursor.moveToFirst()) {// 没有该条短信

                ContentValues values = new ContentValues();
                values.put(SmsField.ADDRESS, item.getAddress());
                // 如果是空字符串说明原来的值是null，所以这里还原为null存入数据库
                values.put(SmsField.PERSON, item.getPerson().equals("") ? null : item.getPerson());
                values.put(SmsField.DATE, item.getDate());
                values.put(SmsField.PROTOCOL, item.getProtocol().equals("") ? null : item.getProtocol());
                values.put(SmsField.READ, item.getRead());
                values.put(SmsField.STATUS, item.getStatus());
                values.put(SmsField.TYPE, item.getType());
                values.put(SmsField.REPLY_PATH_PRESENT, item.getReply_path_present().equals("") ? null : item.getReply_path_present());
                values.put(SmsField.BODY, item.getBody());
                values.put(SmsField.LOCKED, item.getLocked());
                values.put(SmsField.ERROR_CODE, item.getError_code());
                values.put(SmsField.SEEN, item.getSeen());
                Uri resultUri = conResolver.insert(mUri, values);
                Cursor cursorAfter = conResolver.query(resultUri, null, null, null, null);
                int size = cursorAfter.getCount();
                Log.i("ImportSMSFromXml", "after insert count: " + size);
                cursorAfter.close();
            }
            cursor.close();
        }
    }

//  public void delete() {
//
//      conResolver.delete(mUri, null, null);
//  }

    public List<SmsItem> getSmsItemsFromXml(){

        SmsItem smsItem = null;
        XmlPullParser parser = Xml.newPullParser();
        String absolutePath = Environment.getExternalStorageDirectory() + "/SMSBackup/message.xml";
        File file = new File(absolutePath);
        if (!file.exists()) {
            Looper.prepare();
            Toast.makeText(context, "message.xml短信备份文件不在sd卡中", 1).show();
            Looper.loop();//退出线程
//          return null;
        }
        try {
            FileInputStream fis = new FileInputStream(file);
            parser.setInput(fis, "UTF-8");
            int event = parser.getEventType();
            while (event != XmlPullParser.END_DOCUMENT) {
                switch (event) {
                    case XmlPullParser.START_DOCUMENT:
                        smsItems = new ArrayList<SmsItem>();
                        break;

                    case XmlPullParser.START_TAG: // 如果遇到开始标记，如<smsItems>,<smsItem>等
                        if ("item".equals(parser.getName())) {
                            smsItem = new SmsItem();

                            smsItem.setAddress(parser.getAttributeValue(0));
                            smsItem.setPerson(parser.getAttributeValue(1));
                            smsItem.setDate(parser.getAttributeValue(2));
                            smsItem.setProtocol(parser.getAttributeValue(3));
                            smsItem.setRead(parser.getAttributeValue(4));
                            smsItem.setStatus(parser.getAttributeValue(5));
                            smsItem.setType(parser.getAttributeValue(6));
                            smsItem.setReply_path_present(parser.getAttributeValue(7));
                            smsItem.setBody(parser.getAttributeValue(8));
                            smsItem.setLocked(parser.getAttributeValue(9));
                            smsItem.setError_code(parser.getAttributeValue(10));
                            smsItem.setSeen(parser.getAttributeValue(11));

                        }
                        break;
                    case XmlPullParser.END_TAG:// 结束标记,如</smsItems>,</smsItem>等
                        if ("item".equals(parser.getName())) {
                            smsItems.add(smsItem);
                            smsItem = null;
                        }
                        break;
                }
                event = parser.next();
            }
        } catch (FileNotFoundException e) {
            Looper.prepare();
            Toast.makeText(context, "短信恢复出错", 1).show();
            Looper.loop();
            e.printStackTrace();
        } catch (XmlPullParserException e) {
            Looper.prepare();
            Toast.makeText(context, "短信恢复出错", 1).show();
            Looper.loop();
            e.printStackTrace();
        } catch (IOException e) {
            Looper.prepare();
            Toast.makeText(context, "短信恢复出错", 1).show();
            Looper.loop();
            e.printStackTrace();
        }
        return smsItems;
    }

}