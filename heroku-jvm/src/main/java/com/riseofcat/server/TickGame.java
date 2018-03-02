package com.riseofcat.server;
import com.n8cats.lib_gwt.DefaultValueMap;
import com.n8cats.share.ClientPayload;
import com.n8cats.share.data.InStateAction;
import com.n8cats.share.data.Logic;
import com.n8cats.share.Params;
import com.n8cats.share.ServerPayload;
import com.n8cats.share.ShareTodo;
import com.n8cats.share.Tick;
import com.n8cats.share.data.BigAction;
import com.n8cats.share.data.NewCarAction;
import com.n8cats.share.data.PlayerAction;
import com.n8cats.share.data.PlayerId;
import com.n8cats.share.data.State;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

public class TickGame {
private final long startTime = System.currentTimeMillis();
private int previousActionsVersion = 0;
volatile private int tick = 0;//todo volatile redundant? //todo float
private State state = new State();
private DefaultValueMap<Tick, List<Action>> actions = new DefaultValueMap<>(new ConcurrentHashMap<>(), ArrayList::new);
private Map<PlayerId, Integer> mapPlayerVersion = new ConcurrentHashMap<>();
public TickGame(ConcreteRoomsServer.Room room) {
	room.onPlayerAdded.add(player -> {
		synchronized(TickGame.this) {
			int d = 1;
			actions.getExistsOrPutDefault(new Tick(tick + d)).add(new Action(++previousActionsVersion, new NewCarAction(player.getId()).toBig()));
			ServerPayload payload = createStablePayload();
			payload.welcome = new ServerPayload.Welcome();
			payload.welcome.id = player.getId();
			payload.actions = new ArrayList<>();
			for(Map.Entry<Tick, List<Action>> entry : actions.map.entrySet()) {
				ArrayList<BigAction> temp = new ArrayList<>();
				for(Action a : entry.getValue()) temp.add(a.pa);
				payload.actions.add(new ServerPayload.TickActions(entry.getKey().getTick(), temp));
			}
			player.session.send(payload);
			mapPlayerVersion.put(player.getId(), previousActionsVersion);
		}
		for(RoomsDecorator<ClientPayload, ServerPayload>.Room.Player p : room.getPlayers()) if(!p.equals(player)) updatePlayer(p);//Говорим другим, что пришёл новый игрок
	});
	room.onMessage.add(message -> {
		synchronized(TickGame.this) {
			if(message.payload.getActions() != null) {
				for(ClientPayload.ClientAction a : message.payload.getActions()) {
					ServerPayload payload = new ServerPayload();
					payload.tick = tick;
					int delay = 0;
					if(a.getTick() < getStableTick().getTick()) {
						if(a.getTick() < getRemoveBeforeTick()) {
							payload.canceled = new HashSet<>();
							payload.canceled.add(a.getAid());
							message.player.session.send(payload);//todo move out of for
							continue;
						} else delay = getStableTick().getTick() - a.getTick();
					} else if(a.getTick() > getFutureTick()) {
						payload.canceled = new HashSet<>();
						payload.canceled.add(a.getAid());
						message.player.session.send(payload);//todo move out of for
						continue;
					}
					payload.apply = new ArrayList<>();
					payload.apply.add(new ServerPayload.AppliedActions(a.getAid(), delay));
					actions.getExistsOrPutDefault(new Tick(a.getTick() + delay)).add(new Action(++previousActionsVersion, new PlayerAction(message.player.getId(), a.getAction()).toBig()));
					if(ShareTodo.INSTANCE.getSIMPLIFY()) updatePlayerInPayload(payload, message.player);
					message.player.session.send(payload);//todo move out of for
				}
			}
		}
		for(RoomsDecorator<ClientPayload, ServerPayload>.Room.Player p : room.getPlayers()) if(!p.equals(message.player)) updatePlayer(p);
	});
	Timer timer = new Timer();
	timer.schedule(new TimerTask() {
		@Override
		public void run() {
			class Adapter implements Iterator<InStateAction> {
				private Iterator<Action> iterator;
				public Adapter(List<Action> arr) {
					if(arr != null) iterator = arr.iterator();
				}
				public boolean hasNext() {
					return iterator != null && iterator.hasNext();
				}
				public InStateAction next() {
					return iterator.next().pa;
				}
			}
			while(System.currentTimeMillis() - startTime > tick * Logic.getUPDATE_MS()) {
				synchronized(TickGame.this) {
					state.act(new Adapter(actions.map.get(getStableTick()))).tick();
					TickGame.this.actions.map.remove(getStableTick());
					++tick;
					if(tick % 200 == 0) /*todo %*/ for(ConcreteRoomsServer.Room.Player player : room.getPlayers()) player.session.send(createStablePayload());
				}
			}
		}
	}, 0, Logic.getUPDATE_MS() / 2);
}
private void updatePlayer(RoomsDecorator<ClientPayload, ServerPayload>.Room.Player p) {
	ServerPayload payload = new ServerPayload();
	updatePlayerInPayload(payload, p);
	p.session.send(payload);
}
private void updatePlayerInPayload(ServerPayload payload, RoomsDecorator<ClientPayload, ServerPayload>.Room.Player p) {
	payload.actions = new ArrayList<>();
	synchronized(this) {
		payload.tick = tick;
		for(Map.Entry<Tick, List<Action>> entry : actions.map.entrySet()) {
			ArrayList<BigAction> temp = new ArrayList<>();
			for(Action a : entry.getValue()) if(ShareTodo.INSTANCE.getSIMPLIFY() || a.pa.getP() == null || !a.pa.getP().getId().equals(p.getId())) if(a.actionVersion > mapPlayerVersion.get(p.getId())) temp.add(a.pa);
			if(temp.size() > 0) payload.actions.add(new ServerPayload.TickActions(entry.getKey().getTick(), temp));
		}
		mapPlayerVersion.put(p.getId(), previousActionsVersion);
	}
}
ServerPayload createStablePayload() {
	ServerPayload result = new ServerPayload();
	result.tick = tick;
	result.stable = new ServerPayload.Stable();
	result.stable.tick = getStableTick().getTick();
	result.stable.state = state;
	return result;
}
private Tick getStableTick() {
	int result = tick - Params.INSTANCE.getDELAY_TICKS() + 1;
	if(result < 0) return new Tick(0);
	return new Tick(result);
}
private int getRemoveBeforeTick() {
	return tick - Params.INSTANCE.getREMOVE_TICKS() + 1;
}
private int getFutureTick() {
	return tick + Params.INSTANCE.getFUTURE_TICKS();
}
private static class ConcreteRoomsServer extends RoomsDecorator<ClientPayload, ServerPayload> {

}
private class Action {
	public int actionVersion;
	public BigAction pa;
	public Action(int actionVersion, BigAction pa) {
		this.actionVersion = actionVersion;
		this.pa = pa;
	}
}
private void todo() {
	ConcreteRoomsServer.Room.Player player = null;
	long startTime = player.session.get(UsageMonitorDecorator.Extra.class).getStartTime();
	Integer latency = player.session.get(PingDecorator.Extra.class).getLatency();
}
}
