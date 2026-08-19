import java.util.Scanner;

/**
 * Entry point of the Chione chatbot.
 *
 * <p>At this stage (Level-1) Chione simply echoes back whatever the user types,
 * until the user enters the {@code bye} command.
 */
public class Chione {
    /** Horizontal divider printed above and below each block of output. */
    private static final String DIVIDER = "    ____________________________________________________________";

    /** Command that ends the conversation. */
    private static final String COMMAND_BYE = "bye";

    public static void main(String[] args) {
        printGreeting();

        // Scanner reads the user's input from standard input (the console), one line at a time.
        Scanner scanner = new Scanner(System.in);

        // hasNextLine() guards against the input stream ending without a "bye"
        // (e.g. when input is piped in from a file, or the user presses Ctrl+D).
        // Without it, nextLine() would throw a NoSuchElementException.
        while (scanner.hasNextLine()) {
            String command = scanner.nextLine().trim();
            if (command.equals(COMMAND_BYE)) {
                break;
            }
            printBlock(command);
        }

        scanner.close();
        printFarewell();
    }

    /** Prints the banner and welcome message shown when Chione starts. */
    private static void printGreeting() {
        // ASCII-art banner spelling "CHIONE". Each "\\" is an escaped
        // backslash, since a lone "\" starts an escape sequence in Java.
        String banner = "  ____  _   _  ___   ___   _   _  _____ \n"
                + " / ___|| | | ||_ _| / _ \\ | \\ | || ____|\n"
                + "| |    | |_| | | | | | | ||  \\| ||  _|  \n"
                + "| |___ |  _  | | | | |_| || |\\  || |___ \n"
                + " \\____||_| |_||___| \\___/ |_| \\_||_____|\n";

        System.out.println(DIVIDER);
        System.out.println(banner);
        System.out.println("     Hello! I'm Chione.");
        System.out.println("     What can I do for you?");
        System.out.println(DIVIDER);
        System.out.println();
    }

    /** Prints the goodbye message shown just before Chione exits. */
    private static void printFarewell() {
        printBlock("Bye. Hope to see you again soon!");
    }

    /**
     * Prints a single line of text wrapped between two dividers, which is the
     * standard shape of every reply Chione gives.
     *
     * @param message the text to show to the user
     */
    private static void printBlock(String message) {
        System.out.println(DIVIDER);
        System.out.println("     " + message);
        System.out.println(DIVIDER);
        System.out.println();
    }
}
