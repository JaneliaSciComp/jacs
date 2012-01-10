
package org.janelia.it.jacs.web.gwt.status.client.panel;

import com.google.gwt.user.client.Command;

import java.util.List;

/**
 * @author Cristian Goina
 */
public interface BlastResultsExportCommand extends Command {

    public void exportResults(List selectedIDsList);

}
