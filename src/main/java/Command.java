/**
 * The set of instructions Chione understands.
 *
 * <p>Each constant owns the keyword the user types to invoke it, so the list of
 * valid commands exists in exactly one place: adding a command here makes it
 * recognised, quotable in error messages, and checkable by the compiler in the
 * switch that carries commands out.
 *
 * <p>An enum is used rather than string constants because a {@code Command} can
 * only ever hold one of these eight values. A typo like {@code Command.LITS}
 * fails to compile, whereas the string {@code "lits"} would compile happily and
 * simply never match anything at runtime.
 */
public enum Command {
    /** Adds a task with no date attached. */
    TODO("todo"),

    /** Adds a task due by a given date. */
    DEADLINE("deadline"),

    /** Adds a task that runs between two given dates. */
    EVENT("event"),

    /** Shows every stored task. */
    LIST("list"),

    /** Marks a task as done. */
    MARK("mark"),

    /** Marks a task as not done. */
    UNMARK("unmark"),

    /** Removes a task from the list. */
    DELETE("delete"),

    /** Ends the conversation. */
    BYE("bye");

    /** The word the user types to invoke this command. */
    private final String keyword;

    /**
     * Associates a keyword with a constant.
     *
     * <p>Enum constructors are implicitly private: constants are created once,
     * when the enum is first loaded, and never anywhere else.
     *
     * @param keyword the word the user types
     */
    Command(String keyword) {
        this.keyword = keyword;
    }

    /**
     * Returns the word the user types to invoke this command.
     *
     * @return the keyword, e.g. {@code "delete"}
     */
    public String getKeyword() {
        return keyword;
    }

    /**
     * Works out which command a line of input invokes.
     *
     * <p>A keyword on its own counts as a match, as well as a keyword followed by
     * arguments. That is what lets a bare {@code "todo"} be reported as a missing
     * description rather than as an unknown command.
     *
     * @param input one line of user input, already trimmed
     * @return the matching command
     * @throws ChioneException if no command matches
     */
    public static Command parse(String input) throws ChioneException {
        // values() returns every constant declared above, in order.
        for (Command command : values()) {
            if (input.equals(command.keyword) || input.startsWith(command.keyword + " ")) {
                return command;
            }
        }
        throw new ChioneException("I don't know what \"" + input + "\" means. "
                + "I understand: " + listKeywords() + ".");
    }

    /**
     * Returns everything after this command's keyword, e.g. {@code "2"} from
     * {@code "delete 2"} or the empty string from {@code "delete"}.
     *
     * @param input one line of user input, already trimmed and known to invoke this command
     * @return the arguments, trimmed
     */
    public String argumentsOf(String input) {
        return input.substring(keyword.length()).trim();
    }

    /**
     * Returns every keyword as a comma-separated list, for use in error messages.
     *
     * <p>Building this from {@code values()} keeps the message honest: a command
     * added later shows up here without anyone having to remember to update it.
     */
    private static String listKeywords() {
        StringBuilder keywords = new StringBuilder();
        for (Command command : values()) {
            if (!keywords.isEmpty()) {
                keywords.append(", ");
            }
            keywords.append(command.keyword);
        }
        return keywords.toString();
    }
}
