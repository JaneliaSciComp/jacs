package org.janelia.it.jacs.shared.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Scanner;
import java.util.TreeMap;

/**
 * Created with IntelliJ IDEA.
 * User: saffordt
 * Date: 9/10/14
 * Time: 2:11 PM
 */
public class BransonScreenUtil {
    public static void main(String[] args) {
        Scanner listScanner=null,desiredScanner=null,representativeScanner=null;
        FileWriter writer=null;
        try {
            listScanner = new Scanner(new File("/Users/saffordt/Desktop/ScreenStacksToPath.txt"));
            desiredScanner = new Scanner(new File("/Users/saffordt/Desktop/Branson_GMRScreenList.csv"));
            representativeScanner = new Scanner(new File("/Users/saffordt/Desktop/RepresentativeSamples.txt"));

            writer = new FileWriter(new File("/groups/jacs/jacsShare/bransonlab/ScreenDataPBD/LineToScreenStackPath.txt"));

            ArrayList<String> representativeLines = new ArrayList<>();
            while ((representativeScanner.hasNextLine())) {
                representativeLines.add(representativeScanner.nextLine().trim().split("\t")[1].replaceAll("\"",""));
            }
            Collections.sort(representativeLines);
            System.out.println("Loaded "+representativeLines.size()+" representative lines.");

            ArrayList<String> desiredLines = new ArrayList<>();
            while ((desiredScanner.hasNextLine())) {
                desiredLines.add(desiredScanner.nextLine().trim());
            }
            Collections.sort(desiredLines);
            System.out.println("Loaded "+desiredLines.size()+" desired lines.");

            TreeMap<String, String> nameToPathMap = new TreeMap<>();
            while (listScanner.hasNextLine()) {
                String[] tmpLine = listScanner.nextLine().split("\t");
                nameToPathMap.put(tmpLine[0], tmpLine[2]);
            }
            System.out.println("Loaded "+nameToPathMap.size()+" total lines.\n");

            // Brute-force it is.  Walk the desired list and output matches
            for (String desiredLine : desiredLines) {
                System.out.println("Checking: "+ desiredLine);
                for (String fullLineName : nameToPathMap.keySet()) {
                    if (fullLineName.contains(desiredLine)) {
                        String tmpLineFilePath = nameToPathMap.get(fullLineName);
                        File tmpLineFile = new File(tmpLineFilePath);
                        File tmpDestinationFile = new File("/groups/jacs/jacsShare/bransonlab/ScreenDataPBD/"+tmpLineFile.getName());
                        System.out.println("\tFound hit "+tmpLineFilePath);
                        String representativeCheck = "";
                        for (String representativeLine : representativeLines) {
                            if (fullLineName.contains(representativeLine)) {
                                representativeCheck="\trepresentative";
                                break;
                            }
                        }
                        writer.write(desiredLine +"\t"+tmpDestinationFile.getAbsolutePath()+representativeCheck+"\n");
                        try {
                            if (!tmpDestinationFile.exists()) {
                                FileUtil.copyFile(tmpLineFilePath, tmpDestinationFile.getAbsolutePath());
                            }
                            else {
                                System.out.println("\tAlready copied "+tmpLineFilePath);
                            }
                        }
                        catch (IOException e) {
                            System.out.println("Failed getting "+tmpDestinationFile.getAbsolutePath()+". Continuing...");
                        }
                    }
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            if (null != writer) {
                try {
                    writer.close();
                    listScanner.close();
                    desiredScanner.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
