package btwmod.centralchat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.java_websocket.WebSocket;
import org.java_websocket.framing.CloseFrame;

import btwmod.centralchat.message.MessagePing;
import btwmod.centralchat.message.MessagePong;

public class PingManager implements Runnable {
	
	public final IServer server;
	public final long pingFrequencySeconds;
	public final long timeoutSeconds;
	
	protected final Map<WebSocket, Long> lastPongTime = new HashMap<WebSocket, Long>();
	
	public PingManager(IServer server, long pingFrequencySeconds, long timeoutSeconds) {
		this.pingFrequencySeconds = pingFrequencySeconds;
		this.server = server;
		this.timeoutSeconds = timeoutSeconds;
	}

	@Override
	public void run() {
		long tick = 0;
		while (!Thread.interrupted()) {
			if (tick++ % pingFrequencySeconds == 0) {
				WebSocket[] connections = server.getConnections();
				
				// Send pings
				for (WebSocket connection : connections) {
					try {
						connection.send(new MessagePing().toJson().toString());
					}
					catch (RuntimeException e) {
						
					}
				}
				
				// Clean up old connection entries.
				List<WebSocket> disconnectList = new ArrayList<WebSocket>();
				System.out.println("Checking time...");
				synchronized (lastPongTime) {
					for (Entry<WebSocket, Long> entry : lastPongTime.entrySet()) {
						System.out.println("Checking time: " + (System.currentTimeMillis() - entry.getValue().longValue()) + " > " + (timeoutSeconds * 1000L));
						if (System.currentTimeMillis() - entry.getValue().longValue() > timeoutSeconds * 1000L) {
							disconnectList.add(entry.getKey());
						}
					}
				}
				
				for (WebSocket conn : disconnectList) {
					try {
						conn.close(CloseFrame.NORMAL, CloseMessage.PONG_TIMEOUT.toString());
					}
					catch (RuntimeException e) {
						
					}
				}
			}
			
			try {
				Thread.sleep(1000L);
			} catch (InterruptedException e) {
				
			}
		}
	}
	
	public void onOpen(WebSocket conn) {
		synchronized (lastPongTime) {
			lastPongTime.put(conn, System.currentTimeMillis());
		}
	}
	
	public void onClose(WebSocket conn) {
		synchronized (lastPongTime) {
			lastPongTime.remove(conn);
		}
	}

	public void onPing(WebSocket conn, MessagePing ping) {
		
	}

	public void onPong(WebSocket conn, MessagePong pong) {
		synchronized (lastPongTime) {
			if (lastPongTime.containsKey(conn)) {
				lastPongTime.put(conn, System.currentTimeMillis());
			}
		}
	}
}
