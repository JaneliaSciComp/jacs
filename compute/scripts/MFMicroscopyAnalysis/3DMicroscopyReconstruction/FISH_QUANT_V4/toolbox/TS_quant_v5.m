function [TxSite_quant TxSite_SIZE REC_prop TS_analysis_results] = TS_quant_v5(image_struct,pos_TS,PSF_shift,parameters_quant)
% Function to quantify the number of nascent transcripts at the transcription site

                
%=== Analysis of site
[TS_analysis_results PSF_shift] = TxSite_reconstruct_ANALYSIS_v4(image_struct,pos_TS,PSF_shift,parameters_quant);

%=== Quantification of the site
[TxSite_quant REC_prop] = TxSite_reconstruct_w_image_v5(TS_analysis_results,PSF_shift,parameters_quant);    

%=== Quantification of the site
TxSite_SIZE = TxSite_size_v1(REC_prop,TS_analysis_results,parameters_quant);
