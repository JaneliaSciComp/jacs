Besides permanent change to {TOMCAT_HOME}/conf/context.xml setting <Context antiJARLocking="true">
(for hot-deployment of wars) there are now 3 artifacts that need to be deployed to Tomcat:

    -jacs.war -> {TOMCAT_HOME}/webapps
    -jaas.config -> {TOMCAT_HOME}/conf
    -jacs-jaasplugin.jar -> {TOMCAT_HOME}/common/lib
    -copy the server.xml file into {TOMCAT_HOME}/conf/server.xml
        This sets up a system which authenticates with LDAP
    -Java process running Tomcat has to be started with one extra command line argument:
	-Djava.security.auth.login.config={TOMCAT_HOME}/conf/jaas.config

For logging:
    Add log4j.jar and commons-logging.jar to {TOMCAT_HOME}/common/lib
    Add log4j.properties to {TOMCAT_HOME}/common/classes
        see jacs/conf/log4j.properties for example.
        