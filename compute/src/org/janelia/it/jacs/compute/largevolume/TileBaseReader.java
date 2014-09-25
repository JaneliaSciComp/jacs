package org.janelia.it.jacs.compute.largevolume;

import org.janelia.it.jacs.compute.largevolume.model.TileBase;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.InputStream;

/**
 * Use YAML reader to pull object based on an input stream.
 *
 * Created by fosterl on 9/24/14.
 */
public class TileBaseReader {
    public TileBase readTileBase( InputStream inputStream ) throws Exception {
        Yaml yaml = new Yaml( new Constructor( TileBase.class ) );
        TileBase tileBase = (TileBase)yaml.load( inputStream );
        return tileBase;
    }
}
