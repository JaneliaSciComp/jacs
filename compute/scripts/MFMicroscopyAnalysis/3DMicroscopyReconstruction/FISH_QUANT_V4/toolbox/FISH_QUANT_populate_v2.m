function handles = FISH_QUANT_populate_v2(handles)

%=== Theoretical PSF
[PSF_theo.xy_nm,PSF_theo.z_nm] = sigma_PSF_BoZhang_v1(handles.par_microscope);
PSF_theo.xy_pix                = PSF_theo.xy_nm / handles.par_microscope.pixel_size.xy ;
PSF_theo.z_pix                 = PSF_theo.z_nm  / handles.par_microscope.pixel_size.z ;
handles.PSF_theo               = PSF_theo;

set(handles.text_psf_theo_xy,'String',num2str(round(PSF_theo.xy_nm)));
set(handles.text_psf_theo_z, 'String',num2str(round(PSF_theo.z_nm)));

%== Filtering
set(handles.text_kernel_factor_bgd,'String',num2str(handles.filter.factor_bgd));
set(handles.text_kernel_factor_filter,'String',num2str(handles.filter.factor_psf));


%== Detection region
%set(handles.text_detect_region_xy,'String',num2str(handles.detect.region.xy));
%set(handles.text_detect_region_z, 'String',num2str(handles.detect.region.z));
%set(handles.text_detection_threshold, 'String',num2str(handles.detect.thresh_int));


%== Select thresholding method for score
%str   = get(handles.pop_up_detect_quality, 'String');
%index = find(strcmp(str, handles.detect.score));
%set(handles.pop_up_detect_quality, 'Value',index)

