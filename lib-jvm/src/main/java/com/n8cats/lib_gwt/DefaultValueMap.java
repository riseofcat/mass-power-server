package com.n8cats.lib_gwt;

import java.util.Map;

public class DefaultValueMap<K,V> {
public final Map<K, V> map;
private final ICreateNew<V> createNew;
public DefaultValueMap(Map<K,V> map, ICreateNew<V> createNew) {
	this.map = map;
	this.createNew = createNew;
}
public V getExistsOrPutDefault(K key) {
	if(map.containsKey(key)) {
		return map.get(key);
	} else {
		V v = createNew.createNew();
		map.put(key, v);
		return v;
	}
}
public V getOrNew(K key, ICreateNew<V> or) {
	if(map.containsKey(key)) {
		return map.get(key);
	} else {
		return or.createNew();
	}
}
public interface ICreateNew<V> {
	V createNew();
}
}
