package com.n8cats.share;

import java.util.ArrayList;

public class ClientPayload {
public int tick;
public ArrayList<ClientAction> actions;

public static class ClientAction {
	public int aid;//Если действия будут отложены или не применимы то сервер сообщит по id-шнику. Рецеркулировать от 0 до 255
	public int wait;
	public int tick;//tick = payload.tick + wait//todo redundant
	public Logic.Action action;
}

}
