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

/**
 * Copy a file - using a runnable.
 *
 * @author Richard Linsdale (richard.linsdale at blueyonder.co.uk)
 */
public class CopyRunnable implements Runnable {

    private final InputStream fromStream;
    private final OutputStream toStream;
    private final PrintStream errWriter;

    /**
     * Constructor
     *
     * @param fromStream the input stream to copy from
     * @param toStream the output stream to copy to
     * @param errWriter the OutputWriter to be used to post error messages
     */
    public CopyRunnable(InputStream fromStream, OutputStream toStream, PrintStream errWriter) {
        this.fromStream = fromStream;
        this.toStream = toStream;
        this.errWriter = errWriter;
    }

    @Override
    public void run() {
        try {
            BufferedReader fromReader = new BufferedReader(new InputStreamReader(fromStream));
            try (PrintWriter toWriter = new PrintWriter(new OutputStreamWriter(toStream))) {
                String line;
                while ((line = fromReader.readLine()) != null) {
                    toWriter.println(line);
                }
            }
        } catch (IOException ex) {
            errWriter.println("*** Error while copying stream (" + ex.getMessage() + ") ***");
        }
    }
}
