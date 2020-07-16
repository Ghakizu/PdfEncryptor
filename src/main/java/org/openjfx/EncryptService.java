package org.openjfx;

import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import org.apache.commons.io.FilenameUtils;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.List;

import static org.openjfx.App.BacktraceDialog;

/*
    Service to encrypt a file that call the fonction encrypt from the Encryptor class.
 */
public class EncryptService extends Service<Integer> {
    private final List<File> files;
    private final String password;

    public EncryptService(List<File> files, String password) {
        this.files = files;
        this.password = password;
    }

    @Override
    protected Task<Integer> createTask() {
        return new Task<>() {
            @Override
            protected Integer call() {
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
                        Platform.runLater(() -> {
                            Alert alert = new Alert(Alert.AlertType.ERROR);
                            alert.setTitle("Erreur");
                            alert.setHeaderText("Une erreur est survenue");
                            alert.setContentText("Impossible de trouver le fichier " + file.getName());

                            alert.showAndWait();
                        });
                    } catch (Exception e) {
                        // Show Error Alert + stacktrace if there is an unknown error
                        Platform.runLater(() -> {
                            Alert alert = BacktraceDialog(e);
                            alert.setContentText("Impossible d'encrypter le fichier " + file.getName());

                            alert.showAndWait();
                        });
                    }
                }
                return encrypted;
            }
        };
    }
}
