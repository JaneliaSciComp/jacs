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

package org.janelia.it.jacs.shared.dma.entity;

import org.janelia.it.jacs.model.dma.Tag;
import org.janelia.it.jacs.model.genomics.DefLineFormat;
import org.janelia.it.jacs.model.genomics.EntityType;
import org.janelia.it.jacs.model.genomics.SequenceType;

import java.util.*;

/**
 * This class encapsulates the information that's parsed for a sequence entry in a fasta
 * file
 *
 * @author Tareq Nabeel
 */
public class SequenceInfo {

    public static final String DEFLINE_ACCESSION = "defline_accession";
    public static final String TAGS = "KEYWORDS";
    public static final String TAG_ASSEMBLY_STATUS = "assembly_status";
    public static final String TAG_DATA_TYPE = "data_type";
    public static final String TAG_PROJECT = "project";
    public static final String TAG_TAXON_GROUP = "taxon_group";
    public static final String DATA_TYPE_GENOMIC = "genomic";
    public static final String DATA_TYPE_PROTEIN = "protein";
    public static final String DATA_TYPE_PEPTIDE = "peptide";
    public static final String ASSEMBLY_STATUS_FINISHED = "finished";
    public static final String ASSEMBLY_STATUS_DRAFT = "draft";
    private static final String DEF_KEY_SEQUENCE_LENGTH = "LENGTH";
    private static final String DEF_KEY_EXTERNAL_ACC = "ACCESSION";
    private static final String DEF_KEY_GI_NUMBER = "GI";
    private static final String DEF_KEY_ORGANISM = "ORGANISM";
    private static final String DEF_KEY_LOCUS = "LOCUS";
    private static final String DEF_KEY_TISSUE = "TISSUE";
    private static final String DEF_KEY_TAXON_ID = "TAXON_ID";
    private static final String DEF_KEY_DESCRIPTION = "DESCRIPTION";

    private String sequence;

    private Map<String, String> deflineMap = new HashMap<String, String>();
    private String defline;

    private Set<Tag> tagSet = new HashSet<Tag>();
    private Set<String> tagValueSet = new HashSet<String>();
    private Map<String, String> tagMap = new HashMap<String, String>();
    private long seqId;
    private long entityId;
    private int giNumber;
    private int seqLength = -1;

    private EntityType entityType;
    private SequenceType seqType;
    private String assemblyAcc;
    private AssemblyStatus assemblyStatus;
    private boolean hasAssemblyStatus;
    private String externalSource;

    public SequenceInfo(String defline, String sequence) {
        setSequence(sequence);
        defline = defline.replace(">GP_", ">NCBI_");
        defline = defline.replace(">GB_", ">NCBI_");
        setDefline(defline);
        parseDefline();
        setTagMap();
        setAssemblyStatus();
        setEntityType();
        setGiNumber();
        setSeqLength();
        setExternalSource();
    }

    private void setDefline(String defline) {
        if (defline == null) {
            throw new IllegalArgumentException("deflineMap cannot be null or of zero size; deflineMap=" + deflineMap);
        }
        this.defline = defline;
    }

    private void parseDefline() {
        DefLineFormat.parseDefline(defline, deflineMap, false);
        if (deflineMap.get(DEFLINE_ACCESSION) == null) {
            throw new IllegalArgumentException("deflineMap must contain " + DEFLINE_ACCESSION + "; deflineMap=" + deflineMap);
        }
    }

    private void setSequence(String sequence) {
        if (sequence == null || sequence.length() == 0) {
            throw new IllegalArgumentException("sequence cannot be null or of zero length; sequence=" + sequence);
        }
        this.sequence = sequence;
    }

    private void setTagMap() {
        String tags = deflineMap.get(TAGS);
        if (tags == null) {
            throw new IllegalArgumentException("deflineMap must contain " + TAGS + "; deflineMap=" + deflineMap);
        }
        String[] nameValuePairs = tags.split(",");
        for (String nameValuePair : nameValuePairs) {
            String[] nameValue = nameValuePair.split(":");
            if (nameValue.length > 1) {
                String name = nameValue[0].trim();
                String value = nameValue[1].trim();
                if (value.length() > 0) {
                    tagMap.put(name, value);
                    StringTokenizer tokenizer = new StringTokenizer(value, " ");
                    while (tokenizer.hasMoreTokens()) {
                        String token = tokenizer.nextToken();
                        tagValueSet.add(token);
                    }
                }
            }
        }
    }


    public SequenceInfo(long entityId, String acc) {
        this.deflineMap = new HashMap();
        deflineMap.put(DEFLINE_ACCESSION, acc);
        this.entityId = entityId;
    }

    public String getSequence() {
        return sequence;
    }


    public Map getDeflineMap() {
        return deflineMap;
    }

    public void setDeflineMap(Map deflineMap) {
        this.deflineMap = deflineMap;
    }

    public String getCameraAcc() {
        return deflineMap.get(DEFLINE_ACCESSION);
    }

    public Collection<String> getParsedTags() {
        return tagValueSet;
    }

    public void setTagSet(Set tagSet) {
        this.tagSet = tagSet;
    }

    public Set<Tag> getTagsSet() {
        return tagSet;
    }

    public boolean addTag(Tag tag) {
        return tagSet.add(tag);
    }

    public long getSeqId() {
        return seqId;
    }

    public void setSeqId(long seqId) {
        this.seqId = seqId;
    }

    public long getEntityId() {
        return entityId;
    }

    public void setEntityId(long entityId) {
        this.entityId = entityId;
    }

    public int getEntityTypeCode() {
        return entityType.getCode();
    }

    public int getSeqTypeCode() {
        return seqType.getCode();
    }

    private void setEntityType() {
        String dataType = getDataType();
        if (dataType == null) {
            throw new IllegalArgumentException("data_type must be specified in /KEYWORKDS; deflineMap=" + deflineMap);
        }
        if (dataType.equals(DATA_TYPE_GENOMIC)) {
            this.seqType = SequenceType.NA;
//            switch (assemblyStatus) {
//                case draft:
//                    this.entityType = EntityType.SCAFFOLD;
//                    break;
//                case finished:
//                    this.entityType = EntityType.CHROMOSOME;
//                    break;
//                default:
//                    this.entityType = EntityType.GENE;
//            }
            this.entityType = EntityType.NUCLEOTIDE;
        }
        else if (dataType.equals(DATA_TYPE_PROTEIN)) {
            this.seqType = SequenceType.AA;
            this.entityType = EntityType.PROTEIN;
        }
        else if (dataType.equals(DATA_TYPE_PEPTIDE)) {
            this.seqType = SequenceType.AA;
            this.entityType = EntityType.PEPTIDE;
        }
        else {
            throw new IllegalArgumentException("data_type:" + dataType + " unrecognized in /KEYWORKDS; deflineMap=" + deflineMap);
        }
    }

    private void setAssemblyStatus() {
        String status = getAssemblyStatusStr();
        if (status != null && status.length() > 0) {
            if (status.equals(ASSEMBLY_STATUS_DRAFT)) {
                this.assemblyStatus = AssemblyStatus.draft;
                hasAssemblyStatus = true;
            }
            else if (status.equals(ASSEMBLY_STATUS_FINISHED)) {
                this.assemblyStatus = AssemblyStatus.finished;
                hasAssemblyStatus = true;
            }
            else {
                throw new IllegalArgumentException("Unknown assembly status:" + status);
            }
        }
        else {
            this.assemblyStatus = AssemblyStatus.unknown;
            hasAssemblyStatus = false;
        }
    }

    public String getAssemblyStatusStr() {
        return tagMap.get(TAG_ASSEMBLY_STATUS);
    }

    public String getDataType() {
        return tagMap.get(TAG_DATA_TYPE);
    }

    public String getTaxonGroup() {
        return tagMap.get(TAG_TAXON_GROUP);
    }

    public String getProject() {
        return tagMap.get(TAG_PROJECT);
    }

    public String getDefline() {
        return defline;
    }

    public int getSeqLength() {
        return seqLength;
    }

    private void setSeqLength() {
//        String length = deflineMap.get(DEF_KEY_SEQUENCE_LENGTH);
//        if (length!=null) {
//            seqLength = Integer.parseInt(length);
//        } else {
        seqLength = sequence.length();
//        }
    }

    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final SequenceInfo other = (SequenceInfo) obj;
        return this.getCameraAcc().equals(other.getCameraAcc());
    }

    public int hashCode() {
        return getCameraAcc().hashCode();
    }

    public String getExternalAcc() {
        return deflineMap.get(DEF_KEY_EXTERNAL_ACC);
    }

    public int getGiNumber() {
        return giNumber;
    }

    public void setGiNumber() {
        String ginum = deflineMap.get(DEF_KEY_GI_NUMBER);
        if (ginum != null) {
            ginum = ginum.trim();
            if (ginum.length() > 0) {
                giNumber = Integer.parseInt(ginum);
            }
            else {
                giNumber = -1;
            }
        }
        else {
            giNumber = -1;
        }
    }

    public String getOrganism() {
        return deflineMap.get(DEF_KEY_ORGANISM);
    }

    public String getLocus() {
        return deflineMap.get(DEF_KEY_LOCUS);
    }

    public String getTissue() {
        return deflineMap.get(DEF_KEY_TISSUE);
    }

    public String getDescription() {
        return deflineMap.get(DEF_KEY_DESCRIPTION);
    }

    public int getTaxonId() {
        String taxonId = deflineMap.get(DEF_KEY_TAXON_ID);
        if (taxonId != null && taxonId.trim().length() > 1) {
            return Integer.parseInt(taxonId);
        }
        else {
//            throw new IllegalArgumentException("Invalid taxon id:"+taxonId+" supplied in defline:"+defline+" deflineMap="+deflineMap);
            return -1;
        }
    }

    public String getAssemblyAcc() {
        return assemblyAcc;
    }

    public void setAssemblyAcc(String assemblyAcc) {
        this.assemblyAcc = assemblyAcc;
    }

    public boolean hasAssemblyStatus() {
        return hasAssemblyStatus;
    }

    public AssemblyStatus getAssemblyStatus() {
        return assemblyStatus;
    }

    private void setExternalSource() {
        externalSource = getCameraAcc().substring(0, getCameraAcc().indexOf('_'));
    }

    public String getExternalSource() {
        return externalSource;
    }
}
