package org.janelia.it.jacs.compute.service.tools;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Scanner;

/**
 * Created with IntelliJ IDEA.
 * User: saffordt
 * Date: 3/18/16
 * Time: 2:09 PM
 */
public class ExportCleanup {
    // This method was used to create symlinks for all redundant sample references.  It enforces a pure set of samples.
    // We run this step first for export of bulk files.
//    public static void main(String[] args) {
//        try {
//            FileWriter writer = new FileWriter(new File("/groups/jacs/jacsDev/saffordt/symlink__2240583447523360913.sh"));
//            String basename = "/nrs/jacs/jacsData/saffordt/download/drive/20160219_VND_for_Myers_group/";
//            Scanner scanner = new Scanner(new File("/groups/jacs/jacsDev/saffordt/export_2240583447523360913.sh.nrs.txt"));
//            HashMap<String,String> sampleMap = new HashMap<>();
//            int totalFileCount = 0;
//            int skipCount=0;
//            while (scanner.hasNextLine()) {
//                String tmpLine = scanner.nextLine();
//                if (tmpLine.contains("jfs")) {
//                    totalFileCount++;
//                    String tmpFileKey = tmpLine.substring(tmpLine.lastIndexOf(basename)+basename.length()).trim();
//                    String tmpSample = tmpFileKey.substring(tmpFileKey.indexOf("/")+1);
//                    if (sampleMap.containsKey(tmpSample)) {
//                        int backticks = tmpFileKey.split("/").length-1;
//                        String formattedbackticks = "";
//                        for (int i = 0; i < backticks; i++) {
//                            formattedbackticks+="../";
//                        }
//                        if (!sampleMap.get(tmpSample).contains(tmpFileKey)) {
//                            String tmpSymlink = "ln -sf "+formattedbackticks+sampleMap.get(tmpSample)+" "+basename+tmpFileKey;
//                            System.out.println("Already have sample "+tmpSample+". Making a symlink ("+tmpSymlink+")");
//                            // Now write the symlink to a file for processing...
//                            writer.write(tmpSymlink+"\n");
//                        }
//                        else {
//                            System.out.println("Cannot have a link reference itself: "+sampleMap.get(tmpSample)+":"+tmpFileKey);
//                            skipCount++;
//                        }
//                    }
//                    else {
//                        sampleMap.put(tmpSample, tmpFileKey);
//                    }
//                }
//            }
//            writer.close();
//            scanner.close();
//            System.out.println("\nThere are "+totalFileCount+" files.");
//            System.out.println("There are "+sampleMap.keySet().size()+" unique files.");
//            System.out.println("Prevented "+skipCount+" references to themselves.");
//        }
//        catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }
//        catch (IOException e) {
//            e.printStackTrace();
//        }
//
//    }


    // This method is used to validate that all necessary files exist.  It sometimes has false positives with symlinks outside the folder
    // We run this step second to ensure all files we wanted to be in the export collection actually made it.
    // Ideally, this method should really be the first step across all folders.  Someone must grep the target files from the original bulk export script.
//    public static void main(String[] args) {
//        try {
//            Scanner scanner = new Scanner(new File("/nrs/jacs/jacsData/saffordt/validateFiles.txt.WEDd1"));
//            PrintWriter writer = new PrintWriter(new File("/nrs/jacs/jacsData/saffordt/validateFiles.txt.WEDd1.missing"));
//            int success=0,failure=0;
//            while (scanner.hasNextLine()){
//                String tmpLine = scanner.nextLine();
//                String tmpfilename = tmpLine.substring(tmpLine.indexOf("-file")+5).trim();
//                File testFile = new File(tmpfilename);
//                if (testFile.exists() && testFile.length()>0) {
//                    success++;
//                }
//                else {
//                    System.out.println("Missing file: "+tmpfilename);
//                    writer.append(tmpLine).append("\n");
//                    failure++;
//                }
//            }
//            writer.close();
//            writer.flush();
//            System.out.println("Success: "+success);
//            System.out.println("Failure: "+failure);
//        }
//        catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }
//    }


    // This method copies the "missed" files into the external drives.
    // This is the third and final export step.  Before this someone manually copies the "missing" file above to the "resync" input here.
    public static void main(String[] args) {
        try {
            Scanner scanner = new Scanner(new File("/nrs/jacs/jacsData/saffordt/resyncFiles.AOTUv3_Part2"));
            PrintWriter writer = new PrintWriter(new File("/nrs/jacs/jacsData/saffordt/resyncFiles.AOTUv3_Part2.sh"));
            while (scanner.hasNextLine()){
                String tmpLine = scanner.nextLine();
                String tmpfilename = tmpLine.substring(tmpLine.indexOf("-file")+5).trim();
                String tmpdestination = tmpfilename.replace("/nrs/jacs/jacsData/saffordt/download/drive/20160219_VND_for_Myers_group/AOTUv3_Prt2/",
                                                            "/Volumes/Lee\\ Lab\\ Drive\\ 3/20160219_VND_for_Myers_group/AOTUv3_Part2/");
                String tmpCmd = "rsync -av "+tmpfilename+" "+tmpdestination;
                writer.append(tmpCmd).append("\n");
            }
            writer.close();
            writer.flush();
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

}
