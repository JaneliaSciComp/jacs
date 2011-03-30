/*
 * Copyright (c) 2010-2011, J. Craig Venter Institute, Inc.
 *
 * This file is part of JCVI VICS.
 *
 * JCVI VICS is free software; you can redistribute it and/or modify it
 * under the terms and conditions of the Artistic License 2.0.  For
 * details, see the full text of the license in the file LICENSE.txt.  No
 * other rights are granted.  Any and all third party software rights to
 * remain with the original developer.
 *
 * JCVI VICS is distributed in the hope that it will be useful in
 * bioinformatics applications, but it is provided "AS IS" and WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTIES including but not limited to
 * implied warranties of merchantability or fitness for any particular
 * purpose.  For details, see the full text of the license in the file
 * LICENSE.txt.
 *
 * You should have received a copy of the Artistic License 2.0 along with
 * JCVI VICS.  If not, the license can be obtained from
 * "http://www.perlfoundation.org/artistic_license_2_0."
 */
/*
GZoom custom map control. Version 0.2 Released 11/21/06

To use:
  oMap = new GMap2($id("large-google-map"));
  oMap.addControl(new GMapTypeControl());

Or with options:
  oMap.addControl(new GZoomControl({sColor:'#000',nOpacity:.3,sBorder:'1px solid yellow'}), new GControlPosition(G_ANCHOR_TOP_RIGHT,new GSize(10,10)));

More info at http://earthcode.com
*/

// base definition and inheritance
function GZoomControl(oBoxStyle,oOptions,oCallbacks) {
	//box style options
  GZoomControl.G.style = {
    nOpacity:.2,
    sColor:"#000",
    sBorder:"2px solid blue"
  };
  var style=GZoomControl.G.style;
  for (var s in oBoxStyle) {style[s]=oBoxStyle[s]};
  var aStyle=style.sBorder.split(' ');
  style.nOutlineWidth=parseInt(aStyle[0].replace(/\D/g,''));
  style.sOutlineColor=aStyle[2];
  style.sIEAlpha='alpha(opacity='+(style.nOpacity*100)+')';

	// Other options
	GZoomControl.G.options={
		bForceCheckResize:false,
		sButtonHTML:'zoom ...',
		oButtonStartingStyle:{width:'52px',border:'1px solid black',padding:'0px 5px 1px 5px'},
		oButtonStyle:{background:'#FFF'},
		sButtonZoomingHTML:'Drag a region on the map',
		oButtonZoomingStyle:{background:'#FF0'},
		nOverlayRemoveMS:6000,
		bStickyZoom:false
	};

	for (var s in oOptions) {GZoomControl.G.options[s]=oOptions[s]};

	// callbacks: buttonClick, dragStart,dragging, dragEnd
	if (oCallbacks == null) {oCallbacks={}};
	GZoomControl.G.callbacks=oCallbacks;
}

GZoomControl.prototype = new GControl();

//class globals
GZoomControl.G={
  bDragging:false,
  mapCoverTop:null,
  mapCoverRight:null,
  mapCoverBottom:null,
  mapCoverLeft:null,
	oMapPos:null,
	oOutline:null,
	nMapWidth:0,
	nMapHeight:0,
	nMapRatio:0,
	nStartX:0,
	nStartY:0,
	nBorderCorrect:0
};

GZoomControl.prototype.initButton_=function(oMapContainer) {
	var G=GZoomControl.G;
	var oButton = document.createElement('div');
	oButton.innerHTML=G.options.sButtonHTML;
	oButton.id='gzoom-control';
	acl.style([oButton],{cursor:'pointer',zIndex:200});
	acl.style([oButton],G.options.oButtonStartingStyle);
	acl.style([oButton],G.options.oButtonStyle);
	oMapContainer.appendChild(oButton);
	return oButton;
};

GZoomControl.prototype.setButtonMode_=function(sMode){
	var G=GZoomControl.G;
	if (sMode=='zooming') {
		G.oButton.innerHTML=G.options.sButtonZoomingHTML;
        G.oButton.style.display='block';
        acl.style([G.oButton],G.options.oButtonZoomingStyle);
	} else {
		G.oButton.innerHTML=G.options.sButtonHTML;
        G.oButton.style.display='none';
		acl.style([G.oButton],G.options.oButtonStyle);
	}
};

// ******************************************************************************************
// Methods required by Google maps -- initialize and getDefaultPosition
// ******************************************************************************************
GZoomControl.prototype.initialize = function(oMap) {
  var G=GZoomControl.G;
	var oMC=oMap.getContainer();
  //DOM:button
	var oButton=this.initButton_(oMC);

	//DOM:map covers
	var o = document.createElement("div");
    o.id='gzoom-map-cover';
	o.innerHTML='<div id="gzoom-outline" style="position:absolute;display:none;"></div><div id="gzoom-mct" style="position:absolute;display:none;"></div><div id="gzoom-mcl" style="position:absolute;display:none;"></div><div id="gzoom-mcr" style="position:absolute;display:none;"></div><div id="gzoom-mcb" style="position:absolute;display:none;"></div>';
	acl.style([o],{position:'absolute',display:'none',overflow:'hidden',cursor:'crosshair',zIndex:101});
	oMC.appendChild(o);

  // add event listeners
	GEvent.addDomListener(oButton, 'click', GZoomControl.prototype.buttonClick_);
	GEvent.addDomListener(o, 'mousedown', GZoomControl.prototype.coverMousedown_);
	GEvent.addDomListener(document, 'mousemove', GZoomControl.prototype.drag_);
	GEvent.addDomListener(document, 'mouseup', GZoomControl.prototype.mouseup_);

  // get globals
	G.oMapPos=acl.getElementPosition(oMap.getContainer());
	G.oOutline=$id("gzoom-outline");
	G.oButton=$id("gzoom-control");
	G.mapCover=$id("gzoom-map-cover");
	G.mapCoverTop=$id("gzoom-mct");
	G.mapCoverRight=$id("gzoom-mcr");
	G.mapCoverBottom=$id("gzoom-mcb");
	G.mapCoverLeft=$id("gzoom-mcl");
	G.oMap = oMap;

	G.nBorderCorrect = G.style.nOutlineWidth*2;
  this.setDimensions_();

  //styles
  this.initStyles_();

  debug("Finished Initializing gzoom control");
  return oButton;
};

// Default location for the control
GZoomControl.prototype.getDefaultPosition = function() {
  return new GControlPosition(G_ANCHOR_TOP_LEFT, new GSize(3, 120));
};

// ******************************************************************************************
// Private methods
// ******************************************************************************************
GZoomControl.prototype.coverMousedown_ = function(e){
  var G=GZoomControl.G;
  var oPos = GZoomControl.prototype.getRelPos_(e);
  debug("Mouse down at "+oPos.left+", "+oPos.top);

  G.bDragging=true;
  G.nStartX=oPos.left;
  G.nStartY=oPos.top;
  GZoomControl.prototype.drawRectangle_(G, G.nStartX, G.nStartY, 10, 10, e);

  // invoke the callback if provided
  if (G.callbacks.dragStart !=null){G.callbacks.dragStart(G.nStartX,G.nStartY)};

  debug("mouse down done");
  return false;
};

/** The map erroneously reports the x value as the map width at lat==-180, so force it to wrap to 0 */
GZoomControl.prototype.fromLatLngToDivPixelCorrected = function(latLng) {
    var G=GZoomControl.G;
    var gpoint = G.oMap.fromLatLngToDivPixel(latLng);
    if (gpoint.x == G.oMap.getSize().width)
        gpoint.x = 0;
    return gpoint;
}

/**
 * Implementation of fromLatLngToContainerPixel() that GMap2 should have but doesn't - calculates the distance a
 * point is from the upper left edge of the map
*/
GZoomControl.prototype.fromLatLngToContainerPixelNew = function(latLng, correctForX) {
    var G=GZoomControl.G;

    // Convert the provided GLatLng into pixel coordinates relative to the map's container, correcting for X if requested
    var pt;
    if (correctForX)
        pt = GZoomControl.prototype.fromLatLngToDivPixelCorrected(latLng);
    else
        pt = G.oMap.fromLatLngToDivPixel(latLng);

    // Grab the visible bounds of the map and translate the NW corner into pixel coords relative to the map's container
    // nw corner and translating it into pixels: we want the northern latitude and the western longitude
    var boundsSW = G.oMap.getBounds().getSouthWest();
    var boundsNE = G.oMap.getBounds().getNorthEast();
    var nwPixel = GZoomControl.prototype.fromLatLngToDivPixelCorrected(new GLatLng(boundsNE.lat(), boundsSW.lng()));
    //alert(
    //    "pt lat/lng   :" + latLng.lat() + " / " + latLng.lng() + "\n" +
    //    "pt     x/y   :" + pt.x + " / " + pt.y + "\n" +
    //    "nwPix lat/lng:" + boundsNE.lat() + " / " + boundsSW.lng() + "\n" +
    //    "nwPix  x/y   :" + nwPixel.x + " / " + nwPixel.y + "\n" +
    //    "");

    // Return a GPoint that represents the X and Y distances in pixels from the NW map corner to the point passed in
    return new GPoint(pt.x - nwPixel.x, pt.y - nwPixel.y);
}

GZoomControl.prototype.redrawRectangle=function(swLat, swLng, neLat, neLng) {
    var G=GZoomControl.G;
    
    // Convert the rectangle coords from lat/lng to map pixels so we can draw the outline 
    var nwpt = GZoomControl.prototype.fromLatLngToContainerPixelNew(new GLatLng(neLat, swLng), true);
    var sept = GZoomControl.prototype.fromLatLngToContainerPixelNew(new GLatLng(swLat, neLng), false);
    
    var left = nwpt.x;
    var top = nwpt.y;
    var width = sept.x - nwpt.x;
    var height = sept.y - nwpt.y;
    //alert(
    //    "nwPt lat/lng: " + neLat + " / " + swLng + "\n" +
    //    "nwPt     x/y: " + nwpt.x + " / " + nwpt.y + "\n" +
    //    "left: " + left + "\n" +
    //    "top : " + top + "\n" +
    //    "width  : " + width + "\n" +
    //    "height : " + height + "\n" +
    //    "");

    // redraw the dashed outline at the location
    acl.style([G.oOutline], {
        left:  left+'px',
        top:   top+'px',
        width :(width-G.nBorderCorrect)+'px',
        height:(height-G.nBorderCorrect)+'px',
        display:'block'
    });

    // Update the map covers to reflect the location
    G.mapCoverTop.style.top='0px';
    G.mapCoverTop.style.height=top+'px';

    G.mapCoverLeft.style.left='0px';
    G.mapCoverLeft.style.top=(top)+'px';
    G.mapCoverLeft.style.width=(left)+'px';

    G.mapCoverRight.style.left=(left+width)+'px';
    G.mapCoverRight.style.top=(top)+'px';
    G.mapCoverRight.style.height=(G.nMapHeight-top)+'px';

    G.mapCoverBottom.style.left=(left)+'px';
    G.mapCoverBottom.style.top=(top+height)+'px';
    G.mapCoverBottom.style.width=(width)+'px';
}

GZoomControl.prototype.drawRectangle_ = function(G, left, top, width, height, e){
    acl.style([G.mapCover],{background:'transparent',opacity:1,filter:'alpha(opacity=100)'});
    acl.style([G.oOutline],{left:left+'px',top:top+'px',display:'block',width:width+'px',height:height+'px'});

    G.mapCoverTop.style.top='0px';
    G.mapCoverTop.style.height=(top)+'px';
    G.mapCoverTop.style.display='block';

    G.mapCoverLeft.style.left='0px';
    G.mapCoverLeft.style.width=(left)+'px';
    G.mapCoverLeft.style.top=(top)+'px';
    G.mapCoverLeft.style.display='block';

    G.mapCoverRight.style.left=(left)+'px';
    G.mapCoverRight.style.top=(top)+'px';
    G.mapCoverRight.style.height=(G.nMapHeight-top)+'px';
    G.mapCoverRight.style.display='block';

    G.mapCoverBottom.style.left=(left)+'px';
    G.mapCoverBottom.style.top=(top)+'px';
    G.mapCoverBottom.style.width='0px';
    G.mapCoverBottom.style.display='block';

    var oPos=GZoomControl.prototype.getRelPos_(e);
    var oRec = GZoomControl.prototype.getRectangle_(G.nStartX,G.nStartY,oPos,G.nMapRatio);
    GZoomControl.prototype.updateRectangle_(G,oRec);
};

GZoomControl.prototype.updateRectangle_ = function(G,oRec){
    G.oOutline.style.width=oRec.nWidth+"px";
    G.oOutline.style.height=oRec.nHeight+"px";

    G.mapCoverRight.style.left=(oRec.nEndX+G.nBorderCorrect)+'px';
    G.mapCoverBottom.style.top=(oRec.nEndY+G.nBorderCorrect)+'px';
    G.mapCoverBottom.style.width=(oRec.nWidth+G.nBorderCorrect)+'px';
}

GZoomControl.prototype.drag_=function(e){
  var G=GZoomControl.G;
  if(G.bDragging) {
    var oPos=GZoomControl.prototype.getRelPos_(e);
    var oRec = GZoomControl.prototype.getRectangle_(G.nStartX,G.nStartY,oPos,G.nMapRatio);
    GZoomControl.prototype.updateRectangle_(G,oRec);

	// invoke callback if provided
	if (G.callbacks.dragging !=null){G.callbacks.dragging(G.nStartX,G.nStartY,oRec.nEndX,oRec.nEndY)};

    return false;
  }
};

GZoomControl.prototype.mouseup_=function(e){
  var G=GZoomControl.G;
  if (G.bDragging) {
    G.bDragging=false;

    // Here's the selected rectangle
    var oPos = GZoomControl.prototype.getRelPos_(e);
    var oRec = GZoomControl.prototype.getRectangle_(G.nStartX,G.nStartY,oPos,G.nMapRatio);

    // Calculate the lat/long of the rectangle for the callback
	var nwpx=new GPoint(oRec.nStartX,oRec.nStartY);
	var nepx=new GPoint(oRec.nEndX,oRec.nStartY);
	var sepx=new GPoint(oRec.nEndX,oRec.nEndY);
	var swpx=new GPoint(oRec.nStartX,oRec.nEndY);
	var nw = G.oMap.fromContainerPixelToLatLng(nwpx);
    var ne = G.oMap.fromContainerPixelToLatLng(nepx);
    var se = G.oMap.fromContainerPixelToLatLng(sepx);
    var sw = G.oMap.fromContainerPixelToLatLng(swpx);

	// invoke callback if provided
	if (G.callbacks.dragEnd !=null){G.callbacks.dragEnd(nw,ne,se,sw)};

    //re-init if sticky
	//if (G.options.bStickyZoom) {GZoomControl.prototype.initCover_()};
  }
};

// set the cover sizes according to the size of the map
GZoomControl.prototype.setDimensions_=function() {
  var G=GZoomControl.G;
  if (G.options.bForceCheckResize){
      G.oMap.checkResize()
  };
  var oSize = G.oMap.getSize();
  G.nMapWidth  = oSize.width;
  G.nMapHeight = oSize.height;
  G.nMapRatio  = G.nMapHeight/G.nMapWidth;
  acl.style([G.mapCover,G.mapCoverTop,G.mapCoverRight,G.mapCoverBottom,G.mapCoverLeft],
            {width:G.nMapWidth+'px', height:G.nMapHeight+'px'});
};

GZoomControl.prototype.initStyles_=function(){
  var G=GZoomControl.G;
  acl.style([G.mapCover,G.mapCoverTop,G.mapCoverRight,G.mapCoverBottom,G.mapCoverLeft],{filter:G.style.sIEAlpha,opacity:G.style.nOpacity,background:G.style.sColor});
  G.oOutline.style.border=G.style.sBorder;
  debug("done initStyles_");
};

// The zoom button's click handler.
GZoomControl.prototype.buttonClick_=function(){
  if (GZoomControl.G.mapCover.style.display=='block'){ // reset if clicked before dragging
    GZoomControl.prototype.resetDragZoom_();
  } else {
		GZoomControl.prototype.initCover_();
	}
};

// Shows the cover over the map
GZoomControl.prototype.initCover_=function(){
  var G=GZoomControl.G;
	G.oMapPos=acl.getElementPosition(G.oMap.getContainer());
	GZoomControl.prototype.setDimensions_();
	GZoomControl.prototype.setButtonMode_('zooming');
	acl.style([G.mapCover],{display:'block',background:G.style.sColor});
	acl.style([G.oOutline],{width:'0px',height:'0px'});
	//invoke callback if provided
	if(GZoomControl.G.callbacks['buttonClick'] !=null){GZoomControl.G.callbacks.buttonClick()};
	debug("done initCover_");
};

GZoomControl.prototype.getRelPos_=function(e) {
  var oPos=acl.getMousePosition (e);
  var G=GZoomControl.G;
  return {top:(oPos.top-G.oMapPos.top),left:(oPos.left-G.oMapPos.left)};
};

GZoomControl.prototype.getRectangle_=function(nStartX,nStartY,oPos,nRatio){
	var dX=oPos.left-nStartX;
	var dY=oPos.top-nStartY;
	if (dX <0) dX =dX*-1;
	if (dY <0) dY =dY*-1;
//	delta = dX > dY ? dX : dY;

  return {
    nStartX:nStartX,
    nStartY:nStartY,
    nEndX:nStartX+dX,
    nEndY:nStartY+dY,
    nWidth:dX,
    nHeight:dY
  }
};

GZoomControl.prototype.resetDragZoom_=function() {
	var G=GZoomControl.G;
	acl.style([G.mapCover,G.mapCoverTop,G.mapCoverRight,G.mapCoverBottom,G.mapCoverLeft],{display:'none',opacity:G.style.nOpacity,filter:G.style.sIEAlpha});
	G.oOutline.style.display='none';
	GZoomControl.prototype.setButtonMode_('normal');
  debug("done with reset drag zoom");
};

/* alias get element by id */
function $id(sId) { return document.getElementById(sId); }
/* utility functions in acl namespace */
if (!window['acldefined']) {var acl={};window['acldefined']=true;}//only set the acl namespace once, then set a flag

/* A general-purpose function to get the absolute position of
the mouse */
acl.getMousePosition=function(e) {
	var posx = 0;
	var posy = 0;
	if (!e) var e = window.event;
	if (e.pageX || e.pageY) {
		posx = e.pageX;
		posy = e.pageY;
	} else if (e.clientX || e.clientY){
		posx = e.clientX + (document.documentElement.scrollLeft?document.documentElement.scrollLeft:document.body.scrollLeft);
		posy = e.clientY + (document.documentElement.scrollTop?document.documentElement.scrollTop:document.body.scrollTop);
	}
	return {left:posx, top:posy};
};

/*
To Use:
	var pos = acl.getElementPosition(element);
	var left = pos.left;
	var top = pos.top;
*/
acl.getElementPosition=function(eElement) {
  var nLeftPos = eElement.offsetLeft;          // initialize var to store calculations
	var nTopPos = eElement.offsetTop;            // initialize var to store calculations
	var eParElement = eElement.offsetParent;     // identify first offset parent element
	while (eParElement != null ) {                // move up through element hierarchy
		nLeftPos += eParElement.offsetLeft;      // appending left offset of each parent
		nTopPos += eParElement.offsetTop;
		eParElement = eParElement.offsetParent;  // until no more offset parents exist
	}
	return {left:nLeftPos, top:nTopPos};
};
//elements is either a coma-delimited list of ids or an array of DOM objects. o is a hash of styles to be applied
//example: style('d1,d2',{color:'yellow'});
acl.style=function(a,o){
	if (typeof(a)=='string') {a=acl.getManyElements(a);}
	for (var i=0;i<a.length;i++){
		for (var s in o) { a[i].style[s]=o[s];}
	}
};
acl.getManyElements=function(s){
	t=s.split(',');
	a=[];
	for (var i=0;i<t.length;i++){a[a.length]=$id(t[i])};
	return a;
};

var jslog = {debug:function(){},info:function(){},
	warning:function(){}, error:function(){},
	text:function(){}}; var debug=function(){};
if (location.href.match(/enablejslog/)){
		document.write('<script type="text/javascript" src="http://earthcode.com/includes/scripts/jslog.js"></script>');};
