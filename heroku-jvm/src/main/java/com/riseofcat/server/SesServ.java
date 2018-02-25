package com.riseofcat.server;
import com.n8cats.lib.TypeMap;

abstract public class SesServ<C, S> {
abstract public void start(Ses session);
abstract public void close(Ses session);
abstract public void message(Ses ses, C code);

public abstract class Ses {
	private TypeMap typeMapCache;

	abstract public int getId();
	abstract public void stop();
	abstract public void send(S message);
	abstract protected TypeMap getTypeMap();

	public <T extends TypeMap.Marker> void put(T value) {
		if(typeMapCache == null) {
			typeMapCache = getTypeMap();
		}
		typeMapCache.put(value);
	}
	public <T extends TypeMap.Marker> T get(Class<T> type) {
		return getTypeMap().get(type);
	}
}

}
