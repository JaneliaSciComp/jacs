
package org.janelia.it.jacs.server.api;

import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.model.genomics.ClusterAnnotation;
import org.janelia.it.jacs.model.genomics.ProteinCluster;
import org.janelia.it.jacs.model.genomics.ProteinClusterAnnotationMember;
import org.janelia.it.jacs.model.genomics.ProteinClusterMember;
import org.janelia.it.jacs.test.JacswebTestCase;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: tdolafi
 * Date: Aug 4, 2006
 * Time: 11:25:09 AM
 *
 */
public class FeatureAPITest extends JacswebTestCase {
    private FeatureAPI featureAPI;

    public FeatureAPITest() {
        super(FeatureAPITest.class.getName());
        setAutowireMode(AUTOWIRE_BY_NAME);
    }

    public FeatureAPI getFeatureAPI() {
        return featureAPI;
    }

    public void setFeatureAPI(FeatureAPI featureAPI) {
        this.featureAPI = featureAPI;
    }

    public void testGetPagedMatchingRepsFromClusterWithAnnotation() {
        try {
            String clusterAcc = "CAM_CRCL_841";
            String annotationID = "GO:0006400";
            SortArgument[] sortArgs = new SortArgument[] {
                    new SortArgument("protein_acc", SortArgument.SORT_ASC),
                    new SortArgument("core_cluster_acc", SortArgument.SORT_ASC),
                    new SortArgument("final_cluster_acc", SortArgument.SORT_ASC),
                    new SortArgument("nr_parent_acc", SortArgument.SORT_ASC),
                    new SortArgument("cluster_quality", SortArgument.SORT_ASC),
                    new SortArgument("defline", SortArgument.SORT_ASC),
                    new SortArgument("evidence", SortArgument.SORT_ASC)
            };
            ProteinCluster cluster = featureAPI.getProteinCluster(clusterAcc);
            int currentOffset = 0;
            int pageLength = 100;
            int nRecords = 0;
            for(;;) {
                List<ProteinClusterAnnotationMember> coreClusterMembers = featureAPI.getPagedMatchingRepsFromClusterWithAnnotation(clusterAcc,
                        annotationID,
                        null, currentOffset,
                        pageLength,
                        sortArgs);
                nRecords += coreClusterMembers.size();
                if(coreClusterMembers.size() < pageLength) {
                    break;
                }
                currentOffset += coreClusterMembers.size();
            }
            List<ClusterAnnotation> annotations = featureAPI.getClusterAnnotations(clusterAcc,null);
            for(ClusterAnnotation annotation : annotations) {
                if(annotation.getAnnotationID().equals(annotationID)) {
                    assertTrue(Math.abs(nRecords/cluster.getNumNonRedundantProteins().doubleValue()*100 -
                            annotation.getEvidencePct().doubleValue()) < 0.01);
                    break;
                }
            }
            assertTrue(nRecords ==
                    featureAPI.getNumMatchingRepsFromClusterWithAnnotation(clusterAcc,annotationID));
        } catch (Exception ex) {
            String message = "Exception: " + ex.getMessage();
            logger.warn(message,ex);
            fail(message);
        }
    }

    public void testGetPagedNRSeqMembersFromCluster() {
        testGetPagedNRSeqMembersFromCluster("CAM_CRCL_841");
        testGetPagedNRSeqMembersFromCluster("CAM_CL_10");
    }

    public void testGetPagedSeqMembersFromCluster() {
        testGetPagedSeqMembersFromCluster("CAM_CRCL_841");
        testGetPagedSeqMembersFromCluster("CAM_CL_10");
    }

    private void testGetPagedNRSeqMembersFromCluster(String clusterAcc) {
        try {
            SortArgument[] sortArgs = new SortArgument[] {
                    new SortArgument("protein_acc", SortArgument.SORT_ASC),
                    new SortArgument("core_cluster_acc", SortArgument.SORT_ASC),
                    new SortArgument("final_cluster_acc", SortArgument.SORT_ASC),
                    new SortArgument("cluster_quality", SortArgument.SORT_ASC),
                    new SortArgument("defline", SortArgument.SORT_ASC)
            };
            ProteinCluster proteinCluster = featureAPI.getProteinCluster(clusterAcc);
            int currentOffset = 0;
            int pageLength = 100;
            int nRecords = 0;
            for(;;) {
                List<ProteinClusterMember> clusterMembers = featureAPI.getPagedNRSeqMembersFromCluster(clusterAcc,
                        null, currentOffset,
                        pageLength,
                        sortArgs);
                nRecords += clusterMembers.size();
                if(clusterMembers.size() < pageLength) {
                    break;
                }
                currentOffset += clusterMembers.size();
            }
            // for now we assert that cluster.numProteins >= selected records because
            // some of the proteins are missing
            assertTrue(proteinCluster.getNumNonRedundantProteins() >= nRecords);
        } catch (Exception ex) {
            String message = "Exception: " + ex.getMessage();
            logger.warn(message,ex);
            fail(message);
        }
    }

    private void testGetPagedSeqMembersFromCluster(String clusterAcc) {
        try {
            SortArgument[] sortArgs = new SortArgument[] {
                    new SortArgument("protein_acc", SortArgument.SORT_ASC),
                    new SortArgument("core_cluster_acc", SortArgument.SORT_ASC),
                    new SortArgument("final_cluster_acc", SortArgument.SORT_ASC),
                    new SortArgument("nr_parent_acc", SortArgument.SORT_ASC),
                    new SortArgument("cluster_quality", SortArgument.SORT_ASC),
                    new SortArgument("defline", SortArgument.SORT_ASC)
            };
            ProteinCluster proteinCluster = featureAPI.getProteinCluster(clusterAcc);
            int currentOffset = 0;
            int pageLength = 100;
            int nRecords = 0;
            for(;;) {
                List<ProteinClusterMember> clusterMembers = featureAPI.getPagedSeqMembersFromCluster(clusterAcc,
                        null, currentOffset,
                        pageLength,
                        sortArgs);
                nRecords += clusterMembers.size();
                if(clusterMembers.size() < pageLength) {
                    break;
                }
                currentOffset += clusterMembers.size();
            }
            // for now we assert that cluster.numProteins >= selected records because
            // some of the proteins are missing
            assertTrue(proteinCluster.getNumProteins() >= nRecords);
        } catch (Exception ex) {
            String message = "Exception: " + ex.getMessage();
            logger.warn(message,ex);
            fail(message);
        }
    }

}
