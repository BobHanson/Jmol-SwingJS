/*
 * Copyright 2008-2011 Sam Adams <sea36 at users.sourceforge.net>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.jnati;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * Uses specifications from META-INF/jnati/system.txt to
 * detect the current operating system/platform.
 *
 * 32bit (eg X86) vs 64bit (eg X86_64) is on the basis of
 * the current JVM, not the underlying operating system.
 *
 * @author Sam Adams
 */
public class SystemTyper {

    private static final String DEFAULT_FILENAME = "META-INF/jnati/system.txt";

    private static final SystemTyper DEFAULT_INSTANCE = new SystemTyper(DEFAULT_FILENAME);
    

    private final String filename;


    public SystemTyper(String filename) {
        if (filename == null) {
            throw new IllegalArgumentException("Null argument: filename");
        }
        this.filename = filename;
    }


    public static SystemTyper getDefaultInstance() {
        return DEFAULT_INSTANCE;
    }



    public SystemType detectPlatform() throws IOException {
        // TODO - what if we can't find definitions file?
        // Search classpath for definitions file
      System.out.println("JNI-InChI SystemTyper detecting platform " + filename);
        Enumeration<URL> enumeration = SystemType.class.getClassLoader().getResources(filename);
        for (URL url : Collections.list(enumeration)) {
            SystemType platform = detectPlatfrom(url);
            if (platform != null) {
                return platform;
            }
        }
        // Search filesystem for definitions file
        File file = new File(filename);
        if (file.isFile()) {
            SystemType platform = detectPlatfrom(file.toURL());
            if (platform != null) {
                return platform;
            }
        }
        return SystemType.UNKNOWN;
    }

    private SystemType detectPlatfrom(URL url) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
        try {
            String platform = null;
            boolean matches = false;
            for (String line = in.readLine(); line != null; line = in.readLine()) {
                // Strip whitespace from start + end of lines
                line = line.trim();
                //System.out.println(matches + " line=" + line);
                
                if (line.length() == 0) {
                    // Either end of platform definition, or blank space
                    if (matches) {
                      System.out.println("JNI-InChI SystemTyper found " + platform);
                        return SystemType.get(platform);
                    } 
                    platform = null;
                }
                else {
                    // ignore comment lines
                    if  (line.charAt(0) == '#') {
                        continue;
                    }
                    // Start of file/end of blank lines
                    // First line is platform name
                    if (platform == null) {
                        platform = line;
                        matches = true;
                        continue;
                    }
                    if (matches) {
                        // File/directory condition
                        if (line.startsWith("file:")) {
                            // format: file: <filename>
                            String filename = line.substring(5).trim();
                            boolean dir = filename.endsWith(File.separator) || filename.endsWith("/");
                            if (dir) {
                                matches &= new File(filename).isDirectory();
                            } else {
                                matches &= new File(filename).isFile();
                            }
                        }
                        // System property condition
                        else {
                            // format: property = regex pattern
                            int i = line.indexOf('=');
                            if (i == -1) {
                                // error - bad definition
                                matches = false;
                                System.err.println();
                                System.err.println("WARNING: Ignoring bad definition in "+url.toString());
                                System.err.println("  Platform: "+platform);
                                System.err.println("  Line: "+line);
                                System.err.println();
                            } else {
                                String property = line.substring(0, i).trim();
                                String regex = line.substring(i+1).trim();
                                String value = System.getProperty(property);
                                Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
                                matches = pattern.matcher(value).matches();
                                //System.out.println("p=" +  property + " r=" + regex + " v=" + value + " p=" + pattern + " m=" + matches);
                            }
                        }
                    }
                }
            }
            if (matches) {
                return SystemType.get(platform);
            }
        } finally {
            in.close();
        }
        return null;
    }

    private static void debug() {
        System.err.println("Properties:");
        for (Map.Entry<?,?> e : new TreeMap<Object,Object>(System.getProperties()).entrySet()) {
            System.err.println("  "+e.getKey()+": "+e.getValue());
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length == 1 && "-debug".equals(args[0])) {
            debug();
        } else {
            System.out.println(getDefaultInstance().detectPlatform().getName());
        }
    }


}
