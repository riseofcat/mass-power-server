package com.riseofcat.lib_gwt;

import java.io.Serializable;

public class Const {

public static final String UTF_8 = "UTF-8";
public static final float MEGA = 1E6f;

public static enum ProxyType {
	socks5,
	socks4,
	https,
	http
}

public static class Id implements Serializable {
	public int id;

	public Id(int id) {
		this.id = id;
	}

	public Id() {
	}

	@Override
	public String toString() {
		return String.valueOf(id);
	}

	@Override
	public boolean equals(Object obj) {
		try {
			return this.id == ((Id) obj).id;
		} catch(Error e) {
			return false;
		}

	}
	public static class BigTask extends Id {
		public BigTask(int id) {
			super(id);
		}
		public BigTask() {
			super();
		}
	}

	public static class Account extends Id {
		public Account(int id) {
			super(id);
		}
		public Account() {
			super();
		}
	}
}


}
