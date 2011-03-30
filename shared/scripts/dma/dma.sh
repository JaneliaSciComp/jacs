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

DMA_DIR=/home/ccbuild/dma
CLASSPATH=.:$DMA_DIR/jar/*:$DMA_DIR/conf/*
cd $DMA_DIR/conf
JAVA=/usr/local/java/1.6.0_0/bin/java
${JAVA} -Dcom.sun.management.jmxremote.port=25000 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -classpath $CLASSPATH -Dvendor=shell org.janelia.it.jacs.shared.dma.DmaManager $*
cd $DMA_DIR/
