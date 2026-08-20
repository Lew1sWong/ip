/**
 * A task that must be finished before a given date or time,
 * e.g. {@code "submit report /by Sunday"}.
 *
 * <p>Displayed as {@code [D][ ] submit report (by: Sunday)}.
 */
public class Deadline extends Task {
    /**
     * When the task is due.
     *
     * <p>Kept as plain text for now: the Level-4 requirements explicitly allow
     * dates to be treated as strings, so {@code "no idea :-p"} is just as valid
     * as {@code "Sunday"}. A later increment converts these to real date objects.
     */
    protected String by;

    /**
     * Creates a deadline that starts out not done.
     *
     * @param description what the user wants to do
     * @param by          when it is due
     */
    public Deadline(String description, String by) {
        super(description);
        this.by = by;
    }

    /** Returns the task prefixed with its type icon and followed by the due date. */
    @Override
    public String toString() {
        return "[D]" + super.toString() + " (by: " + by + ")";
    }
}
