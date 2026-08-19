/**
 * Entry point of the Chione chatbot.
 */
public class Chione {
    public static void main(String[] args) {
        // ASCII-art banner spelling "CHIONE". Each "\\" is an escaped
        // backslash, since a lone "\" starts an escape sequence in Java.
        String banner = "  ____  _   _  ___   ___   _   _  _____ \n"
                + " / ___|| | | ||_ _| / _ \\ | \\ | || ____|\n"
                + "| |    | |_| | | | | | | ||  \\| ||  _|  \n"
                + "| |___ |  _  | | | | |_| || |\\  || |___ \n"
                + " \\____||_| |_||___| \\___/ |_| \\_||_____|\n";
        System.out.println(banner);
    }
}
