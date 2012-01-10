
package org.janelia.it.jacs.server.access;

import org.janelia.it.jacs.model.common.BlastTaskVO;
import org.janelia.it.jacs.server.access.hibernate.DaoException;

import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: lfoster
 * Date: Nov 20, 2006
 * Time: 2:21:50 PM
 */
public interface BlastDAO extends DAO {
    List<String> getSiteLocations(String project) throws DaoException;

    Map<String, String> getNodeIdVsSiteLocation(String project) throws DaoException;

    BlastTaskVO getPrepopulatedBlastTask(String taskId) throws DaoException;
}
