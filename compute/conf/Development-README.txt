In order to deploy the system into JBoss, that JBoss needs:
01/01/2007
- the postgres driver jar in the /server/default/lib/ directory
- the computeServer-ds.xml datasource descriptor file in the /server/default/deploy/ directory
- the compute.ear deployed
- ensure the deployed postgres db allows access to the all IP or subnet address on the project camweb.war deployments and compute.ear deployments

02/14/2007
- the jacs.property "BlastServer.GridMergeSortCmd" will have to have an appropriate path on the UCSD grid.