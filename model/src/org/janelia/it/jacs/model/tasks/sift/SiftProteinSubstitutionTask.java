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

package org.janelia.it.jacs.model.tasks.sift;

import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.vo.ParameterException;
import org.janelia.it.jacs.model.vo.ParameterVO;


/**
 * Created by IntelliJ IDEA.
 * User: zguan
 * Date: Jul 20, 2010
 * Time: 4:05:05 PM
 */
public class SiftProteinSubstitutionTask extends Task {

    transient public static final String PARAM_substitution_string = "substitution string";
    transient public static final String PARAM_fasta_input_node_id = "input node id";

    public SiftProteinSubstitutionTask() {
        super();
        setTaskName("SiftProteinSubstitutionTask");
        setParameter(PARAM_substitution_string, "");
        setParameter(PARAM_fasta_input_node_id, "");
    }

    public String getDisplayName() {
        return "SiftProteinSubstitutionTask";
    }

    public ParameterVO getParameterVO(String key) throws ParameterException {
        if (key == null)
            return null;
        String value = getParameter(key);
        if (value == null)
            return null;
        // no match
        return null;
    }

}
