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

umask 000
DMA_DIR=.
CLASSPATH=.:$DMA_DIR/jar:$DMA_DIR/jar/*:$DMA_DIR/conf
DEBUG_OPTS="-Xnoagent -Djava.compiler=NONE -Xdebug -Xrunjdwp:transport=dt_socket
,server=y,suspend=n,address=5005"
JAVA_OPTS="-server -Xms256m -Xmx512m"
# use the same JMX port as the number of entries per partition
JMX_OPTS="-Dcom.sun.management.jmxremote.port=$3 -Dcom.sun.management.jmxremote.
authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Dvendor=shell "
# $1 -> source fasta file
# $2 -> destination directory
# $3 -> number of sequences per partition
java $JAVA_OPTS $JMX_OPTS -classpath $CLASSPATH org.janelia.it.jacs.shared.fasta.FastaFile -split -p p -t $2 -n 100000000 -e $3 -s $1
java $JAVA_OPTS $JMX_OPTS -classpath $CLASSPATH org.janelia.it.jacs.shared.dma.forma
tdb.FormatDBTool -i $2
