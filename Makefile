# GNU Make file
# Usage:
# * make - to make compile and create the JAR file
# * make deb - to make a Debian package
# * make installer - to make an installer for non-Debian systems.
# * make clean - to clean up (including the installer)
# * make superclean - to clean up and remove the JAR file

# The files core.jar and javase.jar are from the Debian
# packages libzxing-core-java and libzxing-javase-java.


VERSION = 1.3

DATE = $(shell date -R)

SYS_BINDIR = /usr/bin
SYS_MANDIR = /usr/share/man
SYS_DOCDIR = /usr/share/doc/qrlauncher
SYS_MIMEDIR = /usr/share/mime
SYS_ICONDIR = /usr/share/icons/hicolor
SYS_QRLAUNCHERDIR = /usr/share/qrlauncher

SED_QRLAUNCHER = $(shell echo $(SYS_BINDIR)/qrlauncher | sed  s/\\//\\\\\\\\\\//g)
SED_ICONDIR =  $(shell echo $(SYS_ICONDIR) | sed  s/\\//\\\\\\\\\\//g)

APPS_DIR = apps
MIMETYPES_DIR = mimetypes

SYS_APPDIR = /usr/share/applications
SYS_ICON_DIR = /usr/share/icons/hicolor
SYS_POPICON_DIR = /usr/share/icons/Pop
SYS_APP_POPICON_DIR = $(SYS_POPICON_DIR)/scalable/$(APPS_DIR)
SYS_APP_ICON_DIR = $(SYS_ICON_DIR)/scalable/$(APPS_DIR)
SYS_MIME_ICON_DIR =$(SYS_ICON_DIR)/scalable/$(MIMETYPES_DIR)

BINDIR=$(DESTDIR)$(SYS_BINDIR)
MANDIR = $(DESTDIR)$(SYS_MANDIR)
DOCDIR = $(DESTDIR)$(SYS_DOCDIR)
MIMEDIR = $(DESTDIR)$(SYS_MIMEDIR)

ICONDIR = $(DESTDIR)$(SYS_ICONDIR)
QRLAUNCHERDIR = $(DESTDIR)$(SYS_QRLAUNCHERDIR)
APPDIR = $(DESTDIR)$(SYS_APPDIR)
ICON_DIR = $(DESTDIR)$(SYS_ICON_DIR)
MIME_ICON_DIR = $(DESTDIR)$(SYS_MIME_ICON_DIR)
SYS_MIME_POPICON_DIR =$(SYS_POPICON_DIR)/scalable/$(MIMETYPES_DIR)
MIME_POPICON_DIR = $(DESTDIR)$(SYS_MIME_POPICON_DIR)
APP_ICON_DIR = $(DESTDIR)$(SYS_APP_ICON_DIR)
APP_POPICON_DIR = $(DESTDIR)$(SYS_APP_POPICON_DIR)

SOURCEICON = QRLauncher.svg
TARGETICON = QRLauncher.svg
TARGETICON_PNG = QRLauncher.png

SOURCE_CFILE_ICON = QRLConf.svg
TARGET_CFILE_ICON = application-vnd.bzdev.qrlauncher-config.svg
TARGET_CFILE_ICON_PNG = application-vnd.bzdev.qrlauncher-config.png


JLDIR = /usr/share/java
CP1 = $(JLDIR)/libbzdev-base.jar:$(JLDIR)/libbzdev-desktop.jar
CP = $(CP1):$(JLDIR)/core.jar:$(JLDIR)/javase.jar:classes

ICON_WIDTHS = 8 16 20 22 24 32 36 48 64 72 96 128 192 256 512
ICON_WIDTHS2x = 16 24 32 48 64 128 256

POPICON_WIDTHS = 8 16 24 32 48 64 128 256
POPICON_WIDTHS2x = 8 16 24 32 48 64 128 256


DEB = qrlauncher_$(VERSION)_all.deb
POP_DEB = qrlauncher-pop-icons_$(VERSION)_all.deb
SYS_POP_DOCDIR = /usr/share/doc/qrlauncher-pop-icons
POP_DOCDIR=$(DESTDIR)$(SYS_POP_DOCDIR)


jarfile: qrlauncher.jar

release: qrlauncher.jar $(DEB) newpopchangelog $(POP_DEB) \
	qrlauncher-install-$(VERSION).jar

qrlauncher.jar: QRLauncher.java QRLauncher.svg dndTarget.png
	mkdir -p classes
	javac -Xlint:unchecked -d classes -classpath $(CP) QRLauncher.java
	for i in $(ICON_WIDTHS) ; do \
		inkscape -w $$i \
		--export-filename=classes/QRLauncher$${i}.png \
		QRLauncher.svg ; \
		inkscape -w $$i \
		--export-filename=classes/QRLConf$${i}.png \
		QRLConf.svg ; \
	done
	cp dndTarget.png classes
	cp QRL.properties classes
	jar cf qrlauncher.jar -C classes .

install: qrlauncher.jar ReadMe qrl.sh qrl.1 qrl.5 QRLauncher.desktop \
		$(SOURCEICON) $(SOURCE_CFILE_ICON) changelog
	install -d $(DOCDIR)
	install -m 0644 ReadMe $(DOCDIR)
	install -d $(QRLAUNCHERDIR)
	install -d $(MIME_ICON_DIR)
	install -d $(MIMEDIR)
	install -d $(MIMEDIR)/packages
	install -d $(APP_ICON_DIR)
	install -d $(APPDIR)
	install -d $(BINDIR)
	install -d $(MANDIR)
	install -d $(MANDIR)/man1
	install -d $(MANDIR)/man5
	install -m 0644 qrlauncher.jar $(QRLAUNCHERDIR)
	install -m 0755 -T qrl.sh $(BINDIR)/qrl
	sed -e s/VERSION/$(VERSION)/ qrl.1 | gzip -n -9 > qrl.1.gz
	install -m 0644 qrl.1.gz $(MANDIR)/man1
	rm qrl.1.gz
	sed -e s/VERSION/$(VERSION)/ qrl.5 | gzip -n -9 > qrl.5.gz
	install -m 0644 qrl.5.gz $(MANDIR)/man5
	rm qrl.5.gz
	install -m 0644 -T $(SOURCEICON) $(APP_ICON_DIR)/$(TARGETICON)
	for i in $(ICON_WIDTHS) ; do \
		install -d $(ICON_DIR)/$${i}x$${i}/$(APPS_DIR) ; \
		inkscape -w $$i --export-filename=tmp.png $(SOURCEICON) ; \
		install -m 0644 -T tmp.png \
			$(ICON_DIR)/$${i}x$${i}/$(APPS_DIR)/$(TARGETICON_PNG); \
		rm tmp.png ; \
	done
	for i in $(ICON_WIDTHS2x) 512 ; do \
		ii=`expr 2 '*' $$i` ; \
		install -d $(ICON_DIR)/$${i}x$${i}@2x/$(APPS_DIR) ; \
		inkscape -w $$ii --export-filename=tmp.png $(SOURCEICON) ; \
		install -m 0644 -T tmp.png \
		    $(ICON_DIR)/$${i}x$${i}@2x/$(APPS_DIR)/$(TARGETICON_PNG); \
		rm tmp.png ; \
	done
	install -m 0644 -T MIME/QRLauncher.xml \
		$(MIMEDIR)/packages/QRLauncher.xml
	for i in $(ICON_WIDTHS) ; do \
	  dir=$(ICON_DIR)/$${i}x$${i}/$(MIMETYPES_DIR) ; \
	  install -d $$dir ; \
	  inkscape -w $$i --export-filename=tmp.png $(SOURCE_CFILE_ICON) ; \
	  install -m 0644 -T tmp.png $$dir/$(TARGET_CFILE_ICON_PNG); \
	  rm tmp.png ; \
	done
	for i in $(ICON_WIDTHS2x) ; do \
	  ii=`expr 2 '*' $$i` ; \
	  dir=$(ICON_DIR)/$${i}x$${i}@2x/$(MIMETYPES_DIR) ; \
	  install -d $$dir ; \
	  inkscape -w $$ii --export-filename=tmp.png $(SOURCE_CFILE_ICON) ; \
	  install -m 0644 -T tmp.png $$dir/$(TARGET_CFILE_ICON_PNG);\
	  rm tmp.png ; \
	done
	install -m 0644 QRLauncher.desktop $(APPDIR)
	gzip -n -9 < changelog > changelog.gz
	install -m 0644 changelog.gz $(DOCDIR)
	rm changelog.gz
	install -m 0644 copyright $(DOCDIR)
	sed s/VERSION/"$(VERSION)"/ deb/changelog.Debian \
	    | sed s/DATE/"$(DATE)"/ | gzip -9 -n > changelog.Debian.gz
	install -m 0644 -T changelog.Debian.gz \
		$(DOCDIR)/changelog.Debian.gz

install-pop: popchangelog deb/popchangelog.Debian copyright ReadMe
	install -d $(APP_POPICON_DIR)
	install -d $(MIME_POPICON_DIR)
	install -m 0644 -T $(SOURCEICON) $(APP_POPICON_DIR)/$(TARGETICON)
	install -m 0644 -T $(SOURCE_CFILE_ICON) \
		$(MIME_POPICON_DIR)/$(TARGET_CFILE_ICON)
	install -d $(POP_DOCDIR)
	install -m 0644 copyright $(POP_DOCDIR)
	gzip -9 -n < popchangelog > popchangelog.gz
	install -m 0644 -T popchangelog.gz $(POP_DOCDIR)/changelog.gz
	rm popchangelog.gz
	sed s/VERSION/"$(VERSION)"/ deb/popchangelog.Debian \
	    | sed s/DATE/"$(DATE)"/ | gzip -9 -n > popchangelog.Debian.gz
	install -m 0644 -T popchangelog.Debian.gz \
		$(POP_DOCDIR)/changelog.Debian.gz
	rm popchangelog.Debian.gz
	install -m 0644 ReadMe $(POP_DOCDIR)


deb: $(DEB)

debLog:
	sed -e s/VERSION/$(VERSION)/ deb/changelog.Debian \
		| sed -e "s/DATE/$(DATE)/" \
		| gzip -n -9 > changelog.Debian.gz
	install -m 0644 changelog.Debian.gz $(DOCDIR)
	rm changelog.Debian.gz

popDebLog:
	sed -e s/VERSION/$(VERSION)/ deb/popchangelog.Debian \
		| sed -e "s/DATE/$(DATE)/" \
		| gzip -n -9 > changelog.Debian.gz
	install -m 0644 changelog.Debian.gz $(POP_DOCDIR)
	rm changelog.Debian.gz


$(DEB): deb/control copyright changelog deb/changelog.Debian \
		deb/postinst deb/postrm qrlauncher.jar \
		qrl.sh qrl.1 qrl.5 QRLauncher.desktop QRLauncher.svg \
		Makefile MIME/QRLauncher.xml QRLConf.svg
	mkdir -p BUILD
	(cd BUILD ; rm -rf usr DEBIAN)
	mkdir -p BUILD/DEBIAN
	cp deb/postinst BUILD/DEBIAN/postinst
	chmod a+x BUILD/DEBIAN/postinst
	cp deb/postrm BUILD/DEBIAN/postrm
	chmod a+x BUILD/DEBIAN/postrm
	$(MAKE) install DESTDIR=BUILD debLog
	sed -e s/VERSION/$(VERSION)/ deb/control > BUILD/DEBIAN/control
	fakeroot dpkg-deb --build BUILD
	mv BUILD.deb $(DEB)

$(POP_DEB): deb/popicons-control copyright QRLauncher.svg QRLConf.svg \
		deb/popchangelog.Debian deb/popicons-postinst \
		deb/popicons-postrm
	mkdir -p BUILD_POP
	(cd BUILD_POP ; rm -rf usr DEBIAN)
	mkdir -p BUILD_POP/DEBIAN
	$(MAKE) install-pop DESTDIR=BUILD_POP
	sed s/VERSION/$(VERSION)/ deb/popicons-control > \
		BUILD_POP/DEBIAN/control
	cp deb/popicons-postinst BUILD_POP/DEBIAN/postinst
	chmod 0755 BUILD_POP/DEBIAN/postinst
	cp deb/popicons-postrm BUILD_POP/DEBIAN/postrm
	chmod 0755 BUILD_POP/DEBIAN/postrm
	fakeroot dpkg-deb --build BUILD_POP
	mv BUILD_POP.deb $(POP_DEB)

installer: qrlauncher-install-$(VERSION).jar

qrlauncher-install-$(VERSION).jar: qrlauncher.jar
	(cd inst; make)
	cp inst/qrlauncher-install.jar qrlauncher-install-$(VERSION).jar


newpopchangelog:
	echo "qrlauncher-pop-icons ($(VERSION)) unstable; urgency=low" \
		> POPCHANGELOG
	echo >> POPCHANGELOG
	echo "  * Synchronized with QRLauncher version $(VERSION)" \
		>> POPCHANGELOG
	echo >> POPCHANGELOG
	echo " -- " `git config --get user.name` \
		'<'`git config --get user.email`'>  '`date -R` >> POPCHANGELOG
	echo >> POPCHANGELOG
	git show HEAD:popchangelog >> POPCHANGELOG
	cat POPCHANGELOG | git stripspace -s  > popchangelog.tmp
	cmp -s popchangelog popchangelog.tmp || cp popchangelog.tmp popchangelog
	rm -f POPCHANGELOG popchangelog.tmp


clean:
	rm -f classes/*
	rm -rf BUILD
	(cd inst; make clean);

superclean: clean
	rm -f qrlauncher.jar
