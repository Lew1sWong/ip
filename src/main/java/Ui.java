import java.util.Scanner;

/**
 * Everything Chione says to the user, and everything it hears back.
 *
 * <p>All of the console reading and writing is gathered here, so no other class
 * mentions {@code System.out} or {@link Scanner}. Two things follow from that:
 * the wording and layout of every reply can be changed in one file, and the rest
 * of the program can be tested without a console attached, because it no longer
 * prints anything itself.
 *
 * <p>The methods are named for <em>what has happened</em> rather than for what to
 * print, so a caller says {@code showAdded(task, count)} and leaves the choice of
 * words to this class.
 */
public class Ui {
    /** Horizontal divider printed above and below each block of output. */
    private static final String DIVIDER = "    ____________________________________________________________";

    /** Indent placed in front of every line inside a block. */
    private static final String INDENT = "     ";

    /**
     * Reads the user's input from standard input, one line at a time.
     *
     * <p>Held as a field so the same reader lasts for the whole conversation;
     * creating a new one per line would risk losing buffered input.
     */
    private final Scanner scanner;

    /** Connects this Ui to the console. */
    public Ui() {
        this.scanner = new Scanner(System.in);
    }

    /**
     * Reports whether there is another line of input to read.
     *
     * <p>This guards against the input ending without a {@code bye} — when input
     * is piped in from a file, say, or the user presses Ctrl+D. Without it,
     * {@link #readCommand()} would throw a {@code NoSuchElementException}.
     *
     * @return {@code true} if another command can be read
     */
    public boolean hasNextCommand() {
        return scanner.hasNextLine();
    }

    /**
     * Reads one line of input.
     *
     * @return what the user typed, with surrounding spaces removed
     */
    public String readCommand() {
        return scanner.nextLine().trim();
    }

    /** Releases the console once the conversation is over. */
    public void close() {
        scanner.close();
    }

    /** Shows the banner and welcome message printed when Chione starts. */
    public void showWelcome() {
        // ASCII-art banner spelling "CHIONE". Each "\\" is an escaped
        // backslash, since a lone "\" starts an escape sequence in Java.
        String banner = "  ____  _   _  ___   ___   _   _  _____ \n"
                + " / ___|| | | ||_ _| / _ \\ | \\ | || ____|\n"
                + "| |    | |_| | | | | | | ||  \\| ||  _|  \n"
                + "| |___ |  _  | | | | |_| || |\\  || |___ \n"
                + " \\____||_| |_||___| \\___/ |_| \\_||_____|\n";

        System.out.println(DIVIDER);
        System.out.println(banner);
        System.out.println(INDENT + "Hello! I'm Chione.");
        System.out.println(INDENT + "What can I do for you?");
        System.out.println(DIVIDER);
        System.out.println();
    }

    /** Shows the goodbye message printed just before Chione exits. */
    public void showGoodbye() {
        showBlock("Bye. Hope to see you again soon!");
    }

    /**
     * Reports something Chione could not do.
     *
     * @param message an explanation already phrased for the user to read
     */
    public void showError(String message) {
        showBlock(message);
    }

    /**
     * Reports that the saved tasks could not be read, and warns what happens next.
     *
     * @param message an explanation of what was wrong with the save file
     */
    public void showLoadingError(String message) {
        showBlock(message,
                "I'll start with an empty list. Note that saving anything new",
                "will replace that file, so rescue it first if you need it.");
    }

    /**
     * Confirms that a task has been added.
     *
     * @param task      the task just added
     * @param taskCount how many tasks there are now
     */
    public void showAdded(Task task, int taskCount) {
        // Concatenating the task calls its own toString(), so the right type icon
        // and details appear without this class knowing which subclass it holds.
        showBlock("Got it. I've added this task:",
                "  " + task,
                "Now you have " + taskCount + " tasks in the list.");
    }

    /**
     * Confirms that a task has been removed.
     *
     * @param task      the task just removed
     * @param taskCount how many tasks are left
     */
    public void showRemoved(Task task, int taskCount) {
        showBlock("Noted. I've removed this task:",
                "  " + task,
                "Now you have " + taskCount + " tasks in the list.");
    }

    /**
     * Confirms that a task has been marked as done.
     *
     * @param task the task just marked
     */
    public void showMarked(Task task) {
        showBlock("Nice! I've marked this task as done:", "  " + task);
    }

    /**
     * Confirms that a task has been marked as not done.
     *
     * @param task the task just unmarked
     */
    public void showUnmarked(Task task) {
        showBlock("OK, I've marked this task as not done yet:", "  " + task);
    }

    /**
     * Shows the whole task list, numbered.
     *
     * @param tasks the tasks to show
     */
    public void showTasks(TaskList tasks) {
        if (tasks.isEmpty()) {
            showBlock("Your list is empty for now.");
            return;
        }
        showBlock(numbered("Here are the tasks in your list:", tasks));
    }

    /**
     * Shows the tasks falling on one particular day.
     *
     * @param matches      the tasks found on that day, possibly none
     * @param readableDate the day, already formatted for display
     */
    public void showTasksOn(TaskList matches, String readableDate) {
        if (matches.isEmpty()) {
            showBlock("Nothing on " + readableDate + ".");
            return;
        }
        showBlock(numbered("Here is what you have on " + readableDate + ":", matches));
    }

    /**
     * Builds a heading followed by the tasks, numbered from 1.
     *
     * <p>The lines are built before anything is printed, so that the whole list
     * appears inside a single pair of dividers.
     *
     * @param heading the line to place above the list
     * @param tasks   the tasks to number, known not to be empty
     * @return the heading and one line per task
     */
    private static String[] numbered(String heading, TaskList tasks) {
        String[] lines = new String[tasks.size() + 1];
        lines[0] = heading;

        // Read straight through the tasks rather than asking for them by
        // position: every position here is known to be valid, so there is no
        // out-of-range answer to deal with.
        int lineNumber = 1;
        for (Task task : tasks.asList()) {
            lines[lineNumber] = lineNumber + "." + task; // the user counts from 1
            lineNumber++;
        }
        return lines;
    }

    /**
     * Prints one or more lines wrapped between two dividers, which is the shape of
     * every reply Chione gives.
     *
     * <p>The {@code String...} parameter accepts any number of lines, so a
     * one-line reply and a multi-line list share the same method.
     *
     * @param lines the lines to show, in order
     */
    private void showBlock(String... lines) {
        System.out.println(DIVIDER);
        for (String line : lines) {
            System.out.println(INDENT + line);
        }
        System.out.println(DIVIDER);
        System.out.println();
    }
}
