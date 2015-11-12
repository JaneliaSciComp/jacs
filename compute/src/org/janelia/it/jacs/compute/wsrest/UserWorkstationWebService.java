package org.janelia.it.jacs.compute.wsrest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;

import org.glassfish.jersey.server.ResourceConfig;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.Subject;
import org.janelia.it.jacs.model.domain.support.DomainDAO;
import org.janelia.it.jacs.model.domain.workspace.ObjectSet;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.janelia.it.jacs.model.domain.workspace.Workspace;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

@Path("/")
public class UserWorkstationWebService extends ResourceConfig {
    @Context
    SecurityContext securityContext;

    DomainDAO dao;

    public DomainDAO getDao() {
        if (dao==null) {
            dao = WebServiceContext.getDomainManager();
        }
        return dao;
    }

    public void setDao(DomainDAO dao) {
        this.dao = dao;
        WebServiceContext.setDomainManager(dao);
    }

    public UserWorkstationWebService() {
        register(JacksonJsonProvider.class);
    }

    // mapping using explicit object mapping; TO DO configure jackson integration with jersey
    @GET
    @Path("/workspace")
    @Produces(MediaType.APPLICATION_JSON)
    public String getWorkspace(@QueryParam("subjectKey") String subjectKey,
                               @QueryParam("option") String option) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            if (option!=null && option.toLowerCase().equals("full")) {
                Map<String, List<DomainObject>> workspaceMap = new HashMap<String, List<DomainObject>>();
                return mapper.writeValueAsString(dao.getWorkspaces(subjectKey));
            } else {
                Workspace workspace = dao.getDefaultWorkspace(subjectKey);
                return mapper.writeValueAsString(workspace);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @PUT
    @Path("/treenode")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String createTreeNode(@QueryParam("subjectKey") final String subjectKey,
                               TreeNode treeNode) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            TreeNode newTreeNode = dao.save(subjectKey, treeNode);
            return mapper.writeValueAsString(newTreeNode);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @POST
    @Path("/treenode/reorder")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String reorderTreeNode(@QueryParam("subjectKey") final String subjectKey,
                                  @QueryParam("treeNodeId") final Long treeNodeId,
                                  List<Integer> orderList) {
        ObjectMapper mapper = new ObjectMapper();
        int[] order = new int[orderList.size()];
        for (int i=0; i<orderList.size(); i++) {
            order[i] = orderList.get(i).intValue();
        }
        Reference treeNodeRef = new Reference("treeNode",treeNodeId);
        try {
            TreeNode treeNode = (TreeNode)dao.getDomainObject(subjectKey, treeNodeRef);
            TreeNode updatedNode = dao.reorderChildren(subjectKey, treeNode, order);
            return mapper.writeValueAsString(updatedNode);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @POST
    @Path("/treenode/children")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String addChildren(@QueryParam("subjectKey") final String subjectKey,
                              @QueryParam("treeNodeId") final Long treeNodeId,
                              @QueryParam("children") final List<Reference> children) {
        ObjectMapper mapper = new ObjectMapper();
        Reference treeNodeRef = new Reference("treeNode",treeNodeId);
        try {
            TreeNode treeNode = (TreeNode)dao.getDomainObject(subjectKey, treeNodeRef);
            TreeNode updatedNode = dao.addChildren(subjectKey, treeNode, children);
            return mapper.writeValueAsString(updatedNode);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    @DELETE
    @Path("/treenode/children")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String removeChildren(@QueryParam("subjectKey") final String subjectKey,
                                 @QueryParam("treeNodeId") final Long treeNodeId,
                                 @QueryParam("children") final List<Reference> children) {
        ObjectMapper mapper = new ObjectMapper();
        Reference treeNodeRef = new Reference("treeNode",treeNodeId);
        try {
            TreeNode treeNode = (TreeNode)dao.getDomainObject(subjectKey, treeNodeRef);
            TreeNode updatedNode = dao.removeChildren(subjectKey, treeNode, children);
            return mapper.writeValueAsString(updatedNode);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @PUT
    @Path("/objectset")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String createObjectSet(@QueryParam("subjectKey") final String subjectKey,
                                 ObjectSet objectSet) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            ObjectSet newObjectSet = dao.save(subjectKey, objectSet);
            return mapper.writeValueAsString(newObjectSet);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @POST
    @Path("/objectset/member")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String addMembers(@QueryParam("subjectKey") final String subjectKey,
                              @QueryParam("objectSetId") final Long objectSetId,
                              @QueryParam("members") final List<Long> members) {
        ObjectMapper mapper = new ObjectMapper();
        Reference objectSetRef = new Reference("objectSet",objectSetId);
        try {
            ObjectSet objectSet = (ObjectSet)dao.getDomainObject(subjectKey, objectSetRef);
            List<Reference> refs = new ArrayList<>();
            for(Long id : members) {
                refs.add(new Reference(objectSet.getClassName(), id));
            }
            ObjectSet updatedNode = dao.addMembers(subjectKey, objectSet, refs);
            return mapper.writeValueAsString(updatedNode);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @DELETE
    @Path("/objectset/member")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String removeMembers(@QueryParam("subjectKey") final String subjectKey,
                            @QueryParam("objectSetId") final Long objectSetId,
                            @QueryParam("members") final List<Long> members) {
        ObjectMapper mapper = new ObjectMapper();
        Reference objectSetRef = new Reference("objectSet",objectSetId);
        try {
            ObjectSet objectSet = (ObjectSet)dao.getDomainObject(subjectKey, objectSetRef);
            List<Reference> refs = new ArrayList<>();
            for(Long id : members) {
                refs.add(new Reference(objectSet.getClassName(), id));
            }
            ObjectSet updatedNode = dao.removeMembers(subjectKey, objectSet, refs);
            return mapper.writeValueAsString(updatedNode);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @GET
    @Path("/user/subjects")
    @Produces(MediaType.APPLICATION_JSON)
    public String getSubjects() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            List<Subject> subjects = dao.getSubjects();
            return mapper.writeValueAsString(subjects);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}