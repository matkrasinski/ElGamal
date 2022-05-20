package pl.crypto;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import org.apache.commons.io.FilenameUtils;
import pl.crypto.elgamal.ElGamalAlgorithm;
import pl.crypto.keys.Keys;
import pl.crypto.services.FileManager;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

public class Controller {
    private final Keys keys = new Keys();
    private String keyLen;
    private byte[] bytes;
    private String extension;
    private File encrypted;
    private byte[] bytesCoded;
    private final ElGamalAlgorithm algorithm = new ElGamalAlgorithm();

    @FXML
    private TextField keyP;

    @FXML
    private TextField keyLength;

    @FXML
    private TextField keyA;

    @FXML
    private TextField keyG;

    @FXML
    private TextField keyH;

    @FXML
    private TextArea publicText;

    @FXML
    private TextArea cipherArea;


    public void codeText() {
        byte[] byteFromText = publicText.getText().getBytes(StandardCharsets.UTF_8);
        bytesCoded = algorithm.encrypt(byteFromText, keys);
        BigInteger coded = new BigInteger(bytesCoded);
        cipherArea.setWrapText(true);
        cipherArea.setText(coded.toString());
        publicText.clear();
    }

    public void decodeText() {
        byte[] decryptedToArray = algorithm.decrypt(bytesCoded, keys);
        publicText.setText(new String(decryptedToArray));
    }

    public void generate() {
        keyLen = keyLength.getText();
        keys.generateKeys(Integer.parseInt(keyLen));
        keyG.setText(keys.getG().toString());
        keyH.setText(keys.getH().toString());
        keyA.setText(keys.getA().toString());
        keyP.setText(keys.getP().toString());
    }

    public void codeFile() throws IOException {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("PDF", "*.pdf", "*.txt", "*.png", "*.jpg"));
        File toEncrypt = fileChooser.showOpenDialog(null);
        //pobranie rozwiniecia pliku
        extension = FilenameUtils.getExtension(toEncrypt.getName());

        byte[] message = new byte[0];
        try {
            message = FileManager.loadFile(toEncrypt);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        bytes = algorithm.encrypt(message, keys);

        encrypted = new File("encrypted");
        FileManager.saveToFile(bytes, encrypted);
        System.out.println("Zaszyfrowano do pliku");
    }

    public void decodeFile() throws IOException {
        byte[] decryptedToArray = algorithm.decrypt(bytes, keys);
        File toSave = new File("result." + extension);
        FileManager.saveToFile(decryptedToArray, toSave);
        System.out.println("Zapisano do pliku");
    }
}