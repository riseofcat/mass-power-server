package com.n8cats.share;

import com.n8cats.share.data.BigAction;
import com.n8cats.share.data.PlayerId;
import com.n8cats.share.data.State;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;

public class ServerPayload {
public float tick;
@Nullable public Welcome welcome;
@Nullable public Stable stable;
@Nullable public ArrayList<TickActions> actions;
@Nullable public HashSet<Integer> canceled;
@Nullable public ArrayList<AppliedActions> apply;
@Nullable public ServerError error;

public static class TickActions {
	public int tick;
	public ArrayList<BigAction> list;//Порядок важен
	@SuppressWarnings("unused") public TickActions() {
	}
	public TickActions(int tick, ArrayList<BigAction> list) {
		this.tick = tick;
		this.list = list;
	}
}

public static class Welcome {
	public PlayerId id;
}

public static class AppliedActions {
	@SuppressWarnings("unused") public AppliedActions() {
	}
	public AppliedActions(int aid, int delay) {
		this.aid = aid;
		this.delay = delay;
	}
	public int aid;
	public int delay;
}

public static class Stable {
	public int tick;//все actions уже пришли и новых больше не будет. Если кто-то кого-то убил, то в этом кадре засчитывается фраг. Но само убийство и набор очков мог произойти в прошлом
	public State state;
}

public static class ServerError {
	public int code;
	public String message;
}

}
