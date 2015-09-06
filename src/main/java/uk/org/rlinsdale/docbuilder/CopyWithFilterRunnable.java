/*
 * Copyright (C) 2015 Richard Linsdale
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package uk.org.rlinsdale.docbuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Copy a file applying a filter to all lines copied, executing the action on a
 * runnable.
 *
 * @author Richard Linsdale (richard.linsdale at blueyonder.co.uk)
 */
public class CopyWithFilterRunnable implements Runnable {

    private final InputStream fromStream;
    private final OutputStream toStream;
    private final PrintStream errWriter;
    private final Properties properties;

    /**
     * Constructor
     *
     * @param fromStream the input stream to copy from
     * @param toStream the output stream to copy to
     * @param errWriter the OutputWriter to be used to post error messages
     * @param params the parameters defined for this filter action
     * @param props the defined properties for this filter action
     */
    public CopyWithFilterRunnable(InputStream fromStream, OutputStream toStream, PrintStream errWriter,
            Properties params, Properties props) {
        this.fromStream = fromStream;
        this.toStream = toStream;
        this.errWriter = errWriter;
        props.putAll(params);
        props.setProperty("doc.menu", createMenu(props.getProperty("doc.menu.menu"), errWriter));
        properties = props;
    }

    private String createMenu(String menustring, PrintStream errWriter) {
        StringBuilder menu = new StringBuilder();
        int sep;
        Pattern r = Pattern.compile("^\\[(.*)\\]\\((.*)\\)");
        do {
            sep = menustring.indexOf('|');
            String menucmd;
            if (sep == -1) {
                menucmd = menustring.trim();
                menustring = "";
            } else {
                menucmd = menustring.substring(0,sep).trim();
                menustring = menustring.substring(sep+1);
            }
            Matcher m = r.matcher(menucmd);
            if (m.find()) {
                menu.append("<span class=\"tabtext\"><a href=\"");
                menu.append(m.group(2));
                menu.append("\">");
                menu.append(m.group(1));
                menu.append("</a></span>");
            } else {
                errWriter.println("*** Error while creating menu - malformed menu definition (" + menucmd + ") ***");
            }
        } while (!menustring.isEmpty());
        return menu.toString();
    }

    @Override
    public void run() {
        try {
            BufferedReader fromReader = new BufferedReader(new InputStreamReader(fromStream));
            try (PrintWriter toWriter = new PrintWriter(new OutputStreamWriter(toStream))) {
                String line;
                while ((line = fromReader.readLine()) != null) {
                    line = applypropertiesfilter(line);
                    toWriter.println(line);
                }
            }
        } catch (IOException ex) {
            errWriter.println("*** Error while copying stream (" + ex.getMessage() + ") ***");
        }
    }

    private String applypropertiesfilter(String line) {
        while (true) {
            int brapos = line.indexOf("${");
            if (brapos == -1) {
                return line;
            }
            int ketpos = line.indexOf("}", brapos + 2);
            if (ketpos == -1) {
                errWriter.println("*** Error while filtering stream - malformed substitution brackets (" + line + ") ***");
                return line;
            }
            String prebra = line.substring(0, brapos);
            String postket = line.substring(ketpos + 1);
            String propname = line.substring(brapos + 2, ketpos);
            int barpos = propname.indexOf('|');
            String propvalue;
            if (barpos != -1) {
                String value = propname.substring(barpos + 1);
                propname = propname.substring(0, barpos);
                propvalue = properties.getProperty(propname);
                if (propvalue == null) {
                    propvalue = value;
                }
            } else {
                propvalue = properties.getProperty(propname);
                if (propvalue == null) {
                    errWriter.println("*** Error while filtering stream - request property value (" + propname + ") is undefined ***");
                    propvalue = "???";
                }
            }
            line = prebra + propvalue + postket;
        }
    }
}
