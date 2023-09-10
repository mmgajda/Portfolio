package Project.client.views;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

import Project.client.Card;
import Project.client.ICardControls;

public class Menu extends JMenuBar{
    public Menu(ICardControls controls){
        JMenu roomsMenu = new JMenu("Rooms");
        JMenuItem roomsSearch = new JMenuItem("Search");
        roomsSearch.addActionListener((event) -> {
            controls.show(Card.ROOMS.name());
        });
        roomsMenu.add(roomsSearch);

        JMenu fileMenu = new JMenu("File");
        JMenuItem chatHistoryItem = new JMenuItem("Export Chat History");
        chatHistoryItem.addActionListener((event) -> {
            controls.show(Card.HISTORY.name());
        });
        fileMenu.add(chatHistoryItem);

        this.add(roomsMenu);
        this.add(fileMenu);
    }
}