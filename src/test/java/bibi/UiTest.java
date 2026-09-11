package bibi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import bibi.task.Todo;

/**
 * Tests the capture mode the GUI depends on, and the wording that goes through
 * it. The console path writes to standard output and is covered instead by the
 * scripted tests in test/ui-test-plan.md.
 */
public class UiTest {

    private static Ui capturing() {
        Ui ui = new Ui();
        ui.startCapture();
        return ui;
    }

    @Test
    public void takeCapturedReply_withoutStartCapture_assertionThrown() {
        // Returning the previous capture would put a stale reply on screen.
        assertThrows(AssertionError.class, () -> new Ui().takeCapturedReply());
    }

    @Test
    public void startCapture_afterAPreviousCapture_startsEmpty() {
        Ui ui = capturing();
        ui.showMessage("first");
        ui.takeCapturedReply();

        ui.startCapture();
        ui.showMessage("second");

        assertEquals("second", ui.takeCapturedReply().text());
    }

    @Test
    public void showMessage_whileCapturing_omitsTheSpeakerPrefix() {
        Ui ui = capturing();
        ui.showMessage("hello");

        // The window puts Bibi's icon beside every reply, so the console's
        // "Bibi: " would only be noise repeated down the transcript.
        assertEquals("hello", ui.takeCapturedReply().text());
    }

    @Test
    public void showDetail_severalLines_eachIndentedInOrder() {
        Ui ui = capturing();
        ui.showDetail("one", "two", "three");

        assertEquals("  one\n  two\n  three", ui.takeCapturedReply().text());
    }

    @Test
    public void showDetail_noLines_addsNothing() {
        Ui ui = capturing();
        ui.showMessage("only this");
        ui.showDetail();

        assertEquals("only this", ui.takeCapturedReply().text());
    }

    @Test
    public void showNumberedTask_anyTask_prefixesTheNumber() throws BibiException {
        Ui ui = capturing();
        ui.showNumberedTask(7, new Todo("read book"));

        assertEquals("7. [T][ ] read book", ui.takeCapturedReply().text());
    }

    @Test
    public void showPlain_anyText_passesItThroughUnchanged() {
        Ui ui = capturing();
        ui.showPlain("no prefix, no indent");

        assertEquals("no prefix, no indent", ui.takeCapturedReply().text());
    }

    @Test
    public void showError_anyMessage_flagsTheReply() {
        Ui ui = capturing();
        ui.showError("something went wrong");

        Reply reply = ui.takeCapturedReply();
        assertTrue(reply.isError());
        assertEquals("something went wrong", reply.text());
    }

    @Test
    public void showMessage_ordinaryOutput_notFlaggedAsError() {
        Ui ui = capturing();
        ui.showMessage("all good");

        assertFalse(ui.takeCapturedReply().isError());
    }

    @Test
    public void showError_followedByANewCapture_flagIsCleared() {
        Ui ui = capturing();
        ui.showError("bad");
        ui.takeCapturedReply();

        ui.startCapture();
        ui.showMessage("fine");

        assertFalse(ui.takeCapturedReply().isError());
    }

    @Test
    public void showWelcome_always_namesBibiAndPointsAtTheCommands() {
        Ui ui = capturing();
        ui.showWelcome();

        String text = ui.takeCapturedReply().text();
        assertTrue(text.contains("Bibi online."));
        assertTrue(text.contains("todo"));
        assertTrue(text.contains("help"));
    }

    @Test
    public void showHelp_always_listsEveryCommand() {
        Ui ui = capturing();
        ui.showHelp();

        String text = ui.takeCapturedReply().text();
        for (String command : new String[] {"todo", "deadline", "event", "list", "sort",
            "find", "on", "mark", "unmark", "remove", "help", "bye"}) {
            assertTrue(text.contains(command), "help omitted " + command);
        }
    }

    @Test
    public void showBanner_always_drawsTheName() {
        Ui ui = capturing();
        ui.showBanner();

        assertTrue(ui.takeCapturedReply().text().contains("B B B B"));
    }

    @Test
    public void showGoodbye_always_saysTheTasksAreKept() {
        Ui ui = capturing();
        ui.showGoodbye();

        assertTrue(ui.takeCapturedReply().text().contains("Powering down."));
    }

    @Test
    public void showLoaded_oneTask_usesTheSingular() {
        Ui ui = capturing();
        ui.showLoaded(1);

        assertTrue(ui.takeCapturedReply().text().contains("1 task restored"));
    }

    @Test
    public void showLoaded_severalTasks_usesThePlural() {
        Ui ui = capturing();
        ui.showLoaded(4);

        assertTrue(ui.takeCapturedReply().text().contains("4 tasks restored"));
    }

    @Test
    public void showLoadWarnings_damagedLines_listsThemAndFlagsAnError() {
        Ui ui = capturing();
        ui.showLoadWarnings(List.of("Line 2: nonsense", "Line 5: also nonsense"));

        Reply reply = ui.takeCapturedReply();
        assertTrue(reply.isError());
        assertTrue(reply.text().contains("Line 2: nonsense"));
        assertTrue(reply.text().contains("Line 5: also nonsense"));
    }

    @Test
    public void showLoadingError_anyFailure_namesTheFileAndTheCause() {
        Ui ui = capturing();
        ui.showLoadingError(Path.of("data", "bibi.txt"), new NoSuchFileException("bibi.txt"));

        String text = ui.takeCapturedReply().text();
        assertTrue(text.contains("bibi.txt"));
        assertTrue(text.contains("NoSuchFileException"));
    }

    @Test
    public void showSaveError_anyFailure_warnsThatChangesAreAtRisk() {
        Ui ui = capturing();
        ui.showSaveError(Path.of("data", "bibi.txt"), new IOException("disk full"));

        String text = ui.takeCapturedReply().text();
        assertTrue(text.contains("bibi.txt"));
        assertTrue(text.contains("disk full"));
    }

    @Test
    public void describeCount_oneAndMany_pickTheRightPlural() {
        assertEquals("0 tasks", Ui.describeCount(0));
        assertEquals("1 task", Ui.describeCount(1));
        assertEquals("2 tasks", Ui.describeCount(2));
    }
}
