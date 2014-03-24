package org.janelia.it.jacs.shared.utils;

import java.io.File;
import java.io.FileFilter;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 3/22/14
 * Time: 3:36 PM
 */
public class NobackupMigration {
    public static void main(String[] args) {
        File sourceFilestoreDir = new File("/groups/scicomp/jacsData/filestore");
        File nobackupFilestoreDir = new File("/nobackup/jacs/jacsData/filestore");
        File[] userDirs = sourceFilestoreDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isDirectory();
            }
        });
        for (File userDir : userDirs) {
            File[] userSubDirs = userDir.listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    return pathname.isDirectory();
                }
            });
            for (File userSubDir : userSubDirs) {

            }
        }
    }
}
