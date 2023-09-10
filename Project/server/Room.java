package Project.server;

import Project.common.Constants;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Room implements AutoCloseable {

	// protected static Server server;// used to refer to accessible server
	// functions
	private String name;
	protected List<ServerThread> clients = new ArrayList<ServerThread>();
	private boolean isRunning = false;
	private MuteListHandler muteListHandler;
	// Commands
	private static final String COMMAND_TRIGGER = "/";
	private static final String CREATE_ROOM = "createroom";
	private static final String JOIN_ROOM = "joinroom";
	private static final String DISCONNECT = "disconnect";
	private static final String LOGOUT = "logout";
	private static final String LOGOFF = "logoff";
	private static final String ROLL = "roll";
	private static final String FLIP = "flip";
	private static Logger logger = Logger.getLogger(Room.class.getName());

	public Room(String name) {
		this.name = name;
		isRunning = true;
	}

	public void setMuteListHandler(MuteListHandler muteListHandler) {
		this.muteListHandler = muteListHandler;
	}

	public String getName() {
		return name;
	}

	public boolean isRunning() {
		return isRunning;
	}

	protected synchronized void addClient(ServerThread client) {
		logger.info("Room addClient called");
		if (!isRunning) {
			return;
		}
		client.setCurrentRoom(this);
		if (clients.indexOf(client) > -1) {
			logger.warning("Attempting to add a client that already exists in room");
		} else {
			clients.add(client);
			sendConnectionStatus(client, true);
			sendRoomJoined(client);
			sendUserListToClient(client);
		}
	}

	protected synchronized void removeClient(ServerThread client) {
		if (!isRunning) {
			return;
		}
		clients.remove(client);
		// we don't need to broadcast it to the server
		// only to our own Room
		if (clients.size() > 0) {
			// sendMessage(client, "left the room");
			sendConnectionStatus(client, false);
		}
		checkClients();
	}

	/*
	 * Checks the number of clients.
	 * If zero, begins the cleanup process to dispose of the room.
	 */
	private void checkClients() {
		// cleanup if room is empty and not the lobby
		if (!name.equalsIgnoreCase("lobby") &&
				(clients == null || clients.size() == 0)) {
			close();
		}
	}

	public long getClientIdByUsername(String targetUsername) {
		Iterator<ServerThread> iter = clients.iterator();
		while (iter.hasNext()) {
			ServerThread client = iter.next();
			if (client.getClientName().equalsIgnoreCase(targetUsername)) {
				ServerThread target = client;
				return target.getClientId();
			}
		}
		return Constants.DEFAULT_CLIENT_ID; // Return default clientId if not found
	}

	/***
	 * Helper function to process messages to trigger different functionality.
	 *
	 * @param message The original message being sent
	 * @param client  The sender of the message (since they'll be the ones
	 *                triggering the actions)
	 */
	private boolean processCommands(String message, ServerThread client) {
		boolean wasCommand = false;
		try {
			if (message.startsWith(COMMAND_TRIGGER)) {
				String[] comm = message.split(COMMAND_TRIGGER);
				String part1 = comm[1];
				String[] comm2 = part1.split(" ");
				String command = comm2[0];
				String roomName;
				wasCommand = true;
				switch (command) {
					case CREATE_ROOM:
						roomName = comm2[1];
						Room.createRoom(roomName, client);
						break;
					case JOIN_ROOM:
						roomName = comm2[1];
						Room.joinRoom(roomName, client);
						break;
					case ROLL:
						break;
					case FLIP:
						break;
					case DISCONNECT:
					case LOGOUT:
					case LOGOFF:
						Room.disconnectClient(client, this);
						break;
					default:
						wasCommand = false;
						break;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return wasCommand;
	}

	// Command helper methods
	protected static void getRooms(String query, ServerThread client) {
		String[] rooms = Server.INSTANCE.getRooms(query).toArray(new String[0]);
		client.sendRoomsList(
				rooms,
				(rooms != null && rooms.length == 0)
						? "No rooms found containing your query string"
						: null);
	}

	protected static void createRoom(String roomName, ServerThread client) {
		if (Server.INSTANCE.createNewRoom(roomName)) {
			// server.joinRoom(roomName, client);
			Room.joinRoom(roomName, client);
		} else {
			client.sendMessage(
					Constants.DEFAULT_CLIENT_ID,
					String.format("Room %s already exists", roomName));
			client.sendRoomsList(
					null,
					String.format("Room %s already exists", roomName));
		}
	}

	/***
	 * Will cause the client to leave the current room
	 * and be moved to the new room if applicable
	 *
	 * @param roomName
	 * @param client
	 */
	protected static void joinRoom(String roomName, ServerThread client) {
		if (!Server.INSTANCE.joinRoom(roomName, client)) {
			client.sendMessage(
					Constants.DEFAULT_CLIENT_ID,
					String.format("Room %s doesn't exist", roomName));
			client.sendRoomsList(
					null,
					String.format("Room %s already exists", roomName));
		}
	}

	protected static void disconnectClient(ServerThread client, Room room) {
		client.setCurrentRoom(null);
		client.disconnect();
		room.removeClient(client);
	}

	// end command helper methods

	/***
	 * Takes a sender and a message and broadcasts the message to all clients in
	 * this room. Client is mostly passed for command purposes but we can also use
	 * it to extract other client info.
	 *
	 * @param sender  The client sending the message
	 * @param message The message to broadcast inside the room
	 */
	protected synchronized void sendMessage(ServerThread sender, String message) {
		if (!isRunning) {
			return;
		}
		logger.info(String.format("Sending message to %s clients", clients.size()));
		if (sender != null && processCommands(message, sender)) {
			// it was a command, don't broadcast
			return;
		}

		long from = (sender == null ? Constants.DEFAULT_CLIENT_ID : sender.getClientId());
		Iterator<ServerThread> iter = clients.iterator();
		while (iter.hasNext()) {
			ServerThread client = iter.next();
			String clientName = sender.getClientName();
			String targetName = client.getClientName();
			if (!muteListHandler.isClientMuted(clientName, targetName)) {
				System.out.println(
						"Sending message to client: " + client.getClientName());
				boolean messageSent = client.sendMessage(from, message);
				if (!messageSent) {
					handleDisconnect(iter, client);
				}
			} else {
				System.out.println(
						"Skipping message to muted client: " + client.getClientName()); // Print the muted client being
																						// skipped
			}
		}
	}

	/**
	 * Method takes specified target client and iterates through
	 * list until target is found, message is only sent to that client.
	 *
	 * @param sender
	 * @param message
	 */
	protected synchronized void sendDirectMessage(
			ServerThread sender,
			String message) {
		if (!isRunning) {
			return;
		}
		message = message.replace("@", "").trim();
		String[] messageToTarget = message.split(" ", 2);
		String targetClientName = messageToTarget[0];
		message = messageToTarget[1];
		logger.info(
				String.format("Sending private message to %s", targetClientName));
		if (sender != null && processCommands(message, sender)) {
			// it was a command, don't broadcast
			return;
		}

		long from = (sender == null ? Constants.DEFAULT_CLIENT_ID : sender.getClientId());
		Iterator<ServerThread> iter = clients.iterator();
		while (iter.hasNext()) {
			ServerThread client = iter.next();
			if (client.getClientName().equalsIgnoreCase(targetClientName)) {
				ServerThread target = client;
				String clientName = (sender == null ? null : sender.getClientName());
				String targetName = client.getClientName();
				if (!muteListHandler.isClientMuted(clientName, targetName)) {
					System.out.println(
							"Sending direct message to client: " + client.getClientName());
					boolean messageSent = target.sendMessage(from, message);
					if (!messageSent) {
						sendMessage(
								null,
								String.format("%s disconnected", targetClientName));
						checkClients();
					}
				} else {
					logger.info("Client muted, message not sent.");
				}
			}
		}
	}

	/***
	 * Takes a sender and a message and attempts to broadcast the message to all
	 * clients in this room as a message from the server. Client is mostly passed for command
	 * purposes but we can also use it to extract other client info.
	 *
	 * @param sender
	 * @param message
	 */
	protected synchronized void sendAnnouncement(
			ServerThread sender,
			String message) {
		if (!isRunning) {
			return;
		}
		logger.info(
				String.format("Sending announcement to %s clients", clients.size()));
		if (sender != null && processCommands(message, sender)) {
			// it was a command, don't broadcast
			return;
		}
		message = String.format("<b>%s</b>", message);
		long from = Constants.DEFAULT_CLIENT_ID;
		Iterator<ServerThread> iter = clients.iterator();
		while (iter.hasNext()) {
			ServerThread client = iter.next();
			boolean messageSent = client.sendMessage(from, message);
			if (!messageSent) {
				handleDisconnect(iter, client);
			}
		}
	}

	/**
	 * Attempts to identify mute/unmute commands, converts the sender and target
	 * Threads to ClientNames
	 * and passes them to the appropriate mute/unmute method
	 * in the MuteListHandler class.
	 *
	 * @param sender  the ServerThread of the client who is muting/unmuting
	 * @param message the message containing the mute/unmute command and the target
	 */
	public synchronized void setClientMuteStatus(ServerThread sender, String message) {
		Iterator<ServerThread> iter = clients.iterator();
		String[] muteTarget = message.split(" ", 3);
		String targetClientName = muteTarget[1];
		try{
			if (message.startsWith("mute")) {
				while (iter.hasNext()) {
					ServerThread potentialMuteTarget = iter.next();
					if (potentialMuteTarget.getClientName().equalsIgnoreCase(targetClientName)) {
						muteListHandler.muteClient(sender.getClientName(), targetClientName);
						sender.sendMuteChange(targetClientName, sender.getClientName());
					}
				}
			} else if (message.startsWith("unmute")) {
				while (iter.hasNext()) {
					ServerThread potentialUnmuteTarget = iter.next();
					if (potentialUnmuteTarget.getClientName().equalsIgnoreCase(targetClientName)) {
						muteListHandler.unmuteClient(sender.getClientName(),targetClientName);
						sender.sendMuteChange(targetClientName,sender.getClientName());
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected synchronized void sendUserListToClient(ServerThread receiver) {
		logger.log(
				Level.INFO,
				String.format(
						"Room[%s] Syncing client list of %s to %s",
						getName(),
						clients.size(),
						receiver.getClientName()));
		synchronized (clients) {
			Iterator<ServerThread> iter = clients.iterator();
			while (iter.hasNext()) {
				ServerThread clientInRoom = iter.next();
				if (clientInRoom.getClientId() != receiver.getClientId()) {
					boolean messageSent = receiver.sendExistingClient(
							clientInRoom.getClientId(),
							clientInRoom.getClientName());
					// receiver somehow disconnected mid iteration
					if (!messageSent) {
						handleDisconnect(null, receiver);
						break;
					}
				}
			}
		}
	}

	protected synchronized void sendRoomJoined(ServerThread receiver) {
		boolean messageSent = receiver.sendRoomName(getName());
		if (!messageSent) {
			handleDisconnect(null, receiver);
		}
	}

	protected synchronized void sendConnectionStatus(
			ServerThread sender,
			boolean isConnected) {
		Iterator<ServerThread> iter = clients.iterator();
		while (iter.hasNext()) {
			ServerThread receivingClient = iter.next();
			boolean messageSent = receivingClient.sendConnectionStatus(
					sender.getClientId(),
					sender.getClientName(),
					isConnected);
			if (!messageSent) {
				handleDisconnect(iter, receivingClient);
			}
		}
	}

	private void handleDisconnect(
			Iterator<ServerThread> iter,
			ServerThread client) {
		if (iter != null) {
			iter.remove();
		} else {
			Iterator<ServerThread> iter2 = clients.iterator();
			while (iter2.hasNext()) {
				ServerThread th = iter2.next();
				if (th.getClientId() == client.getClientId()) {
					iter2.remove();
					break;
				}
			}
		}
		logger.info(String.format("Removed client %s", client.getClientName()));
		sendMessage(null, client.getClientName() + " disconnected");
		checkClients();
	}

	public void close() {
		Server.INSTANCE.removeRoom(this);
		isRunning = false;
		clients.clear();
	}
}
