#!/bin/bash

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

# required parameters are:
#       java executable
#       java classpath
#       main class
#       template file path
#       task id
#


#JAVA_OPT="-Xrunjdwp:transport=dt_socket,address=15006,server=y,suspend=y"
if [ $# -lt 5 ]
then
        echo "Invalid number of arguments"
        exit 1
fi

JAVA_EXE=$1
JAVA_CP=$2
SUBMITTER=$3
shift 3

$JAVA_EXE -cp $JAVA_CP $JAVA_OPT $SUBMITTER $*

# possible commands are:
#/usr/local/java/1.6.0/bin/java -cp /home/lkagan/test/compute-drmaa.jar org.janelia.it.jacs.compute.drmaa.DrmaaSubmitter template=/tmp/DrmaaTemplate10771.oos task_id=4434634456 bulk=1,3,1 loop_sleep=10 return=system

#/usr/local/java/1.6.0/bin/java -cp /home/lkagan/test/compute-drmaa.jar org.janelia.it.jacs.compute.drmaa.DrmaaSubmitter template=/tmp/DrmaaTemplate10771.oos task_id=4434634456 return=queue
