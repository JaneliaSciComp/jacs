function Ihat = fit_2d(I);
% FIT_2D 2 dimensional 2nd degree polynomial surface fit
% 
%   Ihat = fit_2d(I);
% 
%   In:
%   I, grayscale image
%
%   Out:
%   Ihat, matrix (image) consisting of the fit surface
%
%
%   See also CELLC

% Copyright (C) Pekka Ruusuvuori, 5.11.2004
% This program is free software; you can redistribute it and/or
% modify it under the terms of the GNU General Public License.


warning off all;
[h,w] = size(I);
[X,Y] = meshgrid(1:w,1:h);

% z = c1 + c2*x + c3*x.^2 + c4*x.*y + c5*y.^2 + c6*y 
A = [ones(h,1) X X.^2 X.*Y Y.^2 Y];

% least squares fit:
c = A\I;

% Ihat = points*coefficients
Ihat = A*c;
warning on all;