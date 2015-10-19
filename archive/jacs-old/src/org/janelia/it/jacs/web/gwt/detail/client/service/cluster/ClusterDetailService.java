
package org.janelia.it.jacs.web.gwt.detail.client.service.cluster;

import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.model.genomics.ClusterAnnotation;
import org.janelia.it.jacs.model.genomics.ProteinCluster;
import org.janelia.it.jacs.model.genomics.ProteinClusterAnnotationMember;
import org.janelia.it.jacs.model.genomics.ProteinClusterMember;
import org.janelia.it.jacs.web.gwt.common.client.service.GWTServiceException;
import org.janelia.it.jacs.web.gwt.detail.client.DetailService;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: cgoina
 * Date: Nov 15, 2007
 * Time: 10:14:14 PM
 */
public interface ClusterDetailService extends DetailService {
    ProteinCluster getProteinCluster(String clusterAcc)
            throws GWTServiceException;

    List<ClusterAnnotation> getClusterAnnotations(String clusterAcc, SortArgument[] sortArgs)
            throws GWTServiceException;

    List<ProteinCluster> getPagedCoreClustersFromFinalCluster(String finalClusterAcc,
                                                              int startIndex,
                                                              int numRows,
                                                              SortArgument[] sortArgs)
            throws GWTServiceException;

    List<ProteinClusterMember> getPagedNRSeqMembersFromCluster(String clusterAcc,
                                                               int startIndex,
                                                               int numRows,
                                                               SortArgument[] sortArgs)
            throws GWTServiceException;

    List<ProteinClusterMember> getPagedSeqMembersFromCluster(String clusterAcc,
                                                             int startIndex,
                                                             int numRows,
                                                             SortArgument[] sortArgs)
            throws GWTServiceException;

    int getNumMatchingRepsFromClusterWithAnnotation(String clusterAcc, String annotationId)
            throws GWTServiceException;

    List<ProteinClusterAnnotationMember> getPagedMatchingRepsFromClusterWithAnnotation(String clusterAcc,
                                                                                       String annotationId,
                                                                                       int startIndex,
                                                                                       int numRows,
                                                                                       SortArgument[] sortArgs)
            throws GWTServiceException;
}
