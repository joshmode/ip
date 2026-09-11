package bibi;

import java.util.Arrays;
import java.util.Locale;

import bibi.command.AddCommand;
import bibi.command.Command;
import bibi.command.DeleteCommand;
import bibi.command.ExitCommand;
import bibi.command.FindCommand;
import bibi.command.HelpCommand;
import bibi.command.ListCommand;
import bibi.command.MarkCommand;
import bibi.command.OnCommand;
import bibi.command.SortCommand;
import bibi.command.UnmarkCommand;
import bibi.task.Deadline;
import bibi.task.Event;
import bibi.task.TaskDateTime;
import bibi.task.Todo;

/**
 * Turns a line of typed text into the {@link Command} it asks for.
 *
 * <p>All knowledge of command words and their syntax lives here, so the rest of
 * the program never inspects raw input. Anything the command needs is worked out
 * while parsing: a {@code deadline} line becomes a finished {@link Deadline}, and
 * {@code mark 2} becomes a number, so a command can assume its input is sound.
 *
 * <p>Input is treated generously where the user's meaning is obvious and strictly
 * where it is not. Any run of whitespace separates the command word from its
 * argument, so a stray tab or a double space is accepted; but a command given an
 * argument it cannot use, or a parameter supplied twice, is rejected rather than
 * silently ignored, because quietly dropping part of what someone typed is how a
 * user ends up with a task that is not what they asked for.
 */
public final class Parser {
    /** The commands that take no argument at all. */
    private static final String COMMANDS_WITHOUT_ARGUMENTS = "list, sort, help and bye";

    /**
     * Hides the constructor, because this class holds only static helpers and
     * is never meant to be instantiated.
     */
    private Parser() {
    }

    /**
     * Reads one line of user input and returns the command it describes.
     *
     * @param fullCommand the line the user typed
     * @return the command to execute
     * @throws BibiException if the line is empty, unknown, or malformed
     */
    public static Command parse(String fullCommand) throws BibiException {
        String input = fullCommand.strip();
        if (input.isEmpty()) {
            throw new BibiException("Please enter a command.");
        }

        // Splitting on any run of whitespace means a tab or a double space
        // between the command word and its argument is read as the user meant
        // it, rather than as an unknown command.
        String[] parts = input.split("\\s+", 2);
        String commandWord = parts[0].toLowerCase(Locale.ROOT);
        String argument = parts.length > 1 ? parts[1].strip() : "";

        return switch (commandWord) {
            case "bye" -> {
                requireNoArgument(argument, "bye");
                yield new ExitCommand();
            }
            case "list" -> {
                requireNoArgument(argument, "list");
                yield new ListCommand();
            }
            case "sort" -> {
                requireNoArgument(argument, "sort");
                yield new SortCommand();
            }
            case "help" -> {
                requireNoArgument(argument, "help");
                yield new HelpCommand();
            }
            case "todo" -> new AddCommand(new Todo(collapseSpaces(argument)));
            case "deadline" -> new AddCommand(parseDeadline(argument));
            case "event" -> new AddCommand(parseEvent(argument));
            case "mark" -> new MarkCommand(parseTaskNumber(argument, "mark"));
            case "unmark" -> new UnmarkCommand(parseTaskNumber(argument, "unmark"));
            case "remove" -> new DeleteCommand(parseTaskNumber(argument, "remove"));
            case "find" -> new FindCommand(requireKeyword(collapseSpaces(argument)));
            // Any time of day in the query is ignored, since the question is which
            // tasks belong to the day as a whole.
            case "on" -> new OnCommand(TaskDateTime.parse(argument).getDate());
            default -> throw new BibiException("I don't understand '" + parts[0] + "'. "
                    + "Try todo, deadline, event, list, sort, find, on, mark, unmark, remove, "
                    + "help, or bye.");
        };
    }

    /**
     * Rejects an argument given to a command that does not take one.
     *
     * <p>Ignoring the extra text would be friendlier in the moment and worse
     * overall: someone typing {@code list done} means to filter the list, and
     * showing them everything without a word looks like the filter worked.
     *
     * @param argument whatever followed the command word
     * @param commandWord the command being used, for the message
     * @throws BibiException if anything followed the command word
     */
    private static void requireNoArgument(String argument, String commandWord)
            throws BibiException {
        if (!argument.isEmpty()) {
            throw new BibiException(commandWord + " does not take anything after it, but I found '"
                    + argument + "'. " + COMMANDS_WITHOUT_ARGUMENTS + " are used on their own.");
        }
    }

    /**
     * Replaces runs of whitespace inside text with single spaces.
     *
     * <p>Applied to text the user typed freely, so that "read    book" and
     * "read book" are stored as the same description and are recognised as
     * duplicates of each other.
     *
     * @param text the text to tidy
     * @return the text with its inner spacing normalised
     */
    private static String collapseSpaces(String text) {
        return text.replaceAll("\\s+", " ").strip();
    }

    /**
     * Checks that a parameter such as {@code /by} was supplied exactly once.
     *
     * @param text the text being parsed
     * @param marker the parameter to count, for example {@code /by}
     * @throws BibiException if the parameter appears more than once
     */
    private static void requireSingleUse(String text, String marker) throws BibiException {
        long uses = Arrays.stream(text.split("\\s+"))
                .filter(token -> token.equalsIgnoreCase(marker))
                .count();
        if (uses > 1) {
            throw new BibiException("You used " + marker + " " + uses + " times, but it belongs "
                    + "exactly once. I would not know which one you meant.");
        }
    }

    /**
     * Builds a deadline from text in the form {@code description /by date}.
     *
     * @param deadlineText the text after the {@code deadline} command word
     * @return the finished deadline
     * @throws BibiException if the text is missing required information
     */
    private static Deadline parseDeadline(String deadlineText) throws BibiException {
        requireSingleUse(deadlineText, "/by");

        int byIndex = deadlineText.toLowerCase(Locale.ROOT).indexOf(" /by ");
        if (byIndex < 0) {
            throw new BibiException("Use deadline <description> /by <time>, "
                    + "for example: deadline return book /by 2019-10-15");
        }

        String description = collapseSpaces(deadlineText.substring(0, byIndex));
        String by = collapseSpaces(deadlineText.substring(byIndex + " /by ".length()));
        return new Deadline(description, by);
    }

    /**
     * Builds an event from text in the form
     * {@code description /from start /to end}.
     *
     * @param eventText the text after the {@code event} command word
     * @return the finished event
     * @throws BibiException if the text is missing required information
     */
    private static Event parseEvent(String eventText) throws BibiException {
        requireSingleUse(eventText, "/from");
        requireSingleUse(eventText, "/to");

        String normalizedText = eventText.toLowerCase(Locale.ROOT);
        int fromIndex = normalizedText.indexOf(" /from ");
        int toIndex = normalizedText.indexOf(" /to ");

        if (fromIndex >= 0 && toIndex >= 0 && toIndex < fromIndex) {
            throw new BibiException("The /to came before the /from. "
                    + "Use event <description> /from <start> /to <end>.");
        }
        if (fromIndex < 0 || toIndex < fromIndex + " /from ".length()) {
            throw new BibiException("Use event <description> /from <start> /to <end>, for example: "
                    + "event project meeting /from 2019-08-06 1400 /to 2019-08-06 1600");
        }

        String description = collapseSpaces(eventText.substring(0, fromIndex));
        String from = collapseSpaces(eventText.substring(fromIndex + " /from ".length(), toIndex));
        String to = collapseSpaces(eventText.substring(toIndex + " /to ".length()));
        return new Event(description, from, to);
    }

    /**
     * Checks that a search was given something to look for.
     *
     * @param keyword the text supplied after {@code find}
     * @return the keyword when it is usable
     * @throws BibiException if no keyword was supplied
     */
    private static String requireKeyword(String keyword) throws BibiException {
        if (keyword.isEmpty()) {
            throw new BibiException("Use find followed by a keyword, for example: find book");
        }
        return keyword;
    }

    /**
     * Reads the task number that a command such as {@code mark 2} refers to.
     *
     * @param numberText the text supplied after the command word
     * @param commandWord the command word, used in the error message
     * @return the one-based task number
     * @throws BibiException if the text is not a usable task number
     */
    private static int parseTaskNumber(String numberText, String commandWord)
            throws BibiException {
        if (numberText.isEmpty()) {
            throw new BibiException("Use " + commandWord + " followed by a task number, "
                    + "for example: " + commandWord + " 2");
        }

        int taskNumber;
        try {
            taskNumber = Integer.parseInt(numberText);
        } catch (NumberFormatException notANumber) {
            throw new BibiException("'" + numberText + "' is not a task number. Use "
                    + commandWord + " followed by one number, for example: " + commandWord + " 2");
        }

        // Caught here rather than in TaskList so the message can say what is
        // wrong with the number itself, not merely that no such task exists.
        if (taskNumber < 1) {
            throw new BibiException("Task numbers start at 1, so " + taskNumber
                    + " cannot refer to a task. Use list to see the numbers.");
        }
        return taskNumber;
    }
}
