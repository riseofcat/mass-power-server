package com.n8cats.share;
//todo redundant. Replace with Integer
public class Tick {
//do not use in JSON
public final int tick;
public Tick(int tick) {
	this.tick = tick;
}
public Tick add(int t) {
	return new Tick(tick + t);
}
public boolean equals(Object o) {
	if(this == o) return true;
	if(o == null || getClass() != o.getClass()) return false;
	Tick tick1 = (Tick) o;
	return tick == tick1.tick;
}
public int hashCode() {
	return tick;
}
}