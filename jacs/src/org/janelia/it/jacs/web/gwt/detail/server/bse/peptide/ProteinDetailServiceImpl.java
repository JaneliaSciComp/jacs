
package org.janelia.it.jacs.web.gwt.detail.server.bse.peptide;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.model.genomics.ProteinAnnotation;
import org.janelia.it.jacs.model.genomics.ProteinClusterMember;
import org.janelia.it.jacs.server.access.FeatureDAO;
import org.janelia.it.jacs.web.gwt.common.client.service.GWTServiceException;
import org.janelia.it.jacs.web.gwt.common.server.JcviGWTSpringController;
import org.janelia.it.jacs.web.gwt.detail.client.service.protein.ProteinDetailService;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: cgoina
 * Date: Nov 15, 2007
 * Time: 2:30:53 PM
 */
public class ProteinDetailServiceImpl extends JcviGWTSpringController implements ProteinDetailService {
    static transient Logger logger = Logger.getLogger(ProteinDetailServiceImpl.class.getName());

    private transient FeatureDAO featureDAO;

    public void setFeatureDAO(FeatureDAO featureDAO) {
        this.featureDAO = featureDAO;
    }

    public ProteinClusterMember getProteinClusterInfo(String proteinAcc) throws GWTServiceException {
        try {
            return featureDAO.getProteinClusterMemberInfo(proteinAcc);
        }
        catch (Exception e) {
            throw new GWTServiceException(e.toString(), e);
        }
    }

    public List<ProteinAnnotation> getProteinAnnotations(String proteinAcc, SortArgument[] sortArgs) throws GWTServiceException {
        try {
            return featureDAO.getProteinAnnotations(proteinAcc, sortArgs);
        }
        catch (Exception e) {
            throw new GWTServiceException(e.toString(), e);
        }
    }
}
