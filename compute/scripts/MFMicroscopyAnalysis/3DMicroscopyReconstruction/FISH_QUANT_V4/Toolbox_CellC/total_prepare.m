function [bw,threshold,graynuclei]=total_prepare(nuclei,threshold,thrmethod,backsub)
%TOTAL_PREPARE   Convert RGB or grayscale cell image to binary.
% 
%   [bw,threshold,graynuclei]=total_prepare(nuclei,threshold,thrmethod,backsub)
% 
%   In:
%   nuclei, RGB or grayscale cell image.
%   threshold, global image threshold that is used in converting grayscale 
%   image to a binary image. If threshold = 2, automatic thresholding
%   is applied.
%   thrmethod, 'otsu' = select threshold automatically using Otsu
%   backsub, 1 = background is subtracted using polynomial surface fit
%   
%   Out:
%   bw, resulting binary image
%   threshold, threshold value used
%   graynuclei, resulting grayscale image
%
%   See also CELLC

% Copyright (C) Jyrki Selinummi, 5.11.2004
% This program is free software; you can redistribute it and/or
% modify it under the terms of the GNU General Public License.


% background subtraction is on by default
if nargin < 4
    backsub=1;
end

graynuclei=double(nuclei);
graynuclei=graynuclei-min(min(min(graynuclei)));
graynuclei=graynuclei/max(max(max(graynuclei)));

% RGB2grayscale
if size(nuclei,3)>1
    graynuclei=rgb2hsv(graynuclei);
    graynuclei=graynuclei(:,:,3);
end


% contrast enhancement
% NOTE! This same enhancement is done when using manual threshold
% selection (adjustthr.m).
se = strel('disk', 15);
Itop = imtophat(graynuclei, se);
Ibot = imbothat(graynuclei, se);
graynuclei = imsubtract(imadd(Itop, graynuclei), Ibot);
graynuclei=graynuclei-min(min(graynuclei));
graynuclei=graynuclei/max(max(graynuclei));

% contrast enhancement using histogram stretching (remove comments for testing)
% low_high = stretchlim(graynuclei);
% graynuclei = imadjust(graynuclei,low_high,[0 1]);

% Background subtraction using mathematical morphology (remove comments for testing)
% Note that adaptivity must be added to the size of the structuring
% element for the subtraction to work properly
% background = imopen(graynuclei,strel('disk', 60));
% graynuclei = imsubtract(graynuclei,background);

% Background subtraction using polynomial fit (remove comments for testing)
% S = polfit(graynuclei,2);
% graynuclei=graynuclei-S;
% graynuclei=graynuclei-min(min(graynuclei));
% graynuclei=graynuclei/max(max(graynuclei));

if backsub==1
    S=fit_2d(imresize(graynuclei,0.2));
    S=fit_2d(S');
    graynuclei=graynuclei-imresize(S',size(graynuclei));
    graynuclei=graynuclei-min(min(graynuclei));
    graynuclei=graynuclei/max(max(graynuclei));
end

% if threshold is set to 'Auto'
if threshold==2
    threshold = graythresh(graynuclei);
    if strcmp('otsu',thrmethod)
        bw = im2bw(graynuclei,threshold);
    elseif strcmp('localvar',thrmethod)
        bw = adthr(graynuclei);
    end
else
    % if threshold was manual
    bw = im2bw(graynuclei,threshold);
end