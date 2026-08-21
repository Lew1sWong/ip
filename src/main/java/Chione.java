import java.util.Scanner;

/**
 * Entry point of the Chione chatbot.
 *
 * <p>At this stage (Level-5) Chione tracks three kinds of task (todo, deadline
 * and event), lists them on request, and can mark them as done or not done.
 * Anything it cannot carry out is reported as a {@link ChioneException} and
 * explained to the user, so the conversation continues until {@code bye}.
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

    /** Upper bound on how many tasks can be stored, as allowed by the Level-2 requirements. */
    private static final int MAX_TASKS = 100;

    public static void main(String[] args) {
        printGreeting();

        // A fixed-size array is enough here because the requirements let us assume
        // at most 100 tasks. A resizable ArrayList comes later, in A-Collections.
        Task[] tasks = new Task[MAX_TASKS];

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
            }

            try {
                taskCount = handleCommand(command, tasks, taskCount);
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
     * @param tasks     array holding the stored tasks
     * @param taskCount how many entries of {@code tasks} were in use before this call
     * @param command   one line of user input, already trimmed
     * @return the number of tasks in the list afterwards
     * @throws ChioneException if the command is unknown or its arguments are unusable
     */
    private static int handleCommand(String command, Task[] tasks, int taskCount) throws ChioneException {
        if (command.equals(COMMAND_LIST)) {
            printTasks(tasks, taskCount);
        } else if (isCommand(command, COMMAND_MARK)) {
            Task task = tasks[parseTaskNumber(command, COMMAND_MARK, taskCount)];
            task.markAsDone();
            printBlock("Nice! I've marked this task as done:", "  " + task);
        } else if (isCommand(command, COMMAND_UNMARK)) {
            Task task = tasks[parseTaskNumber(command, COMMAND_UNMARK, taskCount)];
            task.markAsNotDone();
            printBlock("OK, I've marked this task as not done yet:", "  " + task);
        } else if (isCommand(command, COMMAND_TODO)) {
            return addTask(tasks, taskCount, createTodo(command));
        } else if (isCommand(command, COMMAND_DEADLINE)) {
            return addTask(tasks, taskCount, createDeadline(command));
        } else if (isCommand(command, COMMAND_EVENT)) {
            return addTask(tasks, taskCount, createEvent(command));
        } else {
            throw new ChioneException("I don't know what \"" + command + "\" means. "
                    + "I understand: todo, deadline, event, list, mark, unmark, bye.");
        }
        return taskCount;
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
     * <p>Java passes {@code taskCount} by value, so incrementing it inside this
     * method would not affect the caller. The updated count is returned instead,
     * and the caller assigns it back.
     *
     * @param tasks     array holding the stored tasks
     * @param taskCount how many entries of {@code tasks} were in use before this call
     * @param task      the task to store
     * @return the number of tasks in the list after adding
     * @throws ChioneException if the list is already full
     */
    private static int addTask(Task[] tasks, int taskCount, Task task) throws ChioneException {
        // Without this check the next line would throw ArrayIndexOutOfBoundsException,
        // which says nothing useful to the user.
        if (taskCount >= MAX_TASKS) {
            throw new ChioneException("Your list is full at " + MAX_TASKS
                    + " tasks, so I cannot add another one.");
        }

        tasks[taskCount] = task;
        taskCount++;

        // "  " + task calls the task's own toString(), so the right type icon and
        // details appear without this method knowing which subclass it holds.
        printBlock("Got it. I've added this task:",
                "  " + task,
                "Now you have " + taskCount + " tasks in the list.");
        return taskCount;
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
     * @param tasks     array holding the stored tasks
     * @param taskCount how many entries of {@code tasks} are actually in use
     */
    private static void printTasks(Task[] tasks, int taskCount) {
        if (taskCount == 0) {
            printBlock("Your list is empty for now.");
            return;
        }

        // Build the display lines first, so the whole list can be printed inside
        // a single pair of dividers. The extra line is the heading above the list.
        String[] lines = new String[taskCount + 1];
        lines[0] = "Here are the tasks in your list:";
        for (int i = 0; i < taskCount; i++) {
            lines[i + 1] = (i + 1) + "." + tasks[i]; // numbering shown to the user starts at 1
        }
        printBlock(lines);
    }

    /**
     * Extracts the array index of the task referred to by a {@code mark}/{@code unmark} command.
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
