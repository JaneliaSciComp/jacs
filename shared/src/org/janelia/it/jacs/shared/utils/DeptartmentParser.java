package org.janelia.it.jacs.shared.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.TreeMap;

/**
 * Created with IntelliJ IDEA.
 * User: saffordt
 * Date: 4/26/13
 * Time: 2:38 PM
 */
public class DeptartmentParser {
    public static void main(String[] args) {
        File tmpFile = new File("/Users/saffordt/Desktop/UpdatedDepartmentInformation.txt");
        try {
            Scanner scanner = new Scanner(tmpFile);
            TreeMap<String, String> codeToDeptMap = new TreeMap<String, String>();
            while (scanner.hasNextLine()) {
                String[] pieces = scanner.nextLine().split("\t");
                if (null!=pieces && pieces.length==4) {
                    codeToDeptMap.put(pieces[2], pieces[3]);
                }
            }
            System.out.println("Here is the unique list of codes to department:");
            for (String keyCode : codeToDeptMap.keySet()) {
                System.out.println(keyCode+"\t"+codeToDeptMap.get(keyCode));
            }
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
