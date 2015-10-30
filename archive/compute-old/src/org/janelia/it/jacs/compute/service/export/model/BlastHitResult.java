
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
