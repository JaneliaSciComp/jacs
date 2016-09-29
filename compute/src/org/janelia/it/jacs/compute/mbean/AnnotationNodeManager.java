
package org.janelia.it.jacs.compute.mbean;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.shared.utils.SystemCall;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

import javax.ejb.Remote;
import javax.ejb.Singleton;
import javax.ejb.Startup;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Jan 14, 2009
 * Time: 11:38:49 AM
 */
@Singleton
@Startup
@Remote(AnnotationManagerMBean.class)
public class AnnotationNodeManager extends AbstractComponentMBean implements AnnotationNodeManagerMBean {
    private static final Logger logger = Logger.getLogger(AnnotationNodeManager.class);

    public AnnotationNodeManager() {
        super("jacs");
    }

    public void resetNewNcbiGenomes(String ncbiGenomeLoadFile, boolean isTest) {
        try {
            Scanner scanner = new Scanner(new File(ncbiGenomeLoadFile));
            while (scanner.hasNextLine()) {
                try {
                    String tmpNext = scanner.nextLine();
                    if (null == tmpNext || tmpNext.indexOf("\t") < 0) {
                        break;
                    }
                    String[] pieces = tmpNext.trim().split("\t");
                    String tmpGenomeDir = pieces[0].trim().toUpperCase();
                    // Nuke the directories
                    SystemCall call = new SystemCall(logger);
                    String tmpRemoveString = "rm -rf " + SystemConfigurationProperties.getString("ProkAnnotation.BaseDir") +
                            File.separator + tmpGenomeDir + File.separator + "*";
                    System.out.println("Calling " + tmpRemoveString);
                    if (!isTest) {
                        call.emulateCommandLine(tmpRemoveString, true);
                    }

                    // Need to wipe the dbs here
                }
                catch (Exception e) {
                    System.out.println("Error uploading data for new NCBI genomes.");
                    e.printStackTrace();
                }
            }
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }


    public void prokDirTest(){
        String rootDir = "/usr/local/annotation/GSTEC";
        ArrayList<String> failedDirs = new ArrayList<String>();
        failedDirs.add("23");
        failedDirs.add("13");
        failedDirs.add("09");
        failedDirs.add("08");
        failedDirs.add("07");
        failedDirs.add("15");
        failedDirs.add("11");
        failedDirs.add("14");
        failedDirs.add("05");
        String cog = "COG";

        try {
            for (String failedDir : failedDirs) {
                File tmpDir = new File(rootDir+failedDir+File.separator+cog);
                if (tmpDir.isDirectory()){
                    logger.debug("Testing "+tmpDir.getAbsolutePath());
                    File[] missedSearches = tmpDir.listFiles(new FilenameFilter(){
                        @Override
                        public boolean accept(File dir, String name) {
                            return name.startsWith("ORF") && name.indexOf(".")<=0;
                        }
                    });
                    for (File missedSearch : missedSearches) {
                        logger.debug("Missed "+missedSearch.getAbsolutePath());
                        SystemCall call = new SystemCall(logger);
                        call.emulateCommandLine("cat "+missedSearch.getAbsolutePath(), true);
                    }
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
