package cn.jet.mobilesafe.BlackNumber.entity;

import cn.jet.mobilesafe.common.Constants;

public class BlackContactInfo {
	/**黑名单号码*/
	public String phoneNumber;
	/**黑名单联系人名称*/
	public String contactName;
	/**黑名单拦截模式</br>   1为电话拦截   2为短信拦截  3为电话、短信都拦截*/
	public int mode;
	
	public String getModeString(int mode){
		switch (mode) {
		case Constants.BLACK_NUM_CALL:
			return "电话拦截";
		case Constants.BLACK_NUM_SMS:
			
			return "短信拦截";
		case Constants.BLACK_NUM_ALL:
			
			return "电话、短信拦截";

		}
		return "";
	}
}
