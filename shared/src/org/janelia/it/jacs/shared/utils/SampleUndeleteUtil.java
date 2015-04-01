package org.janelia.it.jacs.shared.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.Scanner;

/**
 * Created with IntelliJ IDEA.
 * User: saffordt
 * Date: 2/11/15
 * Time: 12:20 PM
 */
public class SampleUndeleteUtil {
    public static void main(String[] args) {
        try {
            Scanner scanner = new Scanner(new File("/groups/jacs/jacsHosts/servers/jacs/jboss-4.2.3.GA/server/default/log/tanyadelete.log"));
            int count=0;
            HashSet<String> idHash = new HashSet<>();
            while (scanner.hasNextLine()){
                String tmpLine = scanner.nextLine();
                if (!"".equals(tmpLine.trim()) && tmpLine.contains("id=")) {
                    tmpLine = tmpLine.substring(tmpLine.indexOf("id=")+3);
                    tmpLine = tmpLine.substring(0,tmpLine.indexOf(")"));
                    count++;
                    System.out.println(tmpLine);
                    idHash.add(tmpLine);
                }
            }
            System.out.println("\nThere were "+count+" ids.");
            System.out.println("There are "+idHash.size()+" ids in the set.\n");

            StringBuilder builder = new StringBuilder();
            for (String id : idHash) {
                builder.append(id).append(",");
            }
            System.out.println(builder);
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
