package Project.server;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/*UCID: mg936
* Date: 3 August 2023
* Comment:  Created MuteListHandler class to keep code organized and a bit more concise
*/
public class MuteListHandler {
    private static HashMap<String, Set<String>> mutedUsersMap = new HashMap<>();
    private static Logger logger = Logger.getLogger(MuteListHandler.class.getName());
    private Room currentRoom;
    private static MuteListHandler instance = new MuteListHandler();

    private MuteListHandler() {
        mutedUsersMap = new HashMap<>();
    }
    
    public static MuteListHandler getInstance() {
        return instance;
    }

    public void setCurrentRoom(Room room) {
        this.currentRoom = room;
    }
    /*UCID: mg936
    * Date: 2 August 2023
    * Comment:  mute and unmute methods check client's status, add/remove from the map
                as required.  Then call the save method, and finally sent a mute/unmute
                message.  This message is sent from the server(client id -1) to prevent
                a user from just typing in fake mute messages*/

    /*** Method attempts to add a target user to teh mutedUsersMap 
     * if they are being muted and are not already in the map*/
    public synchronized void muteClient(String clientName, String targetName) {
        if (!mutedUsersMap.containsKey(targetName)) {
            try {
                mutedUsersMap.put(targetName, new HashSet<>());
                Set<String> mutedUsers = mutedUsersMap.getOrDefault(clientName, new HashSet<>());
                mutedUsers.add(targetName);
                mutedUsersMap.put(clientName, mutedUsers);
                logger.info("MUTED CLIENT");
                saveMutedUsersMap(mutedUsersMap);
                // Inform the target user that they have been muted and by whom
                String muteMessage = ("@" + targetName +" You have been muted by " + clientName);
                ServerThread clientThread = null;
                currentRoom.sendDirectMessage(clientThread, muteMessage);
            } catch (Exception e) {
                logger.info("Error muting the user.");
            }
        } else {
            logger.info("User is already muted.");
        }
    }

    
    /*** Method attempts to remove a target user from the mutedUsersMap 
     * if they are being unmuted and are currently in the map  */
    public synchronized void unmuteClient(String clientName, String targetName) {
        if (mutedUsersMap.containsKey(targetName)){
            try {
                Set<String> mutedUsers = mutedUsersMap.get(clientName);
                mutedUsers.remove(targetName);
                logger.info("UNMUTED CLIENT");
                saveMutedUsersMap(mutedUsersMap);
                // Inform the target user that they have been unmuted and by whom
                String unmuteMessage = ("@" + targetName +" You have been unmuted by " + clientName);
                ServerThread clientThread = null;
                currentRoom.sendDirectMessage(clientThread, unmuteMessage);
            } catch (Exception e) {
                logger.info("Error unmuting the user.");
            }
        } else {
            logger.info("User is not muted.");
        }
    }
    
    /*** Function to check client's mute status */
    public synchronized boolean isClientMuted(String muterUsername, String targetUsername) {
        Set<String> mutedUsers = mutedUsersMap.get(targetUsername);
        if(mutedUsers != null && mutedUsers.contains(muterUsername)){
            return true;
        } else {
            return false;
        }
    }

    /*UCID: mg936
    * Date: 3 August 2023
    * Comment:  loadMutedUsersMap in the MuteListHandler class reads the HashMap from a CSV
                It is called by the server class's run() method*/

     /*** Function to load hashMap of muted users from CSV */
    public void loadMutedUserMap() {
    try (BufferedReader br = new BufferedReader(new FileReader("Project/server/muted_users.csv"))) {
        String line;
        while ((line = br.readLine()) != null) {
            String[] values = line.split(",");
            if (values.length > 1) {
                String clientName = values[0];
                Set<String> mutedUsers = new HashSet<>();
                for (int i = 1; i < values.length; i++) {
                    mutedUsers.add(values[i]);
                }
                mutedUsersMap.put(clientName, mutedUsers);
                }
            }
            logger.info("Muted users map loaded from CSV file.");
        } catch (IOException e) {
        logger.warning("Error loading muted users map from CSV file: " + e.getMessage());
    }
}
    /*UCID: mg936
    * Date: 3 August 2023
    * Comment:  SaveMutedUsersMap in the MuteListHandler class writes the HashMap to a CSV
                It is called by the class's other methods when any user is muted or unmuted
    */

    /*** Function to save hashMap of muted users to CSV */
    public void saveMutedUsersMap(HashMap<String, Set<String>> mutedUsersMap) {
        try (PrintWriter writer = new PrintWriter(new FileWriter("Project/server/muted_users.csv"))) {
            for (String clientName : mutedUsersMap.keySet()) {
                Set<String> mutedUsers = mutedUsersMap.get(clientName);
                StringBuilder sb = new StringBuilder(clientName);
                for (String targetName : mutedUsers) {
                    sb.append(",").append(targetName);
                }
                writer.println(sb.toString());
            }
            writer.flush();
            logger.info("Muted users map saved to CSV file.");
        } catch (IOException e) {
            logger.info("Error saving muted users map to CSV file: " + e.getMessage());
        }
    }
}