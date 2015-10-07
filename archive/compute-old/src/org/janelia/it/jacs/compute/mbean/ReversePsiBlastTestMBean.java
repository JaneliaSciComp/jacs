
package org.janelia.it.jacs.compute.mbean;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Nov 7, 2008
 * Time: 11:21:05 AM
 */
public interface ReversePsiBlastTestMBean {

    public void start();

    public void stop();

    /*
    public void createFastaNode_seqpath_usr_name_desc_type(
            String seqPath, String username, String name, String description, String sequenceType);
    */

    public void submitReversePsiBlastSmallSingleTest();

    // public void submitReversePsiBlastTestByQueryNodeId(long queryNodeId);

    // public void createReversePsiDatabaseNode(String databaseFilePath, String name, String description, int numberOfHmms);

}