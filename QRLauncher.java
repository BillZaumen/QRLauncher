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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.swing.ImageIcon;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import java.util.LinkedList;
import java.util.ResourceBundle;
import org.bzdev.io.AppendableWriter;
import org.bzdev.swing.DarkmodeMonitor;
import org.bzdev.swing.SimpleConsole;
import org.bzdev.swing.ExtObjTransferHandler;
import org.bzdev.protocols.Handlers;

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

    static void qrEncode(String text, File cdir, String pathname,
			 ErrorCorrectionLevel level,
			 int width, int height)
	throws Exception
    {
	Map<EncodeHintType,ErrorCorrectionLevel> map =  new HashMap<>();
	map.put(EncodeHintType.ERROR_CORRECTION, level);
	Charset cs = Charset.forName("UTF-8");
	// they suggested new String(data.getBytes(charset), charset)
	// which should be equal to data.
	BitMatrix bm = new MultiFormatWriter().encode
	    (text, BarcodeFormat.QR_CODE, width, height);
	int ind = pathname.lastIndexOf('.');
	if (ind != -1) {
	    String suffix = pathname.substring(ind + 1);
	    MatrixToImageWriter.writeToFile(bm, suffix,
					    new File(cdir, pathname));
	} else {
	    throw new IOException("missing suffix");
	}
    }


    public static void main(String argv[]) throws Exception {
	int index = 0;
	int width = 100;
	int height = 100;
	String ipath = null;
	String path = null;
	boolean trim = false;
	ErrorCorrectionLevel level = ErrorCorrectionLevel.L;
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
	    } else if (argv[index].equals("-i")) {
		index++;
		if (index >= argv.length) {
		    System.err.println(localeString("qrl") +": "
				       + localeString("missingInput"));
		    System.exit(1);
		}
		ipath = argv[index];
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
	    } else if (argv[index].equals("-t")) {
		trim = true;
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
	File cdir = new File(cwd);
	if (path != null) {
	    InputStream is = System.in;
	    try {
		if (ipath != null) {
		    is = new FileInputStream(new File(cdir, ipath));
		}
		InputStreamReader rd = new InputStreamReader(is, "UTF-8");
		StringBuffer sb = new StringBuffer();
		AppendableWriter w = new AppendableWriter(sb);
		rd.transferTo(w);
		w.flush();
		String text = sb.toString();
		if (trim) text = text.trim();
		qrEncode(text, cdir, path, level, width, height);
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
		String uri;
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
		uriList.add((new File (cwd, arg)).toURI());
	    }
	    index++;
	}
	if (exitMode) {
	    if (launch == false) output = System.out;
	    for (URI uri: uriList) {
		doAdd(uri.toURL());
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
