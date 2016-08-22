package org.janelia.it.jacs.shared.lvv;

import java.util.Date;

import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.SimpleHttpConnectionManager;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Created by murphys on 5/11/2016.
 */
public class HttpDataSource {

    private static Logger logger = LoggerFactory.getLogger(HttpDataSource.class);
    private static String restServer;
    private static Long mouseLightCurrentSampleId;
    private static boolean useHttp=true;

    private static HttpClient httpClient;
    private static HttpClient simpleHttpClient;
    static {
        // Strange threading/connection issues were resolved by reusing a single HTTP Client with a high connection count.
        // The solution was found here:
        // http://amilachinthaka.blogspot.com/2010/01/improving-axis2-http-transport-client.html
        MultiThreadedHttpConnectionManager multiThreadedHttpConnectionManager = new MultiThreadedHttpConnectionManager();
        HttpConnectionManagerParams params = new HttpConnectionManagerParams();
        params.setDefaultMaxConnectionsPerHost(20);
        multiThreadedHttpConnectionManager.setParams(params);
        httpClient = new HttpClient(multiThreadedHttpConnectionManager);

        // Non-threaded version for Horta tile streaming
        SimpleHttpConnectionManager simpleHttpConnectionManager = new SimpleHttpConnectionManager();
        simpleHttpClient = new HttpClient(simpleHttpConnectionManager);
    }

    public static boolean useHttp() {
        return useHttp;
    }

    public static void setUseHttp(boolean useHttp1) {
        useHttp=useHttp1;
        logger.info("useHttp="+useHttp1);
    }

    public static String getRestServer() {
        return restServer;
    }

    public static void setRestServer(String restServer) {
        HttpDataSource.restServer = restServer;
        logger.info("HttpDataSource is using server " + restServer);
    }

    public static Long getMouseLightCurrentSampleId() {
        return mouseLightCurrentSampleId;
    }

    public static void setMouseLightCurrentSampleId(Long mouseLightCurrentSampleId) {
        HttpDataSource.mouseLightCurrentSampleId = mouseLightCurrentSampleId;
    }

    public static TextureData2d getSample2DTile(TileIndex tileIndex) {

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

        String url= restServer + "mouselight/sample2DTile?"+
                "sampleId="+mouseLightCurrentSampleId+
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

            long startTime=new Date().getTime();
            int statusCode=httpClient.executeMethod(getMethod);

            // debug: sometimes you want to see all the results...comment
            //  in and out as needed
            // System.out.println("HttpDataSource: " + statusCode + " from " + url);

            // note: not all tiff tiles exist, so some will return
            //  no content (204), and that's OK
            if (statusCode!= HttpStatus.SC_OK && statusCode != HttpStatus.SC_NO_CONTENT) {
                throw new Exception("HTTP status " + statusCode + " (not OK) from url " + url);
            }
            byte[] responseBytes=getMethod.getResponseBody();

            if (responseBytes!=null) {
                textureData2d = new TextureData2d(responseBytes);
            }

            long getTime=new Date().getTime()-startTime;

            //logger.info("getSample2DTile() ms="+getTime);

        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            getMethod.releaseConnection();
        }
        return textureData2d;
    }

    public static byte[] fileToBytesByPath(String filepath) throws Exception {
        String url= restServer + "mouselight/fileBytes?path="+filepath;
        GetMethod getMethod=new GetMethod(url);
        byte[] bytes=null;
        try {
            int statusCode = httpClient.executeMethod(getMethod);
            if (statusCode != HttpStatus.SC_OK) {
                throw new Exception("HTTP status " + statusCode + " (not OK) from url " + url);
            }
            bytes=getMethod.getResponseBody();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            getMethod.releaseConnection();
        }
        return bytes;
    }

    /**
     * This method uses a single-threaded version of the HTTP client, so threading must be done by the caller.
     * @param filepath
     * @return
     */
    public static GetMethod getMouseLightTiffStream(String filepath) throws Exception {
        String url = restServer + "mouselight/mouseLightTiffStream?suggestedPath="+filepath;
        GetMethod getMethod = new GetMethod(url);
        try {
            int statusCode = simpleHttpClient.executeMethod(getMethod);
            if (statusCode != HttpStatus.SC_OK) {
                throw new Exception("HTTP status " + statusCode + " (not OK) from url " + url);
            }
            return getMethod;
        }
        finally {
            getMethod.releaseConnection();
        }
    }

}
