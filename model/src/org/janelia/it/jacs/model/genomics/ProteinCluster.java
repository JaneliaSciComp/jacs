/*
 * Copyright (c) 2010-2011, J. Craig Venter Institute, Inc.
 *
 * This file is part of JCVI VICS.
 *
 * JCVI VICS is free software; you can redistribute it and/or modify it
 * under the terms and conditions of the Artistic License 2.0.  For
 * details, see the full text of the license in the file LICENSE.txt.  No
 * other rights are granted.  Any and all third party software rights to
 * remain with the original developer.
 *
 * JCVI VICS is distributed in the hope that it will be useful in
 * bioinformatics applications, but it is provided "AS IS" and WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTIES including but not limited to
 * implied warranties of merchantability or fitness for any particular
 * purpose.  For details, see the full text of the license in the file
 * LICENSE.txt.
 *
 * You should have received a copy of the Artistic License 2.0 along with
 * JCVI VICS.  If not, the license can be obtained from
 * "http://www.perlfoundation.org/artistic_license_2_0."
 */

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
