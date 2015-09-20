/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.org.rlinsdale.docbuilder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import uk.org.rlinsdale.kramdownlibrary.SubstitutionProperties;

/**
 * The Common Index file is an xhtml file which is used to provide information
 * about all released software module/program and provide links to its specific
 * documentation.
 *
 * @author Richard Linsdale (richard.linsdale at blueyonder.co.uk)
 */
public class CommonIndexFile {

    /**
     * Update the common index file with the updated particulars for a specific
     * module/program
     *
     * @param fromfile the file containing the particular modules markdown file
     * and parameters
     * @param indexfile the index file
     * @param envprops the initial environment properties
     */
    public void update(File fromfile, File indexfile, SubstitutionProperties envprops) {
        try {
            if (!fromfile.isFile()) {
                return; // exit if files does not exist
            }
            if (!indexfile.isFile()) {
                return; // exit if files does not exist
            }
            String filepath = fromfile.getAbsolutePath();
            int dotpos = filepath.lastIndexOf(".");
            String propertiespath = filepath.substring(0, dotpos + 1) + "properties";
            SubstitutionProperties props = new SubstitutionProperties();
            props.putAll(envprops);
            props.addFromPropertiesFile(new File(propertiespath));
            props.extractProperties(fromfile);
            List<String> indexmemoryfile = readfile(indexfile);
            updateindexfile(indexmemoryfile, props);
            writefile(indexfile, indexmemoryfile);
        } catch (IOException ex) {
            System.err.println("*** Error while processing files (" + ex.getMessage() + ") ***");
        }
    }

    private List<String> readfile(File indexfile) throws IOException {
        List<String> fileinmemory = new ArrayList<>();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(indexfile)))) {
            in.lines().forEach((line) -> {
                fileinmemory.add(line);
            });
        }
        return fileinmemory;
    }

    private void writefile(File indexfile, List<String> fileinmemory) throws IOException {
        try (PrintWriter out = new PrintWriter(new FileOutputStream(indexfile))) {
            fileinmemory.stream().forEach((line) -> {
                out.println(line);
            });
        }
    }

    private void updateindexfile(List<String> fileinmemory, SubstitutionProperties props) throws IOException {
        String sectiontext = "SECTION_" + props.getProperty("project-key");
        String inserttext = "INSERT_NEW";
        for (int i = 0; i < fileinmemory.size(); i++) {
            if (fileinmemory.get(i).contains(sectiontext)) {
                removesection(fileinmemory, i + 1);
                insertsection(fileinmemory, i + 1, props);
                return;
            }
            if (fileinmemory.get(i).contains(inserttext)) {
                fileinmemory.add(i, "<!-- " + sectiontext + " -->");
                insertsection(fileinmemory, i + 1, props);
                return;
            }
        }
        throw new IOException("Malformed index file");
    }

    private void insertsection(List<String> fileinmemory, int i, SubstitutionProperties props) {
        String projectkey = props.getProperty("project-key", "");
        String projectversion = props.getProperty("project-version", "");
        String docheader = props.getProperty("doc.header", "");
        String doctagline = props.getProperty("doc.tagline", "");
        String status = props.getProperty("status", "");
        fileinmemory.add(i, "<tr>\n"
                + "<td rowspan=\"2\"><a href=\"http://www.rlinsdale.org.uk/software/" + projectkey + "\">" + docheader + "</a></td>\n"
                + "<td colspan=\"2\">" + doctagline + "</td>\n"
                + "</tr>\n"
                + "<tr>\n"
                + "<td>" + projectversion + "</td>\n"
                + "<td>" + status + "</td>\n"
                + "</tr>");
    }

    private void removesection(List<String> fileinmemory, int i) {
        for (int j = 0; j < 8; j++) {
            fileinmemory.remove(i);
        }
    }
}
