package org.janelia.it.jacs.compute.largevolume.model;

import java.util.List;

/**
 * This class is the top of the hierarchy of classes gathered in this package.  They are used for SnakeYaml-reading
 * of a YML file.  They are all value objects with references to each other.
 *
 * Created by fosterl on 9/24/14.
 */
public class TileBase {
    private String path;
    private List<Tile> tiles;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public List<Tile> getTiles() {
        return tiles;
    }

    public void setTiles(List<Tile> tiles) {
        this.tiles = tiles;
    }
}
