function img = load_stack_data_v4(file_name)
% Load a stack of images and return a structure:
%
%   img.data  : the stack of images
%   img.h     : the height of the images
%   img.w     : the width of the images
%   img.size  : the number of images in the stack 

% v3 
% - based on bfopen_v1 rather than tiff-read. Allows reading in
% deltavision files as well.


[dum, dum, ext] = fileparts(file_name);


if strcmpi(ext,'.tif') || strcmpi(ext,'.stk')
    img = tiffread29(file_name);
    img = dat2mat3d_v1(img);
else

    data = bfopen_v1(file_name);
    dum  = data{1};

    %-Dimensions of image
    [img.h   img.w]  = size(dum{1});
    img.size = size(dum,1);

    %- Intensity values of image
    data_mat = zeros(img.h,img.w,img.size);
    for iP =1:img.size
       data_mat(:,:,iP) = dum{iP,1};        
    end
    img.data = data_mat;
end
