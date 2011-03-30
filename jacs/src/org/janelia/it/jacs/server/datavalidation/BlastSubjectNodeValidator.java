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

package org.janelia.it.jacs.server.datavalidation;

import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.blast.BlastDatabaseFileNode;
import org.janelia.it.jacs.model.user_data.blast.BlastDatasetNode;
import org.janelia.it.jacs.server.access.NodeDAO;

import java.io.File;
import java.io.FilenameFilter;
import java.util.List;
import java.util.Set;

/**
 * Test program for validating blastable subject nodes
 */
public class BlastSubjectNodeValidator {
    public static int UNKNOWN = -1;
    public static int VALIDSUBJECTNODE = 0;
    public static int INVALIDNODETYPE = 1;
    public static int DATAPATHNOTSET = 2;
    public static int DATADIRNOTFOUND = 3;
    public static int NODEVSFILESTOREPARTITIONMISSMATCH = 4;
    public static int NOPARITIONFILEFOUND = 5;

    private NodeDAO nodeDAO;
    private ValidationReportBuilder reportBuilder;

    public BlastSubjectNodeValidator() {
    }

    public void setNodeDAO(NodeDAO nodeDAO) {
        this.nodeDAO = nodeDAO;
    }

    public ValidationReportBuilder getReportBuilder() {
        return reportBuilder;
    }

    public void setReportBuilder(ValidationReportBuilder reportBuilder) {
        this.reportBuilder = reportBuilder;
    }

    public int validatePublicSubjectNodes(boolean generateReport) {
        return validateSubjectNodesForUsers(null, generateReport);
    }

    public int validateSubjectNodesForUsers(String[] userList, boolean generateReport) {
        ValidationReport report = null;
        if (generateReport) {
            if (reportBuilder != null) {
                report = reportBuilder.createaValidationReport();
            }
            else {
                throw new IllegalArgumentException("Validation report requested but no reportBuilder provided");
            }
        }
        int validationResult = validateSubjectNodesForUsers(userList, report);
        if (generateReport) {
            reportBuilder.printReport(report);
        }
        return validationResult;
    }

    private int validateDatasetSubjectNode(BlastDatasetNode blastSubjectNode, ValidationReport report) {
        int result = UNKNOWN;
        if (report != null) {
            reportBuilder.startObjectValidation(blastSubjectNode, report);
        }
        Set subjectFileNodes = blastSubjectNode.getBlastDatabaseFileNodes();
        // validate all file nodes that are part of this data set
        for (Object subjectFileNodeObject : subjectFileNodes) {
            BlastDatabaseFileNode subjectFileNode = (BlastDatabaseFileNode) subjectFileNodeObject;
            int tmpResult = validateFileSubjectNode(subjectFileNode, report);
            if (tmpResult != VALIDSUBJECTNODE) {
                result = tmpResult;
            }
        }
        if (result == UNKNOWN) {
            result = VALIDSUBJECTNODE;
        }
        if (report != null) {
            reportBuilder.endObjectValidation(blastSubjectNode, report, result, null);
        }
        return result;
    }

    private int validateFileSubjectNode(BlastDatabaseFileNode blastSubjectNode, ValidationReport report) {
        int result = UNKNOWN;
        if (report != null) {
            reportBuilder.startObjectValidation(blastSubjectNode, report);
        }
        String dataPath = blastSubjectNode.getDirectoryPath();
        if (dataPath == null || dataPath.length() == 0) {
            result = DATAPATHNOTSET;
            if (report != null) {
                reportBuilder.endObjectValidation(blastSubjectNode, report, result,
                        "Undefined data path");
            }
            return result;
        }
        // check if the directory exists for this node
        File dataDir = new File(dataPath);
        if (!dataDir.exists()) {
            result = DATADIRNOTFOUND;
            if (report != null) {
                reportBuilder.endObjectValidation(blastSubjectNode, report, result,
                        "Data path does not exist on the filestore: " + "'" + dataPath + "'");
            }
            return result;
        }
        // count the fasta file from the directory and their number
        // should match the node's partition count
        File[] partitionFiles = dataDir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                if (name.endsWith(".fasta")) {
                    return true;
                }
                else {
                    return false;
                }
            }
        });
        int partitionCount = 0;
        if (blastSubjectNode.getPartitionCount() != null) {
            partitionCount = blastSubjectNode.getPartitionCount();
        }
        if (partitionCount > 0) {
            if (partitionFiles == null || partitionFiles.length == 0) {
                result = NOPARITIONFILEFOUND;
                if (report != null) {
                    reportBuilder.endObjectValidation(blastSubjectNode, report, result,
                            "No partition found on the filestore");
                }
                return result;
            }
            else if (partitionFiles.length != partitionCount) {
                result = NODEVSFILESTOREPARTITIONMISSMATCH;
                if (report != null) {
                    reportBuilder.endObjectValidation(blastSubjectNode, report, result,
                            "The number of node partitions does not match the filestore. " +
                                    "The node has " + String.valueOf(partitionCount) + " paritions and the filestore " +
                                    "contains " + String.valueOf(partitionFiles.length) + " partitions");
                }
                return result;
            }
        }
        else {
            if (partitionFiles != null && partitionFiles.length > 0) {
                result = NODEVSFILESTOREPARTITIONMISSMATCH;
                if (report != null) {
                    reportBuilder.endObjectValidation(blastSubjectNode, report, result,
                            "The number of node partitions does not match the filestore. " +
                                    "The node has no partition and the filestore " +
                                    "contains " + String.valueOf(partitionFiles.length) + " partitions");
                }
                return result;
            }
        }
        result = VALIDSUBJECTNODE;
        if (report != null) {
            reportBuilder.endObjectValidation(blastSubjectNode, report, result, null);
        }
        return result;
    }

    private int validateSubjectNodesForUsers(String[] userList, ValidationReport report) {
        int result = UNKNOWN;
        try {
            int nSubjectNodes = nodeDAO.getNumBlastableSubjectNodes(userList);
            int pageLength = 20;
            int nIndex = 0;
            for (; nIndex < nSubjectNodes;) {
                List<Node> blastSubjectNodes = nodeDAO.getPagedBlastableSubjectNodes(userList, nIndex, pageLength);
                if (blastSubjectNodes == null || blastSubjectNodes.size() == 0) {
                    break;
                }
                for (Node blastSubjectNode : blastSubjectNodes) {
                    validateSubjectNode(blastSubjectNode, report);
                }
                nIndex += blastSubjectNodes.size();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    private int validateSubjectNode(Node blastSubjectNode, ValidationReport report) {
        int result = UNKNOWN;
        if (blastSubjectNode instanceof BlastDatabaseFileNode) {
            result = validateFileSubjectNode((BlastDatabaseFileNode) blastSubjectNode, report);
        }
        else if (blastSubjectNode instanceof BlastDatasetNode) {
            result = validateDatasetSubjectNode((BlastDatasetNode) blastSubjectNode, report);
        }
        else {
            result = INVALIDNODETYPE;
        }
        return result;
    }

}
