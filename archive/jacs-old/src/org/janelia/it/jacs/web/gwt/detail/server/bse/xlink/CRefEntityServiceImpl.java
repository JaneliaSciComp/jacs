
package org.janelia.it.jacs.web.gwt.detail.server.bse.xlink;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.model.genomics.BaseSequenceEntity;
import org.janelia.it.jacs.model.genomics.BseEntityDetail;
import org.janelia.it.jacs.model.genomics.PeptideDetail;
import org.janelia.it.jacs.model.genomics.ScaffoldReadAlignment;
import org.janelia.it.jacs.server.access.FeatureDAO;
import org.janelia.it.jacs.server.access.hibernate.DaoException;
import org.janelia.it.jacs.web.gwt.common.client.service.GWTServiceException;
import org.janelia.it.jacs.web.gwt.common.server.JcviGWTSpringController;
import org.janelia.it.jacs.web.gwt.detail.client.bse.xlink.CRefEntityService;

import java.util.List;

public class CRefEntityServiceImpl extends JcviGWTSpringController
        implements CRefEntityService {

    private static Logger logger =
            Logger.getLogger(CRefEntityServiceImpl.class);

    private FeatureDAO featureDAO;

    public void setFeatureDAO(FeatureDAO dao) {
        featureDAO = dao;
    }

    public int getNumAssembledScaffoldForReadByAccNo(String readAccNo)
            throws GWTServiceException {
        logger.debug("Retrieve the number of related assemblies");
        try {
            return featureDAO.getNumScaffoldsForReadByAccNo(readAccNo);
        }
        catch (DaoException de) {
            logger.error("getNumAssembledScaffoldForReadByAccNo error", de);
            throw new GWTServiceException(de);
        }
    }

    public List<ScaffoldReadAlignment> getAssembledScaffoldForReadByAccNo(String readAccNo, int startIndex, int numRecords, SortArgument[] sortArgs)
            throws GWTServiceException {
        logger.debug("Retrieve related assemblies");
        try {
            List<ScaffoldReadAlignment> assembledScaffolds = featureDAO.getScaffoldsForReadByAccNo(readAccNo, startIndex, numRecords, sortArgs);
            cleanForGWT(assembledScaffolds);
            return assembledScaffolds;
        }
        catch (DaoException de) {
            logger.error("getAssembledScaffoldForReadByAccNo error", de);
            throw new GWTServiceException(de);
        }
    }

    public int getNumRelatedNCRNAs(String entityAccNo)
            throws GWTServiceException {
        logger.debug("Retrieve the number of related ncRNAs");
        try {
            return featureDAO.getNumRelatedNCRNAs(entityAccNo);
        }
        catch (DaoException de) {
            logger.error("getNumRelatedNCRNAs error", de);
            throw new GWTServiceException(de);
        }
    }

    public List<BaseSequenceEntity> getRelatedNCRNAs(String entityAccNo, int startIndex, int numRecords, SortArgument[] sortArgs)
            throws GWTServiceException {
        logger.debug("Retrieve related ncRNAs");
        try {
            List<BaseSequenceEntity> ncRNAs = featureDAO.getRelatedNCRNAs(entityAccNo, startIndex, numRecords, sortArgs);
            cleanForGWT(ncRNAs);
            return ncRNAs;
        }
        catch (DaoException de) {
            logger.error("getRelatedNCRNAs error", de);
            throw new GWTServiceException(de);
        }
    }

    public int getNumRelatedORFs(String entityAccNo)
            throws GWTServiceException {
        logger.debug("Retrieve the number of related ORFs");
        try {
            return featureDAO.getNumRelatedORFs(entityAccNo);
        }
        catch (DaoException de) {
            logger.error("getNumRelatedORFs error", de);
            throw new GWTServiceException(de);
        }
    }

    public List<BaseSequenceEntity> getRelatedORFs(String entityAccNo, int startIndex, int numRecords, SortArgument[] sortArgs)
            throws GWTServiceException {
        logger.debug("Retrieve related ORFs");
        try {
            List<BaseSequenceEntity> orfs = featureDAO.getRelatedORFs(entityAccNo, startIndex, numRecords, sortArgs);
            cleanForGWT(orfs);
            return orfs;
        }
        catch (DaoException de) {
            logger.error("getRelatedORFs error", de);
            throw new GWTServiceException(de);
        }
    }

    public List<BseEntityDetail> getRelatedORFsAndRNAs(String entityAccNo, int startIndex, int numRecords, SortArgument[] sortArgs)
            throws GWTServiceException {
        logger.debug("Retrieve related ORFs and RNAs");
        try {
            List<BseEntityDetail> bseEntityDetailList = featureDAO.getRelatedORFsAndRNAs(entityAccNo, startIndex, numRecords, sortArgs);
            cleanForGWT(bseEntityDetailList);
            return bseEntityDetailList;
        }
        catch (DaoException de) {
            logger.error("getRelatedORFs error", de);
            throw new GWTServiceException(de);
        }
    }

    public int getNumFeatures(String entityAccNo)
            throws GWTServiceException {
        logger.debug("Retrieve the number of related features");
        try {
            return featureDAO.getNumReadFeaturesBySubclassName(new String[]{"ORF", "NonCodingRNA"}, entityAccNo);
        }
        catch (DaoException de) {
            logger.error("getNumFeatures error", de);
            throw new GWTServiceException(de);
        }
    }

    public List<BaseSequenceEntity> getFeatures(String entityAccNo, int startIndex, int numRecords, SortArgument[] sortArgs)
            throws GWTServiceException {
        logger.debug("Retrieve related ORFs");
        try {
            List<BaseSequenceEntity> features = featureDAO.getReadFeaturesBySubclassName(new String[]{"ORF", "NonCodingRNA"},
                    entityAccNo, startIndex, numRecords, sortArgs);
            cleanForGWT(features);
            return features;
        }
        catch (DaoException de) {
            logger.error("getFeatures error", de);
            throw new GWTServiceException(de);
        }
    }

    public int getNumOfReadsForScaffoldByAccNo(String scaffoldAccNo) throws GWTServiceException {
        logger.debug("Retrieve the number of related reads");
        try {
            return featureDAO.getNumOfReadsForScaffoldByAccNo(scaffoldAccNo);
        }
        catch (DaoException de) {
            logger.error("getNumOfReadsForScaffoldByAccNo error", de);
            throw new GWTServiceException(de);
        }
    }

    public List<ScaffoldReadAlignment> getReadsForScaffoldByAccNo(String scaffoldAccNo, int startIndex, int numRecords, SortArgument[] sortArgs) throws GWTServiceException {
        logger.debug("Retrieve related assemblies");
        try {
            List<ScaffoldReadAlignment> assembledScaffolds = featureDAO.getReadsForScaffoldByAccNo(scaffoldAccNo, startIndex, numRecords, sortArgs);
            cleanForGWT(assembledScaffolds);
            return assembledScaffolds;
        }
        catch (DaoException de) {
            logger.error("getReadsForScaffoldByAccNo error", de);
            throw new GWTServiceException(de);
        }
    }

    public int getNumOfPeptidesForScaffoldByAccNo(String scaffoldAccNo) throws GWTServiceException {
        logger.debug("Retrieve the number of related peptides");
        try {
            return featureDAO.getNumOfPeptidesForScaffoldByAccNo(scaffoldAccNo);
        }
        catch (DaoException de) {
            logger.error("getNumOfPeptidesForScaffoldByAccNo error", de);
            throw new GWTServiceException(de);
        }
    }

    public List<PeptideDetail> getPeptidesForScaffoldByAccNo(String scaffoldAccNo, int startIndex, int numRecords, SortArgument[] sortArgs) throws GWTServiceException {
        logger.debug("Retrieve related peptides");
        try {
            List<PeptideDetail> peptides = featureDAO.getPeptidesForScaffoldByAccNo(scaffoldAccNo, startIndex, numRecords, sortArgs);
            cleanForGWT(peptides);
            return peptides;
        }
        catch (DaoException de) {
            logger.error("getPeptidesForScaffoldByAccNo error", de);
            throw new GWTServiceException(de);
        }
    }

}
