package cn.jet.mobilesafe.AdvancedTools.test;

import android.test.AndroidTestCase;
import android.util.Log;

import cn.jet.mobilesafe.AdvancedTools.utils.Crypto;

/**
 * Created by jerry on 16-3-20.
 */
public class TestCrypto  extends AndroidTestCase {

    private static final String TAG = "TestCrypto";
    private static String mSrcText;
    private static String mEncryptText;
    private static String mSeed = "21fcde8796dbd25088db32118dd73687";
    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    /**
     * 测试加密
     *
     * @throws Exception
     */
    public void testEncrypt() throws Exception {
        mSrcText = "中国移动";
        mEncryptText = Crypto.encrypt(mSeed, mSrcText);
        Log.i(TAG, "testEncrypt " + mSrcText + " " + mEncryptText);
    }

    public void testDecrypt() throws Exception {
        testEncrypt();
        Log.i(TAG, "seed: " + mSeed + " encryptText: " + mEncryptText);
        String decryptText = Crypto.decrypt(mSeed, mEncryptText);
        if(!decryptText.equals(mSrcText)) {
            throw new IllegalArgumentException(decryptText + "!=" + mSrcText );
        } else {
            Log.i(TAG, "encryptText: " + mEncryptText + " = " + mSrcText);
        }
    }


}
