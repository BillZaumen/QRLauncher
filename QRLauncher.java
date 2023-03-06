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
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import javax.swing.ImageIcon;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.TableCellRenderer;
import org.bzdev.gio.OutputStreamGraphics;
import org.bzdev.graphs.Colors;
import org.bzdev.io.AppendableWriter;
import org.bzdev.protocols.Handlers;
import org.bzdev.swing.CSSCellEditor;
import org.bzdev.swing.ConfigPropertyEditor;
import org.bzdev.swing.DarkmodeMonitor;
import org.bzdev.swing.ExtObjTransferHandler;
import org.bzdev.swing.FileNameCellEditor;
import org.bzdev.swing.HtmlPane;
import org.bzdev.swing.SimpleConsole;
import org.bzdev.swing.TextCellEditor;
import org.bzdev.swing.WholeNumbTextField;
import org.bzdev.swing.table.CSSTableCellRenderer;


import com.google.zxing.qrcode.encoder.Encoder;
import com.google.zxing.qrcode.encoder.QRCode;
import com.google.zxing.qrcode.encoder.ByteMatrix;

public class QRLauncher {



    private static URI STDIN_URI = null;
    static {
	try {
	    STDIN_URI = new URI("stdin:localhost");
	} catch (Exception e) {
	    System.err.println("qrl: couuld not initialize STDIN_URI");
	    System.exit(1);
	}
    }

    static private final String resourceBundleName = "QRL";
    static ResourceBundle bundle = ResourceBundle.getBundle(resourceBundleName);
    public static String localeString(String name) {
	return bundle.getString(name);
    }
    
    static java.util.List<Image> iconList = new LinkedList<Image>();
    static SimpleConsole tc = null;
    static Appendable output = null;
    static boolean launch = true;
    static boolean geth = false;
    static boolean queryMode = false;
    static boolean exitMode = false;
    static boolean contentsOnly = false;
    static boolean nub64 = false;

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

    static String decodeImage(BufferedImage bufferedImage)
	throws NotFoundException
    {
	    LuminanceSource source = new
		BufferedImageLuminanceSource(bufferedImage);
	    BinaryBitmap bitmap =
		new BinaryBitmap(new HybridBinarizer(source));
	    return (new MultiFormatReader().decode(bitmap)).getText();
    }

    static void doAdd(URL url) throws IOException {
	try {
	    if (contentsOnly == false) {
		String urlString = (url == null)? "[ stdin ]":
		    url.toString();
		if (output != null) output.append("" + urlString);
		if (output != tc && tc != null) {
		    tc.append("" + urlString);
		}
	    }
	    String contents = null;
	    InputStream is = (url == null)? System.in: url.openStream();
	    BufferedImage bufferedImage = ImageIO.read(is);
	    String uri = decodeImage(bufferedImage);
	    URI u = null;
	    boolean foundHttpURL = false;
	    try {
		u = new URI(uri);
		String scheme = u.getScheme();
		foundHttpURL = (scheme != null) &&
		    (scheme.equals("http") || scheme.equals("https"));
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
			    tst = uri.substring(6 + 6);
			    if(tst.length() > 2 &&
			       ((tst.charAt(0) == '\r' && tst.charAt(1) == '\n')
				|| tst.charAt(0) == '\n')) {
				// The people implemnting this apparently
				// can't be bothered to use VCALENDAR, so
				// we'll add a wrapper.
				//
				// In case they forgot the ending CRLF ...
				char ch = uri.charAt(uri.length() - 1);
				if (ch != '\r' && ch != '\n') {
				    uri = uri + "\r\n";
				}
				uri = "BEGIN:VCALENDAR\r\n" + uri
				    + "END:VCALENDAR\r\n";
				mode = 1;
			    }
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
		if (contentsOnly == false) {
		    File tmp = File.createTempFile("qrl-url-contents", suffix);
		    tmp.deleteOnExit();
		    PrintWriter w = new PrintWriter(tmp, "UTF-8");
		    contents = uri;
		    w.print(uri);
		    w.close();
		    u = tmp.toURI();
		    uri = "[ " + u.toString() + " ]";
		}
	    }

	    if (contentsOnly) {
		contents = uri;
	    } else {
		if (output != null) output.append(" \u2192 " + uri);
		if (output != tc && tc != null) {
		    tc.append(" \u2192 " + uri);
		}
	    }
	    if (launch) {
		if (Desktop.isDesktopSupported()) {
		    Desktop desktop = Desktop.getDesktop();
		    if (desktop.isSupported(Desktop.Action.BROWSE)) {
			if (exitMode) {
			    try {
				if (queryMode) {
				    java.io.Console console = System.console();
				    if (console != null) {
					console.format(localeString("query"),
						       u.toString(), " ");
					console.flush();
					String reply = console.readLine()
					    .trim().toLowerCase();
					if (reply.charAt(0) != 'y') {
					    return;
					}
				    }
				}
				desktop.browse(u);
			    } catch (IOException eio) {
				System.err.println(eio.getMessage());
			    }
			} else {
			    URI uu = u;
			    SwingUtilities.invokeLater(() -> {
				    try {
					if (queryMode) {
					    int status = JOptionPane
						.showConfirmDialog
						(frame,
						 String.format
						 (localeString("queryMsg"),
						  uu.toString()),
						 localeString("queryTitle"),
						 JOptionPane.OK_CANCEL_OPTION);
					    if (status !=
						JOptionPane.OK_OPTION) {
						return;
					    }
					}
					desktop.browse(uu);
				    } catch (IOException eio) {
					System.err.println(eio.getMessage());
				    }
				});
			}
		    }
		}
	    } else if (contents != null) {
		if (contentsOnly == false) {
		    contents = " ...\n" + contents + "\n------------";
		} else if (nub64 && contentsOnly) {
		    if (contents.length() %2 != 0) {
			throw new IOException(localeString("notNUB64"));
		    }
		    int len = contents.length() / 2;
		    char[] carray = new char[len];
		    for (int i = 0; i < len; i++) {
			int j = 2*i;
			int val = 45 + (contents.charAt(j) - '0')*10
			    + (contents.charAt(j+1) - '0');
			carray[i] = (char) val;
		    }
		    contents = new String(carray);
		    Base64.Decoder decoder = Base64.getUrlDecoder();
		    byte[] results = decoder.decode(contents);
		    System.out.write(results);
		    System.out.flush();
		} else {
		    if (output != null) output.append(contents);
		    if (output != tc && tc != null) {
			tc.append(contents);
		    }
		}
	    } else if (geth && foundHttpURL) {
		String gethPath =
		    System.getProperty("qrl.cmd").replace("qrl","gethdrs");
		File gethFile = new File(gethPath);
		if (!gethFile.canExecute()) {
		    gethPath = gethPath.replace("qrlauncher","gethdrs");
		    gethFile = new File (gethPath);
		}
		if (gethFile.canExecute()) {
		    ProcessBuilder pb = new ProcessBuilder(gethPath, "-r",
							   u.toString());
		    pb.redirectErrorStream(true);
		    Process p = pb.start();
		    InputStreamReader r = new
			InputStreamReader(p.getInputStream());
		    output.append('\n');
		    r.transferTo(new AppendableWriter(output));
		    int status = p.waitFor();
		} else {
		    output.append("\n");
		    output.append(String.format(localeString("noExec"),
						gethPath));
		}
	    }
	} catch (Exception e) {
	    String msg = e.getMessage();
	    if (contentsOnly == false) {
		if (output != null) {
		    output.append (" " + localeString("FAILED")
				   + (msg == null? "": " - " + msg));
		}
		if (output != tc && tc != null) {
		    tc.append (" " + localeString("FAILED")
			       + (msg == null? "": " - " + msg));
		}
	    } else {
		System.err.println(localeString("qrl") + ": FAILED"
				   + (msg == null? "": " - " + msg));
		System.exit(1);
	    }
	}
	if (contentsOnly == false) {
	    if (output != null) output.append("\n");
	    if (output != tc && tc != null) {
		tc.append("\n");
	    }
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
			if (url != null) {
			    doAdd(url);
			}
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

    // tests indicate that this seems to work with MIN_MULT = 8
    private static int MIN_MULT = /*16*/8;


    static void encodeImage(Graphics2D g2d, ByteMatrix input,
			    int inputWidth, int inputHeight,
			    int outputWidth, int outputHeight,
			    int leftPadding, int topPadding,
			    Color bgColor, Color fgColor,
			    int multiple)
    {
	if (bgColor != null && bgColor.getAlpha() != 0) {
	    g2d.setColor(bgColor);
	    drawRect(g2d, 0, 0, outputWidth, outputHeight);
	}
	g2d.setColor(fgColor);
	for (int inputY = 0, outputY = topPadding;
	     inputY < inputHeight; inputY++, outputY += multiple) {
	    // Write the contents of this row of the barcode
	    int n = 1;
	    for (int inputX = 0, outputX = leftPadding;
		 inputX < inputWidth; /*inputX++,*/ outputX += n*multiple) {
		if (input.get(inputX, inputY) == 1) {
		    n = 0;
		    while (inputX < inputWidth
			   && input.get(inputX, inputY) == 1) {
			n++;
			inputX++;
		    }
		    drawRect(g2d, outputX, outputY, n*multiple, multiple);
		} else {
		    n = 1;
		    inputX++;
		}
	    }
	}
    }


    static void qrEncode(String text, File cdir,
			 String iformat,
			 String pathname,
			 ErrorCorrectionLevel level,
			 int width, int height,
			 Color fgColor, Color bgColor,
			 String label, int fontSize,
			 int minMultiple)
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
	    // Set up by copying from QRCodeWriter in zxing
	    ByteMatrix input = code.getMatrix();
	    int inputWidth = input.getWidth();
	    int inputHeight = input.getHeight();
	    int qrWidth = inputWidth + (QUIET_ZONE_SIZE * 2);
	    int qrHeight = inputHeight + (QUIET_ZONE_SIZE * 2);
	    int outputWidth, outputHeight, multiple;
	    int leftPadding, topPadding;
	    boolean loop = (minMultiple == 0);
	    boolean zeroWidth = (width == 0);
	    boolean zeroHeight = (height == 0);
	    if (loop) minMultiple = 2;
	    do {
		if (zeroWidth) width = inputWidth*minMultiple;
		if (zeroHeight) height = inputHeight*minMultiple;
		outputWidth = Math.max(width, qrWidth);
		outputHeight = Math.max(height, qrHeight);
		multiple = Math.min(outputWidth / qrWidth,
				    outputHeight / qrHeight);

		if (multiple < minMultiple) {
		    int m1 = outputWidth /qrWidth;
		    int m2 = outputHeight /qrHeight;
		    if (m1 < minMultiple) {
			outputWidth += (minMultiple-m1)*qrWidth;
		    }
		    if (m2 < minMultiple) {
			outputHeight += (minMultiple-m2)*qrHeight;
		    }
		    multiple = Math.min(outputWidth / qrWidth,
					outputHeight / qrHeight);
		}
		leftPadding = (outputWidth - (inputWidth * multiple)) / 2;
		topPadding = (outputHeight - (inputHeight * multiple)) / 2;
		if (loop) {
		    BufferedImage bi = new BufferedImage(outputWidth,
							 outputHeight,
							 BufferedImage
							 .TYPE_INT_ARGB);
		    Graphics2D tg2d = bi.createGraphics();
		    encodeImage(tg2d, input, inputWidth, inputHeight,
				outputWidth, outputHeight,
				leftPadding, topPadding,
				bgColor, fgColor, multiple);
		    tg2d.dispose();
		    try {
			decodeImage(bi);
			loop = false;
		    } catch (Exception e) {
			minMultiple++;
			if (minMultiple > 16) throw e;
		    }
		}
	    } while (loop);

	    if (label != null) {
		if (fontSize == 0) {
		    int lastSize = 12;
		    BufferedImage bi = new BufferedImage(outputWidth,
							 outputHeight + 24,
							 BufferedImage
							 .TYPE_INT_ARGB);
		    Graphics2D tg2d = bi.createGraphics();
		    double lw = 0.0;
		    double lh = 0.0;
		    int maxLabelWidth = outputWidth - 2 * leftPadding;
		    for(;;) {
			Font font = new Font(Font.SANS_SERIF, Font.BOLD,
					     fontSize);
			tg2d.setFont(font);
			FontRenderContext frc = tg2d.getFontRenderContext();
			Rectangle2D bounds = font.getStringBounds(label, frc);
			if (bounds.getHeight() > 0.1 *  outputHeight
			    || bounds.getWidth() > maxLabelWidth) {
			    fontSize = lastSize;
			    break;
			}
			lastSize = fontSize;
			fontSize++;
		    }
		    tg2d.dispose();
		}
		if (topPadding < 2*fontSize) {
		    topPadding += 2*fontSize;
		    outputHeight += 4*fontSize;
		}
	    }

	    OutputStreamGraphics osg = OutputStreamGraphics
		.newInstance(os, outputWidth, outputHeight, iformat, true);
	    Graphics2D g2d = osg.createGraphics();
	    encodeImage(g2d, input, inputWidth, inputHeight,
			outputWidth, outputHeight,
			leftPadding, topPadding,
			bgColor, fgColor,
			multiple);
	    g2d.setStroke(new BasicStroke(0.5F));
	    int textX = -1;
	    int textY = -1;
	    if (label != null) {
		Font font= new Font(Font.SANS_SERIF, Font.BOLD, fontSize);
		g2d.setFont(font);
		FontRenderContext frc = g2d.getFontRenderContext();
		Rectangle2D bounds = font.getStringBounds(label, frc);
		LineMetrics lm = font.getLineMetrics(label, frc);
		textX = outputWidth/2 - (int)Math.round(bounds.getWidth()/2);
		textY = outputHeight - topPadding + fontSize/2
		    + Math.round(lm.getAscent());
		g2d.drawString(label, textX, textY);
	    }
	    osg.flush();
	    osg.imageComplete();
	    g2d.dispose();
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

	private boolean hasHelpWindow = false;
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
	    addReservedKeys("Label", "FontSize", "Binary", "multiple.min");

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

	    /*
	    JComboBox<String> binaryCB = new JComboBox<>(new String[] {
		    "true", "false"
		});
	    addRE("Binary", null, new DefaultCellEditor(binaryCB));
	    */
	    addRE("Binary",
		  new TableCellRenderer() {
		      private JCheckBox cb = new JCheckBox();
		      public Component getTableCellRendererComponent
			  (JTable table, Object value, boolean isSelected,
			   boolean hasFocus, int row, int col) {
			  if (value == null) {
			      cb.setSelected(false);
			  } else if (value instanceof String) {
			      String val = (String)value;
			      val = val.trim();
			      cb.setSelected(val.equalsIgnoreCase("true"));
			  }
			  return cb;
		      }
		  },
		  new DefaultCellEditor(new JCheckBox()) {
		      public Object getCellEditorValue() {
			  Object object = super.getCellEditorValue();
			  if (object.equals(Boolean.TRUE)) {
			      return "true";
			  } else {
			      return "false";
			  }
		      }
		  });

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

	    TextCellEditor tce = new
		TextCellEditor<String>(String.class,
					new WholeNumbTextField());
	    addRE("Width", null, tce);
	    addRE("Height", null, tce);
	    addRE("FontSize", null, tce);
	    addRE("multiple.min", null, tce);

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
	    JMenuItem helpMenuItem = new JMenuItem(localeString("help"));
	    helpMenuItem.setEnabled(true);
	    helpMenuItem.addActionListener((ae) -> {
		    if (hasHelpWindow) return;
		    try {
			ProcessBuilder pb = new ProcessBuilder
			    (System.getProperty("qrl.cmd"), "--help");
			pb.inheritIO();
			Process p = pb.start();
			hasHelpWindow = true;
			helpMenuItem.setEnabled(false);
			new Thread(() -> {
				try {
				    p.waitFor();
				} catch(Exception ie) {}
				SwingUtilities.invokeLater(()->{
				    hasHelpWindow = false;
				    helpMenuItem.setEnabled(true);
				    });

			}).start();
		    } catch (Exception e) {
			System.err.println("qrl: " + e.getMessage());
			System.exit(1);
		    }
		});
	    setHelpMenuItem(helpMenuItem);
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
	String width = results.getProperty("Width", "0");
	String height = results.getProperty("Height", "0");
	String fgColor = results.getProperty("Foreground.color", "black");
	String bgColor = results.getProperty("Background.color", "white");
	String level = results.getProperty("ErrorCorrection.level", "L");
	String label = results.getProperty("Label");
	String fontSize = results.getProperty("FontSize");
	String imageFormat = results.getProperty("QRCode.imageFormat",
						 "png");
	String output = results.getProperty("QRCode.file");
	boolean binary = results.getProperty("Binary", "false")
	    .equalsIgnoreCase("true");
	String minmult = results.getProperty("multiple.min", "0");
	if (minmult.equals("0")) {
	    minmult = null;
	}
	
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
	    LinkedList<String> args = new LinkedList<>();
	    String[] iargs = {
		System.getProperty("qrl.cmd"),
		((uri == null)? "-i": "-u"),
		((uri == null)? ifile: uri),
		"-w", width,
		"-h", height,
		"-F", fgColor,
		"-B", bgColor,
		"-L", level,
		"-o", output};
	    for (String s: iargs) {
		args.add(s);
	    }
	    if (label != null && label.trim().length() > 0) {
		args.add("--label");
		args.add(label);
		if (fontSize != null) {
		    fontSize = fontSize.trim();
		    while (fontSize.startsWith("0")) {
			fontSize = fontSize.substring(1);
		    }
		    if(fontSize.length() > 0) {
			args.add("--fontSize");
			args.add(fontSize);
		    }
		}
	    }
	    if (binary) {
		args.add("--binary");
	    }
	    if (minmult != null) {
		args.add ("-m");
		args.add(minmult);
	    }

	    ProcessBuilder pb = new ProcessBuilder(args);
	    pb.inheritIO();
	    Process p = pb.start();
	    System.exit(p.waitFor());
	} catch (Exception e) {
	    System.err.println("qrl: " + e.getMessage());
	    System.exit(1);
	}
    }

    private static  void showHelp() {
	Handlers.enable();
	SwingUtilities.invokeLater(() -> {
		DarkmodeMonitor.setSystemPLAF();
		DarkmodeMonitor.init();
	    });
	SwingUtilities.invokeLater(() -> {
		try {
		    URL url = new URL(localeString("helpURL"));
		    HtmlPane htmlPane= new HtmlPane(url);
		    htmlPane.setSize(800, 600);
		    JFrame frame = new JFrame(localeString("help"));
		    Container hpane = frame.getContentPane();
		    hpane.setLayout(new BorderLayout());
		    frame.addWindowListener(new WindowAdapter () {
			    public void windowClosing(WindowEvent e) {
				System.exit(0);
			    }
			});
		    hpane.add(htmlPane, "Center");
		    frame.setSize(800,600);
		    frame.setVisible(true);
		} catch (Exception e) {
		    e.printStackTrace();
		}
	    });
    }


    public static void main(String argv[]) throws Exception {
	int index = 0;
	int width = 0;
	int height = 0;
	int minMult = 0;
	String label = null;
	int fontSize = 0;
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
		queryMode = false;
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
	    } else if (argv[index].equals("-m")) {
		index++;
		if (index >= argv.length) {
		    System.err.println(localeString("qrl") +": "
				       + localeString("missingMult"));
		    System.exit(1);
		}
		try {
		    minMult = Integer.parseInt(argv[index]);
		    if (minMult < 0) {
			System.err.println(localeString("qrl") +": "
					   + localeString("missingMult"));
			System.exit(1);
		    }
		} catch (NumberFormatException nfe) {
			System.err.println(localeString("qrl") +": "
					   + localeString("missingWidth"));
			System.exit(1);
		}
	    } else if (argv[index].equals("--label")) {
		index++;
		if (index >= argv.length) {
		    System.err.println(localeString("qrl") +": "
				       + localeString("missingLabel"));
		    System.exit(1);
		}
		label = argv[index].trim();
		if (label.length() == 0) label = null;
	    } else if (argv[index].equals("--fontSize")) {
		index++;
		if (index >= argv.length) {
		    System.err.println(localeString("qrl") +": "
				       + localeString("missingfontSize"));
		    System.exit(1);
		}
		try {
		    fontSize = Integer.parseInt(argv[index]);
		    if (fontSize < 0) {
			System.err.println(localeString("qrl") +": "
					   + localeString("illegalFontSize"));
			System.exit(1);
		    }
		} catch (NumberFormatException nfe) {
			System.err.println(localeString("qrl") +": "
					   + localeString("illegalFontSize"));
			System.exit(1);
		}
	    } else if (argv[index].equals("--binary")) {
		nub64 = true;
	    } else if (argv[index].equals("--help")) {
		showHelp();
		// main should exit because showHelp() opens a
		// window.
		return;
	    } else if (argv[index].equals("-H")) {
		geth = true;
		launch = false;
	    } else if (argv[index].equals("-q")) {
		queryMode = true;
		launch = true;
	    } else if (argv[index].equals("-C")) {
		contentsOnly = true;
		queryMode = false;
		launch = false;
		geth = false;
		output = System.out;
		index++;
		break;
	    } else if (argv[index].equals("-")) {
		// assume an '-' alone denotes standard input and is
		// therefore a filename/url argument.
		break;
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
	if (nub64 && contentsOnly == false && path == null) {
	    nub64 = false;
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
		    if (nub64) {
			ByteArrayOutputStream baos =
			    new ByteArrayOutputStream(4096);
			is.transferTo(baos);
			byte[] array = baos.toByteArray();
			Base64.Encoder encoder = Base64.getUrlEncoder()
			    .withoutPadding();
			byte[] encoded = encoder.encode(array);
			array = new byte[2*encoded.length];
			for (int i = 0; i < encoded.length; i++) {
			    byte val = (byte)(encoded[i] - 45);
			    int j = 2*i;
			    array[j] = (byte)('0' + val/10);
			    array[j+1] = (byte)('0' + val % 10);
			}
			is = new ByteArrayInputStream(array);
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
			 fgColor, bgColor, label, fontSize, minMult);
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
	    if (contentsOnly == false && arg.equals("-")) {
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
	    } else if (arg.equals("-")) {
		uriList.add(STDIN_URI);
	    } else {
		File f = new File(arg);
		uriList.add((f.isAbsolute()? f: new File (cwd, arg)).toURI());
	    }
	    index++;
	}
	if (exitMode || contentsOnly) {
	    if (launch == false || contentsOnly) output = System.out;
	    boolean sawStdin = false;
	    for (URI u: uriList) {
		if (u==STDIN_URI) {
		    if (sawStdin) {
			System.err.println(localeString("qrl") + ": "
					   + localeString("multipleStdin"));
			System.exit(1);
		    }
		    doAdd(null);
		    sawStdin = true;
		} else {
		    doAdd(u.toURL());
		}
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
