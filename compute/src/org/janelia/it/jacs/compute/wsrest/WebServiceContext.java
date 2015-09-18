package org.janelia.it.jacs.compute.wsrest;

import java.io.IOException;
import java.util.*;
import java.io.File;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.janelia.it.jacs.compute.access.mongodb.*;

/**
 * Created by schauderd on 8/8/15.
 */
@javax.servlet.annotation.WebListener
public class WebServiceContext implements ServletContextListener  {
    private static DomainDAO domainManager;

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> config = (Map<String,Object>)mapper.readValue(new File(classLoader.getResource("c3p0-nosql.json").getFile()), Map.class);

            // load providers
            Map<String, Object> mongoConfig = (Map<String,Object>)config.get("mongo");
            if (mongoConfig != null) {
                if (mongoConfig.containsKey("connection")) {
                    Map<String, Object> connectionInfo = (Map<String, Object>) mongoConfig.get("connection");

                    String host = (String) connectionInfo.get("host");
                    String db = (String) connectionInfo.get("db");
                    //String username = (String) connectionInfo.get("flylight");
                    //String password = (String) connectionInfo.get("flylight");

                    domainManager = new DomainDAO(host, db);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static DomainDAO getDomainManager() {
        if (domainManager==null) {
            try {
                ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> config = (Map<String,Object>)mapper.readValue(classLoader.getResourceAsStream("c3p0-nosql.json"), Map.class);

                // load providers
                Map<String, Object> mongoConfig = (Map<String,Object>)config.get("mongodb");
                if (mongoConfig != null) {
                    if (mongoConfig.containsKey("connection")) {
                        Map<String, Object> connectionInfo = (Map<String, Object>) mongoConfig.get("connection");

                        String host = (String) connectionInfo.get("host");
                        String db = (String) connectionInfo.get("dbname");
                        String username = (String) connectionInfo.get("username");
                        String password = (String) connectionInfo.get("password");

                        domainManager = new DomainDAO(host, db, null, null);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return domainManager;
    }

    public static void setDomainManager(DomainDAO domainManager) {
        WebServiceContext.domainManager = domainManager;
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {

    }
}
