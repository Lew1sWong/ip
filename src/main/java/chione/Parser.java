package chione;

import java.time.LocalDate;

/**
 * Turns what the user typed into something Chione can act on.
 *
 * <p>Every piece of knowledge about the shape of a command lives here: which
 * keyword starts it, where {@code /by} splits a deadline, and what counts as a
 * usable task number. What comes back is a {@link Command} that already knows how
 * to carry itself out, so {@link Chione} never has to ask what it was handed.
 *
 * <p>Gathering the checks here also gathers the error messages here, so the
 * advice offered when a command is mistyped stays consistent across commands.
 *
 * <p>This class only holds static helpers, so it is {@code final} and its
 * constructor is private: there is never any reason to create one.
 */
public final class Parser {
    /** Prevents this utility class from being instantiated. */
    private Parser() {
    }

    /**
     * Turns one line of input into the command it asks for.
     *
     * <p>The switch covers every {@link CommandType}, and covers it by name rather
     * than with a fallback branch. A command word added to the enum therefore
     * stops this file compiling until it is given a command to produce here,
     * which is a far better reminder than an error at run time would be.
     *
     * @param input one line of user input, already trimmed
     * @return the command the user asked for
     * @throws ChioneException if the line does not name a command, or its
     *                         arguments cannot be used
     */
    public static Command parse(String input) throws ChioneException {
        CommandType type = parseType(input);

        // Stripping the keyword once here means the branches below deal only with
        // what the user typed after it.
        String arguments = type.argumentsOf(input);

        return switch (type) {
        case TODO -> new AddCommand(parseTodo(arguments));
        case DEADLINE -> new AddCommand(parseDeadline(arguments));
        case EVENT -> new AddCommand(parseEvent(arguments));
        case LIST -> new ListCommand();
        case ON -> new OnCommand(parseDate(arguments));
        case MARK -> new MarkCommand(parseTaskNumber(arguments, type));
        case UNMARK -> new UnmarkCommand(parseTaskNumber(arguments, type));
        case DELETE -> new DeleteCommand(parseTaskNumber(arguments, type));
        case BYE -> new ExitCommand();
        };
    }

    /**
     * Works out which command word a line of input starts with.
     *
     * <p>A keyword on its own counts as a match, as well as a keyword followed by
     * arguments. That is what lets a bare {@code "todo"} be reported as a missing
     * description rather than as an unknown command.
     *
     * @param input one line of user input, already trimmed
     * @return the matching command word
     * @throws ChioneException if no command matches
     */
    private static CommandType parseType(String input) throws ChioneException {
        // values() returns every constant of the enum, in the order declared.
        for (CommandType command : CommandType.values()) {
            String keyword = command.getKeyword();
            if (input.equals(keyword) || input.startsWith(keyword + " ")) {
                return command;
            }
        }
        throw new ChioneException("I don't know what \"" + input + "\" means. "
                + "I understand: " + listKeywords() + ".");
    }

    /**
     * Builds a todo from the arguments of a {@code todo} command,
     * e.g. {@code "borrow book"}.
     *
     * @param description everything typed after the keyword
     * @return the new todo
     * @throws ChioneException if no description was given
     */
    public static Todo parseTodo(String description) throws ChioneException {
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
    public static Deadline parseDeadline(String arguments) throws ChioneException {
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
    public static Event parseEvent(String arguments) throws ChioneException {
        // Peel off the description first, then the start time, leaving the end time.
        // The glued-on spaces serve the same purpose as in parseDeadline.
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
     * <p>Whether the number actually points at a task is not decided here:
     * {@link TaskList} knows how long it is and refuses a position it does not
     * hold. This method only insists that a number was given at all.
     *
     * @param arguments everything typed after the keyword, e.g. {@code "2"}
     * @param command   the command being carried out, quoted back in error messages
     * @return the index into the task list
     * @throws ChioneException if the number is missing or is not a number
     */
    public static int parseTaskNumber(String arguments, CommandType command)
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

        return taskNumber - 1;
    }

    /**
     * Extracts the day referred to by an {@code on} command.
     *
     * <p>{@link DateTimes} knows what a date looks like; what this adds is that an
     * {@code on} with nothing after it is a missing argument rather than an
     * unreadable date, and deserves to be told so.
     *
     * @param arguments everything typed after {@code on}, e.g. {@code "2019-10-15"}
     * @return the day being asked about
     * @throws ChioneException if no day was given, or it is not a readable date
     */
    public static LocalDate parseDate(String arguments) throws ChioneException {
        if (arguments.isEmpty()) {
            throw new ChioneException("Tell me which day to look at. Try: on 2019-10-15");
        }
        return DateTimes.parseDate(arguments);
    }

    /**
     * Returns every keyword as a comma-separated list, for use in error messages.
     *
     * <p>Building this from {@code values()} keeps the message honest: a command
     * added later shows up here without anyone having to remember to update it.
     *
     * @return e.g. {@code "todo, deadline, event, list, on, ..."}
     */
    private static String listKeywords() {
        StringBuilder keywords = new StringBuilder();
        for (CommandType command : CommandType.values()) {
            if (!keywords.isEmpty()) {
                keywords.append(", ");
            }
            keywords.append(command.getKeyword());
        }
        return keywords.toString();
    }
}
