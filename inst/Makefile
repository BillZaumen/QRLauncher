
all: qrlauncher.jar
	rm -rf TBUILD
	mkdir TBUILD
	javac -d TBUILD Installer.java
	(cd .. ; git log -64 ) > TBUILD/changelog
	echo >> TBUILD/changelog
	echo --- Earlier changes not shown --- >> TBUILD/changelog
	cp qrlauncher.jar TBUILD/qrlauncher.jar
	cp /usr/share/java/libbzdev-base.jar TBUILD/libbzdev-base.jar
	cp /usr/share/java/libbzdev-desktop.jar TBUILD/libbzdev-desktop.jar
	cp /usr/share/java/libbzdev-math.jar TBUILD/libbzdev-math.jar
	cp /usr/share/java/libbzdev-graphics.jar TBUILD/libbzdev-graphics.jar
	cp /usr/share/java/libosgbatik.jar TBUILD/libosgbatik.jar
	cp /usr/share/java/core.jar TBUILD/core.jar
	cp /usr/share/java/javase.jar TBUILD/javase.jar
	cp ../qrl.1 TBUILD/qrl.1
	cp ../qrl.5 TBUILD/qrl.5
	cp ../copyright TBUILD/copyright
	cp ../QRLauncher.desktop TBUILD/QRLauncher.desktop
	gzip -9 < TBUILD/qrl.1 > TBUILD/qrl.1.gz
	groff -T html ../qrl.1 > TBUILD/qrl.html
	gzip -9 < TBUILD/qrl.5 > TBUILD/qrl.5.gz
	groff -T html ../qrl.5 > TBUILD/qrl-files.html
	mkdir -p icons/hicolor/scalable/apps
	cp ../QRLauncher.svg icons/hicolor/scalable/apps/QRLauncher.svg
	for i in 512 256 128 96 72 64 48 32 24 16 8 ; do \
	mkdir -p icons/hicolor/$${i}x$${i}/apps ; \
	inkscape -w $$i \
	  --export-filename=icons/hicolor/$${i}x$${i}/apps/QRLauncher.png \
	  ../QRLauncher.svg ; done
	(zfile=`pwd`/TBUILD/share.zip ; zip -r -q $$zfile icons)
	rm -r icons
	cp finish.txt TBUILD/finish.txt
	echo '#!/bin/sh' > TBUILD/qrl.sh
	echo 'JAVA -Dqrl.cmd=$$0 -jar QRLDIR/qrlauncher.jar "$$@"'\
		>> TBUILD/qrl.sh
	echo \
	'@JAVA -Dqrl.cmd=QRLDIR/bin/qrl.cmd -jar QRLDIR/qrlauncher.jar %*' \
	> TBUILD/qrl.cmd
	echo \
	'@JAVAW -Dqrl.cmd=QRLDIR/bin/qrlw.cmd -jar QRLDIR/qrlauncher.jar %*' \
	> TBUILD/qrlw.cmd
	jar cfm qrlauncher-install.jar install.mf -C TBUILD .

qrlauncher.jar: ../classes/QRLauncher.class manifest.mf
	jar cfm qrlauncher.jar manifest.mf  -C ../classes .

../classes/QRLauncher.class: ../QRLauncher.java ../QRLauncher.svg
	(cd .. ; make qrlauncher.jar)

clean:
	rm -rf TBUILD qrlauncher.jar

superclean: clean
	rm -f qrlauncher-install.jar
