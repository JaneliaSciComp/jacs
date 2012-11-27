function TxSite_quant_save_results_v1(REC_prop,parameters)

file_name_save_REC = parameters.file_name_save_REC;
file_name_save_RES = parameters.file_name_save_RES;

img_TS  = REC_prop.img_TS;
img_REC = REC_prop.img_fit;
img_RES = REC_prop.img_res;


img_RES_pos = img_RES.*(img_RES>0);
img_RES_neg = img_RES.*(img_RES<0);


img_TS_REC = cat(2, img_TS, img_REC);
img_Res_pos_neg = cat(2, img_RES_pos, img_RES_neg);

image_save_v1(img_TS_REC,file_name_save_REC);
image_save_v1(img_Res_pos_neg,file_name_save_RES);