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

package org.janelia.it.jacs.model.user_data;

import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.tasks.Task;

import java.io.File;

/**
 * Represents a downloadable file.
 */
public abstract class DownloadableFileNode extends FileNode {
    private String filePath;
    private String infoFilePath;

    protected abstract String getFileExtension();

    protected abstract String getFilePrefix();

    protected abstract String getDescriptorPrefix();

    protected abstract String getDescriptorExtension();

    public DownloadableFileNode() {
    }

    /**
     * old full constructor
     *
     * @param owner               - person who owns the node
     * @param task                - task which created this node
     * @param name                - name of the node
     * @param description         - description of the node
     * @param visibility          - visibility of the node to others
     * @param relativeSessionPath - name of the work session this node belongs to
     * @param dataType            - tag for the node
     */
    public DownloadableFileNode(String owner, Task task, String name, String description, String visibility, String dataType,
                                String relativeSessionPath) {
        super(owner, task, name, description, visibility, dataType, relativeSessionPath);
    }

    /**
     * Constructs the path to the downloadable main file.  BlastDatabaseFileNode currently inherits this method.
     * It means that it's downloadable fasta file (the whole thing as opposed to the little p's) would
     * have a different base location from the blastable database files (determined by getDirectoryPath() and
     * getFilePath())
     *
     * @return - path to the file
     */
    public String getDownloadableFilePath() {
        if (filePath == null) {
            filePath = getDownloadableFilePath(getFilePrefix(), getFileExtension(), getObjectId());
        }
        return filePath;
    }

    /**
     * Constructs the path to the downloadable descriptor file.  BlastDatabaseFileNode currently inherits this method.
     * It means that it's downloadable descriptor file (the whole thing as opposed to the little p's) would
     * have a different base location from the blastable database files (determined by getDirectoryPath() and
     * getFilePath())
     *
     * @return - path to the info file
     */
    public String getDownloadableInfoFilePath() {
        if (infoFilePath == null) {
            infoFilePath = getDownloadableFilePath(getDescriptorPrefix(), getDescriptorExtension(), getObjectId());
        }
        return infoFilePath;
    }

    /**
     * Constructs the path to the downloadable main file.  BlastDatabaseFileNode currently inherits this method.
     * It means that it's downloadable fasta file (the whole thing as opposed to the little p's) would
     * have a different base location from the blastable database files (determined by getDirectoryPath() and
     * getFilePath())
     *
     * @param objectId - file to be downloaded
     * @return - path to the file
     */
    public String createDownloadableFilePath(Long objectId) {
        return getDownloadableFilePath(getFilePrefix(), getFileExtension(), objectId);
    }

    /**
     * Constructs the path to the downloadable descriptor file.  BlastDatabaseFileNode currently inherits this method.
     * It means that it's downloadable fasta file (the whole thing as opposed to the little p's) would
     * have a different base location from the blastable database files (determined by getDirectoryPath() and
     * getFilePath())
     *
     * @param objectId - file to be downloaded
     * @return - path to the file
     */
    public String createDownloadableInfoFilePath(Long objectId) {
        return getDownloadableFilePath(getDescriptorPrefix(), getDescriptorExtension(), objectId);
    }

    /**
     * Constructs the path to the downloadable file item using the prefix, extension, and id of the node
     *
     * @param filePrefix    - prefix of the node
     * @param fileExtension - extension of the file
     * @param objectId      - file to be downloaded
     * @return - path to the file
     */
    private static String getDownloadableFilePath(String filePrefix, String fileExtension, Long objectId) {
        if (objectId == null) {
            throw new IllegalStateException("Id is mandatory");
        }
        // Don't get bogged down with why the key begins with dma.  It could be changed to downloadableNodeDirPath
        // This is the base path for downloading the actual files
        return new File(SystemConfigurationProperties.getString("dma.downloadableNodeDirPath")).getAbsolutePath()
                + File.separator + filePrefix + objectId + fileExtension;
    }

    public String getSubDirectory() {
        return "";
    }

}
