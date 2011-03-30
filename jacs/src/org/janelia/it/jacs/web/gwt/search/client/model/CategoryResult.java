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

package org.janelia.it.jacs.web.gwt.search.client.model;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Sep 20, 2007
 * Time: 1:05:39 PM
 */
public abstract class CategoryResult implements Serializable, IsSerializable {
    private String accession;
    private String headline;

    /**
     * Returns the search hits for the given categories
     */
    List<DocumentResult> documentResult = new ArrayList<DocumentResult>();

    public String getAccession() {
        return accession;
    }

    public void setAccession(String accession) {
        this.accession = accession;
    }

    /**
     * Returns the search hits for the given categories
     *
     * @return list of document results
     */
    public List<DocumentResult> getDocumentResult() {
        return documentResult;
    }

    /**
     * Returns the search hits for the given categories
     *
     * @param documentResult doc result being set
     */
    public void setDocumentResult(List<DocumentResult> documentResult) {
        this.documentResult = documentResult;
    }

    public void addDocumentResult(DocumentResult dr) {
        this.documentResult.add(dr);
    }

    public String getHeadline() {
        return headline;
    }

    public void setHeadline(String headline) {
        this.headline = headline;
    }

    abstract public String getResultType();
}
