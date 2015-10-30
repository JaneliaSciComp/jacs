
package org.janelia.it.jacs.web.gwt.search.client.model;

import org.janelia.it.jacs.model.tasks.search.SearchTask;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Sep 20, 2007
 * Time: 1:15:37 PM
 */
public class AccessionResult extends CategoryResult {
    String accessionType;
    String description; // can be customized for each accession type

    public String getResultType() {
        return SearchTask.TOPIC_ACCESSION;
    }

    public String getAccessionType() {
        return accessionType;
    }

    public void setAccessionType(String accessionType) {
        this.accessionType = accessionType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

}
