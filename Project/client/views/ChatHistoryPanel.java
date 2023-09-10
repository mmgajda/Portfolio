package Project.client.views;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;

import Project.client.Card;
import Project.client.ICardControls;
/*UCID: mg936
 * Date: 6 August 2023
 * Comment:  The ChatHistoryPanel class works with the UI to allow a user to export their history
 *              and it works with ChatPanel to capture the chat history itself
 */
public class ChatHistoryPanel extends JPanel {
    JPanel container;
    private JTextArea chatTextArea;
    ChatPanel chatPanel;
    private static Logger logger = Logger.getLogger(ChatHistoryPanel.class.getName());
    public List<String> chatHistory = new ArrayList<>();

    public ChatHistoryPanel(ICardControls controls, ChatPanel chatPanel) {
        super(new BorderLayout(10, 10));
        this.chatPanel = chatPanel;
        setChatHistory(chatPanel.getChatHistory());
        
        container = new JPanel(
                new BoxLayout(this, BoxLayout.Y_AXIS));
        JScrollPane scroll = new JScrollPane(container);
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setAlignmentY(TOP_ALIGNMENT);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        chatTextArea = new JTextArea();
        chatTextArea.setEditable(false);
        container.add(chatTextArea);
        this.add(scroll, BorderLayout.CENTER);

        setChatHistory(chatHistory);

        JButton back = new JButton("Go Back");
        back.addActionListener((event) -> {
            controls.previous();
        });
        this.add(back, BorderLayout.SOUTH);
        
        JButton exportButton = new JButton("Export Chat History");
        exportButton.addActionListener((event) -> {
            exportChatHistory();
        });
        this.add(exportButton, BorderLayout.NORTH);

        this.setName(Card.HISTORY.name());
        controls.addPanel(Card.HISTORY.name(), this);
    }

    public void setChatHistory(List<String> chatHistory) {
        this.chatHistory = chatHistory;
        //chatTextArea.setText(String.join("\n", chatHistory));
    }

    public void exportChatHistory() {
        JFileChooser fileChooser = new JFileChooser();
        int choice = fileChooser.showSaveDialog(this);
        if (choice == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            String filePath = selectedFile.getAbsolutePath();

            try (FileWriter fileWriter = new FileWriter(filePath)) {
                String chatHistory = chatPanel.getChatHistory().toString();
                fileWriter.write(chatHistory);
                logger.info(chatHistory);
                fileWriter.flush();
                logger.info("Chat history exported successfully to: " + filePath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}