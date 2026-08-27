import java.time.LocalDate;

/**
 * The Chione chatbot.
 *
 * <p>Chione tracks three kinds of task (todo, deadline and event), and can list,
 * mark, unmark, delete and search them by date. Each line of input is matched to
 * a {@link CommandType} and carried out; anything it cannot carry out is reported as
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
                CommandType command = Parser.parseCommand(input);
                if (command == CommandType.BYE) {
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
    private void handleCommand(CommandType command, String input) throws ChioneException {
        // Stripping the keyword once here means the branches below deal only with
        // what the user typed after it.
        String arguments = command.argumentsOf(input);

        switch (command) {
        case LIST -> ui.showTasks(tasks);
        case ON -> showTasksOn(arguments);
        case MARK -> {
            Task task = tasks.get(Parser.parseTaskNumber(arguments, command));
            task.markAsDone();
            ui.showMarked(task);
        }
        case UNMARK -> {
            Task task = tasks.get(Parser.parseTaskNumber(arguments, command));
            task.markAsNotDone();
            ui.showUnmarked(task);
        }
        case DELETE -> {
            Task removed = tasks.remove(Parser.parseTaskNumber(arguments, command));
            ui.showRemoved(removed, tasks.size());
        }
        case TODO -> addTask(Parser.parseTodo(arguments));
        case DEADLINE -> addTask(Parser.parseDeadline(arguments));
        case EVENT -> addTask(Parser.parseEvent(arguments));
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
        LocalDate date = Parser.parseDate(arguments);
        ui.showTasksOn(tasks.findOn(date), DateTimes.formatDate(date));
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
