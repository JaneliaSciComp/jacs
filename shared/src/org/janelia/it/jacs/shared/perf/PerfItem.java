
package org.janelia.it.jacs.shared.perf;

/**
 * Created by IntelliJ IDEA.
 * User: tnabeel
 * Date: Mar 30, 2007
 * Time: 11:04:56 AM
 */
public class PerfItem implements Comparable<PerfItem> {
    long startTime;
    long total;
    String name;

    public PerfItem(String name) {
        this.name = name;
    }

    public int compareTo(PerfItem o) {
        long thisVal = total;
        long anotherVal = o.total;
        return (thisVal < anotherVal ? -1 : (thisVal == anotherVal ? 0 : 1));
    }
}
