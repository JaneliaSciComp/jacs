
package org.janelia.it.jacs.compute.mbean;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Nov 15, 2006
 * Time: 1:04:46 PM
 */
public interface FileNodeManagerMBean {

    public void start();

    public void stop();

    public void createNucleotideFastaFileNode(String name, String description, String path);

    public void createPeptideFastaFileNode(String name, String description, String path);

    public void createNucleotideBlastableDatabase(String name, String description, String path);

    public void createPeptideBlastableDatabase(String name, String description, String path);

    public void createEmptyBlastableDatabase_name_desc_type_length_pnum(String csvString);

    public void createFastaFileNode_name_desc_path_type(String csvString);

    public void createBlastableDatabase_name_desc_path_type(String csvString);

    public void createMultipleFastaFileNode_name_desc_path_type(String csvString);

    public void createMultipleBlastableDatabase_name_desc_path_type(String csvString);

    public void gzipFileUtility(String pathToCompress);

    public void createBlastDatabaseFileNodeMovePartitionFiles(String name, String description, String sourcePath,
                                                              boolean isNucleotide, Integer partitionCount, Long length);

    public void findDeprecatedBlastDBDirs();

}
