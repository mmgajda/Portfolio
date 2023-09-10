package Project.server;

import java.util.Random;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import Project.common.Payload;
import Project.common.PayloadType;
/*UCID:  mg936
* Date: 10 July 2023
* GameHandler class accepts input starting with game commands, 
* currently flip and roll, and returns result of game actions*/   
public class GameHandler {
    
    private static Random random = new Random();
    private static int totalRoll = 0;
    private static int numberOfDie = 0;
    private static int sides = 0;
    private static String diceResult;
    private static String flipResult;
    private static String playerName;
    private static Logger logger = Logger.getLogger(GameHandler.class.getName()); 

    /***
	 * Method to process rolls of die
	 * 
	 * @param message The string that starts with the roll command
	 * @param clientName  The sender of the roll command
	 */
    protected static String rollDice(String message, String clientName) {
        playerName = clientName;
        try { 
            Pattern pattern = Pattern.compile("(\\d+)d(\\d+)");
            Matcher matcher = pattern.matcher(message);
            if (matcher.find()) {
                String diceString = matcher.group();
                String[] roll = diceString.split("d");
                if(roll.length == 2){
                    numberOfDie = Integer.parseInt(roll[0]);
                    sides = Integer.parseInt(roll[1]);
                    int[] diceRolls = new int[numberOfDie];
                    for (int i = 0; i < numberOfDie; i++) {
                            diceRolls[i] = random.nextInt(sides) + 1;
                            totalRoll += diceRolls[i];
                    }
                }
                diceResult = String.format("%s rolled %d die with %d sides totals %d",
                                            playerName, numberOfDie, sides, totalRoll);
                
            } else if (message.length() == 1) {
                sides = Integer.parseInt(message);
                totalRoll = random.nextInt(sides) + 1;
                diceResult = String.format("%s rolled a dice with %d sides and it landed on %d", 
                                                    playerName, sides, totalRoll);
            } else {
                logger.info("Error processing requested roll.");
            }
        } catch (Exception e) {
            logger.severe("Error processing roll command.");
            e.printStackTrace();
        }
        return(diceResult);
    }
    /*UCID:  mg936
    * Date: 10 July 2023*/

    /***
	 * Method to process coin flip
	 * 
	 * @param clientName  The sender of the flip command
	 */
    protected static String coinFlip (String clientName) {
        playerName = clientName;
        sides = random.nextInt(6000) + 1;
        try {
            if(sides < 3000) {
                flipResult = String.format("%s flipped coin and it landed on heads.", playerName);
            } else if (sides > 3000) {
                flipResult = String.format("%s flipped a coin and it landed on tails.", playerName);
            } else { //there is a 1 in 6000 chance of a flipped coin landing on its edge
                flipResult = String.format("%s flipped a coin and....WOW it landed on its edge!" +
                                    "They should go and buy some lottery tickets!", playerName);
            }
            } catch (Exception e) {
                logger.severe("Error processing the flip, please try again.");
                e.printStackTrace();
            }
        return(flipResult);
    }
}