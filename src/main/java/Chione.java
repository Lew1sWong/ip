import java.time.LocalDate;

/**
 * The Chione chatbot.
 *
 * <p>Chione tracks three kinds of task (todo, deadline and event), and can list,
 * mark, unmark, delete and search them by date. Each line of input is matched to
 * a {@link Command} and carried out; anything it cannot carry out is reported as
 * a {@link ChioneException} and explained to the user, so the conversation
 * continues until {@code bye}.
 *
 * <p>This class now does very little itself. It owns a {@link Ui}, a
 * {@link Storage} and a {@link TaskList}, and its job is to decide which of them
 * to call and in what order — not to print, to parse, or to manage a collection.
 */
public class Chione {
    /**
     * Where the task list is kept between runs.
     *
     * <p>Relative to the folder Chione is started from, so the program works on
     * any machine; {@link Storage} turns it into a path suited to the operating
     * system it finds itself on.
     */
    private static final String SAVE_FILE_PATH = "data/chione.txt";

    /** Everything the user sees, and everything they type. */
    private final Ui ui;

    /** Where the tasks are read from and written back to. */
    private final Storage storage;

    /** The tasks being tracked; replaced by the saved ones when the run starts. */
    private TaskList tasks;

    /**
     * Prepares a chatbot that will keep its tasks in the given file.
     *
     * <p>Nothing is read from disk yet: loading happens in {@link #run()}, so that
     * the greeting appears before any complaint about the save file rather than
     * after it.
     *
     * @param filePath where to keep the tasks, e.g. {@code "data/chione.txt"}
     */
    public Chione(String filePath) {
        this.ui = new Ui();
        this.storage = new Storage(filePath);
        this.tasks = new TaskList();
    }

    /** Greets the user, then answers commands until they say goodbye. */
    public void run() {
        ui.showWelcome();
        this.tasks = loadTasks();

        while (ui.hasNextCommand()) {
            String input = ui.readCommand();

            try {
                Command command = Command.parse(input);
                if (command == Command.BYE) {
                    break;
                }

                handleCommand(command, input);

                // Saving here, once, keeps the file in step with the list without
                // every branch of handleCommand having to remember to do it.
                // Commands such as "list" change nothing and so rewrite an
                // identical file, which is cheap at this size and never wrong.
                storage.save(tasks);
            } catch (ChioneException e) {
                // Every anticipated problem arrives here with a message already
                // phrased for the user, so one catch block covers them all and
                // the conversation carries on instead of crashing.
                ui.showError(e.getMessage());
            }
        }

        ui.close();
        ui.showGoodbye();
    }

    /**
     * Reads the saved tasks, or starts afresh if they cannot be read.
     *
     * <p>Chione is still usable without its save file, so a loading problem is
     * reported and stepped over rather than allowed to stop the program.
     *
     * @return the stored tasks, or an empty list if they could not be loaded
     */
    private TaskList loadTasks() {
        try {
            return new TaskList(storage.load());
        } catch (ChioneException e) {
            ui.showLoadingError(e.getMessage());
            return new TaskList();
        }
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
     * @throws ChioneException if the command's arguments are unusable
     */
    private void handleCommand(Command command, String input) throws ChioneException {
        // Stripping the keyword once here means the branches below deal only with
        // what the user typed after it.
        String arguments = command.argumentsOf(input);

        switch (command) {
        case LIST -> ui.showTasks(tasks);
        case ON -> showTasksOn(arguments);
        case MARK -> {
            Task task = tasks.get(parseTaskNumber(arguments, command, tasks.size()));
            task.markAsDone();
            ui.showMarked(task);
        }
        case UNMARK -> {
            Task task = tasks.get(parseTaskNumber(arguments, command, tasks.size()));
            task.markAsNotDone();
            ui.showUnmarked(task);
        }
        case DELETE -> {
            Task removed = tasks.remove(parseTaskNumber(arguments, command, tasks.size()));
            ui.showRemoved(removed, tasks.size());
        }
        case TODO -> addTask(createTodo(arguments));
        case DEADLINE -> addTask(createDeadline(arguments));
        case EVENT -> addTask(createEvent(arguments));
        default -> throw new ChioneException("I have not learnt to do that yet.");
        }
    }

    /**
     * Stores a newly created task and tells the user about it.
     *
     * @param task the task to store
     */
    private void addTask(Task task) {
        tasks.add(task);
        ui.showAdded(task, tasks.size());
    }

    /**
     * Shows every task falling on one particular day.
     *
     * @param arguments everything typed after {@code on}, e.g. {@code "2019-10-15"}
     * @throws ChioneException if no day was given, or it is not a readable date
     */
    private void showTasksOn(String arguments) throws ChioneException {
        if (arguments.isEmpty()) {
            throw new ChioneException("Tell me which day to look at. Try: on 2019-10-15");
        }

        LocalDate date = DateTimes.parseDate(arguments);
        ui.showTasksOn(tasks.findOn(date), DateTimes.formatDate(date));
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
     * e.g. {@code "return book /by 2019-10-15"}.
     *
     * @param arguments everything typed after the keyword
     * @return the new deadline
     * @throws ChioneException if the description or the due date is missing or unreadable
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
        // DateTimes.parse rejects anything that is not a date it can read, so a
        // Deadline can never be built around text that only looks like one.
        return new Deadline(description, DateTimes.parse(by));
    }

    /**
     * Builds an event from the arguments of an {@code event} command,
     * e.g. {@code "meeting /from 2019-10-15 1400 /to 2019-10-15 1600"}.
     *
     * @param arguments everything typed after the keyword
     * @return the new event
     * @throws ChioneException if the description, the start or the end is missing or unreadable
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
        return new Event(description, DateTimes.parse(from), DateTimes.parse(to));
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

    /**
     * Starts Chione.
     *
     * @param args ignored; Chione takes no command-line arguments
     */
    public static void main(String[] args) {
        new Chione(SAVE_FILE_PATH).run();
    }
}
