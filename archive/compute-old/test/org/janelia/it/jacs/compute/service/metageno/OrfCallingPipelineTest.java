
package org.janelia.it.jacs.compute.service.metageno;

import junit.framework.TestCase;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.api.ComputeBeanRemote;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.api.TaskServiceProperties;
import org.janelia.it.jacs.model.tasks.metageno.MetaGenoOrfCallerTask;

import java.io.Writer;
import java.rmi.RemoteException;

/**
 * Created by IntelliJ IDEA.
 * User: jgoll
 * Date: Apr 1, 2009
 * Time: 3:28:53 PM
 *
 */
public class OrfCallingPipelineTest extends TestCase {
    private long startTestTime;
    protected TaskServiceProperties blastProperties;
    protected ComputeBeanRemote computeBean;
    private Writer perfNumsWriter;
    private static String perfFileName;


    //runs only during class instantiation
    public OrfCallingPipelineTest() {

        // example taken from the BlastTestBase class

        //get compute bean
        startTestTime   = System.currentTimeMillis();
        //blastProperties = new TaskServiceProperties(FileUtil.getResourceAsStream(null));
        computeBean     = EJBFactory.getRemoteComputeBean();
        System.out.println(computeBean.toString());
        //init orf caller - submit job
        MetaGenoOrfCallerTask orfCallerTask = new MetaGenoOrfCallerTask();


       // orfCallerTask.setParameter(MetaGenoOrfCallerTask.PARAM_input_node_id, inputNodeForOrfCaller.trim());
        orfCallerTask.setOwner("smurphy");
       // orfCallerTask.setParameter("project", projectCode);

//        String clearRangeStr = httpServletRequest.getParameter("clear_range");
//        Boolean useClearRange = false;
//        if (clearRangeStr != null && clearRangeStr.equals("true"))
//            useClearRange = true;

        //logger.info("useClearRange=" + useClearRange);
        //orfCallerTask.setParameter(MetaGenoOrfCallerTask.PARAM_useClearRange, useClearRange.toString());

        long taskId = orfCallerTask.getObjectId();
       // logger.info("Submitting job for OrfCallerTaskId=" + taskId);
        try {
            orfCallerTask = (MetaGenoOrfCallerTask) computeBean.saveOrUpdateTask(orfCallerTask);
            computeBean.submitJob("MetaGenoORFCaller", taskId);
        } catch (RemoteException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (DaoException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        //httpServletRequest.getSession().setAttribute(SESSION_MG_ORF_TASK_ID, taskId);
        //httpServletResponse.sendRedirect("/compute/MetaGenoPipeStatus?status=orf");


       // blastProperties.put(BlastRunner.PARAM_USER_ID_KEY, null);
//
//        try {
//            perfNumsWriter = new FileWriter(FileUtil.ensureFileExists(perfFileName),true);
//        } catch (IOException e) {
//            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
//        }
    }


//    public void setUp() {
//
//
//    }

    public void testRrnas() {
        assertTrue(1 == 1);
    }

    public void testTrnas() {
        String fileOne = "/local/camera_grid/system/MetaGenoOrfCallerResult/1322374935665967479/camera_extract_trna.combined.fasta";
        String fileTwo = "/usr/local/annotation/METAGENOMIC/results/YLAKE/FPBMLOC01_orf_results/camera_extract_trna/13520_default/camera_extract_trna.fsa.combined.out";

        SequenceComparator comp = SequenceComparator.getInstance();
        assertTrue(comp.compare(fileOne, fileTwo));

    }

    public void testPeptides() {
        assertTrue(1 == 1);
    }

    public void tearDown() {
        //free resources
    }
}
