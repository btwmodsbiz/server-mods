package btwmod.centralchat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;

import org.java_websocket.WebSocket;
import org.java_websocket.drafts.Draft;
import org.java_websocket.framing.CloseFrame;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import btwmod.centralchat.message.Message;
import btwmod.centralchat.message.MessageConnect;
import btwmod.centralchat.message.MessageDisconnect;
import btwmod.centralchat.message.MessageGatewayDisconnect;
import btwmod.centralchat.message.MessageUserInfo;
import btwmod.centralchat.message.MessageUserInfoList;
import btwmod.centralchat.struct.User;

public class WSServer extends WebSocketServer {
	
	//public static final String PROTOCOL_NAME = "btw-json";
	
	private final IServer serverController;

	public WSServer(IServer serverController) throws IOException {
		super();
		this.serverController = serverController;
		init();
	}

	public WSServer(IServer serverController, InetSocketAddress address, int decodercount, List<Draft> drafts) {
		super(address, decodercount, drafts);
		this.serverController = serverController;
		init();
	}

	public WSServer(IServer serverController, InetSocketAddress address, int decoders) {
		super(address, decoders);
		this.serverController = serverController;
		init();
	}

	public WSServer(IServer serverController, InetSocketAddress address, List<Draft> drafts) {
		super(address, drafts);
		this.serverController = serverController;
		init();
	}

	public WSServer(IServer serverController, InetSocketAddress address) {
		super(address);
		this.serverController = serverController;
		init();
	}
	
	protected void init() {
		
	}

	@Override
	public void onOpen(WebSocket conn, ClientHandshake handshake) {
		System.out.println(conn.getRemoteSocketAddress().getAddress().getHostAddress() + " connected to " + conn.getResourceDescriptor());
		
		/*String protocol = handshake.getFieldValue("Sec-WebSocket-Protocol");
		if (!PROTOCOL_NAME.equalsIgnoreCase(protocol) && false) {
			conn.close(CloseFrame.PROTOCOL_ERROR, "Protocol must be " + PROTOCOL_NAME);
			return
		}*/
		
		ResourceConfig config = ResourceConfig.parse(conn.getResourceDescriptor());
		if (validateConn(conn, config)) {
			
			// More than one gateway key cannot be used at the same time.
			if (config.clientType == ClientType.GATEWAY && serverController.hasConnectedClient(conn, config)) {
				conn.close(CloseFrame.NORMAL, CloseMessage.ALREADY_CONNECTED.toString());
			}
			
			else {
				if (config.clientType == ClientType.USER) {
					serverController.disconnectSameClient(conn, config);
					conn.send(MessageUserInfoList.build(serverController).toJson().toString());
					new MessageConnect(serverController.getActualUsername(config.id), null, serverController.getChatColor(config.id), serverController.getChatAlias(config.id)).handleAsServer(serverController, conn, config);
				}
				
				serverController.onOpen(conn, config);
			}
		}
	}
	
	protected boolean validateConn(WebSocket conn, ResourceConfig config) {
		if (!serverController.isValidConfig(config)) {
			conn.close(CloseFrame.NORMAL, CloseMessage.INVALID_CREDENTIALS.toString());
			return false;
		}
		
		return true;
	}

	@Override
	public void onClose(WebSocket conn, int code, String reason, boolean remote) {
		System.out.println(conn.getRemoteSocketAddress().getAddress().getHostAddress() + " has disconnected" + (reason == null || reason.equals("") ? " (no reason)." : ": " + reason));

		ResourceConfig config = ResourceConfig.parse(conn.getResourceDescriptor());
		if (serverController.isValidConfig(config)) {
			if (config.clientType == ClientType.USER) {
				new MessageDisconnect(config.id, null, null).handleAsServer(serverController, conn, config);
			}
			else if (config.clientType == ClientType.GATEWAY) {
				User[] users = serverController.getLoggedInUserList(config.id);
				serverController.removeLoggedInUsers(config.id);
				
				MessageUserInfo[] cleanedUsers = new MessageUserInfo[users.length];
				for (int i = 0, len = users.length; i < len; i++) {
					cleanedUsers[i] = new MessageUserInfo(serverController.getActualUsername(users[i].username), null, null, null);
				}
				 
				 new MessageGatewayDisconnect(config.id, cleanedUsers).handleAsServer(serverController, conn, config);
			}
		}
		serverController.onClose(conn, config);
	}

	@Override
	public void onMessage(WebSocket conn, String rawMessage) {
		try {
			ResourceConfig config = ResourceConfig.parse(conn.getResourceDescriptor());
			if (validateConn(conn, config)) {
				Message message = Message.parse(rawMessage);
				
				if (message != null && message.canSendMessage(config)) {
					if (message.includeInLogs())
						System.out.println("<" + conn.getRemoteSocketAddress() + "> " + message.toJson());
					
					if (message.canSendMessage(config))
						message.handleAsServer(serverController, conn, config);
				}
				else {
					System.err.println(conn.getRemoteSocketAddress() + " sent invalid message: " + rawMessage);
					conn.close(CloseFrame.PROTOCOL_ERROR, "Invalid message received.");
				}
			}
		}
		catch (RuntimeException e) {
			System.err.println(conn.getRemoteSocketAddress() + " sent a message that caused an exception (" + e.getClass().getSimpleName() + "): " + e.getMessage());
			e.printStackTrace();
		}
	}

	@Override
	public void onError(WebSocket conn, Exception ex) {
		ex.printStackTrace();
	}
}
