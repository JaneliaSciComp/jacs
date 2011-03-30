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

package org.janelia.it.jacs.web.gwt.common.client.model.genomics;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Client-side value object based on org.janelia.it.jacs.model.genomics.Alignment.
 *
 * @author Michael Press
 */
abstract public class Alignment implements IsSerializable {
    public static final String SORT_BY_EVAL = "hit.expectScore";
    public static final String SORT_BY_QUERY_DEFLINE = "queryDef";
    public static final String SORT_SUBJECT_DEFLINE = "subjectDef";
    public static final String SORT_SUBJECT_ACC = "subjectAcc";

    private String blastHitId = null;
    private Integer subjectBegin = null;
    private Integer subjectEnd = null;
    private Integer subjectOrientation = null;
    private Integer queryBegin = null;
    private Integer queryEnd = null;
    private Integer queryOrientation = null;
    private BaseSequenceEntity queryEntity;
    private BaseSequenceEntity subjectEntity;
    private AlignmentType alignmentType;

    public Alignment() {
    }

    protected Alignment(Long blastHitId, Integer subjectBegin, Integer subjectEnd, Integer subjectOrientation,
                        Integer queryBegin, Integer queryEnd, Integer queryOrientation, BaseSequenceEntity queryEntity,
                        BaseSequenceEntity subjectEntity) {
        if (blastHitId != null)
            this.blastHitId = String.valueOf(blastHitId);
        this.subjectBegin = subjectBegin;
        this.subjectEnd = subjectEnd;
        this.subjectOrientation = subjectOrientation;
        this.queryBegin = queryBegin;
        this.queryEnd = queryEnd;
        this.queryOrientation = queryOrientation;
        this.subjectEntity = subjectEntity;
        this.queryEntity = queryEntity;
    }

    public Integer getSubjectBegin() {
        return subjectBegin;
    }

    public void setSubjectBegin(Integer subjectBegin) {
        this.subjectBegin = subjectBegin;
    }

    public Integer getSubjectEnd() {
        return subjectEnd;
    }

    public void setSubjectEnd(Integer subjectEnd) {
        this.subjectEnd = subjectEnd;
    }

    public Integer getSubjectBegin_oneResCoords() {
        return new Integer(subjectBegin.intValue() + 1);
    }

    public void setSubjectBegin_oneResCoords(Integer subjectBegin_oneResidueCoords) {
        this.subjectBegin = new Integer(subjectBegin_oneResidueCoords.intValue() - 1);
    }

    public Integer getSubjectEnd_oneResCoords() {
        return subjectEnd;
    }

    public void setSubjectEnd_oneResidueCoords(Integer subjectEnd_oneResidueCoords) {
        this.subjectEnd = subjectEnd_oneResidueCoords;
    }

    public Integer getSubjectOrientation() {
        return subjectOrientation;
    }

    public void setSubjectOrientation(Integer subjectOrientation) {
        this.subjectOrientation = subjectOrientation;
    }

    public Integer getQueryBegin() {
        return queryBegin;
    }

    public void setQueryBegin(Integer queryBegin) {
        this.queryBegin = queryBegin;
    }

    public Integer getQueryEnd() {
        return queryEnd;
    }

    public void setQueryEnd(Integer queryEnd) {
        this.queryEnd = queryEnd;
    }

    public Integer getQueryBegin_oneResCoords() {
        return new Integer(queryBegin.intValue() + 1);
    }

    public void setQueryBegin_oneResCoords(Integer queryBegin_oneResidueCoords) {
        this.queryBegin = new Integer(queryBegin_oneResidueCoords.intValue() - 1);
    }

    public Integer getQueryEnd_oneResCoords() {
        return queryEnd;
    }

    public void setQueryEnd_oneResCoords(Integer queryEnd_oneResidueCoords) {
        this.queryEnd = queryEnd_oneResidueCoords;
    }

    public Integer getQueryOrientation() {
        return queryOrientation;
    }

    public void setQueryOrientation(Integer queryOrientation) {
        this.queryOrientation = queryOrientation;
    }

    public String getBlastHitId() {
        return blastHitId;
    }

    public BaseSequenceEntity getSubjectEntity() {
        return subjectEntity;
    }

    public void setSubjectEntity(BaseSequenceEntity subjectEntity) {
        this.subjectEntity = subjectEntity;
    }

    public void setAlignmentType(AlignmentType alignmentType) {
        this.alignmentType = alignmentType;
    }

    public AlignmentType getAlignmentType() {
        return alignmentType;
    }

    public BaseSequenceEntity getQueryEntity() {
        return queryEntity;
    }

    public void setQueryEntity(BaseSequenceEntity queryEntity) {
        this.queryEntity = queryEntity;
    }

}
