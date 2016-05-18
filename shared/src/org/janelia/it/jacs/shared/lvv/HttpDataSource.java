package org.janelia.it.jacs.shared.lvv;

import com.sun.media.jai.codec.ByteArraySeekableStream;
import com.sun.media.jai.codec.SeekableStream;
import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;


/**
 * Created by murphys on 5/11/2016.
 */
public class HttpDataSource {

    private static Logger logger= LoggerFactory.getLogger(HttpDataSource.class);
    private static String interactiveServer;
    private static Long mouseLightCurrentSampleId;
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

    public static String getInteractiveServer() {
        return interactiveServer;
    }

    public static void setInteractiveServer(String interactiveServer) {
        HttpDataSource.interactiveServer = interactiveServer;
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

        String url="http://"+interactiveServer+":8180/rest-v1/mouselight/sample2DTile?"+
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

    public static byte[] fileToBytesByPath(String filepath) throws Exception {
        String url="http://"+interactiveServer+":8180/rest-v1/mouselight/fileBytes?path="+filepath;
        GetMethod getMethod=new GetMethod(url);
        byte[] bytes=null;
        try {
            int statusCode = httpClient.executeMethod(getMethod);
            if (statusCode != HttpStatus.SC_OK) {
                throw new Exception("HTTP status not OK");
            }
            bytes=getMethod.getResponseBody();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            getMethod.releaseConnection();
        }
        return bytes;
    }

    public static byte[] getMouseLightTiffBytes(String filepath) throws Exception {
        String url="http://"+interactiveServer+":8180/rest-v1/mouselight/mouseLightTiffBytes?suggestedPath="+filepath;
        GetMethod getMethod=new GetMethod(url);
        byte[] bytes=null;
        try {
            int statusCode = httpClient.executeMethod(getMethod);
            if (statusCode != HttpStatus.SC_OK) {
                throw new Exception("HTTP status not OK");
            }
            bytes=getMethod.getResponseBody();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            getMethod.releaseConnection();
        }
        return bytes;
    }

    public static InputStream getMouseLightTiffStream(String filepath) throws Exception {
        String url="http://"+interactiveServer+":8180/rest-v1/mouselight/mouseLightTiffStream?suggestedPath="+filepath;
        GetMethod getMethod=new GetMethod(url);
        try {
            int statusCode = httpClient.executeMethod(getMethod);
            if (statusCode != HttpStatus.SC_OK) {
                throw new Exception("HTTP status not OK");
            }
            // NOTE: if I try simply returning the inputStream, it shows up as closed in the decoder.
            // If I inefficiently convert the stream to a byte array and then create another stream
            // it works. Need to figure out how to make this more efficient.
            InputStream inputStream=getMethod.getResponseBodyAsStream();
            byte[] bytes = IOUtils.toByteArray(inputStream);
            SeekableStream s = new ByteArraySeekableStream(bytes);
            return s;
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            getMethod.releaseConnection();
        }
        return null;
    }

}
