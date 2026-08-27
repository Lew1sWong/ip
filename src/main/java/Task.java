import java.time.LocalDate;

/**
 * A single item in the user's task list.
 *
 * <p>A task knows its own description and whether it has been completed, and
 * knows how to render itself for display. Keeping that knowledge here rather
 * than in {@link Chione} means the chatbot only has to decide <em>when</em> to
 * show a task, not <em>how</em> to format one.
 */
public class Task {
    /**
     * What the user wants to do, e.g. {@code "read book"}.
     *
     * <p>{@code protected} rather than {@code private} so that the subclasses
     * added in Level-4 (Todo, Deadline, Event) can read it directly.
     */
    protected String description;

    /** Whether the user has marked this task as completed. */
    protected boolean isDone;

    /**
     * Creates a task that starts out not done.
     *
     * @param description what the user wants to do
     */
    public Task(String description) {
        this.description = description;
        this.isDone = false;
    }

    /**
     * Returns the single character shown inside the status brackets.
     *
     * @return {@code "X"} if this task is done, or a space if it is not
     */
    public String getStatusIcon() {
        return isDone ? "X" : " "; // a done task is marked with X
    }

    /** Marks this task as completed. */
    public void markAsDone() {
        this.isDone = true;
    }

    /** Marks this task as not completed, undoing a previous {@link #markAsDone()}. */
    public void markAsNotDone() {
        this.isDone = false;
    }

    /**
     * Returns this task in the form shown to the user, e.g. {@code "[X] read book"}.
     *
     * <p>Overriding {@code toString()} means a task can be dropped straight into
     * string concatenation without the caller building the format by hand.
     */
    @Override
    public String toString() {
        return "[" + getStatusIcon() + "] " + description;
    }

    /**
     * Reports whether this task falls on the given day.
     *
     * <p>A plain task has no date, so the answer here is always no. Deadlines and
     * events override this with their own answer. Asking the task itself, rather
     * than testing its type from outside, means the {@code on} command works for
     * any task type added later without being changed.
     *
     * @param date the day being asked about
     * @return {@code true} if this task falls on that day
     */
    public boolean occursOn(LocalDate date) {
        return false;
    }

    /**
     * Returns this task as one line of the save file, e.g. {@code "1 | read book"}.
     *
     * <p>Each subclass prefixes its own type letter and appends its own extra
     * fields, so every class writes exactly the part it knows about and no single
     * method has to know about all three task types. Fields are separated by
     * {@code " | "} rather than a comma, because a comma is far more likely to
     * turn up inside a description the user typed.
     *
     * @return the part every task shares: the done flag and the description
     */
    public String toSaveFormat() {
        return (isDone ? "1" : "0") + " | " + description;
    }
}
