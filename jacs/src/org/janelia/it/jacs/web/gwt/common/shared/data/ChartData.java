
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
public class ChartData implements Serializable, IsSerializable {

    private String name;
    private Number total;
    private List<ChartDataEntry> chartDataEntries;

    public ChartData() {
        this(null);
    }

    public ChartData(String name) {
        this.name = name;
        chartDataEntries = new ArrayList();
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Number getTotal() {
        if (total == null) {
            total = computeTotal();
        }
        return total;
    }

    public void setTotal(Number total) {
        this.total = total;
    }

    public ChartDataEntry findChartEntry(String name) {
        if (name == null) {
            return null;
        }
        int nEntries = 0;
        if (chartDataEntries != null) {
            nEntries = chartDataEntries.size();
        }
        for (int i = 0; i < nEntries; i++) {
            ChartDataEntry currentEntry = getChartEntry(i);
            if (currentEntry.getName().equals(name)) {
                return currentEntry;
            }
        }
        return null;
    }

    public ChartDataEntry getChartEntry(int i) {
        return chartDataEntries.get(i);
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
