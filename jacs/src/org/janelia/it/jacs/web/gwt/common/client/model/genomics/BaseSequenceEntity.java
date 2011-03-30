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

abstract public class BaseSequenceEntity implements IsSerializable {

    public static Integer FORWARD_ORIENTATION = new Integer(1);
    public static Integer REVERSE_ORIENTATION = new Integer(-1);

    private Long entityId;
    private String cameraAcc;
    private String externalAcc;
    //protected User owner;
    private String description;
    private String descriptionFormatted;
    //protected EntityType entityType;
    //protected BioSequence bioSequence;
    private Integer seqLength;
    private String defline;
    private String deflineFormatted;

    public BaseSequenceEntity() {
    }

    //public BaseSequenceEntity(EntityType entityType) {
    //    this.entityType = entityType;
    //    this.bioSequence = new BioSequence(entityType.getSequenceType());
    //    this.seqLength = new Integer(this.bioSequence.getLength());
    //}

    public Long getEntityId() {
        return entityId;
    }

    public String getCameraAcc() {
        return cameraAcc;
    }

    public void setCameraAcc(String cameraAcc) {
        this.cameraAcc = cameraAcc;
    }

    /**
     * external Accession is stored in form LOCATION|ACC
     *
     * @return
     */
    public String getFullExternalAcc() {
        return externalAcc;
    }

    /**
     * parses out an ID and returns it
     *
     * @return
     */
    public String getExternalAcc() {
        if (externalAcc == null || externalAcc.length() == 0)
            return null;
        int pipeCharLoc = externalAcc.indexOf('|');
        // if no pipe char - assume no location, and whole string is an ACC
        return externalAcc.substring(pipeCharLoc + 1);
    }

    /**
     * parses out location from external ACC
     */
    public String getExternalAccLocation() {
        if (externalAcc == null || externalAcc.length() == 0)
            return null;
        int pipeCharLoc = externalAcc.indexOf('|');
        // if no pipe char - assume no location, and whole string is an ACC
        if (pipeCharLoc < 0)
            return null;
        else
            return externalAcc.substring(0, pipeCharLoc);
    }


    public void setExternalAcc(String externalAcc) {
        this.externalAcc = externalAcc;
    }

    //public User getOwner() {
    //    return owner;
    //}
    //
    //public void setOwner(User owner) {
    //    this.owner = owner;
    //}

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getSeqLength() {
        return seqLength;
    }

    //public EntityType getEntityType() {
    //    return entityType;
    //}
    //
    //public SequenceType getSequenceType() {
    //    return entityType.getSequenceType();
    //}
    //
    //public BioSequence getBioSequence() {
    //    if ( bioSequence!=null)
    //        return bioSequence;
    //    else
    //        return new BioSequence(getSequenceType());
    //}

    /*
     * get sequence data
     */
    //public String getSequence() {
    //   return getBioSequence().getSequence();
    //}

    //public String getSequence(int begin, int end, int ori) {
    //    return getBioSequence().subSequence(begin,end,ori).getSequence();
    //}

    /*
     * old calls to get sequence data (for backward compatibility)
     */
    //public String getSeqText() {
    //    return getSequence();
    //}
    //
    //public String getSeqText(int begin) {
    //    return getSequence(begin, getSeqLength().intValue(), FORWARD_ORIENTATION.intValue());
    //}
    //
    //public String getSeqText(int begin, int end) {
    //    return getSequence(begin, end, FORWARD_ORIENTATION.intValue());
    //}
    /*
     * set Sequence - one of three ways
     * 1. as a BioSequence object
     * 2. as a String (creates a BioSequence object from the string)
     * 3. as a Subfeature
     */
    //public void setSequence(BioSequence bioSequence) {
    //    if (entityType.getSequenceType()!=bioSequence.getSequenceType())
    //        throw new SequenceException(
    //                    "Entity Type ".concat(entityType.getName())
    //                    .concat(" is incompatible with ")
    //                    .concat("Sequence Type ").concat(bioSequence.getSequenceType().getName())
    //                    .concat("."));
    //    this.bioSequence = bioSequence;
    //    this.seqLength = new Integer(bioSequence.getLength());
    //}

    //public void setSequence(String seqText) {
    //    setSequence(new BioSequence(getSequenceType(),seqText));
    //}
    //
    //public void setSequence(BaseSequenceEntity parentEntity, Integer parentBegin, Integer parentEnd, Integer parentOrientation) {
    //    if (parentBegin.intValue()<0 || parentEnd.intValue()<parentBegin.intValue())
    //      throw new SequenceException("Invalid sequence, improper range specification \""
    //                             .concat(parentBegin.toString()).concat("-")
    //                             .concat(parentEnd.toString()).concat("\""));
    //    else if ( parentEnd.intValue()>parentEntity.getSeqLength().intValue())
    //        throw new SequenceException("Invalid sequence, range \""
    //                               .concat(parentBegin.toString()).concat("-").concat(parentEnd.toString())
    //                               .concat("\" is out of bounds (0-").concat(parentEntity.getSeqLength().toString()).concat(")."));
    //    else
    //      this.bioSequence = parentEntity.getBioSequence().subSequence(parentBegin,parentEnd,parentOrientation);
    //      this.seqLength = new Integer(parentEnd.intValue()-parentBegin.intValue());
    //}

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public void setSeqLength(Integer seqLength) {
        this.seqLength = seqLength;
    }

    //protected void setEntityType(EntityType entityType) {
    //    this.entityType = entityType;
    //}
    //protected void setBioSequence(BioSequence bioSequence) {
    //    this.bioSequence = bioSequence;
    //}

    public String getDefline() {
        return defline;
    }

    public void setDefline(String defline) {
        this.defline = defline;
    }

    public String getDeflineFormatted() {
        return deflineFormatted;
    }

    public void setDeflineFormatted(String deflineFormatted) {
        this.deflineFormatted = deflineFormatted;
    }

    public String getDescriptionFormatted() {
        return descriptionFormatted;
    }

    public void setDescriptionFormatted(String descriptionFormatted) {
        this.descriptionFormatted = descriptionFormatted;
    }
}
