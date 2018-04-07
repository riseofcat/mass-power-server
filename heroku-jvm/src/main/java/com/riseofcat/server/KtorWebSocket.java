package com.riseofcat.server;

import com.riseofcat.lib.LibJvm;
import com.riseofcat.lib.TypeMap;

import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.eclipse.jetty.websocket.api.WriteCallback;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

import java.io.Reader;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@WebSocket public class KtorWebSocket {
//https://github.com/tipsy/spark-websocket
//http://sparkjava.com/tutorials/websocket-chat
//http://sparkjava.com/documentation#embedded-web-server
private final Map<Session, Ses<String>> map = new ConcurrentHashMap<>();
private static int lastId = 0;
private final SesServ<Reader, String> server;
public KtorWebSocket(SesServ<Reader, String> server) {
  this.server = server;
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
          public void writeFailed(Throwable x) { LibJvm.getLog().error("SparkSession.send.writeFailed", x); }
          public void writeSuccess() { }
        });
      } else LibJvm.getLog().error("session no open", null);
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
  LibJvm.getLog().info("Session closed: ");
  Ses<String> s = map.get(session);
  server.close(s);
  LibJvm.getLog().info("Session id: " + s.getId());
  map.remove(session);
}
//@OnWebSocketMessage public void byteMessage(Session session, byte buf[], int offset, int length)
//@OnWebSocketMessage public void message(Session session, String message) {
@OnWebSocketMessage public void message(Session session, Reader reader) {
  if(!session.isOpen()) {
    LibJvm.getLog().error("SparkWebSocket session not open", null);
    return;
  }
  server.message(map.get(session), reader);
}
@OnWebSocketError public void error(Session session, Throwable error) {
  LibJvm.getLog().error("OnWebSocketError", error);
  if(false) map.get(session).stop();
}

}
