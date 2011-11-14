#!/bin/sh
/usr/bin/wget -v --post-data="action=invokeOp&name=ComputeServer%3Aservice%3DWorkstationDataManager&methodIndex=2&arg0=system&arg1=False" http://$1:8180/jmx-console/HtmlAdaptor

