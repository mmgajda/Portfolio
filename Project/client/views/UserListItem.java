package Project.client.views;

import java.awt.Color;

import javax.swing.JEditorPane;

import Project.client.ClientUtils;

public class UserListItem extends JEditorPane {
    private long clientId;
    private String clientName;

    public UserListItem(long clientId, String clientName) {
        super("text/html", clientName);
        this.clientId = clientId;
        this.clientName = clientName;
        this.setEditable(false);
        ClientUtils.clearBackground(this);
        setSelectedTextColor(Color.BLACK);
        // Customize the appearance of the UserListItem here if needed
    }

    public long getClientId() {
        return clientId;
    }

    public String getClientName() {
        return clientName;
    }

    /*public void setMuted(boolean muted) {
        // Update the font color based on the muted state
        if (muted) {
            setForeground(Color.PINK); // Set the text color to pink when muted
        } else {
            setForeground(Color.RED); // Set the text color to black (default) when not muted
        }
    }*/
}