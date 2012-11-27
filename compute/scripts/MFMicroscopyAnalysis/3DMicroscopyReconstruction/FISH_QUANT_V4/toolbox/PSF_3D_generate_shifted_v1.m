function [PSF_shift_all N_PSF_shift] = PSF_3D_generate_shifted_v1(img_PSF,parameters)

%% Parameters

%== For shifting
fact_os        = parameters.fact_os;
pixel_size_os  = parameters.pixel_size_os;
pixel_size     = parameters.pixel_size;
range_shift_xy = parameters.range_shift_xy;
range_shift_z  = parameters.range_shift_z;

%== For fitting
par_crop       = parameters.par_crop;
par_microscope = parameters.par_microscope;
flags          = parameters.flags;


%% Generate different PSF' with shifting them in all possible direction

            
%- Loop over all differnt shifts
ind_PSF = 1;
for i_x = 1:length(range_shift_xy)
    
    disp(['Processing x-shift : ', num2str(i_x),'/', num2str(length(range_shift_xy))])
    
    for i_y = 1:length(range_shift_xy)        
        for i_z = 1:length(range_shift_z)  

                
            % === CREATE SHIFTED IMAGE
            
            %- Shift over-sampled image
            par_shift                 = {range_shift_xy(i_x),range_shift_xy(i_y),range_shift_z(i_z)};
            [psf_os_shift dum dim_os] = PSF_3D_os_shift_v1(img_PSF.data,fact_os,pixel_size_os,par_shift);    


            %- Dimension and subregion of image in normal sampling
            dim_rec.Y = floor(dim_os.X/fact_os.xy); 
            dim_rec.X = floor(dim_os.X/fact_os.xy); 
            dim_rec.Z = floor(dim_os.Z/fact_os.z); 

            range_rec.X_nm = (1:dim_rec.X)*pixel_size.xy;
            range_rec.Y_nm = (1:dim_rec.Y)*pixel_size.xy;
            range_rec.Z_nm = (1:dim_rec.Z)*pixel_size.z;

            %- Calculate image in normal sampling
            PSF_shift.data  = PSF_3D_reconstruct_from_os_v1(psf_os_shift,range_rec,fact_os,0);    
            
            
            if flags.norm == 1
                if ind_PSF == 1
                    I_norm = sum(PSF_shift.data(:));
                else
                    I_data = sum(PSF_shift.data(:));
                    PSF_shift.data = PSF_shift.data * I_norm / I_data;
                end
            end
            
            %== Fit with Gaussian
            parameters_fit.pixel_size      = pixel_size;
            parameters_fit.par_crop        = par_crop;
            parameters_fit.par_microscope  = par_microscope;
            parameters_fit.flags           = flags;
    
            [PSF_fit PSF_shift]            = PSF_3D_Gauss_fit_v4(PSF_shift,parameters_fit);    
           
            
            %=== Save everything
            PSF_shift_all(ind_PSF).data        = PSF_shift.data;
            PSF_shift_all(ind_PSF).max         = PSF_shift.max;
            PSF_shift_all(ind_PSF).crop        = PSF_shift.crop;
            
            PSF_shift_all(ind_PSF).PSF_fit.sigma_xy        = PSF_fit.sigma_xy ;
            PSF_shift_all(ind_PSF).PSF_fit.sigma_z         = PSF_fit.sigma_z;
            PSF_shift_all(ind_PSF).PSF_fit.amp             = PSF_fit.amp;
            PSF_shift_all(ind_PSF).PSF_fit.bgd             = PSF_fit.bgd;
           
            
            PSF_shift_all(ind_PSF).par_shift   = par_shift;
            PSF_shift_all(ind_PSF).index_shift = [i_x, i_y,i_z];            
            
            PSF_shift_all(ind_PSF).PSF_fit_OS.sigma_xy = img_PSF.PSF_fit.sigma_xy ;
            PSF_shift_all(ind_PSF).PSF_fit_OS.sigma_z  = img_PSF.PSF_fit.sigma_z;
            PSF_shift_all(ind_PSF).PSF_fit_OS.amp      = img_PSF.PSF_fit.amp;
            PSF_shift_all(ind_PSF).PSF_fit_OS.bgd      = img_PSF.PSF_fit.bgd;  
            
            

            %=== Update counter
            ind_PSF = ind_PSF+1;  
            
        end
    end
end
N_PSF_shift         = ind_PSF -1;