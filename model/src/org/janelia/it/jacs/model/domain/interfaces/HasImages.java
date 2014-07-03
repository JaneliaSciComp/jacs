package org.janelia.it.jacs.model.domain.interfaces;

import java.util.Map;

import org.janelia.it.jacs.model.domain.enums.ImageType;

public interface HasImages {

    public abstract Map<ImageType,String> getImages();

}