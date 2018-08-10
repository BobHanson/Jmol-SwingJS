-------------------------------------------------------------------------------
Copyright © 2004 Sun Microsystems, Inc. All rights reserved. Use is
subject to license terms.

This program is free software; you can redistribute it and/or modify
it under the terms of the Lesser GNU General Public License as
published by the Free Software Foundation; either version 2 of the
License, or (at your option) any later version.

This program is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
USA
-------------------------------------------------------------------------------

SaverBeans Screensaver Pack Unix README
---------------------------------------

Requirements:
  * Administrator access
  * lesstif (provides /usr/X11R6/lib/libXm.so)
    Any version will do - you may need an older version if you don't 
    have glibc2.3; such is the case with Java Desktop System for example.
  * xscreensaver (any version should do, 4-14 and greater is known to work)
  * Java VM 1.5 Beta or higher (Get at http://java.sun.com/).  

To Install:
  * Edit Makefile and jdkhome and xscreensaverhome to valid directories
  * Run 'make' to build the screensaver binaries for your platform
  * In the future, there will be a 'make install' task (contributions welcome).
    For now this has to be done manually:
  * Copy files to the right directories.
      Java Desktop System:
        SCREENSAVER_BIN=/usr/lib/xscreensaver
        SCREENSAVER_CONF=/usr/lib/xscreensaver/config
      Solaris:
        SCREENSAVER_BIN=/usr/openwin/lib/xscreensaver
        SCREENSAVER_CONF=/usr/openwin/share/control-center-2.0/screensavers
      Red Hat 9:
        SCREENSAVER_BIN=/usr/X11R6/bin
        SCREENSAVER_CONF=/usr/share/control-center/screensavers
      Other platforms:
        SCREENSAVER_BIN=(search for an xscreensaver, like apollonian)
        SCREENSAVER_CONF=(search for a config file, like apollonian.xml)
    Please check where your other screensavers are installed.  You will
    need root access to do this (RPM will be provided later)
    1. Copy or symbolically link *.jar to SCREENSAVER_BIN
    2. Copy *.xml to SCREENSAVER_CONF
    3. For each screensaver, you will see two files, e.g.
       bouncingline and bouncingline-bin.  Copy or symbolically 
       link bouncingline and bouncingline-bin to SCREENSAVER_BIN
  * Edit ~/.xscreensaver and add an entry for the screensaver.  For
    example, for BouncingLine, add the following to the programs section:
      "Bouncing Line (Java)" /full/path/to/bouncingline -root \n\
    (the bouncingline.bin, saverbeans-examples.jar saverbeans-api.jar files 
    must appear in the same directory)
    NOTE: If you don't have a .xscreensaver file, go to your screensaver
    preferences and adjust the settings of a screensaver.  The file will
    be created for you automatically.
  * Make sure the Java Virtual Machine can be located by each screensaver
    from the shell launched by the xscreensaver process. 
    The following sources will be checked for your Java Virtual Machine
    (in order).  See the screensaver wrapper script for more details.
    At worst, you can always edit these scripts directly, but usually
    editing ~/.xscreensaver and adding -jdkhome will suffice.
      - -jdkhome parameter, if present (this parameter is also set by the
        screensaver "Java Home" option in the control panel)
      - $JAVA_HOME environment variable, if set
      - `rpm -ql j2sdk`, if available
      - `which java`, if found
      - otherwise error
  * Run xscreensaver-demo to test and select.  Look for 
    "Bouncing Line (Java)".  If it works, you should see a bouncing line
    in the preview window.  If not, look for an error message in stderr.

To Run:
  * Go to screensaver settings - the new screensavers will appear there.
    For a basic example, look for BouncingLine.

Release Notes:
  * JDK 1.4 support is upcoming in a future release.

  * If it does not work but you get no error, try running bouncingline 
    directly from the commandline and observe the output.

  * If you get an error containing:
      libjvm.so: cannot open shared object file: No such file or directory
    Then the screensaver cannot find the JDK.  Pass -jdkhome as 
    a parameter, pointing to a valid installation of J2SDK 1.5.0 or 
    greater.

  * If you get the error:
      Could not find class sun/awt/X11/XEmbeddedFrame
      Exception in thread "main" java.lang.NoClassDefFoundError: 
      sun/awt/X11/XEmbeddedFrame
    Then you're not using JDK 1.5.  JDK 1.5 is currently required on
    Linux/Solaris.  See above for how to fix this (most likely with 
    -jdkhome)

  * If you get the error:
      java.lang.UnsupportedClassVersionError ... 
    (unsupported major.minor version)
    Then you're not using JDK 1.5.  JDK 1.5 is currently required on
    Linux/Solaris.  See above for how to fix this (most likely with 
    -jdkhome)

  * If "Bouncing Line" does not appear, try restarting xscreensaver 
    (or log out and log back in):
      pkill xscreensaver
      xscreensaver -nosplash &
