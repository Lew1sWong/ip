package chione;

import java.util.Scanner;

import chione.task.Task;
import chione.task.TaskList;

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
 *
 * <p>Nothing is written out as it is said. Each {@code show...} call adds to the
 * reply being built, which is then collected in one piece — by
 * {@link #printResponse()} for the console, or by {@link #consumeResponse()} for
 * the window. That is what lets one set of replies serve both interfaces: the
 * words are chosen here, and only the delivery differs.
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
     * creating a new one per line would risk losing buffered input. Only the text
     * interface reads from it; the window supplies its own input.
     */
    private final Scanner scanner;

    /**
     * The reply being built, emptied each time one is collected.
     *
     * <p>A single command can say several things — a confirmation and then a
     * count, say — and they belong together in one reply, so they are gathered
     * here rather than sent out one at a time.
     */
    private final StringBuilder response;

    /** Connects this Ui to the console and starts with nothing to say. */
    public Ui() {
        this.scanner = new Scanner(System.in);
        this.response = new StringBuilder();
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

    /**
     * Shows the banner and welcome message the text interface starts with.
     *
     * <p>The banner is drawn out of characters, so it only lines up in the
     * console's fixed-width type. The window uses {@link #showGreeting()} instead.
     */
    public void showWelcome() {
        // ASCII-art banner spelling "CHIONE". Each "\\" is an escaped
        // backslash, since a lone "\" starts an escape sequence in Java.
        String banner = "  ____  _   _  ___   ___   _   _  _____ \n"
                + " / ___|| | | ||_ _| / _ \\ | \\ | || ____|\n"
                + "| |    | |_| | | | | | | ||  \\| ||  _|  \n"
                + "| |___ |  _  | | | | |_| || |\\  || |___ \n"
                + " \\____||_| |_||___| \\___/ |_| \\_||_____|";

        showBlock(banner, "");
        showGreeting();
    }

    /** Shows the welcome message on its own, without the ASCII-art banner. */
    public void showGreeting() {
        showBlock("Hello! I'm Chione.", "What can I do for you?");
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
        showBlock(buildNumberedLines("Here are the tasks in your list:", tasks));
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
        showBlock(buildNumberedLines("Here is what you have on " + readableDate + ":", matches));
    }

    /**
     * Shows the tasks whose description contains a given word.
     *
     * @param matches the tasks found, possibly none
     * @param keyword the word that was searched for, quoted back when nothing
     *                matched so the user can see what was actually looked for
     */
    public void showMatchingTasks(TaskList matches, String keyword) {
        if (matches.isEmpty()) {
            showBlock("Nothing in your list matches \"" + keyword + "\".");
            return;
        }
        showBlock(buildNumberedLines("Here are the matching tasks in your list:", matches));
    }

    /**
     * Builds the lines of a listing: a heading, then the tasks numbered from 1.
     *
     * <p>The lines are built before anything is printed, so that the whole list
     * appears inside a single pair of dividers.
     *
     * @param heading the line to place above the list
     * @param tasks   the tasks to number, known not to be empty
     * @return the heading and one line per task
     */
    private static String[] buildNumberedLines(String heading, TaskList tasks) {
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
     * Hands over everything said since the last time it was collected, and starts
     * a fresh reply.
     *
     * <p>This is what the window shows in a dialog box. The lines arrive plain,
     * without the console's dividers and indent, because the window draws its own
     * border around them.
     *
     * @return the reply, or an empty string if there was nothing to say
     */
    public String consumeResponse() {
        // Only trailing space is removed: the banner's leading spaces are what
        // keep its letters lined up.
        String reply = response.toString().stripTrailing();
        response.setLength(0);
        return reply;
    }

    /**
     * Prints everything said since the last time it was collected, wrapped between
     * two dividers, which is the shape of every reply in the text interface.
     *
     * <p>A command that says nothing prints nothing, rather than an empty pair of
     * dividers.
     */
    public void printResponse() {
        String reply = consumeResponse();
        if (reply.isEmpty()) {
            return;
        }

        System.out.println(DIVIDER);
        for (String line : reply.split("\\R")) {
            System.out.println(INDENT + line);
        }
        System.out.println(DIVIDER);
        System.out.println();
    }

    /**
     * Adds one or more lines to the reply being built.
     *
     * <p>The {@code String...} parameter accepts any number of lines, so a
     * one-line reply and a multi-line list share the same method.
     *
     * @param lines the lines to add, in order
     */
    private void showBlock(String... lines) {
        for (String line : lines) {
            response.append(line).append(System.lineSeparator());
        }
    }
}
