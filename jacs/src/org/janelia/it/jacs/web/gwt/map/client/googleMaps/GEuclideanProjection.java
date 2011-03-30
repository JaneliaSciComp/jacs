/*
 * Copyright (c) 2010-2011, J. Craig Venter Institute, Inc.
 *
 * This file is part of JCVI VICS.
 *
 * JCVI VICS is free software; you can redistribute it and/or modify it
 * under the terms and conditions of the Artistic License 2.0.  For
 * details, see the full text of the license in the file LICENSE.txt.  No
 * other rights are granted.  Any and all third party software rights to
 * remain with the original developer.
 *
 * JCVI VICS is distributed in the hope that it will be useful in
 * bioinformatics applications, but it is provided "AS IS" and WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTIES including but not limited to
 * implied warranties of merchantability or fitness for any particular
 * purpose.  For details, see the full text of the license in the file
 * LICENSE.txt.
 *
 * You should have received a copy of the Artistic License 2.0 along with
 * JCVI VICS.  If not, the license can be obtained from
 * "http://www.perlfoundation.org/artistic_license_2_0."
 */

package org.janelia.it.jacs.web.gwt.map.client.googleMaps;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.maps.client.geom.LatLng;
import com.google.gwt.maps.client.geom.Point;
import com.google.gwt.maps.client.geom.Projection;
import com.google.gwt.maps.client.geom.TileIndex;

/**
 * Extension of GProjection that represents the map as a flat surface, not a sphere.
 *
 * @author Michael Press
 * @see org.janelia.it.jacs.web.gwt.map.client.googleMaps.NonRepeatingGMercatorProjectionImpl
 */
public class GEuclideanProjection extends Projection {
    protected GEuclideanProjection(JavaScriptObject element) {
        super(element);
    }

    @Override
    public Point fromLatLngToPixel(LatLng latLng, int i) {
        return null;
    }

    @Override
    public LatLng fromPixelToLatLng(Point point, int i, boolean b) {
        return null;
    }

    @Override
    public double getWrapWidth(int i) {
        return 0;
    }

    @Override
    public boolean tileCheckRange(TileIndex tileIndex, int i, int i1) {
        return false;
    }

    public GEuclideanProjection(int zoomlevels) {
        this(GEuclideanProjectionImpl.create(zoomlevels));
    }

    public static GEuclideanProjection narrowToGEuclideanProjection(JavaScriptObject element) {
        return (element == null) ? null : new GEuclideanProjection(element);
    }
}
