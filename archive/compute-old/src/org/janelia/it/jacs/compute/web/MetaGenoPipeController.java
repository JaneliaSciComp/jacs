
package org.janelia.it.jacs.compute.web;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.ComputeBeanRemote;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.model.genomics.SequenceType;
import org.janelia.it.jacs.model.tasks.metageno.MetaGenoAnnotationTask;
import org.janelia.it.jacs.model.tasks.metageno.MetaGenoCombinedOrfAnnoTask;
import org.janelia.it.jacs.model.tasks.metageno.MetaGenoOrfCallerTask;
import org.janelia.it.jacs.model.user_data.FastaFileNode;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.shared.node.FastaUtil;
import org.janelia.it.jacs.shared.utils.SystemCall;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Feb 10, 2009
 * Time: 10:19:40 AM
 */
public class MetaGenoPipeController extends HttpServlet {
    public static String SUBMIT_NODE = "node";
    public static String SUBMIT_ORF = "orf";
    public static String SUBMIT_ANNO = "anno";
    public static String SUBMIT_COMBINED = "combined";

    public static String SESSION_INPUT_NODE_ID = "SESSION_INPUT_NODE_ID";
    public static String SESSION_MG_ORF_TASK_ID = "SESSION_MG_ORF_TASK_ID";
    public static String SESSION_MG_ANNO_TASK_ID = "SESSION_MG_ANNO_TASK_ID";
    public static String SESSION_MG_COMBINED_TASK_ID = "SESSION_MG_COMBINED_TASK_ID";

    private static Logger logger = Logger.getLogger(MetaGenoPipeController.class);

    protected void doPost(HttpServletRequest httpServletRequest,
                          HttpServletResponse httpServletResponse)
            throws ServletException, IOException {
        doProcess(httpServletRequest, httpServletResponse);
    }

    protected void doGet(HttpServletRequest httpServletRequest,
                         HttpServletResponse httpServletResponse)
            throws ServletException, IOException {
        doProcess(httpServletRequest, httpServletResponse);
    }

    protected void doProcess(HttpServletRequest httpServletRequest,
                             HttpServletResponse httpServletResponse)
            throws ServletException, IOException {
        try {
            String uploadFastaFilePath = httpServletRequest.getParameter("Upload Fasta File Node");
            String inputNodeForOrfCaller = httpServletRequest.getParameter("Start Metagenomic ORF Calling Pipeline");
            String inputNodeForAnnotation = httpServletRequest.getParameter("Start Metagenomic Annotation Pipeline");
            String inputNodeForCombined = httpServletRequest.getParameter("Start Metagenomic Combined Pipeline");
            String projectCode = httpServletRequest.getParameter("Project Code");
            ComputeBeanRemote computeBean = EJBFactory.getRemoteComputeBean();
            String submitType = httpServletRequest.getParameter("type");
            logger.info("Received submitType=" + submitType);
            if (submitType.equals(SUBMIT_NODE)) {
                String systemUser = "smurphy";
                File uploadFastaFile = new File(uploadFastaFilePath.trim());
                if (!uploadFastaFile.exists()) {
                    throw new ServletException("Could not locate file for upload=" + uploadFastaFilePath);
                }
                String fastaType = FastaUtil.determineSequenceType(uploadFastaFile);
                Long fastaFileNodeId;
                if (fastaType.equals(SequenceType.NUCLEOTIDE)) {
                    logger.info("Beginning creation of nucleotide fasta file");
                    fastaFileNodeId = createNucleotideFastaFileNode(systemUser, uploadFastaFile.getName(),
                            "File for metagenomic annotation", uploadFastaFilePath);
                    logger.info("End of creation of nucleotide fasta file node id=" + fastaFileNodeId);
                }
                else if (fastaType.equals(SequenceType.PEPTIDE)) {
                    logger.info("Beginning creation of peptide fasta file");
                    fastaFileNodeId = createPeptideFastaFileNode(systemUser, uploadFastaFile.getName(),
                            "File for metagenomic annotation", uploadFastaFilePath);
                    logger.info("End of creation of peptide fasta file node id=" + fastaFileNodeId);
                }
                else {
                    String msg = "Could not determine type of fasta file=" + uploadFastaFile.getAbsolutePath();
                    logger.error(msg);
                    throw new Exception(msg);
                }
                httpServletRequest.getSession().setAttribute(SESSION_INPUT_NODE_ID, fastaFileNodeId);
                httpServletResponse.sendRedirect("/compute/MetaGenoPipeStatus?status=node");
            }
            else if (submitType.equals(SUBMIT_ORF)) {
                MetaGenoOrfCallerTask orfCallerTask = new MetaGenoOrfCallerTask();
                orfCallerTask.setParameter(MetaGenoOrfCallerTask.PARAM_input_node_id, inputNodeForOrfCaller.trim());
                orfCallerTask.setOwner("smurphy");
                orfCallerTask.setParameter("project", projectCode);
                String clearRangeStr = httpServletRequest.getParameter("clear_range");
                Boolean useClearRange = false;
                if (clearRangeStr != null && clearRangeStr.equals("true"))
                    useClearRange = true;
                logger.info("useClearRange=" + useClearRange);
                orfCallerTask.setParameter(MetaGenoOrfCallerTask.PARAM_useClearRange, useClearRange.toString());
                orfCallerTask = (MetaGenoOrfCallerTask) computeBean.saveOrUpdateTask(orfCallerTask);
                long taskId = orfCallerTask.getObjectId();
                logger.info("Submitting job for OrfCallerTaskId=" + taskId);
                computeBean.submitJob("MetaGenoORFCaller", taskId);
                httpServletRequest.getSession().setAttribute(SESSION_MG_ORF_TASK_ID, taskId);
                httpServletResponse.sendRedirect("/compute/MetaGenoPipeStatus?status=orf");
            }
            else if (submitType.equals(SUBMIT_ANNO)) {
                MetaGenoAnnotationTask annoTask = new MetaGenoAnnotationTask();
                annoTask.setParameter(MetaGenoAnnotationTask.PARAM_input_node_id, inputNodeForAnnotation.trim());
                annoTask.setOwner("smurphy");
                annoTask.setParameter("project", projectCode.trim());
                annoTask = (MetaGenoAnnotationTask) computeBean.saveOrUpdateTask(annoTask);
                long taskId = annoTask.getObjectId();
                logger.info("Submitting job for AnnotationTaskId=" + taskId);
                computeBean.submitJob("MetaGenoAnnotation", taskId);
                httpServletRequest.getSession().setAttribute(SESSION_MG_ANNO_TASK_ID, taskId);
                httpServletResponse.sendRedirect("/compute/MetaGenoPipeStatus?status=anno");
            }
            else if (submitType.equals(SUBMIT_COMBINED)) {
                MetaGenoCombinedOrfAnnoTask combinedTask = new MetaGenoCombinedOrfAnnoTask();
                combinedTask.setParameter(MetaGenoCombinedOrfAnnoTask.PARAM_input_node_id, inputNodeForCombined.trim());
                combinedTask.setOwner("smurphy");
                combinedTask.setParameter("project", projectCode);
                String clearRangeStr = httpServletRequest.getParameter("combined_clear_range");
                Boolean useClearRange = false;
                if (clearRangeStr != null && clearRangeStr.equals("true"))
                    useClearRange = true;
                logger.info("useClearRange=" + useClearRange);
                combinedTask.setParameter(MetaGenoCombinedOrfAnnoTask.PARAM_useClearRange, useClearRange.toString());
                combinedTask = (MetaGenoCombinedOrfAnnoTask) computeBean.saveOrUpdateTask(combinedTask);
                long taskId = combinedTask.getObjectId();
                logger.info("Submitting job for CombinedOrfAnnoTaskId=" + taskId);
                computeBean.submitJob("MetaGenoCombinedOrfAnno", taskId);
                httpServletRequest.getSession().setAttribute(SESSION_MG_COMBINED_TASK_ID, taskId);
                httpServletResponse.sendRedirect("/compute/MetaGenoPipeStatus?status=combined");
            }
        }
        catch (Exception ex) {
            logger.error(ex, ex);
            ex.printStackTrace();
        }
    }

    protected Long createNucleotideFastaFileNode(String user, String name, String description, String sourcePath)
            throws Exception {
        if (logger.isInfoEnabled()) logger.info("Starting createFastaFileNode() with source path: " + sourcePath);
        File sourceFile = new File(sourcePath);
        long[] sequenceCountAndTotalLength = FastaUtil.findSequenceCountAndTotalLength(sourceFile);
        FastaFileNode ffn = new FastaFileNode(user, null/*Task*/, name, description,
                Node.VISIBILITY_PUBLIC, FastaFileNode.NUCLEOTIDE, (int) sequenceCountAndTotalLength[0], null);
        ffn.setLength(sequenceCountAndTotalLength[1]);
        ffn = (FastaFileNode) EJBFactory.getRemoteComputeBean().saveOrUpdateNode(ffn);
        File ffnDir = new File(ffn.getDirectoryPath());
        ffnDir.mkdirs();
        String copyCmd = "cp " + sourcePath + " " + ffn.getFastaFilePath();
        if (logger.isInfoEnabled()) logger.info("Executing: " + copyCmd);
        SystemCall call = new SystemCall(logger);
        int exitVal = call.emulateCommandLine(copyCmd, true);
        if (logger.isInfoEnabled()) logger.info("Exit value: " + exitVal);
        return ffn.getObjectId();
    }

    protected Long createPeptideFastaFileNode(String user, String name, String description, String sourcePath)
            throws Exception {
        if (logger.isInfoEnabled()) logger.info("Starting createFastaFileNode() with source path: " + sourcePath);
        File sourceFile = new File(sourcePath);
        long[] sequenceCountAndTotalLength = FastaUtil.findSequenceCountAndTotalLength(sourceFile);
        FastaFileNode ffn = new FastaFileNode(user, null/*Task*/, name, description,
                Node.VISIBILITY_PUBLIC, FastaFileNode.PEPTIDE, (int) sequenceCountAndTotalLength[0], null);
        ffn.setLength(sequenceCountAndTotalLength[1]);
        ffn = (FastaFileNode) EJBFactory.getRemoteComputeBean().saveOrUpdateNode(ffn);
        File ffnDir = new File(ffn.getDirectoryPath());
        ffnDir.mkdirs();
        String copyCmd = "cp " + sourcePath + " " + ffn.getFastaFilePath();
        if (logger.isInfoEnabled()) logger.info("Executing: " + copyCmd);
        SystemCall call = new SystemCall(logger);
        int exitVal = call.emulateCommandLine(copyCmd, true);
        if (logger.isInfoEnabled()) logger.info("Exit value: " + exitVal);
        return ffn.getObjectId();
    }


}
