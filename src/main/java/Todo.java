/**
 * A task with no date or time attached, e.g. {@code "visit new theme park"}.
 *
 * <p>Displayed as {@code [T][ ] visit new theme park}.
 */
public class Todo extends Task {
    /**
     * Creates a todo that starts out not done.
     *
     * @param description what the user wants to do
     */
    public Todo(String description) {
        super(description); // hands the description to the Task constructor
    }

    /**
     * Returns the task prefixed with its type icon.
     *
     * <p>{@code super.toString()} produces the {@code "[ ] description"} part that
     * every task shares, so only the {@code [T]} prefix is added here.
     */
    @Override
    public String toString() {
        return "[T]" + super.toString();
    }
}
