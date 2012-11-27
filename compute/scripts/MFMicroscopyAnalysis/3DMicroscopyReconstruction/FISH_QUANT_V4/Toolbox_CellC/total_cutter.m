function [labimg,number_of_nuclei]=total_cutter(x,small_nuclei,hmin,method,do_cut)
% TOTAL_CUTTER Separation of single cells from cell clusters using watershed
% 
%   [labimg,number_of_nuclei]=total_cutter(x,small_nuclei,hmin,method,do_cut)
% 
%   In:
%   x, binary input image
%   small_nuclei, grayscale version of x
%   hmin, suppress all watershed minima whose depth is less than hmin (see imhmin)
%   method, 'shape' = watershed calculated from distance transformed image
%           'inte' = watershed calculated directly from intensity image
%   do_cut, 1 =  separate the cell clusters, 0 = do not separate cells
%   
%   Out:
%   labimg, labeled output image, each cell labeled independently
%   number_of_nuclei, number of cells
%
%   See also CELLC

% Copyright (C) Jyrki Selinummi, 5.11.2004
% This program is free software; you can redistribute it and/or
% modify it under the terms of the GNU General Public License.

% Whether to cut or not. Now this function can make the final cutting decision 
if ~do_cut
    [labimg,number_of_nuclei] = bwlabel(x,4);
    return;
end

% Watershed calculated from distance transform (implemented for CELLC1.5)
if isequal(method,'shape')
    % Scale the imposed minima parameter to include a wider range of
    % values.
    % 0-1 was sufficient in intensity watershed since the intensity values
    % are scaled between 0 and 1, but distance transform needs more in some
    % cases
    hmin=hmin*5;
    distance=imcomplement(bwdist(imcomplement(x)));
    % suppress insignificant minima
    distance2 = imhmin(distance,hmin);
    lines=ones(size(distance2));
    % watershed lines in a matrix
    lines(watershed(distance2)==0)=0;
    % separate the cells
    y=lines&x;
    [labimg,number_of_nuclei] = bwlabel(y,4);
else 
    % Watershed calculated from intensity image
    
    % For this technique to work correctly, the cells/bacteria should
    % be brighter in the centers and darker around the edges.
    if (size(small_nuclei,3)==3)
        small_nuclei=rgb2gray(small_nuclei);
    end
    small_nuclei=double(small_nuclei);
    small_nuclei=small_nuclei/max(max(small_nuclei));
    lines=ones(size(small_nuclei));
    small_nuclei=imcomplement(small_nuclei);
    markers=imhmin(small_nuclei,hmin);
    lines(watershed(markers)==0)=0;
    y=lines&x;
    [labimg,number_of_nuclei] = bwlabel(y,4);
end
