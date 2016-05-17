package org.janelia.it.workstation.gui.large_volume_viewer;

import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.http.HttpResponse;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.shared.lvv.TextureData2d;
import org.janelia.it.jacs.shared.lvv.TileIndex;
import org.janelia.it.workstation.gui.large_volume_viewer.top_component.LargeVolumeViewerTopComponent;
import org.janelia.it.workstation.shared.util.ConsoleProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * Created by murphys on 5/11/2016.
 */
public class HttpDataSource {

    private static Logger logger= LoggerFactory.getLogger(HttpDataSource.class);
    private static final String INTERACTIVE_SERVER = ConsoleProperties.getInstance().getProperty("interactive.server.url");
    private static boolean useHttp=false;

    private static HttpClient httpClient;
    static {
        // Strange threading/connection issues were resolved by reusing a single HTTP Client with a high connection count.
        // The solution was found here:
        // http://amilachinthaka.blogspot.com/2010/01/improving-axis2-http-transport-client.html
        MultiThreadedHttpConnectionManager multiThreadedHttpConnectionManager = new MultiThreadedHttpConnectionManager();
        HttpConnectionManagerParams params = new HttpConnectionManagerParams();
        params.setDefaultMaxConnectionsPerHost(20);
        multiThreadedHttpConnectionManager.setParams(params);
        httpClient = new HttpClient(multiThreadedHttpConnectionManager);
    }

    public static boolean useHttp() {
        return useHttp;
    }

    public static void setUseHttp(boolean useHttp1) {
        useHttp=useHttp1;
        logger.info("useHttp="+useHttp1);
    }

    public static TextureData2d getSample2DTile(TileIndex tileIndex) {

        long sampleId=LargeVolumeViewViewer.getCurrentSampleId();

//        @QueryParam("sampleId") String sampleIdString,
//        @QueryParam("x") String xString,
//        @QueryParam("y") String yString,
//        @QueryParam("z") String zString,
//        @QueryParam("zoom") String zoomString,
//        @QueryParam("maxZoom") String maxZoomString,
//        @QueryParam("index") String indexString,
//        @QueryParam("axis") String axisString)

        TileIndex.IndexStyle indexStyle=tileIndex.getIndexStyle();
        String indexStyleString=null;
        if (indexStyle.equals(TileIndex.IndexStyle.QUADTREE)) {
            indexStyleString="QUADTREE";
        } else {
            indexStyleString="OCTREE";
        }

        String url="http://"+INTERACTIVE_SERVER+":8180/rest-v1/mouselight/sample2DTile?"+
                "sampleId="+sampleId+
                "&x="+tileIndex.getX()+
                "&y="+tileIndex.getY()+
                "&z="+tileIndex.getZ()+
                "&zoom="+tileIndex.getZoom()+
                "&maxZoom="+tileIndex.getMaxZoom()+
                "&index="+indexStyleString+
                "&axis="+tileIndex.getSliceAxis().getName();
        GetMethod getMethod=new GetMethod(url);
        getMethod.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, new DefaultHttpMethodRetryHandler(3, false));

        TextureData2d textureData2d=null;

        try {
            int statusCode=httpClient.executeMethod(getMethod);

            if (statusCode!= HttpStatus.SC_OK) {
                throw new Exception("HTTP status not OK");
            }
            byte[] responseBytes=getMethod.getResponseBody();

            if (responseBytes!=null) {
                textureData2d = new TextureData2d(responseBytes);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            getMethod.releaseConnection();
        }
        return textureData2d;
    }

}
