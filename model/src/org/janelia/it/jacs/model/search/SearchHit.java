
package org.janelia.it.jacs.model.search;

import com.google.gwt.user.client.rpc.IsSerializable;
import org.janelia.it.jacs.model.user_data.Node;

import java.io.Serializable;

public class SearchHit implements Serializable, IsSerializable {

    // Fields
    private Node searchResultNode;
    private String accession;
    private Long documentId;
    private String documentName;
    private String documentType;
    private String headline;

    // Constructors

    /**
     * default constructor
     */
    public SearchHit() {
    }

    /**
     * full constructor
     */
    public SearchHit(Long documentId,
                     String accession,
                     String documentName,
                     String documentType,
                     String headline,
                     Node searchResultNode) {
        this.documentId = documentId;
        this.accession = accession;
        this.documentName = documentName;
        this.documentType = documentType;
        this.headline = headline;
        this.searchResultNode = searchResultNode;
    }

    public Long getDocumentId() {
        return documentId;
    }

    public void setDocumentId(Long documentId) {
        this.documentId = documentId;
    }

    public String getAccession() {
        return accession;
    }

    public void setAccession(String accession) {
        this.accession = accession;
    }

    public String getDocumentName() {
        return documentName;
    }

    public void setDocumentName(String documentName) {
        this.documentName = documentName;
    }

    public Node getSearchResultNode() {
        return searchResultNode;
    }

    public void setSearchResultNode(Node searchResultNode) {
        this.searchResultNode = searchResultNode;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public String getHeadline() {
        return headline;
    }

    public void setHeadline(String headline) {
        this.headline = headline;
    }
}
