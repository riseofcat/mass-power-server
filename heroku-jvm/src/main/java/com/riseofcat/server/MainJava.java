package com.riseofcat.server;

import com.riseofcat.lib.LibJava;
import com.riseofcat.share.mass.ClientPayload;
import com.riseofcat.share.mass.ServerPayload;

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
	Spark.webSocket("/socket", new SparkWebSocket(
			new UsageMonitorDecorator<>(
			new ConvertDecorator<>(
			new PingDecorator<>(
			new RoomsDecorator<ClientPayload, ServerPayload>(TickGame::new), 1000),
			obj -> Util.Companion.fromJsonClientSay(obj),
			ss->Util.Companion.toServerSayJson(ss)))));
	Spark.get("/", new Route() {
		@Override
		public Object handle(Request request, Response response) {
			return LibJava.info();
		}
	});
	Spark.init();//Spark.stop();
}
}
