package cn.jet.mobilesafe.AdvancedTools.test;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.test.AndroidTestCase;
import android.util.Log;

import cn.jet.mobilesafe.AdvancedTools.entity.SmsInfo;
import cn.jet.mobilesafe.AdvancedTools.utils.SmsExportUtil;
import cn.jet.mobilesafe.AdvancedTools.utils.SmsImportUtil;

/**
 * Created by jerry on 16-3-20.
 */
public class TestSMS extends AndroidTestCase {
    private static final String TAG = "TestSMS";
    private Context mContext;
    @Override
    protected void setUp() throws Exception {
        mContext = getContext();
        super.setUp();
    }

    /**
     * 测试添加
     *
     * @throws Exception
     */
    public void testAddSmsMsg() throws Exception {
        SmsInfo smsInfo = new SmsInfo();
        smsInfo.body = "【测试短信】\n";
        smsInfo.address = "10690755771082";

        smsInfo.type = "1";
        smsInfo.date = "1451105936159";

        ContentValues values = new ContentValues();
        values.put("address", smsInfo.address);
        values.put("type", smsInfo.type);
        values.put("date", smsInfo.date);
        values.put("body", smsInfo.body);

        ContentResolver resolver = mContext.getContentResolver();
        Uri uri = Uri.parse("content://sms");
        Cursor cursor = resolver.query(uri, new String[]{"address",
                "body", "type", "date"}, null, null, null);
        int size = cursor.getCount();
        Log.i("testAddSmsMsg", "before insert count: " + size);
        cursor.close();

        Uri newValueURI = resolver.insert(uri, values);
        cursor = resolver.query(newValueURI, new String[] { "address",
                "body", "type", "date" }, null, null, null);
        // 得到总的条目的个数
        size = cursor.getCount();
        if (1 == size) {
            Log.i("testAddSmsMsg", "insert OK");
        } else {
            Log.i("testAddSmsMsg", "insert failed.");
        }
        while(cursor.moveToNext()) {
            String body = cursor.getString(1);
            if (body.equals(smsInfo.body)) {
                Log.i("testAddSmsMsg", "query ok");
            }
        }
        cursor.close();

        cursor = resolver.query(uri, new String[]{"address",
                "body", "type", "date"}, null, null, null);
        size = cursor.getCount();
        Log.i("testAddSmsMsg", "after insert count: " + size);
        cursor.close();
    }

    public void testSmsExport() {
        try {
            SmsExportUtil exporter = new SmsExportUtil(mContext);
            boolean bRet = exporter.createXml();
            Log.i(TAG, "export result " + bRet);
        } catch (Exception e) {
            Log.i(TAG, "SmsExportUtil exception " + e.getMessage());
        }
    }

    public void testSmsImport() {
        SmsImportUtil importer = new SmsImportUtil(mContext);
        importer.ImportSMSFromXml();
    }
}
