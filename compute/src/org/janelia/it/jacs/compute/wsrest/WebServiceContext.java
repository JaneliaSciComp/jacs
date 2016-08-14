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
    private static final Logger log = LoggerFactory.getLogger(WebServiceContext.class);
    private static SolrConnector solr;

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        init();
    }

    public static void init() {
        if (WebServiceContext.solr==null) {
           WebServiceContext.solr = new SolrConnector(DomainDAOManager.getInstance().getDao(), false, false);
        }
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
