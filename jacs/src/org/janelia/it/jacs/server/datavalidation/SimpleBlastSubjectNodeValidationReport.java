
package org.janelia.it.jacs.server.datavalidation;

import java.util.ArrayList;
import java.util.List;

/**
 * Test program for validating blastable subject nodes
 */
public class SimpleBlastSubjectNodeValidationReport extends ValidationReport {

    private List<String> content;

    public SimpleBlastSubjectNodeValidationReport() {
        content = new ArrayList<String>();
    }

    List<String> getContent() {
        return content;
    }

    void addMessage(String message) {
        if (message != null) {
            content.add(message);
        }
    }

}
