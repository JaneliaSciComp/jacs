#!/bin/sh
#
# Check for updates before starting the Janelia Workstation.
#

TEMP=/tmp
INSTALL=$(cd "$(dirname "$0")"; pwd)
EXTRA=""
if echo "$INSTALL" |grep -q "/Contents/Resources"; then
    # on Mac, get rid of the extra path from the app bundle root
    INSTALL=${INSTALL%Contents/Resources}
    EXTRA="/Contents/Resources"
fi

java -Xms512m -Xmx1024m -cp workstation.jar -Dlogback.configurationFile=logback_autoupdater.xml org.janelia.it.FlyWorkstation.gui.application.AutoUpdater > autoupdater.out 2>&1
DOWNLOAD=`cat autoupdater.out | sed '/^$/d' | tail -1`
if [ "$DOWNLOAD" != "" ] && [ -e "$DOWNLOAD" ] ; then
  echo "Updater downloaded a new version to $DOWNLOAD."

  cat > $TEMP/updateScript.sh << EOF
#!/bin/sh
echo "Updating..."
rm -rf $INSTALL/*
if cp -fR $DOWNLOAD/* "$INSTALL"; then
  echo "done. Update complete."
else
  echo "failed!"
fi
sh ${INSTALL}${EXTRA}/start.sh
EOF

  echo -n "Executing update..."
  exec sh $TEMP/updateScript.sh  
  exit 1
fi

echo "Already at latest version."
sh start.sh

