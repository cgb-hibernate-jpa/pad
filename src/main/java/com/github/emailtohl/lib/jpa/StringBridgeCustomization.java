package com.github.emailtohl.lib.jpa;

import org.hibernate.search.bridge.builtin.StringBridge;

/**
 * SearchRepository只能搜索字符串域的内容，若遇到其他值类型，如数字、日期，则需要进行字符串转换。
 * 在非字符串的值类型上使用注解：@FieldBridge(impl = StringBridgeCustomization.class)
 * 
 * @author HeLei
 */
public class StringBridgeCustomization extends StringBridge {
	@Override
	public String objectToString(Object object) {
		if (object == null) {
			return "";
		}
		return object.toString();
	}
}
