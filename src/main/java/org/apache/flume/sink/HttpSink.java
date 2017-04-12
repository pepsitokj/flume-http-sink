package org.apache.flume.sink;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.net.UnknownHostException;

import org.apache.flume.Channel;
import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.EventDeliveryException;
import org.apache.flume.Transaction;
import org.apache.flume.conf.Configurable;
import org.apache.log4j.Logger;
import org.apache.flume.sink.AbstractSink;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * @author jose alvarez muguerza
 */
public class HttpSink extends AbstractSink implements Configurable {
	private String wsURL;

	private static final Logger LOG = Logger.getLogger(HttpSink.class);
	private static final JsonParser JSON_PARSER = new JsonParser();
	private static String HOST ;
	private static final String ARRAY_START = "[";
	private static final String ARRAY_END = "]";
	
	static {
		try {
			HOST = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void configure(Context context) {
		String wsEndpoint = context.getString("endpoint", "http://commonapi.huizhuang.com/dingtalk/User/sendTextMsgByRobot.do");
		String wsPort = context.getString("port", "80");
		validateFullEndpoint(wsEndpoint, wsPort);

		this.wsURL = wsEndpoint + ":" + wsPort;
	}

	@Override
	public void start() {
		// Initialize the connection to the external repository (e.g. HDFS) that
		// this Sink will forward Events to ..
		LOG.debug("Start method. Open new connection");
		return;
	}

	@Override
	public void stop() {
		// Disconnect from the external respository and do any
		// additional cleanup (e.g. releasing resources or nulling-out
		// field values) ..
		LOG.debug("Stop method. Closed connection");

		return;
	}

	public Status process() throws EventDeliveryException {
		Status status = null;
		StringBuffer stringBuffer = new StringBuffer();
		// Start transaction
		Channel ch = getChannel();
		Transaction txn = ch.getTransaction();
		txn.begin();
		try {
			Event event = ch.take();
			LOG.debug(event);
			String message = URLEncoder.encode(new String(event.getBody()),
					"UTF-8");
			LOG.debug(message);
			URL serverURL = new URL(this.wsURL);
			LOG.debug(serverURL);
			char[] jsonMessage = convertToJSON(message);
			LOG.debug(jsonMessage);
			stringBuffer
			.append("hook")
			.append("=")
			.append(URLEncoder.encode("ac59f95548c74adeaf03d9fbb66a02657f87b578ad9bbce977018e8304623b75", "utf-8"))
			.append("&")
			.append("msg")
			.append("=")
			.append(message);
			char[] mydata = stringBuffer.toString().toCharArray();
			LOG.debug(mydata);
			LOG.debug("New event ready: " + new String(jsonMessage) + " --> "
					+ wsURL);
//            if(message.contains("at")|message.contains("java.lang.NullPointerException")|message.contains("")){
			HttpURLConnection connection = (HttpURLConnection) serverURL
					.openConnection();
			connection.setDoOutput(true);
			connection.setRequestMethod("POST");
			OutputStreamWriter wr = new OutputStreamWriter(
					connection.getOutputStream());

			wr.write(mydata);
			wr.flush();
			connection.getInputStream();
			connection.disconnect();
			LOG.debug("message sucessfuly sent");
//			}
			txn.commit();
			status = Status.READY;
		
		} catch (Throwable t) {
			txn.rollback();

			LOG.error(t.getMessage());

			status = Status.BACKOFF;

			// re-throw all Errors
			if (t instanceof Error) {
				throw (Error) t;
			}
		} finally {
			txn.close();
		}
		return status;
	}

	private char[] convertToJSON(final String message) {
		JsonObject jsonObject = (JsonObject)JSON_PARSER.parse(
				"{\"headers\" : {\"timestamp\" : \""
						+ System.currentTimeMillis() + "\",\"host\" : \""
						+ HOST + "\"},\"body\" : \"" + message + "\" }");
		String result =  ARRAY_START + jsonObject.toString()+ ARRAY_END;
		return result.toCharArray() ;

	}

	private void validateFullEndpoint(final String endpoint, final String port) {
		try {
			URL url = new URL(endpoint + ":" + port);
			URLConnection conn = url.openConnection();
			conn.connect();
		} catch (MalformedURLException e) {
			String errMsg = "Web Service endpoint is malformed: (" + endpoint
					+ ":" + port + ")";
			LOG.error(errMsg);
			new EventDeliveryException(errMsg);
		} catch (IOException e) {
			String errMsg = "Web Service endpoint is not valid: (" + endpoint
					+ ":" + port + ")";
			LOG.error(errMsg);
			new IOException(errMsg);
		}
	}

}
