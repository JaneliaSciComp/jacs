package org.janelia.it.jacs.model.user_data.geci;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.io.Serializable;

public class GeciImageDirectoryVO implements IsSerializable, Serializable {
    private Long nodeId;
    private String _localDirName;
    private String _targetDirectoryPath;
    private boolean processed;

    public GeciImageDirectoryVO() {
    }

    public String getLocalDirName() {
        return _localDirName;
    }

    public void setLocalDirName(String _localDirName) {
        this._localDirName = _localDirName;
    }

    public String getTargetDirectoryPath() {
        return _targetDirectoryPath;
    }

    public void setTargetDirectoryPath(String _targetDirectoryPath) {
        this._targetDirectoryPath = _targetDirectoryPath;
    }

    public boolean isProcessed() {
        return processed;
    }

    public void setProcessed(boolean processed) {
        this.processed = processed;
    }

    public Long getNodeId() {
        return nodeId;
    }

    public void setNodeId(Long nodeId) {
        this.nodeId = nodeId;
    }
}
