#!/bin/sh
if [ $# -lt 2 ]; then
    /usr/bin/wget -v --post-data="action=invokeOpByName&name=ComputeServer%3Aservice%3DSampleDataManager&methodName=runAllSampleMaintenancePipelines" http://${1}:8180/jmx-console/HtmlAdaptor
else
    /usr/bin/wget -v --post-data="action=invokeOpByName&name=ComputeServer%3Aservice%3DSampleDataManager&methodName=runUserSampleMaintenancePipelines&argType=java.lang.String&arg0=${2}" http://${1}:8180/jmx-console/HtmlAdaptor
fi

