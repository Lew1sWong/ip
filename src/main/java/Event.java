/**
 * A task that starts and ends at a given date or time,
 * e.g. {@code "project meeting /from Mon 2pm /to 4pm"}.
 *
 * <p>Displayed as {@code [E][ ] project meeting (from: Mon 2pm to: 4pm)}.
 */
public class Event extends Task {
    /** When the event starts, kept as plain text like {@link Deadline#by}. */
    protected String from;

    /** When the event ends, kept as plain text like {@link Deadline#by}. */
    protected String to;

    /**
     * Creates an event that starts out not done.
     *
     * @param description what the event is
     * @param from        when it starts
     * @param to          when it ends
     */
    public Event(String description, String from, String to) {
        super(description);
        this.from = from;
        this.to = to;
    }

    /** Returns the task prefixed with its type icon and followed by its time range. */
    @Override
    public String toString() {
        return "[E]" + super.toString() + " (from: " + from + " to: " + to + ")";
    }

    /**
     * Returns the saved line with the type letter in front and both times
     * appended, e.g. {@code "E | 0 | project meeting | Mon 2pm | 4pm"}.
     */
    @Override
    public String toSaveFormat() {
        return "E | " + super.toSaveFormat() + " | " + from + " | " + to;
    }
}
