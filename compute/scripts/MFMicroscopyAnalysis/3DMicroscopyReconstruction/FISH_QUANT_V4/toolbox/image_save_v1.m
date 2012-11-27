function image_save_v1(img,name_save)
%
% Function to save PSF as actual image. 
%
% Florian Mueller, muef@gmx.net
%
% === INPUT PARAMETER
% img       ... 3d array with actual image
% name_save ... Name to save file (path+file name). If empty user will be
%               asked to specify name
%
% === FLAGS
%
%
% === OUTPUT PARAMETERS
%
%
% === VERSION HISTORY
%
% v1 Feb 8,2011
% - Initial implementation


if isempty(name_save)
    file_default                    = ['PSF_exp_',datestr(date, 'yymmdd'),'.tif'];
    [file_name_save,path_name_save] = uiputfile(file_default,'Specify file name to save results'); 

    name_save = fullfile(path_name_save,file_name_save);
else
    file_name_save = 1;
end

%% Write image only if file name was specified
if file_name_save ~= 0

    % Check if file is already present - then don't write    
    if not(exist(name_save,'file'))

        % Dimensions of image
        [dim.Y dim.X dim.Z] = size(img);

        %- Write image
        for iZ = 1:1:dim.Z
            img_plane = uint16(round(img(:,:,iZ)));
            imwrite(img_plane,name_save,'tif','Compression','none','WriteMode','append')   
        end
    end
    
else
   warndlg('File already exists - choose different file-name','PSF_3D_save_image_v1')    
end