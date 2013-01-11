package org.janelia.it.jacs.shared.utils;

import java.io.File;
import java.io.FileFilter;
import java.util.Scanner;
import java.util.TreeSet;

/**
 * Created by IntelliJ IDEA.
 * Used this for 2 years in a row.  DO NOT remove.  :-)
 * User: saffordt
 * Date: 1/5/12
 * Time: 8:14 PM
 */
public class SubversionParser {

    private static long LINE_COUNT = 0;
    private static long FILE_COUNT = 0;
    private static TreeSet<String> projectList = new TreeSet<String>();
    private static TreeSet<String> extensionList = new TreeSet<String>();
    private static TreeSet<String> unknownExtensionList = new TreeSet<String>();

//    public static void main(String[] args) {
//        try {
//            TreeSet<String> fileSet = new TreeSet<String>();
//            Scanner scanner = new Scanner(new File("/Users/saffordt/JacsSVN.txt"));
//            while(scanner.hasNextLine()){
//                String nextLine = scanner.nextLine();
//                if (nextLine.startsWith("/jacs/trunk/")) {
//                    fileSet.add(nextLine);
//                }
//            }
//            System.out.println("There were "+fileSet.size()+" files changed.");
//            System.out.println("The changed files are:");
//            for (String s : fileSet) {
//                System.out.println(s);
//            }
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }
//    }

    public static void main(String[] args) {
        try {
            extensionList.add(".java");
            extensionList.add(".jsp");
            extensionList.add(".html");
            extensionList.add(".htm");
            extensionList.add(".xhtml");
            extensionList.add(".shtml");
            extensionList.add(".m");
            extensionList.add(".py");
            extensionList.add(".pl");
            extensionList.add(".php");
            extensionList.add(".css");
            //extensionList.add(".sql");
            extensionList.add(".h");
            extensionList.add(".c");
            extensionList.add(".cpp");
            extensionList.add(".process");
            extensionList.add(".groovy");


            File rootDir = new File("/groups/scicomp/jacsData/svnTest");
            File[] projectDirs = rootDir.listFiles(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    return file.isDirectory();
                }
            });
            for (File projectDir : projectDirs) {
                projectList.add(projectDir.getName());
                LINE_COUNT += findAllLOC(projectDir);
            }
            System.out.println("\n\nNumber of project directories parsed = "+projectList.size());
            System.out.println("Total number of applicable files found = "+FILE_COUNT);
            System.out.println("Total number of Lines of Code found = "+LINE_COUNT);
            System.out.println("Projects searched: ");
            for (String s : projectList) {
                System.out.println(s);
            }
            System.out.println("Searched extensions: ");
            for (String s : extensionList) {
                System.out.println(s);
            }
            System.out.println("\nExtensions ignored: ");
            for (String s : unknownExtensionList) {
                System.out.println(s);
            }
            System.out.println("Filtered out lines beginning with #, //, /**, and empty lines");
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static int findAllLOC(File projectDir) {
        File[] tmpDirs = projectDir.listFiles();
        int localLOC = 0;
        for (File tmpFile : tmpDirs) {
            if (tmpFile.isDirectory()) {
                localLOC += findAllLOC(tmpFile);
            }
            else {
                String fileExtension = (tmpFile.getName().contains("."))?(tmpFile.getName().substring(tmpFile.getName().lastIndexOf("."))):"";
                if (extensionList.contains(fileExtension.toLowerCase())) {
                    Scanner scanner=null;
                    int tmpCounter = 0;
                    try {
                        FILE_COUNT++;
                        scanner = new Scanner(tmpFile);
                        while (scanner.hasNextLine()){
                            String tmpLine = scanner.nextLine().trim();
                            if (!tmpLine.startsWith("#") && !tmpLine.startsWith("//") && !tmpLine.startsWith("/**") && !tmpLine.equals("")) {
                                tmpCounter++;
                            }
                        }
//                        System.out.println("Adding "+tmpCounter+" from file "+tmpFile.getName());
                        localLOC += tmpCounter;
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                    finally {
                        if (null!=scanner) { scanner.close(); }
                    }
                }
                else {
                    if (!unknownExtensionList.contains(fileExtension.toLowerCase())) {
                        unknownExtensionList.add(fileExtension.toLowerCase());
                    }
                }
            }
        }
        return localLOC;
    }

//    public static void main(String[] args) {
//        try {
//            Scanner scanner = new Scanner(new File("/groups/scicomp/jacsData/svnTest/svnList.txt"));
//            while (scanner.hasNextLine()) {
//                String[] pieces = scanner.nextLine().split(" ");
//                String svnTarget = "svn co https://subversion.int.janelia.org/ScientificComputing/Projects/"+pieces[0]+
//                        " /groups/scicomp/jacsData/svnTest/"+pieces[1];
//                Runtime.getRuntime().exec(svnTarget);
//            }
//        }
//        catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }
//        catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
}
