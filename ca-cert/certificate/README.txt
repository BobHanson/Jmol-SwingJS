Note to developers:

To build a Jmol version with a trusted-signed certificate, you have three options:

Option 1: Indicate a private property file at the prompt. 

This file needs to contain the following properties:

  Private.keystore
  Private.storetype
  Private.storepass
  Private.keypass
  Private.alias

Option 2: create a custom build.xml file or properties file:
 
- Put the PKCS12 file containing your own certificate in this directory. It must be named Jmol.p12
- Run build.xml as usual, but define the following values:
  - Jmol.p12.password: password for the PKCS12 file
  - Jmol.p12.alias: key alias in the PKCS12 file to be used for signing Jmol
  - Jmol.p12key.password: password for the key to be used for signing Jmol

Option 3:

If neither of the first two options is chosen, build.xml will sign the 
jar files using the self-signed certificate in ca-cert/selfSignedCertificate/.
You can then use jarsigner to re-sign the files with your own trusted certificate.
The applet files will be in appletweb/jsmol.zip in the jsmol/java directory.

