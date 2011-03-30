/*
 * Copyright (c) 2010-2011, J. Craig Venter Institute, Inc.
 *
 * This file is part of JCVI VICS.
 *
 * JCVI VICS is free software; you can redistribute it and/or modify it
 * under the terms and conditions of the Artistic License 2.0.  For
 * details, see the full text of the license in the file LICENSE.txt.  No
 * other rights are granted.  Any and all third party software rights to
 * remain with the original developer.
 *
 * JCVI VICS is distributed in the hope that it will be useful in
 * bioinformatics applications, but it is provided "AS IS" and WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTIES including but not limited to
 * implied warranties of merchantability or fitness for any particular
 * purpose.  For details, see the full text of the license in the file
 * LICENSE.txt.
 *
 * You should have received a copy of the Artistic License 2.0 along with
 * JCVI VICS.  If not, the license can be obtained from
 * "http://www.perlfoundation.org/artistic_license_2_0."
 */

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
