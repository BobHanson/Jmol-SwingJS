Summary: A molecular viewer and editor.
Name: jmol
Version: @version@
Release: @release@
Copyright: LGPL
Group: Applications/Science
Packager: Miguel <mth@mth.com>
Source: jmol-@version@.source.tar.gz
BuildArchitectures: noarch
BuildRoot: /var/tmp/%{name}-buildroot

%description
Jmol is an open-source molecule viewer and editor written in Java. Jmol
runs as both an application and an applet. For more information, please
visit the Jmol Web site at http://jmol.sourceforge.net/

%prep
%setup

%build
ant doc main

%install
rm -rf $RPM_BUILD_ROOT
mkdir -p $RPM_BUILD_ROOT/usr/bin
mkdir -p $RPM_BUILD_ROOT/usr/share/jmol/
install -m 755 jmol $RPM_BUILD_ROOT/usr/share/jmol/
ln -s /usr/share/jmol/jmol $RPM_BUILD_ROOT/usr/bin/jmol
install -m 444 jmol.jar $RPM_BUILD_ROOT/usr/share/jmol/
install -m 444 JmolApplet.jar $RPM_BUILD_ROOT/usr/share/jmol/
install -m 444 *.txt $RPM_BUILD_ROOT/usr/share/jmol/
mkdir $RPM_BUILD_ROOT/usr/share/jmol/samples
install -m 444 samples/* $RPM_BUILD_ROOT/usr/share/jmol/samples/
mkdir $RPM_BUILD_ROOT/usr/share/jmol/doc
install -m 444 build/doc/*.html $RPM_BUILD_ROOT/usr/share/jmol/doc/
mkdir $RPM_BUILD_ROOT/usr/share/jmol/doc/JmolHistory
install -m 444 build/doc/JmolHistory/* $RPM_BUILD_ROOT/usr/share/jmol/doc/JmolHistory
mkdir $RPM_BUILD_ROOT/usr/share/jmol/doc/JmolUserGuide
install -m 444 build/doc/JmolHistory/* $RPM_BUILD_ROOT/usr/share/jmol/doc/JmolUserGuide

%clean
rm -rf $RPM_BUILD_ROOT

%files
%defattr(-,root,root)
%doc README.txt COPYRIGHT.txt
/usr/bin/jmol
/usr/share/jmol

%changelog

