1. Register mysql driver

Use
`bin/jboss-cli.sh`

`
module add --name=mysql-connector-java-5.1.40 --resources=/Users/goinac/Tools/wildfly-10.1.0.Final/standalone/lib/ext  mysql-connector-java-5.1.40.jar

/subsystem=datasources/jdbc-driver=mysql:add(driver-name="mysql", driver-module-name="mysql-connector-java-5.1.40", driver-class-name=com.mysql.jdbc.Driver, driver-datasource-class-name=com.mysql.jdbc.jdbc2.optional.MysqlDataSource, driver-xa-datasource-class-name=com.mysql.jdbc.jdbc2.optional.MysqlXADataSource)

data-source add --name=ComputeDS --driver-name=mysql --jndi-name=java:jboss/datasources/ComputeDS --connection-url=jdbc:mysql://localhost:3306/jacs2 --user-name=jacs2 --password=jacs2 --connection-properties={"url"=>"jdbc:mysql://localhost:3306/jacs2"}

`
