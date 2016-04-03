package cn.jet.mobilesafe.AdvancedTools.utils;

/**
 * Created by jerry on 16-3-21.
 */
public class SmsField {
    //_id 一个自增字段，从1开始
    //thread_id 序号，同一发信人的id相同
    public static final String ADDRESS = "address"; //发件人手机号码
    public static final String PERSON = "person"; //联系人列表里的序号，陌生人为null
    public static final String DATE = "date"; //发件日期
    public static final String PROTOCOL = "protocol";//0 SMS_RPOTO, 1 MMS_PROTO
    public static final String READ = "read"; //read=0表示未读，read=1表示读过
    public static final String STATUS = "status"; //-1接收，0 complete, 64 pending, 128 failed
    /*
    ALL = 0;
    INBOX = 1; //收件箱
    SENT = 2;  //发件箱
    DRAFT = 3;
    OUTBOX = 4;
    FAILED = 5;
    QUEUED = 6;
     */
    public static final String TYPE = "type";
    //service_center 短信服务中心号码编号
    //subject 短信的主题
    public static final String REPLY_PATH_PRESENT = "reply_path_present";
    public static final String BODY = "body"; //短信内容
    public static final String LOCKED = "locked";
    public static final String ERROR_CODE = "error_code";
    public static final String SEEN = "seen"; //seen=0表示未读，seen=1表示读过
}