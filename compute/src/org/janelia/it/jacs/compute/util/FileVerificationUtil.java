package org.janelia.it.jacs.compute.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

/**
 * Created with IntelliJ IDEA.
 * User: saffordt
 * Date: 3/17/15
 * Time: 10:57 AM
 */
public class FileVerificationUtil {

    public static void main(String[] args) throws IOException {
        new FileVerificationUtil().runFileVerificationUtil();
    }

    public void runFileVerificationUtil() throws IOException {
        FileWriter writer = null;
        Scanner scanner = null;
        int errorCounter=0, mainCounter=0;
        try {
            writer = new FileWriter("/Users/saffordt/Desktop/FilePathExpose-Errors.txt");
            scanner = new Scanner(new File("/Users/saffordt/Desktop/FilePathExpose.txt"));
            while (scanner.hasNextLine()) {
                mainCounter++;
                if (mainCounter%10000==0) {
                    System.out.println("Scanning "+mainCounter+"...");
                }
                File tmpFile = new File(scanner.nextLine());
                if (tmpFile.exists()) {continue;}
                if (tmpFile.getName().toLowerCase().endsWith(".lsm")) {
                    File tmpbz2 = new File(tmpFile.getAbsolutePath()+".bz2");
                    if (tmpbz2.exists()) {
                        writer.append("File not found but compressed version is - ").append(tmpbz2.getAbsolutePath()).append("\n");
                    }
                }
                else {
                    writer.append("Cannot find file - ").append(tmpFile.getAbsolutePath()).append("\n");
                }
                errorCounter++;
            }
            System.out.println("Found "+errorCounter+" errors with files.");
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            if (null!=writer) {writer.close();}
            if (null!=scanner) {scanner.close();}
        }
    }
}
