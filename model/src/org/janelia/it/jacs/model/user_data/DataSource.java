
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

    public static DataSource getDataSourceByName(String name){
        if (DataSource.HHMI.getSourceName().equals(name)) {return DataSource.HHMI;}
        else if (DataSource.CAMERA.getSourceName().equals(name)) {return DataSource.CAMERA;}
        else if (DataSource.TIGR.getSourceName().equals(name)) {return DataSource.TIGR;}
        else if (DataSource.NCBI.getSourceName().equals(name)) {return DataSource.NCBI;}
        else if (DataSource.Ensemble.getSourceName().equals(name)) {return DataSource.Ensemble;}
        return DataSource.UNKNOWN;
    }
}
