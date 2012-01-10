
package org.janelia.it.jacs.server.api;

import org.janelia.it.jacs.web.gwt.common.client.Constants;
import org.janelia.it.jacs.web.gwt.common.shared.data.ChartData;
import org.janelia.it.jacs.web.gwt.common.shared.data.ChartDataEntry;
import org.janelia.it.jacs.web.gwt.common.shared.data.ImageAreaModel;
import org.janelia.it.jacs.web.gwt.common.shared.data.ImageModel;
import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.entity.StandardEntityCollection;
import org.jfree.chart.labels.*;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.general.PieDataset;
import org.jfree.ui.TextAnchor;

import java.awt.*;
import java.io.File;
import java.text.NumberFormat;
import java.util.List;

/**
 * User: cgoina
 * Implementation of a Chart Tool based on JFreeChart
 */
public class JFreeChartTool extends ChartTool {
    public static final int DEFAULT_WIDTH = 250;
    public static final int DEFAULT_HEIGHT = 175;

    protected static Font DEFAULT_TITLE_FONT = new Font("SansSerif", Font.PLAIN, 14);
    protected static Font DEFAULT_LABEL_FONT = new Font("SansSerif", Font.PLAIN, 9);

    static class PercentCategoryItemLabelGenerator extends StandardCategoryItemLabelGenerator {
        private ChartData chartData;
        private NumberFormat percentFormatter;

        public PercentCategoryItemLabelGenerator() {
            this(null);
        }

        PercentCategoryItemLabelGenerator(ChartData chartData) {
            super();
            this.chartData = chartData;
            percentFormatter = NumberFormat.getPercentInstance();
        }

        public ChartData getChartData() {
            return chartData;
        }

        public void setChartData(ChartData chartData) {
            this.chartData = chartData;
        }

        public String generateLabel(CategoryDataset dataset, int row, int column) {
            ChartDataEntry chartEntry = chartData.getChartEntry(column);
            Number total = chartData.getTotal();
            if (total == null) {
                return null;
            }
            else {
                double percent = chartEntry.getValue().doubleValue() / total.doubleValue();
                return percentFormatter.format(percent);
            }
        }
    }

    /**
     * A custom renderer that returns a different color for each item in a single series.
     */
    static class BarChartRenderer extends BarRenderer {

        public BarChartRenderer() {
            super();
        }

        private transient Paint[] colors;

        /**
         * Creates a new renderer.
         *
         * @param colors the colors.
         */
        public BarChartRenderer(final Paint[] colors) {
            this.colors = colors;
        }

        /**
         * Returns the paint for an item.  Overrides the default behaviour inherited from
         * AbstractSeriesRenderer.
         *
         * @param row    the series.
         * @param column the category.
         * @return The item color.
         */
        public Paint getItemPaint(final int row, final int column) {
            return this.colors[column % this.colors.length];
        }

    }

    static class BarChartToolTipGenerator implements CategoryToolTipGenerator {
        private ChartData chartData;

        BarChartToolTipGenerator(ChartData chartData) {
            this.chartData = chartData;
        }

        public String generateToolTip(CategoryDataset categoryDataset, int row, int col) {
            String entryName = categoryDataset.getColumnKey(col).toString();
            ChartDataEntry chartEntry = chartData.findChartEntry(entryName);
            if (chartEntry != null) {
                if (chartEntry.getDescription() != null) {
                    return chartEntry.getDescription();
                }
                else {
                    return chartEntry.getName();
                }
            }
            else {
                return null;
            }
        }

    }

    static class PieChartToolTipGenerator implements PieToolTipGenerator {
        private ChartData chartData;

        PieChartToolTipGenerator(ChartData chartData) {
            this.chartData = chartData;
        }

        public String generateToolTip(PieDataset pieDataset, Comparable comparable) {
            String entryName = comparable.toString();
            ChartDataEntry chartEntry = chartData.findChartEntry(entryName);
            if (chartEntry != null) {
                if (chartEntry.getDescription() != null) {
                    return chartEntry.getDescription();
                }
                else {
                    return chartEntry.getName();
                }
            }
            else {
                return null;
            }
        }

    }

    public JFreeChartTool() {
    }

    public ImageModel createPieChart(String chartTitle,
                                     ChartData chartValues,
                                     int width,
                                     int height,
                                     String chartBaseDirectory,
                                     String chartRelativeDirectory)
            throws Exception {
        File chartFile = getChartFile(chartBaseDirectory);
        JFreeChart chart = createPieChart(chartTitle, chartValues);
        return createChartImage(chart, chartFile, chartTitle, width, height, chartRelativeDirectory);
    }

    public ImageModel createBarChart(String chartTitle,
                                     String domainLabel,
                                     String rangeLabel,
                                     ChartData chartValues,
                                     int width,
                                     int height,
                                     String chartBaseDirectory,
                                     String chartRelativeDirectory)
            throws Exception {
        File chartFile = getChartFile(chartBaseDirectory);
        JFreeChart chart = createBarChart(chartTitle, domainLabel, rangeLabel, chartValues);
        return createChartImage(chart, chartFile, chartTitle, width, height, chartRelativeDirectory);
    }

    private JFreeChart createBarChart(String title,
                                      String domainLabel,
                                      String rangeLabel,
                                      ChartData chartValues) {
        CategoryPlot plot = createBarChartPlot(domainLabel,
                rangeLabel,
                chartValues);
        JFreeChart chart = new JFreeChart(/*no title*/null, plot);
        chart.removeLegend();
        chart.setBackgroundPaint(Color.white);
        chart.setBorderVisible(false);
        return chart;
    }

    private JFreeChart createPieChart(String title, ChartData chartValues) {
        ChartData restructuredChartData = restructureChartData(chartValues);
        PiePlot plot = createPieChartPlot(restructuredChartData);
        JFreeChart chart = new JFreeChart(/*no title*/null, plot);
        chart.removeLegend();
        chart.setBackgroundPaint(Color.white);
        chart.setBorderVisible(false);
        return chart;
    }

    private ImageModel createChartImage(JFreeChart chart,
                                        File chartFile,
                                        String chartTitle,
                                        int width,
                                        int height,
                                        String chartRelativeDirectory)
            throws Exception {
        ChartRenderingInfo info =
                new ChartRenderingInfo(new StandardEntityCollection());
        //info.setChartArea(new Rectangle2D());
        if (width <= 0) {
            width = DEFAULT_WIDTH;
        }
        if (height <= 0) {
            height = DEFAULT_HEIGHT;
        }
        ChartUtilities.saveChartAsPNG(chartFile, chart, width, height, info);
        ImageModel resultChartImage = new ImageModel();
        resultChartImage.setName(chartFile.getName());
        resultChartImage.setTitle(chartTitle);
        resultChartImage.setLocation(Constants.SERVLET_CONTEXT + "/" + chartRelativeDirectory);
        EntityCollection chartEntities = info.getEntityCollection();
        if (chartEntities != null) {
            int nChartEntities = chartEntities.getEntityCount();
            for (int i = nChartEntities - 1; i >= 0; i--) {
                ChartEntity chartEntity = chartEntities.getEntity(i);
                if (chartEntity.getShapeType() != null &&
                        chartEntity.getShapeCoords() != null) {
                    ImageAreaModel imageArea = new ImageAreaModel();
                    imageArea.setShape(chartEntity.getShapeType());
                    imageArea.setCoordinates(chartEntity.getShapeCoords());
                    imageArea.setTips(chartEntity.getToolTipText());
                    resultChartImage.addImageArea(imageArea);
                }
            }
        }
        return resultChartImage;
    }

    private CategoryPlot createBarChartPlot(String domainLabel,
                                            String rangeLabel,
                                            ChartData chartValues) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (ChartDataEntry chartEntry : (List<ChartDataEntry>) chartValues.getChartDataEntries()) {
            String category = chartEntry.getCategory();
            String name = chartEntry.getName();
            Number value = chartEntry.getValue();
            if (name == null || name.length() == 0) {
                name = "Unknown";
            }
            dataset.setValue(value, category, name);
        }
        CategoryAxis domainAxis = new CategoryAxis(domainLabel);
        domainAxis.setLabelFont(DEFAULT_LABEL_FONT);
        domainAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);
        ValueAxis rangeAxis = new NumberAxis(rangeLabel);
        BarChartRenderer barRenderer = new BarChartRenderer(new Paint[]{
                Color.red,
                Color.blue,
                Color.green,
                Color.yellow,
                Color.magenta
        });
        ItemLabelPosition labelPos = new ItemLabelPosition(ItemLabelAnchor.INSIDE1,
                TextAnchor.TOP_CENTER,
                TextAnchor.CENTER,
                0);
        PercentCategoryItemLabelGenerator percentLabelGenerator =
                new PercentCategoryItemLabelGenerator(chartValues);
        CategoryToolTipGenerator toolTipGenerator = new BarChartToolTipGenerator(chartValues);
        for (int i = 0; i < dataset.getColumnCount(); i++) {
            barRenderer.setSeriesItemLabelsVisible(i, true);
            barRenderer.setSeriesItemLabelGenerator(i, percentLabelGenerator);
            barRenderer.setSeriesPositiveItemLabelPosition(i, labelPos);
            barRenderer.setSeriesToolTipGenerator(i, toolTipGenerator);
        }

        CategoryPlot plot = new CategoryPlot(dataset, domainAxis, rangeAxis, barRenderer);
        plot.setBackgroundPaint(Color.white);
        plot.setOrientation(PlotOrientation.VERTICAL);

        return plot;
    }

    private PiePlot createPieChartPlot(ChartData chartData) {
        DefaultPieDataset dataset = new DefaultPieDataset();
        for (ChartDataEntry chartValue : (List<ChartDataEntry>) chartData.getChartDataEntries()) {
            String name = chartValue.getName();
            Number value = chartValue.getValue();
            if (name == null || name.length() == 0) {
                name = "Unknown";
            }
            dataset.setValue(name, value);
        }
        PiePlot plot = new PiePlot(dataset);
        plot.setStartAngle(Math.PI / 4);
        plot.setBackgroundPaint(Color.white);
        plot.setSectionOutlinesVisible(false);
        plot.setOutlineStroke(null);
        plot.setCircular(true);
        plot.setToolTipGenerator(new PieChartToolTipGenerator(chartData));

        plot.setInteriorGap(0.000);
        //plot.setInsets(RectangleInsets.ZERO_INSETS);
        //plot.setInsets(new RectangleInsets(UnitType.ABSOLUTE, 1, 1, 1, 1));

        plot.setLabelBackgroundPaint(plot.getBackgroundPaint());
        plot.setLabelFont(DEFAULT_LABEL_FONT);
        plot.setLabelOutlineStroke(null);
        plot.setLabelShadowPaint(null);
        plot.setLabelGap(0.019);
        plot.setMaximumLabelWidth(0.244);
        plot.setLabelLinkMargin(0.275);       // increasing this decreases size of pie

        return plot;
    }

}
