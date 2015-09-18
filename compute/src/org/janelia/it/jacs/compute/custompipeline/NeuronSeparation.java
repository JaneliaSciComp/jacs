package org.janelia.it.jacs.compute.custompipeline;

import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.filter.*;
import java.util.*;

/**
 * Created by schauderd on 9/14/15.
 */
public class NeuronSeparation {

    public NeuronSeparation() {
        int r_max_ = 10;
        int r_min_ = 10;
        int r_step_ = 2;
        int quality_ = 4;
        double const_ = 20.0;
        int filtertype = 0;
        boolean skeletonize = false;
        int noisenum = 0;
        int measuresize = 5;

        String[] cgap = {"None", "1px", "2px"};
        String noisemethod;
        String ftype_;
        String cgapS;
        boolean branchdeletion = false;
        int closegap = 0;
        int noiseC = 1;
    }

    // import stitched pbd file into memory channels


     /*   public int setup(String arg, ImagePlus imp) {
            this.imp_ = imp;
            return DOES_8G + DOES_16 + DOES_32;
        }

        private boolean showDialog() {
            String[] types = {"GAUSSIAN", "MEAN"};

            GenericDialog gd = new GenericDialog("DSLT parameter");
            gd.addNumericField("Radius_r_max",  r_max_, 0);
            gd.addNumericField("Radius_r_min",  r_min_, 0);
            gd.addNumericField("Radius_r_step gap", r_step_, 0);
            gd.addNumericField("Rotation angle",  quality_, 0);
            gd.addNumericField("Weight (high = less sensitive)", const_, 1);
            gd.addChoice("Filter", types, types[filtertype]);
            gd.addCheckbox("Skeletonize", skeletonize);
            gd.addRadioButtonGroup("Close gap", cgap, 3, 3, cgap[closegap]);
            gd.addSlider("less than this value is the noise", 1, 10, measuresize);
            gd.addCheckbox("Delete Branch", branchdeletion);

            gd.showDialog();

            if (gd.wasCanceled()) return false;
            r_max_ = (int)gd.getNextNumber();
            r_min_ = (int)gd.getNextNumber();
            r_step_ = (int)gd.getNextNumber();
            quality_ = (int)gd.getNextNumber();
            const_ = gd.getNextNumber();
            ftype_ = types[gd.getNextChoiceIndex()];
            skeletonize = gd.getNextBoolean();
            cgapS = (String)gd.getNextRadioButton();
            measuresize = (int)gd.getNextNumber();

            branchdeletion = gd.getNextBoolean();

            Prefs.set("r_max.int", r_max_);
            Prefs.set("r_min.int", r_min_);
            Prefs.set("r_step.int", r_step_);
            Prefs.set("quality.int", quality_);
            Prefs.set("const.double", const_);
            Prefs.set("skel.boolean", skeletonize);
            Prefs.set("branch.boolean", branchdeletion);
            Prefs.set("measuresize.int", measuresize);

            if(ftype_ == "GAUSSIAN")  filtertype = 0;
            else if(ftype_ == "MEAN") filtertype = 1;
            Prefs.set("ftype.int", filtertype);


            if(cgapS == "None")  closegap = 0;
            else if(cgapS == "1px") closegap = 1;
            else if(cgapS == "2px") closegap = 2;
            Prefs.set("closegap.int", closegap);

            if(noisemethod=="1px") noisenum=0;
            else if(noisemethod=="5px") noisenum=1;
            Prefs.set("Noise canceling method2.int", noisenum);

            return true;
        }

        public void run(ImageProcessor ip) {
            if (!showDialog()) return;

            if(quality_ < 1) return;
            if(r_max_ < 1) return;
            if(r_min_ > r_max_) return;
            if(r_step_ < 1) return;

            ImageStack stack = imp_.getStack();

            int[] dims = imp_.getDimensions();
            int imageW = dims[0];
            int imageH = dims[1];
            int nCh    = dims[2];
            int imageD = dims[3];
            int nFrame = dims[4];
            int bdepth = imp_.getBitDepth();


            int total_img = imageD * nCh * nFrame;
            int cur_img = 0;
            IJ.showProgress(0.0);

            ImagePlus newimp = IJ.createHyperStack("r_max; "+String.valueOf(r_max_)+"    r_min; "+String.valueOf(r_min_)+"    r_step; "+String.valueOf(r_step_)+"    quality; "+String.valueOf(quality_)+"    C; "+String.valueOf(const_)+"    Close gap; "+cgapS+"    Noise deletion; "+noisemethod+"    branchdeletion; "+String.valueOf(branchdeletion), imageW, imageH, nCh, imageD, nFrame, 8);
            ImageStack newst = newimp.getStack();


            for(int f = 0; f < nFrame; f++){
                for(int ch = 0; ch < nCh; ch++){
                    for(int s = 0; s < imageD; s++){
                        ImageProcessor src_ip = stack.getProcessor(imp_.getStackIndex(ch+1, s+1, f+1));
                        ByteProcessor dst_ip  = (ByteProcessor)newst.getProcessor(newimp.getStackIndex(ch+1, s+1, f+1));
                        for(int r = r_max_; r >= r_min_ && r > 0; r -= r_step_){
                            dslt(src_ip, dst_ip, r, quality_, const_/500.0*255.0, filtertype);
                            //	IJ.log("r: " + r);
                            if(r_min_ > 0 && r != r_min_ && r-r_step_ < r_min_){
                                dslt(src_ip, dst_ip, r_min_, quality_, const_/500.0*255.0, filtertype);
                                //		IJ.log("r: " + r_min_);
                            }
                        }
                        //	if(skeletonize)IJ.run(new ImagePlus("", dst_ip), "Skeletonize", "");
                        cur_img++;

                        noise(src_ip, dst_ip, noisenum); //function

                        IJ.showProgress((double)cur_img/(double)total_img);

                        //			IJ.log("n: " + imp_.getStackIndex(ch+1, s+1, f+1));
                    }
                }
            } //for(int f = 0; f < nFrame; f++){

            newimp.show();

        } //public void run(ImageProcessor ip) {

        public void dslt(ImageProcessor in, ByteProcessor out, int r, int rd, double c, int filter_type) {
            if(r < 1) return;
            if(rd < 1)return;

            double[] filter;

            if(filter_type == 1)filter = mean1D(r);
            else filter = gaussian1D(r);

            if(filter == null) return;

            int width = in.getWidth();
            int height = in.getHeight();
            for(int x = 0; x < width; x++){
                for(int y = 0; y < height; y++){
                    for(int d = 0; d < 2*rd-1; d++){
                        double deg = (double)d * (90.0 / (double)rd);
                        double ex = Math.cos(Math.toRadians(deg));
                        double ey = Math.sin(Math.toRadians(deg));
                        double src = in.getInterpolatedValue(x, y);
                        double sum = src * filter[r];
                        for(int i = 1; i <= r; i++){
                            double x1 = (double)x+ex*(double)i;
                            if(x1 > width)  x1 = width;
                            else if(x1 < 0) x1 = 0;
                            double y1 = (double)y+ey*(double)i;
                            if(y1 > height) y1 = height;
                            else if(y1 < 0) y1 = 0;

                            double x2 = (double)x-ex*(double)i;
                            if(x2 > width)  x2 = width;
                            else if(x2 < 0) x2 = 0;
                            double y2 = (double)y-ey*(double)i;
                            if(y2 > height) y2 = height;
                            else if(y2 < 0) y2 = 0;

                            sum += in.getInterpolatedValue(x1, y1) * filter[r+i];
                            sum += in.getInterpolatedValue(x2, y2) * filter[r-i];
                        }
                        if(src > sum+c) out.set(x, y, 255);
                    }
                }
            } // for (x=0)
        } //public void dslt(ImageProcessor in, ByteProcessor out, int r, int rd, double c, int filter_type) {

        public void noise(ImageProcessor in, ByteProcessor out, int noisenum) {
            int WWnoise=0;
            int HHnoise=0;

            int width = in.getWidth();
            int height = in.getHeight();

            if (noisenum==1){
                WWnoise=width-5;
                HHnoise=height-5;
            }
            if (noisenum==0){
                WWnoise=width-3;
                HHnoise=height-3;
            }

            if(branchdeletion){
                int branch=1;
                while(branch==1){
                    branch=0;
                    for (int nncon2=0; nncon2 < WWnoise; nncon2++){ //endbranch delection
                        for (int vvvcon2=0; vvvcon2 < HHnoise; vvvcon2++){
                            int pixcon1 = out.get (nncon2, vvvcon2);
                            int pixcon2 = out.get (nncon2, vvvcon2+1);
                            int pixcon3 = out.get (nncon2, vvvcon2+2);
                            int pixcon4 = out.get (nncon2+1, vvvcon2);
                            int pixcon5 = out.get (nncon2+1, vvvcon2+1); //center
                            int pixcon6 = out.get (nncon2+1, vvvcon2+2);
                            int pixcon7 = out.get (nncon2+2, vvvcon2);
                            int pixcon8 = out.get (nncon2+2, vvvcon2+1);
                            int pixcon9 = out.get (nncon2+2, vvvcon2+2);

                            if(pixcon5==255){
                                int sum2=(pixcon1+pixcon2+pixcon3+pixcon4+pixcon5+pixcon6+pixcon7+pixcon8+pixcon9);
                                if(sum2==510 || sum2==255){
                                    out.set (nncon2, vvvcon2, 0);
                                    out.set (nncon2, vvvcon2+1, 0);
                                    out.set (nncon2, vvvcon2+2, 0);
                                    out.set (nncon2+1, vvvcon2, 0);
                                    out.set (nncon2+1, vvvcon2+1, 0); //center
                                    out.set (nncon2+1, vvvcon2+2, 0);
                                    out.set (nncon2+2, vvvcon2, 0);
                                    out.set (nncon2+2, vvvcon2+1, 0);
                                    out.set (nncon2+2, vvvcon2+2, 0);
                                    branch=1;
                                }
                            }
                        }
                    }
                }
            }//branchdeletion

            int sumpx = out.getPixelCount();
            int pix=0;

            if(noiseC==1){
                for (int noisetime=1; noisetime <= 2; noisetime++){ //noise deletion 1px - 9px
                    for(int xx=0; xx<sumpx-(4*width); xx+=width){//horizontal line to vertical grow
                        for(int i=xx; i<xx+width-4; i++){

                            int pixsum=0;
                            int background1=0;
                            int signal1=0;

                            for(int ii=i; ii<=i+4; ii++){//within 5x5 px window horizontal
                                for(int iv=ii; iv<=ii+(4*width); iv+=width){//within 5x5 px window vertical
                                    pix = out.get (iv);
                                    pixsum=pixsum+1;

                                    if(pix>=200){
                                        if(pixsum<7 || pixsum==10 || pixsum==11 || pixsum==15 || pixsum==16 || pixsum>19)
                                            background1=background1+1;
                                        else
                                            signal1=signal1+1;
                                    }
                                }////vertical
                            }//horizontal

                            if (background1==0){
                                if (signal1>0){
                                    if (signal1<=measuresize){
                                        for(int iii=i+1; iii<=i+3; iii++){//horizontal
                                            for(int ivv=iii+width; ivv<=iii+(3*width); ivv+=width){//vertical
                                                out.set (ivv, 0);
                                            }
                                        }
                                    }
                                }
                            }else{
                                pixsum=0;
                                background1=0;
                                signal1=0;
                                for(int ii=i; ii<=i+2; ii++){//within 5x5 px window horizontal
                                    for(int iv=ii; iv<=ii+(2*width); iv+=width){//within 5x5 px window vertical
                                        pix = out.get (iv);
                                        pixsum=pixsum+1;

                                        if(pix>=200){
                                            if(pixsum<4 || pixsum>6)
                                                background1=background1+1;
                                            else
                                                signal1=signal1+1;
                                        }
                                    }////vertical
                                }//horizontal

                                if (background1==0){
                                    if (signal1>0){
                                        if (signal1<=measuresize){
                                            //	for(int iii=i+1; iii<=i+3; iii++){//horizontal
                                            //		for(int ivv=iii+width; ivv<=iii+(3*width); ivv+=width){//vertical
                                            int ivv=i+1+width;
                                            out.set (ivv, 0);
                                            //		}
                                            //	}
                                        }
                                    }
                                }//if (background1==0){
                            }
                        }//for(int i=xx; i<xx+width-4; i++){
                    }//for(int i=0; i<sumpx-(4*width); i++){
                } //1st and 2nd noise, for (int noisetime=1; noisetime <= 2; noisetime++){ //noise deletion 1px and 5px
            }//noise==1
            if(closegap==1 || closegap==2){ //close gap
                int exclosegap=1;
                while(exclosegap==1){
                    exclosegap=0;
                    for (int nncon=0; nncon < WWnoise; nncon++){ //1px, 2px connection
                        for (int vvvcon=0; vvvcon < HHnoise; vvvcon++){

                            int pixcon1 = out.get (nncon, vvvcon);
                            int pixcon2 = out.get (nncon, vvvcon+1);
                            int pixcon3 = out.get (nncon, vvvcon+2);
                            int pixcon34 = out.get (nncon, vvvcon+3);

                            int pixcon4 = out.get (nncon+1, vvvcon);
                            int pixcon5 = out.get (nncon+2, vvvcon);
                            int pixcon6 = out.get (nncon+3, vvvcon);

                            int pixcon11 = out.get (nncon+1, vvvcon+1);
                            int pixcon12 = out.get (nncon+2, vvvcon+2);
                            int pixcon13 = out.get (nncon+3, vvvcon+3);

                            int pixcon21 = out.get (nncon+2, vvvcon+1);
                            int pixcon22 = out.get (nncon+1, vvvcon+2);

                            if(pixcon2==0){ //vertical
                                if(pixcon1==255){
                                    if(pixcon3==255){
                                        out.set(nncon, vvvcon+1, 255);
                                        exclosegap=1;
                                    }
                                }

                                if(closegap==2){
                                    if(pixcon3==0){
                                        if((pixcon1+pixcon34)==510){
                                            out.set(nncon, vvvcon+1, 255);
                                            out.set(nncon, vvvcon+2, 255);
                                            exclosegap=1;
                                        }
                                    } //2px
                                } //if(closegap==2)
                            }

                            if(pixcon4==0){ // horizontal
                                if(pixcon1==255){
                                    if(pixcon5==255){
                                        out.set(nncon+1, vvvcon, 255);
                                        exclosegap=1;
                                    }
                                }
                                if(closegap==2){
                                    if(pixcon5==0){
                                        if((pixcon1+pixcon6)==510){
                                            out.set(nncon+1, vvvcon, 255);
                                            out.set(nncon+2, vvvcon, 255);
                                            exclosegap=1;
                                        }
                                    }
                                } //if(closegap==2)
                            } //	if(pixcon4==0){

                            if(pixcon11==0){ // //right bottom lateral
                                if(pixcon1==255){
                                    if(pixcon12==255){
                                        out.set(nncon+1, vvvcon+1, 255);
                                        exclosegap=1;
                                    }
                                }
                                if(closegap==2){
                                    if(pixcon12==0){
                                        if((pixcon1+pixcon13)==510){
                                            out.set(nncon+1, vvvcon+1, 255);
                                            out.set(nncon+2, vvvcon+2, 255);
                                            exclosegap=1;
                                        }
                                    }
                                } //if(closegap==2)
                            } //	if(pixcon11==0){

                            if(pixcon21==0){ // //left bottom lateral
                                if(pixcon6==255){
                                    if(pixcon22==255){
                                        out.set(nncon+2, vvvcon+1, 255);
                                        exclosegap=1;
                                    }
                                }
                                if(closegap==2){
                                    if(pixcon6==0){
                                        if((pixcon6+pixcon34)==510){
                                            out.set(nncon+2, vvvcon+1, 255);
                                            out.set(nncon+1, vvvcon+2, 255);
                                            exclosegap=1;
                                        }
                                    }
                                } //if(closegap==2)
                            } //	if(pixcon21==0){

                        }
                    } //for (int nn=0; nn < WWnoise; nn++){ //1px connection
                }	//while(exclosegap==1){
            } //closegap

            if(skeletonize){
                IJ.run(new ImagePlus("", out), "Skeletonize", "");

                if(branchdeletion){
                    for(int deletime=1; deletime <=3; deletime++){

                        for (int nncon3=0; nncon3 < WWnoise; nncon3++){ //3-分岐処理
                            for (int vvvcon2=0; vvvcon2 < HHnoise; vvvcon2++){
                                int pixcon1 = out.get (nncon3, vvvcon2);
                                int pixcon2 = out.get (nncon3, vvvcon2+1);
                                int pixcon3 = out.get (nncon3, vvvcon2+2);
                                int pixcon4 = out.get (nncon3+1, vvvcon2);
                                int pixcon5 = out.get (nncon3+1, vvvcon2+1); //center
                                int pixcon6 = out.get (nncon3+1, vvvcon2+2);
                                int pixcon7 = out.get (nncon3+2, vvvcon2);
                                int pixcon8 = out.get (nncon3+2, vvvcon2+1);
                                int pixcon9 = out.get (nncon3+2, vvvcon2+2);

                                if(pixcon5==255){
                                    if(deletime==1){
                                        int sum2=(pixcon1+pixcon2+pixcon3+pixcon4+pixcon5+pixcon6+pixcon7+pixcon8+pixcon9);
                                        if(sum2==765 || sum2==1020){
                                            out.set (nncon3+1, vvvcon2+1, 250);
                                        }
                                    } //if(deletime==1){

                                    if(deletime>1){
                                        int sum2=(pixcon1+pixcon2+pixcon3+pixcon4+pixcon5+pixcon6+pixcon7+pixcon8+pixcon9);
                                        if(sum2==1020){
                                            out.set (nncon3+1, vvvcon2+1, 250);
                                        }
                                    } //if(deletime==2){
                                } //if(pixcon5==255){
                            }
                        } //for (int nncon3=0; nncon3 < WWnoise; nncon3++){ //3-分岐処理

                        int branch=1;
                        while(branch==1){
                            branch=0;
                            for (int nncon3=0; nncon3 < WWnoise; nncon3++){ //endbranch delection
                                for (int vvvcon2=0; vvvcon2 < HHnoise; vvvcon2++){
                                    int pixcon1 = out.get (nncon3, vvvcon2);
                                    int pixcon2 = out.get (nncon3, vvvcon2+1);
                                    int pixcon3 = out.get (nncon3, vvvcon2+2);
                                    int pixcon4 = out.get (nncon3+1, vvvcon2);
                                    int pixcon5 = out.get (nncon3+1, vvvcon2+1); //center
                                    int pixcon6 = out.get (nncon3+1, vvvcon2+2);
                                    int pixcon7 = out.get (nncon3+2, vvvcon2);
                                    int pixcon8 = out.get (nncon3+2, vvvcon2+1);
                                    int pixcon9 = out.get (nncon3+2, vvvcon2+2);

                                    if(pixcon5==255){
                                        int sum2=(pixcon1+pixcon2+pixcon3+pixcon4+pixcon5+pixcon6+pixcon7+pixcon8+pixcon9);
                                        if(sum2==510 || sum2==255){
                                            out.set (nncon3, vvvcon2, 0);
                                            out.set (nncon3, vvvcon2+1, 0);
                                            out.set (nncon3, vvvcon2+2, 0);
                                            out.set (nncon3+1, vvvcon2, 0);
                                            out.set (nncon3+1, vvvcon2+1, 0); //center
                                            out.set (nncon3+1, vvvcon2+2, 0);
                                            out.set (nncon3+2, vvvcon2, 0);
                                            out.set (nncon3+2, vvvcon2+1, 0);
                                            out.set (nncon3+2, vvvcon2+2, 0);
                                            branch=1;
                                        }
                                    } //if(pixcon5==255){
                                }
                            }
                        } //while(branch==1){

                        int threpix;
                        int allpx = out.getPixelCount();

                        for (int threpx=0; threpx < allpx; threpx++){ //endbranch delection

                            threpix = out.get (threpx);

                            if (threpix ==250)
                                out.set (threpx, 255);
                        }

                    } //for(int deletime=1; deletime <=2; deletime++){
                }//branchdeletion
            } //if(skeletonize){

        } //public void noise(ImageProcessor in, ByteProcessor out, int noisenum) {


        public double[] gaussian1D(int r){
            if(r < 1)return null;
            int ksize = 2*r + 1;
            double[] filter = new double[ksize];
            double sigma = 0.3*(ksize/2 - 1) + 0.8;
            double denominator = 2.0*sigma*sigma;
            double sum;
            double xx, d;
            int x;

            sum = 0.0;
            for(x = 0; x < ksize; x++){
                xx = x - (ksize - 1)/2;
                d = xx*xx;
                filter[x] = Math.exp(-1.0*d/denominator);
                sum += filter[x];
            }

            for(x = 0; x < ksize; x++)filter[x] /= sum;

            return filter;
        }

        public double[] mean1D(int r){
            if(r < 1)return null;
            int ksize = 2*r + 1;
            double[] filter = new double[ksize];

            for(int x = 0; x < ksize; x++)filter[x] = 1.0/(double)ksize;

            return filter;
        }
    }*/


    public static void main (String[] args) {

    }
}
