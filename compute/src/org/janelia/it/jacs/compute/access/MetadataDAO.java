
package org.janelia.it.jacs.compute.access;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.janelia.it.jacs.model.metadata.Sample;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Jul 11, 2008
 * Time: 4:46:28 PM
 */
public class MetadataDAO extends ComputeBaseDAO {

    public MetadataDAO(Logger logger) {
        super(logger);
    }

    /**
     * This method returns a Sample object with their sites loaded for the given sampleAcc.
     *
     * @param sampleAcc
     * @return the sample with the given accession
     * @throws DaoException
     */
    public Sample getSampleWithSitesBySampleAcc(String sampleAcc) throws DaoException {
        try {
            Query query = getSession().createQuery("select sample from Sample sample where sample.sampleAcc = '" + sampleAcc + "'");
            List samples = query.list();
            return samples != null && samples.size() > 0 ? (Sample) samples.get(0) : null;
        }
        catch (Exception e) {
            // No need to be granular with exception handling since we're going to wrap 'em all in DaoException
            throw handleException(e, this.getClass().getName() + " - getSamples");
        }
    }

//    protected Object findByNamedQueryAndNamedParam(String hqlQueryName, String paramName, Object paramValue, boolean uniqueResult) {


}
