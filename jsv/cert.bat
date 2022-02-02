rem keytool -delete -alias JSVcertificate -keystore certificate\JSVcertificate.store -keypass JSV2013 -dname "cn=Chemistry Department UWI Jamaica" -storepass JSV2013 -validity 3650

keytool -genkey -alias JSVcertificate -keystore certificate\JSVcertificate.store -keypass JSV2013 -dname "cn=Chemistry Department UWI Jamaica" -storepass JSV2013  -validity 3650
