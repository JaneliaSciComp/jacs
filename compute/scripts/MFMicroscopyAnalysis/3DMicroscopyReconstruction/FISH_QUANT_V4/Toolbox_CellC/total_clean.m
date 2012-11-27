function clean=total_clean(bwnuclei,fill)
%TOTAL_CLEAN   Remove noise from cell image (before cutting).
%   TOTAL_CLEAN(BWNUCLEI,METHOD) returns a binary image removed of
%   noise. Binary operations performed before division of cell clusters can
%   be added into this file.
%
%   In:
%   bwnuclei, binary image
%   fill, binary value whether to fill holes of objects or not
%
%   Out:
%   clean, filtered image
%

% Copyright (C) Jyrki Selinummi, 5.11.2004
% This program is free software; you can redistribute it and/or
% modify it under the terms of the GNU General Public License.

% Markers of size below one tenth of the mean size of markers are considered noise
% and removed. Change "cutratio" value to change this. E.g. if cutratio is 0.2,
% markers of size below one fifth of the mean size of markers are removed.
% Changes to this file only affect cleaning before cutting. To change
% cutratio after cutting also, see function nuc_clean_aft.m
%cutratio=0.1;

% if no special method is selected, fill is performed.
% in CELLC fill is NOT performed by default
if fill == 1
    warning off;
    bwnuclei=imfill(bwnuclei,'holes');
    warning on;
end

% delete all objects smaller than 0.1*meansize
% [labeled,N] = bwlabel(bwnuclei,4);
% tempor = regionprops(labeled,'Area');
% x=[tempor.Area];
% idx = find([tempor.Area] > cutratio*mean(x));
% labimg = ismember(labeled,idx);
% labimg=labimg>0;

% erode image to separate nuclei better (calculation of solidity gives
% better results if eroding is done)
%tempor2 = bwlabel(labimg,4);
%tempor = regionprops(tempor2,'Area');
%minarea=(2/3)*mean([tempor.Area]);
% no erode if nuclei are "too small"
%if minarea>300
%    labimg=bwmorph(labimg,'erode');
%    labimg=bwmorph(labimg,'erode');
%end

%clean=labimg;
clean=bwnuclei;
