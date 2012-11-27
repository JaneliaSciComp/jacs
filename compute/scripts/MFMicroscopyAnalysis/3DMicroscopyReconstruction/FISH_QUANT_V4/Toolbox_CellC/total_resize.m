function [nuclei_image,resize_factor]=total_resize(nuclei_image,do_resize)
%TOTAL_RESIZE   Automatically resize cell image.
%   [nuclei_image,resize_factor]=total_resize(nuclei_image,do_resize) 
%   returns a resized image.
%
%   In:
%   nuclei, RGB or grayscale cell image.
%   do_resize, if do_resize = 1 resizing is applied.
%
%   Out:
%   nuclei_image, resized image
%   resize_factor, size factor between input and output images
%

% Copyright (C) Jyrki Selinummi, 5.11.2004
% This program is free software; you can redistribute it and/or
% modify it under the terms of the GNU General Public License.

% Appr. size of image after resizing.
sizefactor=470000;

if do_resize==1
    resize_factor=sqrt(sizefactor/(size(nuclei_image,1)*size(nuclei_image,2)));
else 
    resize_factor=1;
end
warning off all
nuclei_image = imresize(nuclei_image,resize_factor);
warning on all