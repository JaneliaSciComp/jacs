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

package org.janelia.it.jacs.model.user_data.entity;

import com.google.gwt.user.client.rpc.IsSerializable;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.model.user_data.Node;

import java.io.File;
import java.io.FilenameFilter;
import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Dec 15, 2008
 * Time: 3:32:11 PM
 */
public class ScreenPipelineResultNode extends FileNode implements IsSerializable, Serializable {

    /**
     * constructor
     */
    public ScreenPipelineResultNode() {
    }

    public ScreenPipelineResultNode(String owner, Task task, String name, String description, String visibility,
                                     String relativeSessionPath) {
        super(owner, task, name, description, visibility, Node.DIRECTORY_DATA_TYPE, relativeSessionPath);
    }

    public String getSubDirectory() {
        return "ScreenPipeline";
    }

    public String getFilePathByTag(String tag) {
        return getOutputFilePath(tag);
    }

    private String getOutputFilePath(final String tagTarget) {
        File tmpDir = new File(getDirectoryPath());
        File[] tmpFiles = tmpDir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return (name.toLowerCase().endsWith(tagTarget.toLowerCase()));
            }
        });
        // Assumes one and only one file, and that the tag is a unique part of the filename!
        if (1 <= tmpFiles.length) {
            return tmpFiles[0].getAbsolutePath();
        }
        return null;
    }

}
