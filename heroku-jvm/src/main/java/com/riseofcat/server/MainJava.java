package com.riseofcat.server;

import com.google.gson.Gson;
import com.n8cats.lib.LibAll;
import com.n8cats.share.ClientPayload;
import com.n8cats.share.ServerPayload;
import com.n8cats.share.redundant.ClientSayC;

import spark.Request;
import spark.Response;
import spark.Route;
import spark.Spark;
//import static spark.Spark.*;
//http://sparkjava.com/documentation

public class MainJava {
public static void main(String[] args) {
	String port = java.lang.System.getenv("PORT");
	Spark.port(port != null ? Integer.parseInt(port) : 5000);
	if(false) {
		Spark.threadPool(30, 2, 30_000);
		Spark.webSocketIdleTimeoutMillis(30_000);
	}
	Spark.staticFiles.location("/public");
	Spark.staticFiles.expireTime(600);
	final Gson gson = new Gson();
	Spark.webSocket("/socket", new SparkWebSocket(
			new UsageMonitorDecorator<>(
			new ConvertDecorator<>(
			new PingDecorator<>(
			new RoomsDecorator<ClientPayload, ServerPayload>(TickGame::new), 1000),
			obj -> gson.fromJson(obj, ClientSayC.class),
			gson::toJson))));
	Spark.get("/", new Route() {
		@Override
		public Object handle(Request request, Response response) {
			return LibAll.JSON.toPrettyStr(App.info);
		}
	});
	Spark.init();//Spark.stop();
}
}