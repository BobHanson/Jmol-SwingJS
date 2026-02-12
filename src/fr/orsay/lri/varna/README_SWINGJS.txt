
Status
======

2/12/2026  

Interest with DSSR revived.

         *  thisApplet.__Info.sequenceDBN = "GGGGCCAAUAUGGCCAUCC";
         *  thisApplet.__Info.structureDBN = "((((((.....))))..))";
         *  thisApplet.__Info.title = "Hello RNA world, from SwingJS!";//prompt("Title?","Hello RNA world!");

- had to remove two lines of code in VARNAPanel


1/10/2018

fixes VueUI handling of JFileChooser and JColorChooser callbacks not using revised SelectedFile and SelectedColor property names
implements blinking iterator when dragging to reposition RNA blob
implements annotations 

1/7/2018

JTable implemented, including editing; needs better efficiency 
Modal dialogs are working, including file open.
Drag-and-drop of Files is working 
 - note that the dropped File object has bytes field with data
Animated interpolation working; switched to simple JTimer mechanism for Java and JavaScript

1/2/2018

Varna is running.
modal JOptionPane implemented fully
JColorChooser implemented fully
JTable implemented; still some minor issues
 

Modal dialogs are working, except for FileOpen.
Popup menu is working.


Issues
======

- JTable has minor issues:
 - Headings are not shaded
 - needs attention to higher efficiency 

- DnD only implemented for files. 
  - probably x,y coord are off - untested
  - needs checking for isolated frames (works in applet)
  

Modifications for SwingJS
=========================

Search for "@j2sNative", "BH", or "SwingJS"


VARNA.java 
----------

moved to fr.orsay.lri.varna (all SwingJS project files must be in packages)

adds default RNA JavaScript:

	  if (!thisApplet.__Info.sequenceDBN) {
	   thisApplet.__Info.sequenceDBN = "GGGGCCAAUAUGGCCAUCC";
	   thisApplet.__Info.structureDBN = "((((((.....))))..))";
	   thisApplet.__Info.title = prompt("Title?","Hello RNA world, from SwingJS!");
	  } 



fr.orsay.lri.varna.factories.RNAFactory
---------------------------------------

Cannot depend upon Java ArrayIndexOutOfBounds for trapping when testing formats

JAVA fix: Removing unnecessary exception print stack traces during testing for formats
JAVA fix: RNAFactory was not closing file reader



fr.orsay.lri.varna.applications.VARNAEditor
-------------------------------------------

switched to RNAFactory.loadSecStr((File) o) for drag-drop allows passing byte data


fr.orsay.lri.varna.applications.VARNAGUI
----------------------------------------

switched to RNAFactory.loadSecStr((File) o) for drag-drop allows passing byte data


 
fr.orsay.lri.varna.controlers.ControleurBaseSpecialColorEditor
--------------------------------------------------------------

Since the editor is not modal, we have to catch the window hiding event 
before closing the editor.



fr.orsay.lri.varna.controlers.ControleurInterpolator
----------------------------------------------------

switch to JTimer for interpolation
JavaScript uses 2-second delay; Java uses 15-second delay



fr.orsay.lri.varna.views.PrinterTest.java
-----------------------------------------

simpler test that does not use java.awt.font.TextLayout, which is not implemented.



fr.orsay.lri.varna.VarnaPanel.java
----------------------------------

now implements PropertyChangeListener for asynchronous callback.



fr.orsay.lri.varna.views.VueUI.java
------------------------------------

All JOptionPane, JFileChooser, and JColorChooser action made asynchronous. 
Basically, the results OK, CANCEL, YES, NO, CLOSED, and custom button index
are delivered to instances of runnable via a PropertyChangeListener callback
to the indicated parent frame. 

Simple ERROR_OPTION and WARN_OPTION messages are handled via JavaScript Alert;
fall back options for simple showConfirmDialog and showInputDialog are used
automatically if the parent frame does not implement PropertyChangeListener. 

Initial JavaScript-only return is:

 for int-returning methods, NaN, 
   testable as value != Math.floor(value), where value is an int, and
 for Object-returning methods, an Object that implements UIResource,  
   testable as event.getNewValue() instanceof UIResource. 
 
This allows full compatibility in Java and JavaScript.


See notes in fr.orsay.lri.varna.views.VueUI.java.
 


fr.orsay.lri.varna.views.VueMenu.java
-------------------------------------

changes JLabel to JMenuItem - not 100% sure why that is necessary. 
changes JSeparator to JPopupMenu.Separator



fr.orsay.lri.varna.views.VueAbout.java
--------------------------------------

added simple JTimer for asynchronous animation
added asynchronous callback modal option for JavaScript

