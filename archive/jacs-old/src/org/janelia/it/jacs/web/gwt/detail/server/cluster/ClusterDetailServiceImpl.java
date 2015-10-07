
package org.janelia.it.jacs.web.gwt.detail.server.cluster;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.model.genomics.ClusterAnnotation;
import org.janelia.it.jacs.model.genomics.ProteinCluster;
import org.janelia.it.jacs.model.genomics.ProteinClusterAnnotationMember;
import org.janelia.it.jacs.model.genomics.ProteinClusterMember;
import org.janelia.it.jacs.server.api.FeatureAPI;
import org.janelia.it.jacs.web.gwt.common.client.service.GWTServiceException;
import org.janelia.it.jacs.web.gwt.common.server.JcviGWTSpringController;
import org.janelia.it.jacs.web.gwt.detail.client.service.cluster.ClusterDetailService;

import java.io.Serializable;
import java.util.List;

/**
 * This class is used by GWT client to retrieve cluster info needed for detail UI
 *
 * @author Cristian Goina
 */
public class ClusterDetailServiceImpl extends JcviGWTSpringController implements ClusterDetailService {
    private static Logger logger = Logger.getLogger(ClusterDetailServiceImpl.class);

    private FeatureAPI featureAPI;

    public void setFeatureAPI(FeatureAPI featureAPI) {
        this.featureAPI = featureAPI;
    }

    /**
     * Returns a GWT-consumable BaseSequenceEntity instance given a BaseSequenceEntity accession
     *
     * @param accession the camera accession
     * @return BaseSequenceEntity instance
     */
    public Serializable getEntity(String accession) {
        ProteinCluster proteinCluster;
        try {
            proteinCluster = getProteinCluster(accession);
        }
        catch (Throwable e) {
            logger.error("Unexpected exception in ClusterDetailServiceImpl.getEntity: ", e);
            throw new RuntimeException(e);
        }
        return proteinCluster;
    }

    public ProteinCluster getProteinCluster(String clusterAcc) throws GWTServiceException {
        ProteinCluster proteinCluster;
        try {
            logger.debug("getting protein cluster with accession=" + clusterAcc);
            proteinCluster = featureAPI.getProteinCluster(clusterAcc);
            if (proteinCluster != null) {
                cleanForGWT(proteinCluster);
            }
        }
        catch (Throwable e) {
            logger.error("ClusterDetailServiceImpl.getProteinCoreCluster error: ", e);
            throw new GWTServiceException(e);
        }
        return proteinCluster;
    }

    public List<ClusterAnnotation> getClusterAnnotations(String clusterAcc, SortArgument[] sortArgs) throws GWTServiceException {
        List<ClusterAnnotation> clusterAnnotations;
        try {
            logger.debug("getting annotations for cluster with accession=" + clusterAcc);
            clusterAnnotations = featureAPI.getClusterAnnotations(clusterAcc, sortArgs);
            if (clusterAnnotations != null) {
                cleanForGWT(clusterAnnotations);
            }
        }
        catch (Throwable e) {
            logger.error("ClusterDetailServiceImpl.getClusterAnnotations error: ", e);
            throw new GWTServiceException(e);
        }
        return clusterAnnotations;
    }

    public List<ProteinCluster> getPagedCoreClustersFromFinalCluster(String finalClusterAcc,
                                                                     int startIndex,
                                                                     int numRows,
                                                                     SortArgument[] sortArgs)
            throws GWTServiceException {
        List<ProteinCluster> memberClusters;
        try {
            logger.debug("getting core cluster from final cluster with accession=" + finalClusterAcc);
            memberClusters = featureAPI.getPagedCoreClustersFromFinalCluster(finalClusterAcc,
                    null, startIndex,
                    numRows,
                    sortArgs);
            if (memberClusters != null) {
                cleanForGWT(memberClusters);
            }
        }
        catch (Throwable e) {
            logger.error("ClusterDetailServiceImpl.getPagedCoreClustersFromFinalCluster error: ", e);
            throw new GWTServiceException(e);
        }
        return memberClusters;
    }

    public List<ProteinClusterMember> getPagedNRSeqMembersFromCluster(String clusterAcc,
                                                                      int startIndex,
                                                                      int numRows,
                                                                      SortArgument[] sortArgs)
            throws GWTServiceException {
        List<ProteinClusterMember> memberSequences;
        try {
            logger.debug("getting non redundant sequence members from core cluster with accession=" + clusterAcc);
            memberSequences = featureAPI.getPagedNRSeqMembersFromCluster(clusterAcc,
                    null, startIndex,
                    numRows,
                    sortArgs);
            if (memberSequences != null) {
                cleanForGWT(memberSequences);
            }
        }
        catch (Throwable e) {
            logger.error("ClusterDetailServiceImpl.getPagedNRSeqMembersFromCluster error: ", e);
            throw new GWTServiceException(e);
        }
        return memberSequences;
    }

    public List<ProteinClusterMember> getPagedSeqMembersFromCluster(String clusterAcc,
                                                                    int startIndex,
                                                                    int numRows,
                                                                    SortArgument[] sortArgs)
            throws GWTServiceException {
        List<ProteinClusterMember> memberSequences;
        try {
            logger.debug("getting sequence members from core cluster with accession=" + clusterAcc);
            memberSequences = featureAPI.getPagedSeqMembersFromCluster(clusterAcc,
                    null, startIndex,
                    numRows,
                    sortArgs);
            if (memberSequences != null) {
                cleanForGWT(memberSequences);
            }
        }
        catch (Throwable e) {
            logger.error("ClusterDetailServiceImpl.getPagedSeqMembersFromCluster error: ", e);
            throw new GWTServiceException(e);
        }
        return memberSequences;
    }

    public int getNumMatchingRepsFromClusterWithAnnotation(String clusterAcc, String annotationId) throws GWTServiceException {
        int numberOfMatchingSequences;
        try {
            logger.debug("getting number of sequences that match the annotation " + annotationId +
                    " from  members from core cluster with accession=" + clusterAcc);
            numberOfMatchingSequences = featureAPI.getNumMatchingRepsFromClusterWithAnnotation(clusterAcc,
                    annotationId);
        }
        catch (Throwable e) {
            logger.error("ClusterDetailServiceImpl.getNumMatchingRepsFromClusterWithAnnotation error: ", e);
            throw new GWTServiceException(e);
        }
        return numberOfMatchingSequences;
    }

    public List<ProteinClusterAnnotationMember> getPagedMatchingRepsFromClusterWithAnnotation(String clusterAcc,
                                                                                              String annotationId,
                                                                                              int startIndex,
                                                                                              int numRows,
                                                                                              SortArgument[] sortArgs)
            throws GWTServiceException {
        List<ProteinClusterAnnotationMember> memberSequences;
        try {
            logger.debug("getting number of sequences that match the annotation " + annotationId +
                    " from  members from core cluster with accession=" + clusterAcc);
            memberSequences = featureAPI.getPagedMatchingRepsFromClusterWithAnnotation(clusterAcc,
                    annotationId,
                    null, startIndex,
                    numRows,
                    sortArgs);
            if (memberSequences != null) {
                cleanForGWT(memberSequences);
            }
        }
        catch (Throwable e) {
            logger.error("ClusterDetailServiceImpl.getPagedMatchingRepsFromClusterWithAnnotation error: ", e);
            throw new GWTServiceException(e);
        }
        return memberSequences;
    }

}
