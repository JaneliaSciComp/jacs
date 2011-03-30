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
Based on code from http://www.econym.demon.co.uk/googlemaps/index.htm
*/
      function EuclideanProjection(zoomLevels){
        this.pixelsPerLonDegree=[];
        this.pixelsPerLonRadian=[];
        this.pixelOrigTileCenter=[];
        this.tileBounds=[];
        this.numZoomLevels=zoomLevels;

        var numHorizTilesPerLevel=1;
        var totalMapSize=256;
        for(var i=0; i<zoomLevels; i++){
          this.pixelsPerLonDegree.push(totalMapSize/360);
          this.pixelsPerLonRadian.push(totalMapSize/(2*Math.PI));
          this.pixelOrigTileCenter.push(new GPoint(totalMapSize/2,totalMapSize/2));
          this.tileBounds.push(numHorizTilesPerLevel);
          totalMapSize*=2;
          numHorizTilesPerLevel*=2
        }
      }

      // == Attach it to the GProjection() class ==
      EuclideanProjection.prototype=new GProjection();

      // == A method for converting latitudes and longitudes to pixel coordinates ==
      EuclideanProjection.prototype.fromLatLngToPixel=function(gLatLng,zoomLevel){
        if (zoomLevel > this.numZoomLevels)
          alert("bad zoom level " + zoomLevel);
        var x=Math.round(this.pixelOrigTileCenter[zoomLevel].x + gLatLng.lng()*this.pixelsPerLonDegree[zoomLevel]);
        var y=Math.round(this.pixelOrigTileCenter[zoomLevel].y + (-2*gLatLng.lat())*this.pixelsPerLonDegree[zoomLevel]);
        return new GPoint(x,y)
      };

      // == a method for converting pixel coordinates to latitudes and longitudes ==
      EuclideanProjection.prototype.fromPixelToLatLng=function(a,b,c){
        var d=(a.x-this.pixelOrigTileCenter[b].x)/this.pixelsPerLonDegree[b];
        var e=-0.5*(a.y-this.pixelOrigTileCenter[b].y)/this.pixelsPerLonDegree[b];
        return new GLatLng(e,d,c)
      };

      // == a method that checks if the y value is in range, and wraps the x value ==
      EuclideanProjection.prototype.tileCheckRange=function(a,b,c){
        var d=this.tileBounds[b];
        if (a.y<0||a.y>=d) {
          return false;
        }
        if(a.x<0||a.x>=d){
          a.x=a.x%d;
          if(a.x<0){
            a.x+=d;
          }
        }
        return true
      }

      // == a method that returns the width of the tilespace ==
      EuclideanProjection.prototype.getWrapWidth=function(zoom) {
        return this.tileBounds[zoom]*256;
      }
