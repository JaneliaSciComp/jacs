package org.janelia.it.jacs.compute.wsrest.mouselight;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.Query;
import org.hibernate.Session;
import org.janelia.it.jacs.compute.access.AnnotationDAO;
import org.janelia.it.jacs.compute.access.TiledMicroscopeDAO;
import org.janelia.it.jacs.compute.api.AnnotationBeanRemote;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.api.EntityBeanRemote;
import org.janelia.it.jacs.compute.api.TiledMicroscopeBeanRemote;
import org.janelia.it.jacs.compute.largevolume.RawFileFetcher;
import org.janelia.it.jacs.compute.util.HibernateSessionUtils;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.entity.json.JsonTaskEvent;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.CoordinateToRawTransform;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmGeoAnnotation;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmNeuron;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmWorkspace;
import org.janelia.it.jacs.model.user_data.tiled_microscope_protobuf.TmProtobufExchanger;
import org.janelia.it.jacs.shared.geom.CoordinateAxis;
import org.janelia.it.jacs.shared.lvv.BlockTiffOctreeLoadAdapter;
import org.janelia.it.jacs.shared.lvv.CoordinateToRawTransformFileSource;
import org.janelia.it.jacs.shared.lvv.TextureData2d;
import org.janelia.it.jacs.shared.lvv.TileIndex;
import org.jboss.resteasy.annotations.providers.jaxb.Formatted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.BASE64Encoder;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.nio.file.Files;
import java.sql.Timestamp;
import java.util.*;


/**
 * Created by murphys on 3/24/2016.
 */

@Path("/mouselight")
public class WorkspaceRestService {

    public static class ExpirableLoadAdapter {
        public BlockTiffOctreeLoadAdapter blockTiffOctreeLoadAdapter;
        public long setupTime;
    }

    private static Map<Long, ExpirableLoadAdapter> sampleLoadAdapterMap=new HashMap<>();
    public static final long LOAD_ADAPTER_TIMEOUT_MS=600000;

    private static final Logger log = LoggerFactory.getLogger(WorkspaceRestService.class);

    private static long quasiTimebasedGuid=0L;

    private static long[] getNextGuidArray(int count) {
        long timeValue=new Date().getTime();
        if (timeValue>quasiTimebasedGuid) {
            quasiTimebasedGuid=timeValue;
        }
        long[] guidArr=new long[count];
        for (int i=0;i<count;i++) {
            guidArr[i]=quasiTimebasedGuid++;
        }
        return guidArr;
    }

    //=============================================================================================================//
    // TESTS
    //=============================================================================================================//

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public class WorkspaceRestResponse2 {
        @JsonProperty
        public String getMessage() {
            return "this is a test message";
        }
    }

    @GET
    @Path("/hello")
    @Produces(MediaType.APPLICATION_JSON)
    @Formatted
    public WorkspaceRestResponse2 getMessage() {
        log.info("getMessage() invoked");
        return new WorkspaceRestResponse2();
    }

    //=============================================================================================================//
    // DATA CLASSES
    //=============================================================================================================//

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public class WorkspaceInfo {

        public String idString;
        public String ownerName;
        public String name;
        public long createTimestamp;
        public String createTimeString;
        public String dataType;
        public int neuronCount;

        @JsonProperty
        public String getIdString() { return idString; }

        @JsonProperty
        public String getOwnerName() { return ownerName; }

        @JsonProperty
        public String getName() { return name; }

        @JsonProperty
        public long getCreateTimestamp() { return createTimestamp; }

        @JsonProperty
        public String getCreateTimeString() { return createTimeString; }

        @JsonProperty
        public String getDataType() { return dataType; }

        @JsonProperty
        public int getNeuronCount() { return neuronCount; }

    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public class NeuronInfo {

        public String idString;
        public String name;
        public long createTimestamp;
        public String createTimeString;
        public int pointCount;

        @JsonProperty
        public String getIdString() { return idString; }

        @JsonProperty
        public String getName() { return name; }

        @JsonProperty
        public long getCreateTimestamp() { return createTimestamp; }

        @JsonProperty
        public String getCreateTimeString() { return createTimeString; }

        @JsonProperty
        public int getPointCount() { return pointCount; }

    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public class PointInfo {
        public String idString;
        public String parentIdString;
        public double x;
        public double y;
        public double z;
        public long createTimestamp;
        public String createTimeString;

        @JsonProperty
        public String getIdString() {
            return idString;
        }

        @JsonProperty
        public String getParentIdString() {
            return parentIdString;
        }

        @JsonProperty
        public double getX() { return x; }

        @JsonProperty
        public double getY() { return y; }

        @JsonProperty
        public double getZ() { return z; }

        @JsonProperty
        public long getCreateTimestamp() { return createTimestamp; }

        @JsonProperty
        public String getCreateTimeString() { return createTimeString; }

    }

    //=============================================================================================================//
    // METHODS
    //=============================================================================================================//

    @GET
    @Path("/workspaces")
    @Produces(MediaType.APPLICATION_JSON)
    @Formatted
    public List<WorkspaceInfo> getWorkspaces() {
        log.info("getWorkspaces() invoked");
        List<WorkspaceInfo> workspaces=new ArrayList<>();

        Session dbSession = null;
        try {
            dbSession = HibernateSessionUtils.getSession();

            StringBuilder hql = new StringBuilder(256);

            hql.append("select e.id, e.ownerKey, e.name, e.creationDate from Entity e ");
            hql.append("where e.entityTypeName = :entityTypeName ");

            Query q = dbSession.createQuery(hql.toString());

            q.setParameter("entityTypeName", EntityConstants.TYPE_TILE_MICROSCOPE_WORKSPACE);

            List queryResult = q.list();

            log.info("getWorkspaces() query returned "+queryResult.size()+" values");

            for (Object o : queryResult) {
                Object[] qo=(Object[])o;
                if (qo.length==4) {
                    Long q_id=(Long)qo[0];
                    String q_ownerKey=(String)qo[1];
                    String q_name=(String)qo[2];
                    Timestamp t=(Timestamp)qo[3];
                    WorkspaceInfo wi = new WorkspaceInfo();
                    wi.idString = q_id.toString();
                    wi.name = q_name;
                    wi.ownerName = q_ownerKey;
                    wi.createTimestamp = t.getTime();
                    wi.createTimeString = new Date(wi.createTimestamp).toString();
                    wi.dataType = "unknown";
                    wi.neuronCount = -1;

                    {
                        StringBuilder wql = new StringBuilder(256);
                        wql.append("select distinct e from Entity e ");
                        wql.append("where e.id = :entityId ");

                        Query wq = dbSession.createQuery(wql.toString());
                        wq.setParameter("entityId", q_id);
                        Object wo = wq.uniqueResult();
                        if (wo==null) {
                            log.info("Object returned is null");
                        }
                        Entity workspaceEntity = (Entity)wo;

                        findWorkspaceDataTypeAndNeuronCount(wi, workspaceEntity);

                    }

                    workspaces.add(wi);
                } else {
                    log.info("workspace list size unexpectedly="+qo.length);
                }
            }

        } finally {
            HibernateSessionUtils.closeSession(dbSession);
        }

        return workspaces;
    }

    @GET
    @Path("/workspaceInfo")
    @Produces(MediaType.APPLICATION_JSON)
    @Formatted
    public WorkspaceInfo getWorkspaceInfo(@QueryParam("id") String idString) {
        log.info("getWorkspaceInfo() invoked, id="+idString);

        Session dbSession = null;

        WorkspaceInfo wi=null;
        try {
            dbSession = HibernateSessionUtils.getSession();

            StringBuilder hql = new StringBuilder(256);

            hql.append("select distinct e from Entity e ");
            hql.append("where e.id = :entityId ");

            Query q = dbSession.createQuery(hql.toString());
            long entityId=new Long(idString);
            q.setParameter("entityId", entityId);
            Object o = q.uniqueResult();
            if (o==null) {
                log.info("Object returned is null");
            }
            Entity workspaceEntity = (Entity)o;

            wi = new WorkspaceInfo();
            wi.idString = workspaceEntity.getId().toString();
            wi.name = workspaceEntity.getName();
            wi.ownerName = workspaceEntity.getOwnerKey();
            wi.createTimestamp = workspaceEntity.getCreationDate().getTime();
            wi.createTimeString = new Date(wi.createTimestamp).toString();

            findWorkspaceDataTypeAndNeuronCount(wi, workspaceEntity);

        } finally {
            HibernateSessionUtils.closeSession(dbSession);
        }

        return wi;
    }

    @GET
    @Path("/neuronSummaryForWorkspace")
    @Produces(MediaType.APPLICATION_JSON)
    @Formatted
    public List<NeuronInfo> getNeuronSummaryForWorkspace(@QueryParam("id") String idString) {
        log.info("getNeuronSummaryForWorkspace() invoked, id="+idString);
        List<NeuronInfo> neuronInfoList=new ArrayList<>();
        try {
            final TiledMicroscopeBeanRemote tiledMicroscopeBeanRemote=EJBFactory.getRemoteTiledMicroscopeBean();
            Long workspaceId=new Long(idString);
            Set<TmNeuron> neurons=tiledMicroscopeBeanRemote.getNeuronsFromProtobufDataByWorkspaceId(workspaceId);
            for (TmNeuron neuron : neurons) {
                NeuronInfo neuronInfo=new NeuronInfo();
                neuronInfo.idString=neuron.getId().toString();
                neuronInfo.name=neuron.getName();
                neuronInfo.pointCount=neuron.getRootAnnotationCount();
                neuronInfo.createTimestamp=neuron.getCreationDate().getTime();
                neuronInfo.createTimeString=new Date(neuronInfo.createTimestamp).toString();
                neuronInfoList.add(neuronInfo);
            }
        } catch (Exception e) {
            log.error("Exception e="+e.getMessage());
        }
        log.info("Returning neuronInfoList of size="+neuronInfoList.size());
        return neuronInfoList;
    }

    @GET
    @Path("/pointSummaryForNeuron")
    @Produces(MediaType.APPLICATION_JSON)
    @Formatted
    public List<PointInfo> getPointSummaryForNeuron(@QueryParam("workspaceId") String workspaceIdString,
                                                    @QueryParam("neuronId") String neuronIdString) {
        log.info("getPointSummaryForNeuron() invoked, workspaceIdString="+workspaceIdString+" neuronIdString="+neuronIdString);
        List<PointInfo> pointInfoList=new ArrayList<>();
        try {
            final TiledMicroscopeBeanRemote tiledMicroscopeBeanRemote=EJBFactory.getRemoteTiledMicroscopeBean();
            Long workspaceId=new Long(workspaceIdString);
            Long neuronId=new Long(neuronIdString);
            Set<TmNeuron> neurons=tiledMicroscopeBeanRemote.getNeuronsFromProtobufDataByWorkspaceId(workspaceId);
            for (TmNeuron neuron : neurons) {
                if (neuron.getId().equals(neuronId)) {
                    Map<Long, TmGeoAnnotation> geoAnnotationMap=neuron.getGeoAnnotationMap();
                    for (TmGeoAnnotation tmGeoAnnotation : geoAnnotationMap.values()) {
                        PointInfo pointInfo=new PointInfo();
                        pointInfo.idString=tmGeoAnnotation.getId().toString();
                        pointInfo.parentIdString=tmGeoAnnotation.getParentId().toString();
                        pointInfo.x=tmGeoAnnotation.getX();
                        pointInfo.y=tmGeoAnnotation.getY();
                        pointInfo.z=tmGeoAnnotation.getZ();
                        pointInfoList.add(pointInfo);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Exception e="+e.getMessage());
        }
        log.info("Returning pointInfoList of size="+pointInfoList.size());
        return pointInfoList;
    }

    @GET
    @Path("/workspaceAddArtificialNeurons")
    @Produces(MediaType.APPLICATION_JSON)
    @Formatted
    public WorkspaceInfo addWorkspaceArtificialNeurons(
            @QueryParam("id") String idString,
            @QueryParam("name") String nameString,
            @QueryParam("number") String numberString,
            @QueryParam("points") String pointsString,
            @QueryParam("brprob") String brprobString) {
        log.info("addWorkspaceArtificialNeurons() invoked, id="+idString+" name="+nameString+" number="+numberString+" points="+pointsString+" brprob="+brprobString);

        Session dbSession = null;
        WorkspaceInfo wi=null;

        try {
            final EntityBeanRemote entityBeanRemote=EJBFactory.getRemoteEntityBean();
            Long workspaceId=new Long(idString);
            Entity workspaceEntity=EJBFactory.getRemoteEntityBean().getEntityById(null, workspaceId);
            int numberOfNeurons=new Integer(numberString);
            int numberOfPoints=new Integer(pointsString);
            double branchProbability=new Double(brprobString);
            for (int n=0;n<numberOfNeurons;n++) {

                // Must now push a new entity data - we have to save twice to get Id
                EntityData entityData = new EntityData();
                entityData.setOwnerKey(workspaceEntity.getOwnerKey());
                entityData.setCreationDate(new Date());
                entityData.setParentEntity(workspaceEntity);
                entityData.setEntityAttrName(EntityConstants.ATTRIBUTE_PROTOBUF_NEURON);
                entityData=entityBeanRemote.saveOrUpdateEntityData(workspaceEntity.getOwnerKey(), entityData);

                TmNeuron neuron=createArtificialNeuron(entityData.getId(), numberOfPoints, branchProbability, (long)n);
                neuron.setCreationDate(new Date());
                neuron.setName(nameString+"."+n);
                neuron.setOwnerKey(workspaceEntity.getOwnerKey());
                neuron.setWorkspaceId(workspaceId);
                neuron.setId(entityData.getId());

                TmProtobufExchanger exchanger=new TmProtobufExchanger();
                byte[] neuronBytes=exchanger.serializeNeuron(neuron);
                BASE64Encoder base64Encoder=new BASE64Encoder();
                String protobufString=base64Encoder.encode(neuronBytes);

                entityData.setValue(protobufString);
                entityBeanRemote.saveOrUpdateEntityData(workspaceEntity.getOwnerKey(), entityData);
            }
            wi = getWorkspaceInfo(idString);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        finally {
            HibernateSessionUtils.closeSession(dbSession);
        }

        return wi;
    }

    private static synchronized BlockTiffOctreeLoadAdapter getStashedLoadAdapterForSample(Long sampleId) {
        BlockTiffOctreeLoadAdapter blockTiffOctreeLoadAdapter=null;
        ExpirableLoadAdapter expirableLoadAdapter=sampleLoadAdapterMap.get(sampleId);
        if (expirableLoadAdapter==null) {
            log.info("getting new BlockTiffOctreeAdapter for sampleId="+sampleId);
            ExpirableLoadAdapter newAdapter=new ExpirableLoadAdapter();
            try {
                newAdapter.blockTiffOctreeLoadAdapter=getLoadAdapterForSample(sampleId);
                newAdapter.setupTime=new Date().getTime();
                sampleLoadAdapterMap.put(sampleId, newAdapter);
                blockTiffOctreeLoadAdapter=newAdapter.blockTiffOctreeLoadAdapter;
            } catch (Exception ex) {
                log.error("Error in getBlocTiffOctreeLoadAdapterForSample() sampleId="+sampleId+":"+ex.getMessage());
            }
        } else {
            long elapsedMs=new Date().getTime()-expirableLoadAdapter.setupTime;
            if (elapsedMs>LOAD_ADAPTER_TIMEOUT_MS) {
                log.info("Refreshing BlockTiffOctreeLoadAdapter for sampleId="+sampleId);
                expirableLoadAdapter.blockTiffOctreeLoadAdapter=getLoadAdapterForSample(sampleId);
                expirableLoadAdapter.setupTime=new Date().getTime();
                blockTiffOctreeLoadAdapter=expirableLoadAdapter.blockTiffOctreeLoadAdapter;
            } else {
                blockTiffOctreeLoadAdapter=expirableLoadAdapter.blockTiffOctreeLoadAdapter;
            }
        }
        return blockTiffOctreeLoadAdapter;
    }

    private static BlockTiffOctreeLoadAdapter getLoadAdapterForSample(Long sampleId) {
        BlockTiffOctreeLoadAdapter blockTiffOctreeLoadAdapter=null;
        try {
            blockTiffOctreeLoadAdapter = new BlockTiffOctreeLoadAdapter();
            Entity sampleEntity = EJBFactory.getLocalEntityBean().getEntityById(sampleId);
            String sampleTopLevelDirPath = sampleEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
            blockTiffOctreeLoadAdapter.setTopFolderAndCoordinateSource(new File(sampleTopLevelDirPath), new CoordinateToRawTransformFileSource() {
                @Override
                public CoordinateToRawTransform getCoordToRawTransform(String filePath) throws Exception {
                    RawFileFetcher fetcher = RawFileFetcher.getRawFileFetcher(filePath);
                    return fetcher.getTransform();
                }
            });
        } catch (Exception ex) {
            log.error("Error in getLoadAdapterForSample() sampleId="+sampleId+" ex="+ex.getMessage());
        }
        return blockTiffOctreeLoadAdapter;
    }

    @GET
    @Path("/sample2DTile")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public byte[] getSample2DTile(
            @QueryParam("sampleId") String sampleIdString,
            @QueryParam("x") String xString,
            @QueryParam("y") String yString,
            @QueryParam("z") String zString,
            @QueryParam("zoom") String zoomString,
            @QueryParam("maxZoom") String maxZoomString,
            @QueryParam("index") String indexString,
            @QueryParam("axis") String axisString) {
        byte[] textureData2dBytes = null;
        try {
            //log.info("getSample2DTile() invoked, sampleId=" + sampleIdString + " x=" + xString + " y=" + yString + " z=" + zString + " zoom=" + zoomString + " maxZoom=" + maxZoomString + " index=" + indexString + " axis=" + axisString);
            BlockTiffOctreeLoadAdapter blockTiffOctreeLoadAdapter=null;
            ExpirableLoadAdapter expirableLoadAdapter=sampleLoadAdapterMap.get(new Long(sampleIdString));
            if (expirableLoadAdapter!=null &&  ((new Date().getTime()-expirableLoadAdapter.setupTime)<LOAD_ADAPTER_TIMEOUT_MS)) {
                // THIS IS NOT SYNCHRONIZED
                blockTiffOctreeLoadAdapter=expirableLoadAdapter.blockTiffOctreeLoadAdapter;
            } else {
                // THIS IS SYNCHRONIZED
                blockTiffOctreeLoadAdapter=getStashedLoadAdapterForSample(new Long(sampleIdString));
            }
            TileIndex.IndexStyle indexStyle = null;
            if (indexString.equals("QUADTREE")) {
                indexStyle = TileIndex.IndexStyle.QUADTREE;
            } else {
                indexStyle = TileIndex.IndexStyle.OCTREE;
            }
            CoordinateAxis axis = null;
            if (axisString.equals("X")) {
                axis = CoordinateAxis.X;
            } else if (axisString.equals("Y")) {
                axis = CoordinateAxis.Y;
            } else if (axisString.equals("Z")) {
                axis = CoordinateAxis.Z;
            }
            TileIndex tileIndex = new TileIndex(new Integer(xString), new Integer(yString), new Integer(zString), new Integer(zoomString), new Integer(maxZoomString), indexStyle, axis);
            TextureData2d textureData2d = blockTiffOctreeLoadAdapter.loadToRam(tileIndex);
            if (textureData2d!=null) {
                textureData2dBytes = textureData2d.copyToByteArray();
            }
        } catch (Exception ex) {
            log.error("getSample2DTile() error: "+ex.getMessage());
            ex.printStackTrace();
        }
        return textureData2dBytes;
    }

    @GET
    @Path("/fileBytes")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public byte[] getFileBytes(@QueryParam("path") String path) {
        File file=new File(path);
        log.info("getFileBytes() file="+file.getAbsolutePath());
        byte[] fileBytes=null;
        try {
            fileBytes=Files.readAllBytes(file.toPath());
        } catch (Exception ex) {
            log.error("Error in getFileBytes() ="+ex.getMessage());
            ex.printStackTrace();
        }
        return fileBytes;
    }

    @GET
    @Path("/mouseLightTiffBytes")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public byte[] getMouseLightTiffBytes(@QueryParam("suggestedPath") String suggestedPath) {
        File actualFile=getMouseLightTiffFileBySuggestion(suggestedPath);
        if (actualFile!=null) {
            log.info("getMouseLightTiffBytes() file=" + actualFile.getAbsolutePath());
            byte[] fileBytes=null;
            try {
                fileBytes=Files.readAllBytes(actualFile.toPath());
            } catch (Exception ex) {
                log.error("Error in getMouseLightTiffBytes() =" + ex.getMessage());
                ex.printStackTrace();
            }
            return fileBytes;
        } else {
            return null;
        }
    }

    @GET
    @Path("/mouseLightTiffStream")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response getMouseLightTiffStream(@QueryParam("suggestedPath") String suggestedPath) {
        File actualFile=getMouseLightTiffFileBySuggestion(suggestedPath);
        if (actualFile!=null) {
            Response.ResponseBuilder responseBuilder=Response.ok(actualFile);
            responseBuilder.header("Content-Disposition", "attachment;filename="+actualFile.getName());
            return responseBuilder.build();
        } else {
            return null;
        }
    }

    //=================================================================================================//
    // UTILITIES
    //=================================================================================================//

    private File getMouseLightTiffFileBySuggestion(String suggestedPath) {
        File suggestedFile=new File(suggestedPath);
        File actualFile=null;
        if (suggestedFile.exists()) {
            actualFile=suggestedFile;
        } else {
            String suggestedName=suggestedFile.getName();
            int lastDot=suggestedName.lastIndexOf(".");
            String suggestedSuffix=suggestedName;
            if (lastDot>-1) {
                suggestedSuffix = suggestedName.substring(lastDot);
            }
            File parentDir=suggestedFile.getParentFile();
            File[] childFiles=parentDir.listFiles();
            log.info("getMouseLightTiffFileBySuggestion() using suggestedSuffix="+suggestedSuffix);
            for (File childFile : childFiles) {
                if (childFile.getName().endsWith(suggestedSuffix) ||
                        childFile.getName().endsWith(suggestedSuffix+".tif")) {
                    actualFile=childFile;
                    break;
                }
            }
        }
        return actualFile;
    }

    private void findWorkspaceDataTypeAndNeuronCount(WorkspaceInfo wi, Entity e) {
        Set<EntityData> entityDataSet=e.getEntityData();
        int protobufCount=0;
        for (EntityData ed : entityDataSet) {
            if (ed.getEntityAttrName().equals(EntityConstants.ATTRIBUTE_PROTOBUF_NEURON)) {
                protobufCount++;
            }
        }
        wi.dataType="analyzed";
        wi.neuronCount=0;
        if (protobufCount>0) {
            wi.dataType = "protobuf";
            wi.neuronCount = protobufCount;
        }
    }

    private TmNeuron createArtificialNeuron(long neuronId, int numberOfPoints, double branchProbability, long randomSeed) {
        final double CENTER_X=22000.0;
        final double CENTER_Y=10000.0;
        final double CENTER_Z=5600.0;

        Random random=new Random(randomSeed);

        long[] guidArr=getNextGuidArray(numberOfPoints);
        double[] xyz=new double[3];
        xyz[0]=CENTER_X;
        xyz[1]=CENTER_Y;
        xyz[2]=CENTER_Z;
        randomizePoint(xyz, 4000.0);
        TmNeuron neuron=new TmNeuron();
        Map<Long,TmGeoAnnotation> map=neuron.getGeoAnnotationMap();
        TmGeoAnnotation rootAnnotation = new TmGeoAnnotation();
        rootAnnotation.setId(guidArr[0]);
        rootAnnotation.setNeuronId(neuronId);
        rootAnnotation.setParentId(neuronId);
        neuron.addRootAnnotation(rootAnnotation);
        rootAnnotation.setCreationDate(new Date());
        rootAnnotation.setX(xyz[0]);
        rootAnnotation.setY(xyz[1]);
        rootAnnotation.setZ(xyz[2]);
        map.put(rootAnnotation.getId(), rootAnnotation);

        List<TmGeoAnnotation> endPoints=new LinkedList<>();
        endPoints.add(rootAnnotation);
        for (int i=1;i<numberOfPoints;i++) {
            TmGeoAnnotation geoAnnotation=new TmGeoAnnotation();
            geoAnnotation.setCreationDate(new Date());
            geoAnnotation.setId(guidArr[i]);
            geoAnnotation.setNeuronId(neuronId);
            map.put(geoAnnotation.getId(), geoAnnotation);
            int branchIndex=(int)(endPoints.size()*Math.random());
            TmGeoAnnotation branchEnd=endPoints.get(branchIndex);
            xyz[0]=branchEnd.getX();
            xyz[1]=branchEnd.getY();
            xyz[2]=branchEnd.getZ();
            randomizePoint(xyz, 20.0);
            geoAnnotation.setX(xyz[0]);
            geoAnnotation.setY(xyz[1]);
            geoAnnotation.setZ(xyz[2]);
            double branchCheck=random.nextDouble();
            if (branchCheck<branchProbability && i>10) {
                // Branch
                TmGeoAnnotation parent=map.get(branchEnd.getParentId());
                geoAnnotation.setParentId(parent.getId());
                parent.addChild(geoAnnotation);
                endPoints.add(geoAnnotation);
            } else {
                geoAnnotation.setParentId(branchEnd.getId());
                branchEnd.addChild(geoAnnotation);
                endPoints.remove(branchEnd);
                endPoints.add(geoAnnotation);
            }
        }
        return neuron;
    }

    private void randomizePoint(double[] pointArr, double radius) {
        Random random=new Random();
        double r2=radius/2.0;
        pointArr[0]+=(random.nextDouble()*radius-r2);
        pointArr[1]+=(random.nextDouble()*radius-r2);
        pointArr[2]+=(random.nextDouble()*radius-r2);
    }

}