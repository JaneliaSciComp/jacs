package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import static org.junit.Assert.*;

import org.janelia.it.FlyWorkstation.gui.viewer3d.CoordinateAxis;
import org.janelia.it.FlyWorkstation.gui.viewer3d.Vec3;
import org.junit.Test;

public class TestTileFormat {

	// Volume like "M:/render/2013-04-25-AAV/"
	private TileFormat createAavFormat() {
		TileFormat tileFormat = new TileFormat();
		tileFormat.setDefaultParameters();
		tileFormat.setVolumeSize(new int[] {65536, 65536, 7936});
		tileFormat.setVoxelMicrometers(new double[] {1.0, 1.0, 1.0});
		tileFormat.setTileSize(new int[] {1024, 1024, 124});
		tileFormat.setZoomLevelCount(7);
		return tileFormat;
	}
	
	// xyz->TileIndex->cornerXyz sanity check
	private void sanityCheckXyz(Vec3 xyz, TileFormat format, int zoom) {
		TileIndex ix = format.tileIndexForXyz(xyz, zoom, CoordinateAxis.Z);
		Vec3 corners[] = format.cornersForTileIndex(ix);
		assertTrue(xyz.getX() >= corners[0].getX() - 1e-6);
		assertTrue(xyz.getX() <= corners[1].getX() + 1e-6);
		// 
		assertTrue(xyz.getY() >= corners[0].getY() - 1e-6);
		assertTrue(xyz.getY() <= corners[3].getY() + 1e-6);
		// From Les Foster test 8/19/2013
		assertTrue(corners[0].getY() <= corners[3].getY());
		assertTrue(corners[0].getX() <= corners[3].getX());
	}
	
	@Test
	public void testTileIndexForXyz() {
		TileFormat tileFormat = createAavFormat();
		
		// Test simple upper left front corner tile
		Vec3 xyz1 = new Vec3(0, 0, 0);
		TileIndex ix1 = tileFormat.tileIndexForXyz(xyz1, 0, CoordinateAxis.Z);
		assertEquals(0, ix1.getX());
		assertEquals(63, ix1.getY());
		assertEquals(0, ix1.getZ());

		// Test general tile
		Vec3 xyz2 = new Vec3(29952.0, 24869.6, 1243.5);
		TileIndex ix2 = tileFormat.tileIndexForXyz(xyz2, 0, CoordinateAxis.Z);
		assertEquals(29, ix2.getX());
		assertEquals(39, ix2.getY());
		assertEquals(1243, ix2.getZ());
		
		sanityCheckXyz(new Vec3(0,0,0), tileFormat, 0);
		sanityCheckXyz(new Vec3(2048,2048,0), tileFormat, 0);
		sanityCheckXyz(new Vec3(2047,2047,0), tileFormat, 0);
		sanityCheckXyz(new Vec3(2049,2049,0), tileFormat, 0);
		
		sanityCheckXyz(new Vec3(0,0,0), tileFormat, 3);
		sanityCheckXyz(new Vec3(2048,2048,0), tileFormat, 3);
		sanityCheckXyz(new Vec3(2047,2047,0), tileFormat, 3);
		sanityCheckXyz(new Vec3(2049,2049,0), tileFormat, 3);
	}

	@Test
	public void testCornersForTileIndex() {
		TileFormat tileFormat = createAavFormat();

		// upper left front corner tile
		TileIndex ix1 = new TileIndex(0, 63, 0,
				0, 0, 
				TileIndex.IndexStyle.OCTREE,
				CoordinateAxis.Z);
		Vec3 corners[] = tileFormat.cornersForTileIndex(ix1);
		// Order of corners should be like this:
		//  0 --- 1
		//  |     |
		//  |     |
		//  2 --- 3
		//
		// X coordinate : the easy case
		assertEquals(0, corners[0].getX(), 1e-6);
		assertEquals(0, corners[2].getX(), 1e-6);
		assertEquals(1024.0, corners[1].getX(), 1e-6);
		assertEquals(1024.0, corners[3].getX(), 1e-6);
		// Y : trickier because of inversion between Raveler and image order
		assertEquals(0.0, corners[0].getY(), 1e-6);
		assertEquals(0.0, corners[1].getY(), 1e-6);
		assertEquals(1024.0, corners[2].getY(), 1e-6);
		assertEquals(1024.0, corners[3].getY(), 1e-6);
		// Z : constant
		assertEquals(0.5, corners[0].getZ(), 1e-6);
		assertEquals(0.5, corners[1].getZ(), 1e-6);
		assertEquals(0.5, corners[2].getZ(), 1e-6);
		assertEquals(0.5, corners[3].getZ(), 1e-6);
	}

	@Test
	public void testZoomLevelForCameraZoom() {
		// fail("Not yet implemented");
	}

}
