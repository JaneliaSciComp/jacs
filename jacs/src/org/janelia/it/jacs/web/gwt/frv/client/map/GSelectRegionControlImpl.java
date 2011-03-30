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

package org.janelia.it.jacs.web.gwt.frv.client.map;

import com.google.gwt.core.client.JavaScriptObject;
import org.janelia.it.jacs.web.gwt.frv.client.panels.FrvImagePanel;

public class GSelectRegionControlImpl {
    //TODO: remove dependency on FrvImagePanel
    public static native JavaScriptObject create(FrvImagePanel frv)/*-{
        return new $wnd.GZoomControl(
            {
                sBorder:"1px dashed blue"
            },
            {
                sButtonHTML:'export<br>region',
                oButtonStyle: {
                    width:'auto',
                    background:'#FFF',
                    padding:'2px 3px 2px 3px',
                    borderLeft:'1px solid #BBB',
                    borderTop:'1px solid #BBB',
                    borderRight:'2px solid #777',
                    borderBottom:'2px solid #777',
                    display:'none'
                }
            },
            {
                dragEnd: function (nw,ne,se,sw)
                 {
                     frv.@org.janelia.it.jacs.web.gwt.frv.client.panels.FrvImagePanel::onRegionSelected(FFFF)(nw.x,nw.y,se.x,se.y);
                 }
            }
        );
    }-*/;

    /**
     * Execs resetDragZoom_() on the provided JSOBject (which must be a GZoomControl)
     *
     * @param control javascript control object
     */
    public static native void reset(JavaScriptObject control)/*-{
        control.resetDragZoom_();
    }-*/;

    /**
     * Execs redrawRectangle() on the provided JSOBject (which must be a GZoomControl)
     *
     * @param control javascript control object
     * @param swLat   southwest latitude loc
     * @param swLng   southwest longitude loc
     * @param neLat   northeast latitude loc
     * @param neLng   northeast longitude loc
     */
    public static native void redraw(JavaScriptObject control, double swLat, double swLng, double neLat, double neLng)/*-{
        control.redrawRectangle(swLat, swLng, neLat, neLng);
    }-*/;

    /**
     * Execs redrawRectangle() on the provided JSOBject (which must be a GZoomControl)
     *
     * @param control javascrip control object
     */
    public static native void promptedInit(JavaScriptObject control)/*-{
        control.initCover_();
    }-*/;

}
