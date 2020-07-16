package org.openjfx;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;

import java.io.File;
import java.io.IOException;

public class Encryptor {
    /*
        Encrypt a PDDocument
     */
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
    /*
        Decrypt a PDDocument
     */
    static public PDDocument decrypt(File file, String pwd) throws IOException{
        PDDocument doc;
        doc = PDDocument.load(file, pwd);
        doc.setAllSecurityToBeRemoved(true);
        return doc;
    }
}
