package org.janelia.it.jacs.compute.service.tic;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexTestHarness {

    public static void main(String[] args){
//            Pattern pattern = Pattern.compile("_t[0-9]*.tif");
//            Matcher matcher = pattern.matcher("xxx_t0001.tif");
        Pattern pattern = Pattern.compile("_[0-9]*_[a-zA-Z]*.tif");
        Matcher matcher = pattern.matcher("001_a_red.nd2_99_RC.tif");
        System.out.println(matcher.find());
            matcher.reset();
            boolean found = false;
            while (matcher.find()) {
                System.out.printf(String.format("I found the text" +
                        " \"%s\" starting at " +
                        "index %d and ending at index %d.%n",
                        matcher.group(),
                        matcher.start(),
                        matcher.end()));
                found = true;
            }
            if(!found){
                System.out.println("No match found.");
            }
        }
}