package com.riseofcat.server

import com.riseofcat.lib.*
import java.io.*
import java.lang.management.*

object App {
  val info = Info()
}

class Info {
  val maxMemory:Float get() = Runtime.getRuntime().maxMemory()/Lib.Const.MEGA
  val usedMemory:Float get() = (Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())/Lib.Const.MEGA
  val currentThreads:Int get() = Thread.activeCount()
  val availableProcessors:Int get() = Runtime.getRuntime().availableProcessors()
  val cpuLoad:Float get() = ManagementFactory.getOperatingSystemMXBean().systemLoadAverage.toFloat()
  val totalSpace:Float get() = File(".").totalSpace/Lib.Const.MEGA
  val freeSpace:Float get() = File(".").freeSpace/Lib.Const.MEGA
  //todo
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
  val stackSizeMB:Float get() = 0f

}
