package Project.client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Hashtable;
import java.util.logging.Logger;
import java.util.logging.Level;

import Project.common.Constants;
import Project.common.Payload;
import Project.common.PayloadType;
import Project.common.RoomResultPayload;

public enum Client {
    INSTANCE;

    Socket server = null;
    ObjectOutputStream out = null;
    ObjectInputStream in = null;
    boolean isRunning = false;
    private Thread fromServerThread;
    private long myClientId;
    private String clientName = "";
    private static Logger logger = Logger.getLogger(Client.class.getName());
    private static IClientEvents events;
    private Hashtable<Long, String> userList = new Hashtable<Long, String>();

    
    public boolean isConnected() {
        if (server == null) {
            return false;
        }
        // https://stackoverflow.com/a/10241044
        // Note: these check the client's end of the socket connect; therefore they
        // don't really help determine
        // if the server had a problem
        return server.isConnected() && !server.isClosed() && !server.isInputShutdown() && !server.isOutputShutdown();

    }

    /**
     * Takes an ip address and a port to attempt a socket connection to a server.
     * 
     * @param address
     * @param port
     * @return true if connection was successful
     */
    public boolean connect(String address, int port, String username, IClientEvents callback) {
        this.clientName = username;
        Client.events = callback;
        try {
            server = new Socket(address, port);
            // channel to send to server
            out = new ObjectOutputStream(server.getOutputStream());
            // channel to listen to server
            in = new ObjectInputStream(server.getInputStream());
            logger.log(Level.INFO, "Client connected");
            listenForServerPayload();
            sendConnect();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return isConnected();
    }

    // Send methods

    public void sendGetRooms(String query) throws IOException, NullPointerException {
        Payload p = new Payload();
        p.setPayloadType(PayloadType.GET_ROOMS);
        p.setMessage(query);
        send(p);
    }

    public void sendJoinRoom(String room) throws IOException, NullPointerException {
        Payload p = new Payload();
        p.setPayloadType(PayloadType.JOIN_ROOM);
        p.setMessage(room);
        send(p);
    }

    public void sendCreateRoom(String room) throws IOException, NullPointerException {
        Payload p = new Payload();
        p.setPayloadType(PayloadType.CREATE_ROOM);
        p.setMessage(room);
        send(p);
    }

    public void sendDisconnect() throws IOException, NullPointerException {
        Payload p = new Payload();
        p.setPayloadType(PayloadType.DISCONNECT);
        send(p);
    }

    private void sendConnect() throws IOException {
        Payload p = new Payload();
        p.setPayloadType(PayloadType.CONNECT);
        p.setClientName(clientName);
        send(p);
    }

    public void sendMessage(String message) throws IOException {
        Payload p = new Payload();
        p.setPayloadType(PayloadType.MESSAGE);
        p.setMessage(message);
        p.setClientName(clientName);
        send(p);
    }
    
    public void send(Payload p) throws IOException, NullPointerException {
        logger.log(Level.FINE, "Sending Payload: " + p);
        out.writeObject(p);
        out.flush();
        logger.log(Level.INFO, "Sent Payload: " + p);
    }
    /*UCID:  mg936
    *Date: 5 August 2023
    *Comment: Added send method for MUTE_CLIENT payload type*/
    public void sendMuteChange(String message) throws IOException {
        Payload p = new Payload();
        p.setPayloadType(PayloadType.MUTE_CLIENT);
        p.setMessage(message);
        p.setClientName(clientName);
        p.setClientId(myClientId);
        send(p);
    }

    public void sendRoll(String message) throws IOException {
        Payload p = new Payload();
        p.setPayloadType(PayloadType.ROLL);
        p.setMessage(message);
        p.setClientName(clientName);
        send(p);
    }

    public void sendFlip(String message) throws IOException {
        Payload p = new Payload();
        p.setPayloadType(PayloadType.FLIP);
        p.setMessage(message);
        p.setClientName(clientName);
        send(p);
        }
    // end send methods

    private void listenForServerPayload() {
        fromServerThread = new Thread() 
        {
            @Override
            public void run() {
                isRunning = true;
                try {
                    Payload fromServer;

                    // while we're connected, listen for strings from server
                    while (isRunning && !server.isClosed() && !server.isInputShutdown()
                            && (fromServer = (Payload) in.readObject()) != null) {

                        logger.info("Debug Info: " + fromServer);
                        processPayload(fromServer);

                    }
                    logger.info("listenForServerPayload() loop exited");
                } catch (Exception e) {
                    e.printStackTrace();
                    if (!server.isClosed()) {
                        System.out.println("Server closed connection");
                    } else {
                        System.out.println("Connection closed");
                    }
                } finally {
                    close();
                    logger.info("Stopped listening to server input");
                }
            }
        };
        fromServerThread.start();// start the thread
    }

    public String getClientNameById(long id) {
        if (userList.containsKey(id)) {
            return userList.get(id);
        }
        if (id == Constants.DEFAULT_CLIENT_ID) {
            return "[Server]";
        }
        return "unknown user";
    }

    /**
     * Processes incoming payloads from ServerThread
     * 
     * @param p
     * @throws IOException
     */
    private void processPayload(Payload p) throws IOException {
        logger.log(Level.FINE, "Received Payload: " + p);
        if (events == null) {
            logger.log(Level.FINER, "Events not initialized/set" + p);
            return;
        }
        switch (p.getPayloadType()) {
            case CONNECT:
                events.onClientConnect(p.getClientId(), p.getClientName(), p.getMessage());
                break;
            case DISCONNECT:
                events.onClientDisconnect(p.getClientId(), p.getClientName(), p.getMessage());
                break;
            case MESSAGE:
                events.onMessageReceive(p.getClientId(), p.getMessage());
                break;
            case CLIENT_ID:
                events.onReceiveClientId(p.getClientId());
                break;
            case RESET_USER_LIST:
                events.onResetUserList();
                break;
            case SYNC_CLIENT:
                events.onSyncClient(p.getClientId(), p.getClientName());
                break;
            case MUTE_CLIENT:
                events.onClientMuteChange(p.getMessage(), p.getClientName());
                break;
            /*UCID:  mg936
            *Date: 5 August 2023
            *Comment: Added MUTE_CLIENT payload type*/
            case ROLL:
                events.onAnnouncementReceive(p.getMessage());
                break;
            case FLIP:
                events.onAnnouncementReceive(p.getMessage());
                break;
                case GET_ROOMS:
                events.onReceiveRoomList(((RoomResultPayload) p).getRooms(), p.getMessage());
                break;
            case JOIN_ROOM:
                events.onRoomJoin(p.getMessage());
                break;
            default:
            logger.log(Level.WARNING, "Unhandled payload type");
            break;
        }
    }

    private void close() {
        myClientId = Constants.DEFAULT_CLIENT_ID;
        userList.clear();
        try {
            fromServerThread.interrupt();
        } catch (Exception e) {
            System.out.println("Error interrupting listener");
            e.printStackTrace();
        }
        try {
            fromServerThread.interrupt();
        } catch (Exception e) {
            System.out.println("Error interrupting listener");
            e.printStackTrace();
        }
        try {
            System.out.println("Closing output stream");
            out.close();
        } catch (NullPointerException ne) {
            System.out.println("Server was never opened so this exception is ok");
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            System.out.println("Closing input stream");
            in.close();
        } catch (NullPointerException ne) {
            System.out.println("Server was never opened so this exception is ok");
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            System.out.println("Closing connection");
            server.close();
            System.out.println("Closed socket");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NullPointerException ne) {
            System.out.println("Server was never opened so this exception is ok");
        }
    }
}