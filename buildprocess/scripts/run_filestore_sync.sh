#!/bin/sh
/usr/bin/wget -v --post-data="action=invokeOpByName&name=ComputeServer%3Aservice%3DWorkstationDataManager&methodName=runSampleSyncService&argType=java.lang.String&arg0=system" http://$1:8180/jmx-console/HtmlAdaptor

