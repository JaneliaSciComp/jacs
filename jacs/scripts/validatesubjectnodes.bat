REM 
REM  Copyright (c) 2010-2011, J. Craig Venter Institute, Inc.
REM  
REM  This file is part of JCVI VICS.
REM  
REM  JCVI VICS is free software; you can redistribute it and/or modify it 
REM  under the terms and conditions of the Artistic License 2.0.  For 
REM  details, see the full text of the license in the file LICENSE.txt.  
REM  No other rights are granted.  Any and all third party software rights 
REM  to remain with the original developer.
REM  
REM  JCVI VICS is distributed in the hope that it will be useful in 
REM  bioinformatics applications, but it is provided "AS IS" and WITHOUT 
REM  ANY EXPRESS OR IMPLIED WARRANTIES including but not limited to implied 
REM  warranties of merchantability or fitness for any particular purpose.  
REM  For details, see the full text of the license in the file LICENSE.txt.
REM  
REM  You should have received a copy of the Artistic License 2.0 along with 
REM  JCVI VICS.  If not, the license can be obtained from 
REM  "http://www.perlfoundation.org/artistic_license_2_0."
REM  

java -cp %TOMCAT_HOME%\webapps\jacs;%TOMCAT_HOME%\webapps\jacs\WEB-INF;%TOMCAT_HOME%\webapps\jacs\WEB-INF\classes;%TOMCAT_HOME%\webapps\jacs\WEB-INF\lib;%TOMCAT_HOME%\webapps\jacs\WEB-INF\lib\jacs-model.jar;%TOMCAT_HOME%\webapps\jacs\WEB-INF\lib\jacs-shared.jar;%TOMCAT_HOME%\webapps\jacs\WEB-INF\lib\compute-client.jar;%TOMCAT_HOME%\webapps\jacs\WEB-INF\lib\jacs.jar;%TOMCAT_HOME%\webapps\jacs\WEB-INF\lib\postgresql-8.1-404.jdbc3.jar;%TOMCAT_HOME%\webapps\jacs\WEB-INF\lib\hibernate3.jar;%TOMCAT_HOME%\webapps\jacs\WEB-INF\lib\log4j-1.2.14.jar;%TOMCAT_HOME%\webapps\jacs\WEB-INF\lib\spring.jar;%TOMCAT_HOME%\webapps\jacs\WEB-INF\lib\dom4j-1.6.1.jar;%TOMCAT_HOME%\webapps\jacs\WEB-INF\lib\jta.jar;%TOMCAT_HOME%\webapps\jacs\WEB-INF\lib\cglib.jar;%TOMCAT_HOME%\webapps\jacs\WEB-INF\lib\commons-collections-2.1.1.jar;%TOMCAT_HOME%\webapps\jacs\WEB-INF\lib\ehcache-1.2.2.jar;%TOMCAT_HOME%\webapps\jacs\WEB-INF\lib\gwt-servlet.jar;%TOMCAT_HOME%\webapps\jacs\WEB-INF\lib\antlr-2.7.6.jar;%TOMCAT_HOME%\webapps\jacs\WEB-INF\lib\jbossall-client.jar;%TOMCAT_HOME%\bin\commons-logging-api.jar;%CLASSPATH% org.janelia.it.jacs.server.datavalidation.ValidateBlastableSubjectSets %*
