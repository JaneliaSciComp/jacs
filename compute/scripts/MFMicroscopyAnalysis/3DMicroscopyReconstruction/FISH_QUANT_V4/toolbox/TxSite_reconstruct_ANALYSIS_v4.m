function [results img_PSF_all] = TxSite_reconstruct_ANALYSIS_v4(image_struct,pos_TS,img_PSF_all,parameters)


% v1 Aug 11, 2011
% - Initial implementation

flags               = parameters.flags;
pixel_size          = parameters.pixel_size;
per_avg_bgd         = parameters.per_avg_bgd;
par_microscope      = parameters.par_microscope;

%= Extract image of transcription site
min_x = round(min(pos_TS.x));
max_x = round(max(pos_TS.x));
min_y = round(min(pos_TS.y));
max_y = round(max(pos_TS.y));

img_TS = image_struct.data(min_y:max_y,min_x:max_x,:); 
    
    
%% Restrict TS in xy

%= Manual cropping
if flags.crop == 0
    
    %- Show maximimum projection of image
    img_TS_proj_xy = max(img_TS,[],3);
    figure(200)
    imshow(img_TS_proj_xy,[]);

    %- Draw rectangular region around transcription site - double click on  region to confirm shape   
    h_trans = imrect(gca,[]);
    api     = iptgetapi(h_trans);
    wait(h_trans);      
    PositionTranscription = api.getPosition(); % Position of transcription site: [x,y,w,h]

    close (200)       

    ymin = uint32(PositionTranscription(:,2));
    ymax = uint32((PositionTranscription(:,2) + PositionTranscription(:,4)));

    xmin = uint32(PositionTranscription(:,1));
    xmax = uint32((PositionTranscription(:,1) + PositionTranscription(:,3))); 

    img_TS_crop_xy = img_TS(ymin:ymax,xmin:xmax,:);

    %==== Restrict TS in z

    %- Show maximimum projection of image
    img_TS_proj_xz = squeeze(max(img_TS_crop_xy,[],1))';
    figure(200)
    imshow(img_TS_proj_xz,[]);

    %- Draw rectangular region around transcription site - double click on  region to confirm shape   
    h_trans = imrect(gca,[]);
    api     = iptgetapi(h_trans);
    wait(h_trans);      
    PositionTranscription = api.getPosition(); % Position of transcription site: [x,y,w,h]

    %- Crop in Z
    zmin = uint32(PositionTranscription(:,2));
    zmax = uint32((PositionTranscription(:,2) + PositionTranscription(:,4))); 

    img_TS_crop_xyz = img_TS_crop_xy(:,:,zmin:zmax);
    
    close (200) 

%- Cropping with specified region     
elseif flags.crop == 1 
        
    crop_xy_nm = parameters.crop_image.xy_nm;
    crop_z_nm  = parameters.crop_image.z_nm;

    crop_xy_pix = ceil(crop_xy_nm / pixel_size.xy);
    crop_z_pix  = ceil(crop_z_nm / pixel_size.z);

    if not(isfield(pos_TS,'auto'))
        pos_TS.auto = 0;
    end
    
    if isempty(pos_TS.auto)
        pos_TS.auto = 0;
    end
    
    
    if pos_TS.auto == 0 
        
        %== Find center of PSF 
        [img_TS_max.val img_TS_max.ind_lin]      = max(img_TS(:));
        [img_TS_max.Y,img_TS_max.X,img_TS_max.Z] = ind2sub(size(img_TS),img_TS_max.ind_lin(1));

        [dim.Y dim.X dim.Z] = size(img_TS);
      
        xmin = round(img_TS_max.X - crop_xy_pix);
        xmax = round(img_TS_max.X + crop_xy_pix);
        
        ymin = round(img_TS_max.Y - crop_xy_pix);
        ymax = round(img_TS_max.Y + crop_xy_pix);
        
        zmin = round(img_TS_max.Z - crop_z_pix);
        zmax = round(img_TS_max.Z + crop_z_pix);
   
        if ymin<1;     ymin = 1;     end
        if ymax>dim.Y; ymax = dim.Y; end
        
        if xmin<1;     xmin = 1;     end
        if xmax>dim.X; xmax = dim.X; end
                
        if zmin<1;     zmin = 1;     end
        if zmax>dim.Z; zmax = dim.Z; end
        
        img_TS_crop_xyz = img_TS(ymin:ymax,xmin:xmax,zmin:zmax);
        
    else
        coord = pos_TS.coord;
        
        coord.X_center = mean(coord.X);
        coord.Y_center = mean(coord.Y);
        coord.Z_center = mean(coord.Z);
    
        [dim.Y dim.X dim.Z] = size(image_struct.data);
           
        xmin = round(coord.X_center - crop_xy_pix);
        x_max = round(coord.X_center + crop_xy_pix);

        ymin = round(coord.Y_center - crop_xy_pix);
        y_max = round(coord.Y_center + crop_xy_pix);    

        zmin = round(coord.Z_center - crop_z_pix);
        zmax = round(coord.Z_center + crop_z_pix);
        
        if ymin<1;     ymin = 1;     end
        if ymax>dim.Y; ymax = dim.Y; end
        
        if xmin<1;     xmin = 1;     end
        if xmax>dim.X; xmax = dim.X; end
                
        if zmin<1;     zmin = 1;     end
        if zmax>dim.Z; zmax = dim.Z; end
        
        img_TS_crop_xyz = image_struct.data(ymin:ymax,xmin:xmax,zmin:zmax);
    end
    
    
%- Cropping with padding - used for simualated sites
elseif flags.crop == 2    
    
    
    pad_image = parameters.pad_image;

        
    [dim_TS.Y dim_TS.X dim_TS.Z] = size(img_TS);       

    center.Y = round(dim_TS.Y/2);
    center.X = round(dim_TS.X/2);
    center.Z = round(dim_TS.Z/2);

    ymin = center.Y - 2*pad_image;
    ymax = center.Y + 2*pad_image;

    xmin = center.X - 2*pad_image;
    xmax = center.X + 2*pad_image;

    zmin = center.Z - 2*pad_image;
    zmax = center.Z + 2*pad_image;

    img_TS_crop_xyz = img_TS(ymin:ymax,xmin:xmax,zmin:zmax);
end        
        

%% Calculate assignment table for linear indices to matrix coordinates
%  Used for intensity based random placement of PSF

%==  Vectors with dimensions
[dim_TS_crop.Y,dim_TS_crop.X,dim_TS_crop.Z] = size(img_TS_crop_xyz);
X_nm_crop = []; Y_nm_crop = []; Z_nm_crop = []; 
X_nm_crop(:,1) = (1:dim_TS_crop.X)*pixel_size.xy;
Y_nm_crop(:,1) = (1:dim_TS_crop.Y)*pixel_size.xy;
Z_nm_crop(:,1) = (1:dim_TS_crop.Z)*pixel_size.z;

coord.X_nm = X_nm_crop;
coord.Y_nm = Y_nm_crop;
coord.Z_nm = Z_nm_crop;

%== List of intensity values for intensity based selection of position
index_table = zeros(dim_TS_crop.Z*dim_TS_crop.X*dim_TS_crop.Y,3);
ind_loop    = 1;

%- Loop through in the same way than linear indexing and assign index
for iZ=1:dim_TS_crop.Z
    for iX=1:dim_TS_crop.X
        for iY =1:dim_TS_crop.Y        
            index_table(ind_loop,:) = [iY, iX, iZ];
            ind_loop = ind_loop+1;
        end
    end
end



%% Show results of crop
if flags.output == 2
    
    %== Visualize site in projections
    img_TS_crop_proj_xy = max(img_TS_crop_xyz,[],3);
    img_TS_crop_proj_xz = squeeze(max(img_TS_crop_xyz,[],1));
    img_TS_crop_proj_yz = squeeze(max(img_TS_crop_xyz,[],2));

    figure
    subplot(2,2,4)
    imshow(img_TS_crop_proj_xy,[],'XData',X_nm_crop,'YData',Y_nm_crop);
    title('TxSite MIP XY - cropped')
    
    subplot(2,2,2)
    imshow(img_TS_crop_proj_xz',[],'XData',X_nm_crop,'YData',Z_nm_crop);
    title('TxSite MIP XZ - cropped')
    
    subplot(2,2,3)
    imshow(img_TS_crop_proj_yz,[],'XData',Z_nm_crop,'YData',Y_nm_crop);
    title('TxSite MIP ZY - cropped')
    colormap hot
end

%% Estimate background - on not cropped image!

if flags.bgd_local
        
    nBins = parameters.nBins;

    %- Calculate histogram and determine threshold
    [counts, bin ] = hist(img_TS_crop_xyz(:),nBins);
    counts_max     = max(counts);
    int_row_sort   = sort(img_TS_crop_xyz(:),'ascend');
    N_vox          = length(int_row_sort);
    bgd_avg        = mean(int_row_sort(1:round(per_avg_bgd*N_vox)));

    %- Manual correct threshold
    if flags.bgd_local == 2
        
        %- Show results of histrogram and threshold
        figure, hold on
        bar(bin,counts)
        plot([bgd_avg bgd_avg], [0 counts_max],'r')
        hold off
        xlabel('Intensity value')
        ylabel('Count')
        title('Thresholding of background > red line')

        %- Check if threshold is ok
        choice = questdlg('Background ok?','Estimation of background','Yes','No','Yes');


        %- Ask user if threshold is ok
        while (strcmp(choice,'No'))

            %- Ask user for new threshold
            prompt    = {'Value of threshold:'};
            dlg_title = 'Threshold histogram';
            num_lines = 1;
            def       = {num2str(bgd_avg)};
            answer    = inputdlg(prompt,dlg_title,num_lines,def);
            bgd_avg      = str2double(answer{1});  

            %- Plot histogram with location of threshold
            figure, hold on
            bar(bin,counts)
            plot([bgd_avg bgd_avg], [0 counts_max],'r')
            hold off
            xlabel('Intensity value')
            ylabel('Count')
            title('Thresholding based on curvature threshold > red line')

            %- Check if threshold is ok
            choice = questdlg('Background ok?','Estimation of background','Yes','No','Yes');
        end
    end  
    
else
    bgd_avg = parameters.BGD.amp;    
end

%- Determine background image
%img_bgd     = bgd_avg*ones(size(img_TS_crop_xyz));


%% Add padding to different PSF's  to avoid too large shifts

N_PSF = size(img_PSF_all,1)*size(img_PSF_all,2);

for i_PSF = 1:N_PSF

    img_PSF = img_PSF_all(i_PSF);

    img_PSF_all(i_PSF).pad = padarray(img_PSF.data,[dim_TS_crop.Y dim_TS_crop.X dim_TS_crop.Z]);

    img_PSF_all(i_PSF).max.X_pad = img_PSF.max.X + dim_TS_crop.X;
    img_PSF_all(i_PSF).max.Y_pad = img_PSF.max.Y + dim_TS_crop.Y;
    img_PSF_all(i_PSF).max.Z_pad = img_PSF.max.Z + dim_TS_crop.Z;
end


%% Fit with Gaussian
parameters_fit.pixel_size     = pixel_size;
parameters_fit.par_microscope = par_microscope;
parameters_fit.par_microscope = par_microscope;
parameters_fit.flags.output   = 0;
parameters_fit.flags.crop     = 0;

test.data     = img_TS_crop_xyz;
TS_Fit_Result = PSF_3D_Gauss_fit_v4(test,parameters_fit);  


%=== Integrated intensity of spot
par_mod_int(1)  = TS_Fit_Result.sigma_xy;
par_mod_int(2)  = TS_Fit_Result.sigma_xy;
par_mod_int(3)  = TS_Fit_Result.sigma_z;

par_mod_int(4)  = 0;
par_mod_int(5)  = 0;
par_mod_int(6)  = 0;

par_mod_int(7)  = TS_Fit_Result.amp ;
par_mod_int(8)  = 0 ;


x_int.min = TS_Fit_Result.mu_x - 5*TS_Fit_Result.sigma_xy;
x_int.max = TS_Fit_Result.mu_x + 5*TS_Fit_Result.sigma_xy;

y_int.min = TS_Fit_Result.mu_y - 5*TS_Fit_Result.sigma_xy;
y_int.max = TS_Fit_Result.mu_y + 5*TS_Fit_Result.sigma_xy;

z_int.min = TS_Fit_Result.mu_z - 5*TS_Fit_Result.sigma_z;
z_int.max = TS_Fit_Result.mu_z + 5*TS_Fit_Result.sigma_z;

TS_Fit_Result.Integrated_int = fun_Gaussian_3D_triple_integral_v1(x_int,y_int,z_int,par_mod_int);
TS_Fit_Result.x_int = x_int;
TS_Fit_Result.y_int = y_int;
TS_Fit_Result.z_int = z_int;


%% Save results
results.img_TS_crop_xyz  = img_TS_crop_xyz;
results.coord            = coord;
results.index_table      = index_table;
%results.img_bgd          = img_bgd;
results.TS_Fit_Result    = TS_Fit_Result;
results.bgd_amp          = bgd_avg;

