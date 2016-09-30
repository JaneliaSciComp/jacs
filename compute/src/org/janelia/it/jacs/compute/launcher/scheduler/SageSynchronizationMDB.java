
package org.janelia.it.jacs.compute.launcher.scheduler;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.shared.utils.SystemCall;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Scanner;

//@MessageDriven(activationConfig = {
//        // crontTrigger starts with seconds.  Below should run every 30 minutes, every day
//        @ActivationConfigProperty(propertyName = "cronTrigger", propertyValue = "0 0/30 * * * ?")
//})
//@ResourceAdapter("quartz-ra.rar")
/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: May 29, 2009
 * Time: 4:30:39 PM
 */
public class SageSynchronizationMDB implements Job {
    private static final Logger log = Logger.getLogger(SageSynchronizationMDB.class);
    private static boolean runAlready = false;

    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        if (!SystemConfigurationProperties.getBoolean("ComputeServer.UseSageSync")) {
            return;
        }
        try {
            log.info("\n\n\nStarting to synchronize the data with the Sage datasource...");
            String reportsDir = SystemConfigurationProperties.getString("Reports.Dir");
            String sageImageDumpFilePath = reportsDir+File.separator+"sageImageDump.txt";
            String sageImagePropertyDumpFilePath = reportsDir+File.separator+"sageImagePropertyDump.txt";
            String sageCVDumpFilePath = reportsDir+File.separator+"sageCVDump.txt";

            // Clear the old files
//            ensureFileCleared(sageImageDumpFilePath);
//            ensureFileCleared(sageImagePropertyDumpFilePath);
//            ensureFileCleared(sageCVDumpFilePath);
            // Build the new dump files
//            ensureFileCreated("\"select * from image_vw\" > ", sageImageDumpFilePath);
//            ensureFileCreated("\"select * from image_property_vw\" > ", sageImagePropertyDumpFilePath);
//            ensureFileCreated("\"select * from cv_term_vw\" > ", sageCVDumpFilePath);
            // Port in the data
            // Grab the raw image data first
            buildAttributesForCvTerms(sageCVDumpFilePath);
//            buildImageEntities(sageImageDumpFilePath);
//            buildImageProperties(sageImagePropertyDumpFilePath);
        }
        catch (Exception e) {
            log.error("There was a problem synchronizing data from Sage.", e);
        }
        log.info("Sage data synchronization is complete.");
        runAlready = true;
    }

    private void ensureFileCleared(String dumpFile) throws ServiceException {
        // Ensure all old data is wiped
        File tmpSageDumpFile = new File(dumpFile);
        if (tmpSageDumpFile.exists()) {
            boolean deleteSuccessful = new File(dumpFile).delete();
            if (!deleteSuccessful) {
                throw new ServiceException("Unable to delete the SageSync file at "+tmpSageDumpFile.getAbsolutePath());
            }
        }
    }

    private void ensureFileCreated(String customSQL, String outputFilePath) throws IOException, InterruptedException, ServiceException {
        SystemCall call = new SystemCall(log);
        // Create the dumps of the image data
        String dbPrefix = "mysql -u sageRead -psageRead -h sage-db sage -e ";
        int success = call.emulateCommandLine(dbPrefix + customSQL + outputFilePath, true);
        if (success!=0) {
            log.error("There was a problem getting data for "+ outputFilePath);
            throw new ServiceException("Unable to grab the Sage data and place into "+outputFilePath);
        }
    }

    private void buildAttributesForCvTerms(String sageCVDumpFilePath) {
        try {
            Scanner scanner = new Scanner(new File(sageCVDumpFilePath));
            String[] headerItems = scanner.nextLine().split("\t");
            for (String headerItem : headerItems) {
                System.out.println("Header Item Found: "+headerItem);
            }
        }
        catch (FileNotFoundException e) {
            log.error("There was a problem reading from file "+sageCVDumpFilePath+" and creating data.");
        }
    }

}
