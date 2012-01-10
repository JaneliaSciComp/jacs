
package org.janelia.it.jacs.compute.mbean;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Nov 7, 2008
 * Time: 11:21:05 AM
 */
public interface HmmerTestMBean {

    public void start();

    public void stop();

    public void createFastaNode_seqpath_usr_name_desc_type(
            String seqPath, String username, String name, String description, String sequenceType);

    public void submitHmmpfamSmallSingleTest();

    public void submitHmmpfamTIGRFAMTestByQueryNodeId(long queryNodeId);

    public void createHmmpfamDatabaseNode(String databaseFilePath, String name, String description, int numberOfHmms);

}
