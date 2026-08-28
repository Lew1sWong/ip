package chione.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import chione.ChioneException;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

/**
 * Tests {@link TaskList}, which both holds the tasks and guards its own bounds.
 *
 * <p>The bounds checking gets the most attention here, because it is the one
 * place standing between a mistyped task number and an exception thrown from
 * somewhere that could not explain it.
 */
public class TaskListTest {
    /** Builds a list holding a todo, a deadline and a three-day event. */
    private TaskList buildSampleList() {
        TaskList tasks = new TaskList();
        tasks.add(new Todo("read book"));
        tasks.add(new Deadline("return book", LocalDateTime.of(2019, 10, 15, 18, 0)));
        tasks.add(new Event("meeting",
                LocalDateTime.of(2019, 10, 15, 14, 0),
                LocalDateTime.of(2019, 10, 17, 16, 0)));
        return tasks;
    }

    @Test
    public void isEmpty_newList_true() {
        assertTrue(new TaskList().isEmpty());
    }

    @Test
    public void isEmpty_afterAdding_false() {
        TaskList tasks = new TaskList();
        tasks.add(new Todo("read book"));
        assertFalse(tasks.isEmpty());
    }

    @Test
    public void add_severalTasks_sizeMatches() {
        assertEquals(3, buildSampleList().size());
    }

    @Test
    public void get_firstTask_returnedInInsertionOrder() throws ChioneException {
        assertEquals("[T][ ] read book", buildSampleList().get(0).toString());
    }

    @Test
    public void get_onePastTheEnd_exceptionCountsFromOne() {
        // The message has to speak in the numbers the user saw, not in indices.
        ChioneException e = assertThrows(ChioneException.class, () -> buildSampleList().get(3));
        assertEquals("There is no task 4. Your list has 3 task(s) right now.", e.getMessage());
    }

    @Test
    public void get_negativeIndex_exceptionThrown() {
        // "mark 0" arrives here as index -1.
        ChioneException e = assertThrows(ChioneException.class, () -> buildSampleList().get(-1));
        assertEquals("There is no task 0. Your list has 3 task(s) right now.", e.getMessage());
    }

    @Test
    public void get_anyIndexOnAnEmptyList_exceptionThrown() {
        ChioneException e = assertThrows(ChioneException.class, () -> new TaskList().get(0));
        assertEquals("There is no task 1. Your list has 0 task(s) right now.", e.getMessage());
    }

    @Test
    public void remove_middleTask_returnedAndTakenOut() throws ChioneException {
        TaskList tasks = buildSampleList();
        Task removed = tasks.remove(1);

        assertEquals("[D][ ] return book (by: Oct 15 2019, 6:00pm)", removed.toString());
        assertEquals(2, tasks.size());
        // What followed it has moved up into its place.
        assertEquals("[E][ ] meeting (from: Oct 15 2019, 2:00pm to: Oct 17 2019, 4:00pm)",
                tasks.get(1).toString());
    }

    @Test
    public void remove_onePastTheEnd_listLeftAlone() {
        TaskList tasks = buildSampleList();
        assertThrows(ChioneException.class, () -> tasks.remove(3));
        assertEquals(3, tasks.size());
    }

    @Test
    public void findOn_dayOfTheDeadline_deadlineAndEventFound() {
        TaskList found = buildSampleList().findOn(LocalDate.of(2019, 10, 15));
        assertEquals(2, found.size());
    }

    @Test
    public void findOn_dayInsideTheEvent_onlyTheEventFound() throws ChioneException {
        // The event runs 15th to 17th, so the 16th is a day it covers even though
        // it neither starts nor ends then.
        TaskList found = buildSampleList().findOn(LocalDate.of(2019, 10, 16));
        assertEquals(1, found.size());
        assertEquals("[E][ ] meeting (from: Oct 15 2019, 2:00pm to: Oct 17 2019, 4:00pm)",
                found.get(0).toString());
    }

    @Test
    public void findOn_lastDayOfTheEvent_stillFound() {
        assertEquals(1, buildSampleList().findOn(LocalDate.of(2019, 10, 17)).size());
    }

    @Test
    public void findOn_dayAfterTheEvent_nothingFound() {
        assertTrue(buildSampleList().findOn(LocalDate.of(2019, 10, 18)).isEmpty());
    }

    @Test
    public void findOn_dayBeforeEverything_nothingFound() {
        assertTrue(buildSampleList().findOn(LocalDate.of(2019, 10, 14)).isEmpty());
    }

    @Test
    public void findOn_anyDay_todoNeverFound() {
        // A todo has no date, so no day can be the day it falls on.
        TaskList todosOnly = new TaskList();
        todosOnly.add(new Todo("read book"));
        assertTrue(todosOnly.findOn(LocalDate.of(2019, 10, 15)).isEmpty());
    }

    @Test
    public void findOn_matches_originalListUnchanged() {
        TaskList tasks = buildSampleList();
        tasks.findOn(LocalDate.of(2019, 10, 15));
        assertEquals(3, tasks.size());
    }

    @Test
    public void asList_addingThroughTheView_exceptionThrown() {
        // The view is handed out for reading only; changing the list has to go
        // through the methods above.
        assertThrows(UnsupportedOperationException.class,
                () -> buildSampleList().asList().add(new Todo("sneaked in")));
    }

    @Test
    public void asList_tasks_sameOrderAsTheList() {
        assertEquals(3, buildSampleList().asList().size());
        assertEquals("[T][ ] read book", buildSampleList().asList().get(0).toString());
    }
}
