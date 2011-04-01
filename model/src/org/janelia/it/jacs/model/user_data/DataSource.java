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

package org.janelia.it.jacs.model.user_data;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * User: jhoover
 * Date: Jan 4, 2007
 * Time: 3:29:39 PM
 */
public class DataSource implements Serializable, IsSerializable {

    public static final DataSource UNKNOWN  = new DataSource((long) -1, "Unknown", null);
    public static final DataSource CAMERA   = new DataSource((long) 0, "CAMERA", null);
    public static final DataSource TIGR     = new DataSource((long) 1, "TIGR", null);
    public static final DataSource NCBI     = new DataSource((long) 2, "NCBI", null);
    public static final DataSource Ensemble = new DataSource((long) 3, "Ensemble", null);
    public static final DataSource HHMI     = new DataSource((long) 4, "HHMI", null);
    private Long sourceId;
    private String sourceName;
    private String dataVersion;

    public DataSource() {
    }

    public DataSource(Long sourceId, String sourceName, String dataVersion) {
        this.sourceId = sourceId;
        this.sourceName = sourceName;
        this.dataVersion = dataVersion;
    }

    public DataSource(String sourceName, String dataVersion) {
        this.sourceName = sourceName;
        this.dataVersion = dataVersion;
    }

    public Long getSourceId() {
        return sourceId;
    }

    public void setSourceId(Long sourceId) {
        this.sourceId = sourceId;
    }

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public String getDataVersion() {
        return dataVersion;
    }

    public void setDataVersion(String dataVersion) {
        this.dataVersion = dataVersion;
    }
}
