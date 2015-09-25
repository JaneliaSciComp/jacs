#!/bin/sh

SCRIPT_DIR=$(cd "$(dirname "$0")"; pwd)
JAVA_MEMORY="6G"

# Prepare the tiler parameters
TILER_JAR_FILE="$SCRIPT_DIR/TileCATMAID-jar-with-dependencies.jar"

TILER_PARAMS="-DsourceUrlFormat=${SOURCE_URL_ROOT}/${SOURCE_STACK_FORMAT}"

if [ "$TARGET_ROOT_URL" != "" ]; then
    TILER_PARAMS="${TILER_PARAMS} -DexportBasePath=$TARGET_ROOT_URL"
fi							    

if [ "$TARGET_STACK_FORMAT" != "" ]; then
    TILER_PARAMS="${TILER_PARAMS} -DtilePattern=$TARGET_STACK_FORMAT"
fi								

if [ "$IMAGE_WIDTH" != "" ]; then
    TILER_PARAMS="${TILER_PARAMS} -DsourceWidth=${IMAGE_WIDTH}"
fi

if [ "$IMAGE_HEIGHT" != "" ]; then
    TILER_PARAMS="${TILER_PARAMS} -DsourceHeight=$IMAGE_HEIGHT"
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

if [ "$SOURCE_MIN_X" != "" ]; then
    TILER_PARAMS="${TILER_PARAMS} -DminX=$SOURCE_MIN_X"
fi				    

if [ "$SOURCE_MIN_Y" != "" ]; then
    TILER_PARAMS="${TILER_PARAMS} -DminY=$SOURCE_MIN_Y"
fi					

if [ "$SOURCE_MIN_Z" != "" ]; then
    TILER_PARAMS="${TILER_PARAMS} -DminZ=$SOURCE_MIN_Z"
fi					    

if [ "$SOURCE_WIDTH" != "" ]; then
    TILER_PARAMS="${TILER_PARAMS} -DsourceWidth=$SOURCE_WIDTH"
fi						

if [ "$SOURCE_HEIGHT" != "" ]; then
    TILER_PARAMS="${TILER_PARAMS} -DsourceHeight=$SOURCE_HEIGHT"
fi						    

if [ "$SOURCE_DEPTH" != "" ]; then
    TILER_PARAMS="${TILER_PARAMS} -DsourceDepth=$SOURCE_DEPTH"
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

# Invoke the tiler
echo java -Xms${JAVA_MEMORY} -Xmx${JAVA_MEMORY} ${TILER_PARAMS} -jar ${TILER_JAR_FILE}


# Prepare the scaler parameters
SCALER_JAR_FILE="$SCRIPT_DIR/ScaleCATMAID-jar-with-dependencies.jar"

TARGET_TILES_FORMAT=""

if [ "$TARGET_TILES_FORMAT_URL" != "" ]; then
    TARGET_TILES_FORMAT="${TARGET_TILES_FORMAT_URL}"
fi							    

if [ "$TARGET_STACK_FORMAT" != "" ]; then
    TARGET_TILES_FORMAT="$TARGET_TILES_FORMAT/$TARGET_STACK_FORMAT"
fi								

SCALER_PARAMS="-DtileFormat=${TARGET_TILES_FORMAT}"

if [ "$TARGET_MIN_Z" != "" ]; then
    SCALER_PARAMS="${SCALER_PARAMS} -DminZ=$TARGET_MIN_Z"
fi										    

if [ "$TARGET_MAX_Z" != "" ]; then
    SCALER_PARAMS="${SCALER_PARAMS} -DmaxZ=$TARGET_MAX_Z"
fi										    

if [ "$TARGET_TILE_WIDTH" != "" ]; then
    SCALER_PARAMS="${SCALER_PARAMS} -DtileWidth=$TARGET_TILE_WIDTH"
fi								    

if [ "$TARGET_TILE_HEIGHT" != "" ]; then
    SCALER_PARAMS="${SCALER_PARAMS} -DtileHeight=$TARGET_TILE_HEIGHT"
fi									

echo java -Xms${JAVA_MEMORY} -Xmx${JAVA_MEMORY} ${SCALER_PARAMS} -jar ${SCALER_JAR_FILE}
