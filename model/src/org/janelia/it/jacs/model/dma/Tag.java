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

package org.janelia.it.jacs.model.dma;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.io.Serializable;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: tnabeel
 * Date: Sep 7, 2007
 * Time: 10:01:51 AM
 */
public class Tag implements Serializable, IsSerializable {

    private Long id;

    private String name;

    private String description;

    private Classification classification;

    private Set entities;

    private Set blastNodes;

    public Tag() {
    }

    public Tag(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }


    public Classification getClassification() {
        return classification;
    }

    public void setClassification(Classification classification) {
        this.classification = classification;
    }

    public Set getBlastNodes() {
        return blastNodes;
    }

    public void setBlastNodes(Set blastNodes) {
        this.blastNodes = blastNodes;
    }

    public Set getEntities() {
        return entities;
    }

    public void setEntities(Set entities) {
        this.entities = entities;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Tag tag = (Tag) o;

//        if (id!=null && tag.getId()!=null) {
//            return id.equals(tag.getId());
//        }
        return name.equals(tag.name);

    }

    public int hashCode() {
//        if (this.id!=null) {
//            return id.hashCode();
//        } else {
        return name.hashCode();
//        }
    }

    public String toString() {
        return name;
    }
}
