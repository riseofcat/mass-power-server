package com.riseofcat.server;

import com.n8cats.lib_gwt.Signal;
import com.n8cats.share.Logic;
import com.n8cats.share.data.PlayerId;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class RoomsDecorator<TClientPayload, TServerPayload> extends SesServ<TClientPayload, TServerPayload> {
public final static int MAXIMUM_ROOM_PLAYERS = 5;
public final Signal<Room> onRoomCreated = new Signal<>();
//todo onRoomDestroyed
private final Set<Room> rooms = new HashSet<>();
private final Map<Ses, Room> map = new ConcurrentHashMap<>();
public RoomsDecorator() {
}
public RoomsDecorator(Signal.Listener<Room> roomCreatedListener) {
	onRoomCreated.add(roomCreatedListener);
}
public void start(Ses session) {
	Room room = null;
	synchronized(this) {
		for(Room r : rooms) {
			if(r.getPlayersCount() < MAXIMUM_ROOM_PLAYERS) {
				room = r;
				break;
			}
		}
		if(room == null) {
			App.log.info("new room created");
			room = new Room();
			rooms.add(room);
			onRoomCreated.dispatch(room);
		}
		room.add(session);
	}
	map.put(session, room);
}
public void close(Ses session) {
	Room room = map.remove(session);
	room.remove(session);
}
public void message(Ses session, TClientPayload payload) {
	Room room = map.get(session);
	room.message(session, payload);
}
public class Room {
	final public Signal<Player> onPlayerAdded = new Signal<>();
	final public Signal<Player> onPlayerRemoved = new Signal<>();
	final public Signal<PlayerMessage> onMessage = new Signal<>();
	private final Map<Ses, Player> players = new ConcurrentHashMap<>();
	public int getPlayersCount() {
		return players.size();
	}
	public Collection<Player> getPlayers() {
		return players.values();
	}
	private void add(Ses session) {
		Player player = new Player(session);
		players.put(session, player);
		synchronized(this) {
			onPlayerAdded.dispatch(player);
		}
	}
	private void message(Ses session, TClientPayload payload) {
		onMessage.dispatch(new PlayerMessage(players.get(session), payload));
	}
	private void remove(Ses session) {
		Player remove = players.remove(session);
		synchronized(this) {
			onPlayerRemoved.dispatch(remove);
		}
	}
	public class Player {
		public final Ses session;
		public Player(Ses session) {
			this.session = session;
		}
		public PlayerId getId() {
			return new PlayerId(session.getId());
		}
	}
}
public class PlayerMessage {
	public final Room.Player player;
	public final TClientPayload payload;
	public PlayerMessage(Room.Player player, TClientPayload payload) {
		this.player = player;
		this.payload = payload;
	}
}

public static class RoomHandler<TClientPayload, TServerPayload, E> {
	public void handleRoomCreated(RoomsDecorator<TClientPayload, TServerPayload>.Room room) {

	}
}
}
