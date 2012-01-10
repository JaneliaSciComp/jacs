
package org.janelia.it.jacs.model.common;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Aug 13, 2009
 * Time: 10:52:24 AM
 */
public class AP16QualityThresholds {
    private String _type = "";
    private String _configFile = "";
    private int _readLengthMinimum;
    private int _minAvgQV;
    private int _maxNCount;
    private int _minIdentCount;

    public AP16QualityThresholds(String type, String configFile, int readLengthMinimum, int minAvgQV, int maxNCount,
                                 int minIdentCount) {
        _type = type;
        _configFile = configFile;
        _readLengthMinimum = readLengthMinimum;
        _minAvgQV = minAvgQV;
        _maxNCount = maxNCount;
        _minIdentCount = minIdentCount;
    }

    public String getType() {
        return _type;
    }

    public String getConfigFile() {
        return _configFile;
    }

    public int getReadLengthMinimum() {
        return _readLengthMinimum;
    }

    public int getMinAvgQV() {
        return _minAvgQV;
    }

    public int getMaxNCount() {
        return _maxNCount;
    }

    public int getMinIdentCount() {
        return _minIdentCount;
    }
}
