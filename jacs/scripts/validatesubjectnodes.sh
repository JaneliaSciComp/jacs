#!/bin/sh

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

if test $1; then
    CATALINA_HOME=$1
else
:   ${CATALINA_HOME:=/usr/local/apache-tomcat-5.5.23}
fi

: ${CATALINA_BASE:=${CATALINA_HOME}}
: ${APACHE_COMMONS_LOGGING:=${CATALINA_HOME}/bin/commons-logging-api.jar}

: ${JACSWEB_DEPLOY_DIR:=${CATALINA_BASE}/webapps/jacs}

JACSWEB_CLASSPATH=\
${JACSWEB_DEPLOY_DIR}:\
${JACSWEB_DEPLOY_DIR}/WEB-INF:\
${JACSWEB_DEPLOY_DIR}/WEB-INF/classes:\
${JACSWEB_DEPLOY_DIR}/WEB-INF/lib:\
${JACSWEB_DEPLOY_DIR}/WEB-INF/lib/jacs.jar:\
${JACSWEB_DEPLOY_DIR}/WEB-INF/lib/jacs-model.jar:\
${JACSWEB_DEPLOY_DIR}/WEB-INF/lib/jacs-shared.jar:\
${JACSWEB_DEPLOY_DIR}/WEB-INF/lib/compute-client.jar:\
${JACSWEB_DEPLOY_DIR}/WEB-INF/lib.jar:\
${JACSWEB_DEPLOY_DIR}/WEB-INF/lib/postgresql-8.1-404.jdbc3.jar:\
${JACSWEB_DEPLOY_DIR}/WEB-INF/lib/hibernate3.jar:\
${JACSWEB_DEPLOY_DIR}/WEB-INF/lib/log4j-1.2.14.jar:\
${JACSWEB_DEPLOY_DIR}/WEB-INF/lib/spring.jar:\
${JACSWEB_DEPLOY_DIR}/WEB-INF/lib/dom4j-1.6.1.jar:\
${JACSWEB_DEPLOY_DIR}/WEB-INF/lib/jta.jar:\
${JACSWEB_DEPLOY_DIR}/WEB-INF/lib/cglib.jar:\
${JACSWEB_DEPLOY_DIR}/WEB-INF/lib/commons-collections-2.1.1.jar:\
${JACSWEB_DEPLOY_DIR}/WEB-INF/lib/ehcache-1.2.2.jar:\
${JACSWEB_DEPLOY_DIR}/WEB-INF/lib/gwt-servlet.jar:\
${JACSWEB_DEPLOY_DIR}/WEB-INF/lib/antlr-2.7.6.jar:\
${JACSWEB_DEPLOY_DIR}/WEB-INF/lib/jbossall-client.jar:\
${APACHE_COMMONS_LOGGING}:\
$CLASSPATH

java -cp ${JACSWEB_CLASSPATH} org.janelia.it.jacs.server.datavalidation.Vali\
dateBlastableSubjectSets $*
