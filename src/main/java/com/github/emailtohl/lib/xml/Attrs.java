package com.github.emailtohl.lib.xml;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.util.StringUtils;

/**
 * 简易的xml属性的数据结构，但满足自定义的equals hashcode，可在容器中识别
 * 
 * @author helei
 */
public class Attrs extends HashMap<String, String> {
	private static final long serialVersionUID = -7313446390944972542L;

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		for (Map.Entry<String, String> e : super.entrySet()) {
			result = prime * result + e.getKey().hashCode();
			String val = e.getValue();
			result = prime * result + ((val == null) ? 0 : val.hashCode());
		}
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Attrs other = (Attrs) obj;
		// 若两个集合之间有差集，那么就看差集部分是否有值，若有值，则两者不相等
		if (diff(other)) {
			return false;
		}
		// 差集比较没有差别后，就逐个对每个属性进行比较
		// 此处空字符串和null都视为相等，只有两个字符串都有值且存在差异才视为不相等
		for (Map.Entry<String, String> e : super.entrySet()) {
			String thisVal = e.getValue();
			String otherVal = other.get(e.getKey());
			if (StringUtils.hasText(thisVal) && StringUtils.hasText(otherVal)) {
				if (!thisVal.equals(otherVal)) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * 对两个属性集合进行判断 先判断差集中是否有不一样的 如果有差集，则查看差集部分是否有值，若有则表示不一致
	 * 
	 * @param other 另一个属性
	 * @return 若不一致则返回false
	 */
	private boolean diff(Attrs other) {
		Set<Map.Entry<String, String>> thisSet = super.entrySet();
		Set<Map.Entry<String, String>> otherSet = other.entrySet();
		if (thisSet.removeAll(otherSet)) {
			for (Map.Entry<String, String> e : thisSet) {
				if (StringUtils.hasText(e.getValue())) {
					return true;
				}
			}
		}
		thisSet = super.entrySet();
		otherSet = other.entrySet();
		if (otherSet.removeAll(thisSet)) {
			for (Map.Entry<String, String> e : otherSet) {
				if (StringUtils.hasText(e.getValue())) {
					return true;
				}
			}
		}
		return false;
	}

}
