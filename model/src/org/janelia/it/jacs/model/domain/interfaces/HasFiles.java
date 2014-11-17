package org.janelia.it.jacs.model.domain.interfaces;

import java.util.Map;

import org.janelia.it.jacs.model.domain.enums.FileType;

public interface HasFiles {

    public abstract Map<FileType, String> getFiles();

}
