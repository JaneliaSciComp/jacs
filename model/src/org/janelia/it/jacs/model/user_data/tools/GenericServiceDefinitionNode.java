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

package org.janelia.it.jacs.model.user_data.tools;

import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.model.user_data.Node;

/**
 * Created by IntelliJ IDEA.
 * User: jhoover
 * Date: Apr 15, 2010
 * Time: 1:33:51 PM
 */
public class GenericServiceDefinitionNode extends FileNode {

    public enum serviceTags {
        initialization, execution, finalization, readme
    }

    public GenericServiceDefinitionNode() {
        super();
    }

    public GenericServiceDefinitionNode(String owner, Task task, String name, String description, String visibility,
                                        String relativeSessionPath) {
        super(owner, task, name, description, visibility, Node.DIRECTORY_DATA_TYPE, relativeSessionPath);
    }


    @Override
    public String getSubDirectory() {
        return "GenericServices";  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getFilePathByTag(String tag) {
        switch (serviceTags.valueOf(tag.toLowerCase())) {
            case initialization:
                return getDirectoryPath().concat("/initialization.sh");
            case execution:
                return getDirectoryPath().concat("/execution.sh");
            case finalization:
                return getDirectoryPath().concat("/finalization.sh");
            case readme:
                return getDirectoryPath().concat("/readme.txt");
            default:
                return null;
        }
    }
}
