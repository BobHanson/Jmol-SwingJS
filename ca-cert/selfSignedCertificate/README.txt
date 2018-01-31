[mth@mykiss Jmol-HEAD]$ cd selfSignedCertificate/
[mth@mykiss selfSignedCertificate]$ ls
README.txt  selfSignedCertificate.cer  selfSignedCertificate.store
[mth@mykiss selfSignedCertificate]$ emacs README.txt
[mth@mykiss selfSignedCertificate]$ ls
README.txt  selfSignedCertificate.cer  selfSignedCertificate.store
[mth@mykiss selfSignedCertificate]$ rm self*
[mth@mykiss selfSignedCertificate]$ keytool -v -genkey -keyalg RSA -validity 10000 -keystore selfSignedCertificate.store -alias selfSignedCertificate
Enter keystore password:  selfSignedCertificate
What is your first and last name?
  [Unknown]:  Jmol www.jmol.org
What is the name of your organizational unit?
  [Unknown]:  Jmol Developers
What is the name of your organization?
  [Unknown]:  Jmol Open Source Molecular Visualization Project
What is the name of your City or Locality?
  [Unknown]:  www.jmol.org
What is the name of your State or Province?
  [Unknown]:  jmol.sourceforge.net
What is the two-letter country code for this unit?
  [Unknown]:  US
Is CN=Jmol www.jmol.org, OU=Jmol Developers, O=Jmol Open Source Molecular Visualization Project, L=www.jmol.org, ST=jmol.sourceforge.net, C=US correct?
  [no]:  yes

Generating 1,024 bit RSA key pair and self-signed certificate (MD5WithRSA)
        for: CN=Jmol www.jmol.org, OU=Jmol Developers, O=Jmol Open Source Molecular Visualization Project, L=www.jmol.org, ST=jmol.sourceforge.net, C=US
Enter key password for <selfSignedCertificate>
        (RETURN if same as keystore password):
[Storing selfSignedCertificate.store]
[mth@mykiss selfSignedCertificate]$  keytool -export -keystore selfSignedCertificate.store -alias selfSignedCertificate -file selfSignedCertificate.cer
Enter keystore password:  selfSignedCertificate
Certificate stored in file <selfSignedCertificate.cer>
[mth@mykiss selfSignedCertificate]$


[mth@mykiss Jmol-HEAD]$ jarsigner -verbose -keystore selfSignedCertificate/selfSignedCertificate.store -storepass selfSignedCertificate -signedjar JmolAppletSigned.jar JmolApplet.jar selfSignedCertificate
 updating: META-INF/MANIFEST.MF
   adding: META-INF/SELFSIGN.SF
   adding: META-INF/SELFSIGN.RSA
   adding: org/
[snip]

