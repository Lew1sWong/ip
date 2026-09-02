package chione.gui;

import java.io.IOException;
import java.util.Collections;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

/**
 * One message in the conversation: a picture of who is speaking, and what they
 * said.
 *
 * <p>The user's messages and Chione's differ only in which way round those two
 * sit, so both are built here and one of them is flipped. A dialog box is made
 * through {@link #getUserDialog} or {@link #getChioneDialog} rather than with
 * {@code new}, because the names say which speaker is meant, while a constructor
 * taking a picture would not.
 */
public class DialogBox extends HBox {
    @FXML
    private Label dialog;

    @FXML
    private ImageView displayPicture;

    /**
     * Builds a dialog box from the shared layout.
     *
     * <p>Unusually, this loads FXML with itself as both the root and the
     * controller. That is what {@code fx:root} in {@code DialogBox.fxml} allows,
     * and it is what makes a {@code DialogBox} usable as an ordinary control
     * rather than something that has to be unwrapped from a loader first.
     *
     * @param text what was said
     * @param image the speaker's picture
     */
    private DialogBox(String text, Image image) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(MainWindow.class.getResource("/view/DialogBox.fxml"));
            fxmlLoader.setController(this);
            fxmlLoader.setRoot(this);
            fxmlLoader.load();
        } catch (IOException e) {
            // As in Main: the layout is packaged with the program, so this can
            // only mean a broken build.
            e.printStackTrace();
        }

        dialog.setText(text);
        displayPicture.setImage(image);
    }

    /**
     * Creates a dialog box for something the user said.
     *
     * @param text what the user typed
     * @param image the user's picture
     * @return a dialog box with the picture on the right
     */
    public static DialogBox getUserDialog(String text, Image image) {
        return new DialogBox(text, image);
    }

    /**
     * Creates a dialog box for something Chione said.
     *
     * @param text Chione's reply
     * @param image Chione's picture
     * @return a dialog box with the picture on the left
     */
    public static DialogBox getChioneDialog(String text, Image image) {
        DialogBox dialogBox = new DialogBox(text, image);
        dialogBox.flip();
        return dialogBox;
    }

    /**
     * Turns the dialog box around, so the picture is on the left and the text on
     * the right.
     *
     * <p>Having the two speakers face each other from opposite sides is what makes
     * the conversation readable at a glance, without labelling every line.
     */
    private void flip() {
        ObservableList<Node> children = FXCollections.observableArrayList(this.getChildren());
        Collections.reverse(children);
        getChildren().setAll(children);
        setAlignment(Pos.TOP_LEFT);
    }
}
