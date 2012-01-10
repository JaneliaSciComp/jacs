
package org.janelia.it.jacs.compute.service.prokAnnotation;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.ComputeBeanRemote;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.common.SubmitJobAndWaitHelper;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.prokAnnotation.*;
import org.janelia.it.jacs.shared.utils.SystemCall;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Jan 20, 2009
 * Time: 11:44:08 PM
 */
public class ProkaryoticAnnotationService implements IService {
    private Logger logger;
    private ProkaryoticAnnotationTask task;
    private ComputeBeanRemote computeBean;
    private String targetGenomeDirectory;

    public void execute(IProcessData processData) throws ServiceException {
        try {
            logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            this.task = (ProkaryoticAnnotationTask) ProcessDataHelper.getTask(processData);
            //String scriptBaseDir = SystemConfigurationProperties.getString("Perl.ModuleBase") + SystemConfigurationProperties.getString("ProkAnnotation.PerlBaseDir");
            targetGenomeDirectory = task.getParameter(ProkaryoticAnnotationTask.PARAM_targetDirectory);
            computeBean = EJBFactory.getRemoteComputeBean();
            List<String> actionSet = task.getActionList();
            // Now run the section commands
            for (String flag : ProkaryoticAnnotationTask.allSectionFlags) {
                // Only run the actions the user selected for this task
                if (actionSet.contains(flag)) {
                    runSection(flag);
                }
            }
            // Finally, brute-force an attempt to make all files in the dir readable.  Not elegant but saves many headaches
            SystemCall call = new SystemCall(logger);
            call.emulateCommandLine("chmod 777 -R "+ targetGenomeDirectory, true);
        }
        catch (Exception e) {
            try {
                computeBean.addEventToTask(task.getObjectId(), new Event("ERROR running the Prokaryotic Pipeline:" + e.getMessage(), new Date(), Event.ERROR_EVENT));
            }
            catch (Exception e1) {
                System.err.println("Error trying to log the error message.");
            }
            throw new ServiceException("Error running the ProkAnnotation ProkAnnotationService:" + e.getMessage(), e);
        }
    }

    private void runSection(String sectionStep) throws Exception {
        // The loader is run "manually", before this
        if (ProkaryoticAnnotationTask.GENOMELOADER_RUN.equalsIgnoreCase(sectionStep)) {
            return;
        }
        // todo This can probably be broken out into another, generic, service
        if (ProkaryoticAnnotationTask.CUSTOM_COMMANDS.equalsIgnoreCase(sectionStep)) {
            logger.debug("\n\n" + sectionStep + " executing...\n\n");
            computeBean.addEventToTask(task.getObjectId(), new Event("Running Step:" + sectionStep, new Date(), sectionStep));
            SystemCall call = new SystemCall(logger);
            String tmpCommand = task.getParameter(ProkaryoticAnnotationTask.PARAM_customCommand);
            String perlPath = SystemConfigurationProperties.getString("Perl.Path");
            String basePath= SystemConfigurationProperties.getString("Executables.ModuleBase")+SystemConfigurationProperties.getString("ProkAnnotation.PerlBaseDir");
            String fullCmd=perlPath+" "+basePath+tmpCommand;
            fullCmd="export PATH=$PATH:"+basePath+";export PERL5LIB=$PERL5LIB:"+basePath+";"+fullCmd;

            int exitValue = call.emulateCommandLine(fullCmd, true, null, new File(targetGenomeDirectory));
            if (0 != exitValue) {
                logger.error("WARNING: Script exited with value " + exitValue);
                throw new ServiceException("Error running " + sectionStep + ". Exit value not 0");
            }
            else {
                computeBean.addEventToTask(task.getObjectId(), new Event("Completed Step:" + sectionStep, new Date(), sectionStep));
                logger.debug("\n\n" + sectionStep + " executed successfully.\n\n");
            }
            return;
        }

        logger.debug("\n\n" + sectionStep + " executing...\n\n");
        computeBean.addEventToTask(task.getObjectId(), new Event("Running Step:" + sectionStep, new Date(), sectionStep));
        List<SectionStep> sectionTasks = new ArrayList<SectionStep>();
        // Create the next task and set task-specific values
        if (ProkaryoticAnnotationTask.LOAD_CONTIGS.equals(sectionStep)) {
            String loadType = task.getParameter(ProkaryoticAnnotationTask.LOAD_CONTIGS);
            String contigFilePath = task.getParameter(ProkaryoticAnnotationTask.PARAM_contigFilePath);
            if (null != loadType && ProkaryoticAnnotationTask.CONTIG_TYPE_FASTA.equals(loadType)) {
                LoadFastaContigsTask loadFastaContigs = new LoadFastaContigsTask();
                loadFastaContigs.setParameter(LoadFastaContigsTask.PARAM_CONTIG_FILE_PATH, contigFilePath);
                sectionTasks.add(new SectionStep(loadFastaContigs, "LoadFastaContigs"));
            }
            else {
                LoadGopherContigsTask loadGopherContigs = new LoadGopherContigsTask();
                loadGopherContigs.setParameter(LoadGopherContigsTask.PARAM_CONTIG_FILE_PATH, contigFilePath);
                sectionTasks.add(new SectionStep(loadGopherContigs, "LoadGopherContigs"));
            }
        }
//        else if (ProkaryoticAnnotationTask.GIP_LAUNCHER.equals(sectionStep)) {
//            GipLauncherTask gipLauncherTask = new GipLauncherTask();
//            gipLauncherTask.setParameter(GipLauncherTask.PARAM_ASSEMBLY_ID, GipLauncherTask.ASSEMBLY_ISCURRENT);
//            gipLauncherTask.setParameter(GipLauncherTask.PARAM_GENE_TYPE, GipLauncherTask.GENE_TYPE_LOCUS);
//            sectionTasks.add(new SectionStep(gipLauncherTask, "GipLauncher"));
//        }
        else if (ProkaryoticAnnotationTask.GIP_RUNNER.equals(sectionStep)) {
            ensureGipConfigFile();
            GipTask gipTask = new GipTask();
            gipTask.setParameter(GipTask.PARAM_CONFIG_LOCATION, targetGenomeDirectory + File.separator + "gip/config_file_ISCURRENT");
            sectionTasks.add(new SectionStep(gipTask, "Gip"));
        }
//        else if (ProkaryoticAnnotationTask.REWRITE_CHECKER.equals(sectionStep)) {
//            CheckSmallGenomeTask checkSmallGenomeTask = new CheckSmallGenomeTask();
//            checkSmallGenomeTask.setParameter(CheckSmallGenomeTask.PARAM_ANNOTATION_MODE, task.getParameter(ProkaryoticAnnotationTask.PARAM_annotationMode));
//            checkSmallGenomeTask.setParameter(CheckSmallGenomeTask.PARAM_STAGE, CheckSmallGenomeTask.STAGE_LOAD);
//            sectionTasks.add(new SectionStep(checkSmallGenomeTask, "CheckSmallGenome"));
//        }
//        else if (ProkaryoticAnnotationTask.GIP_CHECKER.equals(sectionStep)) {
//            CheckSmallGenomeTask checkSmallGenomeTask = new CheckSmallGenomeTask();
//            checkSmallGenomeTask.setParameter(CheckSmallGenomeTask.PARAM_ANNOTATION_MODE, task.getParameter(ProkaryoticAnnotationTask.PARAM_annotationMode));
//            checkSmallGenomeTask.setParameter(CheckSmallGenomeTask.PARAM_STAGE, CheckSmallGenomeTask.STAGE_GIP);
//            sectionTasks.add(new SectionStep(checkSmallGenomeTask, "CheckSmallGenome"));
//        }
//        else if (ProkaryoticAnnotationTask.SGC_CHECKER.equals(sectionStep)) {
//            CheckSmallGenomeTask checkSmallGenomeTask = new CheckSmallGenomeTask();
//            checkSmallGenomeTask.setParameter(CheckSmallGenomeTask.PARAM_ANNOTATION_MODE, task.getParameter(ProkaryoticAnnotationTask.PARAM_annotationMode));
//            checkSmallGenomeTask.setParameter(CheckSmallGenomeTask.PARAM_STAGE, CheckSmallGenomeTask.STAGE_SGC);
//            sectionTasks.add(new SectionStep(checkSmallGenomeTask, "CheckSmallGenome"));
//        }
        else if (ProkaryoticAnnotationTask.CONSISTENCY_CHECKER.equals(sectionStep)) {
            ConsistencyCheckerTask consistencyCheckerTask = new ConsistencyCheckerTask();
            consistencyCheckerTask.setParameter(CheckSmallGenomeTask.PARAM_ANNOTATION_MODE, task.getParameter(ProkaryoticAnnotationTask.PARAM_annotationMode));
            consistencyCheckerTask.setParameter(CheckSmallGenomeTask.PARAM_STAGE, CheckSmallGenomeTask.STAGE_SGC);
            sectionTasks.add(new SectionStep(consistencyCheckerTask, "ConsistencyChecker"));
        }
        else if (ProkaryoticAnnotationTask.GC_CONTENT_LOAD.equals(sectionStep)) {
            sectionTasks.add(new SectionStep(new GcContentLoaderTask(), "GcContentLoader"));
        }
        else if (ProkaryoticAnnotationTask.OVERLAP_RUNNER.equals(sectionStep)) {
            OverlapAnalysisTask overlapTask = new OverlapAnalysisTask();
            // NOTE: forcing NT_DATA param to true as CMR Genome operation is the only one which calls this step
            overlapTask.setParameter(OverlapAnalysisTask.PARAM_IS_NT_DATA, "true");
            sectionTasks.add(new SectionStep(overlapTask, "OverlapAnalysis"));
        }
        else if (ProkaryoticAnnotationTask.REWRITE_STEP.equals(sectionStep)) {
            sectionTasks.add(new SectionStep(new RewriteSequencesTask(), "RewriteSequences"));
        }
        else if (ProkaryoticAnnotationTask.LOCUS_LOADER.equals(sectionStep)) {
            sectionTasks.add(new SectionStep(new LocusLoaderTask(), "LocusLoader"));
        }
        else if (ProkaryoticAnnotationTask.SHORT_ORF_TRIM_RUNNER.equals(sectionStep)) {
            ShortOrfTrimTask shortOrfTrimTask = new ShortOrfTrimTask();
            shortOrfTrimTask.setParameter(ShortOrfTrimTask.PARAM_REMOVE_BLACKLISTED_ORFS, "true");
            sectionTasks.add(new SectionStep(shortOrfTrimTask, "ShortOrfTrim"));
        }
        else if (ProkaryoticAnnotationTask.SgcSetup.equals(sectionStep)) {
            SgcSetupTask sgcSetupTask = new SgcSetupTask();
            if (ProkaryoticAnnotationTask.MODE_JCVI_GENOME.equals(task.getParameter(ProkaryoticAnnotationTask.PARAM_annotationMode))) {
                sgcSetupTask.setParameter(SgcSetupTask.PARAM_TOGGLE_ANNOTATION, "true");
            }
            sectionTasks.add(new SectionStep(sgcSetupTask, "SgcSetup"));
        }
        else if (ProkaryoticAnnotationTask.ValetPepHmmIdentify.equals(sectionStep)) {
            sectionTasks.add(new SectionStep(new ValetPepHmmIdentifyTask(), "ValetPepHmmIdentify"));
        }
        else if (ProkaryoticAnnotationTask.ParseForNcRna.equals(sectionStep)) {
            sectionTasks.add(new SectionStep(new ParseForNcRnaTask(), "ParseForNcRna"));
        }
        else if (ProkaryoticAnnotationTask.SkewUpdate.equals(sectionStep)) {
            sectionTasks.add(new SectionStep(new SkewUpdateTask(), "SkewUpdate"));
        }
        else if (ProkaryoticAnnotationTask.TerminatorsFinder.equals(sectionStep)) {
            sectionTasks.add(new SectionStep(new TerminatorsFinderTask(), "TerminatorsFinder"));
        }
        else if (ProkaryoticAnnotationTask.RewriteSequences.equals(sectionStep)) {
            sectionTasks.add(new SectionStep(new RewriteSequencesTask(), "RewriteSequences"));
        }
        else if (ProkaryoticAnnotationTask.TransmembraneUpdate.equals(sectionStep)) {
            sectionTasks.add(new SectionStep(new TransmembraneUpdateTask(), "TransmembraneUpdate"));
        }
        else if (ProkaryoticAnnotationTask.MolecularWeightUpdate.equals(sectionStep)) {
            sectionTasks.add(new SectionStep(new MolecularWeightUpdateTask(), "MolecularWeightUpdate"));
        }
        else if (ProkaryoticAnnotationTask.OuterMembraneProteinUpdate.equals(sectionStep)) {
            sectionTasks.add(new SectionStep(new OuterMembraneProteinUpdateTask(), "OuterMembraneProteinUpdate"));
        }
        else if (ProkaryoticAnnotationTask.SignalPUpdate.equals(sectionStep)) {
            sectionTasks.add(new SectionStep(new SignalPUpdateTask(), "SignalPUpdate"));
        }
        else if (ProkaryoticAnnotationTask.LipoproteinUpdate.equals(sectionStep)) {
            sectionTasks.add(new SectionStep(new LipoproteinUpdateTask(), "LipoproteinUpdate"));
        }
//        else if (ProkaryoticAnnotationTask.SgcPsortB.equals(sectionStep)) {
//            sectionTasks.add(new SectionStep(new SgcPsortBTask(), "SgcPsortB"));
//        }
        else if (ProkaryoticAnnotationTask.CogSearch.equals(sectionStep)) {
            sectionTasks.add(new SectionStep(new CogSearchTask(), "CogSearch"));
        }
        else if (ProkaryoticAnnotationTask.Hmmer3Search.equals(sectionStep)) {
            sectionTasks.add(new SectionStep(new ProkHmmer3SearchTask(), "ProkHmmer3Search"));
        }
        else if (ProkaryoticAnnotationTask.BtabToMultiAlignment.equals(sectionStep)) {
            sectionTasks.add(new SectionStep(new BtabToMultiAlignmentTask(), "BtabToMultiAlignment"));
        }
//        else if (ProkaryoticAnnotationTask.PrositeSearch.equals(sectionStep)) {
//            sectionTasks.add(new SectionStep(new PrositeSearchTask(), "PrositeSearch"));
//        }
        else if (ProkaryoticAnnotationTask.AutoGeneCuration.equals(sectionStep)) {
            sectionTasks.add(new SectionStep(new AutoGeneCurationTask(), "AutoGeneCuration"));
        }
        else if (ProkaryoticAnnotationTask.LinkToNtFeatures.equals(sectionStep)) {
            sectionTasks.add(new SectionStep(new LinkToNtFeaturesTask(), "LinkToNtFeatures"));
        }
        else if (ProkaryoticAnnotationTask.TaxonLoader.equals(sectionStep)) {
            sectionTasks.add(new SectionStep(new TaxonLoaderTask(), "TaxonLoader"));
        }
        else if (ProkaryoticAnnotationTask.EvaluateGenomeProperties.equals(sectionStep)) {
            sectionTasks.add(new SectionStep(new EvaluateGenomePropertiesTask(), "EvaluateGenomeProperties"));
        }
        else if (ProkaryoticAnnotationTask.AutoFrameShiftDetection.equals(sectionStep)) {
            sectionTasks.add(new SectionStep(new AutoFrameShiftDetectionTask(), "AutoFrameShiftDetection"));
        }
        else if (ProkaryoticAnnotationTask.BuildContigFile.equals(sectionStep)) {
            sectionTasks.add(new SectionStep(new BuildContigFileTask(), "BuildContigFile"));
        }
//        else if (ProkaryoticAnnotationTask.PressDb1Con.equals(sectionStep)) {
//            PressDbTask pressDb1conTask = new PressDbTask();
//            pressDb1conTask.setParameter(PressDbTask.PARAM_FILE_SUFFIX, PressDbTask.SUFFIX_1CON);
//            sectionTasks.add(new SectionStep(pressDb1conTask, "PressDb"));
//        }
        else if (ProkaryoticAnnotationTask.BuildCoordinateSetFile.equals(sectionStep)) {
            sectionTasks.add(new SectionStep(new BuildCoordinateSetFileTask(), "BuildCoordinateSetFile"));
        }
        else if (ProkaryoticAnnotationTask.BuildSequenceFile.equals(sectionStep)) {
            sectionTasks.add(new SectionStep(new BuildSequenceFileTask(), "BuildSequenceFile"));
        }
//        else if (ProkaryoticAnnotationTask.PressDbSeq.equals(sectionStep)) {
//            PressDbTask pressDbSeqTask = new PressDbTask();
//            pressDbSeqTask.setParameter(PressDbTask.PARAM_FILE_SUFFIX, PressDbTask.SUFFIX_SEQ);
//            sectionTasks.add(new SectionStep(pressDbSeqTask, "PressDb"));
//        }
        else if (ProkaryoticAnnotationTask.BuildPeptideFile.equals(sectionStep)) {
            sectionTasks.add(new SectionStep(new BuildPeptideFileTask(), "BuildPeptideFile"));
        }
//        else if (ProkaryoticAnnotationTask.SetDb.equals(sectionStep)) {
//            SetDbTask setDbTask = new SetDbTask();
//            sectionTasks.add(new SectionStep(setDbTask, "SetDb"));
//        }
        else if (ProkaryoticAnnotationTask.CoreHMMCheck.equals(sectionStep)) {
            sectionTasks.add(new SectionStep(new CoreHmmCheckTask(), "CoreHmmCheck"));
        }

        // Set base values all tasks need, then submit them sequentially
        for (SectionStep sectionTask : sectionTasks) {
            Task tmpTask = sectionTask.getTask();
            setBaseValues(tmpTask);
            tmpTask = EJBFactory.getLocalComputeBean().saveOrUpdateTask(tmpTask);
            SubmitJobAndWaitHelper jobHelper = new SubmitJobAndWaitHelper(sectionTask.getProcessFile(), tmpTask.getObjectId());
            jobHelper.startAndWaitTillDone();
            computeBean.addEventToTask(task.getObjectId(), new Event("Completed Step:" + tmpTask.getDisplayName(), new Date(), tmpTask.getDisplayName()));
        }
        computeBean.addEventToTask(task.getObjectId(), new Event("Completed Step:" + sectionStep, new Date(), sectionStep));
        logger.debug("\n\n" + sectionStep + " executed successfully.\n\n");
    }

    private void setBaseValues(Task tmpTask) {
        File tmpGenomeDir = new File(targetGenomeDirectory);
        tmpTask.setOwner(task.getOwner());
        tmpTask.setParentTaskId(task.getObjectId());
        tmpTask.setParameter(ProkPipelineBaseTask.PARAM_DB_NAME, tmpGenomeDir.getName().toLowerCase());
        tmpTask.setParameter(ProkPipelineBaseTask.PARAM_DB_USERNAME, task.getParameter(ProkaryoticAnnotationTask.PARAM_username));
        tmpTask.setParameter(ProkPipelineBaseTask.PARAM_DB_PASSWORD, task.getParameter(ProkaryoticAnnotationTask.PARAM_sybasePassword));
        tmpTask.setParameter(ProkPipelineBaseTask.PARAM_DIRECTORY, task.getParameter(ProkaryoticAnnotationTask.PARAM_targetDirectory));
        tmpTask.setParameter(ProkPipelineBaseTask.PARAM_project, task.getParameter(ProkaryoticAnnotationTask.PARAM_project));
    }

    private class SectionStep {
        private Task __task;
        private String __processFile;
        public SectionStep(Task task, String processFile) {
            this.__task = task;
            this.__processFile = processFile;
        }

        public Task getTask() {
            return __task;
        }

        public String getProcessFile() {
            return __processFile;
        }
    }

    private void ensureGipConfigFile() throws IOException, ServiceException {
        // Write the Gip configuration file
        File tmpGipDir = new File(targetGenomeDirectory + File.separator + "gip");
        if (!tmpGipDir.exists()) {
            boolean tmpDirCreated = tmpGipDir.mkdirs();
            if (!tmpDirCreated) {
                throw new ServiceException("Unable to create the gip directory.");
            }
        }

        if (null != task.getParameter(ProkaryoticAnnotationTask.PARAM_gipConfigurationString)) {
            FileWriter tmpGipConfigWriter = new FileWriter(tmpGipDir + File.separator + "config_file_ISCURRENT");
            try {
                if (null != task.getParameter(ProkaryoticAnnotationTask.PARAM_gipConfigurationString)) {
                    tmpGipConfigWriter.write(task.getParameter(ProkaryoticAnnotationTask.PARAM_gipConfigurationString));
                }
            }
            finally {
                tmpGipConfigWriter.flush();
                tmpGipConfigWriter.close();
            }
        }
    }

}