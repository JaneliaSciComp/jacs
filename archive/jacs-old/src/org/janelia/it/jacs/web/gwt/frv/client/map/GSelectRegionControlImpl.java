
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
