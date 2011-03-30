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

package org.janelia.it.jacs.model.tasks.recruitment;

import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.ParameterVO;
import org.janelia.it.jacs.model.vo.TextParameterVO;

import java.util.List;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Apr 17, 2007
 * Time: 1:05:55 PM
 */
public class UserBlastFrvTask extends Task {

    public static final String DISPLAY_NAME = "User Blast-to-Frv Task";

    public UserBlastFrvTask() {
        super();
    }

    public UserBlastFrvTask(Set inputNodes, String owner, List events, Set parameters) {
        super(inputNodes, owner, events, parameters);
        this.taskName = "User-initiated Recruitment Task";
    }

    public ParameterVO getParameterVO(String key) throws ParameterException {
        if (key == null) {
            return null;
        }
        String value = getParameter(key);
        if (value == null) {
            return null;
        }
        // Default to something.  Not expecting any parameters for this task anyway.
        else {
            return new TextParameterVO(value, value.length() > 500 ? value.length() : 500);
        }
    }

    public String getDisplayName() {
        return DISPLAY_NAME;
    }

}