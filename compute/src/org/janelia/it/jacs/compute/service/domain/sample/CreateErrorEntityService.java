package org.janelia.it.jacs.compute.service.domain.sample;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.hibernate.exception.ExceptionUtils;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.grid.submit.SubmitJobException;
import org.janelia.it.jacs.compute.service.domain.AbstractDomainService;
import org.janelia.it.jacs.compute.service.domain.util.SampleHelperNG;
import org.janelia.it.jacs.compute.service.exceptions.MetadataException;
import org.janelia.it.jacs.compute.service.exceptions.MissingGridResultException;
import org.janelia.it.jacs.compute.util.FileUtils;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.domain.DomainConstants;
import org.janelia.it.jacs.model.domain.sample.ObjectiveSample;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.sample.SamplePipelineRun;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.shared.utils.FileUtil;
import org.janelia.it.jacs.shared.utils.domain.DataReporter;

import com.google.common.collect.Lists;

/**
 * Creates an Error based on the given exception, and adds it to the pipeline run.
 * 
 * Also tries to classify the error and take appropriate action: resubmit the job if the error is recoverable, submit
 * a JIRA ticket if there is a data problem, etc.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class CreateErrorEntityService extends AbstractDomainService {

	public static final String ANNOTATION_OWNER = "group:workstation_users";
    private static final String FROM_EMAIL = SystemConfigurationProperties.getString("System.DataErrorSource");
    private static final String TO_EMAIL = SystemConfigurationProperties.getString("System.DataErrorDestination");
	private static final String ERRORS_DIR_NAME = "Error";
    private static final String CENTRAL_DIR_PROP = "FileStore.CentralDir";
    private static final String LAB_ERROR_TERM_NAME = "Lab Error";
    private static final int MAX_CONSECUTIVE_ERRORS = 3;
    
    private FileNode resultFileNode;
    private SampleHelperNG sampleHelper;
    private Sample sample;
    private ObjectiveSample objectiveSample;
    private ClassifiedError error;
    
    public void execute() throws Exception {

        this.sampleHelper = new SampleHelperNG(computeBean, ownerKey, logger, contextLogger);
        this.sample = sampleHelper.getRequiredSample(data);
        this.objectiveSample = sampleHelper.getRequiredObjectiveSample(sample, data);
        SamplePipelineRun run = sampleHelper.getRequiredPipelineRun(sample, objectiveSample, data);
        
    	File outputDir = null;
        this.resultFileNode = (FileNode)processData.getItem("RESULT_FILE_NODE");
        String username = DomainUtils.getNameFromSubjectKey(ownerKey);

        if (resultFileNode!=null) {
            outputDir = new File(resultFileNode.getDirectoryPath());
        }
        
    	if (!outputDir.exists()) {
    		outputDir = null;
    	}
        
        if (outputDir==null) {
            File userFilestore = new File(SystemConfigurationProperties.getString(CENTRAL_DIR_PROP) + File.separator + username + File.separator);
            outputDir = new File(userFilestore, ERRORS_DIR_NAME);
            outputDir.mkdirs();
        }

    	Exception exception = (Exception)processData.getItem(IProcessData.PROCESSING_EXCEPTION);
        this.error = new ClassifiedError(exception);
        contextLogger.info("Classified error as "+error.getType()+" with description '"+error.getDescription()+"'");
        
        File errorFile = new File(outputDir, domainDao.getNewId()+".txt");
        FileUtils.writeStringToFile(errorFile, error.getStackTrace());
        contextLogger.info("Wrote error message to "+errorFile);

        sampleHelper.setPipelineRunError(run, errorFile.getAbsolutePath(), error.getDescription(), error.getType().toString());
        
    	switch (error.getType()) {
    	case RecoverableError:
    	    markSampleForReprocessing();
    	    break;
    	case LabError:
    	    reportError();
    	    break;
    	case ComputeError:
    	    logError();
    	    break;
	    default:
	        logger.warn("Cannot classify error type");
	        break;
    	}

    	// Save any changes to the sample, including errors and status changes
        sampleHelper.saveSample(sample);
    }
    
    private void markSampleForReprocessing() {
        try {

            List<SamplePipelineRun> runs = Lists.reverse(objectiveSample.getPipelineRuns());
            
            int numConsecutiveErrors = 0;
            for(SamplePipelineRun run : runs) {
                if (!run.hasError()) break;
                numConsecutiveErrors++;
            }
            
            if (numConsecutiveErrors>=MAX_CONSECUTIVE_ERRORS) {
            	contextLogger.info("Sample has experienced "+numConsecutiveErrors+" consecutive errors. Will not mark for reprocessing.");
            	return;
            }
            
            sample.setStatus(DomainConstants.VALUE_MARKED);
            contextLogger.info("Marked sample for reprocessing: "+sample.getId());
        }
        catch (Exception e) {
            logger.error("Error trying to mark sample for reprocessing",e);
        }
    }

    private void reportError() {
        try {            
            // Report the sample
            DataReporter reporter = new DataReporter(FROM_EMAIL, TO_EMAIL);
            reporter.reportData(sample, LAB_ERROR_TERM_NAME);
        }
        catch (Exception e) {
            logger.error("Error trying to report error on "+sample.getId(), e);
        }
        
    }

    private void logError() {
        // Should already be logged in the server log
    }
    
    private enum ErrorType {
        RecoverableError,
        LabError,
        ComputeError,
        UnclassifiedError
    }
    
    private class ClassifiedError {
        private Exception exception;
        private String stackTrace;
        private ErrorType type;
        private String description;
        public ClassifiedError(Exception exception) {
        	this.exception = exception;
            this.stackTrace = ExceptionUtils.getStackTrace(exception);
            classify();
        }
        private void classify() {
            List<Throwable> trace = new ArrayList<>();
            Throwable e = exception;
            while (e!=null) {
                trace.add(e);
                e = e.getCause();
            }
            // Start with the inner-most exception and find one that we know how to process
            Collections.reverse(trace);
            for(Throwable t : trace) {
            	contextLogger.info("Attempting to classify "+t.getClass().getName());
                if (t instanceof MissingGridResultException) {
                    classify((MissingGridResultException)t);
                    return;
                }
                else if (t instanceof MissingDataException) {
                    classify((MissingDataException)t);
                    return;
                }
                else if (t instanceof MetadataException) {
                    classify((MetadataException)t);
                    return;
                }
                else if (t instanceof SubmitJobException) {
                    classify((SubmitJobException)t);
                    return;
                }
                else if (t instanceof ServiceException) {
                    classify((ServiceException)t);
                    return;
                }
            }
            this.type = ErrorType.UnclassifiedError;
            this.description = "Unable to classify error";
        }

        private void classify(MissingGridResultException e) {
            if (e.getFilepath()==null) {
                classify((MissingDataException)e);
                return;
            }
            if (e.getMessage().contains("core dumped")) {
                this.type = ErrorType.ComputeError;
                this.description = "Segmentation fault";
                return;
            }
            File dir = new File(e.getFilepath());
            if (!dir.exists()) {
                this.type = ErrorType.ComputeError;
                this.description = "Missing grid result directory: "+dir.getAbsolutePath();
                return;
            }
            File sgeOutputDir = new File(dir, "sge_output");
            File sgeErrorDir = new File(dir, "sge_error");
            File[] outputFiles = FileUtil.getFiles(sgeOutputDir);
            File[] errorFiles = FileUtil.getFiles(sgeErrorDir);
            List<File> files = new ArrayList<File>();
            files.addAll(Arrays.asList(errorFiles));
            files.addAll(Arrays.asList(outputFiles));
            files.add(new File(dir, "DrmaaSubmitter.log"));
            
            for(File file : files) {
            	contextLogger.info("  Parsing file "+file.getAbsolutePath());
                try(BufferedReader br = new BufferedReader(new FileReader(file))) {
                    String line = br.readLine();
                    while (line != null) {
                        if (line.matches(".*?\\d+ Bus error.*?")) {
                            this.type = ErrorType.RecoverableError;
                            this.description = "I/O error on the grid";
                            return;
                        }
                        else if (line.matches(".*?\\d+ Killed.*?")) {
                            this.type = ErrorType.RecoverableError;
                            this.description = "Job killed on the grid";
                            return;
                        }
                        else if (line.matches(".*?Fail to allocate memory.*?")) {
                            this.type = ErrorType.RecoverableError;
                            this.description = "Failed to allocate enough memory";
                            return;
                        }
                        else if (line.matches(".*?failed on (the )?compute grid.*?")) {
                            this.type = ErrorType.RecoverableError;
                            this.description = "Job failed on the compute grid";
                            return;
                        }
                        else if (line.matches(".*?Images are different dimensions! Do nothing!.*?")) {
                            this.type = ErrorType.LabError;
                            this.description = "Inconsistent image dimensions";
                            return;
                        }
                        line = br.readLine();
                    }
                }
                catch (Exception ex) {
                    logger.error("Error trying to classify error", ex);
                }
            }

            this.type = ErrorType.ComputeError;
            this.description = "Unrecognized missing grid result error";
        }
        
        private void classify(MissingDataException e) {
            this.type = ErrorType.ComputeError;
            this.description = "Unrecognized missing data error";
        }

        private void classify(MetadataException e) {
            this.type = ErrorType.LabError;
            this.description = e.getMessage();
        }
        
        private void classify(SubmitJobException e) {
            this.type = ErrorType.ComputeError;
            this.description = "Unrecognized grid error";
        }

        private void classify(ServiceException e) {
            String m = e.getMessage();
            if (m.matches(".*?failed receiving gdi request response.*?")) {
                this.type = ErrorType.RecoverableError;
                this.description = "DRMAA connection problem";
            }
            if (m.matches(".*?java.net.SocketTimeoutException: Read timed out.*?")) {
                this.type = ErrorType.RecoverableError;
                this.description = "Problem connecting to JMS queue";
            }
            else if (m.matches(".*?failed on (the )?compute grid.*?")) {
                this.type = ErrorType.RecoverableError;
                this.description = "Job failed on the compute grid";
            }
            else {
                this.type = ErrorType.ComputeError;
                this.description = "Unrecognized compute error";
            }
        }
        
        public String getStackTrace() {
            return stackTrace;
        }
        
        public ErrorType getType() {
            return type;
        }
        
        public String getDescription() {
            return description;
        }
    }
}
