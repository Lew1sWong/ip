package chione.command;

import chione.ChioneException;
import chione.Storage;
import chione.Ui;
import chione.task.Task;
import chione.task.TaskList;

/**
 * Adds a task to the list.
 *
 * <p>One class covers todos, deadlines and events alike, because by this point
 * {@link chione.Parser Parser} has already built the task: what differs between the three is
 * how they are read from the input, not what adding one involves.
 */
public class AddCommand extends Command {
    /** The task to add, already built. */
    private final Task task;

    /**
     * Remembers the task to be added.
     *
     * @param task the task to add
     */
    public AddCommand(Task task) {
        this.task = task;
    }

    @Override
    public void execute(TaskList tasks, Ui ui, Storage storage) throws ChioneException {
        tasks.add(task);
        ui.showAdded(task, tasks.size());
        storage.save(tasks);
    }
}
