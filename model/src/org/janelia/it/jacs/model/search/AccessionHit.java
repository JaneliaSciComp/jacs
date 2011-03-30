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

package org.janelia.it.jacs.model.search;

import com.google.gwt.user.client.rpc.IsSerializable;
import org.janelia.it.jacs.model.user_data.Node;

public class AccessionHit implements java.io.Serializable, IsSerializable {

    // Fields
    private Node searchResultNode;
    private String accessionType;
    private Long description;
    private String accession;
    private String replacedBy;

    // Constructors

    /**
     * default constructor
     */
    public AccessionHit() {
    }

    /**
     * full constructor
     */
    public AccessionHit(Node node,
                        String accessionType,
                        String accession,
                        String replacedBy) {
        this.accessionType = accessionType;
        this.accession = accession;
        this.replacedBy = replacedBy;
        this.searchResultNode = node;
    }

    public String getAccession() {
        return accession;
    }

    public void setAccession(String accession) {
        this.accession = accession;
    }

    public Node getSearchResultNode() {
        return searchResultNode;
    }

    public void setSearchResultNode(Node searchResultNode) {
        this.searchResultNode = searchResultNode;
    }

    public String getAccessionType() {
        return accessionType;
    }

    public void setAccessionType(String accessionType) {
        this.accessionType = accessionType;
    }

    public Long getDescription() {
        return description;
    }

    public void setDescription(Long description) {
        this.description = description;
    }

    public String getReplacedBy() {
        return replacedBy;
    }

    public void setReplacedBy(String replacedBy) {
        this.replacedBy = replacedBy;
    }
}
