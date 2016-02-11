package org.janelia.it.jacs.shared.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Scanner;
import java.util.TreeMap;

/**
 * Created with IntelliJ IDEA.
 * User: saffordt
 * Date: 8/11/14
 * Time: 1:18 PM
 */
public class PurgeCleanup {

    private static long missedFileCount=0;

    public static void main(String[] args) {
        PurgeCleanup.cleanFiles();
    }

    private static void handleFile(File tmpFile, AccountInfo tmpAccount) throws IOException {
        if (tmpFile.isDirectory()) {
            File[] childFiles = tmpFile.listFiles();
            if (null!=childFiles) {
                for (File childFile : childFiles) {
                    handleFile(childFile, tmpAccount);
                }
            }
        }
        else {
            if (tmpFile.exists()) {
                tmpAccount.setFileCount(tmpAccount.getFileCount() + 1);
                tmpAccount.setCumulativeFileSize(tmpAccount.getCumulativeFileSize()+tmpFile.length());
//                boolean deleted = tmpFile.delete();
//                if (!deleted) {
//                    System.out.println("Unable to delete file: "+tmpFile.getAbsolutePath());
//                }
            }
            else {
//                System.out.println("Cannot find file: " + tmpFile.getAbsolutePath());
                missedFileCount++;
            }
        }
    }

    public static void cleanFiles(){
        TreeMap<String,AccountInfo> userMap = new TreeMap<>();
        try {
            Scanner scanner = new Scanner(new File("/groups/jacs/jacsShare/saffordTest/PurgeMisses08112014.txt"));
            while (scanner.hasNextLine()) {
                String tmpLine = scanner.nextLine();
                String tmpFilePath = tmpLine.substring(tmpLine.indexOf("/filestore/")+11).trim();
                String tmpUserName = tmpFilePath.substring(0,tmpFilePath.indexOf("/"));
                AccountInfo tmpAccount;
                tmpAccount = userMap.get(tmpUserName);
                if (null==tmpAccount) {
                    tmpAccount = new AccountInfo(tmpUserName);
                    userMap.put(tmpUserName, tmpAccount);
                }
                // Now update the stats
                // Force the path to the current filestore
                tmpFilePath = "/nrs/jacs/jacsData/filestore/"+tmpFilePath;
                File tmpFile = new File(tmpFilePath);
                try {
                    handleFile(tmpFile, tmpAccount);
                }
                catch (IOException e) {
                    // Do nothing
                }
            }
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        long totalSize = 0;
        long totalFileCount=0;
        System.out.println("Summary stats:");
        for (String tmpUser : userMap.keySet()) {
            AccountInfo tmpAccount = userMap.get(tmpUser);
            totalSize+=tmpAccount.getCumulativeFileSize();
            totalFileCount+=tmpAccount.getFileCount();
            System.out.println(tmpAccount.toString());
        }
        System.out.println("\nTotal File Size: "+totalSize/1024/1024/1024+" GB");
        System.out.println("Total file count: "+totalFileCount);
        System.out.println("Missed file count: "+missedFileCount);
    }

    public static class AccountInfo {
        private String accountName;
        private int fileCount;
        private long cumulativeFileSize;

        public AccountInfo(String accountName) {
            this.fileCount = 0;
            this.cumulativeFileSize = 0;
            this.accountName = accountName;
        }

        public int getFileCount() {
            return fileCount;
        }

        public void setFileCount(int fileCount) {
            this.fileCount = fileCount;
        }

        public long getCumulativeFileSize() {
            return cumulativeFileSize;
        }

        public void setCumulativeFileSize(long cumulativeFileSize) {
            this.cumulativeFileSize = cumulativeFileSize;
        }

//        public String getAccountName() {
//            return accountName;
//        }
//
//        public void setAccountName(String accountName) {
//            this.accountName = accountName;
//        }
//
        @Override
        public String toString() {
            return "AccountInfo{" +
                    "accountName='" + accountName + '\'' +
                    ", fileCount=" + fileCount +
                    ", cumulativeFileSize=" + cumulativeFileSize/1024/1024/1024 +
                    " GB}";
        }
    }

}
