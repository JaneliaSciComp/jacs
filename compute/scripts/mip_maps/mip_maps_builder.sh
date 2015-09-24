#!/bin/sh

SCRIPT_DIR=$(cd "$(dirname "$0")"; pwd)
JAVA_MEMORY="6G"

TILER_JAR_FILE="$SCRIPT_DIR/TileCATMAID-jar-with-dependencies.jar"

PARAMS="-DsourceUrlFormat=${SOURCE_URL_ROOT}/${SOURCE_STACK_FORMAT}"

if [ "$IMAGE_WIDTH" != "" ]; then
    PARAMS="${PARAMS} -DsourceWidth=${IMAGE_WIDTH}"
fi

if [ "$IMAGE_HEIGHT" != "" ]; then
    PARAMS="${PARAMS} -DsourceHeight=$IMAGE_HEIGHT"
fi    

if [ "$SOURCE_MAGNIFICATION_LEVEL" != "" ]; then
    PARAMS="${PARAMS} -DsourceScaleLevel=$SOURCE_MAGNIFICATION_LEVEL"
fi		

if [ "$SOURCE_TILE_WIDTH" != "" ]; then
    PARAMS="${PARAMS} -DsourceTileWidth=$SOURCE_TILE_WIDTH"
fi

if [ "$SOURCE_TILE_HEIGHT" != "" ]; then
    PARAMS="${PARAMS} -DsourceTileHeight=$SOURCE_TILE_HEIGHT"
fi

if [ "$SOURCE_XY_RESOLUTION" != "" ]; then
    PARAMS="${PARAMS} -DsourceResXY=$SOURCE_XY_RESOLUTION"
fi

if [ "$SOURCE_Z_RESOLUTION" != "" ]; then
    PARAMS="${PARAMS} -DsourceResZ=$SOURCE_Z_RESOLUTION"
fi				

if [ "$SOURCE_MIN_X" != "" ]; then
    PARAMS="${PARAMS} -DminX=$SOURCE_MIN_X"
fi				    

if [ "$SOURCE_MIN_Y" != "" ]; then
    PARAMS="${PARAMS} -DminY=$SOURCE_MIN_Y"
fi					

if [ "$SOURCE_MIN_Z" != "" ]; then
    PARAMS="${PARAMS} -DminZ=$SOURCE_MIN_Z"
fi					    

if [ "$SOURCE_WIDTH" != "" ]; then
    PARAMS="${PARAMS} -DsourceWidth=$SOURCE_WIDTH"
fi						

if [ "$SOURCE_HEIGHT" != "" ]; then
    PARAMS="${PARAMS} -DsourceHeight=$SOURCE_HEIGHT"
fi						    

if [ "$SOURCE_DEPTH" != "" ]; then
    PARAMS="${PARAMS} -DsourceDepth=$SOURCE_DEPTH"
fi							

if [ "$TARGET_ROOT_URL" != "" ]; then
    PARAMS="${PARAMS} -DexportBasePath=$TARGET_ROOT_URL"
fi							    

if [ "$TARGET_STACK_FORMAT" != "" ]; then
    PARAMS="${PARAMS} -DtilePattern=$TARGET_STACK_FORMAT"
fi								

if [ "$TARGET_TILE_WIDTH" != "" ]; then
    PARAMS="${PARAMS} -DtileWidth=$TARGET_TILE_WIDTH"
fi								    

if [ "$TARGET_TILE_HEIGHT" != "" ]; then
    PARAMS="${PARAMS} -DtileHeight=$TARGET_TILE_HEIGHT"
fi									

if [ "$TARGET_MIN_ROW" != "" ]; then
    PARAMS="${PARAMS} -DexportMinR=$TARGET_MIN_ROW"
fi									    

if [ "$TARGET_MAX_ROW" != "" ]; then
    PARAMS="${PARAMS} -DexportMaxR=$TARGET_MAX_ROW"
fi									    

if [ "$TARGET_MIN_COL" != "" ]; then
    PARAMS="${PARAMS} -DexportMinC=$TARGET_MIN_COL"
fi										

if [ "$TARGET_MAX_COL" != "" ]; then
    PARAMS="${PARAMS} -DexportMaxC=$TARGET_MAX_COL"
fi										

if [ "$TARGET_MIN_Z" != "" ]; then
    PARAMS="${PARAMS} -DexportMinZ=$TARGET_MIN_Z"
fi										    

if [ "$TARGET_MAX_Z" != "" ]; then
    PARAMS="${PARAMS} -DexportMaxZ=$TARGET_MAX_Z"
fi										    

echo java -Xms${JAVA_MEMORY} -Xmx${JAVA_MEMORY} ${PARAMS} -jar ${TILER_JAR_FILE}
