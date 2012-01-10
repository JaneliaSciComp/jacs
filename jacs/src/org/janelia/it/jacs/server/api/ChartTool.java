
package org.janelia.it.jacs.server.api;

import org.janelia.it.jacs.web.gwt.common.shared.data.ChartData;
import org.janelia.it.jacs.web.gwt.common.shared.data.ChartDataEntry;
import org.janelia.it.jacs.web.gwt.common.shared.data.CompositeChartDataEntry;
import org.janelia.it.jacs.web.gwt.common.shared.data.ImageModel;

import java.io.File;
import java.util.Collections;
import java.util.List;

/**
 * User: cgoina
 * Implementation of a Chart Tool
 */
abstract public class ChartTool {

    private static int MAX_LABEL_CHARS = 45;
    private static int MIN_LABEL_CHARS = 8;
    private static int MAX_DATA_POINTS = 7;
    private static double MIN_SHARE_OF_TOTAL = 1. / 40.001;

    protected ChartTool() {
    }

    abstract public ImageModel createPieChart(String chartTitle,
                                              ChartData chartValues,
                                              int width,
                                              int height,
                                              String chartBaseDirectory,
                                              String chartRelativeDirectory)
            throws Exception;

    abstract public ImageModel createBarChart(String chartTitle,
                                              String domainLabel,
                                              String rangeLabel,
                                              ChartData chartValues,
                                              int width,
                                              int height,
                                              String chartBaseDirectory,
                                              String chartRelativeDirectory)
            throws Exception;

    public ChartData restructureChartData(ChartData chartValues) {
// if there are too many data points aggregate the smallest points into "others"
        CompositeChartDataEntry otherValues = new CompositeChartDataEntry();
        otherValues.setName("other");
        Double totalValue = chartValues.getTotal().doubleValue();
        ChartData restructuredChartData = new ChartData();
        restructuredChartData.setName(chartValues.getName());
        restructuredChartData.setTotal(totalValue);

// sort the data points
        List<ChartDataEntry> sortedData = chartValues.getChartDataEntries();
        Collections.sort(sortedData);
        Object dataArray[] = sortedData.toArray();

// calculate cutOff point for aggregation into "others" column
        int numDataPoints = sortedData.size();
        int cutOffPoint = numDataPoints - MAX_DATA_POINTS + 1;
        Double cutOff = totalValue * MIN_SHARE_OF_TOTAL;
        if (cutOffPoint > 0)
            if (((ChartDataEntry) dataArray[cutOffPoint - 1]).getValue().doubleValue() > cutOff)
                cutOff = ((ChartDataEntry) dataArray[cutOffPoint - 1]).getValue().doubleValue();

// rebuild chart data with "others" aggregation
        int i = 0;
        while (i < numDataPoints) {
            if (((ChartDataEntry) dataArray[i]).getValue().doubleValue() <= cutOff)
                otherValues.addChartDataEntry((ChartDataEntry) dataArray[i]);
            else
                restructuredChartData.addChartDataEntry((ChartDataEntry) dataArray[i]);
            i++;
        }
        if ((otherValues.getNumberOfEntries() == numDataPoints && numDataPoints <= 6) ||
                otherValues.getNumberOfEntries() == 1)
            restructuredChartData = chartValues;
        else if (otherValues.getNumberOfEntries() > 0) {
            otherValues.setDescription(otherValues.getMembersDescription());
            restructuredChartData.addChartDataEntry(otherValues);
        }

// if there are several slices interleave the large and small labels to help labeling
        numDataPoints = restructuredChartData.getChartDataEntries().size();
        sortedData = restructuredChartData.getChartDataEntries();
        Collections.sort(sortedData);
        dataArray = sortedData.toArray();
        restructuredChartData = new ChartData();
        restructuredChartData.setName(chartValues.getName());
        restructuredChartData.setTotal(totalValue);

        i = numDataPoints - 1;
        while (i >= 0) {
            ChartDataEntry dataPoint = (ChartDataEntry) dataArray[i];

            String name = dataPoint.getName();
            if (name.length() > MAX_LABEL_CHARS) {
                int j = MAX_LABEL_CHARS + 1;
                while (" ,-.;".indexOf(name.charAt(j - 1)) == -1 && j > 0) j--;
                if (j >= MIN_LABEL_CHARS) dataPoint.setName(name.substring(0, j - 1).trim() + " ...");
            }

            Double pctTotal = dataPoint.getValue().doubleValue() / totalValue * 100;
            if (pctTotal < 1) {
                if (pctTotal < 100. / 150.)
                    dataPoint.setValue((Double) totalValue / (Double) 150.);
                dataPoint.setDescription("<1%-" + (dataPoint.getDescription() != null && dataPoint.getDescription().length() > 0 ?
                        dataPoint.getDescription() :
                        name));
            }
            else if (new Double(pctTotal + 0.5).intValue() == 100 && dataPoint.getValue().doubleValue() < totalValue) {
                dataPoint.setDescription(">99%-" + (dataPoint.getDescription() != null && dataPoint.getDescription().length() > 0 ?
                        dataPoint.getDescription() :
                        name));
            }
            else {
                dataPoint.setDescription(String.valueOf(new Double(pctTotal + 0.5).intValue()) + "%-" +
                        (dataPoint.getDescription() != null && dataPoint.getDescription().length() > 0 ?
                                dataPoint.getDescription() :
                                name));
            }
            restructuredChartData.addChartDataEntry(dataPoint);
            i--;
        }

        return restructuredChartData;
    }

    protected File getChartFile(String chartBaseDirectory) throws Exception {
        File chartFile = File.createTempFile("chart", ".png", new File(chartBaseDirectory));
        chartFile.deleteOnExit();
        return chartFile;
    }
}
