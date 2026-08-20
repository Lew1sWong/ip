import java.util.Scanner;

/**
 * Entry point of the Chione chatbot.
 *
 * <p>At this stage (Level-4) Chione tracks three kinds of task (todo, deadline
 * and event), lists them on request, and can mark them as done or not done,
 * until the user enters the {@code bye} command.
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
            } else if (command.equals(COMMAND_LIST)) {
                printTasks(tasks, taskCount);
            } else if (command.startsWith(COMMAND_MARK + " ")) {
                Task task = tasks[parseTaskNumber(command)];
                task.markAsDone();
                printBlock("Nice! I've marked this task as done:", "  " + task);
            } else if (command.startsWith(COMMAND_UNMARK + " ")) {
                Task task = tasks[parseTaskNumber(command)];
                task.markAsNotDone();
                printBlock("OK, I've marked this task as not done yet:", "  " + task);
            } else if (command.startsWith(COMMAND_TODO + " ")) {
                taskCount = addTask(tasks, taskCount, createTodo(command));
            } else if (command.startsWith(COMMAND_DEADLINE + " ")) {
                taskCount = addTask(tasks, taskCount, createDeadline(command));
            } else if (command.startsWith(COMMAND_EVENT + " ")) {
                taskCount = addTask(tasks, taskCount, createEvent(command));
            } else {
                // Every task now has a type, so free text is no longer stored as-is.
                // Level-5 replaces this message with proper exception handling.
                printBlock("Sorry, I don't know what that means :-(");
            }
        }

        scanner.close();
        printFarewell();
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
     */
    private static int addTask(Task[] tasks, int taskCount, Task task) {
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
     */
    private static Todo createTodo(String command) {
        String description = command.substring(COMMAND_TODO.length()).trim();
        return new Todo(description);
    }

    /**
     * Builds a deadline from a command such as {@code "deadline return book /by Sunday"}.
     *
     * @param command the full command line
     * @return the new deadline
     */
    private static Deadline createDeadline(String command) {
        String arguments = command.substring(COMMAND_DEADLINE.length()).trim();

        // The limit of 2 stops the split at the first " /by ", so a description
        // that itself contains " /by " is kept intact.
        String[] parts = arguments.split(" /by ", 2);
        return new Deadline(parts[0].trim(), parts[1].trim());
    }

    /**
     * Builds an event from a command such as {@code "event meeting /from Mon 2pm /to 4pm"}.
     *
     * @param command the full command line
     * @return the new event
     */
    private static Event createEvent(String command) {
        String arguments = command.substring(COMMAND_EVENT.length()).trim();

        // Peel off the description first, then the start time, leaving the end time.
        String[] descriptionAndRest = arguments.split(" /from ", 2);
        String[] fromAndTo = descriptionAndRest[1].split(" /to ", 2);
        return new Event(descriptionAndRest[0].trim(), fromAndTo[0].trim(), fromAndTo[1].trim());
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
     * <p>Malformed input (a missing or non-numeric number, or one outside the
     * list) is not handled yet; that is what Level-5 adds.
     *
     * @param command the full command line, e.g. {@code "mark 2"}
     * @return the index into the task array
     */
    private static int parseTaskNumber(String command) {
        // split(" ") breaks "mark 2" into ["mark", "2"]; element 1 is the number.
        String[] words = command.split(" ");
        return Integer.parseInt(words[1]) - 1;
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
