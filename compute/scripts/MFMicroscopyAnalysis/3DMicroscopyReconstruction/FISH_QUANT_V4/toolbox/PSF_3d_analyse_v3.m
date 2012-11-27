function [img_PSF_struct PSF_fit file_name_full] = PSF_3d_analyse_v3(file_name_full, parameters)

%== Parameters
pixel_size     = parameters.pixel_size;
par_microscope = parameters.par_microscope;
flags          = parameters.flags;
par_crop       = parameters.par_crop;
PSF_BGD        = parameters.PSF_BGD;


%== Get file-name if not defined
if isempty(file_name_full) || not(exist(file_name_full))
    [file_name_PSF,path_name_PSF] = uigetfile('.tif','Select image of PSF which should be analyzed','MultiSelect','off');
    file_name_full = fullfile(path_name_PSF,file_name_PSF);
else
    file_name_PSF = 1;
end

%== Read-in if specified
if file_name_PSF ~= 0
    
    %=== Read in file 
    img_PSF_struct = load_stack_data_v4(file_name_full);
    
    
    %=== Background subtraction    
    
    %- Scalar (can also be zero)    
    if  size(PSF_BGD,1)*size(PSF_BGD,2) == 1

        img_PSF_dum = double(img_PSF_struct.data) - PSF_BGD;

    %- Matrix (can also be zero)     
    elseif size(PSF_BGD,1) == size(img_PSF_struct.data,1)  && size(PSF_BGD,2) == size(img_PSF_struct.data,2)  && size(PSF_BGD,3) == size(img_PSF_struct.data,3)  

        img_PSF_dum = img_PSF_struct.data;

        for i = 1:size(img_PSF_struct.data,3)
           bgd_slice = PSF_BGD(:,:,i);
           bgd_mean = mean( bgd_slice(:));

           img_PSF_dum(:,:,i) = img_PSF_dum(:,:,i) - bgd_mean;
        end
        
    else

        warndlg('Background of PSF in invalid format')
        img_PSF_dum= double(img_PSF_rec.data);    
    end
    
    %- Assign and set negative values to zero
    img_PSF_struct.data_w_bgd   = img_PSF_struct.data;
    img_PSF_struct.data         = img_PSF_dum .* (img_PSF_dum>0);    
        
   
    %== Fit with Gaussian
    parameters_fit.pixel_size      = pixel_size;
    parameters_fit.par_crop        = par_crop;
    parameters_fit.par_microscope  = par_microscope;
    parameters_fit.flags           = flags;
    
    [PSF_fit img_PSF_struct]       = PSF_3D_Gauss_fit_v4(img_PSF_struct,parameters_fit);
    
    img_PSF_struct.PSF_fit.sigma_xy = PSF_fit.sigma_xy ;
    img_PSF_struct.PSF_fit.sigma_z  = PSF_fit.sigma_z;
    img_PSF_struct.PSF_fit.amp      = PSF_fit.amp;
    img_PSF_struct.PSF_fit.bgd      = PSF_fit.bgd ; 
    
        
    %=== Integrated intensity of spot
    par_mod_int(1)  = PSF_fit.sigma_xy;
    par_mod_int(2)  = PSF_fit.sigma_xy;
    par_mod_int(3)  = PSF_fit.sigma_z;

    par_mod_int(4)  = PSF_fit.mu_x;
    par_mod_int(5)  = PSF_fit.mu_y;
    par_mod_int(6)  = PSF_fit.mu_z;

    par_mod_int(7)  = PSF_fit.amp ;
    par_mod_int(8)  = 0 ;


    x_int.min = PSF_fit.mu_x - 10*PSF_fit.sigma_xy;
    x_int.max = PSF_fit.mu_x + 10*PSF_fit.sigma_xy;

    y_int.min = PSF_fit.mu_y - 10*PSF_fit.sigma_xy;
    y_int.max = PSF_fit.mu_y + 10*PSF_fit.sigma_xy;

    z_int.min = PSF_fit.mu_z - 10*PSF_fit.sigma_z;
    z_int.max = PSF_fit.mu_z + 10*PSF_fit.sigma_z;

    img_PSF_struct.PSF_fit.Integrated_int = fun_Gaussian_3D_triple_integral_v1(x_int,y_int,z_int,par_mod_int);
    
 
else
    img_PSF_struct = [];
    PSF_fit        = [];
end



if flags.output
    
    %- Data for plot
    img_PSF_xy = max(img_PSF_struct.data,[],3);
    img_PSF_xz = squeeze(max(img_PSF_struct.data,[],1));
    img_PSF_yz = squeeze(max(img_PSF_struct.data,[],2)); 
    
    [dim.Y dim.X dim.Z] = size(img_PSF_struct.data);

    %- PSF
    figure
    subplot(1,3,1)
    imshow(img_PSF_xy,[ ],'XData',[0 dim.X]*pixel_size.xy,'YData',[0 dim.Y]*pixel_size.xy)
    title('PSF - XY')

    subplot(1,3,2)
    imshow(img_PSF_xz',[ ],'XData',[0 dim.X]*pixel_size.xy,'YData',[0 dim.Z]*pixel_size.z)
    title('PSF - XZ')

    subplot(1,3,3)
    imshow(img_PSF_yz',[ ],'XData',[0 dim.Y]*pixel_size.xy,'YData',[0 dim.Z]*pixel_size.z)
    title('PSF - YZ')
    
    colormap hot
end

