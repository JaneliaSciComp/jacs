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
