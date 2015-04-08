package org.janelia.it.jacs.compute.process_result_validation.octree;

import org.janelia.it.jacs.compute.largevolume.TileBaseReader;
import org.janelia.it.jacs.compute.largevolume.model.Aabb;
import org.janelia.it.jacs.compute.largevolume.model.Tile;
import org.janelia.it.jacs.compute.largevolume.model.TileBase;

import java.io.File;
import java.io.FileInputStream;

/**
 * Created by fosterl on 4/1/15.
 * Grabs information from some tilebase, given also a tile index (sequence number within tilebase.yml).
 */
public class TileMetaInfoFetcher {

    private TileBase tileBase;

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            throw new IllegalArgumentException("Usage: java " + TileMetaInfoFetcher.class + " <tilebase.cache.yml-loc> <tile-index or offset>");
        }
        File tilebaseYml = new File(args[0]);
        String[] tileIndices = args[1].split(",");
        TileMetaInfoFetcher fetcher = new TileMetaInfoFetcher(tilebaseYml);
        for (String tileIndexStr: tileIndices) {
            fetcher.report(Integer.parseInt(tileIndexStr));
        }
    }

    public TileMetaInfoFetcher( File infile) throws Exception {
        tileBase = new TileBaseReader().readTileBase( new FileInputStream(infile) );
    }

    public void report(Integer tileIndex) {
        // NOTE: tile indexes from other dump are 1-based, whereas this Java collection is zero-based.
        Tile tile = tileBase.getTiles().get(tileIndex - 1);

        // Now, dump full info on tile.
        Aabb aabb = tile.getAabb();
        System.out.println("__________________________________________________________________");
        System.out.println("TILE=" + tileIndex);
        System.out.print("Shape=");
        for ( Integer element: aabb.getShape() ) {
            System.out.print(element + " ");
        }
        System.out.println();

        System.out.print("Origin=");
        for (Integer element: aabb.getOri()) {
            System.out.print(element + " ");
        }
        System.out.println();
        System.out.println("==================================================================");
    }
}
