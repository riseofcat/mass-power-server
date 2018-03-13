package com.riseofcat.server;

import com.riseofcat.lib.LibJava;
import com.riseofcat.lib.TypeMap;

import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WriteCallback;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

import java.io.Reader;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@WebSocket public class SparkWebSocket {
//https://github.com/tipsy/spark-websocket
//http://sparkjava.com/tutorials/websocket-chat
//http://sparkjava.com/documentation#embedded-web-server
private final Map<Session, Ses<String>> map = new ConcurrentHashMap<>();
private static int lastId = 0;
private final SesServ<Reader, String> server;
public SparkWebSocket(SesServ<Reader, String> server) {
  this.server = server;
}
private void todo(Session session) {//todo
  session.suspend().resume();
  session.getRemoteAddress();//client
  session.getRemote().getBatchMode();//AUTO by default
}
@OnWebSocketConnect public void connected(Session session) {
  Ses<String> s = new Ses<String>() {
    private int id = ++lastId;
    private TypeMap typeMap;
    public int getId() { return id; }
    public void stop() {
      session.close();
    }
    public void send(String message) {
      if(session.isOpen()) {
        RemoteEndpoint remote = session.getRemote();
        remote.sendString(message, new WriteCallback() {
          public void writeFailed(Throwable x) { LibJava.getLog().error("SparkSession.send.writeFailed", x); }
          public void writeSuccess() { }
        });
      }
    }
    public TypeMap getTypeMap() {
      if(typeMap == null) typeMap = new TypeMap();
      return typeMap;
    }
  };
  map.put(session, s);
  server.start(s);
}
@OnWebSocketClose public void closed(Session session, int statusCode, String reason) {
  LibJava.getLog().info("Session closed: ");
  Ses<String> s = map.get(session);
  server.close(s);
  LibJava.getLog().info("Session id: " + s.getId());
  map.remove(session);
}
//@OnWebSocketMessage public void byteMessage(Session session, byte buf[], int offset, int length)
//@OnWebSocketMessage public void message(Session session, String message) {
@OnWebSocketMessage public void message(Session session, Reader reader) {
  if(!session.isOpen()) {
    LibJava.getLog().error("SparkWebSocket session not open", null);
    return;
  }
  server.message(map.get(session), reader);
}
@OnWebSocketError public void error(Session session, Throwable error) {
  LibJava.getLog().error("OnWebSocketError", error);
  if(false) map.get(session).stop();
}

}
