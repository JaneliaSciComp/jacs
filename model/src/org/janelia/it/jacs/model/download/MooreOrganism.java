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

package org.janelia.it.jacs.model.download;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.io.Serializable;
import java.util.Date;

/**
 * This class represents a data row in the organism table of MF150 database
 *
 * @author Tareq Nabeel
 */
public class MooreOrganism implements IsSerializable, Serializable {

    /**
     * Organism accession
     */
    private String accession;

    /**
     * AvgContigSize
     */
    private Double avgContigSize;

    /**
     * Citations
     */
    private String citations;

    /**
     * CollectionMethod
     */
    private String collectionMethod;

    /**
     * DeliveryDate
     */
    private Date deliveryDate;

    /**
     * Depth
     */
    private String depth;


    /**
     * Description
     */
    private String description;


    /**
     * GenomeSize
     */
    private Double genomeSize;

    /**
     * GcContentPerc
     */
    private Double gcContentPerc;

    /**
     * GenbankContribDate
     */
    private Date genbankContribDate;

    /**
     * GeneCodingPerc
     */
    private Double GeneCodingPerc;

    /**
     * GeneCount
     */
    private Long geneCount;

    /**
     * GeneRrnaCount
     */
    private Long GeneRrnaCount;

    /**
     * GeneTrnaCount
     */
    private Long geneTrnaCount;

    /**
     * GenomeLength
     */
    private Double genomeLength;

    /**
     * InvestigatorEmail
     */
    private String investigatorEmail;

    /**
     * InvestigatorWebsite
     */
    private String investigatorWebsite;

    /**
     * Location
     */
    private String location;

    /**
     * NcbiUrl
     */
    private String ncbiUrl;

    /**
     * Proposer
     */
    private String proposer;

    /**
     * ReceivedDate
     */
    private Date receivedDate;

    /**
     * Relevance
     */
    private String relevance;

    /**
     * ScaffoldCount
     */
    private Long scaffoldCount;

    /**
     * Species
     */
    private String species;

    /**
     * StatusTag
     */
    private String statusTag;

    /**
     * Strain
     */
    private String strain;

    /**
     * Organism name
     */
    private String organismName;

    /**
     * Used to construct external link to MF150 web site organism detail
     */
    private String speciesTag;

    /**
     * Name of the investigator
     */
    private String investigatorName;

    /**
     * Release date
     */
    private Date releaseDate;

    /**
     * Status
     */
    private String status;


    public MooreOrganism() {
    }


    public String getAccession() {
        return accession;
    }

    public void setAccession(String accession) {
        this.accession = accession;
    }

    public String getOrganismName() {
        return organismName;
    }

    public void setOrganismName(String organismName) {
        this.organismName = organismName;
    }

    public String getSpeciesTag() {
        return speciesTag;
    }

    public void setSpeciesTag(String speciesTag) {
        this.speciesTag = speciesTag;
    }

    public String getInvestigatorName() {
        return investigatorName;
    }

    public void setInvestigatorName(String investigatorName) {
        this.investigatorName = investigatorName;
    }

    public Date getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(Date releaseDate) {
        this.releaseDate = releaseDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Double getAvgContigSize() {
        return avgContigSize;
    }

    public void setAvgContigSize(Double avgContigSize) {
        this.avgContigSize = avgContigSize;
    }

    public String getCitations() {
        return citations;
    }

    public void setCitations(String citations) {
        this.citations = citations;
    }

    public String getCollectionMethod() {
        return collectionMethod;
    }

    public void setCollectionMethod(String collectionMethod) {
        this.collectionMethod = collectionMethod;
    }

    public Date getDeliveryDate() {
        return deliveryDate;
    }

    public void setDeliveryDate(Date deliveryDate) {
        this.deliveryDate = deliveryDate;
    }

    public String getDepth() {
        return depth;
    }

    public void setDepth(String depth) {
        this.depth = depth;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Double getGenomeSize() {
        return genomeSize;
    }

    public void setGenomeSize(Double genomeSize) {
        this.genomeSize = genomeSize;
    }

    public Double getGcContentPerc() {
        return gcContentPerc;
    }

    public void setGcContentPerc(Double gcContentPerc) {
        this.gcContentPerc = gcContentPerc;
    }

    public Date getGenbankContribDate() {
        return genbankContribDate;
    }

    public void setGenbankContribDate(Date genbankContribDate) {
        this.genbankContribDate = genbankContribDate;
    }

    public Double getGeneCodingPerc() {
        return GeneCodingPerc;
    }

    public void setGeneCodingPerc(Double geneCodingPerc) {
        GeneCodingPerc = geneCodingPerc;
    }

    public Long getGeneCount() {
        return geneCount;
    }

    public void setGeneCount(Long geneCount) {
        this.geneCount = geneCount;
    }

    public Long getGeneRrnaCount() {
        return GeneRrnaCount;
    }

    public void setGeneRrnaCount(Long geneRrnaCount) {
        GeneRrnaCount = geneRrnaCount;
    }

    public Long getGeneTrnaCount() {
        return geneTrnaCount;
    }

    public void setGeneTrnaCount(Long geneTrnaCount) {
        this.geneTrnaCount = geneTrnaCount;
    }

    public Double getGenomeLength() {
        return genomeLength;
    }

    public void setGenomeLength(Double genomeLength) {
        this.genomeLength = genomeLength;
    }

    public String getInvestigatorEmail() {
        return investigatorEmail;
    }

    public void setInvestigatorEmail(String investigatorEmail) {
        this.investigatorEmail = investigatorEmail;
    }

    public String getInvestigatorWebsite() {
        return investigatorWebsite;
    }

    public void setInvestigatorWebsite(String investigatorWebsite) {
        this.investigatorWebsite = investigatorWebsite;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getNcbiUrl() {
        return ncbiUrl;
    }

    public void setNcbiUrl(String ncbiUrl) {
        this.ncbiUrl = ncbiUrl;
    }

    public String getProposer() {
        return proposer;
    }

    public void setProposer(String proposer) {
        this.proposer = proposer;
    }

    public Date getReceivedDate() {
        return receivedDate;
    }

    public void setReceivedDate(Date receivedDate) {
        this.receivedDate = receivedDate;
    }

    public String getRelevance() {
        return relevance;
    }

    public void setRelevance(String relevance) {
        this.relevance = relevance;
    }

    public Long getScaffoldCount() {
        return scaffoldCount;
    }

    public void setScaffoldCount(Long scaffoldCount) {
        this.scaffoldCount = scaffoldCount;
    }

    public String getSpecies() {
        return species;
    }

    public void setSpecies(String species) {
        this.species = species;
    }

    public String getStatusTag() {
        return statusTag;
    }

    public void setStatusTag(String statusTag) {
        this.statusTag = statusTag;
    }

    public String getStrain() {
        return strain;
    }

    public void setStrain(String strain) {
        this.strain = strain;
    }

    public String toString() {
        return "\naccession=" + accession + "\nreleasedDate=" + releaseDate + "\nstatusTag=" + statusTag + "\norganismName=" + organismName + "\ninvestigatorName=" + investigatorName;
    }
}
