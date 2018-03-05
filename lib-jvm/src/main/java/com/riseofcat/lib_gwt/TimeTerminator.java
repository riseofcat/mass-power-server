package com.riseofcat.lib_gwt;

public class TimeTerminator implements com.riseofcat.lib_gwt.ITerminator {

private final int maxWaitTimeMs;
private final long initTimeMs;

public TimeTerminator(int maxWaitTimeMs) {
	this.maxWaitTimeMs = maxWaitTimeMs;
	initTimeMs = System.currentTimeMillis();
}

@Override
public boolean terminated() {
	return System.currentTimeMillis() >= initTimeMs + maxWaitTimeMs;
}
}
