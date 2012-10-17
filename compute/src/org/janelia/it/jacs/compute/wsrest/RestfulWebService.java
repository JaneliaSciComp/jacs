package org.janelia.it.jacs.compute.wsrest;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.AnnotationBeanRemote;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.api.EntityBeanRemote;
import org.janelia.it.jacs.model.entity.DataSet;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityType;
import org.jboss.resteasy.annotations.providers.jaxb.Formatted;
import org.jboss.resteasy.annotations.providers.jaxb.Wrapped;
import org.jboss.resteasy.spi.NotFoundException;

import javax.ws.rs.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Defines RESTful web service entry points.
 *
 * <p>
 * The web-rest-ws.xml file references the resteasy components that are
 * used to load an instance of this class as a JAX-RS service.
 * </p>
 * <p>
 * The application.xml file defines the root context for this service.
 * A root context of "/rest-v1" would make resources available at: <br/>
 * http://[compute-server]:8180/rest-v1/[annotated path]
 * (e.g. http://jacs:8180/rest-v1/dataSet ).
 * </p>
 *
 * @author Eric Trautman
 */
@Path("/")
public class RestfulWebService {

    private Logger logger = Logger.getLogger(this.getClass());

    /**
     * Retrieve data sets that match specified filter criteria.
     *
     * @param  userList             list of user login names for filter
     *                              (or null if all are desired).
     *
     * @param  includeOnlySageSync  if defined (not null), indicates
     *                              that only sage sync data sets should
     *                              be returned.
     *
     * @return list of data sets that match the specified filter criteria.
     *
     * @throws NotFoundException
     *   if no matching data sets can be found.
     */
    @GET
    @Path("dataSet")
    @Produces("application/xml")
    @Formatted
    @Wrapped(element = "dataSetList")
    public List<DataSet> getDataSets(
            @QueryParam("user") List<String> userList,
            @QueryParam("includeOnlySageSync") String includeOnlySageSync)
            throws NotFoundException{

        List<Entity> entityList = null;
        try {
            final AnnotationBeanRemote annotationBean =
                    EJBFactory.getRemoteAnnotationBean();
            if (userList == null) {
                entityList = annotationBean.getAllDataSets();
            } else {
                entityList = annotationBean.getUserDataSets(userList);
            }
        } catch (Exception e) {
            logger.error("getDataSets: failed retrieval, userList=" +
                         userList, e);
        }

        final boolean includeOnlySageSyncFlag =
                Boolean.parseBoolean(includeOnlySageSync);
        final List<DataSet> dataSetList =
                toDataSetList(entityList,
                              includeOnlySageSyncFlag);

        if ((dataSetList == null) || (dataSetList.size() == 0)) {
            StringBuilder msg = new StringBuilder(256);
            msg.append("There are no");
            if (includeOnlySageSyncFlag) {
                msg.append(" SAGE Sync ");
            }
            msg.append(" data sets");
            if ((userList != null) && (userList.size() > 0)) {
                    msg.append(" for the following user(s): ");
                    msg.append(userList);
            }
            msg.append('.');

            throw new NotFoundException(msg.toString());
        }

        if (logger.isDebugEnabled()) {
            logger.debug("getDataSets: exit, returning " + dataSetList.size() +
                    " data sets, userList=" + userList +
                    ", includeOnlySageSync=" + includeOnlySageSyncFlag);
        }

        return dataSetList;
    }

    /**
     * @return list of all supported entity types.
     */
    @GET
    @Path("entityType")
    @Produces("application/xml")
    @Formatted
    @Wrapped(element = "entityTypeList")
    public List<EntityType> getEntityTypes() {

        List<EntityType> list = null;
        try {
            EntityBeanRemote entityBean = EJBFactory.getRemoteEntityBean();
            list = entityBean.getEntityTypes();
        } catch (Exception e) {
            logger.error("getEntityTypes: failed retrieval", e);
        }

        if (list == null) {
            list = new ArrayList<EntityType>();
        }

        if (logger.isDebugEnabled()) {
            logger.debug("getEntityTypes: exit, returning " + list.size() +
                         " entity types");
        }

        return list;
    }

    // This prototype method has been included in the initial check-in just
    // in case we want to restore it later.
    // Make sure to consider the security access implications if/when
    // the method gets restored.
    @GET
    @Path("entity/id/{entityId}")
    @Produces("application/xml")
    @Formatted
    public Entity getEntity(@PathParam("entityId") String entityId) {

        logger.debug("getEntity: entry, entityId=" + entityId);

        Entity entity = null;
        try {
            EntityBeanRemote entityBean = EJBFactory.getRemoteEntityBean();
            entity = entityBean.getEntityById(entityId);
        } catch (Exception e) {
            logger.error("getEntity: failed to retrieve entityId " + entityId,
                    e);
        }

        logger.debug("getEntity: exit");

        return entity;
    }

    /**
     * Wraps {@link Entity} objects in the specified list as {@link DataSet}
     * objects so that JAXB can marshall the resulting list more clearly.
     *
     * @param  entityList           list of entity objects to wrap.
     * @param  includeOnlySageSync  indicates whether list should be filtered
     *                              to only include entities with a
     *                              defined sage sync attribute.
     *
     * @return list of wrapped objects.
     */
    private List<DataSet> toDataSetList(List<Entity> entityList,
                                        boolean includeOnlySageSync) {
        List<DataSet> dataSetList = null;
        if (entityList != null) {

            dataSetList = new ArrayList<DataSet>(entityList.size());
            DataSet dataSet;
            for (Entity entity : entityList) {

                if (entity != null) {
                    dataSet = new DataSet(entity);
                    if ((! includeOnlySageSync) || dataSet.hasSageSync()) {
                        dataSetList.add(dataSet);
                    }

                }
            }

        }

        return dataSetList;
    }


}
