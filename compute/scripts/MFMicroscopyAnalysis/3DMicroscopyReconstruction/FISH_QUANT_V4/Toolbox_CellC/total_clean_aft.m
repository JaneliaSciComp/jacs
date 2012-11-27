function cut_nuclei=total_clean_aft(bw,do_ratio,auto_remove,ratio,toosmall,toolarge,resize_factor);
%TOTAL_CLEAN_AFT   Remove noise from cell image (after cutting).
%   cut_nuclei=total_clean_aft(bw,do_ratio,auto_remove,ratio,toosmall,toolarge,resize_factor)
%   returns a binary image removed of noise. Binary operations performed
%   after division of cell clusters can be added into this file.
%
%   In:
%   bw, binary image
%   do_ratio, binary value to specify whether scaling from pixels to 
%   micrometers is needed 
%   auto_remove, binary value to specify whether the object removal
%   thresholds are selected automatically
%   ratio, scaling ratio between pixels and micrometers
%   toosmall, if auto_remove=0, objects smaller than toosmall are removed
%   toolarge, if auto_remove=0, objects larger than toolarge are removed
%   resize_factor, scale the results to match image resizing
%
%   Out:
%   cut_nuclei, filtered image
%

% Copyright (C) Jyrki Selinummi, 5.11.2004
% This program is free software; you can redistribute it and/or
% modify it under the terms of the GNU General Public License.

% Markers of size below one tenth of the mean size of markers are considered noise
% and removed. Change "cutratio" value to change this. E.g. if cutratio is 0.2,
% markers of size below one fifth of the mean size of markers are removed.
% Changes to this file only affect cleaning after cutting. To change
% cutratio before cutting also, see function nuc_clean_bef.m

cutratio=0.10;

% remove objects smaller than cutratio*meansize
labimg = bwlabel(bw,4);
tempor = regionprops(labimg,'Area');

x=[tempor.Area];
if auto_remove
    idx = find([tempor.Area] > cutratio*mean(x));
else
    if do_ratio
        % scale the areas to match the ratio between pixels and microm.
        areas=[tempor.Area].*(ratio.^2);
    else
        areas=[tempor.Area];
    end
    % scale the areas to match the resizing done
    areas=areas*((1/resize_factor)^2);
    
    % remove too large and small objects
    idx = find((areas > toosmall)&(areas < toolarge));
end

labimg = ismember(labimg,idx);

labimg=(labimg>0);

cut_nuclei=labimg;

    