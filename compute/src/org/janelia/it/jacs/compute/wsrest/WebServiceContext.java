package org.janelia.it.jacs.compute.wsrest;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.janelia.it.jacs.compute.access.mongodb.SolrConnector;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.domain.support.DomainDAO;

import com.fasterxml.jackson.databind.ObjectMapper;
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
    private static String MONGO_SERVER_URL = SystemConfigurationProperties.getString("MongoDB.ServerURL");
    private static String MONGO_DATABASE = SystemConfigurationProperties.getString("MongoDB.Database");
    private static String MONGO_USERNAME = SystemConfigurationProperties.getString("MongoDB.Username");
    private static String MONGO_PASSWORD = SystemConfigurationProperties.getString("MongoDB.Password");

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        init();
    }

    public static void init() {
        if (WebServiceContext.mongo==null) {
            try {
                WebServiceContext.mongo = new DomainDAO(MONGO_SERVER_URL, MONGO_DATABASE, MONGO_USERNAME, MONGO_PASSWORD);
            } catch (IOException e) {
                log.error("Couldn't initialize database connection for RESTful services", e);
            }
        }
        if (WebServiceContext.solr==null) {
            try {
                WebServiceContext.solr = new SolrConnector(WebServiceContext.mongo);
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
