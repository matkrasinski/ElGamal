package pl.crypto.services;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

public class FileManager {

    public static byte[] loadFile(File file) throws IOException {
        return FileUtils.readFileToByteArray(file);
    }

    public static void saveToFile(byte[] bytes, File file) throws IOException {
        FileUtils.writeByteArrayToFile(file, bytes);
    }

}
