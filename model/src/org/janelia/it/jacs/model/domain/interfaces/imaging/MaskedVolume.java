package org.janelia.it.jacs.model.domain.interfaces.imaging;


public interface MaskedVolume {

    public abstract String getSignalVolumePath();

    public abstract String getSignalLabelPath();

    public abstract String getReferenceVolumePath();

    public abstract String getFastVolumePath(ArtifactType type, Size size, Channels channels, boolean lossless);

    public abstract String getFastMetadataPath(ArtifactType type, Size size);

}