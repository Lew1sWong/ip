package chione;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A task that must be finished before a given date or time,
 * e.g. {@code "submit report /by 2019-10-15 1800"}.
 *
 * <p>Displayed as {@code [D][ ] submit report (by: Oct 15 2019, 6:00pm)}.
 */
public class Deadline extends Task {
    /**
     * When the task is due.
     *
     * <p>Held as a {@link LocalDateTime} rather than as text, so that the date
     * can be compared with other dates — which is what lets the {@code on}
     * command find every task falling on a particular day. Storing it as a
     * string would make that a matter of comparing spelling rather than time.
     */
    protected LocalDateTime by;

    /**
     * Creates a deadline that starts out not done.
     *
     * @param description what the user wants to do
     * @param by          when it is due
     */
    public Deadline(String description, LocalDateTime by) {
        super(description);
        this.by = by;
    }

    /** Reports whether this deadline falls on the given day. */
    @Override
    public boolean occursOn(LocalDate date) {
        return by.toLocalDate().equals(date);
    }

    /** Returns the task prefixed with its type icon and followed by the due date. */
    @Override
    public String toString() {
        return "[D]" + super.toString() + " (by: " + DateTimes.format(by) + ")";
    }

    /**
     * Returns the saved line with the type letter in front and the due date
     * appended, e.g. {@code "D | 0 | return book | 2019-10-15 1800"}.
     */
    @Override
    public String toSaveFormat() {
        return "D | " + super.toSaveFormat() + " | " + DateTimes.toSaveFormat(by);
    }
}
