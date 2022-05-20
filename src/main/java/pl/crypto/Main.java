package pl.crypto;

import pl.crypto.elgamal.ElGamalAlgorithm;
import pl.crypto.keys.Keys;
import pl.crypto.services.FileManager;

import java.io.File;


public class Main {

    public static void main(String[] args) throws Exception {
        Keys keys = new Keys();
        keys.generateKeys(512);

        ElGamalAlgorithm algorithm = new ElGamalAlgorithm();

        File file = new File("plik.txt");
        byte[] message = FileManager.loadFile(file);

        byte[] bytes = algorithm.encrypt(message, keys);

        FileManager.saveToFile(bytes, new File("encrypted"));


        byte[] decryptedToArray = algorithm.decrypt(bytes, keys);

        FileManager.saveToFile(decryptedToArray, new File("result.txt"));

    }
}
