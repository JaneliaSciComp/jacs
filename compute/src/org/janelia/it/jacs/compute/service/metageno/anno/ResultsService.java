
package org.janelia.it.jacs.compute.service.metageno.anno;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.ComputeBaseDAO;
import org.janelia.it.jacs.compute.api.ComputeBeanRemote;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.metageno.MetaGenoAnnotationTask;
import org.janelia.it.jacs.model.user_data.metageno.MetaGenoAnnotationResultNode;
import org.janelia.it.jacs.shared.utils.FileUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Mar 19, 2009
 * Time: 2:34:36 PM
 */
public class ResultsService implements IService {
    private Logger logger;

    MetaGenoAnnotationTask parentTask;
    MetaGenoAnnotationResultNode resultNode;
    File parentDir;
    List<File> inputFileList;
    boolean parentTaskErrorFlag = false;

    /* SCRIPT DEPENDENCIES
        <none>
     */

    /*
     *  Step 1: 'copying split_multifasta_pep'
     *
     *     1.a Create result directories:
     *
     *         camera_annotation_parser
     *         camera_annotation_rules
     *         ldhmmpfam
     *         lipoprotein_motif
     *         ncbi-blastp
     *         priam_ec
     *         split_multifasta_pep
     *         tmhmm
     *
     *     1.b copy all original multifasta files to the 'split_multifasta_pep' directory
     *
     *  Step 2: 'copying ldhmmpfam' (dir='ldhmmpfam')
     *
     *     2.a  concat all frag htab results >> ldhmmpfam.htab.combined.out
     *
     *     2.b  concat all frag ldhmmpfam exec results >> ldhmmpfam.raw.combined.out
     *
     *     2.c  concat all full htab results >> ldhmmpfam.htab.combined.out
     *
     *     2.d  concat all full ldhmmpfam exec results >> ldhmmpfam.raw.combined.out
     *
     *  Step 3: 'copying camera_annotation parser' (dir='camera_annotation_parser')
     *
     *     3.a  concat all blastp_panda annotation results >> camera_annotation_parser.raw.combined.out
     *
     *     3.b  concat all hmm frag annotation results >> ""
     *
     *     3.c  concat all hmm full annotation results >> ""
     *
     *     3.d  concat all hypothetical annotation results >> ""
     *
     *     3.e  concat all lipoprotein annotation results >> ""
     *
     *     3.f  concat all priam_ec annotation results >> ""
     *
     *     3.g  concat all tmhmm annotation results >> ""
     *
     *  Step 4: 'copying ncbi-blastp' (dir='ncbi-blastp')
     *
     *     4.a  concat all ncbi-blastp btab results >> ncbi_blastp_btab.combined.out
     *
     *     4.b  concat all blast_out results >> ncbi_blastp_raw.combined.out
     *
     *  Step 5: 'copying priam_ec' (dir='priam_ec')
     *
     *     5.a  concat all ectab results >> priam_ec.ectab.combined.out
     *
     *     5.b  concat all ec hits >> priam_ec_output.hits
     *
     *  Step 6: 'copying lipprotein_motif'
     *
     *     6.a  concat all bsml >> lipoprotein_bsml.combined.out
     *
     *  Step 7: 'copying tmhmm'
     *
     *     7.a  concat tmhmm exec output >> tmhmm.raw.combined.out
     *
     *  Step 8: 'copying camera_annotation_rules'
     *
     *     8.a  concat rules files >> annotation_rules.combined.out
     *
     */

    public void execute(IProcessData processData) throws ServiceException {
        try {
            logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            logger.info(this.getClass().getName() + " execute() start");
            init(processData);

            logger.info("Processing full hmm results...");
            processLdhmmpfamResults("full", Hmmpfam3Service.class.getSimpleName());

//            logger.info("Processing full frag results...");
//            processLdhmmpfamResults("frag", HmmpfamFragService.class.getSimpleName());

            logger.info("Processing parser results...");
            processParserResults();

            logger.info("Processing blastp results...");
            processNcbiBlastpResults();
            //processPriamResults();
            logger.info("Processing lipo results...");
            processLipoResults();
            logger.info("Processing tmhmm results...");
            processTmhmmResults();
            logger.info("Processing rules results...");
            processRulesResults();
            logger.info(this.getClass().getName() + " execute() finish");
        }
        catch (Exception e) {
            if (parentTaskErrorFlag) {
                logger.info("Parent task has error -returning");
                logger.info("ERROR MESSAGE:"+this.getClass().getName() + " : " + e.getMessage());
            }
            else {
                this.setParentTaskToErrorStatus(this.getClass().getName() + " : " + e.getMessage());
                throw new ServiceException(e);
            }
        }
    }

    private void init(IProcessData processData) throws MissingDataException {
        parentTask = (MetaGenoAnnotationTask) ProcessDataHelper.getTask(processData);
        if (parentTask == null) {
            throw new MissingDataException("Could not get parent task for " + this.getClass().getName());
        }
        if (checkParentTaskForError()) {
            this.parentTaskErrorFlag = true;
            throw new MissingDataException("Parent task has ERROR event");
        }
        resultNode = (MetaGenoAnnotationResultNode) processData.getItem("META_GENO_ANNOTATION_RESULT_NODE");
        if (resultNode == null) {
            throw new MissingDataException("Could not get result node for task=" + parentTask.getObjectId());
        }
        inputFileList = (List<File>) processData.getItem("MG_INPUT_ARRAY");
        parentDir = new File(resultNode.getDirectoryPath());
    }

    private void processLdhmmpfamResults(String type, String name) throws Exception {
        File ldHtabConcatFile = new File(parentDir, "ldhmmpfam_" + type + ".htab.combined.out");
        File ldRawConcatFile = new File(parentDir, "ldhmmpfam_" + type + ".raw.combined.out");
        List<File> htabFiles = new ArrayList<File>();
        List<File> rawFiles = new ArrayList<File>();

        // Frag first
        File ldDir = getServiceDir(name);
        File[] files = ldDir.listFiles();
        for (File f : files) {
            if (f.getName().endsWith(".htab")) {
                htabFiles.add(f);
            }
            else if (f.getName().endsWith(".raw")) {
                rawFiles.add(f);
            }
        }

        // Concat
        FileUtil.concatFilesUsingSystemCall(htabFiles, ldHtabConcatFile);
        FileUtil.concatFilesUsingSystemCall(rawFiles, ldRawConcatFile);
    }

    File getServiceDir(String name) throws Exception {
        File dir = new File(parentDir, name);
        if (!dir.exists()) {
            throw new Exception("Could not find dir=" + dir.getAbsolutePath());
        }
        return dir;
    }

    private void processParserResults() throws Exception {
        File parserConcatFile = new File(parentDir, "camera_annotation_parser.raw.combined.out");
        List<File> fileList = new ArrayList<File>();
        List<File> dirList = new ArrayList<File>();
        dirList.add(getServiceDir(PandaBlastpService.class.getSimpleName()));
        //dirList.add(getServiceDir(HmmpfamFragService.class.getSimpleName()));
        dirList.add(getServiceDir(Hmmpfam3Service.class.getSimpleName()));
        dirList.add(getServiceDir(HypotheticalService.class.getSimpleName()));
        dirList.add(getServiceDir(LipoproteinService.class.getSimpleName()));
//        List<File> priamDirList = getPriamResultDirs();
//        for (File d : priamDirList) {
//            dirList.add(d);
//        }
        dirList.add(getServiceDir(MgTmhmmService.class.getSimpleName()));
        for (File d : dirList) {
            logger.info("Evaluating this directory for camera_annotation_parser.raw.combined.out=" + d.getAbsolutePath());
            File[] files = d.listFiles();
            int parsedCount = 0;
            for (File f : files) {
                if (f.getName().endsWith(".parsed")) {
                    logger.info("Adding this file to camera_annotation_parser.raw.combined.out=" + f.getAbsolutePath());
                    fileList.add(f);
                    parsedCount++;
                }
            }
            if (parsedCount == 0) {
                throw new Exception("Could not find any parsed files in dir=" + d.getAbsolutePath());
            }
        }
        FileUtil.concatFilesUsingSystemCall(fileList, parserConcatFile);
    }

    private List<File> getPriamResultDirs() throws Exception {
        File priamTopDir = new File(parentDir, "PriamResult");
        if (!priamTopDir.isDirectory()) {
            throw new Exception("Could not locate directory " + priamTopDir);
        }
        File[] pArr = priamTopDir.listFiles();
        List<File> pList = new ArrayList<File>();
        pList.addAll(Arrays.asList(pArr));
        return pList;
    }

    private void processNcbiBlastpResults() throws Exception {
        File btabFile = new File(parentDir, "ncbi_blastp_btab.combined.out");
        List<File> btabFileList = new ArrayList<File>();
        File rawFile = new File(parentDir, "ncbi_blastp_raw.combined.out");
        List<File> rawFileList = new ArrayList<File>();
        File blastDir = getServiceDir(PandaBlastpService.class.getSimpleName());
        File[] files = blastDir.listFiles();
        for (File f : files) {
            if (f.getName().endsWith(".blastp_out")) {
                rawFileList.add(f);
            }
            else if (f.getName().endsWith(".blastp_out.btab")) {
                btabFileList.add(f);
            }
        }
        FileUtil.concatFilesUsingSystemCall(btabFileList, btabFile);
        FileUtil.concatFilesUsingSystemCall(rawFileList, rawFile);
    }

    private void processPriamResults() throws Exception {
        File ectabFile = new File(parentDir, "priam_ec.ectab.combined.out");
        List<File> ectabFileList = new ArrayList<File>();
        File hitsFile = new File(parentDir, "priam_ec.output.hits");
        List<File> hitFileList = new ArrayList<File>();
        List<File> priamDirs = getPriamResultDirs();
        for (File d : priamDirs) {
            File[] files = d.listFiles();
            for (File f : files) {
                if (f.getName().startsWith("priam_ec_hits") && f.getName().endsWith(".ectab")) {
                    ectabFileList.add(f);
                }
                else if (f.getName().endsWith("priam_ec_hits")) {
                    hitFileList.add(f);
                }
            }
        }
        FileUtil.concatFilesUsingSystemCall(ectabFileList, ectabFile);
        FileUtil.concatFilesUsingSystemCall(hitFileList, hitsFile);
    }

    private void processLipoResults() throws Exception {
        File lipoFile = new File(parentDir, "lipoprotein_bsml.parsed");
        List<File> lipoFileList = new ArrayList<File>();
        File lipoDir = getServiceDir(LipoproteinService.class.getSimpleName());
        File[] files = lipoDir.listFiles();
        for (File f : files) {
            if (f.getName().endsWith(".parsed")) {
                lipoFileList.add(f);
            }
        }
        FileUtil.concatFilesUsingSystemCall(lipoFileList, lipoFile);
    }

    private void processTmhmmResults() throws Exception {
        File tmFile = new File(parentDir, "tmhmm.raw.combined.out");
        List<File> tmFileList = new ArrayList<File>();
        File tmDir = getServiceDir(MgTmhmmService.class.getSimpleName());
        File[] files = tmDir.listFiles();
        for (File f : files) {
            if (f.getName().endsWith(".tmhmm_out")) {
                tmFileList.add(f);
            }
        }
        FileUtil.concatFilesUsingSystemCall(tmFileList, tmFile);
    }

    private void processRulesResults() throws Exception {
        File arFile = new File(parentDir, "annotation_rules.combined.out");
        List<File> arFileList = new ArrayList<File>();
        File arDir = getServiceDir(RulesService.class.getSimpleName());
        File[] files = arDir.listFiles();
        for (File f : files) {
            if (f.getName().endsWith(".rules")) {
                arFileList.add(f);
            }
        }
        FileUtil.concatFilesUsingSystemCall(arFileList, arFile);
    }

    protected void setParentTaskToErrorStatus(String message) {
        try {
            ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
            computeBean.saveEvent(parentTask.getObjectId(), Event.ERROR_EVENT, message, new Date());
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    protected boolean checkParentTaskForError() {
        try {
            ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
            String[] status = computeBean.getTaskStatus(parentTask.getObjectId());
            return status[ComputeBaseDAO.STATUS_TYPE].equals(Event.ERROR_EVENT);
        }
        catch (Exception ex) {
            ex.printStackTrace();
            return true;
        }
    }

}
