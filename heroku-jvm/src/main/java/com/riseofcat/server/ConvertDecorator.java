package com.riseofcat.server;
import com.riseofcat.lib.TypeMap;
import com.riseofcat.lib_gwt.IConverter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ConvertDecorator<CSay, SSay, CCode, SCode> extends SesServ<CCode, SCode> {
private final IConverter<CCode, CSay> cConv;
private final IConverter<SSay, SCode> sConv;
private final Map<SesServ.Ses, SesServ<CSay, SSay>.Ses> map = new ConcurrentHashMap<>();
private final SesServ<CSay, SSay> server;
public ConvertDecorator(SesServ<CSay, SSay> server, IConverter<CCode, CSay> cConv, IConverter<SSay, SCode> sConv) {
	this.cConv = cConv;
	this.sConv = sConv;
	this.server = server;
}
public void start(Ses session) {
	SesServ<CSay, SSay>.Ses s = server.new Ses() {
		public int getId() {
			return session.getId();
		}
		public void stop() {
			session.stop();
		}
		public void send(SSay data) {
			session.send(sConv.convert(data));
		}
		protected TypeMap getTypeMap() {
			return session.getTypeMap();
		}
	};
	map.put(session, s);
	server.start(s);
}
public void close(Ses sess) {
	server.close(map.get(sess));
	map.remove(sess);
}
public void message(Ses ses, CCode code) {
	server.message(map.get(ses), cConv.convert(code));
}

}
