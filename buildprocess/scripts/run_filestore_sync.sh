#!/bin/sh
/usr/bin/wget -v --post-data="action=invokeOpByName&name=ComputeServer%3Aservice%3DSampleDataManager&methodName=runSampleTrashCompactor&argType=java.lang.String&arg0=${2}&argType=java.lang.Boolean&arg1=False" http://${1}:8180/jmx-console/HtmlAdaptor

