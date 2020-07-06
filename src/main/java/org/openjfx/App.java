package org.openjfx;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.*;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import org.apache.pdfbox.pdmodel.PDDocument;

import org.apache.commons.io.FilenameUtils;

import java.awt.*;
import java.io.*;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
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

    List<File> files = new ArrayList<>();

    private void initUI(Stage stage) {
        final FileChooser fileChooser = new FileChooser();
        setupFileChooser(fileChooser);

        // Init Box
        final VBox vb = new VBox();
        vb.setSpacing(10);
        vb.setPadding(new Insets(15,20, 10,10));

        final HBox hb1 = new HBox();
        final HBox hb2 = new HBox();
        hb1.setSpacing(10);
        hb2.setSpacing(10);


        // Path field area
        final TextField paths = new TextField("");
        paths.setMinWidth(120);
        hb1.getChildren().add(paths);

        // File chooser button
        final Button fileButton = new Button("Select pdf");
        fileButton.setOnAction(actionEvent -> {
            paths.clear();
            files = fileChooser.showOpenMultipleDialog(stage);
            printPaths(paths, files);
        });
        hb1.getChildren().add(fileButton);

        // Encrypt Button
        Button encryptButton = new Button("Encrypt");
        encryptButton.setOnAction(actionEvent -> {
            // If no file is selected
            if (paths.getText().isEmpty()) {
                Alert alert = new  Alert(Alert.AlertType.ERROR);
                alert.setTitle("Erreur");
                alert.setHeaderText("Une erreur est survenue");
                alert.setContentText("Aucun fichier n'a été spécifié");

                alert.showAndWait();
            }
            else {
                // Generate a password with passay
                String password = PwdGenerator.generatePassayPassword();

                // Encrypt all file in the list
                FileSystem fs = FileSystems.getDefault();
                Path userPath = fs.getPath(System.getProperty("user.home"), "Documents/PdfEncryptor");
                PDDocument pdf;
                int encrypted = 0;
                for (File file : files) {
                    try {
                        pdf = Encryptor.encrypt(file, password);

                        new File(userPath.toUri()).mkdir();
                        String filename = FilenameUtils.removeExtension(file.getName()) + "_encrypted.pdf";
                        pdf.save(userPath.toString() + "/" + filename);
                        pdf.close();

                        encrypted++;
                    } catch (FileNotFoundException e) {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Erreur");
                        alert.setHeaderText("Une erreur est survenue");
                        alert.setContentText("Impossible de trouver le fichier " + file.getName());

                        alert.showAndWait();
                    } catch (Exception e) {
                        // Show Error Alert + stacktrace if there is an unknown error
                        Alert alert = BacktraceDialog(e);
                        alert.setContentText("Impossible d'encrypter le fichier " + file.getName());

                        alert.showAndWait();
                    }
                }
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Information Dialog");
                alert.setHeaderText(null);
                alert.setContentText(encrypted + " sur " + files.size() + " fichiers ont pu être encrypté" +
                        "\nLe mot de passe utilisé est : " + password +
                        "\n ATTENTION : Ce mot de passe ne s'affichera plus n'oubliez pas de le noter si besoin");

                alert.showAndWait();

                // open the output folder
                Desktop desktop = Desktop.getDesktop();
                if(desktop.isSupported(Desktop.Action.BROWSE) && encrypted > 0)
                {
                    try {
                        desktop.browse(userPath.toUri());
                    } catch (Exception e) {
                        alert = BacktraceDialog(e);
                        alert.showAndWait();
                    }
                }

                paths.clear();
            }
        });

        vb.getChildren().add(hb1);
        hb2.getChildren().add(encryptButton);

        //Decrypt Button
        Button decryptButton = new Button("Decrypt");
        decryptButton.setOnAction(actionEvent -> {
            // TODO
        });

        hb2.getChildren().add(decryptButton);
        vb.getChildren().add(hb2);

        // Drag'n'drop
        vb.setOnDragOver(event -> {
            event.acceptTransferModes(TransferMode.ANY);
            System.out.println("Drag'n'drop detected");
            event.consume();
        });

        vb.setOnDragDropped(event -> {
            if (!event.getDragboard().hasFiles())
                return;
            List<File> draggedFiles = event.getDragboard().getFiles();
            if (files.size() > 0)
                files.clear();
            for (File file : draggedFiles) {
                if (!FilenameUtils.getExtension(file.getName()).equals("pdf"))
                    draggedFiles.remove(file);
                printPaths(paths, draggedFiles);
            }
            System.out.println("Got " + files.size() + " files");
            event.consume();
        });

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

    private void printPaths(TextField paths, List<File> files) {
        if (files == null || files.isEmpty())
            return;
        for(File file : files) {
            paths.appendText(file.getAbsolutePath() + ";");
        }
    }

    public static void main(String[] args) {
        launch();
    }

    private Alert BacktraceDialog(Exception e) {

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
}

