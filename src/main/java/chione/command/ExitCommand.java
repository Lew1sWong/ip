package chione.command;

import chione.Storage;
import chione.Ui;
import chione.task.TaskList;

/**
 * Ends the conversation.
 */
public class ExitCommand extends Command {
    /** Creates the command; ending the conversation needs no further detail. */
    public ExitCommand() {
    }

    @Override
    public void execute(TaskList tasks, Ui ui, Storage storage) {
        // Nothing to do here. The goodbye is said by the run loop once it stops,
        // so that it is also said when the input simply runs out without a "bye"
        // -- when a script is piped in, for instance.
    }

    /** {@inheritDoc} */
    @Override
    public boolean isExit() {
        return true;
    }
}
