package com.github.emailtohl.lib.jpa;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.security.Timestamp;
import java.time.temporal.TemporalAmount;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimeZone;

import javax.persistence.Temporal;

/**
 * 使用公开JSON库难以避开懒加载异常，这里自定义序列化json
 * 
 * @author HeLei
 */
class JsonBuilder {
	/**
	 * 将对象序列化为json，对于集合，只考虑java.util包下的，这样可以避免懒加载异常
	 * 
	 * @param o
	 *            需要序列化的对象
	 * @return json字符串
	 */
	static String build(Object o) {
		JsonBuilder jb = new JsonBuilder();
		jb.toJson(o);
		return jb.json.toString();
	}

	private StringBuilder json = new StringBuilder();
	private Set<Object> used = new HashSet<Object>();

	/**
	 * 在对象中筛选出值对象
	 * 
	 * @param o
	 *            参数对象
	 * @return 值对象返回true，否则返回false
	 */
	private boolean otherAvailableObj(Object o) {
		return o instanceof Serializable && o instanceof String || o instanceof Number || o instanceof Enum
				|| o instanceof Character || o instanceof Boolean || o instanceof Date || o instanceof Calendar
				|| o instanceof Timestamp || o instanceof TimeZone || o instanceof TemporalAmount
				|| o instanceof Temporal;
	}

	private void toJson(Object obj) {
		if (!(obj instanceof Serializable)) {
			json.append('"').append("").append('"');
			return;
		}
		Class<?> clz = obj.getClass();
		if (obj instanceof Number || obj instanceof Boolean) {
			json.append(obj);
		} else if (obj instanceof Calendar) {
			json.append(((Calendar) obj).getTime().getTime());
		} else if (otherAvailableObj(obj)) {// 将已知的可以直接toString使用的类列进来
			json.append('"').append(obj).append('"');
		} else if (clz.isArray()) {
			json.append('[');
			int length = Array.getLength(obj);
			for (int i = 0; i < length; i++) {
				if (i > 0) {
					json.append(',');
					toJson(Array.get(obj, i));
				} else {
					toJson(Array.get(obj, i));
				}
			}
			json.append(']');
		} else if (Collection.class.isAssignableFrom(clz) && clz.getName().startsWith("java.util")) {
			json.append('[');
			Iterator<?> i = ((Collection<?>) obj).iterator();
			boolean first = true;
			while (i.hasNext()) {
				if (first) {
					first = false;
				} else {
					json.append(',');
				}
				toJson(i.next());
			}
			json.append(']');
		} else if (Map.class.isAssignableFrom(clz) && clz.getName().startsWith("java.util")) {
			json.append('{');
			Map<?, ?> map = (Map<?, ?>) obj;
			boolean first = true;
			for (Entry<?, ?> entry : map.entrySet()) {
				String name = entry.getKey().toString();
				if (first) {
					first = false;
				} else {
					json.append(',');
				}
				json.append('"').append(name).append('"').append(':');
				toJson(entry.getValue());
			}
			json.append('}');
		} else {// 当做普通的bean处理
			if (used.contains(obj)) {// 若遇到相互关联的情况，则终止递归
				json.append('"').append("").append('"');
				return;
			}
			used.add(obj);
			json.append('{');
			boolean first = true;
			for (EntityProperty prop : EntityInspector.getEntityProperty(obj.getClass())) {
				if (first) {
					first = false;
				} else {
					json.append(',');
				}
				json.append('"').append(prop.name).append('"').append(':');
				toJson(prop.getValue(obj));
			}
			json.append('}');
		}
	}

}
