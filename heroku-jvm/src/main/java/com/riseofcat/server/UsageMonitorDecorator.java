package com.riseofcat.server;

import com.riseofcat.lib.TypeMap;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UsageMonitorDecorator<C, S> extends SesServ<C, S> {
private final SesServ<C, S> server;
private Map<Ses, CountSes> map = new ConcurrentHashMap<>();
private int sessionsCount = 0;
public UsageMonitorDecorator(SesServ<C, S> server) {
	this.server = server;
}
public void start(Ses session) {
	CountSes s = new CountSes(session);
	map.put(session, s);
	server.start(s);
	sessionsCount++;
}
final public void close(Ses session) {
	server.close(map.get(session));
	map.remove(session);
	sessionsCount--;
}
final public void message(Ses session, C code) {
	CountSes s = map.get(session);
	server.message(s, code);
	s.incomeCalls++;
}
public final int getSessionsCount() {
	return sessionsCount;
}

public class CountSes extends SesServ<C, S>.Ses {
	private int incomeCalls;
	private int outCalls;
	private final Ses sess;
	private final long startTimeMs;
	public CountSes(Ses session) {
		this.sess = session;
		startTimeMs = System.currentTimeMillis();
		put(new Extra(this));
	}
	public int getId() {
		return sess.getId();
	}
	public void stop() {
		sess.stop();
	}
	public void send(S message) {
		sess.send(message);
		outCalls++;
	}
	public TypeMap getTypeMap() {
		return sess.getTypeMap();
	}
}

public static class Extra implements TypeMap.Marker {
	private final UsageMonitorDecorator.CountSes countSes;

	public Extra(UsageMonitorDecorator.CountSes countSes) {
		this.countSes = countSes;
	}

	public int getIncomeCalls() {
		return countSes.incomeCalls;
	}
	public int getOutCalls() {
		return countSes.outCalls;
	}
	public long getStartTime() {
		return countSes.startTimeMs;
	}
}

}
