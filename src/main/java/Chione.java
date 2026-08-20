import java.util.Scanner;

/**
 * Entry point of the Chione chatbot.
 *
 * <p>At this stage (Level-2) Chione remembers the text the user types and can
 * list it back on request, until the user enters the {@code bye} command.
 */
public class Chione {
    /** Horizontal divider printed above and below each block of output. */
    private static final String DIVIDER = "    ____________________________________________________________";

    /** Command that ends the conversation. */
    private static final String COMMAND_BYE = "bye";

    /** Command that shows everything stored so far. */
    private static final String COMMAND_LIST = "list";

    /** Upper bound on how many tasks can be stored, as allowed by the Level-2 requirements. */
    private static final int MAX_TASKS = 100;

    public static void main(String[] args) {
        printGreeting();

        // A fixed-size array is enough here because the requirements let us assume
        // at most 100 tasks. A resizable ArrayList comes later, in A-Collections.
        String[] tasks = new String[MAX_TASKS];

        // Number of tasks stored so far, which is also the index of the next free slot.
        int taskCount = 0;

        // Scanner reads the user's input from standard input (the console), one line at a time.
        Scanner scanner = new Scanner(System.in);

        // hasNextLine() guards against the input stream ending without a "bye"
        // (e.g. when input is piped in from a file, or the user presses Ctrl+D).
        // Without it, nextLine() would throw a NoSuchElementException.
        while (scanner.hasNextLine()) {
            String command = scanner.nextLine().trim();

            if (command.equals(COMMAND_BYE)) {
                break;
            } else if (command.equals(COMMAND_LIST)) {
                printTasks(tasks, taskCount);
            } else {
                // Anything that is not a known command is stored as a new task.
                tasks[taskCount] = command;
                taskCount++;
                printBlock("added: " + command);
            }
        }

        scanner.close();
        printFarewell();
    }

    /**
     * Prints the stored tasks as a numbered list, one per line.
     *
     * @param tasks     array holding the stored tasks
     * @param taskCount how many entries of {@code tasks} are actually in use
     */
    private static void printTasks(String[] tasks, int taskCount) {
        if (taskCount == 0) {
            printBlock("Your list is empty for now.");
            return;
        }

        // Build the display lines first, so the whole list can be printed inside
        // a single pair of dividers.
        String[] lines = new String[taskCount];
        for (int i = 0; i < taskCount; i++) {
            lines[i] = (i + 1) + ". " + tasks[i]; // numbering shown to the user starts at 1
        }
        printBlock(lines);
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
     * Prints one or more lines of text wrapped between two dividers, which is the
     * standard shape of every reply Chione gives.
     *
     * <p>The {@code String...} parameter accepts any number of lines, so a one-line
     * reply and a multi-line list can share the same method.
     *
     * @param lines the lines to show to the user, in order
     */
    private static void printBlock(String... lines) {
        System.out.println(DIVIDER);
        for (String line : lines) {
            System.out.println("     " + line);
        }
        System.out.println(DIVIDER);
        System.out.println();
    }
}
