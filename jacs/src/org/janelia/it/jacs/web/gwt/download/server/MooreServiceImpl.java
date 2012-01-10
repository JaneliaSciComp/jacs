
package org.janelia.it.jacs.web.gwt.download.server;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.model.download.MooreOrganism;
import org.janelia.it.jacs.server.access.MooreDAO;
import org.janelia.it.jacs.server.access.hibernate.DaoException;
import org.janelia.it.jacs.web.gwt.common.server.JcviGWTSpringController;
import org.janelia.it.jacs.web.gwt.download.client.MooreService;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * This class is used by GWT client to retrieve data objects needed for detail UI
 *
 * @author Tareq Nabeel
 */
public class MooreServiceImpl extends JcviGWTSpringController implements MooreService {

    private static Logger logger = Logger.getLogger(MooreServiceImpl.class);

    private MooreDAO mooreDAO;

    public void setMooreDAO(MooreDAO mooreDAO) {
        this.mooreDAO = mooreDAO;
    }

    public List getOrganisms() {
        try {
            List loadedOrganismList = mooreDAO.findAllOrganisms();
            logger.info("loadedOrganismList.size()=" + loadedOrganismList.size());
            Collections.sort(loadedOrganismList, new OrganismNameComparator());
            cleanForGWT(loadedOrganismList);
            return loadedOrganismList;
        }
        catch (DaoException e) {
            logger.error("Unexpected exception in MooreServiceImpl.getOrganisms: ", e);
            throw new RuntimeException(e);
        }
    }

    private class OrganismNameComparator implements Comparator<MooreOrganism> {
        public int compare(MooreOrganism o1, MooreOrganism o2) {
            return o1.getOrganismName().compareTo(o2.getOrganismName());
        }
    }
}