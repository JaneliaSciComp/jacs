
package org.janelia.it.jacs.server.access;

import org.janelia.it.jacs.model.download.MooreOrganism;
import org.janelia.it.jacs.server.access.hibernate.DaoException;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: tdolafi
 * Date: Mar 30, 2006
 * Time: 10:36:03 AM
 */
public interface MooreDAO extends DAO {

    List<MooreOrganism> findAllOrganisms() throws DaoException;

}