package com.riseofcat.server;

import com.n8cats.lib_gwt.Const;
import com.n8cats.lib_gwt.ILog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.management.ManagementFactory;

public class App {
private static Logger logger = LoggerFactory.getLogger("main");
public static ILog log = new ILog() {
	public String info(String s) {
		logger.info(s);
		return s;
	}
	public void error(String s) {
		logger.error(s);
	}
	public void warning(String s) {
		logger.warn(s);
	}
	public void debug(String s) {
		logger.debug(s);
	}
};
public static final Info info = new Info();
public static void breakpoint() {
	int a = 1+1;
}
}
final class Info {
public float getMaxMemory() {
	return Runtime.getRuntime().maxMemory() / Const.MEGA;
}
public float getUsedMemory() {
	return (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / Const.MEGA;
}
public int getCurrentThreads() {
	return Thread.activeCount();
}
public int getAvailableProcessors() {
	return Runtime.getRuntime().availableProcessors();
}
public float getCpuLoad() {
	return (float) ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage();
}
public float getTotalSpace() {
	return new File(".").getTotalSpace()/Const.MEGA;
}
public float getFreeSpace() {
	return new File(".").getFreeSpace()/Const.MEGA;
}
public float averagePing() {
	return 0;//todo
}
public int activeClients() {
	return 0;//todo
}
public float getStackSizeMB() {
	return 0;//todo
	//-Xss set java thread stack size//todo test
	/*-Xss allows to configure Java thread stack size according to application needs:
	larger stack size is for an application that uses recursive algorithms or otherwise deep method calls;
	smaller stack size is for an application that runs thousands of threads - you may want to save memory occupied by thread stacks.
	Bear in mind that HotSpot JVM also utilizes the same Java thread stack for the native methods and JVM runtime calls (e.g. class loading). This means Java thread stack is used not only for Java methods, but JVM should reserve some stack pages for its own operation as well.

	The minimum required stack size is calculated by the formula:

	(StackYellowPages + StackRedPages + StackShadowPages + 2*BytesPerWord + 1) * 4096
	where

	StackYellowPages and StackRedPages are required to detect and handle StackOverflowError;
	StackShadowPages are reserved for native methods;
	2*4 (32-bit JVM) or 2*8 (64-bit JVM) is for VM runtime functions;
	extra 1 is for JIT compiler recursion in main thread;
	4096 is the default page size.
	E.g. for 32-bit Windows JVM minimum stack size = (3 + 1 + 4 + 2*4 + 1) * 4K = 68K

	BTW, you may reduce the minumum required stack size using these JVM options: (not recommended!)

	-XX:StackYellowPages=1 -XX:StackRedPages=1 -XX:StackShadowPages=1*/
}
}
