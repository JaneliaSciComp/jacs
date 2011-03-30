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

package org.janelia.it.jacs.model.tasks.blast;

import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.vo.*;

import java.util.List;
import java.util.Set;

/**
 * @author Michael Press
 */
public class TBlastNTask extends BlastTask {
    transient public static final String TBLASTN_NAME = "tblastn";
    transient public static final String DISPLAY_TBLASTN = "TBLASTN (prot/nuc)";

    // Default values
    transient public static final Long wordsize_TBLASTN_DEFAULT = (long) 3;
    transient public static final Long gappedAlignmentDropoff_TBLASTN_DEFAULT = (long) 15;
    transient public static final Long hitExtensionThreshold_TBLASTN_DEFAULT = (long) 13;
    transient public static final Double ungappedExtensionDropoff_TBLASTN_DEFAULT = 7.0;
    transient public static final Double finalGappedDropoff_TBLASTN_DEFAULT = 25.0;
    transient public static final Long multiHitWindowSize_TBLASTN_DEFAULT = (long) 40;

    // Custom defaults
    transient public static final Boolean gappedAlignment_DEFAULT = Boolean.TRUE;

    // Parameter Keys
    // transient public static final String PARAM_gappedAlignment = "gapped alignment";//-g)";

    public TBlastNTask() {
        super();
        setParameter(PARAM_wordsize, wordsize_TBLASTN_DEFAULT.toString());
        setParameter(PARAM_gappedAlignmentDropoff, gappedAlignmentDropoff_TBLASTN_DEFAULT.toString());
        setParameter(PARAM_hitExtensionThreshold, hitExtensionThreshold_TBLASTN_DEFAULT.toString());
        setParameter(PARAM_ungappedExtensionDropoff, ungappedExtensionDropoff_TBLASTN_DEFAULT.toString());
        setParameter(PARAM_finalGappedDropoff, finalGappedDropoff_TBLASTN_DEFAULT.toString());
        setParameter(PARAM_multiHitWindowSize, multiHitWindowSize_TBLASTN_DEFAULT.toString());
        setParameter(PARAM_gappedAlignment, gappedAlignment_DEFAULT.toString());

        constructorCommon();
    }

    public ParameterVO getParameterVO(String key) throws ParameterException {
        if (key == null)
            return null;
        String value = getParameter(key);
        if (value == null)
            return null;
        ParameterVO pvo = super.getParameterVO(key);
        if (pvo != null)
            return pvo;
        if (key.equals(PARAM_wordsize))
            return new LongParameterVO(new Long(value));
        if (key.equals(PARAM_gappedAlignmentDropoff))
            return new LongParameterVO(new Long(value));
        if (key.equals(PARAM_hitExtensionThreshold))
            return new LongParameterVO(new Long(value));
        if (key.equals(PARAM_ungappedExtensionDropoff))
            return new DoubleParameterVO(new Double(value));
        if (key.equals(PARAM_finalGappedDropoff))
            return new DoubleParameterVO(new Double(value));
        if (key.equals(PARAM_multiHitWindowSize))
            return new LongParameterVO(new Long(value));
        if (key.equals(PARAM_gappedAlignment))
            return new BooleanParameterVO(Boolean.valueOf(value));
        // No match
        return null;
    }

    /**
     * full constructor
     */
    public TBlastNTask(Set<Node> inputNodes,
                       String owner,
                       List<Event> events,
                       Set<TaskParameter> parameters) {
        super(inputNodes, owner, events, parameters);
        constructorCommon();
    }

    public String getDisplayName() {
        return DISPLAY_TBLASTN;
    }

    private void constructorCommon() {
        this.taskName = TBLASTN_NAME;
    }

    public String generateCommandStringNotIncludingIOParams() throws ParameterException {
        StringBuffer sb = new StringBuffer(generateDefaultCommandStringNotIncludingIOParams());
        sb.append("-g ").append((getParameterVO(PARAM_gappedAlignment)).getStringValue().equals("true") ? "T" : "F").append(" ");
        return sb.toString();
    }

}
