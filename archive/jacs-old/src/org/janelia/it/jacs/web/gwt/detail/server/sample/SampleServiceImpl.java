
package org.janelia.it.jacs.web.gwt.detail.server.sample;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.model.genomics.Read;
import org.janelia.it.jacs.server.access.FeatureDAO;
import org.janelia.it.jacs.web.gwt.common.client.service.GWTServiceException;
import org.janelia.it.jacs.web.gwt.common.server.JcviGWTSpringController;
import org.janelia.it.jacs.web.gwt.detail.client.service.sample.SampleService;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: cgoina
 * Date: Nov 15, 2007
 * Time: 2:30:53 PM
 */
public class SampleServiceImpl extends JcviGWTSpringController implements SampleService {
    static transient Logger logger = Logger.getLogger(SampleServiceImpl.class.getName());

    private transient FeatureDAO featureDAO;

    public void setFeatureDAO(FeatureDAO featureDAO) {
        this.featureDAO = featureDAO;
    }

    public Integer getNumSampleReads(String sampleAcc) throws GWTServiceException {
        try {
            return featureDAO.getNumReadsFromSample(sampleAcc);
        }
        catch (Exception e) {
            throw new GWTServiceException(e.toString(), e);
        }
    }

    public List<Read> getPagedSampleReads(String sampleAcc, int startIndex, int numRows, SortArgument[] sortArgs) throws GWTServiceException {
        try {
            List<Read> sampleReads = featureDAO.getPagedReadsFromSample(sampleAcc, null, startIndex, numRows, sortArgs);
            cleanForGWT(sampleReads);
            return sampleReads;
        }
        catch (Exception e) {
            throw new GWTServiceException(e.toString(), e);
        }
    }

}