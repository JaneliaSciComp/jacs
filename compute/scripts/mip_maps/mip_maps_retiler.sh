#!/bin/sh

SCRIPT_DIR=$(cd "$(dirname "$0")"; pwd)
JAVA_MEMORY="6G"

# Prepare the tiler parameters
TILER_JAR_FILE="$SCRIPT_DIR/TileCATMAID-jar-with-dependencies.jar"

TILER_PARAMS="-DsourceUrlFormat=${SOURCE_URL_ROOT}/${SOURCE_STACK_FORMAT} -Dinterpolation=NN"

if [ "$TARGET_ROOT_URL" != "" ]; then
    TILER_PARAMS="${TILER_PARAMS} -DexportBasePath=$TARGET_ROOT_URL"
fi

if [ "$TARGET_STACK_FORMAT" != "" ]; then
    TILER_PARAMS="${TILER_PARAMS} -DtilePattern=$TARGET_STACK_FORMAT"
fi								

if [ "$TARGET_MEDIA_FORMAT" != "" ]; then
    TILER_PARAMS="${TILER_PARAMS} -Dformat=$TARGET_MEDIA_FORMAT"
fi										    

if [ "$IMAGE_WIDTH" != "" ]; then
    TILER_PARAMS="${TILER_PARAMS} -DsourceWidth=${IMAGE_WIDTH}"
    TILER_PARAMS="${TILER_PARAMS} -Dwidth=$IMAGE_WIDTH"
fi

if [ "$IMAGE_HEIGHT" != "" ]; then
    TILER_PARAMS="${TILER_PARAMS} -DsourceHeight=$IMAGE_HEIGHT"
    TILER_PARAMS="${TILER_PARAMS} -Dheight=$IMAGE_HEIGHT"
fi

if [ "$IMAGE_DEPTH" != "" ]; then
    TILER_PARAMS="${TILER_PARAMS} -DsourceDepth=$IMAGE_DEPTH"
fi    

if [ "$SOURCE_MAGNIFICATION_LEVEL" != "" ]; then
    TILER_PARAMS="${TILER_PARAMS} -DsourceScaleLevel=$SOURCE_MAGNIFICATION_LEVEL"
fi		

if [ "$SOURCE_TILE_WIDTH" != "" ]; then
    TILER_PARAMS="${TILER_PARAMS} -DsourceTileWidth=$SOURCE_TILE_WIDTH"
fi

if [ "$SOURCE_TILE_HEIGHT" != "" ]; then
    TILER_PARAMS="${TILER_PARAMS} -DsourceTileHeight=$SOURCE_TILE_HEIGHT"
fi

if [ "$SOURCE_XY_RESOLUTION" != "" ]; then
    TILER_PARAMS="${TILER_PARAMS} -DsourceResXY=$SOURCE_XY_RESOLUTION"
fi

if [ "$SOURCE_Z_RESOLUTION" != "" ]; then
    TILER_PARAMS="${TILER_PARAMS} -DsourceResZ=$SOURCE_Z_RESOLUTION"
fi				

if [ "$SOURCE_DEPTH" != "" ]; then
    TILER_PARAMS="${TILER_PARAMS} -Ddepth=$SOURCE_DEPTH"
fi							

if [ "$TARGET_TILE_WIDTH" != "" ]; then
    TILER_PARAMS="${TILER_PARAMS} -DtileWidth=$TARGET_TILE_WIDTH"
fi								    

if [ "$TARGET_TILE_HEIGHT" != "" ]; then
    TILER_PARAMS="${TILER_PARAMS} -DtileHeight=$TARGET_TILE_HEIGHT"
fi									

if [ "$TARGET_MIN_ROW" != "" ]; then
    TILER_PARAMS="${TILER_PARAMS} -DexportMinR=$TARGET_MIN_ROW"
fi									    

if [ "$TARGET_MAX_ROW" != "" ]; then
    TILER_PARAMS="${TILER_PARAMS} -DexportMaxR=$TARGET_MAX_ROW"
fi									    

if [ "$TARGET_MIN_COL" != "" ]; then
    TILER_PARAMS="${TILER_PARAMS} -DexportMinC=$TARGET_MIN_COL"
fi										

if [ "$TARGET_MAX_COL" != "" ]; then
    TILER_PARAMS="${TILER_PARAMS} -DexportMaxC=$TARGET_MAX_COL"
fi										

if [ "$TARGET_MIN_Z" != "" ]; then
    TILER_PARAMS="${TILER_PARAMS} -DexportMinZ=$TARGET_MIN_Z"
fi										    

if [ "$TARGET_MAX_Z" != "" ]; then
    TILER_PARAMS="${TILER_PARAMS} -DexportMaxZ=$TARGET_MAX_Z"
fi										    

if [ "$TARGET_QUALITY" != "" ]; then
    TILER_PARAMS="${TILER_PARAMS} -Dquality=$TARGET_QUALITY"
fi										    

if [ "$TARGET_TYPE" != "" ]; then
    TILER_PARAMS="${TILER_PARAMS} -Dtype=$TARGET_TYPE"
fi										    

if [ "$TARGET_MEDIA_FORMAT" != "" ]; then
    TILER_PARAMS="${TILER_PARAMS} -Dformat=$TARGET_MEDIA_FORMAT"
fi										    

if [ "$TARGET_SKIP_EMPTY_TILES" != "" ]; then
    TILER_PARAMS="${TILER_PARAMS} -DignoreEmptyTiles=$TARGET_SKIP_EMPTY_TILES"
fi 

# Invoke the tiler
tiler_cmd="java -Xms${JAVA_MEMORY} -Xmx${JAVA_MEMORY} ${TILER_PARAMS} -jar ${TILER_JAR_FILE}"
echo "Execute $tiler_cmd"

java -Xms${JAVA_MEMORY} -Xmx${JAVA_MEMORY} ${TILER_PARAMS} -jar ${TILER_JAR_FILE}
