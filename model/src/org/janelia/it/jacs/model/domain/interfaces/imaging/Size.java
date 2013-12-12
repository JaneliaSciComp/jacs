package org.janelia.it.jacs.model.domain.interfaces.imaging;

public enum Size {
    Full(0),
    Subsampled_25mv(25),
    Subsampled_50mv(50),
    Subsampled_100mv(100);
    private int megaVoxels;
    Size(int megaVoxels) {
        this.megaVoxels = megaVoxels;
    }
    public int getMegaVoxels() {
        return megaVoxels;
    }
}