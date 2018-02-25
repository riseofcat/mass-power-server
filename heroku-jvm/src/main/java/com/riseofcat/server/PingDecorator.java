package com.riseofcat.server;
import com.n8cats.lib.TypeMap;
import com.n8cats.share.ClientSay;
import com.n8cats.share.ServerSay;

import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PingDecorator<C, S> extends SesServ<ClientSay<C>, ServerSay<S>> {
private final int pingIntervalMs;
private final SesServ<C, S> server;
private final Map<Ses, PingSes> map = new ConcurrentHashMap<>();
public PingDecorator(SesServ<C, S> server, int pingIntervalMs) {
	this.pingIntervalMs = pingIntervalMs;
	this.server = server;
}
public void start(Ses session) {
	PingSes s = new PingSes(session);
	map.put(session, s);
	server.start(s);
}
public void close(Ses session) {
	server.close(map.get(session));
	map.remove(session);
}
public void message(Ses session, ClientSay<C> say) {
	PingSes s = map.get(session);
	if(say.pong && s.lastPingTime != null) {
		long l = (System.currentTimeMillis() - s.lastPingTime + 1) / 2;
		s.latency = (int) l;
	}
	if(say.payload != null) {
		server.message(s, say.payload);
	}
}

private class PingSes extends SesServ<C, S>.Ses {
	private final Ses sess;
	@Nullable private Long lastPingTime;
	@Nullable private Integer latency;
	private PingSes(Ses sess) {
		this.sess = sess;
		put(new Extra(this));
	}
	public int getId() {
		return sess.getId();
	}
	public void stop() {
		sess.stop();
	}
	public void send(S payload) {
		ServerSay<S> say = new ServerSay<>();
		say.latency = latency;
		if(lastPingTime == null || System.currentTimeMillis() > lastPingTime + pingIntervalMs) {
			say.ping = true;
			lastPingTime = System.currentTimeMillis();
		}
		say.payload = payload;
		sess.send(say);
	}
	protected TypeMap getTypeMap() {
		return sess.getTypeMap();
	}
}

public static class Extra implements TypeMap.Marker{
	private final PingDecorator.PingSes pingSes;
	public Extra(PingDecorator.PingSes pingSes) {
		this.pingSes = pingSes;
	}
	@Nullable public Integer getLatency() {
		return pingSes.latency;
	}
}

}
