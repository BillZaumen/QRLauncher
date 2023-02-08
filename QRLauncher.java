import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.Transferable;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import javax.swing.ImageIcon;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.filechooser.FileFilter;
import org.bzdev.gio.OutputStreamGraphics;
import org.bzdev.graphs.Colors;
import org.bzdev.io.AppendableWriter;
import org.bzdev.protocols.Handlers;
import org.bzdev.swing.CSSCellEditor;
import org.bzdev.swing.ConfigPropertyEditor;
import org.bzdev.swing.DarkmodeMonitor;
import org.bzdev.swing.ExtObjTransferHandler;
import org.bzdev.swing.FileNameCellEditor;
import org.bzdev.swing.SimpleConsole;
import org.bzdev.swing.table.CSSTableCellRenderer;


import com.google.zxing.qrcode.encoder.Encoder;
import com.google.zxing.qrcode.encoder.QRCode;
import com.google.zxing.qrcode.encoder.ByteMatrix;

public class QRLauncher {

    static private final String resourceBundleName = "QRL";
    static ResourceBundle bundle = ResourceBundle.getBundle(resourceBundleName);
    public static String localeString(String name) {
	return bundle.getString(name);
    }
    
    static java.util.List<Image> iconList = new LinkedList<Image>();
    static SimpleConsole tc = null;
    static Appendable output = null;
    static boolean launch = true;
    static boolean exitMode = false;
    static JFrame frame;
    static ExtObjTransferHandler th;
    static final String targetHTML = "<HTML><BODY>"
	+ "<H1><IMG SRC=\"sresource:dndTarget.png\"> &nbsp; &nbsp;"
	+ localeString("instructions") + "</H1>"
	+ "</BODY></HTML>";

    // Default number taken from the zxing class QRCodeWriter
    // so we'll be consistent.
    private static final int QUIET_ZONE_SIZE = 4;
    
    static LinkedList<URI> uriList = new LinkedList<>();
    static void doAdd(URL url) throws IOException {
	try {
	    if (output != null) output.append("" + url);
	    if (output != tc && tc != null) {
		tc.append("" + url);
	    }
	    String contents = null;
	    InputStream is = url.openStream();
	    BufferedImage bufferedImage = ImageIO.read(is);
	    LuminanceSource source = new
		BufferedImageLuminanceSource(bufferedImage);
	    BinaryBitmap bitmap =
		new BinaryBitmap(new HybridBinarizer(source));
	    String uri = (new MultiFormatReader().decode(bitmap))
		.getText();
	    URI u = null;
	    try {
		u = new URI(uri);
	    } catch (URISyntaxException euri) {
		// Assume what was decoded is not a URI,
		// so create a text file containing the data.
		String txt = ".txt";
		String suffix = txt;
		int ind1 = uri.indexOf("<");
		if (ind1 != -1) {
		    int ind2 = uri.indexOf(">");
		    if (ind2 != -1 && ind1 < ind2) {
			String tst = uri.stripLeading();
			if (tst.charAt(0) == '<') {
			    tst = uri.substring(ind1+1, ind2);
			    if (ind1 == 0 &&
				tst.matches("(!DOCTYPE\\s+|\\s*)"
					    + "[hH][tT][mM][lL]"
					    + "(\\s+|\\z)")) {
				suffix =".html";
			    }
			}
		    }
		}
		if (suffix == txt) {
		    if (uri.startsWith("BEGIN:")) {
			String tst = uri.substring(6);
			int mode = -1;
			if (tst.startsWith("VCARD")) {
			    mode = 0;
			    tst = uri.substring(6+5);
			} else if (tst.startsWith("VCALENDAR")) {
			    mode = 1;
			    tst = uri.substring(6+9);
			} else if (tst.startsWith("VEVENT")) {
			    // The people implemnting this apparently
			    // can't be bothered to use VCALENDAR, so
			    // we'll add a wrapper.
			    //
			    // In case they forgot the ending CRLF ...
			    char ch = uri.charAt(uri.length() - 1);
			    if (ch != '\r' && ch != '\n') {
				uri = uri + "\r\n";
			    }
			    tst = uri.substring(6 + 6);
			    uri = "BEGIN:VCALENDAR\r\n" + uri
				+ "END:VCALENDAR\r\n";
			    mode = 1;
			}
			if (tst.length() > 2 && mode != -1 &&
			    ((tst.charAt(0) == '\r' && tst.charAt(1) == '\n')
			     || tst.charAt(0) == '\n')) {
			    switch (mode) {
			    case 0:
				suffix = ".vcf";
				break;
			    case 1:
				suffix = ".ics";
				break;
			    }
			}
		    }
		}
		// found the suffix so create a tmp file and use that
		// to get the URI.
		File tmp = File.createTempFile("qrl-url-contents", suffix);
		tmp.deleteOnExit();
		PrintWriter w = new PrintWriter(tmp, "UTF-8");
		contents = uri;
		w.print(uri);
		w.close();
		u = tmp.toURI();
		uri = "[ " + u.toString() + " ]";
	    }

	    if (output != null) output.append(" \u2192 " + uri);
	    if (output != tc && tc != null) {
		tc.append(" \u2192 " + uri);
	    }
	    if (launch) {
		if (Desktop.isDesktopSupported()) {
		    Desktop desktop = Desktop.getDesktop();
		    if (desktop.isSupported(Desktop.Action.BROWSE)) {
			if (exitMode) {
			    try {
				desktop.browse(u);
			    } catch (IOException eio) {
				System.err.println(eio.getMessage());
			    }
			} else {
			    URI uu = u;
			    SwingUtilities.invokeLater(() -> {
				    try {
					desktop.browse(uu);
				    } catch (IOException eio) {
					System.err.println(eio.getMessage());
				    }
				});
			}
		    }
		}
	    } else if (contents != null) {
		contents = " ...\n" + contents + "\n------------";
		if (output != null) output.append(contents);
		if (output != tc && tc != null) {
		    tc.append(contents);
		}
	    }
			
	} catch (Exception e) {
	    String msg = e.getMessage();
	    if (output != null) {
		output.append (" " + localeString("FAILED")
			       + (msg == null? "": " - " + msg));
	    }
	    if (output != tc && tc != null) {
		tc.append (" " + localeString("FAILED")
			   + (msg == null? "": " - " + msg));
	    }
	}
	if (output != null) output.append("\n");
	if (output != tc && tc != null) {
	    tc.append("\n");
	}
    }


    static void setTransferHandlerRecursively(Component c, TransferHandler h) {
	if (c instanceof JComponent) {
	    ((JComponent) c).setTransferHandler(h);
	}
	if (!(c instanceof AbstractButton)) {
	    /*
	     * We seem to need a key listener on components other than
	     * buttons and menu items: otherwise Control-V doesn't seem
	     * to be recognized as an accelerator in some cases.
	     */
	    c.addKeyListener(keyListener);
	}
	if (c instanceof Container) {
	    synchronized(c.getTreeLock()) {
		for (Component cc: ((Container) c).getComponents()) {
		    setTransferHandlerRecursively(cc, h);
		}
	    }
	}
    }

    static int vk(String key) {
	return org.bzdev.swing.keys.VirtualKeys.lookup(localeString(key));
    }

    static Clipboard cb;

    static ActionListener pasteActionListener;
    static KeyListener keyListener;

    private static int CTRL_SHIFT =
	InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK;

    private static int KEY_MASK = CTRL_SHIFT | InputEvent.META_DOWN_MASK
	| InputEvent.ALT_DOWN_MASK | InputEvent.ALT_GRAPH_DOWN_MASK;

    private static String cwd = System.getProperty("user.dir");
    private static String origCWD = cwd;

    static void startGUI() {
	tc = new SimpleConsole();
	if (output == null) output = tc;
	frame = new JFrame (localeString("title"));
	JMenuBar menubar = new JMenuBar();
	JMenuItem menuItem;
	JMenu fileMenu = new JMenu(localeString("File"));
	fileMenu.setMnemonic(vk("VK_FILE"));
	menubar.add(fileMenu);
	menuItem = new JMenuItem(localeString("Quit"), vk("VK_QUIT"));
	menuItem.setAccelerator(KeyStroke.getKeyStroke
				(KeyEvent.VK_Q, InputEvent.CTRL_DOWN_MASK));
	menuItem.addActionListener((e1) -> {System.exit(0);});
	fileMenu.add(menuItem);
	JMenu editMenu = new JMenu(localeString("Edit"));
	editMenu.setMnemonic(vk("VK_EDIT"));
	menubar.add(editMenu);
	menuItem = new JMenuItem(localeString("PASTE"), vk("VK_PASTE"));
	menuItem.setAccelerator(KeyStroke.getKeyStroke
				(KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK));
	cb = frame.getToolkit().getSystemClipboard();
	pasteActionListener = (e2) -> {
	    try {
		Transferable t = cb.getContents(frame);
		th.importData(tc, t);
	    } catch (Exception e) {
		System.err.println(localeString("qrl") + ": "
				   + e.getMessage());
	    }
	};

	keyListener = new KeyAdapter() {
		public void keyPressed(KeyEvent e) {
		    int modifiers = e.getModifiersEx();
		    int code = e.getKeyCode();
		    if ((modifiers & KEY_MASK) == InputEvent.CTRL_DOWN_MASK) {
			if (code == KeyEvent.VK_V) {
			    pasteActionListener.actionPerformed(null);
			}
		    }
		    
		}

	    };
	menuItem.addActionListener(pasteActionListener);
	editMenu.add(menuItem);
	frame.setJMenuBar(menubar);

	Container fpane = frame.getContentPane();
        JLabel target = new
            JLabel(targetHTML);
	th = new ExtObjTransferHandler(new File(cwd)) {
		protected void clear(boolean all) {
		}
		protected void addURL(URL url) {
		    try {
			doAdd(url);
		    } catch (IOException eio) {}
		}
	    };
        frame.addWindowListener(new WindowAdapter () {
                public void windowClosing(WindowEvent e) {
                    System.exit(0);
                }
            });
	frame.setIconImages(iconList);
        frame.setSize(700,400);
        JScrollPane scrollpane = new JScrollPane(tc);
        fpane.setLayout(new BorderLayout());
        
        fpane.add("North", target);
        fpane.add("Center", scrollpane);
	setTransferHandlerRecursively(frame, th);
	for (URI fileURI: uriList) {
	    try {
		doAdd(fileURI.toURL());
	    } catch (IOException eio) {
	    }
	}
        // fpane.setVisible(true);
        frame.setVisible(true);
    }

    static void drawRect(Graphics2D g2d, int x, int y, int w, int h) {
	g2d.fillRect(x, y, w, h);
        g2d.drawRect(x, y, w, h);
    }


    static void qrEncode(String text, File cdir,
			 String iformat,
			 String pathname,
			 ErrorCorrectionLevel level,
			 int width, int height,
			 Color fgColor, Color bgColor)
	throws Exception
    {
	Map<EncodeHintType,ErrorCorrectionLevel> map =  new HashMap<>();
	map.put(EncodeHintType.ERROR_CORRECTION, level);

	int ind = pathname.lastIndexOf('.');
	if (iformat == null) {
	    iformat = OutputStreamGraphics.getImageTypeForFile(pathname);
	}
	if (iformat != null) {
	    QRCode code = Encoder.encode(text, level, map);
	    File f = new File(pathname);
	    OutputStream os = pathname.equals("-")? System.out:
		new FileOutputStream(f.isAbsolute()? f:
				     new File(cdir, pathname));
	    OutputStreamGraphics osg = OutputStreamGraphics
		.newInstance(os, width, height, iformat, true);
	    Graphics2D g2d = osg.createGraphics();
	    if (bgColor != null && bgColor.getAlpha() != 0) {
		g2d.setColor(bgColor);
		drawRect(g2d, 0, 0, width, height);
	    }
	    g2d.setColor(fgColor);
	    g2d.setStroke(new BasicStroke(0.5F));
	    // Set up by copying from QRCodeWriter in zxing
	    ByteMatrix input = code.getMatrix();
	    int inputWidth = input.getWidth();
	    int inputHeight = input.getHeight();
	    int qrWidth = inputWidth + (QUIET_ZONE_SIZE * 2);
	    int qrHeight = inputHeight + (QUIET_ZONE_SIZE * 2);
	    int outputWidth = Math.max(width, qrWidth);
	    int outputHeight = Math.max(height, qrHeight);
	    int multiple = Math.min(outputWidth / qrWidth,
				    outputHeight / qrHeight);
	    int leftPadding = (outputWidth - (inputWidth * multiple)) / 2;
	    int topPadding = (outputHeight - (inputHeight * multiple)) / 2;

	    for (int inputY = 0, outputY = topPadding;
		 inputY < inputHeight; inputY++, outputY += multiple) {
		// Write the contents of this row of the barcode
		for (int inputX = 0, outputX = leftPadding;
		     inputX < inputWidth; inputX++, outputX += multiple) {
		    if (input.get(inputX, inputY) == 1) {
			drawRect(g2d, outputX, outputY, multiple, multiple);
		    }
		}
	    }
	    osg.flush();
	    osg.imageComplete();
	} else {
	    throw new IOException(localeString("missingSuffix"));
	}
    }

    static class ConfigEditor extends ConfigPropertyEditor {
	private String ifmt = "png";
	private String[] currentImageSuffixes =
	    OutputStreamGraphics.getSuffixesForImageType(ifmt);

	private FileFilter getFileFilter() {
	    return  new FileFilter() {
		    public boolean accept (File f) {
			String path = f.getPath();
			int ind = path.lastIndexOf('.');
			if (ind == -1) return false;
			path = path.substring(ind+1);
			for (String suffix: currentImageSuffixes) {
			    if (path.equals(suffix)) return true;
			}
			return false;
		    }
		    public String getDescription() {
			return String.format(localeString("imageFilesFor"),
					     ifmt);
		    }
		};
	}
	private FileFilter filter = getFileFilter();

	public ConfigEditor() {
	    super();
	    addIcon(QRLauncher.class, "QRLConf16.png");
	    addIcon(QRLauncher.class, "QRLConf24.png");
	    addIcon(QRLauncher.class, "QRLConf32.png");
	    addIcon(QRLauncher.class, "QRLConf48.png");
	    addIcon(QRLauncher.class, "QRLConf64.png");
	    addIcon(QRLauncher.class, "QRLConf96.png");
	    addIcon(QRLauncher.class, "QRLConf128.png");
	    addIcon(QRLauncher.class, "QRLConf256.png");
	    addAltReservedKeys("Input", "uri", "file");
	    addReservedKeys("Width", "Height", "Foreground.color",
			    "Background.color", "ErrorCorrection.level");
	    addReservedKeys("QRCode.imageFormat", "QRCode.file");

	    setDefaultProperty("ErrorCorrection.level", "L");
	    setDefaultProperty("QRCode.imageFormat", "png");
	    setupCompleted();
	    setInitialExtraRows(0);
	    freezeRows();
	    addRE("color",
		  new CSSTableCellRenderer(false),
		  new CSSCellEditor());
	    addRE("file", null, new FileNameCellEditor("QR Code File", true));

	    JComboBox<String>comboBox = new JComboBox<>(new String[] {
		    "L", "M", "Q", "H"
	    });
	    addRE("level", null, new DefaultCellEditor(comboBox));

	    String[] ifmts = OutputStreamGraphics.getAllImageTypes();
	    Arrays.sort(ifmts);
	    
	    JComboBox<String> icomboBox = new JComboBox<>(ifmts);
	    addRE("imageFormat", null, new DefaultCellEditor(icomboBox));

	    FileNameCellEditor outputFileEditor =
		new FileNameCellEditor("QR Code File", true) {
		    public Object getCellEditorValue() {
			Object val = super.getCellEditorValue();
			if (val == null) return null;
			if (val instanceof String) {
			    String value = (String)val;
			    int ind = value.lastIndexOf('.');
			    boolean addSuffix = false;
			    if (ind >= 0) {
				String s = value.substring(ind+1);
				for (String ext: currentImageSuffixes) {
				    if (s.equals(ext)) return val;
				}
			    }
			    String suffix = OutputStreamGraphics
				.getSuffixForImageType(ifmt);
			    return value + "." + suffix;
			}
			return val;
		    }
		};
	    outputFileEditor.addChoosableFileFilter(filter);
	    outputFileEditor.setFileFilter(filter);

	    addRE("QRCode.file", null, outputFileEditor);
	    changedPropertyClears("QRCode.imageFormat", "QRCode.file");

	    monitorProperty("QRCode.imageFormat");
	    addConfigPropertyListener((e) -> {
		    String key = e.getProperty();
		    String fmt = e.getValue();
		    if (key.equals("QRCode.imageFormat")) {
			ifmt = fmt;
			currentImageSuffixes = OutputStreamGraphics
			    .getSuffixesForImageType(fmt);
			outputFileEditor.removeChoosableFileFilter(filter);
			filter = getFileFilter();
			outputFileEditor.addChoosableFileFilter(filter);
			outputFileEditor.setFileFilter(filter);
		    }
		});
	}

	public String errorTitle() {return "QRLauncher Error";}
	public String configTitle() {return "QRLauncher QRCode Configuration";}
	public String mediaType() {
	    return "application/vnd.bzdev.qrlauncher-config";
	}
	public String extensionFilterTitle() {return "QRLauncher Files";}
	public String extension() {return "qrl";}
    }

    static ConfigEditor editor;
    static void config(File cdir, File props)
	throws IOException, InterruptedException, InvocationTargetException
    {
	if (cdir == null)  {
	    cdir = new File (cwd);
	} else {
	    System.setProperty("user.dir", cdir.getCanonicalPath());
	}
	SwingUtilities.invokeAndWait(() -> {
		DarkmodeMonitor.setSystemPLAF();
		DarkmodeMonitor.init();
		editor = new ConfigEditor();
	    });
	if (props != null) {
	    editor.loadFile(props);
	}
	editor.edit(null, ConfigPropertyEditor.Mode.MODAL, null,
		    ConfigPropertyEditor.CloseMode.BOTH);
	Properties results = editor.getDecodedProperties();
	String width = results.getProperty("Width", "100");
	String height = results.getProperty("Height", "100");
	String fgColor = results.getProperty("Foreground.color", "black");
	String bgColor = results.getProperty("Background.color", "white");
	String level = results.getProperty("ErrorCorrection.level", "L");
	String imageFormat = results.getProperty("QRCode.imageFormat",
						 "png");
	String output = results.getProperty("QRCode.file");
	if (output == null) {
	    output = JOptionPane.showInputDialog(null,
						 localeString("outputRequest"),
						 localeString("title"),
						 JOptionPane.QUESTION_MESSAGE);
	    if (output == null || output.trim().length() == 0) System.exit(1);
	}
	String[] suffixes = OutputStreamGraphics
	    .getSuffixesForImageType(imageFormat);
	boolean addSuffix = true;
	for (String ext: suffixes) {
	    if (output.endsWith("." + ext)) {
		addSuffix = false;
		break;
	    }
	}
	if (addSuffix && suffixes.length > 0) {
	    output = output + "."
		+ OutputStreamGraphics.getSuffixForImageType(imageFormat);
	}

	String uri = results.getProperty("Input.uri");
	String ifile = results.getProperty("Input.file");
	if (uri == null && ifile == null) {
	    uri = JOptionPane.showInputDialog(null,
					      localeString("uriRequest"),
					      localeString("title"),
					      JOptionPane.QUESTION_MESSAGE);
	    if (uri == null || uri.trim().length() == 0) System.exit(1);
	    
	}
	try {
	    ProcessBuilder pb = new ProcessBuilder
		(System.getProperty("qrl.cmd"),
		 ((uri == null)? "-i": "-u"),
		 ((uri == null)? ifile: uri),
		 "-w", width,
		 "-h", height,
		 "-F", fgColor,
		 "-B", bgColor,
		 "-L", level,
		 "-o", output);
	    pb.inheritIO();
	    Process p = pb.start();
	    System.exit(p.waitFor());
	} catch (Exception e) {
	    System.err.println("qrl: " + e.getMessage());
	    System.exit(1);
	}
    }


    public static void main(String argv[]) throws Exception {
	int index = 0;
	int width = 100;
	int height = 100;
	String uri = null;
	String ipath = null;
	String path = null;
	boolean trim = false;
	String iformat = null;
	ErrorCorrectionLevel level = ErrorCorrectionLevel.L;
	Color fgColor = Color.BLACK;
	Color bgColor = Color.WHITE;
	while (index < argv.length) {
	    if (argv[index].equals("--")) {
		index++;
		break;
	    } else if (argv[index].equals("-p")) {
		output = System.out;
	    } else if (argv[index].equals("-n")) {
		launch = false;
	    } else if (argv[index].equals("-d")) {
		index++;
		if (index < argv.length) {
		    cwd = argv[index];
		} else {
		    System.err.println(localeString("qrl") +": "
				       + localeString("missingCWD"));
		    System.exit(1);
		}
	    } else if (argv[index].equals("-e")) {
		exitMode = true;
	    } else if (argv[index].equals("-o")) {
		index++;
		if (index >= argv.length) {
		    System.err.println(localeString("qrl") +": "
				       + localeString("missingOutput"));
		    System.exit(1);
		}
		path = argv[index];
	    } else if (argv[index].equals("-u")) {
		index++;
		if (index >= argv.length) {
		    System.err.println(localeString("qrl") +": "
				       + localeString("missingInput"));
		    System.exit(1);
		}
		uri = argv[index];
		try {
		    new URI(uri);
		} catch (URISyntaxException e) {
		    System.err.println(localeString("qrl") + ":"
				       + localeString("badURI" + " - ")
				       + uri);
		    System.exit(1);
		}
	    } else if (argv[index].equals("-i")) {
		index++;
		if (index >= argv.length) {
		    System.err.println(localeString("qrl") +": "
				       + localeString("missingInput"));
		    System.exit(1);
		}
		ipath = argv[index];
		// with no -i option, STDIN is assumed.
		if (ipath.equals("-")) ipath = null;
	    } else if (argv[index].equals("-w")) {
		index++;
		if (index >= argv.length) {
		    System.err.println(localeString("qrl") +": "
				       + localeString("missingWidth"));
		    System.exit(1);
		}
		try {
		    width = Integer.parseInt(argv[index]);
		    if (width <= 0) {
			System.err.println(localeString("qrl") +": "
					   + localeString("missingWidth"));
			System.exit(1);
		    }
		} catch (NumberFormatException nfe) {
			System.err.println(localeString("qrl") +": "
					   + localeString("missingWidth"));
			System.exit(1);
		}
	    } else if (argv[index].equals("-h")) {
		index++;
		if (index >= argv.length) {
		    System.err.println(localeString("qrl") +": "
				       + localeString("missingHeight"));
		    System.exit(1);
		}
		try {
		    height = Integer.parseInt(argv[index]);
		    if (height <= 0) {
			System.err.println(localeString("qrl") +": "
					   + localeString("missingHeight"));
			System.exit(1);
		    }
		} catch (NumberFormatException nfe) {
		    System.err.println(localeString("qrl") +": "
				       + localeString("missingHeight"));
		    System.exit(1);
		}
	    } else if (argv[index].equals("-L")) {
		index++;
		if (index >= argv.length) {
		    System.err.println(localeString("qrl") +": "
				       + localeString("missingLevel"));
		    System.exit(1);
		}
		String arg = argv[index];
		if (arg.length() != 1) {
		    System.err.println(localeString("qrl") +": "
				       + localeString("missingLevel"));
		    System.exit(1);
		}
		char lch = arg.charAt(0);
		switch(lch) {
		case 'L':
		    level = ErrorCorrectionLevel.L;
		    break;
		case 'M':
		    level = ErrorCorrectionLevel.M;
		    break;
		case 'Q':
		    level = ErrorCorrectionLevel.Q;
		    break;
		case 'H':
		    level = ErrorCorrectionLevel.H;
		    break;
		default:
		    System.err.println(localeString("qrl") +": "
				       + localeString("missingLevel"));
		    System.exit(1);
		}
	    } else if (argv[index].equals("-f")) {
		index++;
		if (index >= argv.length) {
		    System.err.println(localeString("qrl") +": "
				       + localeString("missingFormat"));
		    System.exit(1);
		}
		iformat = argv[index];
	    } else if (argv[index].equals("-B")) {
		index++;
		if (index >= argv.length) {
		    System.err.println(localeString("qrl") +": "
				       + localeString("missingBGColor"));
		    System.exit(1);
		}
		try {
		    bgColor = Colors.getColorByCSS(argv[index]);
		} catch (IllegalArgumentException iae) {
		    System.err.println(localeString("qrl") +": "
				       + localeString("illformedBGColor"));
		    System.exit(1);
		}
	    } else if (argv[index].equals("-F")) {
		index++;
		if (index >= argv.length) {
		    System.err.println(localeString("qrl") +": "
				       + localeString("missingFGColor"));
		    System.exit(1);
		}
		try {
		    fgColor = Colors.getColorByCSS(argv[index]);
		} catch (IllegalArgumentException iae) {
		    System.err.println(localeString("qrl") +": "
				       + localeString("illformedFGColor"));
		    System.exit(1);
		}
	    } else if (argv[index].equals("-t")) {
		trim = true;
	    } else if (argv[index].equals("--colors")) {
		System.out.println("transparent");
		for (String color: Colors.namedCSSColors()) {
		    System.out.println(color);
		}
		System.exit(0);
	    } else if (argv[index].equals("--formats")) {
		for (String fmt: OutputStreamGraphics.getImageTypes()) {
		    System.out.println(fmt);
		}
		System.exit(0);
	    } else if (argv[index].equals("--format-aliases")) {
		for (String fmt: OutputStreamGraphics.getAllImageTypes()) {
		    System.out.println(fmt);
		}
		System.exit(0);
	    } else if (argv[index].equals("-g")) {
		    config(new File(cwd), null);
	    } else if (argv[index].startsWith("-")) {
		    System.err.println(localeString("qrl") +" - "
				       + localeString("unrecognizedOption")
				       + ": " + argv[index]);
		    System.exit(1);
	    } else {
		break;
	    }
	    index++;
	}
	if (ipath != null && uri != null) {
	    System.err.println(localeString("qrl") + ": "
			       + localeString("uriAndPath"));
	}
	File cdir = new File(cwd);
	if (path != null) {
	    try {
		String text;
		if (uri != null) {
		    text = uri;
		} else {
		    InputStream is = System.in;
		    if (ipath != null) {
			File f = new File(ipath);
			is = new FileInputStream(f.isAbsolute()? f:
						 new File(cdir, ipath));
		    }
		    InputStreamReader rd = new InputStreamReader(is, "UTF-8");
		    StringBuffer sb = new StringBuffer();
		    AppendableWriter w = new AppendableWriter(sb);
		    rd.transferTo(w);
		    w.flush();
		    text = sb.toString();
		    if (trim) text = text.trim();
		}
		qrEncode(text, cdir, iformat, path, level, width, height,
			 fgColor, bgColor);
	    } catch (Exception ex) {
		String msg = ex.getMessage();
		if (msg == null || msg.trim().length() == 0) {
		    msg = "" + ex.getClass().getName();
		}
		System.err.println(localeString("qrl") +": " + msg);
		System.exit(1);
	    }
	    System.exit(0);
	}

	if (index  < argv.length) {
	    String arg = argv[index];
	    if (arg.equals("-")) {
		// Set args by reading from stdin
		LineNumberReader reader =
		    new LineNumberReader(new InputStreamReader(System.in,
							       "UTF-8"));
		// String uri;
		ArrayList<String> list = new ArrayList<>();
		int count = 0;
		while ((uri = reader.readLine()) != null) {
		    uri = uri.trim();
		    if (uri.length() != 0) {
			list.add(uri);
			count++;
		    }
		}
		argv = new String[count];
		argv = list.toArray(argv);
		index = 0;
	    } else if (arg.endsWith(".qrl")) {
		config(new File(cwd), new File(arg));
		System.exit(0);
	    }
	}

	while (index < argv.length) {
	    String arg = argv[index];
	    if (arg.startsWith("http:") || arg.startsWith("https:")
		|| arg.startsWith("ftp:")) {
		try {
		    uriList.add(new URI(arg));
		} catch (URISyntaxException eurl) {
		    System.err.println(localeString("badURI"));
		}
	    } else if (arg.startsWith("file:")) {
		try {
		    if (arg.startsWith("file:/")) {
			uriList.add(new URI(arg));
		    } else {
			arg = new URI(arg).toString().substring(5);
			uriList.add(cdir.toURI().resolve(arg));
		    }
		} catch (URISyntaxException eurl) {
		    System.err.println(localeString("badURI"));
		}
	    } else {
		File f = new File(arg);
		uriList.add((f.isAbsolute()? f: new File (cwd, arg)).toURI());
	    }
	    index++;
	}
	if (exitMode) {
	    if (launch == false) output = System.out;
	    for (URI u: uriList) {
		doAdd(u.toURL());
	    }
	    System.exit(0);
	}

        DarkmodeMonitor.setSystemPLAF();
        DarkmodeMonitor.init();
	Handlers.enable();
        SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                try {
                    iconList.add((new
                                  ImageIcon((QRLauncher.class
                                             .getResource("QRLauncher16.png")
                                             ))).getImage());
                    iconList.add((new
                                  ImageIcon((QRLauncher.class
                                             .getResource("QRLauncher24.png")
                                             ))).getImage());
                    iconList.add((new
                                  ImageIcon((QRLauncher.class
                                             .getResource("QRLauncher32.png")
                                             ))).getImage());
                    iconList.add((new
                                  ImageIcon((QRLauncher.class
                                             .getResource("QRLauncher48.png")
                                             ))).getImage());
                    iconList.add((new
                                  ImageIcon((QRLauncher.class
                                             .getResource("QRLauncher64.png")
                                             ))).getImage());
                    iconList.add((new
                                  ImageIcon((QRLauncher.class
                                             .getResource("QRLauncher96.png")
                                             ))).getImage());
                    iconList.add((new
                                  ImageIcon((QRLauncher.class
                                             .getResource("QRLauncher128.png")
                                             ))).getImage());
                    iconList.add((new
                                  ImageIcon((QRLauncher.class
                                             .getResource("QRLauncher256.png")
                                             ))).getImage());
                } catch (Exception e) {
                    System.err.println("initialization failed - "
                                       + "missing icon for iconList");
		}
                    startGUI();
                }
            });
    }
}
