package bibi.command;

import bibi.BibiException;
import bibi.Storage;
import bibi.Ui;
import bibi.task.Task;
import bibi.task.TaskList;

/**
 * Adds one already-built task to the list.
 *
 * <p>The task arrives ready-made from {@link bibi.Parser Parser}, so this command is the
 * same whether the user typed a ToDo, a deadline, or an event.
 */
public class AddCommand extends Command {
    private final Task task;

    /**
     * Creates a command that will add the supplied task.
     *
     * @param task the task to add
     */
    public AddCommand(Task task) {
        this.task = task;
    }

    /**
     * Adds the task to the list, confirms it, and saves the longer list.
     *
     * @throws BibiException if the list already holds the same task
     */
    @Override
    public void execute(TaskList tasks, Ui ui, Storage storage) throws BibiException {
        int duplicateNumber = tasks.findSameTask(task);
        if (duplicateNumber > 0) {
            throw new BibiException("You already have that one, as task " + duplicateNumber
                    + ": " + tasks.get(duplicateNumber) + ". I have left the list as it is.");
        }

        tasks.add(task);
        ui.showMessage("Got it. I've added this task:");
        ui.showDetail(task.toString());
        ui.showPlain("Now you have " + tasks.size() + " tasks in the list.");
        saveTasks(tasks, ui, storage);
    }
}
