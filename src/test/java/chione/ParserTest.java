package chione;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import chione.command.AddCommand;
import chione.command.CommandType;
import chione.command.DeleteCommand;
import chione.command.ExitCommand;
import chione.command.FindCommand;
import chione.command.ListCommand;
import chione.command.MarkCommand;
import chione.command.OnCommand;
import chione.command.UnmarkCommand;

/**
 * Tests {@link Parser}, which is the only part of Chione that sees raw input.
 *
 * <p>Two things are checked throughout: that a well-formed line produces the
 * right command, and that a malformed one is refused with the particular message
 * that says what to type instead. A parser that accepted bad input quietly would
 * push the problem into code that has no idea what the user meant.
 */
public class ParserTest {
    @Test
    public void parse_todoLine_addCommandProduced() throws ChioneException {
        assertInstanceOf(AddCommand.class, Parser.parse("todo read book"));
    }

    @Test
    public void parse_listLine_listCommandProduced() throws ChioneException {
        assertInstanceOf(ListCommand.class, Parser.parse("list"));
    }

    @Test
    public void parse_onLine_onCommandProduced() throws ChioneException {
        assertInstanceOf(OnCommand.class, Parser.parse("on 2019-10-15"));
    }

    @Test
    public void parse_markLine_markCommandProduced() throws ChioneException {
        assertInstanceOf(MarkCommand.class, Parser.parse("mark 1"));
    }

    @Test
    public void parse_unmarkLine_unmarkCommandProduced() throws ChioneException {
        assertInstanceOf(UnmarkCommand.class, Parser.parse("unmark 1"));
    }

    @Test
    public void parse_deleteLine_deleteCommandProduced() throws ChioneException {
        assertInstanceOf(DeleteCommand.class, Parser.parse("delete 1"));
    }

    @Test
    public void parse_byeLine_commandEndsTheConversation() throws ChioneException {
        assertInstanceOf(ExitCommand.class, Parser.parse("bye"));
        assertTrue(Parser.parse("bye").isExit());
    }

    @Test
    public void parse_anyOtherCommand_doesNotEndTheConversation() throws ChioneException {
        assertEquals(false, Parser.parse("list").isExit());
    }

    @Test
    public void parse_unknownWord_exceptionListsEveryKeyword() {
        ChioneException e = assertThrows(ChioneException.class, () -> Parser.parse("blah"));
        assertEquals("I don't know what \"blah\" means. "
                + "I understand: todo, deadline, event, list, on, find, mark, unmark, delete, bye.",
                e.getMessage());
    }

    @Test
    public void parse_keywordAsPrefixOfAWord_notTreatedAsThatCommand() {
        // "listing" starts with "list", but it is a different word, so it is not
        // the list command with a stray argument.
        assertThrows(ChioneException.class, () -> Parser.parse("listing"));
    }

    @Test
    public void parse_findLine_findCommandProduced() throws ChioneException {
        assertInstanceOf(FindCommand.class, Parser.parse("find book"));
    }

    @Test
    public void parseKeyword_word_returnedUnchanged() throws ChioneException {
        assertEquals("book", Parser.parseKeyword("book"));
    }

    @Test
    public void parseKeyword_severalWords_allKept() throws ChioneException {
        // The whole of what was typed is the search text, spaces included.
        assertEquals("read book", Parser.parseKeyword("read book"));
    }

    @Test
    public void parseKeyword_nothingTyped_exceptionThrown() {
        // An empty search would match everything, which is what "list" is for.
        ChioneException e = assertThrows(ChioneException.class, () -> Parser.parseKeyword(""));
        assertEquals("Tell me what to look for. Try: find book", e.getMessage());
    }

    @Test
    public void parseTodo_description_todoCreated() throws ChioneException {
        assertEquals("[T][ ] read book", Parser.parseTodo("read book").toString());
    }

    @Test
    public void parseTodo_noDescription_exceptionThrown() {
        ChioneException e = assertThrows(ChioneException.class, () -> Parser.parseTodo(""));
        assertEquals("A todo needs a description. Try: todo borrow book", e.getMessage());
    }

    @Test
    public void parseDeadline_descriptionAndDate_deadlineCreated() throws ChioneException {
        assertEquals("[D][ ] return book (by: Oct 15 2019, 6:00pm)",
                Parser.parseDeadline("return book /by 2019-10-15 1800").toString());
    }

    @Test
    public void parseDeadline_dateWithoutTime_deadlineShownWithoutTime() throws ChioneException {
        assertEquals("[D][ ] return book (by: Oct 15 2019)",
                Parser.parseDeadline("return book /by 2019-10-15").toString());
    }

    @Test
    public void parseDeadline_noBySeparator_exceptionThrown() {
        ChioneException e = assertThrows(ChioneException.class, () ->
                Parser.parseDeadline("return book"));
        assertEquals("A deadline needs a due date after /by. "
                + "Try: deadline return book /by Sunday", e.getMessage());
    }

    @Test
    public void parseDeadline_nothingBeforeBy_exceptionNamesTheDescription() {
        ChioneException e = assertThrows(ChioneException.class, () ->
                Parser.parseDeadline("/by 2019-10-15"));
        assertEquals("A deadline needs a description before /by. "
                + "Try: deadline return book /by Sunday", e.getMessage());
    }

    @Test
    public void parseDeadline_nothingAfterBy_exceptionNamesTheDate() {
        ChioneException e = assertThrows(ChioneException.class, () ->
                Parser.parseDeadline("return book /by "));
        assertEquals("Tell me when it is due after /by. "
                + "Try: deadline return book /by Sunday", e.getMessage());
    }

    @Test
    public void parseDeadline_unreadableDate_exceptionThrown() {
        assertThrows(ChioneException.class, () -> Parser.parseDeadline("return book /by Sunday"));
    }

    @Test
    public void parseDeadline_separatorInsideTheDate_dateKeptWhole() {
        // The split stops at the first " /by ", so everything after it is treated
        // as the date -- separators included. The date is then rejected for being
        // unreadable rather than silently cut in half.
        assertThrows(ChioneException.class, () ->
                Parser.parseDeadline("return book /by 2019-10-15 /by 2019-10-16"));
    }

    @Test
    public void parseEvent_descriptionAndBothTimes_eventCreated() throws ChioneException {
        assertEquals("[E][ ] meeting (from: Oct 15 2019, 2:00pm to: Oct 17 2019, 4:00pm)",
                Parser.parseEvent("meeting /from 2019-10-15 1400 /to 2019-10-17 1600").toString());
    }

    @Test
    public void parseEvent_noFromSeparator_exceptionThrown() {
        ChioneException e = assertThrows(ChioneException.class, () ->
                Parser.parseEvent("meeting /to 2019-10-17 1600"));
        assertEquals("An event needs a start time after /from. "
                + "Try: event project meeting /from Mon 2pm /to 4pm", e.getMessage());
    }

    @Test
    public void parseEvent_noToSeparator_exceptionThrown() {
        ChioneException e = assertThrows(ChioneException.class, () ->
                Parser.parseEvent("meeting /from 2019-10-15 1400"));
        assertEquals("An event needs an end time after /to. "
                + "Try: event project meeting /from Mon 2pm /to 4pm", e.getMessage());
    }

    @Test
    public void parseEvent_emptyDescription_exceptionThrown() {
        ChioneException e = assertThrows(ChioneException.class, () ->
                Parser.parseEvent("/from 2019-10-15 1400 /to 2019-10-17 1600"));
        assertEquals("An event needs a description, a start and an end. "
                + "Try: event project meeting /from Mon 2pm /to 4pm", e.getMessage());
    }

    @Test
    public void parseTaskNumber_number_convertedToAZeroBasedIndex() throws ChioneException {
        // The user counts from 1, the list counts from 0.
        assertEquals(1, Parser.parseTaskNumber("2", CommandType.MARK));
    }

    @Test
    public void parseTaskNumber_nothingTyped_exceptionNamesTheCommand() {
        ChioneException e = assertThrows(ChioneException.class, () ->
                Parser.parseTaskNumber("", CommandType.DELETE));
        assertEquals("Tell me which task to delete. Try: delete 2", e.getMessage());
    }

    @Test
    public void parseTaskNumber_notANumber_exceptionQuotesWhatWasTyped() {
        ChioneException e = assertThrows(ChioneException.class, () ->
                Parser.parseTaskNumber("abc", CommandType.MARK));
        assertEquals("\"abc\" is not a task number. Try: mark 2", e.getMessage());
    }

    @Test
    public void parseTaskNumber_zero_acceptedHereAndRefusedByTheList() throws ChioneException {
        // Whether a number points at a task is the task list's business, so a
        // number that is merely out of range is not refused at this stage.
        assertEquals(-1, Parser.parseTaskNumber("0", CommandType.MARK));
    }

    @Test
    public void parseDate_day_returnsThatDay() throws ChioneException {
        assertEquals("Oct 15 2019", DateTimes.formatDate(Parser.parseDate("2019-10-15")));
    }

    @Test
    public void parseDate_nothingTyped_exceptionThrown() {
        ChioneException e = assertThrows(ChioneException.class, () -> Parser.parseDate(""));
        assertEquals("Tell me which day to look at. Try: on 2019-10-15", e.getMessage());
    }
}
