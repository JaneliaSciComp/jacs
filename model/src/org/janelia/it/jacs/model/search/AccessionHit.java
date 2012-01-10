
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
