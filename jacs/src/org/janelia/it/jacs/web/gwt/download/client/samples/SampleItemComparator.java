
package org.janelia.it.jacs.web.gwt.download.client.samples;

import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.web.gwt.common.client.model.download.DownloadableDataNode;
import org.janelia.it.jacs.web.gwt.common.client.model.metadata.Sample;
import org.janelia.it.jacs.web.gwt.common.client.model.metadata.SampleItem;
import org.janelia.it.jacs.web.gwt.common.client.model.metadata.Site;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.comparables.NumericString;

import java.util.Comparator;
import java.util.Date;

/**
 * This class does server-side sorting of all kinds of sample metadata.  The implementation is horrible, but
 * better than trying to do it in sql, and unfortunately since table sorting is done using String[][] we can't
 * even use ints or enums here....
 *
 * @author Michael Press
 */
public class SampleItemComparator implements Comparator {
    private SortArgument _sortArg;

    //TODO: sync these constants with the ones in SampleMetadataTablePanel
    private static final String SORT_BY_PROJECT = "Project";
    private final static String SORT_BY_DATASET = "Sample Dataset";
    private final static String SORT_BY_FILE_SIZE = "File Size";
    private final static String SORT_BY_HABITAT = "Habitat Type";
    private final static String SORT_BY_GEOGRAPHIC_LOCATION = "Geographic Location";
    private final static String SORT_BY_SAMPLE_LOCATION = "Sample Location";
    private final static String SORT_BY_COUNTRY = "Country";
    private final static String SORT_BY_FILTER_SIZE = "Filter Size";
    private final static String SORT_BY_LAT = "Latitude";
    private final static String SORT_BY_LONG = "Longitude";
    private final static String SORT_BY_SAMPLE_DEPTH = "Depth";
    private final static String SORT_BY_WATER_DEPTH = "Wat. Dep.";
    private final static String SORT_BY_CHLOROPHYLL = "Chlorophyll";
    private final static String SORT_BY_OXYGEN = "Oxygen";
    private final static String SORT_BY_FLOURESCENCE = "Fluor.";
    private final static String SORT_BY_SALINITY = "Salin.";
    private final static String SORT_BY_TEMP = "Temp";
    private final static String SORT_BY_TRANSMISSION = "Trans.";
    private final static String SORT_BY_BIOMASS = "BioMass";
    private final static String SORT_BY_INORG_CARBON = "Inorg. Carbon";
    private final static String SORT_BY_INORG_PHOSPHATE = "Inorg. Phospate";
    private final static String SORT_BY_ORG_CARBON = "Org. Carbon";
    private final static String SORT_BY_NITRITE = "Nitr.";
    private final static String SORT_BY_NUM_POOLED = "# Pooled";
    private final static String SORT_BY_NUM_SAMPLED = "# Sampled";
    private final static String SORT_BY_VOLUME = "Volume";
    private final static String SORT_BY_COLLECTION_DATE = "Coll. Date";

    public SampleItemComparator(SortArgument sortArg) {
        _sortArg = sortArg;
    }

    public int compare(Object obj1, Object obj2) {
        int value = compareInternal((SampleItem) obj1, (SampleItem) obj2);

        // if we have to sort in descending order we simply reverse the comparison result
        if (_sortArg.getSortDirection() == SortArgument.SORT_DESC)
            value *= -1; // reverse the result

        return value;
    }

    private int compareInternal(SampleItem sample1, SampleItem sample2) {
        if (sample1 == null && sample2 == null) return 0;
        else if (sample1 == null) return 1;
        else if (sample2 == null) return -1;
        else if (_sortArg == null) return 0;
        if (_sortArg.getSortArgumentName().equals(SORT_BY_PROJECT))
            return compareStrings(sample1.getProject(), sample2.getProject());
        else if (_sortArg.getSortArgumentName().equals(SORT_BY_DATASET))
            return compareDataFile(sample1, sample2);
        else if (_sortArg.getSortArgumentName().equals(SORT_BY_FILE_SIZE))
            return compareDataFileSize(sample1.getDataFile(), sample2.getDataFile());
        else if (_sortArg.getSortArgumentName().equals(SORT_BY_FILTER_SIZE))
            return compareDoubles(sample1.getSample().getFilterMin(), sample2.getSample().getFilterMin());
        else /* Rest are Site fields */
            return compareSiteField(sample1.getSite(), sample2.getSite());
    }

    private int compareSiteField(Site site1, Site site2) {
        if (site1 == null && site2 == null) return 0;
        else if (site1 == null) return -1;
        else if (site2 == null) return 1;

        if (_sortArg.getSortArgumentName().equals(SORT_BY_HABITAT))
            return compareStrings(site1.getHabitatType(), site2.getHabitatType());
        else if (_sortArg.getSortArgumentName().equals(SORT_BY_GEOGRAPHIC_LOCATION))
            return compareStrings(site1.getGeographicLocation(), site2.getGeographicLocation());
        else if (_sortArg.getSortArgumentName().equals(SORT_BY_SAMPLE_LOCATION))
            return compareStrings(site1.getSampleLocation(), site2.getSampleLocation());
        else if (_sortArg.getSortArgumentName().equals(SORT_BY_COUNTRY))
            return compareStrings(site1.getCountry(), site2.getCountry());
        else if (_sortArg.getSortArgumentName().equals(SORT_BY_LAT))
            return compareDoubles(site1.getLatitudeDouble(), site2.getLatitudeDouble());
        else if (_sortArg.getSortArgumentName().equals(SORT_BY_LONG))
            return compareDoubles(site1.getLongitudeDouble(), site2.getLongitudeDouble());
        else if (_sortArg.getSortArgumentName().equals(SORT_BY_SAMPLE_DEPTH))
            return compareNumericStrings(site1.getSampleDepth(), site2.getSampleDepth());
        else if (_sortArg.getSortArgumentName().equals(SORT_BY_WATER_DEPTH))
            return compareNumericStrings(site1.getWaterDepth(), site2.getWaterDepth());
        else if (_sortArg.getSortArgumentName().equals(SORT_BY_CHLOROPHYLL))
            return compareNumericStrings(site1.getChlorophyllDensity(), site2.getChlorophyllDensity());
        else if (_sortArg.getSortArgumentName().equals(SORT_BY_OXYGEN))
            return compareNumericStrings(site1.getDissolvedOxygen(), site2.getDissolvedOxygen());
        else if (_sortArg.getSortArgumentName().equals(SORT_BY_FLOURESCENCE))
            return compareNumericStrings(site1.getFluorescence(), site2.getFluorescence());
        else if (_sortArg.getSortArgumentName().equals(SORT_BY_SALINITY))
            return compareNumericStrings(site1.getSalinity(), site2.getSalinity());
        else if (_sortArg.getSortArgumentName().equals(SORT_BY_TEMP))
            return compareNumericStrings(site1.getTemperature(), site2.getTemperature());
        else if (_sortArg.getSortArgumentName().equals(SORT_BY_TRANSMISSION))
            return compareNumericStrings(site1.getTransmission(), site2.getTransmission());
        else if (_sortArg.getSortArgumentName().equals(SORT_BY_BIOMASS))
            return compareNumericStrings(site1.getBiomass(), site2.getBiomass());
        else if (_sortArg.getSortArgumentName().equals(SORT_BY_INORG_CARBON))
            return compareNumericStrings(site1.getDissolvedInorganicCarbon(), site2.getDissolvedInorganicCarbon());
        else if (_sortArg.getSortArgumentName().equals(SORT_BY_INORG_PHOSPHATE))
            return compareNumericStrings(site1.getDissolvedInorganicPhospate(), site2.getDissolvedInorganicPhospate());
        else if (_sortArg.getSortArgumentName().equals(SORT_BY_ORG_CARBON))
            return compareNumericStrings(site1.getDissolvedOrganicCarbon(), site2.getDissolvedOrganicCarbon());
        else if (_sortArg.getSortArgumentName().equals(SORT_BY_NITRITE))
            return compareNumericStrings(site1.getNitrate_plus_nitrite(), site2.getNitrate_plus_nitrite());
        else if (_sortArg.getSortArgumentName().equals(SORT_BY_NUM_POOLED))
            return compareNumericStrings(site1.getNumberOfSamplesPooled(), site2.getNumberOfSamplesPooled());
        else if (_sortArg.getSortArgumentName().equals(SORT_BY_NUM_SAMPLED))
            return compareNumericStrings(site1.getNumberOfStationsSampled(), site2.getNumberOfStationsSampled());
        else if (_sortArg.getSortArgumentName().equals(SORT_BY_VOLUME))
            return compareNumericStrings(site1.getVolume_filtered(), site2.getVolume_filtered());
        else if (_sortArg.getSortArgumentName().equals(SORT_BY_COLLECTION_DATE))
            return compareDates(site1.getStartTime(), site2.getStartTime());
        else // unknown field
            return 0;
    }

    private int compareStrings(String s1, String s2) {
        if (s1 == null && s2 == null) return 0;
        else if (s1 == null) return -1;
        else if (s2 == null) return 1;
        else return s1.compareTo(s2);
    }

    private int compareNumericStrings(String s1, String s2) {
        if (s1 == null && s2 == null) return 0;
        else if (s1 == null) return -1;
        else if (s2 == null) return 1;
        else return new NumericString(s1).compareTo(new NumericString(s2));
    }

    private int compareLongs(Long l1, Long l2) {
        if (l1 == null && l2 == null) return 0;
        else if (l1 == null) return -1;
        else if (l2 == null) return 1;
        else return l1.compareTo(l2);
    }

    private int compareDoubles(Double d1, Double d2) {
        if (d1 == null && d2 == null) return 0;
        else if (d1 == null) return -1;
        else if (d2 == null) return 1;
        else return d1.compareTo(d2);
    }

    private int compareDates(Date d1, Date d2) {
        if (d1 == null && d2 == null) return 0;
        else if (d1 == null) return -1;
        else if (d2 == null) return 1;
        else return d1.compareTo(d2);
    }

    private int compareDataFile(SampleItem item1, SampleItem item2) {
        return compareStrings(getDataFileDisplayString(item1), getDataFileDisplayString(item2));
    }

    /**
     * Horrible hack copied from SampleMetadataTablePanel
     */
    private String getDataFileDisplayString(SampleItem item) {
        if (item.getDataFile() != null)
            return item.getDataFile().getAttribute("Description");
        else {
            // See if it's an MF-150 sample
            Sample sample = item.getSample();
            if (sample != null && sample.getSampleName() != null && sample.getSampleName().startsWith("MF_")) {
                String title = sample.getSampleTitle();
                return title.substring(title.indexOf('-') + 1).trim();
            }
            else if (sample != null)
                return sample.getSampleName();
            else
                return "";
        }
    }

    private int compareDataFileSize(DownloadableDataNode dataFile1, DownloadableDataNode dataFile2) {
        return compareLongs(
                (dataFile1 == null ? null : dataFile1.getSize()),
                (dataFile2 == null ? null : dataFile2.getSize())
        );
    }
}
