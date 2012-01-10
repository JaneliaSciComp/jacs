package org.janelia.it.jacs.shared.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.TreeSet;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 1/5/12
 * Time: 8:14 PM
 */
public class SubversionParser {
    public static void main(String[] args) {
        try {
            TreeSet<String> fileSet = new TreeSet<String>();
            Scanner scanner = new Scanner(new File("/Users/saffordt/JacsSVN.txt"));
            while(scanner.hasNextLine()){
                String nextLine = scanner.nextLine();
                if (nextLine.startsWith("/jacs/trunk/")) {
                    fileSet.add(nextLine);
                }
            }
            System.out.println("There were "+fileSet.size()+" files changed.");
            System.out.println("The changed files are:");
            for (String s : fileSet) {
                System.out.println(s);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
