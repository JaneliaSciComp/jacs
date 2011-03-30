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

package org.janelia.it.jacs.compute.service.export.model;

import org.janelia.it.jacs.model.genomics.BlastHit;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Jul 30, 2008
 * Time: 10:55:25 AM
 */
public class BlastHitResult {
    BlastHit blastHit;
    String queryDefline;
    String subjectDefline;
    Integer hspRank;
    Integer nhsps;

    public BlastHitResult() {
    }

    public BlastHitResult(BlastHit blastHit,
                          String queryDefline,
                          String subjectDefline,
                          Integer hspRank,
                          Integer nhsps) {
        this.blastHit = blastHit;
        this.queryDefline = queryDefline;
        this.subjectDefline = subjectDefline;
        this.hspRank = hspRank;
        this.nhsps = nhsps;
    }

    public BlastHit getBlastHit() {
        return blastHit;
    }

    public void setBlastHit(BlastHit blastHit) {
        this.blastHit = blastHit;
    }

    public String getQueryDefline() {
        return queryDefline;
    }

    public void setQueryDefline(String queryDefline) {
        this.queryDefline = queryDefline;
    }

    public String getSubjectDefline() {
        return subjectDefline;
    }

    public void setSubjectDefline(String subjectDefline) {
        this.subjectDefline = subjectDefline;
    }

    public Integer getHspRank() {
        return hspRank;
    }

    public void setHspRank(Integer hspRank) {
        this.hspRank = hspRank;
    }

    public Integer getNhsps() {
        return nhsps;
    }

    public void setNhsps(Integer nhsps) {
        this.nhsps = nhsps;
    }
}
