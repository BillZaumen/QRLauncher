# QRLauncher

QRLauncher is a utility that will read image files or URIs, find a QR
code in the image, decode the QR code, and then pass the encoded URL,
or a file containing encoded data, to a browser.

QRLauncher can be started by a desktop action or by using the command
`qrl`.  The command-line program `qrl` can also create QR codes from
text files or standard input.  When a QR code is created one can specify
the width and height of the image, the image format, the foreground and
background colors (including an alpha channel), and the error-correction
level.

Installation instructions are available
[here](https://billzaumen.github.io/bzdev/).
When an installer is used, libraries to support SVG are automatically
included.  With a package manager, the package libosgbatik-java is needed
to enable SVG support.


## Screenshots

Desktop file:

![Desktop Icon](https//billzaumen.github.io/QRLauncher/desktop.png)

Application window:

![QRLauncher window](https//billzaumen.github.io/QRLauncher/screenshot.png)

Files can be dragged into the window or copied and pasted into the
window. Multiple selections are allowed.  If text containing file names
or URUs are dragged into the window, each URI or file name must be on its
own line.