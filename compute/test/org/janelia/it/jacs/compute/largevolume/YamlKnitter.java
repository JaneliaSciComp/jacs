/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.jacs.compute.largevolume;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Pulls together the two types of YML files.  It uses first as basis,
 * expecting that to be new style.  It adds to the new-style YML, the
 * transform tags from the old one.
 *
 * @author fosterl
 */
public class YamlKnitter {
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            throw new IllegalArgumentException("USAGE: java " + YamlKnitter.class.getName() + " <new-style LVV yaml> <old-style LVV yaml>");
        }
        File newYaml = new File(args[0]);
        File oldYaml = new File(args[1]);
        
        BufferedReader ofr = new BufferedReader(new FileReader(oldYaml));

        // Read all transform tags from the old file.
        String inline = null;
        List<String> allTransforms = new ArrayList<>();
        while (null != (inline = ofr.readLine())) {
            if (inline.contains("transform:")) {
                // Need this line and next 3.
                StringBuilder transline = new StringBuilder(inline);
                for (int i = 0; i < 3; i++) {
                    inline = ofr.readLine();
                    transline.append("\n");
                    transline.append(inline);
                }
                allTransforms.add(transline.toString());

            }
        }        
        ofr.close();
        
        // Write back the knit file from old and new.
        BufferedReader nfr = new BufferedReader(new FileReader(newYaml));
        PrintWriter pw = new PrintWriter(new FileWriter( new File(newYaml.getParent(), "tilebase.cache.yml_knit")));
        
        Iterator it = allTransforms.iterator();
        while (null != (inline = nfr.readLine())) {
            if (inline.contains("homography:")) {
                pw.println(it.next());
            }
            pw.println(inline);
        }
        
        nfr.close();    
        pw.close();
        
    }
}
