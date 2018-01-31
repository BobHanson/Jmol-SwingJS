Q: Where are all the JS files?
A: The js files are in the jsmol directory, which is created when jsmol.zip
   is unzipped. 
   
   jsmol.zip is found in the distribution Jmol binary.zip file that
   can be found at http://sourceforge.net/projects/jmol/files 
    
   See also http://chemapps.stolaf.edu/jmol/zip
      
Q: Where are the JAR files? 
A: The JAR files are in the jsmol/java directory, which is created when jsmol.zip
   is unzipped. 
   
   jsmol.zip is found in the distribution Jmol binary.zip file that
   can be found at http://sourceforge.net/projects/jmol/files 
    
   See also http://chemapps.stolaf.edu/jmol/zip
   
Q: Where is Jmol.js? 
A: In appletweb/old. It's not usable with JSmol. But see jsmol/js/Jmol2.js
   for valuable information about converting from using the older Jmol.js. 
   
Q: Do I really need 1500 js files?
A: No, probably not. Many are included in jsmol/j2s/core/core.z.js, but many are not.
   Rather than figuring out which are and which are not, just upload them all.
    
   That said, you can delete all these directories if you are not interested in them:
   
   jsmol/data         demo/test files
   jsmol/flot         2D graphing package
   jsmol/jquery	      original jQuery files
   jsmol/jspecview    spectroscopy package
   jsmol/jsme         2D structure drawing
   jsmol/js           debugging library files used by some HTML files in demo
   jsmol/php          add only if your server supports PHP
                      used if you point to jsmol/php/jsmol.php with Info.serverURL
   jsmol/spt          contributed script library
   	   
   You can also delete all of jsmol/*.htm, which are Bob's test/demo files
     
   