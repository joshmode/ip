package bibi;

import java.io.IOException;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

/**
 * One turn of the conversation.
 *
 * <p>The two speakers are deliberately not shown alike. This is a person talking
 * to a tool, not two people talking to each other, and laying both sides out the
 * same way makes the app look like a chat between equals while wasting the space
 * that Bibi's longer answers actually need.
 *
 * <p>So Bibi's replies run the full width of the window behind a small icon, and
 * the user's own lines sit on the right as a short tinted chip with no picture at
 * all: the user knows who they are, and a portrait of them earns none of the room
 * it would take. A reply that reports a problem is styled apart again, so an
 * error cannot be mistaken for a confirmation at a glance.
 */
public class DialogBox extends HBox {
    private static final String DIALOG_BOX_FXML = "/view/DialogBox.fxml";

    /** How much of the window's width one of the user's chips may occupy. */
    private static final double USER_CHIP_WIDTH_FRACTION = 0.75;

    @FXML
    private Label dialog;

    @FXML
    private ImageView displayPicture;

    /**
     * Creates a dialog box showing one line of the conversation.
     *
     * @param text what was said
     */
    private DialogBox(String text) {
        try {
            FXMLLoader loader = new FXMLLoader(DialogBox.class.getResource(DIALOG_BOX_FXML));
            // This object is both the root and the controller, which is what lets
            // a dialog box be created in code and still be laid out by FXML.
            loader.setRoot(this);
            loader.setController(this);
            loader.load();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load " + DIALOG_BOX_FXML, exception);
        }

        dialog.setText(text);
    }

    /**
     * Returns the box for something the user typed.
     *
     * <p>Shown as a chip on the right with no picture. It is capped at a fraction
     * of the window rather than a fixed width, so it stays a chip as the window
     * grows instead of stretching into a banner.
     *
     * @param text what the user typed
     * @return the finished box
     */
    public static DialogBox forUser(String text) {
        DialogBox box = new DialogBox(text);

        box.displayPicture.setVisible(false);
        box.displayPicture.setManaged(false);
        box.setAlignment(Pos.TOP_RIGHT);
        box.getStyleClass().add("user-row");
        box.dialog.getStyleClass().add("user-chip");
        box.dialog.maxWidthProperty().bind(box.widthProperty()
                .multiply(USER_CHIP_WIDTH_FRACTION));
        return box;
    }

    /**
     * Returns the box for one of Bibi's replies.
     *
     * @param reply what Bibi said, and whether it reports a problem
     * @param icon the small picture shown beside the reply
     * @return the finished box
     */
    public static DialogBox forBibi(Reply reply, Image icon) {
        DialogBox box = new DialogBox(reply.text());

        box.displayPicture.setImage(icon);
        box.setAlignment(Pos.TOP_LEFT);
        box.getStyleClass().add("bibi-row");
        box.dialog.getStyleClass().add(reply.isError() ? "error-text" : "bibi-text");

        // Letting the label take the leftover width is what makes a long reply
        // wrap into the window rather than being squeezed into a column.
        box.dialog.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(box.dialog, Priority.ALWAYS);
        return box;
    }
}
