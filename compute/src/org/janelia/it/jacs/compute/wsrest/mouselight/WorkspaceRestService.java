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
import org.janelia.it.jacs.compute.util.HibernateSessionUtils;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.entity.json.JsonTaskEvent;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmGeoAnnotation;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmNeuron;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmWorkspace;
import org.janelia.it.jacs.model.user_data.tiled_microscope_protobuf.TmProtobufExchanger;
import org.jboss.resteasy.annotations.providers.jaxb.Formatted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.BASE64Encoder;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.sql.Timestamp;
import java.util.*;


/**
 * Created by murphys on 3/24/2016.
 */

@Path("/mouselight")
public class WorkspaceRestService {

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

    @GET
    @Path("/sample2DTile")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public byte[] getSample2DTile(
            @QueryParam("x") String xString,
            @QueryParam("y") String yString,
            @QueryParam("z") String zString) {
        log.info("getSample2DTile() invoked, x=" + xString + " y=" + yString + " z=" + zString);

        byte[] testArr = new byte[10];

        for (int i = 0; i < 10; i++) {
            testArr[i] = (byte) i;
        }

        return testArr;
    }

    //=================================================================================================//
    // UTILITIES
    //=================================================================================================//

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