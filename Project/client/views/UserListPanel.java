package Project.client.views;
import Project.server.MuteListHandler;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BoxLayout;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import Project.client.Client;
import Project.client.ClientUtils;
import Project.client.ICardControls;

public class UserListPanel extends JPanel {
    JPanel userListArea;
    private MuteListHandler muteListHandler = MuteListHandler.getInstance();
    private String currentUsername;
    private static Logger logger = Logger.getLogger(UserListPanel.class.getName());

    public UserListPanel(ICardControls controls) {
        super(new BorderLayout(10, 10));
        JPanel wrapper = new JPanel();
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setAlignmentY(Component.BOTTOM_ALIGNMENT);
        currentUsername = "";
        // wraps a viewport to provide scroll capabilities
        JScrollPane scroll = new JScrollPane(content);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        // scroll.setBorder(BorderFactory.createEmptyBorder());
        // no need to add content specifically because scroll wraps it

        userListArea = content;

        wrapper.add(scroll);
        this.add(wrapper, BorderLayout.CENTER);

        userListArea.addContainerListener(new ContainerListener() {

            @Override
            public void componentAdded(ContainerEvent e) {
                if (userListArea.isVisible()) {
                    userListArea.revalidate();
                    userListArea.repaint();
                }
            }

            @Override
            public void componentRemoved(ContainerEvent e) {
                if (userListArea.isVisible()) {
                    userListArea.revalidate();
                    userListArea.repaint();
                }
            }

        });
    }

    public void addUserListItem(long clientId, String clientName) {
        UserListItem userListItem = new UserListItem(clientId, clientName);
        userListItem.setName(clientId + "");
        JPanel content = userListArea;
        logger.log(Level.INFO, "Userlist: " + content.getSize());
        userListItem.setLayout(null);
        userListItem.setPreferredSize(new Dimension(content.getWidth(), ClientUtils.calcHeightForText(userListItem.getFontMetrics(userListItem.getFont()), clientName, content.getWidth())));
        userListItem.setMaximumSize(userListItem.getPreferredSize());
        userListItem.setEditable(false);
        ClientUtils.clearBackground(userListItem);
        // add to container
        content.add(userListItem);
        content.revalidate();
        content.repaint();
    }

    public void removeUserListItem(long clientId) {
        logger.log(Level.INFO, "removing user list item for id " + clientId);
        Component[] cs = userListArea.getComponents();
        for (Component c : cs) {
            if (c.getName().equals(clientId + "")) {
                userListArea.remove(c);
                break;
            }
        }
    }

    protected void clearUserList() {
        Component[] cs = userListArea.getComponents();
        for (Component c : cs) {
            userListArea.remove(c);
        }
    }
    
        /*UCID: mg936
        * Date: 5 August 2023
        * Comment: Added the two methods that control highlighting the last sender of a
        *          message, and changing text color of muted usernames
        */
    public void textColorForMutedUsers(String target, String clientName) {
        logger.info("TEXT COLOR METHOD CALLED");        
        boolean isMuted = muteListHandler.isClientMuted(clientName, target);
        logger.info("target: " + target + " and client: " + clientName + " boolean: " + isMuted);
        Component[] cs = userListArea.getComponents();
        for (Component c : cs) {
            if (c instanceof UserListItem){
                UserListItem userListItem = (UserListItem) c;
                
                if (isMuted) {
                    logger.log(Level.INFO, "Adding MUTED user to list: " + clientName);
                    String userListItemHTML = "<html><span style='color: pink'>" + clientName + "</span></html>";
                    ((JEditorPane) c).setText(userListItemHTML);
                } else {
                    logger.log(Level.INFO, "not adding user to mute list: " + clientName);
                    logger.info("target: " + clientName + "current: " + currentUsername);
                    String userListItemHTML = "<html><span style='color: red'>" + clientName + "</span></html>";
                    ((JEditorPane) c).setText(userListItemHTML);
                    //userListArea.repaint();
                }
            
        }
    }
}

    public void highlightLastMessageSender(String lastMessageSender) {
        Component[] cs = userListArea.getComponents();
        for (Component c : cs) {
            if (c instanceof UserListItem){
                UserListItem userListItem = (UserListItem) c;
                String clientName = userListItem.getClientName();
                if (clientName.contains(lastMessageSender + " ")) {
                    String userListItemHTML = "<html><span style='background-color: yellow; color: black'>" + clientName + "</span></html>";
                    ((JEditorPane) c).setText(userListItemHTML);
                } else {
                    String userListItemHTML = "<html><span style='background-color: transparent; color: black'>" + clientName + "</span></html>";
                    //logger.log(Level.INFO, "NO HIGHLIGHT" + clientName);
                    ((JEditorPane) c).setText(userListItemHTML);
                    }
                }
            }
    }
}