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

package org.janelia.it.jacs.server.api;

import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.model.genomics.*;
import org.janelia.it.jacs.server.access.FeatureDAO;
import org.janelia.it.jacs.server.access.hibernate.DaoException;
import org.springframework.dao.DataAccessException;

import java.util.List;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Oct 10, 2006
 * Time: 3:40:29 PM
 */
public class FeatureAPI {

    private FeatureDAO featureDAO;

    public void setFeatureDAO(FeatureDAO featureDAO) {
        this.featureDAO = featureDAO;
    }

    public Chromosome[] getAllMicrobialChromosomes() throws DataAccessException, DaoException {
        List chrList = featureDAO.getAllMicrobialChromosomes();
        Chromosome[] chromosomes = new Chromosome[chrList.size()];
        return (Chromosome[]) chrList.toArray(chromosomes);
    }

    public ProteinCluster getProteinCluster(String clusterAcc) throws Exception {
        ProteinCluster proteinCluster = null;
        if (clusterAcc == null) {
            throw new IllegalArgumentException("Invalid cluster accession");
        }
        if (clusterAcc.startsWith("CAM_CRCL_")) {
            proteinCluster = featureDAO.getProteinCoreCluster(clusterAcc);
        }
        else if (clusterAcc.startsWith("CAM_CL_")) {
            proteinCluster = featureDAO.getProteinFinalCluster(clusterAcc);
        }
        else {
            throw new IllegalArgumentException("Unrecognized cluster accession: " + "'" + clusterAcc + "'");
        }
        return proteinCluster;
    }

    public List<ClusterAnnotation> getClusterAnnotations(String clusterAcc, SortArgument[] sortArgs) throws Exception {
        List<ClusterAnnotation> clusterAnnotations = null;
        if (clusterAcc == null) {
            throw new IllegalArgumentException("Invalid cluster accession");
        }
        if (clusterAcc.startsWith("CAM_CRCL_")) {
            clusterAnnotations = featureDAO.getCoreClusterAnnotations(clusterAcc, sortArgs);
        }
        else if (clusterAcc.startsWith("CAM_CL_")) {
            clusterAnnotations = featureDAO.getFinalClusterAnnotations(clusterAcc, sortArgs);
        }
        else {
            throw new IllegalArgumentException("Unrecognized cluster accession: " + "'" + clusterAcc + "'");
        }
        return clusterAnnotations;
    }

    public List<ProteinCluster> getPagedCoreClustersFromFinalCluster(String finalClusterAcc,
                                                                     Set<String> clusterMemberAccs,
                                                                     int startIndex,
                                                                     int numRows,
                                                                     SortArgument[] sortArgs)
            throws Exception {
        if (finalClusterAcc == null) {
            throw new IllegalArgumentException("Invalid cluster accession");
        }
        return featureDAO.getPagedCoreClustersFromFinalCluster(finalClusterAcc, clusterMemberAccs, startIndex, numRows, sortArgs);
    }

    public List<ProteinClusterMember> getPagedNRSeqMembersFromCluster(String clusterAcc,
                                                                      Set<String> clusterMemberAccs,
                                                                      int startIndex,
                                                                      int numRows,
                                                                      SortArgument[] sortArgs)
            throws Exception {
        if (clusterAcc == null) {
            throw new IllegalArgumentException("Invalid cluster accession");
        }
        List<ProteinClusterMember> clusterMembers = null;
        if (clusterAcc.startsWith("CAM_CRCL_")) {
            clusterMembers = featureDAO.getPagedNRSeqMembersFromCoreCluster(clusterAcc, clusterMemberAccs, startIndex, numRows, sortArgs);
        }
        else if (clusterAcc.startsWith("CAM_CL_")) {
            clusterMembers = featureDAO.getPagedNRSeqMembersFromFinalCluster(clusterAcc, clusterMemberAccs, startIndex, numRows, sortArgs);
        }
        else {
            throw new IllegalArgumentException("Unrecognized cluster accession: " + "'" + clusterAcc + "'");
        }
        return clusterMembers;
    }

    public List<ProteinClusterMember> getPagedSeqMembersFromCluster(String clusterAcc,
                                                                    Set<String> clusterMemberAccs,
                                                                    int startIndex,
                                                                    int numRows,
                                                                    SortArgument[] sortArgs)
            throws Exception {
        if (clusterAcc == null) {
            throw new IllegalArgumentException("Invalid cluster accession");
        }
        List<ProteinClusterMember> clusterMembers = null;
        if (clusterAcc.startsWith("CAM_CRCL_")) {
            clusterMembers = featureDAO.getPagedSeqMembersFromCoreCluster(clusterAcc, clusterMemberAccs, startIndex, numRows, sortArgs);
        }
        else if (clusterAcc.startsWith("CAM_CL_")) {
            clusterMembers = featureDAO.getPagedSeqMembersFromFinalCluster(clusterAcc, clusterMemberAccs, startIndex, numRows, sortArgs);
        }
        else {
            throw new IllegalArgumentException("Unrecognized cluster accession: " + "'" + clusterAcc + "'");
        }
        return clusterMembers;
    }

    public int getNumMatchingRepsFromClusterWithAnnotation(String clusterAcc, String annotationId)
            throws Exception {
        int nMatches = -1;
        if (clusterAcc == null) {
            throw new IllegalArgumentException("Invalid cluster accession");
        }
        if (annotationId == null) {
            throw new IllegalArgumentException("Invalid annotation ID");
        }
        if (clusterAcc.startsWith("CAM_CRCL_")) {
            nMatches = featureDAO.getNumMatchingRepsFromCoreClusterWithAnnotation(clusterAcc, annotationId);
        }
        else if (clusterAcc.startsWith("CAM_CL_")) {
            nMatches = featureDAO.getNumMatchingRepsFromFinalClusterWithAnnotation(clusterAcc, annotationId);
        }
        else {
            throw new IllegalArgumentException("Unrecognized cluster accession: " + "'" + clusterAcc + "'");
        }
        return nMatches;
    }

    public List<ProteinClusterAnnotationMember> getPagedMatchingRepsFromClusterWithAnnotation(String clusterAcc,
                                                                                              String annotationId,
                                                                                              Set<String> clusterMembersAccs,
                                                                                              int startIndex,
                                                                                              int numRows,
                                                                                              SortArgument[] sortArgs)
            throws DaoException {
        List<ProteinClusterAnnotationMember> clusterMembers = null;
        if (clusterAcc == null) {
            throw new IllegalArgumentException("Invalid cluster accession");
        }
        if (annotationId == null) {
            throw new IllegalArgumentException("Invalid annotation ID");
        }
        if (clusterAcc.startsWith("CAM_CRCL_")) {
            clusterMembers = featureDAO.getPagedMatchingRepsFromCoreClusterWithAnnotation(clusterAcc,
                    annotationId,
                    clusterMembersAccs,
                    startIndex,
                    numRows,
                    sortArgs);
        }
        else if (clusterAcc.startsWith("CAM_CL_")) {
            clusterMembers = featureDAO.getPagedMatchingRepsFromFinalClusterWithAnnotation(clusterAcc,
                    annotationId,
                    clusterMembersAccs,
                    startIndex,
                    numRows,
                    sortArgs);
        }
        else {
            throw new IllegalArgumentException("Unrecognized cluster accession: " + "'" + clusterAcc + "'");
        }
        return clusterMembers;
    }

}
