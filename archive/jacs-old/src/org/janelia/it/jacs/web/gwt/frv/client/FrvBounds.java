
package org.janelia.it.jacs.web.gwt.frv.client;

/**
 * @author Michael Press
 */
public class FrvBounds {
    private double _startPctId;
    private double _endPctId;
    private long _startBasePairCoord;
    private long _endBasePairCoord;

    public FrvBounds(double startPctId, double endPctId, long startBasePairCoord, long endBasePairCoord) {
        _startPctId = startPctId;
        _endPctId = endPctId;
        _startBasePairCoord = startBasePairCoord;
        _endBasePairCoord = endBasePairCoord;
    }

    public long getEndBasePairCoord() {
        return _endBasePairCoord;
    }

    public double getEndPctId() {
        return _endPctId;
    }

    public long getStartBasePairCoord() {
        return _startBasePairCoord;
    }

    public double getStartPctId() {
        return _startPctId;
    }

    public void setEndBasePairCoord(long endBasePairCoord) {
        _endBasePairCoord = endBasePairCoord;
    }

    public void setEndPctId(double endPctId) {
        _endPctId = endPctId;
    }

    public void setStartBasePairCoord(long startBasePairCoord) {
        _startBasePairCoord = startBasePairCoord;
    }

    public void setStartPctId(double startPctId) {
        _startPctId = startPctId;
    }
}
