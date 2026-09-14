package bibi.command;

import bibi.BibiException;
import bibi.Storage;
import bibi.Ui;
import bibi.task.Task;
import bibi.task.TaskList;

/**
 * Marks one task complete.
 */
public class MarkCommand extends Command {
    private final int taskNumber;

    /**
     * Creates a command that will complete the given task.
     *
     * @param taskNumber the one-based number shown to the user
     */
    public MarkCommand(int taskNumber) {
        this.taskNumber = taskNumber;
    }

    /**
     * Marks the numbered task complete, shows it, and saves the change.
     *
     * <p>The task itself is shown, not only its number, so the user can see at
     * a glance that the task they meant is the one that changed.
     */
    @Override
    public void execute(TaskList tasks, Ui ui, Storage storage) throws BibiException {
        Task task = tasks.markComplete(taskNumber);
        ui.showMessage("Task " + taskNumber + " done. Look at us getting things done.");
        ui.showDetails(task.toString());
        saveTasks(tasks, ui, storage);
    }
}
