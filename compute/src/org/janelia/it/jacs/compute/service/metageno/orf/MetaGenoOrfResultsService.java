
package org.janelia.it.jacs.compute.service.metageno.orf;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.ComputeBeanRemote;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.metageno.MetaGenoPerlConfig;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.metageno.*;
import org.janelia.it.jacs.model.user_data.metageno.*;
import org.janelia.it.jacs.shared.utils.FileUtil;
import org.janelia.it.jacs.shared.utils.SystemCall;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Feb 19, 2009
 * Time: 4:51:17 PM
 */

/* SCRIPT DEPENDENCIES
    pepFastaCmd=/usr/local/devel/ANNOTATION/mg-annotation/testing/smurphy-20090825-DO_NOT_USE_FOR_PRODUCTION/util/perl/get_seq_no_gzip.pl
        <none>
 */


/*
 *  Steps for completing results of Metagenomic Orf Calling Service:
 *
 *  CLEAR RANGE CASE
 *
 *    1) tRNA fasta files >> camera_extract_trna.combined.fasta
 *
 *    2) rRNA fasta files >> camera_rrna_finder.combined.fasta
 *
 *    3) open_reading_frames >> open_reading_frames.combined.faa
 *                              open_reading_frames.combined.fna
 *
 *    4) clr range filtered orfs >> clr_range_filter_orf.clr.combined.fna
 *                                  clr_range_filter_orf.full.combined.fna
 *
 *    5) clr range filtered pep >> clr_range_filter_pep.clr.combined.faa
 *                                 clr_range_filter_pep.full.combined.faa
 *
 *    6) metagene orfs >> metagene_mapped_pep.fasta
 *
 *    7) metagene raw >> metagene.combined.raw
 *
 *  NO CLEAR RANGE CASE
 *
 *    1) tRNA fasta files >> camera_extract_trna.combined.fasta
 *
 *    2) rRNA fasta files >> camera_rrna_finder.combined.fasta
 *
 *    3) open_reading_frames >> open_reading_frames.combined.faa
 *                              open_reading_frames.combined.fna
 *
 *    4) metagene orfs >> metagene_mapped_pep.fasta
 *
 *    5) metagene raw >> metagene.combined.raw
 * 
 */

public class MetaGenoOrfResultsService implements IService {
    private Logger logger;

    private static String pepFastaCmd = SystemConfigurationProperties.getString("MgPipeline.MetagenePepFastaCmd");
    private static String scratchDirPath = SystemConfigurationProperties.getString("SystemCall.ScratchDir");
    private static Boolean mgOrfCleanup = SystemConfigurationProperties.getBoolean("MgOrfPipeline.Cleanup");

    private Boolean useClearRange = false;
    SystemCall sc;

    public void execute(IProcessData processData) throws ServiceException {
        try {
            logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            MetaGenoOrfCallerTask parentTask = (MetaGenoOrfCallerTask) ProcessDataHelper.getTask(processData);
            if (parentTask.getParameter(MetaGenoOrfCallerTask.PARAM_useClearRange).equals(Boolean.TRUE.toString())) {
                logger.info("setting useClearRange=true");
                useClearRange = true;
            }
            else {
                logger.info("assuming useClearRange=false");
            }
            MetaGenoOrfCallerResultNode parentResultNode = (MetaGenoOrfCallerResultNode) processData.getItem("META_GENO_ORF_CALLER_RESULT_NODE");
            ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
            List<Task> childTasks = computeBean.getChildTasksByParentTaskId(parentTask.getObjectId());
            List<TrnaScanTask> trnaTaskList = new ArrayList<TrnaScanTask>();
            List<RrnaScanTask> rrnaTaskList = new ArrayList<RrnaScanTask>();
            List<SimpleOrfCallerTask> simpleOrfCallerTaskList = new ArrayList<SimpleOrfCallerTask>();
            List<MetageneTask> metageneTaskList = new ArrayList<MetageneTask>();
            for (Task task : childTasks) {
                if (task instanceof TrnaScanTask) {
                    logger.info("For results of orf-caller task=" + parentTask.getObjectId() + " adding TrnaScan task=" + task.getObjectId());
                    trnaTaskList.add((TrnaScanTask) task);
                }
                else if (task instanceof RrnaScanTask) {
                    logger.info("For results of orf-caller task=" + parentTask.getObjectId() + " adding RrnaScan task=" + task.getObjectId());
                    rrnaTaskList.add((RrnaScanTask) task);
                }
                else if (task instanceof SimpleOrfCallerTask) {
                    logger.info("For results of orf-caller task=" + parentTask.getObjectId() + " adding SimpleOrfCaller task=" + task.getObjectId());
                    simpleOrfCallerTaskList.add((SimpleOrfCallerTask) task);
                }
                else if (task instanceof MetageneTask) {
                    logger.info("For results of orf-caller task=" + parentTask.getObjectId() + " adding Metagene task=" + task.getObjectId());
                    metageneTaskList.add((MetageneTask) task);
                }
                else {
                    throw new Exception("Do not recognize task type=" + task.getClass().getName());
                }
            }

            File scratchDir = new File(scratchDirPath);
            logger.info("Using scratchDir=" + scratchDir.getAbsolutePath());
            sc = new SystemCall(null, scratchDir, logger);

            processTrnaResults(parentResultNode, trnaTaskList);
            processRrnaResults(parentResultNode, rrnaTaskList);
            processSimpleOrfCallerResults(parentResultNode, simpleOrfCallerTaskList);
            if (useClearRange) {
                logger.info("processing ClrRangeResults");
                processClrRangeResults(parentResultNode);
            }
            else {
                logger.info("skipping ClrRangeResults processing");
            }
            processPepFasta(parentResultNode, metageneTaskList);
            if (mgOrfCleanup)
                cleanup(parentResultNode);
            sc.cleanup();
            logger.info("Done");
        }
        catch (Exception e) {
            throw new ServiceException(e);
        }
    }

    private void processTrnaResults(MetaGenoOrfCallerResultNode parentResultNode, List<TrnaScanTask> trnaScanTaskList)
            throws Exception {
        ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
        File parentResultNodeDir = new File(parentResultNode.getDirectoryPath());
        File trnaCombinedFastaFile = new File(parentResultNodeDir, "camera_extract_trna.combined.fasta");
        List<File> resultFileList = new ArrayList<File>();
        for (TrnaScanTask t : trnaScanTaskList) {
            TrnaScanResultNode resultNode = (TrnaScanResultNode) computeBean.getResultNodeByTaskId(t.getObjectId());
            File resultDir = new File(resultNode.getDirectoryPath());
            File[] resultFiles = resultDir.listFiles();
            for (File f : resultFiles) {
                if (f.getName().endsWith("raw_tRNA.fasta")) {
                    resultFileList.add(f);
                }
            }
        }
        FileUtil.concatFilesUsingSystemCall(resultFileList, trnaCombinedFastaFile);
    }

    private void processRrnaResults(MetaGenoOrfCallerResultNode parentResultNode, List<RrnaScanTask> rrnaScanTaskList)
            throws Exception {
        ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
        File parentResultNodeDir = new File(parentResultNode.getDirectoryPath());
        File rrnaCombinedFastaFile = new File(parentResultNodeDir, "camera_rrna_finder.combined.fasta");
        logger.info("Consolidated rrna combined fasta file=" + rrnaCombinedFastaFile.getAbsolutePath());
        List<File> resultFileList = new ArrayList<File>();
        int count = 0;
        for (RrnaScanTask t : rrnaScanTaskList) {
            logger.info("Processing RrnaTask=" + t.getObjectId());
            RrnaScanResultNode resultNode = (RrnaScanResultNode) computeBean.getResultNodeByTaskId(t.getObjectId());
            File resultDir = new File(resultNode.getDirectoryPath());
            File[] resultFiles = resultDir.listFiles();
            for (File f : resultFiles) {
                if (f.getName().equals("rrnascan_rRNA.fasta")) {
                    count++;
                    resultFileList.add(f);
                }
            }
        }
        logger.info("Adding " + count + " rrnascan_rRNA.fasta files to produce combined file");
        FileUtil.concatFilesUsingSystemCall(resultFileList, rrnaCombinedFastaFile);
    }

    private void processSimpleOrfCallerResults(MetaGenoOrfCallerResultNode parentResultNode,
                                               List<SimpleOrfCallerTask> simpleOrfCallerTaskList) throws Exception {
        ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
        File parentResultNodeDir = new File(parentResultNode.getDirectoryPath());
        File fnaCombinedFastaFile = new File(parentResultNodeDir, "open_reading_frames.combined.fna");
        File faaCombinedFastaFile = new File(parentResultNodeDir, "open_reading_frames.combined.faa");
        File listFaaFile = new File(parentResultNodeDir, "orf.combined.faa.list");
        File mapFaaFile = new File(parentResultNodeDir, "orf.combined.faa.map");
        List<File> mapPepFileList = new ArrayList<File>();
        FileWriter fw = new FileWriter(listFaaFile);
        List<File> naResultFileList = new ArrayList<File>();
        List<File> aaResultFileList = new ArrayList<File>();
        for (SimpleOrfCallerTask t : simpleOrfCallerTaskList) {
            SimpleOrfCallerResultNode resultNode = (SimpleOrfCallerResultNode) computeBean.getResultNodeByTaskId(t.getObjectId());
            File resultDir = new File(resultNode.getDirectoryPath());
            File[] resultFiles = resultDir.listFiles();
            for (File f : resultFiles) {
                if (f.getName().endsWith("_nt.fasta")) {
                    naResultFileList.add(f);
                }
                else if (f.getName().endsWith("_aa.fasta")) {
                    aaResultFileList.add(f);
                    fw.write(f.getAbsolutePath() + "\n");
                }
                else if (f.getName().endsWith(".mapping_orf")) {
                    mapPepFileList.add(f);
                }
            }
        }
        fw.close();
        FileUtil.concatFilesUsingSystemCall(naResultFileList, fnaCombinedFastaFile);
        FileUtil.concatFilesUsingSystemCall(aaResultFileList, faaCombinedFastaFile);
        if (useClearRange) {
            logger.info("skipping generation of orf.combined.faa.map");
        }
        else {
//            // We must pick up the mapping files from the parent directory
//            logger.info("generating orf.combined.faa.map");
//            File mapFaaFile = new File(parentResultNodeDir, "orf.combined.faa.map");
//            List<File> mapPepFileList=new ArrayList<File>();
//            File[] dirs=parentResultNodeDir.listFiles();
//            for (File dir : dirs) {
//                if (dir.getName().endsWith("_pep.clrDir")) {
//                    File[] files = dir.listFiles();
//                    for (File f : files) {
//                        if (f.getName().endsWith(".faa.btab.mapping_orf")) {
//                            mapPepFileList.add(f);
//                        }
//                    }
//                }
//            }
            FileUtil.concatFilesUsingSystemCall(mapPepFileList, mapFaaFile);
        }
    }

    private void processClrRangeResults(MetaGenoOrfCallerResultNode parentResultNode) throws Exception {
        File parentResultNodeDir = new File(parentResultNode.getDirectoryPath());

        // Part I: orf files
        File clrFnaFile = new File(parentResultNodeDir, "clr_range_filter_orf.clr.combined.fna");
        File fullFnaFile = new File(parentResultNodeDir, "clr_range_filter_orf.full.combined.fna");
        List<File> clrFileList = new ArrayList<File>();
        List<File> fullFileList = new ArrayList<File>();
        File[] dirs = parentResultNodeDir.listFiles();
        for (File dir : dirs) {
            if (dir.getName().endsWith("_orf.clrDir")) {
                File[] files = dir.listFiles();
                for (File f : files) {
                    if (f.getName().endsWith(".clr_range.fna")) {
                        clrFileList.add(f);
                    }
                    else if (f.getName().endsWith(".full_read.fna")) {
                        fullFileList.add(f);
                    }
                }
            }
        }
        FileUtil.concatFilesUsingSystemCall(clrFileList, clrFnaFile);
        FileUtil.concatFilesUsingSystemCall(fullFileList, fullFnaFile);

        // Part II: pep files
        File clrFaaFile = new File(parentResultNodeDir, "clr_range_filter_pep.clr.combined.faa");
        File fullFaaFile = new File(parentResultNodeDir, "clr_range_filter_pep.full.combined.faa");
        File listFaaFile = new File(parentResultNodeDir, "clr_range_filter_pep.clr.combined.faa.list");
        File mapFaaFile = new File(parentResultNodeDir, "clr_range_filter_pep.clr.combined.faa.map");
        FileWriter fw = new FileWriter(listFaaFile);
        List<File> clrPepFileList = new ArrayList<File>();
        List<File> fullPepFileList = new ArrayList<File>();
        List<File> mapPepFileList = new ArrayList<File>();
        for (File dir : dirs) {
            if (dir.getName().endsWith("_pep.clrDir")) {
                File[] files = dir.listFiles();
                for (File f : files) {
                    if (f.getName().endsWith(".clr_range.faa")) {
                        clrPepFileList.add(f);
                        fw.write(f.getAbsolutePath() + "\n");
                    }
                    else if (f.getName().endsWith(".full_read.faa")) {
                        fullPepFileList.add(f);
                    }
                    else if (f.getName().endsWith(".faa.btab.mapping_orf")) {
                        mapPepFileList.add(f);
                    }
                }
            }
        }
        fw.close();
        FileUtil.concatFilesUsingSystemCall(clrPepFileList, clrFaaFile);
        FileUtil.concatFilesUsingSystemCall(fullPepFileList, fullFaaFile);
        FileUtil.concatFilesUsingSystemCall(mapPepFileList, mapFaaFile);
    }

    private void processPepFasta(MetaGenoOrfCallerResultNode parentResultNode,
                                 List<MetageneTask> metageneTaskList) throws Exception {
        File parentResultNodeDir = new File(parentResultNode.getDirectoryPath());

        // Part I: Assign map files
        File metageneMapFile;
        File clrPepFastaListFile;
        if (useClearRange) {
            metageneMapFile = new File(parentResultNodeDir, "clr_range_filter_pep.clr.combined.faa.map");
            clrPepFastaListFile = new File(parentResultNodeDir, "clr_range_filter_pep.clr.combined.faa.list");
        }
        else {
            metageneMapFile = new File(parentResultNodeDir, "orf.combined.faa.map");
            clrPepFastaListFile = new File(parentResultNodeDir, "orf.combined.faa.list");
        }

        // Part II: Pep Fasta file
        File mapFastaFile = new File(parentResultNodeDir, "metagene_mapped_pep.fasta");
        String mapFastaCmd = MetaGenoPerlConfig.getCmdPrefix() + pepFastaCmd +
                " -i " + metageneMapFile.getAbsolutePath() +
                " -s " + clrPepFastaListFile.getAbsolutePath() +
                " -o " + mapFastaFile.getAbsolutePath();
        int ev = sc.execute(mapFastaCmd, false);
        if (ev != 0) {
            throw new Exception("non-zero exit value from cmd=" + mapFastaCmd);
        }

        // Part III: Metagene raw file
        File metageneRawFile = new File(parentResultNodeDir, "metagene.combined.raw");
        List<File> rawFileList = new ArrayList<File>();
        ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
        for (MetageneTask task : metageneTaskList) {
            MetageneResultNode resultNode = (MetageneResultNode) computeBean.getResultNodeByTaskId(task.getObjectId());
            File rawFile = new File(resultNode.getFilePathByTag(MetageneResultNode.TAG_RAW_OUTPUT));
            rawFileList.add(rawFile);
        }
        FileUtil.concatFilesUsingSystemCall(rawFileList, metageneRawFile);
    }

    private void cleanup(MetaGenoOrfCallerResultNode parentResultNode) throws Exception {
        // Delete all directories and subcontents
        File dir = new File(parentResultNode.getDirectoryPath());
        File[] files = dir.listFiles();
        for (File f : files) {
            if (f.isDirectory()) {
                recursiveDelete(f);
            }
            else if (f.getName().endsWith(".list")) {
                f.delete();
            }
            else if (f.getName().endsWith("combined.faa.map")) {
                f.delete();
            }
        }
    }

    private void recursiveDelete(File f) {
        if (f.isDirectory()) {
            File[] fs = f.listFiles();
            for (File ff : fs) {
                recursiveDelete(ff);
            }
        }
        f.delete();
    }

}
