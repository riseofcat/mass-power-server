package com.riseofcat.lib;

import java.util.HashMap;
import java.util.Map;

public class TypeMap {
private Map<Class<?>, Object> map = new HashMap<>();

public <T extends Marker> void put(T value) {
	map.put(value.getClass(), value);
}

public <T extends Marker> T get(Class<T> type) {
	return type.cast(map.get(type));
}

public interface Marker {

}

}