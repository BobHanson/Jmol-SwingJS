/*
 * Copyright 2006-2011 Sam Adams <sea36 at users.sourceforge.net>
 *
 * This file is part of JNI-InChI.
 *
 * JNI-InChI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * JNI-InChI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with JNI-InChI.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.jniinchi;

import net.sf.jnati.NativeCodeException;
import net.sf.jnati.deploy.NativeLibraryLoader;

import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * <p>JNI Wrapper for International Chemical Identifier (InChI) C++ library.
 *
 * <p>This class is not intended to be used directly, but should be accessed
 * through subclasses that read data formats and load them into the InChI
 * data structures.
 *
 * <p>Subclasses should load data through the addAtom, addBond and addParity
 * methods. Once the molecule is fully loaded then the generateInchi method
 * should be called. Ideally this should all take place within the subclass's
 * constructor. The public get methods will all return null until this has
 * happened.
 *
 * <p>See <tt>inchi_api.h</tt>.
 *
 * @author Sam Adams
 */
public class JniInchiWrapper {

    private static final String ID = "jniinchi";
    private static final String VERSION = "1.03_1";

    /**
     * Maximum time to wait for a lock (in seconds).
     */
    private static final int MAX_LOCK_TIMEOUT = 15;

    /**
     * Flag indicating windows or linux.
     */
    private static final boolean IS_WINDOWS = System.getProperty("os.name", "").toLowerCase().startsWith("windows");

    /**
     * Switch character for passing options. / in windows, - on other systems.
     */
    static final String flagChar = IS_WINDOWS ? "/" : "-";

    /**
     * Records whether native library has been loaded by system.
     */
    private static boolean libraryLoaded = false;

    private static JniInchiWrapper inchiWrapper;

    private static String nativeVersion;

    private static final Lock lock = new ReentrantLock(true); 

    /**
     * Loads native library.
     * @throws JniInchiException Library failed to load
     */
    public static synchronized void loadLibrary() throws LoadNativeLibraryException {
        if (!libraryLoaded) {
            try {
                NativeLibraryLoader.loadLibrary(ID, VERSION);

                // Check expected version of native code loaded
                // Throws NativeCodeException if unable to make call / wrong version
                checkNativeCodeVersion();

                // Everything is set up!
                libraryLoaded = true;
                System.out.println("JniInchiWrapper native version " + nativeVersion);
            } catch (NativeCodeException ex) {
                System.err.println();
                System.err.println("Error loading JNI InChI native code.");
                System.err.println("You may need to compile the native code for your platform.");
                System.err.println("See http://jni-inchi.sourceforge.net for instructions.");
                System.err.println();
                throw new LoadNativeLibraryException(ex);
            }
        }
    }

    /**
     * Checks the expected native code version has been loaded.
     * @throws NativeCodeException
     */
    private static void checkNativeCodeVersion() throws NativeCodeException {

        // Get native code version string
        try {
            nativeVersion = JniInchiWrapper.LibInchiGetVersion();
        } catch (UnsatisfiedLinkError e) {
            throw new NativeCodeException("Unable get native code version", e);
        }

        // Compare to expected version
        if (!VERSION.equals(nativeVersion)) {
          System.err.println("Warning: Expected InChI " + VERSION + " but got InChI " + nativeVersion);
        }

    }


    private static synchronized JniInchiWrapper getWrapper() throws LoadNativeLibraryException {
        if (inchiWrapper == null) {
            loadLibrary();
            init();
            inchiWrapper = new JniInchiWrapper();
        }
        return inchiWrapper;
    }

    /**
     * Constructor
     */
    private JniInchiWrapper() throws LoadNativeLibraryException {

    }


    /**
     * Checks and canonicalises options.
     *
     * @param ops  List of INCHI_OPTION
     */
    protected static String checkOptions(List<INCHI_OPTION> ops) throws JniInchiException {
        if (ops == null) {
            throw new IllegalArgumentException("Null options");
        }
        StringBuffer sbOptions = new StringBuffer();

        for (int i = 0; i < ops.size(); i++) {
            Object op = ops.get(i);
            if (op instanceof INCHI_OPTION) {
                sbOptions.append(flagChar + ((INCHI_OPTION) op).name() + " ");
            } else {
                throw new JniInchiException("Unrecognised InChI option");
            }
        }

        return sbOptions.toString();
    }

    /**
     * Checks and canonicalises options.
     *
     * @param ops          Space delimited string of options to pass to InChI library.
     *                     Each option may optionally be preceded by a command line
     *                     switch (/ or -).
     */
    protected static String checkOptions(final String ops) throws JniInchiException {
        if (ops == null) {
            throw new IllegalArgumentException("Null options");
        }
        StringBuilder sbOptions = new StringBuilder();

        StringTokenizer tok = new StringTokenizer(ops);
        while (tok.hasMoreTokens()) {
            String op = tok.nextToken();

            if (op.startsWith("-") || op.startsWith("/")) {
                op = op.substring(1);
            }

            INCHI_OPTION option = INCHI_OPTION.valueOfIgnoreCase(op);
            if (option != null) {
                sbOptions.append(flagChar + option.name());
                if (tok.hasMoreTokens()) {
                     sbOptions.append(" ");
                }
            } else {
                throw new JniInchiException("Unrecognised InChI option");
            }
        }

        return sbOptions.toString();
    }


    /**
     * <p>Generates the InChI for a chemical structure.</p>
     *
     * <p>If no InChI creation/stereo modification options are specified then a standard
     * InChI is produced, otherwise the generated InChI will be a non-standard one.</p>
     *
     * <p><b>Valid options:</b></p>
     * <pre>
     *  Structure perception (compatible with stdInChI):
     *    /NEWPSOFF   /DoNotAddH   /SNon
     *  Stereo interpretation (lead to generation of non-standard InChI)
     *    /SRel /SRac /SUCF /ChiralFlagON /ChiralFlagOFF
     *  InChI creation options (lead to generation of non-standard InChI)
     *    /SUU /SLUUD   /FixedH  /RecMet  /KET /15T
     * </pre>
     *
     * <p><b>Other options:</b></p>
     * <pre>
     *  /AuxNone    Omit auxiliary information (default: Include)
     *  /Wnumber    Set time-out per structure in seconds; W0 means unlimited
     *              In InChI library the default value is unlimited
     *  /OutputSDF  Output SDfile instead of InChI
     *  /WarnOnEmptyStructure
     *              Warn and produce empty InChI for empty structure
     *  /SaveOpt    Save custom InChI creation options (non-standard InChI)
     * </pre>
     *
     * @param input
     * @return
     * @throws JniInchiException
     */
    @SuppressWarnings("unchecked")
    public static JniInchiOutput getInchi(JniInchiInput input) throws JniInchiException {
        if (input == null) {
            throw new IllegalArgumentException("Null input");
        }
        JniInchiWrapper wrapper = getWrapper();
        JniInchiWrapper.getLock();
        try {
            return wrapper.GetINCHI(input);
        } finally {
            lock.unlock();
        }
    }


    /**
     * <p>Calculates the Standard InChI string for a chemical structure.</p>
     * <p>The only valid structure perception options are NEWPSOFF/DoNotAddH/SNon. In any other structural
     * perception options are specified then the calculation will fail.</p>
     * @param input
     * @return
     * @throws JniInchiException
     */
    @SuppressWarnings("unchecked")
    public static JniInchiOutput getStdInchi(JniInchiInput input) throws JniInchiException {
        if (input == null) {
            throw new IllegalArgumentException("Null input");
        }
        JniInchiWrapper wrapper = getWrapper();
        JniInchiWrapper.getLock();
        try {
            return wrapper.GetStdINCHI(input);
        } finally {
            lock.unlock();
        }
    }


    /**
     * <p>Converts an InChI into an InChI for validation purposes (the same as the -InChI2InChI option).</p>
     * <p>This method may also be used to filter out specific layers. For instance, /Snon would remove the
     * stereochemical layer; Omitting /FixedH and/or /RecMet would remove Fixed-H or Reconnected layers.
     * In order to keep all InChI layers use options string "/FixedH /RecMet"; option /InChI2InChI is not needed.</p>         
     * @param input
     * @return  inchi or null
     * @throws JniInchiException
     */
    public static JniInchiOutput getInchiFromInchi(JniInchiInputInchi input) throws JniInchiException {
        if (input == null) {
            throw new IllegalArgumentException("Null input");
        }
        JniInchiWrapper wrapper = getWrapper();
        JniInchiWrapper.getLock();
        try {
            return wrapper.GetINCHIfromINCHI(input.getInchi(), input.getOptions());
        } finally {
            lock.unlock();
        }
    }

    public static JniInchiOutput getInchiFromInchiString(String inchi, String options) throws JniInchiException {
      if (inchi == null) {
          throw new IllegalArgumentException("Null input");
      }
      JniInchiWrapper wrapper = getWrapper();
      JniInchiWrapper.getLock();
      try {
          return wrapper.GetINCHIfromINCHI(inchi, options);
      } finally {
          lock.unlock();
      }
  }

    /**
     * Generated 0D structure from an InChI string.
     * @param input
     * @return
     * @throws JniInchiException
     */
    public static JniInchiOutputStructure getStructureFromInchi(JniInchiInputInchi input) throws JniInchiException {
        if (input == null) {
            throw new IllegalArgumentException("Null input");
        }
        JniInchiWrapper wrapper = getWrapper();
        JniInchiWrapper.getLock();
        try {
            return wrapper.GetStructFromINCHI(input.getInchi(), input.getOptions());
        } finally {
            lock.unlock();
        }
    }


    /**
     * Calculates the InChIKey for an InChI string.
     * @param inchi     source InChI string
     * @return  InChIKey output
     * @throws  JniInchiException
     */
    public static JniInchiOutputKey getInchiKey(final String inchi) throws JniInchiException {
        if (inchi == null) {
            throw new IllegalArgumentException("Null InChI");
        }
        JniInchiWrapper wrapper = getWrapper();
        JniInchiWrapper.getLock();
        try {
            return wrapper.GetINCHIKeyFromINCHI(inchi);
        } finally {
            lock.unlock();
        }
    }


    /**
     * Checks whether a string represents valid InChIKey.
     * @param key
     * @return
     * @throws JniInchiException
     */
    public static INCHI_KEY_STATUS checkInchiKey(final String key) throws JniInchiException {
        if (key == null) {
            throw new IllegalArgumentException("Null InChI key");
        }
        JniInchiWrapper wrapper = getWrapper();
        JniInchiWrapper.getLock();
        try {
            int ret = wrapper.CheckINCHIKey(key);
            INCHI_KEY_STATUS retStatus = INCHI_KEY_STATUS.getValue(ret);
            if (retStatus == null) {
                throw new JniInchiException("Unknown return status: " + ret);
            }
            return retStatus;
        } finally {
            lock.unlock();
        }
    }


    /**
     * <p>Checks if the string represents valid InChI/standard InChI.</p>
     *
     * @param inchi  source InChI
     * @param strict if <code>false</code>, just briefly check for proper layout (prefix, version, etc.) The result
 *               may not be strict.
 *               If <code>true</code>, try to perform InChI2InChI conversion and returns success if a resulting
 *               InChI string exactly match source. The result may be 'false alarm' due to imperfectness of
     */
    public static INCHI_STATUS checkInchi(final String inchi, final boolean strict) throws JniInchiException {
        if (inchi == null) {
            throw new IllegalArgumentException("Null InChI");
        }
        JniInchiWrapper wrapper = getWrapper();
        JniInchiWrapper.getLock();
        try {
            int ret = wrapper.CheckINCHI(inchi, strict);
            INCHI_STATUS retStatus = INCHI_STATUS.getValue(ret);
            if (retStatus == null) {
                throw new JniInchiException("Unknown return status: " + ret);
            }
            return retStatus;
        } finally {
            lock.unlock();
        }
    }

    public static JniInchiInputData getInputFromAuxInfo(String auxInfo) throws JniInchiException {
        if (auxInfo == null) {
            throw new IllegalArgumentException("Null AuxInfo");
        }
        JniInchiWrapper wrapper = getWrapper();
        JniInchiWrapper.getLock();
        try {
            return wrapper.GetINCHIInputFromAuxInfo(auxInfo, false, false);
        } finally {
            lock.unlock();
        }
    }


    private static synchronized void getLock() throws JniInchiException {
        try {
            if (!lock.tryLock(MAX_LOCK_TIMEOUT, TimeUnit.SECONDS)) {
                throw new TimeoutException("Unable to get lock");
            }
        } catch (TimeoutException ex) {
            throw new JniInchiException(ex);
        } catch (InterruptedException ex) {
            throw new JniInchiException(ex);
        }
    }



    protected native static String LibInchiGetVersion();

    private native static void init();


    private native JniInchiOutput GetINCHI(JniInchiInput input);

    private native JniInchiOutput GetStdINCHI(JniInchiInput input);

    private native JniInchiOutput GetINCHIfromINCHI(String inchi, String options);

    private native JniInchiOutputStructure GetStructFromINCHI(String inchi, String options);

    private native JniInchiOutputKey GetINCHIKeyFromINCHI(String inchi);

    private native JniInchiOutputKey GetStdINCHIKeyFromStdINCHI(String inchi);

    private native int CheckINCHIKey(String key);

    private native int CheckINCHI(String inchi, boolean strict);

    private native JniInchiInputData GetINCHIInputFromAuxInfo(String auxInfo, boolean bDoNotAddH, boolean bDiffUnkUndfStereo);

}
