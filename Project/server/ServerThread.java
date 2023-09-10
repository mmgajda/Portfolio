package Project.server;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import Project.common.Constants;
import Project.common.Payload;
import Project.common.PayloadType;
import Project.common.RoomResultPayload;

/**
 * A server-side representation of a single client
 */
public class ServerThread extends Thread {
    private Socket client;
    private String clientName;
    private boolean isRunning = false;
    private HashMap<ServerThread, Set<ServerThread>> mutedUsers;
    protected boolean isMuted = false;
    private ObjectOutputStream out;// exposed here for send()
    // private Server server;
    // ref to our server so we can call methods on it more easily
    private Room currentRoom;
    private static Logger logger = Logger.getLogger(ServerThread.class.getName());
    private long myClientId; 
    private String formattedMessage;
    private String gameResult;


    public void setClientId(long id) {
        myClientId = id;
    }

    public long getClientId() {
        return myClientId;
    }

    public boolean isRunning() {
        return isRunning;
    }
    
    private void info(String message) {
        System.out.println(String.format("Thread[%s]: %s", getId(), message));
    }


    public ServerThread(Socket myClient, Room room) {
        info("ServerThread created");
        // get communication channels to single client
        this.client = myClient;
        this.currentRoom = room;
        this.mutedUsers = new HashMap<>();
    }

    protected void setClientName(String name) {
        if (name == null || name.isBlank()) {
            System.err.println("Invalid client name being set");
            return;
        }
        clientName = name;
    }

    protected String getClientName() {
        return clientName;
    }

    protected synchronized Room getCurrentRoom() {
        return currentRoom;
    }

    protected synchronized void setCurrentRoom(Room room) {
        if (room != null) {
            currentRoom = room;
        } else {
            info("Passed in room was null, this shouldn't happen");
        }
    }

    public void disconnect() {
        sendConnectionStatus(myClientId,getClientName(), false);
        info("Thread being disconnected by server");
        isRunning = false;
        cleanup();
    }
    // send methods

    public boolean sendRoomName(String name) {
        Payload p = new Payload();
        p.setPayloadType(PayloadType.JOIN_ROOM);
        p.setMessage(name);
        return send(p);
    }

    public boolean sendRoomsList(String[] rooms, String message) {
        RoomResultPayload payload = new RoomResultPayload();
        payload.setRooms(rooms);
        if (message != null) {
            payload.setMessage(message);
        }
        return send(payload);
    }

    public boolean sendExistingClient(long clientId, String clientName) {
        Payload p = new Payload();
        p.setPayloadType(PayloadType.SYNC_CLIENT);
        p.setClientId(clientId);
        p.setClientName(clientName);
        return send(p);
    }

    public boolean sendResetUserList() {
        Payload p = new Payload();
        p.setPayloadType(PayloadType.RESET_USER_LIST);
        return send(p);
    }

    public boolean sendClientId(long id) {
        Payload p = new Payload();
        p.setPayloadType(PayloadType.CLIENT_ID);
        p.setClientId(id);
        return send(p);
    }

    public boolean sendMessage(long clientId, String message) {
        Payload p = new Payload();
        p.setPayloadType(PayloadType.MESSAGE);
        p.setClientId(clientId);
        p.setMessage(message);
        return send(p);
    }

    public void sendMuteChange(String target, String client) throws IOException {
        Payload p = new Payload();
        p.setPayloadType(PayloadType.MUTE_CLIENT);
        p.setClientName(client);
        p.setMessage(target);
        send(p);
    }

    public boolean sendConnectionStatus(long clientId, String who, boolean isConnected) {
        Payload p = new Payload();
        p.setPayloadType(isConnected ? PayloadType.CONNECT : PayloadType.DISCONNECT);
        p.setClientId(clientId);
        p.setClientName(who);
        p.setMessage(isConnected ? "connected" : "disconnected");
        return send(p);
    }

    public boolean sendRoll(long clientId, String diceResult) {
        Payload p = new Payload();
        p.setPayloadType(PayloadType.ROLL);
        p.setClientId(clientId);
        p.setMessage(diceResult);
        return send(p);
    }

    public boolean sendFlip(long clientId, String flipResult) {
        Payload p = new Payload();
        p.setPayloadType(PayloadType.FLIP);
        p.setClientId(clientId);
        p.setMessage(flipResult);
        return send(p);
    }

    private boolean send(Payload payload) {
        // added a boolean so we can see if the send was successful
        try {
            logger.log(Level.FINE, "Outgoing payload: " + payload);
            out.writeObject(payload);
            logger.log(Level.INFO, "Sent payload: " + payload);
            return true;
        } catch (IOException e) {
            logger.info("Error sending message to client (most likely disconnected)");
            // comment this out to inspect the stack trace
            // e.printStackTrace();
            cleanup();
            return false;
        } catch (NullPointerException ne) {
            logger.info("Message was attempted to be sent before outbound stream was opened: " + payload);
            return true;// true since it's likely pending being opened
        }
    }
    // end send methods
    
    @Override
    public void run() {
        info("Thread Starting");
        try (ObjectOutputStream out = new ObjectOutputStream(client.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(client.getInputStream());) {
            this.out = out;
            isRunning = true;
            Payload fromClient;
            while (isRunning && // flag to let us easily control the loop
                    (fromClient = (Payload) in.readObject()) != null // reads an object from inputStream 
                                                                     //(null would likely mean a disconnect)
            ) {
                info("Received from client: " + fromClient);
                processPayload(fromClient);
            } // close while loop
        } catch (Exception e) {
            // happens when client disconnects
            e.printStackTrace();
            info("Client disconnected");
        } finally {
            isRunning = false;
            info("Exited thread loop. Cleaning up connection.");
            cleanup();
        }
    }
    /*UCID:  mg936
             *Date: 6 August 2023
             *Comment: MUTE_CLIENT payload is processed and result is handled by 
             *Room, which changes the client's mute status  The*/
    void processPayload(Payload p) throws IOException {
        switch (p.getPayloadType()) {
            case CONNECT:
                setClientName(p.getClientName());
                break;
            case DISCONNECT:
                Room.disconnectClient(this, getCurrentRoom());
                break;
            case MUTE_CLIENT:
                currentRoom.setClientMuteStatus(this, p.getMessage());
                break;
            case MESSAGE:
                if (currentRoom != null) {
                    formattedMessage = Styling.formatTags(p.getMessage());
                    if(formattedMessage.startsWith("@")){
                        currentRoom.sendDirectMessage(this, formattedMessage);
                    } else {
                        currentRoom.sendMessage(this, formattedMessage);
                    }
                } else {
                    // TODO migrate to lobby
                    logger.log(Level.INFO, "Migrating to lobby on message with null room");
                    Room.joinRoom("lobby", this);
                }
                break;
            case GET_ROOMS:
                Room.getRooms(p.getMessage().trim(), this);
                break;
            case CREATE_ROOM:
                Room.createRoom(p.getMessage().trim(), this);
                break;
            case JOIN_ROOM:
                Room.joinRoom(p.getMessage().trim(), this);
                break;
            /*UCID:  mg936
             *Date: 9 July 2023
             *Comment: Added Roll and Flip cases to switch in processPayload method 
             *         Attempts to generate a string of the roll/flip results.  The
             *         string is then announced to room by the server, rather than 
             *         via message from client.
             */
            case ROLL:
                gameResult = GameHandler.rollDice(p.getMessage().trim(), getClientName());
                currentRoom.sendAnnouncement(this, gameResult);
                break;
            case FLIP:
                gameResult = GameHandler.coinFlip(clientName);
                currentRoom.sendAnnouncement(this, gameResult);
                break;                
            default:
                break;
        }
    }

    private void cleanup() {
        info("Thread cleanup() start");
        try {
            client.close();
        } catch (IOException e) {
            info("Client already closed");
        }
        info("Thread cleanup() complete");
    }
}