
package org.janelia.it.jacs.compute.mbean;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Jan 3, 2008
 * Time: 9:28:32 AM
 */
public interface GenomeProjectNodeManagerMBean {

    public void reportEmptyGenomeProjectFileNodeDirs();
    public void parseGenbankFile(String pathToGenbankFile);
    public void howManyDataSetsHaveAnnotations();
    public void runGenomeProjectUpdate();
    public void updateGenomeProjectDataTypeAndSeqTypeValues() throws Exception;
    public void buildAllGenbankFileInfoList();
    public void buildFastaArchive();

}
