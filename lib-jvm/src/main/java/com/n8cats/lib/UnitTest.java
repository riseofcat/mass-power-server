package com.n8cats.lib;

import com.n8cats.lib_gwt.LibAllGwt;

public class UnitTest {
public static void main(String[] args) {
	try {
		String ls = LibAll.nativeCmd("ls").execute().resultStr;
		breakpoint();
	} catch(Exception e) {
		e.printStackTrace();
	}
	breakpoint();
	boolean ip = LibAllGwt.isIp("127.0.0.1");
	boolean ip2 = LibAllGwt.isIp("a127.0.0.1");
	breakpoint();
	String s = LibAllGwt.putStrArgs("my name is {name}", new LibAllGwt.StrArgs().put("name", "Bobr"));
	LibAll.HttpRequest.Response response = LibAll.request("http://127.0.0.1:54322/jre?action=start").body("{\"redundant\":0}").post();
	breakpoint();
}

public static void breakpoint() {
	int a = 1 + 1;
}

}
