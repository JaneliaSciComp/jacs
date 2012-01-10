
package org.janelia.it.jacs.web.gwt.common.shared.data;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: cgoina
 * Date: Sep 26, 2007
 * Time: 9:13:01 AM
 */
public class CompositeChartDataEntry extends ChartDataEntry
        implements Serializable, IsSerializable {

    private Number total;
    private List<ChartDataEntry> chartDataEntries;

    public CompositeChartDataEntry() {
        chartDataEntries = new ArrayList<ChartDataEntry>();
    }

    public void addChartDataEntry(ChartDataEntry chartEntry) {
        chartDataEntries.add(chartEntry);
    }

    public List<ChartDataEntry> getChartDataEntries() {
        return chartDataEntries;
    }

    public int getNumberOfEntries() {
        return chartDataEntries.size();
    }

    public Number getValue() {
        if (total == null) {
            total = computeTotal();
        }
        return total;
    }

    public void setValue(Number total) {
        this.total = total;
    }

    public String getMembersDescription() {
        StringBuffer descBuffer = new StringBuffer();
        int nEntries = chartDataEntries.size();
        if (nEntries > 0) {
            for (int i = 0; i < nEntries; i++) {
                ChartDataEntry currentEntry = getChartEntry(i);
                if (descBuffer.length() > 0) {
                    descBuffer.append("; ");
                }
                descBuffer.append(currentEntry.getName());
/*
                if(currentEntry.getDescription() != null && currentEntry.getDescription().length() > 0) {
                    descBuffer.append(':');
                    descBuffer.append(currentEntry.getDescription());
                }
*/
            }
        }
        return descBuffer.toString();
    }

    public ChartDataEntry getChartEntry(int i) {
        return chartDataEntries.get(i);
    }

    public ChartDataEntry removeChartEntryAt(int i) {
        return chartDataEntries.remove(i);
    }

    protected Number computeTotal() {
        int nEntries = chartDataEntries.size();
        Number computedTotal = null;
        if (nEntries > 0) {
            double totalValue = 0;
            for (int i = 0; i < nEntries; i++) {
                ChartDataEntry currentEntry = getChartEntry(i);
                totalValue += currentEntry.getValue().doubleValue();
            }
            computedTotal = new Double(totalValue);
        }
        return computedTotal;
    }

}
