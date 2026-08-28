package chione;

/**
 * The Chione chatbot.
 *
 * <p>Chione tracks three kinds of task (todo, deadline and event), and can list,
 * mark, unmark, delete and search them by date. Each line of input is turned by
 * {@link Parser} into a {@link Command}, which then carries itself out; anything
 * that cannot be carried out is reported as a {@link ChioneException} and
 * explained to the user, so the conversation continues until {@code bye}.
 *
 * <p>This class does very little itself. It owns a {@link Ui}, a {@link Storage}
 * and a {@link TaskList}, and hands all three to whichever command it was given.
 * It has no branch per command and no knowledge of what any command does, so a
 * new command can be added without this file changing at all.
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

        boolean isExit = false;

        // hasNextCommand() also stops the loop when the input runs out without a
        // "bye" -- when a script is piped in, or the user presses Ctrl+D.
        while (!isExit && ui.hasNextCommand()) {
            String input = ui.readCommand();

            try {
                Command command = Parser.parse(input);
                command.execute(tasks, ui, storage);
                isExit = command.isExit();
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
     * Starts Chione.
     *
     * @param args ignored; Chione takes no command-line arguments
     */
    public static void main(String[] args) {
        new Chione(SAVE_FILE_PATH).run();
    }
}
