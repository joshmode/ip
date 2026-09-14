package bibi.command;

import bibi.BibiException;
import bibi.Storage;
import bibi.Ui;
import bibi.task.Task;
import bibi.task.TaskList;

/**
 * Marks one task incomplete again.
 */
public class UnmarkCommand extends Command {
    private final int taskNumber;

    /**
     * Creates a command that will reopen the given task.
     *
     * @param taskNumber the one-based number shown to the user
     */
    public UnmarkCommand(int taskNumber) {
        this.taskNumber = taskNumber;
    }

    /**
     * Marks the numbered task incomplete, shows it, and saves the change.
     *
     * <p>The task is shown for the same reason as in {@link MarkCommand}.
     */
    @Override
    public void execute(TaskList tasks, Ui ui, Storage storage) throws BibiException {
        Task task = tasks.get(taskNumber);
        task.markIncomplete();
        ui.showMessage("Task " + taskNumber + " is open again.");
        ui.showDetails(task.toString());
        saveTasks(tasks, ui, storage);
    }
}
