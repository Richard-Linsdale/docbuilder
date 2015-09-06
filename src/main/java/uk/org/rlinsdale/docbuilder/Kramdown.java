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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Run the kramdown processing on one or more markdown files.
 *
 * @author Richard Linsdale (richard.linsdale at blueyonder.co.uk)
 */
public class Kramdown {

    /**
     * Run the kramdown processing on one or more markdown files
     *
     * @param indir directory holding the set of markdown sources
     * @param outdir target directory for the generated html
     */
    public void createHTML(File indir, File outdir) {
        if (!indir.isDirectory()) {
            return; // directory does not exist
        }
        if (!outdir.isDirectory()) {
            // need to create directory
            if (!outdir.mkdirs()) {
                System.err.println("*** Errror while attempting to create target directory (" + outdir.getAbsolutePath() + ") ***");
                return;
            }
        }
        // extract the parameters from the environment
        Properties params = new Properties();
        Map<String, String> env = System.getenv();
        env.keySet().stream().forEach((envName) -> {
            if (envName.startsWith("___")) {
                params.setProperty(envName.substring(3), env.get(envName));
            }
        });
        try {
            copyTextResourceFile("kramdown.html.erb", new File("/tmp/kramdowntemplate"));
            copyBinaryDir(new File(indir,"images"), new File(outdir,"images"));
            for (File mdfile : indir.listFiles(new IsMarkdown())) {
                processFileObject(System.err, mdfile, params, outdir);
            }
        } catch (IOException | InterruptedException ex) {
            System.err.println("*** Error while processing files (" + ex.getMessage() + ") ***");
        }
    }

    private static class IsMarkdown implements FilenameFilter {

        @Override
        public boolean accept(File dir, String name) {
            return name.endsWith(".md");
        }
    }

    private void processFileObject(PrintStream err, File mdfile, Properties params, File outdir) throws IOException, InterruptedException {
        copyTextResourceFile("docbuilder.css", new File(outdir, "css"));
        ProcessBuilder pb = new ProcessBuilder("kramdown", "--template", "/tmp/kramdowntemplate/kramdown.html.erb");
        String filepath = mdfile.getAbsolutePath();
        int dotpos = filepath.lastIndexOf(".");
        int slashpos = filepath.lastIndexOf("/");
        String outfilename = filepath.substring(slashpos + 1, dotpos + 1) + "html";
        String propertiespath = filepath.substring(0, dotpos + 1) + "properties";
        System.err.println("Creating " + outfilename);
        Properties props;
        File propsfile = new File(propertiespath);
        if (propsfile.isFile()) {
            props = new Properties();
            props.load(new FileInputStream(propsfile));
        } else {
            props = extractProperties(mdfile, err);
        }
        Process process = pb.start();
        //
        CopyRunnable cr = new CopyRunnable(new BufferedInputStream(new FileInputStream(mdfile)), process.getOutputStream(), err);
        new Thread(cr).start();
        //
        try (OutputStream stdout = new FileOutputStream(new File(outdir, outfilename))) {
            CopyWithFilterRunnable crf = new CopyWithFilterRunnable(process.getInputStream(), stdout, err, params, props);
            new Thread(crf).start();
            readErrorStream(process.getErrorStream(), err);
            process.waitFor();
        }
    }

    private void copyTextResourceFile(String resource, File dir) throws IOException {
        InputStream css = getClass().getResourceAsStream("/uk/org/rlinsdale/docbuilder/" + resource);
        if (!dir.isDirectory()) {
            if (!dir.mkdirs()) {
                System.err.println("*** Errror while attempting to create directory (" + dir.getAbsolutePath() + ") ***");
                return;
            }
        }
        try (BufferedReader in = new BufferedReader(new InputStreamReader(css))) {
            File file = new File(dir, resource);
            try (PrintWriter out = new PrintWriter(new FileOutputStream(file))) {
                String line;
                while ((line = in.readLine()) != null) {
                    out.println(line);
                }
            }
        }
    }
    
    private void copyBinaryDir(File fromdir, File todir) throws IOException {
        if (!fromdir.isDirectory()){
            return; // no from directory so no copy required
        }
        if (!todir.isDirectory()) {
            if (!todir.mkdirs()) {
                System.err.println("*** Errror while attempting to create directory (" + todir.getAbsolutePath() + ") ***");
                return;
            }
        }
        for (File file : fromdir.listFiles()) {
            String filepath = file.getAbsolutePath();
            int slashpos = filepath.lastIndexOf("/");
            String filename = filepath.substring(slashpos + 1);
            try (InputStream is = new FileInputStream(file); OutputStream os = new FileOutputStream(new File(todir,filename))) {
                int read;
                byte[] buffer = new byte[4096];
                while ( (read = is.read(buffer)) != -1){
                    os.write(buffer, 0, read);
                }
            }
        }
    }

    private void readErrorStream(InputStream stderr, PrintStream err) {
        try {
            BufferedReader errReader = new BufferedReader(new InputStreamReader(stderr));
            String line;
            while ((line = errReader.readLine()) != null) {
                err.println(line);
            }
        } catch (IOException ex) {
            err.println("*** Error while reading error stream (" + ex.getMessage() + ") ***");
        }
    }

    private Properties extractProperties(File in, PrintStream err) {
        Properties props = new Properties();
        String line;
        Pattern r = Pattern.compile("^\\s*\\{::comment\\sdefine\\s(\\S*)\\s*=\\s*(.*)\\/\\}");
        try {
            BufferedReader inrdr = new BufferedReader(new FileReader(in));
            while ((line = inrdr.readLine()) != null) {
                Matcher m = r.matcher(line);
                if (m.find()) {
                    props.setProperty(m.group(1).trim(), m.group(2).trim());
                }
            }
        } catch (IOException ex) {
            err.println("*** Error while reading input stream to extract properties (" + ex.getMessage() + ") ***");
        }
        return props;
    }
}
