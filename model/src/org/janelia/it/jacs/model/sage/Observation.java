package org.janelia.it.jacs.model.sage;

// Generated Oct 9, 2015 2:22:35 PM by Hibernate Tools 3.4.0.CR1

import java.util.Date;

/**
 * Observation generated by hbm2java
 */
public class Observation implements java.io.Serializable {

    private Integer id;
    private CvTerm type;
    private SageSession session;
    private CvTerm term;
    private Experiment experiment;
    private String value;
    private Date createDate;

    public Observation() {
    }

    public Observation(CvTerm type, CvTerm term, String value, Date createDate) {
        this.type = type;
        this.term = term;
        this.value = value;
        this.createDate = createDate;
    }

    public Observation(CvTerm type, SageSession session, CvTerm term, Experiment experiment, String value, Date createDate) {
        this.type = type;
        this.session = session;
        this.term = term;
        this.experiment = experiment;
        this.value = value;
        this.createDate = createDate;
    }

    public Integer getId() {
        return this.id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public CvTerm getType() {
        return this.type;
    }

    public void setType(CvTerm type) {
        this.type = type;
    }

    public SageSession getSession() {
        return this.session;
    }

    public void setSession(SageSession session) {
        this.session = session;
    }

    public CvTerm getTerm() {
        return this.term;
    }

    public void setTerm(CvTerm term) {
        this.term = term;
    }

    public Experiment getExperiment() {
        return this.experiment;
    }

    public void setExperiment(Experiment experiment) {
        this.experiment = experiment;
    }

    public String getValue() {
        return this.value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Date getCreateDate() {
        return this.createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

}
