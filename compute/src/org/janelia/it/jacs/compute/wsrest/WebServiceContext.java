package org.janelia.it.jacs.compute.wsrest;

import java.io.IOException;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.janelia.it.jacs.compute.access.mongodb.DomainDAOManager;
import org.janelia.it.jacs.compute.access.mongodb.SolrConnector;
import org.janelia.it.jacs.model.domain.support.DomainDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by schauderd on 8/8/15.
 */
@javax.servlet.annotation.WebListener
public class WebServiceContext implements ServletContextListener  {
    private static final Logger log = LoggerFactory.getLogger(DataViewsWebService.class);

    private static DomainDAO mongo;
    private static SolrConnector solr;

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        init();
    }

    public static void init() {
        if (WebServiceContext.mongo==null) {
            WebServiceContext.mongo = DomainDAOManager.getInstance().getDao();
        }
        if (WebServiceContext.solr==null) {
            try {
                WebServiceContext.solr = new SolrConnector(WebServiceContext.mongo, false, false);
            } catch (IOException e) {
                log.error("Couldn't initialize solr server connection", e);
            }
        }
    }

    public static DomainDAO getDomainManager() {
        init();
        return mongo;
    }

    public static void setDomainManager(DomainDAO mongo) {
        WebServiceContext.mongo = mongo;
    }

    public static SolrConnector getSolr() {
        init();
        return solr;
    }

    public static void setSolr(SolrConnector solr) {
        WebServiceContext.solr = solr;
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {

    }
}
