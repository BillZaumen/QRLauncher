# GNU Make file
# Usage:
# * make - to make compile and create the JAR file
# * make deb - to make a Debian package
# * make installer - to make an installer for non-Debian systems.
# * make clean - to clean up (including the installer)
# * make superclean - to clean up and remove the JAR file

# The files core.jar and javase.jar are from the Debian
# packages libzxing-core-java and libzxing-javase-java.


VERSION = 1.0

DATE = $(shell date -R)

SYS_BINDIR = /usr/bin
SYS_MANDIR = /usr/share/man
SYS_DOCDIR = /usr/share/doc/qrlauncher
ICONDIR = /usr/share/icons/hicolor
SYS_QRLAUNCHERDIR = /usr/share/qrlauncher

SED_QRLAUNCHER = $(shell echo $(SYS_BINDIR)/qrlauncher | sed  s/\\//\\\\\\\\\\//g)
SED_ICONDIR =  $(shell echo $(SYS_ICONDIR) | sed  s/\\//\\\\\\\\\\//g)

APPS_DIR = apps
SYS_APPDIR = /usr/share/applications
SYS_ICON_DIR = /usr/share/icons/hicolor
SYS_APP_ICON_DIR = $(SYS_ICON_DIR)/scalable/$(APPS_DIR)

BINDIR=$(DESTDIR)$(SYS_BINDIR)
MANDIR = $(DESTDIR)$(SYS_MANDIR)
DOCDIR = $(DESTDIR)$(SYS_DOCDIR)
ICONDIR = $(DESTDIR)$(SYS_ICONDIR)
QRLAUNCHERDIR = $(DESTDIR)$(SYS_QRLAUNCHERDIR)
APPDIR = $(DESTDIR)$(SYS_APPDIR)
ICON_DIR = $(DESTDIR)$(SYS_ICON_DIR)
APP_ICON_DIR = $(DESTDIR)$(SYS_APP_ICON_DIR)

SOURCEICON = QRLauncher.svg
TARGETICON = QRLauncher.svg
TARGETICON_PNG = QRLauncher.png


JLDIR = /usr/share/java
CP1 = $(JLDIR)/libbzdev-base.jar:$(JLDIR)/libbzdev-desktop.jar
CP = $(CP1):$(JLDIR)/core.jar:$(JLDIR)/javase.jar:classes

ICON_WIDTHS = 8 16 20 22 24 32 36 48 64 72 96 128 192 256 512
ICON_WIDTHS2x = 16 24 32 48 64 128 256

all: qrlauncher.jar $(DEB) qrlauncher-install-$(VERSION).jar

qrlauncher.jar: QRLauncher.java QRLauncher.svg dndTarget.png
	mkdir -p classes
	javac -Xlint:unchecked -d classes -classpath $(CP) QRLauncher.java
	for i in $(ICON_WIDTHS) ; do \
		inkscape -w $$i \
		--export-filename=classes/QRLauncher$${i}.png \
		QRLauncher.svg ; \
	done
	cp dndTarget.png classes
	cp QRL.properties classes
	jar cf qrlauncher.jar -C classes .

install: qrlauncher.jar
	install -d $(DOCDIR)
	install -d $(QRLAUNCHERDIR)
	install -d $(APP_ICON_DIR)
	install -d $(APPDIR)
	install -d $(BINDIR)
	install -d $(MANDIR)
	install -d $(MANDIR)/man1
	install -m 0644 qrlauncher.jar $(QRLAUNCHERDIR)
	install -m 0755 -T qrl.sh $(BINDIR)/qrl
	sed -e s/VERSION/$(VERSION)/ qrl.1 | gzip -n -9 > qrl.1.gz
	install -m 0644 qrl.1.gz $(MANDIR)/man1
	rm qrl.1.gz
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
	install -m 0644 QRLauncher.desktop $(APPDIR)
	gzip -n -9 < changelog > changelog.gz
	install -m 0644 changelog.gz $(DOCDIR)
	rm changelog.gz
	install -m 0644 copyright $(DOCDIR)

DEB = qrlauncher_$(VERSION)_all.deb

deb: $(DEB)

debLog:
	sed -e s/VERSION/$(VERSION)/ deb/changelog.Debian \
		| sed -e "s/DATE/$(DATE)/" \
		| gzip -n -9 > changelog.Debian.gz
	install -m 0644 changelog.Debian.gz $(DOCDIR)
	rm changelog.Debian.gz

$(DEB): deb/control copyright changelog deb/changelog.Debian \
		deb/postinst deb/postrm qrlauncher.jar \
		qrl.sh qrl.1 QRLauncher.desktop QRLauncher.svg \
		Makefile
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

installer: qrlauncher-install-$(VERSION).jar

qrlauncher-install-$(VERSION).jar: qrlauncher.jar
	(cd inst; make)
	cp inst/qrlauncher-install.jar qrlauncher-install-$(VERSION).jar

clean:
	rm -f classes/*
	rm -rf BUILD
	(cd inst; make clean);

superclean: clean
	rm -f qrlauncher.jar
