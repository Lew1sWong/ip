import java.util.ArrayList;
import java.util.Scanner;

/**
 * Entry point of the Chione chatbot.
 *
 * <p>Chione tracks three kinds of task (todo, deadline and event), and can list,
 * mark, unmark and delete them. Each line of input is matched to a {@link Command}
 * and carried out; anything it cannot carry out is reported as a
 * {@link ChioneException} and explained to the user, so the conversation
 * continues until {@code bye}.
 */
public class Chione {
    /** Horizontal divider printed above and below each block of output. */
    private static final String DIVIDER = "    ____________________________________________________________";

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
            String input = scanner.nextLine().trim();

            try {
                Command command = Command.parse(input);
                if (command == Command.BYE) {
                    break;
                }

                // The list object itself is passed along, so any change made inside
                // is visible here without a count having to be handed back.
                handleCommand(command, input, tasks);
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
     * Carries out an already-identified command.
     *
     * <p>Switching on the enum lets each branch be labelled with the command it
     * serves rather than with a string comparison, and the arrow form has no
     * fall-through, so a missing {@code break} cannot silently run the next branch.
     *
     * @param command the command the user invoked
     * @param input   the full line of user input, already trimmed
     * @param tasks   the task list, modified in place
     * @throws ChioneException if the command's arguments are unusable
     */
    private static void handleCommand(Command command, String input, ArrayList<Task> tasks)
            throws ChioneException {
        // Stripping the keyword once here means the branches below deal only with
        // what the user typed after it.
        String arguments = command.argumentsOf(input);

        switch (command) {
        case LIST -> printTasks(tasks);
        case MARK -> {
            Task task = tasks.get(parseTaskNumber(arguments, command, tasks.size()));
            task.markAsDone();
            printBlock("Nice! I've marked this task as done:", "  " + task);
        }
        case UNMARK -> {
            Task task = tasks.get(parseTaskNumber(arguments, command, tasks.size()));
            task.markAsNotDone();
            printBlock("OK, I've marked this task as not done yet:", "  " + task);
        }
        case DELETE -> {
            // remove() both takes the task out and hands it back, so it can still
            // be shown to the user after it has left the list.
            Task removed = tasks.remove(parseTaskNumber(arguments, command, tasks.size()));
            printBlock("Noted. I've removed this task:",
                    "  " + removed,
                    "Now you have " + tasks.size() + " tasks in the list.");
        }
        case TODO -> addTask(tasks, createTodo(arguments));
        case DEADLINE -> addTask(tasks, createDeadline(arguments));
        case EVENT -> addTask(tasks, createEvent(arguments));
        default -> throw new ChioneException("I have not learnt to do that yet.");
        }
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
     * Builds a todo from the arguments of a {@code todo} command,
     * e.g. {@code "borrow book"}.
     *
     * @param description everything typed after the keyword
     * @return the new todo
     * @throws ChioneException if no description was given
     */
    private static Todo createTodo(String description) throws ChioneException {
        if (description.isEmpty()) {
            throw new ChioneException("A todo needs a description. Try: todo borrow book");
        }
        return new Todo(description);
    }

    /**
     * Builds a deadline from the arguments of a {@code deadline} command,
     * e.g. {@code "return book /by Sunday"}.
     *
     * @param arguments everything typed after the keyword
     * @return the new deadline
     * @throws ChioneException if the description or the due date is missing
     */
    private static Deadline createDeadline(String arguments) throws ChioneException {
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
     * Builds an event from the arguments of an {@code event} command,
     * e.g. {@code "meeting /from Mon 2pm /to 4pm"}.
     *
     * @param arguments everything typed after the keyword
     * @return the new event
     * @throws ChioneException if the description, the start or the end is missing
     */
    private static Event createEvent(String arguments) throws ChioneException {
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
     * <p>The user counts tasks from 1 but the list is indexed from 0, so the
     * number typed is decremented by one.
     *
     * @param arguments everything typed after the keyword, e.g. {@code "2"}
     * @param command   the command being carried out, quoted back in error messages
     * @param taskCount how many tasks exist, used to check the number is in range
     * @return the index into the task list
     * @throws ChioneException if the number is missing, not a number, or outside the list
     */
    private static int parseTaskNumber(String arguments, Command command, int taskCount)
            throws ChioneException {
        String keyword = command.getKeyword();
        if (arguments.isEmpty()) {
            throw new ChioneException("Tell me which task to " + keyword + ". "
                    + "Try: " + keyword + " 2");
        }

        int taskNumber;
        try {
            taskNumber = Integer.parseInt(arguments);
        } catch (NumberFormatException e) {
            // Integer.parseInt reports a parsing failure; the user needs to be told
            // what to type instead, so the low-level exception is translated here.
            throw new ChioneException("\"" + arguments + "\" is not a task number. "
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
