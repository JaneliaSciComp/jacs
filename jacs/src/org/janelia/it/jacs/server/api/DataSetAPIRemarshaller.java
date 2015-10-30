
package org.janelia.it.jacs.server.api;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.model.genomics.BaseSequenceEntity;
import org.janelia.it.jacs.model.genomics.Read;
import org.janelia.it.jacs.model.genomics.Sample;
import org.janelia.it.jacs.shared.node.FastaUtil;
import org.janelia.it.jacs.web.gwt.common.client.model.genomics.BlastHitWithSample;
import org.janelia.it.jacs.web.gwt.common.client.model.genomics.GenericDNA;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Michael Press
 */
public class DataSetAPIRemarshaller {
    private static Logger _logger = Logger.getLogger(DataSetAPIRemarshaller.class);

    /*
    *  @param results List<Object[]> where Object[<BlastHit>][<String queryDefline>][<String subjectDefline>]
    */
    public static List<org.janelia.it.jacs.web.gwt.common.client.model.genomics.BlastHit> createClientBlastHits(
            List<Object[]> results) {
        List<org.janelia.it.jacs.web.gwt.common.client.model.genomics.BlastHit> hits = new ArrayList<org.janelia.it.jacs.web.gwt.common.client.model.genomics.BlastHit>();
        for (Object[] row : results) {
            org.janelia.it.jacs.model.genomics.BlastHit modelHit = (org.janelia.it.jacs.model.genomics.BlastHit) row[0];
            String queryDefline = (String) row[1];
            String subjectDefline = (String) row[2];
            try {
                // Get the modelRead and site metadata (for reads) or defline (for general sequences)
                if (isSampleAvailable(modelHit)) {
                    hits.add(createBlastHitWithSample(modelHit, queryDefline, subjectDefline));
                }
                else {
                    hits.add(createBlastHitSequence(modelHit, queryDefline, subjectDefline));
                }
            }
            catch (Exception e) {
                _logger.error("Error converting BlastHit: ", e);
            }
        }
        return hits;
    }

    private static org.janelia.it.jacs.web.gwt.common.client.model.genomics.BlastHit createBlastHitWithSample(
            org.janelia.it.jacs.model.genomics.BlastHit modelHit, String queryDefline, String subjectDefline)
            throws Exception {
        // Create client-side Sample
        Sample modelSample = modelHit.getSubjectEntity().getSample();
        org.janelia.it.jacs.web.gwt.common.client.model.metadata.Sample sample =
                new org.janelia.it.jacs.web.gwt.common.client.model.metadata.Sample();
//        sample.setObjectId(modelSample.getSampleId());
//        sample.setSampleName(modelSample.getSampleName());
//        sample.setSites(getSites(modelSample));
//        sample.setSampleAcc(modelSample.getSampleAcc());

        // Create a client-side query entity
        org.janelia.it.jacs.web.gwt.common.client.model.genomics.BaseSequenceEntity subjectEntity =
                createSubjectSequenceEntity(modelHit, subjectDefline);

        // Create a client-side subject entity
        org.janelia.it.jacs.web.gwt.common.client.model.genomics.BaseSequenceEntity queryEntity =
                createQuerySequenceEntity(modelHit, queryDefline);

        // Create a client-side BlastHit with the Read and Sample
        BlastHitWithSample bhWithSample = new BlastHitWithSample(modelHit.getBlastHitId(),
                modelHit.getSubjectBegin(), modelHit.getSubjectEnd(), modelHit.getSubjectOrientation(), modelHit.getQueryBegin(),
                modelHit.getQueryEnd(), modelHit.getQueryOrientation(), modelHit.getBitScore(), formatBitScore(modelHit.getBitScore()), modelHit.getHspScore(),
                modelHit.getExpectScore(), formatEval(modelHit.getExpectScore()), modelHit.getComment(), modelHit.getLengthAlignment(),
                modelHit.getEntropy(), modelHit.getNumberIdentical(), modelHit.getNumberSimilar(), modelHit.getSubjectLength(),
                modelHit.getSubjectGaps(), modelHit.getSubjectGapRuns(), modelHit.getSubjectStops(), modelHit.getSubjectNumberUnalignable(),
                modelHit.getSubjectFrame(), modelHit.getQueryLength(), modelHit.getQueryGaps(), modelHit.getQueryGapRuns(),
                modelHit.getQueryStops(), modelHit.getQueryNumberUnalignable(), modelHit.getQueryFrame(), modelHit.getSubjectAlignString(),
                modelHit.getMidline(), modelHit.getQueryAlignString(), modelHit.getPairWiseAlignment(40, /*midline*/false, /*color*/true),
                modelHit.getPairWiseAlignment(80, /*midline*/true, /*color*/true),
                queryEntity, subjectEntity, sample);

        if (modelHit.getSubjectEntity() instanceof Read) {
            Read modelSubjectRead = (Read) modelHit.getSubjectEntity();
            bhWithSample.setClearRangeBegin(modelSubjectRead.getClearRangeBegin());
            bhWithSample.setClearRangeEnd(modelSubjectRead.getClearRangeEnd());
        }

        return bhWithSample;
    }

    /**
     * Formats the expect score using scientific notation (1.234E-5) if there are more than 2 0's after the decimal
     */
    private static String formatEval(Double expectScore) {
        if (expectScore == 0)
            return "0.000";
        if (expectScore < 0.01)
            return new DecimalFormat("0.###E0").format(expectScore);
        else
            return new DecimalFormat("0.###").format(expectScore);
    }

    /**
     * Formats the bit score using to have 1 digita after decimal  point.
     */
    private static String formatBitScore(Float bitScore) {
        return new DecimalFormat("0.0").format(bitScore);
    }

    private static org.janelia.it.jacs.web.gwt.common.client.model.genomics.BlastHit createBlastHitSequence(
            org.janelia.it.jacs.model.genomics.BlastHit modelHit, String queryDefline, String subjectDefline) throws Exception {
        return new org.janelia.it.jacs.web.gwt.common.client.model.genomics.BlastHit(modelHit.getBlastHitId(),
                modelHit.getSubjectBegin(), modelHit.getSubjectEnd(), modelHit.getSubjectOrientation(), modelHit.getQueryBegin(),
                modelHit.getQueryEnd(), modelHit.getQueryOrientation(), modelHit.getBitScore(), formatBitScore(modelHit.getBitScore()), modelHit.getHspScore(),
                modelHit.getExpectScore(), formatEval(modelHit.getExpectScore()), modelHit.getComment(), modelHit.getLengthAlignment(),
                modelHit.getEntropy(), modelHit.getNumberIdentical(), modelHit.getNumberSimilar(), modelHit.getSubjectLength(),
                modelHit.getSubjectGaps(), modelHit.getSubjectGapRuns(), modelHit.getSubjectStops(), modelHit.getSubjectNumberUnalignable(),
                modelHit.getSubjectFrame(), modelHit.getQueryLength(), modelHit.getQueryGaps(), modelHit.getQueryGapRuns(),
                modelHit.getQueryStops(), modelHit.getQueryNumberUnalignable(), modelHit.getQueryFrame(), modelHit.getSubjectAlignString(),
                modelHit.getMidline(), modelHit.getQueryAlignString(), modelHit.getPairWiseAlignment(40, /*midline*/false, /*color*/true),
                modelHit.getPairWiseAlignment(80, /*midline*/true, /*color*/true),
                createQuerySequenceEntity(modelHit, queryDefline),
                createSubjectSequenceEntity(modelHit, subjectDefline));
    }

    private static org.janelia.it.jacs.web.gwt.common.client.model.genomics.GenericDNA createGenericSequenceEntity(
            BaseSequenceEntity modelEntity, String defline) {
        org.janelia.it.jacs.web.gwt.common.client.model.genomics.GenericDNA seq = new GenericDNA();
        seq.setAccession(modelEntity.getAccession());
        seq.setDescription(modelEntity.getDescription());
        seq.setDescriptionFormatted(FastaUtil.wrapDeflineAsHTML(modelEntity.getDescription(), /*chars/line*/80, /*indent*/3));
        seq.setEntityId(modelEntity.getEntityId());
        seq.setExternalAcc(modelEntity.getExternalAcc());
        seq.setSeqLength(modelEntity.getSequenceLength());
        seq.setDefline(defline);
        seq.setDeflineFormatted(FastaUtil.wrapDeflineAsHTML(defline, /*chars/line*/80, /*indent*/3));

        return seq;
    }

    private static org.janelia.it.jacs.web.gwt.common.client.model.genomics.GenericDNA createQuerySequenceEntity(
            org.janelia.it.jacs.model.genomics.BlastHit modelHit, String defline) {
        if (modelHit.getQueryEntity() != null) {
            return createGenericSequenceEntity(modelHit.getQueryEntity(), defline);
        }
        // if no query entity is present set as much as possible from the hit object
        org.janelia.it.jacs.web.gwt.common.client.model.genomics.GenericDNA seq = new GenericDNA();
        seq.setAccession(modelHit.getQueryAcc());
        seq.setSeqLength(modelHit.getQueryLength());
        seq.setDefline(defline);
        seq.setDescription(defline);
        seq.setDeflineFormatted(FastaUtil.wrapDeflineAsHTML(defline, /*chars/line*/80, /*indent*/3));
        seq.setDescriptionFormatted(FastaUtil.wrapDeflineAsHTML(defline, /*chars/line*/80, /*indent*/3));
        return seq;
    }

    private static org.janelia.it.jacs.web.gwt.common.client.model.genomics.GenericDNA createSubjectSequenceEntity(
            org.janelia.it.jacs.model.genomics.BlastHit modelHit, String defline) {
        if (modelHit.getSubjectEntity() != null) {
            return createGenericSequenceEntity(modelHit.getSubjectEntity(), defline);
        }
        // if no query entity is present set as much as possible from the hit object
        org.janelia.it.jacs.web.gwt.common.client.model.genomics.GenericDNA seq = new GenericDNA();
        seq.setAccession(modelHit.getSubjectAcc());
        seq.setSeqLength(modelHit.getSubjectLength());
        seq.setDefline(defline);
        seq.setDescription(defline);
        seq.setDeflineFormatted(FastaUtil.wrapDeflineAsHTML(defline, /*chars/line*/80, /*indent*/3));
        seq.setDescriptionFormatted(FastaUtil.wrapDeflineAsHTML(defline, /*chars/line*/80, /*indent*/3));
        return seq;
    }

    private static boolean isSampleAvailable(org.janelia.it.jacs.model.genomics.BlastHit hit) {
        BaseSequenceEntity bse = hit.getSubjectEntity();
        if (bse != null && bse.getSample() != null) {
            return true;
        }
        else {
            return false;
        }
    }

//    private static Set<org.janelia.it.jacs.web.gwt.common.client.model.metadata.Site> getSites(Sample sample) {
//        Set sites = new HashSet();
//        try {
//            if (sample != null) {
//                for (Object modelSite : sample.getBioMaterials()) {
//                    sites.add(getSiteFromModelSite((BioMaterial) modelSite));
//                }
//            }
//        }
//        catch (Exception e) {
//            _logger.error("Error getting sites for sample " + ((sample == null) ? "null" : sample.getSampleAcc()));
//        }
//        return sites;
//    }
//
//    // TODO: sync with DbPublicationHelper
//    private static org.janelia.it.jacs.web.gwt.common.client.model.metadata.Site getSiteFromModelSite(BioMaterial modelSite) {
//        org.janelia.it.jacs.web.gwt.common.client.model.metadata.Site site = new org.janelia.it.jacs.web.gwt.common.client.model.metadata.Site();
//        site.setSiteId(modelSite.getMaterialAcc());
//        site.setBiomass(modelSite.getObservationAsString("biomass"));
////        site.setChlorophyllDensity(modelSite.getObservationAsString("chlorophyll density"));
//        if (modelSite.getObservationAsString("chlorophyll density").length() > 0)
//            site.setChlorophyllDensity(modelSite.getObservationAsString("chlorophyll density"));
//        else if (modelSite.getObservationAsString("chlorophyll density/sample month").length() > 0)
//            site.setChlorophyllDensity(modelSite.getObservationAsString("chlorophyll density/sample month"));
//        else if (modelSite.getObservationAsString("chlorophyll density/annual").length() > 0)
//            site.setChlorophyllDensity(modelSite.getObservationAsString("chlorophyll density/annual"));
//        else site.setChlorophyllDensity("");
//        site.setCountry(((GeoPoint) modelSite.getCollectionSite()).getCountry());
//        site.setDissolvedInorganicCarbon(modelSite.getObservationAsString("dissolved inorganic carbon"));
//        site.setDissolvedInorganicPhospate(modelSite.getObservationAsString("dissolved inorganic phosphate"));
//        site.setDissolvedOrganicCarbon(modelSite.getObservationAsString("dissolved organic carbon"));
//        site.setDissolvedOxygen(modelSite.getObservationAsString("dissolved oxygen"));
//        site.setFluorescence(modelSite.getObservationAsString("fluorescence"));
//        site.setGeographicLocation(modelSite.getCollectionSite().getRegion());
//        site.setGeographicLocationDetail(modelSite.getCollectionSite().getComment());
//        site.setHabitatType(modelSite.getCollectionSite().getSiteDescription());
//        if (modelSite.getCollectionSite() != null) {
//            site.setLatitude(((GeoPoint) modelSite.getCollectionSite()).getLatitude());
//            site.setLatitudeDouble(((GeoPoint) modelSite.getCollectionSite()).getLatitudeAsDouble());
//            site.setLongitude(((GeoPoint) modelSite.getCollectionSite()).getLongitude());
//            site.setLongitudeDouble(((GeoPoint) modelSite.getCollectionSite()).getLongitudeAsDouble());
//        }
//        if (modelSite.getCollectionHost() != null) {
//            site.setHostOrganism(modelSite.getCollectionHost().getOrganism());
//            site.setHostDetails(modelSite.getCollectionHost().getHostDetails());
//        }
//        site.setNitrate_plus_nitrite(modelSite.getObservationAsString("nitrate+nitrite"));
//        site.setNumberOfSamplesPooled(modelSite.getObservationAsString("number of samples pooled"));
//        site.setNumberOfStationsSampled(modelSite.getObservationAsString("number of stations sampled"));
//        site.setProject(modelSite.getProject());
//        site.setSalinity(modelSite.getObservationAsString("salinity"));
//        site.setSampleDepth(((GeoPoint) modelSite.getCollectionSite()).getDepth());
//        site.setSampleLocation(modelSite.getCollectionSite().getLocation());
//        if (modelSite.getCollectionStartTime() != null)
//            site.setStartTime(new Date(modelSite.getCollectionStartTime().getTime()));
//        if (modelSite.getCollectionStopTime() != null)
//            site.setStopTime(new Date(modelSite.getCollectionStopTime().getTime()));
//        site.setTemperature(modelSite.getObservationAsString("temperature"));
//        site.setTransmission(modelSite.getObservationAsString("transmission"));
//        site.setVolume_filtered(modelSite.getObservationAsString("volume filtered"));
//        site.setWaterDepth(modelSite.getObservationAsString("water depth"));
//        return site;
//    }
//
//    public static Map<org.janelia.it.jacs.web.gwt.common.client.model.metadata.Site, Integer> remapSites(
//            Map<BioMaterial, Integer> modelSites) {
//        _logger.debug("DataSetAPIRemarshaller().remapSites()");
//        if (modelSites == null) {
//            return null;
//        }
//        Map<org.janelia.it.jacs.web.gwt.common.client.model.metadata.Site, Integer> sites =
//                new HashMap<org.janelia.it.jacs.web.gwt.common.client.model.metadata.Site, Integer>();
//        for (Object item : modelSites.keySet()) {
//            BioMaterial modelSite = (BioMaterial) item;
//            sites.put(getSiteFromModelSite(modelSite), modelSites.get(modelSite));
//        }
//        if (_logger.isDebugEnabled()) _logger.debug("remapSites() converted " + sites.size() + " sites");
//        return sites;
//    }

}
