package Project.server;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

/*UCID:  mg936
**Date: 9 July 2023
**Comment: Styling Class handles formatting of text tags based on user-inputted tags.
            Currently: Bold = *b b* -> <b></b>
            Currently: Bold = *i i* -> <i></i>
            Currently: Bold = *u u* -> <u></u>
            Currently: Color (hex code) = *col##xxxxxx col##xxxxxx* -> <font color=#rrggbb> </font color=#rrggbb>
*/
public class Styling {

    private static Pattern bold = Pattern.compile("(.*?)\\*b(.*?)b\\*(.*?)");
    private static Pattern underline = Pattern.compile("(.*?)\\*u(.*?)u\\*(.*?)");
    private static Pattern italic = Pattern.compile("(.*?)\\*i(.*?)i\\*(.*?)");
    private static Pattern color = Pattern.compile("(.*?)\\*col##[a-fA-f0-9]{6}(.*?)col##[a-fA-f0-9]{6}\\*(.*?)");

    /**Method which finds and replaces user input tags with html tags.
     * 
     * @param message
     * @return String result, the formatted message.
     */
    protected static String formatTags(String message){    
        String result = message;
        Matcher boldTag = bold.matcher(result);
        Matcher underlineTag = underline.matcher(result);
        Matcher italicTag = italic.matcher(result);
        Matcher colorTag = color.matcher(result);

        if(boldTag.matches()) {
            result = result.replaceAll("\\*b", "<b>");
            result = result.replaceAll("b\\*", "</b>");
        }
        if(underlineTag.matches()) {
            result = result.replaceAll("\\*u", "<u>");
            result = result.replaceAll("u\\*", "</u>");
        }
        if(italicTag.matches()) {
            result = result.replaceAll("\\*i", "<i>");
            result = result.replaceAll("i\\*", "</i>");
        }
        if(colorTag.matches()) {
                        result = result.replaceAll("\\*col##([a-fA-f0-9]{6})", "<font color=#$1>");
                        result = result.replaceAll("col##([a-fA-f0-9]{6})\\*", "</font color=##$1>");
        }

        return result;
    }
}
