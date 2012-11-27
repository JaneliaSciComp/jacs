function selected=spec_select(bw,marker)
% SPEC_SELECT Select objects that exist in both binary input images
% 
%   selected=spec_select(bw,marker)
% 
%   In:
%   bw, binary input image 1
%   marker, binary input image 2
%   
%   Out:
%   selected, binary image consisting of objects that are present in both
%             bw and marker input images
%
%
%   See also CELLC

% Copyright (C) Jyrki Selinummi, 5.11.2004
% This program is free software; you can redistribute it and/or
% modify it under the terms of the GNU General Public License.

% label the input and multiply with the marker to delete objects that are
% not present in the marker
labimg = bwlabel(bw,4);
sel_num=labimg.*marker;

% see what label numbers are remaining
remaining_labels=unique(sel_num);

% only those objects of input bw that are labeled with remaining numbers
% are left present in the output
selected = ismember(labimg,remaining_labels(2:end));
selected=selected>0;