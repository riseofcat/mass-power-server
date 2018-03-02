package com.n8cats.share.data;

import java.util.Collection;
import java.util.Iterator;

public class CompositeIterator<T> implements Iterator<T> {
private final Collection<? extends T>[] lists;
private int index = 0;
private Iterator<? extends T> iterator;
public CompositeIterator(Collection<? extends T>... lists) {
	this.lists = lists;
	iterator = lists[0].iterator();
}
public boolean hasNext() {
	if(iterator.hasNext()) {
		return true;
	}
	for(int add = 1; index + add < lists.length; add++) {
		if(lists[index + add].size() > 0) {
			return true;
		}
	}
	return false;
}
public T next() {
	while(index < lists.length) {
		if(iterator.hasNext()) {
			return iterator.next();
		}
		index++;
		if(index < lists.length) {
			iterator = lists[index].iterator();
		}
	}
	return null;
}
public void remove() {
	iterator.remove();//todo test
}
}
