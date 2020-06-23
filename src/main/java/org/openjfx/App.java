package org.openjfx;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;

import java.io.*;
import java.util.List;
import java.util.Optional;

/**
 * JavaFX App
 */
public class App extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        initUI(stage);
    }

    List<File> files;

    private void initUI(Stage stage) {
        final FileChooser fileChooser = new FileChooser();
        setupFileChooser(fileChooser);

        // Init Box
        final VBox vb = new VBox();
        vb.setSpacing(10);
        vb.setPadding(new Insets(15,20, 10,10));

        final HBox hb = new HBox();
        hb.setSpacing(10);


        // Path field area
        final TextField paths = new TextField("");
        paths.setMinWidth(120);
        hb.getChildren().add(paths);

        // File chooser button
        final Button fileButton = new Button("Select pdf");
        fileButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                paths.clear();
                files = fileChooser.showOpenMultipleDialog(stage);
                printPaths(paths, files);
            }

            private void printPaths(TextField paths, List<File> files) {
                if (files == null || files.isEmpty())
                    return;
                for(File file : files) {
                    paths.appendText(file.getAbsolutePath() + ";");
                }
            }
        });
        hb.getChildren().add(fileButton);

        // Encrypt Button
        Button encryptButton = new Button("Encrypt");
        encryptButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                PasswordDialog pd  = new PasswordDialog();
                Optional<String> result = pd.showAndWait();
                if (result.isPresent())
                {
                    String password = result.get();
                    PDDocument pdf;
                    int encrypted = 0;
                    for (File file : files) {
                        try {
                            pdf = Encryptor.encrypt(file, password);
                            pdf.save(removeExtension(file) + "_encrypted.pdf");
                            encrypted++;
                        }
                        catch (FileNotFoundException e){
                            Alert alert = new Alert(Alert.AlertType.ERROR);
                            alert.setTitle("Erreur");
                            alert.setHeaderText("Une erreur est survenue");
                            alert.setContentText("Impossible de trouver le fichier " + file.getName());

                            alert.showAndWait();
                        }
                        catch (Exception e){
                            Alert alert = new Alert(Alert.AlertType.ERROR);
                            alert.setTitle("Erreur");
                            alert.setHeaderText("Une erreur est survenue");
                            alert.setContentText("Impossible d'encrypter le fichier " + file.getName());

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

                            alert.showAndWait();
                        }
                    }
                    paths.clear();
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Information Dialog");
                    alert.setHeaderText(null);
                    alert.setContentText(encrypted + " sur " + files.size() + " fichiers ont pu être encrypté");

                    alert.showAndWait();
                }
                else  {
                    System.out.println("No password");
                }
            }

            private String removeExtension(File file) {
                String fileName = file.getAbsolutePath();
                if (fileName.indexOf(".") > 0)
                    fileName = fileName.substring(0, fileName.lastIndexOf("."));
                return fileName;
            }
        });
        vb.getChildren().add(hb);
        vb.getChildren().add(encryptButton);

        Scene scene =  new Scene(vb, 270, 100);

        stage.setResizable(false);
        stage.setTitle("PdfEncryptor");
        stage.setScene(scene);
        stage.show();
    }

    private void setupFileChooser(FileChooser fileChooser) {
        // Set title for the file chooser
        fileChooser.setTitle("Select Pdf to encrypt");

        // Set Initial directory
        fileChooser.setInitialDirectory(new File(System.getProperty("user.dir")));

        // Set Pdf extension filter
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
    }

    public static void main(String[] args) {
        launch();
    }

}

class PasswordDialog extends Dialog<String> {
    private PasswordField passwordField;

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

        Platform.runLater(() -> passwordField.requestFocus());

        setResultConverter(dialogButton -> {
            if (passwordButtonType == dialogButton) return passwordField.getText();
            return null;
        });
    }

    public PasswordField getPasswordField() {
        return passwordField;
    }
}

class Encryptor {
    static public PDDocument encrypt(File file, String pwd) throws IOException {
        PDDocument doc = PDDocument.load(file);

        // Define the length of the encryption key.
        // Possible values are 40, 128 or 256.
        int keyLength = 128;

        AccessPermission ap = new AccessPermission();

        // disable printing, everything else is allowed
        ap.setCanFillInForm(false);
        ap.setCanModify(false);
        ap.setCanExtractContent(false);
        ap.setCanAssembleDocument(false);
        ap.setCanModifyAnnotations(false);
        ap.setReadOnly();

        // Owner password (to open the file with all permissions) is "12345"
        // User password (to open the file but with restricted permissions, is empty here)
        StandardProtectionPolicy spp = new StandardProtectionPolicy(pwd, "", ap);
        spp.setEncryptionKeyLength(keyLength);
        spp.setPermissions(ap);
        doc.protect(spp);

        return doc;
    }
}