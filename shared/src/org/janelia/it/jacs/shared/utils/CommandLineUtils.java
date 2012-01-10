
package org.janelia.it.jacs.shared.utils;

import org.janelia.it.jacs.model.TimebasedIdentifierGenerator;

import java.util.Properties;

/**
 * Created by IntelliJ IDEA.
 * User: lkagan
 * Date: Apr 25, 2008
 * Time: 1:19:19 PM
 */
public class CommandLineUtils {
    public static void main(String[] args) {
        CommandLineUtils clUtils = new CommandLineUtils();
        UtilArgs utilArgs = new UtilArgs(args);
        if (utilArgs.getProperty("action").equals("makeid")) {
            long id = clUtils.generarateID(utilArgs.getProperty("context"));
            if (id > 0)
                System.out.println(id);
            else
                System.exit(1);
        }
    }

    private long generarateID(String context) {
        try {
            int newContext = Integer.parseInt(context);
            if (newContext < 1 || newContext > 12) {
                throw new NumberFormatException("Value must be between 1 and 12");
            }
            TimebasedIdentifierGenerator.setDeploymentContext(newContext);
            return (Long) TimebasedIdentifierGenerator.generate(1);
        }
        catch (NumberFormatException e) {
            System.out.println("Invalid value for agrument 'context':" + context + "\nIt's value must be a number between 1 and 12");
            return -1L;
        }
        catch (Exception e) {
            System.out.println("ERROR: " + e.getMessage());
            e.printStackTrace();
            return -1L;
        }
    }
}

class UtilArgs extends Properties {
    public UtilArgs() {
    }

    public UtilArgs(String[] args) {
        for (String arg : args) {
            String[] nameValue = arg.split("=");
            if (nameValue.length > 1) {
                nameValue[0] = nameValue[0].trim();
                nameValue[1] = nameValue[1].trim();
            }
            else {
                throw new IllegalArgumentException("name and value must be separated by =");
            }
            this.setProperty(nameValue[0], nameValue[1]);
        }
    }
}
