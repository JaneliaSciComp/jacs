#
# Copyright (c) 2010-2011, J. Craig Venter Institute, Inc.
# 
# This file is part of JCVI VICS.
# 
# JCVI VICS is free software; you can redistribute it and/or modify it 
# under the terms and conditions of the Artistic License 2.0.  For 
# details, see the full text of the license in the file LICENSE.txt.  
# No other rights are granted.  Any and all third party software rights 
# to remain with the original developer.
# 
# JCVI VICS is distributed in the hope that it will be useful in 
# bioinformatics applications, but it is provided "AS IS" and WITHOUT 
# ANY EXPRESS OR IMPLIED WARRANTIES including but not limited to implied 
# warranties of merchantability or fitness for any particular purpose.  
# For details, see the full text of the license in the file LICENSE.txt.
# 
# You should have received a copy of the Artistic License 2.0 along with 
# JCVI VICS.  If not, the license can be obtained from 
# "http://www.perlfoundation.org/artistic_license_2_0."
# 

#
# This script shows how to export/import projects from a unix system
# such as camweb where the jacs project is deployed to Tomcat.
# Before running this, the 'hibernate.cfg.xml' file must be copied from the
# 'jacs/conf/container_descriptor' directory into the local directory
# as well as the 'jacs.properties' file. Examples of prop settings:
#    xml_project.ie.dir=./pubxmltest_export/new/project
#    xml_publication.ie.dir=./pubxmltest_export/new/publication
#

PROJECT_EXPORT_LIB=/usr/local/apache-tomcat-5.5.17/webapps/jacs/WEB-INF/lib
PEC=.
PEC=$PEC:$PROJECT_EXPORT_LIB/jacs-model.jar
PEC=$PEC:$PROJECT_EXPORT_LIB/jacs-shared.jar
PEC=$PEC:$PROJECT_EXPORT_LIB/jacs.jar
PEC=$PEC:$PROJECT_EXPORT_LIB/compute-client.jar
PEC=$PEC:$PROJECT_EXPORT_LIB/gwt-servlet.jar
PEC=$PEC:$PROJECT_EXPORT_LIB/hibernate3.jar
PEC=$PEC:$PROJECT_EXPORT_LIB/jaxb1-impl.jar
PEC=$PEC:$PROJECT_EXPORT_LIB/jaxb-api.jar
PEC=$PEC:$PROJECT_EXPORT_LIB/jaxb-impl.jar
PEC=$PEC:$PROJECT_EXPORT_LIB/jaxb-xjc.jar
PEC=$PEC:$PROJECT_EXPORT_LIB/log4j-1.2.14.jar
PEC=$PEC:$PROJECT_EXPORT_LIB/xmlsec.jar
PEC=$PEC:$PROJECT_EXPORT_LIB/xsdlib.jar
PEC=$PEC:$PROJECT_EXPORT_LIB/dom4j-1.6.1.jar
PEC=$PEC:$PROJECT_EXPORT_LIB/commons-beanutils.jar
PEC=$PEC:$PROJECT_EXPORT_LIB/commons-collections-2.1.1.jar
PEC=$PEC:$PROJECT_EXPORT_LIB/commons-fileupload.jar
PEC=$PEC:$PROJECT_EXPORT_LIB/commons-httpclient-3.0.1.jar
PEC=$PEC:/usr/local/apache-tomcat-5.5.17/bin/commons-logging-api.jar
PEC=$PEC:$PROJECT_EXPORT_LIB/standard.jar
PEC=$PEC:$PROJECT_EXPORT_LIB/activation.jar
PEC=$PEC:$PROJECT_EXPORT_LIB/antlr-2.7.6.jar
PEC=$PEC:$PROJECT_EXPORT_LIB/cglib.jar
PEC=$PEC:$PROJECT_EXPORT_LIB/postgresql-8.1-404.jdbc3.jar
PEC=$PEC:$PROJECT_EXPORT_LIB/jbossall-client.jar
/usr/local/java/bin/java -classpath $PEC org.janelia.it.jacs.server.ie.ProjectExporter
