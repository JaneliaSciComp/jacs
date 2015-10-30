
package org.janelia.it.jacs.compute.access.search;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Jul 14, 2008
 * Time: 11:15:52 AM
 */
public abstract class CategoryResult implements Serializable {
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
