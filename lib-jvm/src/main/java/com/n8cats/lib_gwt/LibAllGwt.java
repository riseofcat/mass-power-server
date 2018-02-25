package com.n8cats.lib_gwt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class LibAllGwt {
public static final float MILLIS_IN_SECCOND = 1000f;
public static boolean TRUE() {
	return Boolean.TRUE;
}
public static boolean FALSE() {
	return Boolean.FALSE;
}
public static boolean nullOrEmpty(String s) {
	if(s == null) return true;
	if(s.trim().length() == 0) return true;
	return false;
}
public static boolean strEquals(String str1, String str2) {
	if(str1 == null) return str2 == null;
	return str1.equals(str2);
}
public static int getRand() {
	return (int) (Math.random() * Integer.MAX_VALUE);
}
public static int getRand(int min, int max) {
	return min + getRand() % (max - min + 1);
}
public static String putStrArgs(String str, StrArgs args) {//todo optimize me
	String result = str;
	for(String key : args.map.keySet()) result = result.replaceAll("[{]" + key + "[}]", args.map.get(key));
	return result;
}
public static class StrArgs {
	private final HashMap<String, String> map;
	public StrArgs() {
		map = new HashMap<>();
	}
	public StrArgs put(String key, String value) {
		map.put(key, value);
		return this;
	}
	public HashMap<String, String> getMap() {
		return map;
	}
	public StrArgs applyForAllValues(IApplyForAllValues handler) {
		for(String k : map.keySet()) map.put(k, handler.handle(map.get(k)));
		return this;
	}
	public interface IApplyForAllValues {
		String handle(String value);
	}
}
public static boolean isIp(String str) {
	return str.matches("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$");
}
public static Properties readProperties(String txt) {
	return new Properties(txt);
}
public static Properties createProperties() {
	return new Properties();
}
public static class Properties {
	List<Element> elements = new ArrayList<>();
	Map<String, Property> map = new HashMap<>();
	private Properties() {
	}
	private Properties(String txt) {
		for(String line : txt.split("\n")) {
			if(line.length() == 0) continue;
			if(line.charAt(0) == '#') elements.add(new Comment(line.substring(1)));
			else {
				String[] split = line.split("=");
				Property property = new Property(split[0]);
				for(int i = 1; i < split.length; i++) {
					property.value += split[i];
					if(i != split.length - 1) {//Если не последний элемент
						property.value += "=";
					}
				}
				elements.add(property);
				map.put(property.name, property);
			}
		}
	}
	public void setProperty(String name, String value) {
		Property prop;
		if(map.containsKey(name)) prop = map.get(name);
		else {
			prop = new Property(name);
			map.put(name, prop);
			elements.add(prop);
		}
		prop.value = value;
	}
	public String getProperty(String name) {
		Property property = map.get(name);
		if(property != null) return property.value;
		else return null;
	}
	public String toString() {
		StringBuilder result = new StringBuilder("");
		Iterator<Element> iterator = elements.iterator();
		while(iterator.hasNext()) {
			result.append(iterator.next().toStr());
			if(iterator.hasNext()) result.append("\n");
		}
		return result.toString();
	}
	interface Element {
		String toStr();
	}
	static class Comment implements Element {
		private final String comment;
		public Comment(String comment) {
			this.comment = comment;
		}
		@Override
		public String toStr() {
			return "#" + comment;
		}
	}
	static class Property implements Element {
		public final String name;
		public String value = "";
		public Property(String name) {
			this.name = name;
		}
		@Override
		public String toStr() {
			return name + "=" + value;
		}
	}
}
public static <T> T doNow(IDoNow<T> doNow) {
	return doNow.doNow();
}
public interface IDoNow<T> {
	T doNow();
}
public static <T, R> R checkNull(T value, IDo<T, R> doIfNotNull, IDoNow<R> doIfNull) {
	if(value != null) {
		return doIfNotNull.doIfNotNull(value);
	}
	return doIfNull.doNow();
}
public interface IDo<T, R> {
	R doIfNotNull(T v);
}
public static <T extends Cloneable<T>> T copy(T value) {
	return clone(value);
}
public static <T extends Cloneable<T>> T clone(T value) {
	return value.clone();
}
public interface Cloneable<T> extends java.lang.Cloneable {
	public T clone();
}
public static class Fun {
	static {
		if(arg0toInf(10, 10) != 0.5f) {
			throw new RuntimeException("Lib fail");
		}
	}
	public static int sign(float a) {
		return sign((double) a);
	}
	public static int sign(double a) {
		if(a == 0) return 0;
		return (int) (a / Math.abs(a));
	}
	public static double positive(double v) {
		if(v > 0) return v;
		return 0;
	}
	public static float positive(float v) {
		return (float) positive((double) v);
	}
	public static float arg0toInf(double y, float middle) {
		return (float) (y / middle / (1 + y / middle));
	}
}
}
