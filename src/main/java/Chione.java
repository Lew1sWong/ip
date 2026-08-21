import java.util.ArrayList;
import java.util.Scanner;

/**
 * Entry point of the Chione chatbot.
 *
 * <p>At this stage (Level-6) Chione tracks three kinds of task (todo, deadline
 * and event), and can list, mark, unmark and delete them. Anything it cannot
 * carry out is reported as a {@link ChioneException} and explained to the user,
 * so the conversation continues until {@code bye}.
 */
public class Chione {
    /** Horizontal divider printed above and below each block of output. */
    private static final String DIVIDER = "    ____________________________________________________________";

    /** Command that ends the conversation. */
    private static final String COMMAND_BYE = "bye";

    /** Command that shows everything stored so far. */
    private static final String COMMAND_LIST = "list";

    /** Command prefix for marking a task as done, e.g. {@code "mark 2"}. */
    private static final String COMMAND_MARK = "mark";

    /** Command prefix for marking a task as not done, e.g. {@code "unmark 2"}. */
    private static final String COMMAND_UNMARK = "unmark";

    /** Command prefix for adding a todo, e.g. {@code "todo borrow book"}. */
    private static final String COMMAND_TODO = "todo";

    /** Command prefix for adding a deadline, e.g. {@code "deadline return book /by Sunday"}. */
    private static final String COMMAND_DEADLINE = "deadline";

    /** Command prefix for adding an event, e.g. {@code "event meeting /from Mon 2pm /to 4pm"}. */
    private static final String COMMAND_EVENT = "event";

    /** Command prefix for removing a task, e.g. {@code "delete 3"}. */
    private static final String COMMAND_DELETE = "delete";

    public static void main(String[] args) {
        printGreeting();

        // ArrayList grows on demand and tracks its own size, so there is no capacity
        // limit to enforce and no separate counter to keep in step with the contents.
        // It also shifts the remaining elements along when one is removed, which is
        // exactly what the delete command needs.
        ArrayList<Task> tasks = new ArrayList<>();

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

            try {
                // The list object itself is passed along, so any change made inside
                // is visible here without a count having to be handed back.
                handleCommand(command, tasks);
            } catch (ChioneException e) {
                // Every anticipated problem arrives here with a message already
                // phrased for the user, so one catch block covers them all and
                // the conversation carries on instead of crashing.
                printBlock(e.getMessage());
            }
        }

        scanner.close();
        printFarewell();
    }

    /**
     * Works out which command the user typed and carries it out.
     *
     * @param command one line of user input, already trimmed
     * @param tasks   the task list, modified in place
     * @throws ChioneException if the command is unknown or its arguments are unusable
     */
    private static void handleCommand(String command, ArrayList<Task> tasks) throws ChioneException {
        if (command.equals(COMMAND_LIST)) {
            printTasks(tasks);
        } else if (isCommand(command, COMMAND_MARK)) {
            Task task = tasks.get(parseTaskNumber(command, COMMAND_MARK, tasks.size()));
            task.markAsDone();
            printBlock("Nice! I've marked this task as done:", "  " + task);
        } else if (isCommand(command, COMMAND_UNMARK)) {
            Task task = tasks.get(parseTaskNumber(command, COMMAND_UNMARK, tasks.size()));
            task.markAsNotDone();
            printBlock("OK, I've marked this task as not done yet:", "  " + task);
        } else if (isCommand(command, COMMAND_DELETE)) {
            // remove() both takes the task out and hands it back, so it can still
            // be shown to the user after it has left the list.
            Task removed = tasks.remove(parseTaskNumber(command, COMMAND_DELETE, tasks.size()));
            printBlock("Noted. I've removed this task:",
                    "  " + removed,
                    "Now you have " + tasks.size() + " tasks in the list.");
        } else if (isCommand(command, COMMAND_TODO)) {
            addTask(tasks, createTodo(command));
        } else if (isCommand(command, COMMAND_DEADLINE)) {
            addTask(tasks, createDeadline(command));
        } else if (isCommand(command, COMMAND_EVENT)) {
            addTask(tasks, createEvent(command));
        } else {
            throw new ChioneException("I don't know what \"" + command + "\" means. "
                    + "I understand: todo, deadline, event, list, mark, unmark, delete, bye.");
        }
    }

    /**
     * Checks whether a line of input invokes the given command.
     *
     * <p>Matching the keyword on its own as well as with arguments is what lets a
     * bare {@code "todo"} be reported as a missing description rather than as an
     * unknown command.
     *
     * @param input   one line of user input, already trimmed
     * @param keyword the command keyword to test for
     * @return true if the input is the keyword alone or the keyword plus arguments
     */
    private static boolean isCommand(String input, String keyword) {
        return input.equals(keyword) || input.startsWith(keyword + " ");
    }

    /**
     * Stores a newly created task and tells the user about it.
     *
     * @param tasks the task list, modified in place
     * @param task  the task to store
     */
    private static void addTask(ArrayList<Task> tasks, Task task) {
        tasks.add(task);

        // "  " + task calls the task's own toString(), so the right type icon and
        // details appear without this method knowing which subclass it holds.
        printBlock("Got it. I've added this task:",
                "  " + task,
                "Now you have " + tasks.size() + " tasks in the list.");
    }

    /**
     * Builds a todo from a command such as {@code "todo borrow book"}.
     *
     * @param command the full command line
     * @return the new todo
     * @throws ChioneException if no description was given
     */
    private static Todo createTodo(String command) throws ChioneException {
        String description = command.substring(COMMAND_TODO.length()).trim();
        if (description.isEmpty()) {
            throw new ChioneException("A todo needs a description. Try: todo borrow book");
        }
        return new Todo(description);
    }

    /**
     * Builds a deadline from a command such as {@code "deadline return book /by Sunday"}.
     *
     * @param command the full command line
     * @return the new deadline
     * @throws ChioneException if the description or the due date is missing
     */
    private static Deadline createDeadline(String command) throws ChioneException {
        String arguments = command.substring(COMMAND_DEADLINE.length()).trim();

        // The limit of 2 stops the split at the first " /by ", so a description
        // that itself contains " /by " is kept intact. The space glued on the
        // front lets "deadline /by Sunday" split as well, so its empty
        // description is reported as such rather than as a missing due date.
        String[] parts = (" " + arguments).split(" /by ", 2);
        if (parts.length < 2) {
            throw new ChioneException("A deadline needs a due date after /by. "
                    + "Try: deadline return book /by Sunday");
        }

        String description = parts[0].trim();
        String by = parts[1].trim();
        if (description.isEmpty()) {
            throw new ChioneException("A deadline needs a description before /by. "
                    + "Try: deadline return book /by Sunday");
        }
        if (by.isEmpty()) {
            throw new ChioneException("Tell me when it is due after /by. "
                    + "Try: deadline return book /by Sunday");
        }
        return new Deadline(description, by);
    }

    /**
     * Builds an event from a command such as {@code "event meeting /from Mon 2pm /to 4pm"}.
     *
     * @param command the full command line
     * @return the new event
     * @throws ChioneException if the description, the start or the end is missing
     */
    private static Event createEvent(String command) throws ChioneException {
        String arguments = command.substring(COMMAND_EVENT.length()).trim();

        // Peel off the description first, then the start time, leaving the end time.
        // The glued-on spaces serve the same purpose as in createDeadline.
        String[] descriptionAndRest = (" " + arguments).split(" /from ", 2);
        if (descriptionAndRest.length < 2) {
            throw new ChioneException("An event needs a start time after /from. "
                    + "Try: event project meeting /from Mon 2pm /to 4pm");
        }

        String[] fromAndTo = (" " + descriptionAndRest[1]).split(" /to ", 2);
        if (fromAndTo.length < 2) {
            throw new ChioneException("An event needs an end time after /to. "
                    + "Try: event project meeting /from Mon 2pm /to 4pm");
        }

        String description = descriptionAndRest[0].trim();
        String from = fromAndTo[0].trim();
        String to = fromAndTo[1].trim();
        if (description.isEmpty() || from.isEmpty() || to.isEmpty()) {
            throw new ChioneException("An event needs a description, a start and an end. "
                    + "Try: event project meeting /from Mon 2pm /to 4pm");
        }
        return new Event(description, from, to);
    }

    /**
     * Prints the stored tasks as a numbered list, one per line.
     *
     * @param tasks the task list to show
     */
    private static void printTasks(ArrayList<Task> tasks) {
        if (tasks.isEmpty()) {
            printBlock("Your list is empty for now.");
            return;
        }

        // Build the display lines first, so the whole list can be printed inside
        // a single pair of dividers. The extra line is the heading above the list.
        String[] lines = new String[tasks.size() + 1];
        lines[0] = "Here are the tasks in your list:";
        for (int i = 0; i < tasks.size(); i++) {
            lines[i + 1] = (i + 1) + "." + tasks.get(i); // numbering shown to the user starts at 1
        }
        printBlock(lines);
    }

    /**
     * Extracts the list index of the task referred to by a command such as
     * {@code mark}, {@code unmark} or {@code delete}.
     *
     * <p>The user counts tasks from 1 but the array is indexed from 0, so the
     * number typed is decremented by one.
     *
     * @param command   the full command line, e.g. {@code "mark 2"}
     * @param keyword    the command keyword, stripped off the front and quoted in errors
     * @param taskCount  how many tasks exist, used to check the number is in range
     * @return the index into the task array
     * @throws ChioneException if the number is missing, not a number, or outside the list
     */
    private static int parseTaskNumber(String command, String keyword, int taskCount)
            throws ChioneException {
        String argument = command.substring(keyword.length()).trim();
        if (argument.isEmpty()) {
            throw new ChioneException("Tell me which task to " + keyword + ". "
                    + "Try: " + keyword + " 2");
        }

        int taskNumber;
        try {
            taskNumber = Integer.parseInt(argument);
        } catch (NumberFormatException e) {
            // Integer.parseInt reports a parsing failure; the user needs to be told
            // what to type instead, so the low-level exception is translated here.
            throw new ChioneException("\"" + argument + "\" is not a task number. "
                    + "Try: " + keyword + " 2");
        }

        if (taskNumber < 1 || taskNumber > taskCount) {
            throw new ChioneException("There is no task " + taskNumber + ". "
                    + "Your list has " + taskCount + " task(s) right now.");
        }
        return taskNumber - 1;
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
