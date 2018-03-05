package com.riseofcat.lib;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.riseofcat.lib_gwt.Const;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LibAll {

public static class JSON {
	private static ObjectMapper objectMapper = new ObjectMapper();
	static {
		if(false) {
			DefaultPrettyPrinter pp = new DefaultPrettyPrinter("\n");
			objectMapper.setDefaultPrettyPrinter(pp);
		}
	}
	public static <T> T toObj(String content, Class<T> valueType) {
		try {
			return objectMapper.readValue(content, valueType);
		} catch(IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	public static String toStr(Object value) {
		try {
			return objectMapper.writeValueAsString(value);
		} catch(JsonProcessingException e) {
			e.printStackTrace();
			return "can't writeValueAsString";
		}
	}
	public static String toPrettyStr(Object value) {
		//todo
//		new Json().prettyPrint() for LibGDX
		return toStr(value);
	}
}

public static NativeCmd nativeCmd(String cmd) {
	return new NativeCmd(cmd);
}

public static class ExecResult {
	public String resultStr = "";
	public String errorStr = "";
	public boolean success;
	public int timeMs;
}

public static class NativeCmd {
	private final String cmd;
	private String path;
	private com.riseofcat.lib_gwt.ILog log;
	private com.riseofcat.lib_gwt.ITerminator terminator;
	private boolean root;

	private NativeCmd(String cmd) {
		this.cmd = cmd;
	}
	public NativeCmd path(String value) {
		this.path = value;
		return this;
	}
	public NativeCmd log(com.riseofcat.lib_gwt.ILog value) {
		this.log = value;
		return this;
	}
	public NativeCmd terminator(com.riseofcat.lib_gwt.ITerminator value) {
		this.terminator = value;
		return this;
	}
	public NativeCmd root(/*String password*/) {//todo password
		this.root = true;
		return this;
	}
	public ExecResult execute() {
		long start = System.currentTimeMillis();
		ExecResult result = new ExecResult();
		try {
			String before = "";
			if(root) {
				before += "su";
			}
			Process proc;
			if(before.length() > 0) {
				if(path != null) {
					proc = Runtime.getRuntime().exec(before, null, new File(path));
				} else {
					proc = Runtime.getRuntime().exec(before);
				}
			} else {
				if(path != null) {
					proc = Runtime.getRuntime().exec(cmd, null, new File(path));
				} else {
					proc = Runtime.getRuntime().exec(cmd);
				}
			}
			BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			BufferedReader stdError = new BufferedReader(new InputStreamReader(proc.getErrorStream()));

			if(before.length() > 0) {
				DataOutputStream dataOutputStream = new DataOutputStream(proc.getOutputStream());
				dataOutputStream.writeBytes(cmd);
				dataOutputStream.flush();
				dataOutputStream.close();
			}

			String s;
			while((s = stdInput.readLine()) != null && (terminator == null || !terminator.terminated())) {
				if(log != null) {
					log.info(s);
				}
				if(result.resultStr.length() > 0) {
					result.resultStr += "\n";
				}
				result.resultStr += s;
			}
			if(result.resultStr.length() > 0) {
				result.success = true;
			}
			String error = "";
			while((s = stdError.readLine()) != null && (terminator == null || !terminator.terminated())) {
				error += s;
			}
			proc.destroy();
//            proc.destroyForcibly();
			if(error.length() > 0) {
				if(log != null) {
					log.error("ERROR : " + error + " in cmd: " + cmd);
				}
				if(result.resultStr.length() == 0) {
					result.success = false;
					result.errorStr = error;
				}
			}
		} catch(IOException e) {
			e.printStackTrace();
		}
		result.timeMs = (int) (System.currentTimeMillis() - start);
		return result;
	}
}

public static void sleep(int time) {
	try {
		Thread.sleep(time);
	} catch(InterruptedException e) {
		e.printStackTrace();
	}
}

public static TextFile textFile(String file) {
	return new TextFile(file);
}

public static class TextFile {

	private final String file;

	private TextFile(String file) {
		this.file = file;
	}

	Float waitSec = 0f;
	public TextFile wait(int ms) {
		waitSec = (float) (ms / 1000);
		return this;
	}
	public TextFile waitInfinity() {
		waitSec = Float.POSITIVE_INFINITY;
		return this;
	}
	public String read() {
		File f = new File(file);
		try {
			BufferedReader br = new BufferedReader(new FileReader(f));
			StringBuilder text = new StringBuilder();
			String line;
			while((line = br.readLine()) != null) {
				text.append(line);
				text.append('\n');
			}
			br.close();
			return text.toString();
		} catch(IOException e) {
			com.riseofcat.lib_gwt.TimeTerminator timeTerminator = new com.riseofcat.lib_gwt.TimeTerminator((int) (waitSec * 1000));
			int sleep = 100;
			while(waitSec.isInfinite() || !timeTerminator.terminated()) {
				LibAll.sleep(sleep *= 2);
				String s = new TextFile(file).read();
				if(s != null) {
					return s;
				}
			}
			return null;
		}
	}

	public boolean write(String text) {
//            File f = new File(file);
		Writer out = null;
		try {
			out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), com.riseofcat.lib_gwt.Const.UTF_8));
			try {
				out.write(text);
				return true;
			} catch(IOException e) {
				e.printStackTrace();
				return false;
			} finally {
				try {
					out.close();
				} catch(IOException e) {
					e.printStackTrace();
				}
			}
		} catch(UnsupportedEncodingException | FileNotFoundException e) {
			e.printStackTrace();
			return false;
		}
//            Writer out = new BufferedWriter(new OutputStreamWriter(
//                    new FileOutputStream("outfilename"), "Const.UTF_8"));
//            try {
//                out.write(aString);
//            } finally {
//                out.close();
//            }
	}

	public boolean exist() {
		return new File(file).exists();
	}

	public String getFullName() {
		return file;
	}
}

public static List<File> listDir(File dir) {
	File[] files = dir.listFiles(new FilenameFilter() {
		@Override
		public boolean accept(File dir, String name) {
			return name.charAt(0) != '.';
		}
	});
	ArrayList<File> result = new ArrayList<>();
	Collections.addAll(result, files);
	return result;
}

public static String getFileOrDirMd5(String flaPath) {
	File file = new File(flaPath);
	if(file.isDirectory()) {
		String summ = "";
		for(File fl : listDir(file)) {
			summ += getFileOrDirMd5(fl.getPath());
		}
		return DigestUtils.md5Hex(summ);
	} else {
		FileInputStream fis;
		try {
			fis = new FileInputStream(file);
			String md5 = DigestUtils.md5Hex(fis);
			fis.close();
			return md5;
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
	return null;
}

public static class HttpRequest {
	private final String url;
	private String body;
	private com.riseofcat.lib_gwt.LibAllGwt.StrArgs args;//todo url args ?
	private com.riseofcat.lib_gwt.ILog log;
	private int attempts = 1;

	private HttpRequest(String url) {
		this.url = url;
	}
	public HttpRequest body(String value) {
		this.body = value;
		return this;
	}
	public HttpRequest log(com.riseofcat.lib_gwt.ILog log) {
		this.log = log;
		return this;
	}
	@SuppressWarnings("Don't support yet args")
	public HttpRequest args(com.riseofcat.lib_gwt.LibAllGwt.StrArgs value) {
		throw new RuntimeException("Don't support yet");
//            this.args = value;
//            return this;
	}
	public HttpRequest attempts(int value) {
		attempts = value;
		return this;
	}
	public Response get() {
		return doRequest("GET", attempts - 1);
	}
	public Response post() {
		return doRequest("POST", attempts - 1);
	}
	private Response doRequest(String method, int currentAttempts) {
		try {
			StringBuilder result = new StringBuilder();
			HttpURLConnection conn = (HttpURLConnection) new URL(this.url).openConnection();
			conn.setRequestMethod(method);
			if(body != null) {
				conn.setDoOutput(true);
				OutputStreamWriter outputStreamWriter = new OutputStreamWriter(conn.getOutputStream(), com.riseofcat.lib_gwt.Const.UTF_8);
				outputStreamWriter.write(body);
				outputStreamWriter.flush();
				outputStreamWriter.close();
//                    outputStream.write(body.getBytes());
//                    outputStream.flush();
//                    outputStream.close();
			}
			BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream(), Const.UTF_8));
			String line;
			while((line = rd.readLine()) != null) {
				result.append(line);
			}
			rd.close();
			Response response = new Response();
			response.success = true;
			response.str = result.toString();
			return response;
		} catch(Exception e) {
			if(currentAttempts > 1) {
				return doRequest(method, currentAttempts - 1);
			} else {
				e.printStackTrace();
				String error = "error in request url " + this.url + " method = " + method + " error message = " + e.getMessage();
				if(log != null) {
					log.error(error);
				}
				Response response = new Response();
				response.success = false;
				response.error = error;
				return response;
			}
		}
	}

	public static class Response {
		public String str;
		public String error;
		public boolean success;
	}
}

public static HttpRequest request(String url) {
	return new HttpRequest(url);
}

public static void killWinProcessByName(String process) {
	LibAll.nativeCmd("taskkill /f /im " + process).execute();
}

public static void runWinProgram(String fileLocation) {
	try {
		File file = new File(fileLocation);
		//todo use naticeCmd instead Runtime
		Runtime.getRuntime().exec(file.getAbsolutePath(), null, file.getParentFile());
	} catch(IOException e) {
		e.printStackTrace();
	}
}

public static void moveFile(File source, File destination) {
	try {
		if(destination.exists()) {
			FileUtils.forceDelete(destination);
		}
		FileUtils.moveFile(source, destination);
	} catch(IOException e) {
		e.printStackTrace();
	}
}

public static void moveDir(File source, File destination) {
	try {
		FileUtils.moveDirectory(source, destination);
	} catch(IOException e) {
		e.printStackTrace();
	}
}

public static void copyDir(File source, File destination) {
	try {
		FileUtils.copyDirectory(source, destination);
	} catch(IOException e) {
		e.printStackTrace();
	}
}

public static void deleteDir(File file) {
	try {
		FileUtils.deleteDirectory(file);
	} catch(IOException e) {
		e.printStackTrace();
	}
}

public static String readBuffer(BufferedReader reader) {
	try {
		// Read from request
		StringBuilder buffer = new StringBuilder();
		String line;
		while((line = reader.readLine()) != null) {
			buffer.append(line);
		}
		return buffer.toString();
	} catch(Exception e) {
		e.printStackTrace();
	}
	return null;
}
public static <T extends Serializable> T copy(T value) {
	return copyByte(value);
}
public static <T extends Serializable> T copyByte(T src) {
	ByteArrayOutputStream outByteStream = null;
	ObjectOutputStream outStream = null;
	ObjectInputStream inStream = null;
	ByteArrayInputStream inByteStream = null;
	try{
		outByteStream = new ByteArrayOutputStream();
		outStream = new ObjectOutputStream(outByteStream);
		outStream.writeObject(src);
		outStream.flush();//todo redundant?
		inByteStream = new ByteArrayInputStream(outByteStream.toByteArray());
		inStream = new ObjectInputStream(inByteStream);
		Object result = inStream.readObject();
		return (T) result;
	} catch(IOException | ClassNotFoundException e) {
		e.printStackTrace();//todo
	} finally {
		if(outByteStream != null) {
			try {
				outByteStream.close();//todo redundant?
			} catch(IOException e) {
				e.printStackTrace();
			}
		}
		if(outStream != null) {
			try {
				outStream.close();
			} catch(IOException e) {
				e.printStackTrace();
			}
		}
		if(inByteStream != null) {
			try {
				inByteStream.close();//todo redundant?
			} catch(IOException e) {
				e.printStackTrace();
			}
		}
		if(inStream != null) {
			try {
				inStream.close();
			} catch(IOException e) {
				e.printStackTrace();
			}
		}
	}
	return null;
}


}
