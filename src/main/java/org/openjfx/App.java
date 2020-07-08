package org.openjfx;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.WorkerStateEvent;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import org.apache.commons.io.FilenameUtils;

import java.awt.*;
import java.io.*;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.*;
import java.util.List;

/**
 * Encrypteur et Decrypteur de PDF
 * @author Constant Malanda
 *
 */
public class App extends Application {

    @Override
    public void start(Stage stage) {
        initUI(stage);
    }

    List<File> files = new LinkedList<>();
    Scene scene;
    TextField paths;

    private void initUI(Stage stage) {
        // Init Box
        final VBox vb = new VBox();
        vb.setSpacing(10);
        vb.setPadding(new Insets(15,20, 10,10));

        final HBox hb1 = new HBox();
        final HBox hb2 = new HBox();
        hb1.setSpacing(10);
        hb2.setSpacing(10);

        // Path field area
        paths = new TextField("");
        paths.setMinWidth(120);
        hb1.getChildren().add(paths);

        // File chooser button
        Button fileButton = initFileChooserButton(stage);
        hb1.getChildren().add(fileButton);

        // Encrypt Button
        Button encryptButton = new Button("Encrypt");
        encryptButton.setOnAction(event -> encryptButtonAction());

        vb.getChildren().add(hb1);
        hb2.getChildren().add(encryptButton);

        //Decrypt Button
        Button decryptButton = new Button("Decrypt");
        decryptButton.setOnAction(event -> decryptButtonAction());

        hb2.getChildren().add(decryptButton);
        vb.getChildren().add(hb2);

        // Drag'n'drop
        vb.setOnDragOver(event -> {
            event.acceptTransferModes(TransferMode.ANY);
            event.consume();
        });

        vb.setOnDragDropped(event -> {
            if (!event.getDragboard().hasFiles())
                return;
            files = event.getDragboard().getFiles();
            for (File file : files) {
                if (!FilenameUtils.getExtension(file.getName()).equals("pdf"))
                    files.remove(file);
                printPaths();
            }
            System.out.println("Got " + files.size() + " files");
            event.consume();
        });

        scene =  new Scene(vb, 270, 100);

        stage.setResizable(false);
        stage.setTitle("PdfEncryptor");
        stage.setScene(scene);
        stage.show();
    }

    private void encryptButtonAction() {
        // If no file is selected
        if (paths.getText().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText("Une erreur est survenue");
            alert.setContentText("Aucun fichier n'a été spécifié");

            alert.showAndWait();
        } else {
            final Cursor cursor = scene.getCursor();
            scene.setCursor(Cursor.WAIT);
            String password = PwdGenerator.generatePassayPassword();
            EncryptService encryptService = new EncryptService(files, password);
            encryptService.setOnSucceeded((WorkerStateEvent event) -> {
                scene.setCursor(cursor);
                int encrypted = encryptService.getValue();
                FileSystem fs = FileSystems.getDefault();
                Path userPath = fs.getPath(System.getProperty("user.home"), "Documents/PdfEncryptor");
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Information Dialog");
                alert.setHeaderText(null);

                String alertText = encrypted + " sur " + this.files.size() + " fichiers ont pu être encrypté" +
                        "\nLe mot de passe utilisé est : ";

                TextFlow flow = new TextFlow(new Text(alertText), buildPwdHyperlink(password));
                alert.getDialogPane().setContent(flow);

                alert.showAndWait();

                // open the output folder
                Desktop desktop = Desktop.getDesktop();
                if (desktop.isSupported(Desktop.Action.BROWSE) && encrypted > 0) {
                    try {
                        desktop.browse(userPath.toUri());
                    } catch (Exception e) {
                        alert = BacktraceDialog(e);
                        alert.showAndWait();
                    }
                }
                paths.clear();
            });
            encryptService.start();
        }
    }

    private void decryptButtonAction() {
        if (paths.getText().isEmpty()) {
            Alert alert = new  Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText("Une erreur est survenue");
            alert.setContentText("Aucun fichier n'a été spécifié");

            alert.showAndWait();
        }
        else {
            PasswordDialog passwordDialog = new PasswordDialog();
            Optional<String> result = passwordDialog.showAndWait();
            if (result.isPresent()) {
                String decryptPassword = result.get();
                final Cursor cursor = scene.getCursor();
                scene.setCursor(Cursor.WAIT);
                DecryptService decryptService = new DecryptService(files, decryptPassword);
                decryptService.setOnSucceeded(event -> {
                    int decrypted = decryptService.getValue();
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Information Dialog");
                    alert.setHeaderText(null);
                    alert.setContentText(decrypted + " sur " + this.files.size() + " fichiers ont pu être décrypté avec ce mot de passe");

                    alert.showAndWait();

                    scene.setCursor(cursor);
                    // open the output folder
                    Desktop desktop = Desktop.getDesktop();
                    if (desktop.isSupported(Desktop.Action.BROWSE) && decrypted > 0) {
                        try {
                            desktop.browse(files.get(0).toURI());
                        } catch (Exception e) {
                            alert = BacktraceDialog(e);
                            alert.showAndWait();
                        }
                    }
                    paths.clear();
                });
                decryptService.start();
            }
        }
    }

    static Hyperlink buildPwdHyperlink(String password) {
        Hyperlink pwdHyperlink = new Hyperlink(password);

        pwdHyperlink.setOnAction(event -> PwdToClipboard(password));

        return pwdHyperlink;
    }

    private static void PwdToClipboard(String password) {
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        content.putString(password);
        clipboard.setContent(content);

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setContentText("Le mot de passe a été copié dans le presse-papier");
        alert.showAndWait();
    }

    private Button initFileChooserButton(Stage stage) {
        final FileChooser fileChooser = new FileChooser();

        // Set title for the file chooser
        fileChooser.setTitle("Select Pdf to encrypt");

        // Set Initial directory
        fileChooser.setInitialDirectory(new File(System.getProperty("user.dir")));

        // Set Pdf extension filter
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));

        // Init file chooser Button
        final Button fileButton = new Button("Select pdf");
        fileButton.setOnAction(actionEvent -> {
            paths.clear();
            files = fileChooser.showOpenMultipleDialog(stage);
            printPaths();
        });

        return fileButton;
    }

    private void printPaths() {
        if (files == null || files.isEmpty())
            return;
        for(File file : files) {
            paths.appendText(file.getAbsolutePath() + ";");
        }
    }

    static Alert BacktraceDialog(Exception e) {

        // Show Error Alert + stacktrace if there is an unknown error
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText("Une erreur est survenue");

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        String exceptionText = sw.toString();

        Label label = new Label("The exception stacktrace was:");

        TextArea textArea = new TextArea(exceptionText);
        textArea.setEditable(false);
        textArea.setWrapText(true);

        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea, Priority.ALWAYS);

        GridPane expContent = new GridPane();
        expContent.setMaxWidth(Double.MAX_VALUE);
        expContent.add(label, 0, 0);
        expContent.add(textArea, 0, 1);

        // Set expandable Exception into the dialog pane.
        alert.getDialogPane().setExpandableContent(expContent);

        return alert;
    }

    public static void main(String[] args) {
        launch();
    }
}

class PasswordDialog extends Dialog<String> {
    private final PasswordField passwordField;

    public PasswordDialog() {
        setTitle("Password");
        setHeaderText("Please enter your password.");

        ButtonType passwordButtonType = new ButtonType("Decrypt", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(passwordButtonType, ButtonType.CANCEL);

        passwordField = new PasswordField();
        passwordField.setPromptText("Password");

        HBox hBox = new HBox();
        hBox.getChildren().add(passwordField);
        hBox.setPadding(new Insets(20));

        HBox.setHgrow(passwordField, Priority.ALWAYS);

        getDialogPane().setContent(hBox);

        Platform.runLater(passwordField::requestFocus);

        setResultConverter(dialogButton -> {
            if (passwordButtonType == dialogButton) return passwordField.getText();
            return null;
        });
    }
}



