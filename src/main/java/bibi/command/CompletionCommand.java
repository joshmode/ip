package bibi.command;

import bibi.BibiException;
import bibi.Storage;
import bibi.Ui;
import bibi.task.Task;
import bibi.task.TaskList;

/**
 * Changes whether one task counts as done.
 *
 * <p>Ticking a task off and reopening it are the same piece of work: look the
 * numbered task up, set its completion, show what it now looks like, and save.
 * Only the state being asked for and the words used to confirm it differ, so
 * those are all a subclass supplies.
 *
 * <p>The task is shown, not only its number, so the user can see at a glance
 * that the task they meant is the one that changed.
 */
public abstract class CompletionCommand extends Command {
    private final int taskNumber;

    /**
     * Creates a command that will set the completion of the given task.
     *
     * @param taskNumber the one-based number shown to the user
     */
    protected CompletionCommand(int taskNumber) {
        this.taskNumber = taskNumber;
    }

    /**
     * Sets the numbered task's completion, confirms it, and saves the list.
     */
    @Override
    public void execute(TaskList tasks, Ui ui, Storage storage) throws BibiException {
        Task task = isCompleteWanted()
                ? tasks.markComplete(taskNumber)
                : tasks.markIncomplete(taskNumber);

        ui.showMessage(describeChange(taskNumber));
        ui.showDetails(task.toString());
        saveTasks(tasks, ui, storage);
    }

    /**
     * Reports the completion state this command asks for.
     *
     * @return {@code true} to mark the task done, {@code false} to reopen it
     */
    protected abstract boolean isCompleteWanted();

    /**
     * Returns the line confirming what happened to the task.
     *
     * @param taskNumber the one-based number the user referred to
     * @return the line to show above the task
     */
    protected abstract String describeChange(int taskNumber);
}
