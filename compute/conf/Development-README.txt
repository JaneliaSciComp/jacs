In order to deploy the system into JBoss, that JBoss needs:
10-09-2011
- the database driver driver jar in the /server/default/lib/ directory
- the computeServer-ds.xml datasource descriptor file in the /server/default/deploy/ directory
- the compute.ear deployed
- ensure the deployed db allows access to the all IP or subnet address on the project jacsweb.war deployments and compute.ear deployments

