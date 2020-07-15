package org.openjfx;

import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

import static org.openjfx.App.BacktraceDialog;

public class DecryptService extends Service<Integer> {
    private final List<File> files;
    private final String decryptPassword;

    public DecryptService(List<File> files, String decryptPassword) {
        this.files = files;
        this.decryptPassword = decryptPassword;
    }

    @Override
    protected Task<Integer> createTask() {
        return new Task<>() {
            @Override
            protected Integer call() {
                PDDocument pdDocument;
                int decrypted = 0;
                for (File file : files) {
                    try {
                        pdDocument = Encryptor.decrypt(file, decryptPassword);
                        pdDocument.save(file);
                        pdDocument.close();

                        decrypted++;
                    } catch (FileNotFoundException e) {
                        Platform.runLater(() -> {
                            Alert alert = new Alert(Alert.AlertType.ERROR);
                            alert.setTitle("Erreur");
                            alert.setHeaderText("Une erreur est survenue");
                            alert.setContentText("Impossible de trouver le fichier :" + file.getName());

                            alert.showAndWait();
                        });
                    } catch(InvalidPasswordException e) {
                        Platform.runLater(() -> {
                            // Show Error Alert + stacktrace if there is an unknown error
                            Alert alert = new Alert(Alert.AlertType.ERROR);
                            alert.setTitle("Erreur");
                            alert.setHeaderText("Une erreur est survenue");
                            alert.setContentText("Impossible de décrypter le fichier " + file.getName() +
                                    "\nLe mot de passe est invalide");

                            alert.showAndWait();
                        });
                    } catch (Exception e) {
                        Platform.runLater(() -> {
                            // Show Error Alert + stacktrace if there is an unknown error
                            Alert alert = BacktraceDialog(e);
                            alert.setContentText("Impossible de décrypter le fichier " + file.getName());

                            alert.showAndWait();
                        });
                    }
                }
                return decrypted;
            }
        };
    }
}
