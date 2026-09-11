package bibi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import bibi.command.AddCommand;
import bibi.command.Command;
import bibi.command.DeleteCommand;
import bibi.command.ExitCommand;
import bibi.command.FindCommand;
import bibi.command.HelpCommand;
import bibi.command.ListCommand;
import bibi.command.MarkCommand;
import bibi.command.OnCommand;
import bibi.command.UnmarkCommand;
import bibi.task.TaskList;

/**
 * Tests that typed input is turned into the right command, and that malformed
 * input is refused before any command is built.
 */
public class ParserTest {

    @Test
    public void parse_wordOnlyCommands_matchedWithoutArguments() throws BibiException {
        assertInstanceOf(ListCommand.class, Parser.parse("list"));
        assertInstanceOf(HelpCommand.class, Parser.parse("help"));
        assertInstanceOf(ExitCommand.class, Parser.parse("bye"));
    }

    @Test
    public void parse_mixedCaseCommandWord_stillMatched() throws BibiException {
        assertInstanceOf(ListCommand.class, Parser.parse("LIST"));
        assertInstanceOf(ExitCommand.class, Parser.parse("Bye"));
    }

    @Test
    public void parse_surroundingSpaces_ignored() throws BibiException {
        assertInstanceOf(ListCommand.class, Parser.parse("   list   "));
    }

    @Test
    public void parse_exitCommand_isExitTrue() throws BibiException {
        assertTrue(Parser.parse("bye").isExit());
    }

    @Test
    public void parse_otherCommands_isExitFalse() throws BibiException {
        assertFalse(Parser.parse("list").isExit());
        assertFalse(Parser.parse("todo read book").isExit());
    }

    @Test
    public void parse_addCommands_allThreeTaskTypesBecomeAddCommand() throws BibiException {
        assertInstanceOf(AddCommand.class, Parser.parse("todo read book"));
        assertInstanceOf(AddCommand.class, Parser.parse("deadline return book /by 2019-10-15"));
        assertInstanceOf(AddCommand.class,
                Parser.parse("event camp /from 2019-08-10 /to 2019-08-12"));
    }

    @Test
    public void parse_numberedCommands_matchedWithTheirNumber() throws BibiException {
        assertInstanceOf(MarkCommand.class, Parser.parse("mark 2"));
        assertInstanceOf(UnmarkCommand.class, Parser.parse("unmark 2"));
        assertInstanceOf(DeleteCommand.class, Parser.parse("remove 2"));
    }

    @Test
    public void parse_findCommand_matched() throws BibiException {
        assertInstanceOf(FindCommand.class, Parser.parse("find book"));
    }

    @Test
    public void parse_findKeywordWithSpaces_keptWhole() throws BibiException {
        assertInstanceOf(FindCommand.class, Parser.parse("find sports club"));
    }

    @Test
    public void parse_findWithoutKeyword_exceptionThrown() {
        BibiException thrown = assertThrows(BibiException.class, () -> Parser.parse("find"));
        assertEquals("Use find followed by a keyword, for example: find book",
                thrown.getMessage());
        assertThrows(BibiException.class, () -> Parser.parse("find    "));
    }

    @Test
    public void parse_onCommand_matched() throws BibiException {
        assertInstanceOf(OnCommand.class, Parser.parse("on 2019-08-11"));
    }

    @Test
    public void parse_emptyInput_exceptionThrown() {
        BibiException thrown = assertThrows(BibiException.class, () -> Parser.parse(""));
        assertEquals("Please enter a command.", thrown.getMessage());
        assertThrows(BibiException.class, () -> Parser.parse("    "));
    }

    @Test
    public void parse_unknownCommand_exceptionThrown() {
        BibiException thrown = assertThrows(BibiException.class, () -> Parser.parse("remind me"));
        // The message now names the word it could not place, which is the part
        // that helps a user who simply mistyped.
        assertTrue(thrown.getMessage().contains("I don't understand 'remind'"));
    }

    @Test
    public void parse_commandWordAsPrefixOfAnotherWord_notMatched() {
        // "listen" starts with "list" but is not the list command.
        assertThrows(BibiException.class, () -> Parser.parse("listen"));
    }

    @Test
    public void parse_todoWithoutDescription_exceptionThrown() {
        BibiException thrown = assertThrows(BibiException.class, () -> Parser.parse("todo"));
        assertTrue(thrown.getMessage().contains("Use todo followed by a description."));
    }

    @Test
    public void parse_deadlineWithoutByMarker_exceptionThrown() {
        BibiException thrown = assertThrows(BibiException.class, () ->
                Parser.parse("deadline return book"));
        assertTrue(thrown.getMessage().startsWith("Use deadline <description> /by <time>"));
    }

    @Test
    public void parse_deadlineWithUnreadableDate_exceptionThrown() {
        assertThrows(BibiException.class, () -> Parser.parse("deadline return book /by someday"));
    }

    @Test
    public void parse_eventMissingToMarker_exceptionThrown() {
        BibiException thrown = assertThrows(BibiException.class, () ->
                Parser.parse("event camp /from 2019-08-10"));
        assertTrue(thrown.getMessage()
                .startsWith("Use event <description> /from <start> /to <end>"));
    }

    @Test
    public void parse_eventEndingBeforeItStarts_exceptionThrown() {
        BibiException thrown = assertThrows(BibiException.class, () ->
                Parser.parse("event camp /from 2019-08-12 /to 2019-08-10"));
        assertEquals("An event cannot end before it starts.", thrown.getMessage());
    }

    @Test
    public void parse_taskNumberNotANumber_exceptionThrown() {
        BibiException thrown = assertThrows(BibiException.class, () -> Parser.parse("mark two"));
        assertTrue(thrown.getMessage().contains("'two' is not a task number"));
    }

    @Test
    public void parse_taskNumberMissing_exceptionThrown() {
        BibiException thrown = assertThrows(BibiException.class, () -> Parser.parse("remove"));
        assertTrue(thrown.getMessage().contains("Use remove followed by a task number"));
    }

    @Test
    public void parse_onWithUnreadableDate_exceptionThrown() {
        assertThrows(BibiException.class, () -> Parser.parse("on someday"));
    }

    @Test
    public void parse_addCommand_taskIsBuiltFromTheInput(@TempDir Path tempDir)
            throws BibiException {
        // The parser is responsible for building the task, so executing the
        // command should add exactly what was typed.
        TaskList tasks = new TaskList();
        Command command = Parser.parse("deadline return book /by 2/12/2019 1800");
        command.execute(tasks, new Ui(), new Storage(tempDir.resolve("bibi.txt")));

        assertEquals(1, tasks.size());
        assertEquals("[D][ ] return book (by: Dec 02 2019 6:00PM)", tasks.get(1).toString());
    }

    @Test
    public void parse_tabBetweenCommandAndArgument_accepted() throws BibiException {
        // Any run of whitespace separates the command word from its argument,
        // so a tab is read as the user meant it rather than as an unknown word.
        assertInstanceOf(AddCommand.class, Parser.parse("todo\tread book"));
    }

    @Test
    public void parse_repeatedInnerSpaces_collapsedInDescription() throws BibiException {
        TaskList tasks = new TaskList();
        Parser.parse("todo   read    book").execute(tasks, new Ui(), new Storage(Path.of("x")));

        assertEquals("[T][ ] read book", tasks.get(1).toString());
    }

    @Test
    public void parse_byGivenTwice_exceptionThrown() {
        BibiException thrown = assertThrows(BibiException.class, () ->
                Parser.parse("deadline a /by 2019-10-15 /by 2019-11-11"));
        assertTrue(thrown.getMessage().contains("You used /by 2 times"));
    }

    @Test
    public void parse_toGivenTwice_exceptionThrown() {
        BibiException thrown = assertThrows(BibiException.class, () ->
                Parser.parse("event a /from 2019-08-06 /to 2019-08-07 /to 2019-08-08"));
        assertTrue(thrown.getMessage().contains("You used /to 2 times"));
    }

    @Test
    public void parse_toBeforeFrom_exceptionNamesTheOrder() {
        BibiException thrown = assertThrows(BibiException.class, () ->
                Parser.parse("event a /to 2019-08-07 /from 2019-08-06"));
        assertTrue(thrown.getMessage().contains("/to came before the /from"));
    }

    @Test
    public void parse_argumentAfterArgumentlessCommand_exceptionThrown() {
        for (String command : new String[] {"list extra", "sort now", "help me", "bye now"}) {
            BibiException thrown = assertThrows(BibiException.class, () -> Parser.parse(command));
            assertTrue(thrown.getMessage().contains("does not take anything after it"),
                    "no complaint for: " + command);
        }
    }

    @Test
    public void parse_taskNumberBelowOne_exceptionExplainsNumbering() {
        for (String command : new String[] {"mark 0", "unmark -1", "remove -7"}) {
            BibiException thrown = assertThrows(BibiException.class, () -> Parser.parse(command));
            assertTrue(thrown.getMessage().contains("Task numbers start at 1"),
                    "no complaint for: " + command);
        }
    }

    @Test
    public void parse_commandWordInAnyCase_recognized() throws BibiException {
        assertInstanceOf(ListCommand.class, Parser.parse("LIST"));
        assertInstanceOf(AddCommand.class, Parser.parse("ToDo read book"));
    }
}
