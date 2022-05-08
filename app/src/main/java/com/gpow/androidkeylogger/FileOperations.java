package com.gpow.androidkeylogger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileOperations {

    //  Create a file with text locally
    public static void writeTextToFile(final String filename, final String text) {
        File file = new File(filename);
        try {
            FileOutputStream stream = new FileOutputStream(file);
            stream.write(text.getBytes());
            stream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
