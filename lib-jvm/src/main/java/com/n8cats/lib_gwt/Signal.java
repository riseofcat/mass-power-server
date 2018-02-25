package com.n8cats.lib_gwt;

import java.util.ArrayList;
import java.util.Iterator;

public class Signal<T> {
//todo weak reference
private ArrayList<Callback> callbacks = new ArrayList<>();
public void dispatch(T value) {
	ArrayList<Callback> currentCallbacks = new ArrayList<>(callbacks);
	Iterator<Callback> iterator = currentCallbacks.iterator();
	while(iterator.hasNext()) {
		Callback next = iterator.next();
		next.listener.onSignal(value);
		if(next.once) {
			next.removed = true;
		}
	}
	iterator = callbacks.iterator();
	while(iterator.hasNext()) {
		Callback next = iterator.next();
		if(next.removed) {
			iterator.remove();
		}
	}
}
public void add(Listener<T> listener) {
	Callback c = new Callback();
	c.listener = listener;
	callbacks.add(c);
}
public void addOnce(Listener<T> listener) {
	Callback c = new Callback();
	c.listener = listener;
	c.once = true;
	callbacks.add(c);
}
public void remove(Listener<T> signalListener) {
	Iterator<Callback> iterator = callbacks.iterator();
	while(iterator.hasNext()) {
		Callback next = iterator.next();
		if(next.listener == signalListener) {
			next.removed = true;
			iterator.remove();
		}
	}
}
public void destroy() {
	callbacks.clear();
}
public interface Listener<T> {
    void onSignal(T arg);
}
private class Callback {
	public Signal.Listener<T> listener;
	public boolean removed = false;
	public boolean once = false;
}
}