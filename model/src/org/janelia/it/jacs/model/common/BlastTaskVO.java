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

package org.janelia.it.jacs.model.common;

import com.google.gwt.user.client.rpc.IsSerializable;
import org.janelia.it.jacs.model.tasks.blast.BlastTask;

import java.io.Serializable;
import java.util.Set;

public class BlastTaskVO implements IsSerializable, Serializable {
    private BlastTask _blastTask;
    private UserDataNodeVO _queryNodeVO;
    private Set<BlastableNodeVO> _subjectNodeVOs;
    private String _queryType;
    private String _subjectType;

    /**
     * Required for GWT
     */
    public BlastTaskVO() {
    }

    public BlastTaskVO(BlastTask blastTask) {
        _blastTask = blastTask;
    }

    public BlastTask getBlastTask() {
        return _blastTask;
    }

    public void setBlastTask(BlastTask blastTask) {
        _blastTask = blastTask;
    }

    public UserDataNodeVO getQueryNodeVO() {
        return _queryNodeVO;
    }

    public void setQueryNodeVO(UserDataNodeVO queryNodeVO) {
        _queryNodeVO = queryNodeVO;
    }

    public Set<BlastableNodeVO> getSubjectNodeVOs() {
        return _subjectNodeVOs;
    }

    public void setSubjectNodeVOs(Set<BlastableNodeVO> subjectNodeVOs) {
        _subjectNodeVOs = subjectNodeVOs;
    }

    public String getQueryType() {
        return _queryType;
    }

    public void setQueryType(String queryType) {
        _queryType = queryType;
    }

    public String getSubjectType() {
        return _subjectType;
    }

    public void setSubjectType(String subjectType) {
        _subjectType = subjectType;
    }
}