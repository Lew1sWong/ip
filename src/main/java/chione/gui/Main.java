package chione.gui;

import java.io.IOException;

import chione.Chione;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

/**
 * The window Chione runs in.
 *
 * <p>Its whole job is to put the main window on screen and hand it a
 * {@link Chione} to talk to. What the window looks like is described in
 * {@code MainWindow.fxml}, and how it behaves belongs to {@link MainWindow}, so
 * neither concern is written out here.
 */
public class Main extends Application {
    /**
     * The chatbot behind the window.
     *
     * <p>Built here rather than inside {@link MainWindow} so that the window
     * stays a piece of user interface: it is handed a chatbot to talk to, and
     * could be handed a different one.
     */
    private final Chione chione = new Chione();

    @Override
    public void start(Stage stage) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("/view/MainWindow.fxml"));
            AnchorPane root = fxmlLoader.load();
            Scene scene = new Scene(root);

            stage.setScene(scene);
            stage.setTitle("Chione");
            stage.setMinHeight(400.0);
            stage.setMinWidth(417.0);

            fxmlLoader.<MainWindow>getController().setChione(chione);
            stage.show();
        } catch (IOException e) {
            // The FXML is packaged with the program rather than supplied by the
            // user, so failing to read it means the build is broken, not that
            // anyone did anything wrong. There is no window to apologize in.
            e.printStackTrace();
        }
    }
}
