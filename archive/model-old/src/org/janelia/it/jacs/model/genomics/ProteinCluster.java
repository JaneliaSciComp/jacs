
package org.janelia.it.jacs.model.genomics;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * User: cgoina
 * Date: Nov 28, 2007
 * Time: 10:14:14 PM
 */
public class ProteinCluster implements Serializable, IsSerializable {
    private String clusterAcc;
    private String parentClusterAcc;
    private String clusterQuality;
    private String longestProteinMemberAcc;
    private Integer numClusterMembers;
    private Integer numProteins;
    private Integer numNonRedundantProteins;

    public ProteinCluster() {
    }

    public String getClusterAcc() {
        return clusterAcc;
    }

    public void setClusterAcc(String clusterAcc) {
        this.clusterAcc = clusterAcc;
    }

    public String getParentClusterAcc() {
        return parentClusterAcc;
    }

    public void setParentClusterAcc(String parentClusterAcc) {
        this.parentClusterAcc = parentClusterAcc;
    }

    public String getClusterQuality() {
        return clusterQuality;
    }

    public void setClusterQuality(String clusterQuality) {
        this.clusterQuality = clusterQuality;
    }

    public String getLongestProteinMemberAcc() {
        return longestProteinMemberAcc;
    }

    public void setLongestProteinMemberAcc(String longestProteinMemberAcc) {
        this.longestProteinMemberAcc = longestProteinMemberAcc;
    }

    public Integer getNumClusterMembers() {
        return numClusterMembers;
    }

    public void setNumClusterMembers(Integer numClusterMembers) {
        this.numClusterMembers = numClusterMembers;
    }

    public Integer getNumProteins() {
        return numProteins;
    }

    public void setNumProteins(Integer numProteins) {
        this.numProteins = numProteins;
    }

    public Integer getNumNonRedundantProteins() {
        return numNonRedundantProteins;
    }

    public void setNumNonRedundantProteins(Integer numNonRedundantProteins) {
        this.numNonRedundantProteins = numNonRedundantProteins;
    }

}
