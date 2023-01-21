# QRLauncher

QRLauncher is a utility that will read image files or URIs, find a QR
code in the image, decode the QR code, and then pass the encoded URL,
or a file containing encoded data, to a browser.

QRLauncher can be started by a desktop action or by using the command
`qrl`.  The command-line program `qrl` can also create QR codes from
text files or standard input.  When a QR code is created one can specify
the width and height of the image, the image format, the foreground and
background colors (including an alpha channel), and the error-correction
level. In addition to the image formats that Java supports, `qrl` provides
PostScript and optionally SVG.

Installation instructions are available
[here](https://billzaumen.github.io/bzdev/).
When an installer is used, libraries to support SVG are automatically
included.  With a package manager, the package libosgbatik-java is needed
to enable SVG support.

The short version for installation on Debian systems is to run the
following commands:

```
curl https://billzaumen.github.io/bzdev/setup.sh | sh
sudo apt-get update
sudo apt install qrlauncher
sudo apt isntall libosgbatik-java
```

The file setup.sh contains commands for configuring apt to use the
repository that contains the necessary Debian packages.

For non-linux systems, download the QRLauncher installer listed on
the [installer page](https://billzaumen.github.io/bzdev/installers.html).
and run

```
sudo java -jar INSTALLER
```

where INSTALLER is the file that was downloaded.  Java, of course, must
be installed first.

## Screenshots

Desktop file:

![Desktop Icon](https://billzaumen.github.io/QRLauncher/desktop.png)

Application window:

![QRLauncher window](https://billzaumen.github.io/QRLauncher/screenshot.png)

Files can be dragged into the window or copied and pasted into the
window. Multiple selections are allowed.  If text containing file names
or URUs are dragged into the window, each URI or file name must be on its
own line.