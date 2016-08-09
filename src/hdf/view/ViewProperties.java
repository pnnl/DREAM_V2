/*****************************************************************************
 * Copyright by The HDF Group.                                               *
 * Copyright by the Board of Trustees of the University of Illinois.         *
 * All rights reserved.                                                      *
 *                                                                           *
 * This file is part of the HDF Java Products distribution.                  *
 * The full copyright notice, including terms governing use, modification,   *
 * and redistribution, is contained in the files COPYING and Copyright.html. *
 * COPYING can be found at the root of the source code distribution tree.    *
 * Or, see http://hdfgroup.org/products/hdf-java/doc/Copyright.html.         *
 * If you do not have access to either file, you may request a copy from     *
 * help@hdfgroup.org.                                                        *
 ****************************************************************************/

package hdf.view;

import java.io.File;
import java.io.FileInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileOutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import hdf.HDFVersions;
import hdf.object.FileFormat;

public class ViewProperties extends Properties {
    private static final long   serialVersionUID     = -6411465283887959066L;

    private final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ViewProperties.class);

    /** the version of the HDFViewer */
    public static final String  VERSION              = HDFVersions.HDFVIEW_VERSION;

    /** the local property file name */
    private static final String USER_PROPERTY_FILE   = ".hdfview" + VERSION;

    /** the maximum number of most recent files */
    public static final int     MAX_RECENT_FILES     = 15;

    /** name of the tab delimiter */
    public static final String  DELIMITER_TAB        = "Tab";

    /** name of the tab delimiter */
    public static final String  DELIMITER_COMMA      = "Comma";

    /** name of the tab delimiter */
    public static final String  DELIMITER_SPACE      = "Space";

    /** name of the tab delimiter */
    public static final String  DELIMITER_COLON      = "Colon";

    /** image origin: UpperLeft */
    public static final String  ORIGIN_UL            = "UpperLeft";

    /** image origin: LowerLeft */
    public static final String  ORIGIN_LL            = "LowerLeft";

    /** image origin: UpperRight */
    public static final String  ORIGIN_UR            = "UpperRight";

    /** image origin: LowerRight */
    public static final String  ORIGIN_LR            = "LowerRight";

    /** name of the tab delimiter */
    public static final String  DELIMITER_SEMI_COLON = "Semi-Colon";

    /**
     * Property keys control how the data is displayed.
     */
    public static enum DATA_VIEW_KEY {
        CHAR, CONVERTBYTE, TRANSPOSED, READONLY, OBJECT, BITMASK, BITMASKOP, BORDER, INFO, INDEXBASE1
    }

    /**
     * Property keys control how the data is displayed.
     */
    public static enum BITMASK_OP {
        AND, EXTRACT
    }

    /** the root directory of the HDFView */
    private static String           rootDir                = System.getProperty("user.dir");

    /** user's guide */
    private static String           usersGuide             = rootDir + "/UsersGuide/index.html";

    /** the font size */
    private static int              fontSize               = 12;

    /** the font type */
    private static String           fontType               = null;

    /** the full path of H4toH5 converter */
    private static String           h4toh5                 = "";

    /** data delimiter */
    private static String           delimiter              = DELIMITER_TAB;

    /** image origin */
    private static String           origin                 = ORIGIN_UL;

    /** default index type */
    private static String           indexType              = "H5_INDEX_NAME";

    /** default index order */
    private static String           indexOrder             = "H5_ITER_INC";

    /** a list of most recent files */
    private static Vector<String>   recentFiles;

    /** default starting file directory */
    private static String           workDir                = "user.home";

    /** default HDF file extensions */
    private static String           fileExt                = "hdf, h4, hdf4, h5, hdf5, he2, he5";

    private static ClassLoader      extClassLoader         = null;

    /** a list of srb accounts */
    private static Vector<String[]> srbAccountList         = new Vector<String[]>(5);

    /**
     * flag to indicate if auto contrast is used in image processing. Do not use
     * autocontrast by default (2.6 change).
     */
    private static boolean          isAutoContrast         = false;

    private static boolean          showImageValues        = false;

    private static boolean          showRegRefValues       = false;

    /**
     * flag to indicate if default open file mode is read only. By default, use
     * read/write.
     */
    private static boolean          isReadOnly             = false;

    private static boolean             isEarlyLib                = true;

    /** a list of palette files */
    private static Vector<String>   paletteList            = new Vector<String>(5);

    /** flag to indicate if enum data is converted to strings */
    private static boolean          convertEnum            = true;

    /** flag to indicate if data is 1-based index */
    private static boolean          isIndexBase1           = false;

    /**
     * Current Java applications such as HDFView cannot handle files with a large
     * number of objects such as 1,000,000 objects. max_members defines the maximum
     * number of objects that will be loaded into memory.
     */
    private static int              max_members            = Integer.MAX_VALUE;   // load all by default
    /**
     * Current Java applications such as HDFView cannot handle files with a large
     * number of objects such 1,000,000 objects. start_members defines the
     * starting index of objects that will be loaded into memory.
     */
    private static int              start_members          = 0;

    private static ImageIcon        hdfIcon, h4Icon, h4IconR, h5Icon, h5IconR, largeHdfIcon, blankIcon, helpIcon, fileopenIcon,
    filesaveIcon, filenewIcon, filecloseIcon, foldercloseIcon, folderopenIcon, foldercloseIconA,
    folderopenIconA, datasetIcon, imageIcon, tableIcon, textIcon, datasetIconA, imageIconA, tableIconA,
    textIconA, zoominIcon, zoomoutIcon, paletteIcon, chartIcon, brightIcon, autocontrastIcon, copyIcon,
    cutIcon, pasteIcon, previousIcon, nextIcon, firstIcon, lastIcon, animationIcon, datatypeIcon,
    datatypeIconA, linkIcon, iconAPPS, iconURL, iconVIDEO, iconXLS, iconPDF, iconAUDIO, questionIcon;

    private static String           propertyFile;

    /** a list of treeview modules */
    private static Vector<String>   moduleListTreeView     = new Vector<String>(5);

    /** a list of metaview modules */
    private static Vector<String>   moduleListMetaDataView = new Vector<String>(5);

    /** a list of textview modules */
    private static Vector<String>   moduleListTextView     = new Vector<String>(5);

    /** a list of tableview modules */
    private static Vector<String>   moduleListTableView    = new Vector<String>(5);

    /** a list of imageview modules */
    private static Vector<String>   moduleListImageView    = new Vector<String>(5);

    /** a list of paletteview modules */
    private static Vector<String>   moduleListPaletteView  = new Vector<String>(5);

    /** a list of helpview modules */
    private static Vector<String>   moduleListHelpView     = new Vector<String>(5);

    /**
     * Creates a property list with given root directory of the HDFView.
     *
     * @param viewRoot
     *            the root directory of the HDFView
     */
    public ViewProperties(String viewRoot) {
        super();
        rootDir = viewRoot;
        log.trace("rootDir is {}", rootDir);
        String workPath = System.getProperty("hdfview.workdir");
        log.trace("hdfview.workdir={}", workPath);
        if (workPath != null) {
            workDir = workPath;
        }

        recentFiles = new Vector<String>(MAX_RECENT_FILES + 5);

        // find the property file
        String userHome, userDir, propertyFileName, h5v;

        // look for the property file in the user's home directory
        propertyFileName = USER_PROPERTY_FILE;
        userHome = System.getProperty("user.home") + File.separator + propertyFileName;
        userDir = System.getProperty("user.dir") + File.separator + propertyFileName;
        h5v = workDir + File.separator + propertyFileName;

        if ((new File(userHome)).exists()) {
            propertyFile = userHome;
        }
        else if ((new File(userDir)).exists()) {
            propertyFile = userDir;
        }
        else // create new property file at user home directory
        {
            propertyFile = h5v;
            File pFile = new File(h5v);
            try {
                pFile.createNewFile();
            }
            catch (Exception ex) {
                // Last resort: create new property file at user home directory
                propertyFile = userHome;
                try {
                    pFile = new File(userHome);
                    pFile.createNewFile();
                }
                catch (Exception ex2) {
                    propertyFile = null;
                }
            }
        }
    }

    /* the properties are sorted by keys */
    @SuppressWarnings("unchecked")
    public synchronized Enumeration<Object> keys() {
        Enumeration<?> keysEnum = super.keys();
        @SuppressWarnings("rawtypes")
        Vector keyList = new Vector(50);
        while (keysEnum.hasMoreElements()) {
            keyList.add(keysEnum.nextElement());
        }
        Collections.sort(keyList);
        return keyList.elements();
    }

    /**
     * load module classes
     *
     * @return the ClassLoader
     */
    public static ClassLoader loadExtClass() {
        if (extClassLoader != null) {
            return extClassLoader;
        }
        else {
            // default classloader
            extClassLoader = ClassLoader.getSystemClassLoader();
        }
        log.trace("loadExtClass: default classloader is {}", extClassLoader);

        String rootPath = System.getProperty("hdfview.root");
        if (rootPath == null) {
            rootPath = rootDir;
            log.debug("loadExtClass: rootDir rootPath is {}", rootPath);
        }
        log.debug("loadExtClass: rootPath is {}", rootPath);

        String dirname = rootPath + File.separator + "lib" + File.separator + "ext" + File.separator;
        String[] jars = null;
        File extdir = null;
        try {
            extdir = new File(dirname);
            jars = extdir.list();
            log.trace("loadExtClass: dirname is {} with {} jars", dirname, jars.length);
        }
        catch (Exception ex0) {
            log.debug("loadExtClass: load dirname: {}+lib/ext failed", rootPath, ex0);
        }

        if ((jars == null) || (jars.length <= 0)) {
            return extClassLoader;
        }

        Vector<String> jarList = new Vector<String>(50);
        Vector<String> classList = new Vector<String>(50);
        for (int i = 0; i < jars.length; i++) {
            log.trace("loadExtClass: load jar[{}]", i);
            if (jars[i].endsWith(".jar")) {
                jarList.add(jars[i]);
                // add class names to the list of classes
                File tmpFile = new File(extdir, jars[i]);
                try {
                    JarFile jarFile = new JarFile(tmpFile, false, JarFile.OPEN_READ);
                    Enumeration<?> emu = jarFile.entries();
                    while (emu.hasMoreElements()) {
                        JarEntry jarEntry = (JarEntry) emu.nextElement();
                        String entryName = jarEntry.getName();
                        log.trace("loadExtClass: reading jar[{}] class={}", i, entryName);
                        int idx = entryName.indexOf(".class");
                        if ((idx > 0) && (entryName.indexOf('$') <= 0)) {
                            entryName = entryName.replace('/', '.');
                            classList.add(entryName.substring(0, idx));
                        }
                    }

                    jarFile.close();
                }
                catch (Exception ex) {
                    log.debug("loadExtClass: load jar[{}] failed", i, ex);
                }
            } // if (jars[i].endsWith(".jar")) {
        } // for (int i=0; i<jars.length; i++) {

        int n = jarList.size();
        if (n <= 0) {
            log.debug("loadExtClass: jarList empty");
            return extClassLoader;
        }

        URL[] urls = new URL[n];
        for (int i = 0; i < n; i++) {
            try {
                urls[i] = new URL("file:///" + rootPath + "/lib/ext/" + jarList.get(i));
                log.trace("loadExtClass: load urls[{}] is {}", i, urls[i]);
            }
            catch (MalformedURLException mfu) {
                log.debug("loadExtClass: load urls[{}] failed", i, mfu);
            }
        }

        // try { extClassLoader = new URLClassLoader(urls); }
        try {
            extClassLoader = URLClassLoader.newInstance(urls);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }

        // load user modules into their list
        n = classList.size();
        for (int i = 0; i < n; i++) {
            String theName = classList.get(i);
            log.trace("loadExtClass: load classList[{}] is {}", i, theName);
            try {
                // enables use of JHDF5 in JNLP (Web Start) applications, the
                // system class loader with reflection first.
                Class<?> theClass = null;
                try {
                    theClass = Class.forName(theName);
                }
                catch (Exception ex) {
                    try {
                        theClass = extClassLoader.loadClass(theName);
                    }
                    catch (Exception exc) {
                        log.debug("load: loadClass({}) failed", theName, ex);
                    }
                }

                if(theClass != null) {
                    Class<?>[] interfaces = theClass.getInterfaces();
                    if (interfaces != null) {
                        for (int j = 0; j < interfaces.length; j++) {
                            String interfaceName = interfaces[j].getName();
                            log.trace("loadExtClass: load interfaces[{}] is {}", j, interfaceName);

                            if ("hdf.view.TreeView".equals(interfaceName) && !moduleListTreeView.contains(theName)) {
                                moduleListTreeView.add(theName);
                                break;
                            }
                            else if ("hdf.view.MetaDataView".equals(interfaceName)
                                    && !moduleListMetaDataView.contains(theName)) {
                                moduleListMetaDataView.add(theName);
                                break;
                            }
                            else if ("hdf.view.TextView".equals(interfaceName)
                                    && !moduleListTextView.contains(theName)) {
                                moduleListTextView.add(theName);
                                break;
                            }
                            else if ("hdf.view.TableView".equals(interfaceName)
                                    && !moduleListTableView.contains(theName)) {
                                moduleListTableView.add(theName);
                                break;
                            }
                            else if ("hdf.view.ImageView".equals(interfaceName)
                                    && !moduleListImageView.contains(theName)) {
                                moduleListImageView.add(theName);
                                break;
                            }
                            else if ("hdf.view.PaletteView".equals(interfaceName)
                                    && !moduleListPaletteView.contains(theName)) {
                                moduleListPaletteView.add(theName);
                                break;
                            }
                            else if ("hdf.view.HelpView".equals(interfaceName)
                                    && !moduleListHelpView.contains(theName)) {
                                moduleListHelpView.add(theName);
                                break;
                            }
                        } // for (int j=0; j<interfaces.length; j++) {
                    } // if (interfaces != null) {
                }
            }
            catch (Exception ex) {
                log.debug("loadExtClass: load classList[{}] of {} failed", i, theName, ex);
            }
        } // for (int i=0; i<n; i++)
        log.trace("loadExtClass: finished");

        return extClassLoader;
    }

    /** @return the root directory where the HDFView is installed. */
    public static String getViewRoot() {
        return rootDir;
    }

    public static Icon getFoldercloseIcon() {
        return foldercloseIcon;
    }

    public static Icon getFoldercloseIconA() {
        return foldercloseIconA;
    }

    public static Icon getFolderopenIcon() {
        return folderopenIcon;
    }

    public static Icon getFolderopenIconA() {
        return folderopenIconA;
    }

    public static Icon getHdfIcon() {
        return hdfIcon;
    }

    public static Icon getH4Icon() {
        return h4Icon;
    }

    public static Icon getH4IconR() {
        return h4IconR;
    }

    public static Icon getH5Icon() {
        return h5Icon;
    }

    public static Icon getH5IconR() {
        return h5IconR;
    }

    public static Icon getDatasetIcon() {
        return datasetIcon;
    }

    public static Icon getDatasetIconA() {
        return datasetIconA;
    }

    public static Icon getDatatypeIcon() {
        return datatypeIcon;
    }

    public static Icon getDatatypeIconA() {
        return datatypeIconA;
    }

    public static Icon getLinkIcon() {
        return linkIcon;
    }

    public static Icon getFileopenIcon() {
        return fileopenIcon;
    }

    public static Icon getFilesaveIcon() {
        return filesaveIcon;
    }

    public static Icon getFilenewIcon() {
        return filenewIcon;
    }

    public static Icon getFilecloseIcon() {
        return filecloseIcon;
    }

    public static Icon getPaletteIcon() {
        return paletteIcon;
    }

    public static Icon getBrightIcon() {
        return brightIcon;
    }

    public static Icon getAutocontrastIcon() {
        return autocontrastIcon;
    }

    public static Icon getImageIcon() {
        return imageIcon;
    }

    public static Icon getTableIcon() {
        return tableIcon;
    }

    public static Icon getTextIcon() {
        return textIcon;
    }

    public static Icon getImageIconA() {
        return imageIconA;
    }

    public static Icon getTableIconA() {
        return tableIconA;
    }

    public static Icon getTextIconA() {
        return textIconA;
    }

    public static Icon getZoominIcon() {
        return zoominIcon;
    }

    public static Icon getZoomoutIcon() {
        return zoomoutIcon;
    }

    public static Icon getBlankIcon() {
        return blankIcon;
    }

    public static Icon getHelpIcon() {
        return helpIcon;
    }

    public static Icon getCopyIcon() {
        return copyIcon;
    }

    public static Icon getCutIcon() {
        return cutIcon;
    }

    public static Icon getPasteIcon() {
        return pasteIcon;
    }

    public static Icon getLargeHdfIcon() {
        return largeHdfIcon;
    }

    public static Icon getPreviousIcon() {
        return previousIcon;
    }

    public static Icon getNextIcon() {
        return nextIcon;
    }

    public static Icon getFirstIcon() {
        return firstIcon;
    }

    public static Icon getLastIcon() {
        return lastIcon;
    }

    public static Icon getChartIcon() {
        return chartIcon;
    }

    public static Icon getAnimationIcon() {
        return animationIcon;
    }

    public static ImageIcon getAppsIcon() {
        return iconAPPS;
    }

    public static ImageIcon getUrlIcon() {
        return iconURL;
    }

    public static ImageIcon getVideoIcon() {
        return iconVIDEO;
    }

    public static ImageIcon getXlsIcon() {
        return iconXLS;
    }

    public static ImageIcon getPdfIcon() {
        return iconPDF;
    }

    public static ImageIcon getAudioIcon() {
        return iconAUDIO;
    }

    public static Icon getQuestionIcon() {
        return questionIcon;
    }

    public static void loadIcons() {
        URL u = null;

        // load icon images
        if (hdfIcon == null) {
            u = ViewProperties.class.getResource("/hdf/view/icons/hdf.gif");
            if (u != null) {
                hdfIcon = new ImageIcon(u);
            }
        }

        if (h4Icon == null) {
            u = ViewProperties.class.getResource("/hdf/view/icons/hdf4.gif");
            if (u != null) {
                h4Icon = new ImageIcon(u);
            }
        }

        if (h4IconR == null) {
            u = ViewProperties.class.getResource("/hdf/view/icons/hdf4R.gif");
            if (u != null) {
                h4IconR = new ImageIcon(u);
            }
        }

        if (h5Icon == null) {
            u = ViewProperties.class.getResource("/hdf/view/icons/hdf5.gif");
            if (u != null) {
                h5Icon = new ImageIcon(u);
            }
        }

        if (h5IconR == null) {
            u = ViewProperties.class.getResource("/hdf/view/icons/hdf5R.gif");
            if (u != null) {
                h5IconR = new ImageIcon(u);
            }
        }

        if (foldercloseIcon == null) {
            u = ViewProperties.class.getResource("/hdf/view/icons/folderclose.gif");
            if (u != null) {
                foldercloseIcon = new ImageIcon(u);
            }
        }

        if (foldercloseIconA == null) {
            u = ViewProperties.class.getResource("/hdf/view/icons/foldercloseA.gif");
            if (u != null) {
                foldercloseIconA = new ImageIcon(u);
            }
        }

        if (folderopenIcon == null) {
            u = ViewProperties.class.getResource("/hdf/view/icons/folderopen.gif");
            if (u != null) {
                folderopenIcon = new ImageIcon(u);
            }
        }

        if (folderopenIconA == null) {
            u = ViewProperties.class.getResource("/hdf/view/icons/folderopenA.gif");
            if (u != null) {
                folderopenIconA = new ImageIcon(u);
            }
        }

        if (datasetIcon == null) {
            u = ViewProperties.class.getResource("/hdf/view/icons/dataset.gif");
            if (u != null) {
                datasetIcon = new ImageIcon(u);
            }
        }

        if (datasetIconA == null) {
            u = ViewProperties.class.getResource("/hdf/view/icons/datasetA.gif");
            if (u != null) {
                datasetIconA = new ImageIcon(u);
            }
        }

        if (datatypeIcon == null) {
            u = ViewProperties.class.getResource("/hdf/view/icons/datatype.gif");
            if (u != null) {
                datatypeIcon = new ImageIcon(u);
            }
        }

        if (datatypeIconA == null) {
            u = ViewProperties.class.getResource("/hdf/view/icons/datatypeA.gif");
            if (u != null) {
                datatypeIconA = new ImageIcon(u);
            }
        }

        if (linkIcon == null) {
            u = ViewProperties.class.getResource("/hdf/view/icons/link.gif");
            if (u != null) {
                linkIcon = new ImageIcon(u);
            }
        }

        if (fileopenIcon == null) {
            u = ViewProperties.class.getResource("/hdf/view/icons/fileopen.gif");
            if (u != null) {
                fileopenIcon = new ImageIcon(u);
            }
        }

        if (filesaveIcon == null) {
            u = ViewProperties.class.getResource("/hdf/view/icons/filesave.gif");
            if (u != null) {
                filesaveIcon = new ImageIcon(u);
            }
        }

        if (filenewIcon == null) {
            u = ViewProperties.class.getResource("/hdf/view/icons/filenew.gif");
            if (u != null) {
                filenewIcon = new ImageIcon(u);
            }
        }

        if (filecloseIcon == null) {
            u = ViewProperties.class.getResource("/hdf/view/icons/fileclose.gif");
            if (u != null) {
                filecloseIcon = new ImageIcon(u);
            }
        }

        if (paletteIcon == null) {
            u = ViewProperties.class.getResource("/hdf/view/icons/palette.gif");
            if (u != null) {
                paletteIcon = new ImageIcon(u);
            }
        }

        if (brightIcon == null) {
            u = ViewProperties.class.getResource("/hdf/view/icons/brightness.gif");
            if (u != null) {
                brightIcon = new ImageIcon(u);
            }
        }

        if (autocontrastIcon == null) {
            u = ViewProperties.class.getResource("/hdf/view/icons/autocontrast.gif");
            if (u != null) {
                autocontrastIcon = new ImageIcon(u);
            }
        }

        if (imageIcon == null) {
            u = ViewProperties.class.getResource("/hdf/view/icons/image.gif");
            if (u != null) {
                imageIcon = new ImageIcon(u);
            }
        }

        if (imageIconA == null) {
            u = ViewProperties.class.getResource("/hdf/view/icons/imageA.gif");
            if (u != null) {
                imageIconA = new ImageIcon(u);
            }
        }

        if (tableIcon == null) {
            u = ViewProperties.class.getResource("/hdf/view/icons/table.gif");
            if (u != null) {
                tableIcon = new ImageIcon(u);
            }
        }

        if (tableIconA == null) {
            u = ViewProperties.class.getResource("/hdf/view/icons/tableA.gif");
            if (u != null) {
                tableIconA = new ImageIcon(u);
            }
        }

        if (textIcon == null) {
            u = ViewProperties.class.getResource("/hdf/view/icons/text.gif");
            if (u != null) {
                textIcon = new ImageIcon(u);
            }
        }

        if (textIconA == null) {
            u = ViewProperties.class.getResource("/hdf/view/icons/textA.gif");
            if (u != null) {
                textIconA = new ImageIcon(u);
            }
        }

        if (zoominIcon == null) {
            u = ViewProperties.class.getResource("/hdf/view/icons/zoomin.gif");
            if (u != null) {
                zoominIcon = new ImageIcon(u);
            }
        }

        if (zoomoutIcon == null) {
            u = ViewProperties.class.getResource("/hdf/view/icons/zoomout.gif");
            if (u != null) {
                zoomoutIcon = new ImageIcon(u);
            }
        }

        if (blankIcon == null) {
            u = ViewProperties.class.getResource("/hdf/view/icons/blank.gif");
            if (u != null) {
                blankIcon = new ImageIcon(u);
            }
        }

        if (helpIcon == null) {
            u = ViewProperties.class.getResource("/hdf/view/icons/help.gif");
            if (u != null) {
                helpIcon = new ImageIcon(u);
            }
        }

        if (copyIcon == null) {
            u = ViewProperties.class.getResource("/hdf/view/icons/copy.gif");
            if (u != null) {
                copyIcon = new ImageIcon(u);
            }
        }

        if (cutIcon == null) {
            u = ViewProperties.class.getResource("/hdf/view/icons/cut.gif");
            if (u != null) {
                cutIcon = new ImageIcon(u);
            }
        }

        if (pasteIcon == null) {
            u = ViewProperties.class.getResource("/hdf/view/icons/paste.gif");
            if (u != null) {
                pasteIcon = new ImageIcon(u);
            }
        }

        if (largeHdfIcon == null) {
            u = ViewProperties.class.getResource("/hdf/view/icons/hdf_large.gif");
            if (u != null) {
                largeHdfIcon = new ImageIcon(u);
            }
        }

        if (previousIcon == null) {
            u = ViewProperties.class.getResource("/hdf/view/icons/previous.gif");
            if (u != null) {
                previousIcon = new ImageIcon(u);
            }
        }

        if (nextIcon == null) {
            u = ViewProperties.class.getResource("/hdf/view/icons/next.gif");
            if (u != null) {
                nextIcon = new ImageIcon(u);
            }
        }

        if (firstIcon == null) {
            u = ViewProperties.class.getResource("/hdf/view/icons/first.gif");
            if (u != null) {
                firstIcon = new ImageIcon(u);
            }
        }

        if (lastIcon == null) {
            u = ViewProperties.class.getResource("/hdf/view/icons/last.gif");
            if (u != null) {
                lastIcon = new ImageIcon(u);
            }
        }

        if (chartIcon == null) {
            u = ViewProperties.class.getResource("/hdf/view/icons/chart.gif");
            if (u != null) {
                chartIcon = new ImageIcon(u);
            }
        }

        if (animationIcon == null) {
            u = ViewProperties.class.getResource("/hdf/view/icons/animation.gif");
            if (u != null) {
                animationIcon = new ImageIcon(u);
            }
        }

        if (questionIcon == null) {
            u = ViewProperties.class.getResource("/hdf/view/icons/question.gif");
            if (u != null) {
                questionIcon = new ImageIcon(u);
            }
        }

        try {
            u = ViewProperties.class.getResource("/hdf/view/icons/audio.gif");
            iconAUDIO = new ImageIcon(u);
        }
        catch (Exception ex) {
            iconAUDIO = null;
        }

        try {
            u = ViewProperties.class.getResource("/hdf/view/icons/xls.gif");
            iconXLS = new ImageIcon(u);
        }
        catch (Exception ex) {
            iconXLS = null;
        }

        try {
            u = ViewProperties.class.getResource("/hdf/view/icons/pdf.gif");
            iconPDF = new ImageIcon(u);
        }
        catch (Exception ex) {
            iconPDF = null;
        }

        try {
            u = ViewProperties.class.getResource("/hdf/view/icons/apps.gif");
            iconAPPS = new ImageIcon(u);
        }
        catch (Exception ex) {
            iconAPPS = null;
        }

        try {
            u = ViewProperties.class.getResource("/hdf/view/icons/url.gif");
            iconURL = new ImageIcon(u);
        }
        catch (Exception ex) {
            iconURL = null;
        }

        try {
            u = ViewProperties.class.getResource("/hdf/view/icons/video.gif");
            iconVIDEO = new ImageIcon(u);
        }
        catch (Exception ex) {
            iconVIDEO = null;
        }
    }

    /** Load user properties from property file
     * @throws Exception if a failure occurred
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void load() throws Exception {
        if (propertyFile == null)
            return;

        log.trace("load user properties: begin");

        String propVal = null;

        // add default module.
        String[] moduleKeys = { "module.treeview", "module.metadataview", "module.textview", "module.tableview",
                "module.imageview", "module.paletteview" };
        Vector[] moduleList = { moduleListTreeView, moduleListMetaDataView, moduleListTextView, moduleListTableView,
                moduleListImageView, moduleListPaletteView };
        String[] moduleNames = { "hdf.view.DefaultTreeView", "hdf.view.DefaultMetaDataView",
                "hdf.view.DefaultTextView", "hdf.view.DefaultTableView", "hdf.view.DefaultImageView",
        "hdf.view.DefaultPaletteView" };

        // add default implementation of modules
        for (int i = 0; i < 6; i++) {
            log.trace("load: add default moduleList[{}] is {}", i, moduleNames[i]);
            if (!moduleList[i].contains(moduleNames[i])) {
                moduleList[i].addElement(moduleNames[i]);
            }
        }
        if (extClassLoader == null) loadExtClass();

        // set default selection of data views
        for (int i = 0; i < 6; i++) {
            Vector<String> theList = moduleList[i];
            propVal = (String) get(moduleKeys[i]);

            if (propVal != null) {
                // set default to the module specified in property file
                theList.remove(propVal);
                theList.add(0, propVal);
            }
            else {
                // use default module
                theList.remove(moduleNames[i]);
                theList.add(0, moduleNames[i]);
            }
        }

        try {
            FileInputStream fis = new FileInputStream(propertyFile);
            load(fis);
            fis.close();
        }
        catch (Exception e) {
            log.debug("load: loading propertyFile failed", e);
        }

        // add fileformat modules
        Enumeration local_enum = this.keys();
        String theKey = null;
        String fExt = null;
        while (local_enum.hasMoreElements()) {
            theKey = (String) local_enum.nextElement();
            log.trace("load: add file format {}", theKey);
            if (theKey.startsWith("module.fileformat")) {
                fExt = theKey.substring(18);
                try {
                    // enables use of JHDF5 in JNLP (Web Start) applications,
                    // the system class loader with reflection first.
                    String className = (String) get(theKey);
                    Class theClass = null;
                    try {
                        theClass = Class.forName(className);
                    }
                    catch (Exception ex) {
                        try {
                            theClass = extClassLoader.loadClass(className);
                        }
                        catch (Exception ex2) {
                            log.debug("load: extClassLoader.loadClass({}) failed", className, ex2);
                        }
                    }

                    Object theObject = theClass.newInstance();
                    if (theObject instanceof FileFormat) {
                        FileFormat.addFileFormat(fExt, (FileFormat) theObject);
                    }
                }
                catch (Throwable err) {
                    log.debug("load: load file format failed", err);
                }
            }
        }

        propVal = (String) get("users.guide");
        if (propVal != null) {
            usersGuide = propVal;
        }

        propVal = (String) get("image.contrast");
        if (propVal != null) {
            isAutoContrast = ("auto".equalsIgnoreCase(propVal));
        }

        propVal = (String) get("image.showvalues");
        if (propVal != null) {
            showImageValues = ("true".equalsIgnoreCase(propVal));
        }

        propVal = (String) get("file.mode");
        if (propVal != null) {
            isReadOnly = ("r".equalsIgnoreCase(propVal));
        }

        propVal = (String) get("lib.version");
        if (propVal != null) {
            isEarlyLib = ("early".equalsIgnoreCase(propVal));
        }

        propVal = (String) get("enum.conversion");
        if (propVal != null) {
            convertEnum = ("true".equalsIgnoreCase(propVal));
        }

        propVal = (String) get("regref.showvalues");
        if (propVal != null) {
            showRegRefValues = ("true".equalsIgnoreCase(propVal));
        }

        propVal = (String) get("index.base1");
        if (propVal != null) {
            isIndexBase1 = ("true".equalsIgnoreCase(propVal));
        }

        propVal = (String) get("data.delimiter");
        if ((propVal != null) && (propVal.length() > 0)) {
            delimiter = propVal;
        }

        propVal = (String) get("image.origin");
        if ((propVal != null) && (propVal.length() > 0)) {
            origin = propVal;
        }

        propVal = (String) get("h5file.indexType");
        if ((propVal != null) && (propVal.length() > 0)) {
            indexType = propVal;
        }

        propVal = (String) get("h5file.indexOrder");
        if ((propVal != null) && (propVal.length() > 0)) {
            indexOrder = propVal;
        }

        propVal = (String) get("h4toh5.converter");
        if ((propVal != null) && (propVal.length() > 0)) {
            h4toh5 = propVal;
        }

        propVal = (String) get("work.dir");
        if ((propVal != null) && (propVal.length() > 0)) {
            workDir = propVal;
        }

        propVal = (String) get("file.extension");
        if ((propVal != null) && (propVal.length() > 0)) {
            fileExt = propVal;
            FileFormat.addFileExtension(fileExt);
        }

        propVal = (String) get("font.size");
        if ((propVal != null) && (propVal.length() > 0)) {
            try {
                fontSize = Integer.parseInt(propVal);
            }
            catch (Exception ex) {
                log.debug("load: load fontSize failed", ex);
            }
        }

        propVal = (String) get("font.type");
        if ((propVal != null) && (propVal.length() > 0)) {
            fontType = propVal.trim();
        }

        propVal = (String) get("max.members");
        if ((propVal != null) && (propVal.length() > 0)) {
            try {
                max_members = Integer.parseInt(propVal);
            }
            catch (Exception ex) {
                log.debug("load: load max.members failed", ex);
            }
        }

        // load the most recent file list from the property file
        String theFile = null;
        for (int i = 0; i < MAX_RECENT_FILES; i++) {
            theFile = getProperty("recent.file" + i);
            if ((theFile != null) && !recentFiles.contains(theFile)) {
                if (theFile.startsWith("http://") || theFile.startsWith("ftp://") || (new File(theFile)).exists()) {
                    recentFiles.addElement(theFile);
                }
            }
            else {
                this.remove("recent.file" + i);
            }
        }

        // load the most recent palette file list from the property file
        theFile = null;
        for (int i = 0; i < MAX_RECENT_FILES; i++) {
            theFile = getProperty("palette.file" + i);
            if (theFile != null) theFile = theFile.trim();

            if ((theFile != null && theFile.length() > 0) && !paletteList.contains(theFile)) {
                if ((new File(theFile)).exists()) {
                    paletteList.addElement(theFile);
                }
            }
            else {
                this.remove("palette.file" + i);
            }
        }

        // load srb account
        propVal = null;
        String srbaccount[] = new String[7];
        for (int i = 0; i < MAX_RECENT_FILES; i++) {
            if (null == (srbaccount[0] = getProperty("srbaccount" + i + ".host"))) {
                continue;
            }
            if (null == (srbaccount[1] = getProperty("srbaccount" + i + ".port"))) {
                continue;
            }
            if (null == (srbaccount[2] = getProperty("srbaccount" + i + ".user"))) {
                continue;
            }
            if (null == (srbaccount[3] = getProperty("srbaccount" + i + ".password"))) {
                continue;
            }
            if (null == (srbaccount[4] = getProperty("srbaccount" + i + ".home"))) {
                continue;
            }
            if (null == (srbaccount[5] = getProperty("srbaccount" + i + ".domain"))) {
                continue;
            }
            if (null == (srbaccount[6] = getProperty("srbaccount" + i + ".resource"))) {
                continue;
            }
            srbAccountList.add(srbaccount);
            srbaccount = new String[7];
        }

        // set default modules from user property files
        for (int i = 0; i < 6; i++) {
            String moduleName = (String) get(moduleKeys[i]);
            log.trace("load: default modules from user property is {}", moduleName);
            if ((moduleName != null) && (moduleName.length() > 0)) {
                if (moduleList[i].contains(moduleName)) moduleList[i].remove(moduleName);
                moduleList[i].add(0, moduleName);
            }
        }
        log.trace("load: finish");
    }

    /** Save user properties into property file */
    public void save() {
        if (propertyFile == null)
            return;

        clear();

        // update data saving options
        if (delimiter == null) {
            put("data.delimiter", DELIMITER_TAB);
        }
        else {
            put("data.delimiter", delimiter);
        }

        if (origin == null) {
            put("image.origin", ORIGIN_UL);
        }
        else {
            put("image.origin", origin);
        }

        if (indexType != null) {
            put("h5file.indexType", indexType);
        }

        if (indexOrder != null) {
            put("h5file.indexOrder", indexOrder);
        }

        if (usersGuide != null) {
            put("users.guide", usersGuide);
        }

        if (workDir != null) {
            put("work.dir", workDir);
        }

        if (fileExt != null) {
            put("file.extension", fileExt);
        }

        if (h4toh5 != null) {
            put("h4toh5.converter", h4toh5);
        }

        put("font.size", String.valueOf(fontSize));

        if (fontType != null) {
            put("font.type", fontType);
        }

        put("max.members", String.valueOf(max_members));

        if (isAutoContrast) {
            put("image.contrast", "auto");
        }
        else {
            put("image.contrast", "general");
        }

        if (showImageValues)
            put("image.showvalues", "true");
        else
            put("image.showvalues", "false");

        if (isReadOnly) {
            put("file.mode", "r");
        }
        else {
            put("file.mode", "rw");
        }

        if (isEarlyLib) {
            put("lib.version", "early");
        }
        else {
            put("lib.version", "latest");
        }

        put("enum.conversion", String.valueOf(convertEnum));
        if (showRegRefValues)
            put("regref.showvalues", "true");
        else
            put("regref.showvalues", "false");
        put("index.base1", String.valueOf(isIndexBase1));

        // save the list of most recent files
        String theFile;
        int size = recentFiles.size();
        int minSize = Math.min(size, MAX_RECENT_FILES);
        for (int i = 0; i < minSize; i++) {
            theFile = recentFiles.elementAt(i);
            if ((theFile != null) && (theFile.length() > 0)) {
                put("recent.file" + i, theFile);
            }
        }

        // save the list of most recent palette files
        size = paletteList.size();
        minSize = Math.min(size, MAX_RECENT_FILES);
        for (int i = 0; i < minSize; i++) {
            theFile = paletteList.elementAt(i);
            if ((theFile != null) && (theFile.length() > 0)) {
                put("palette.file" + i, theFile);
            }
        }

        // save srb account
        String srbaccount[] = null;
        size = srbAccountList.size();
        minSize = Math.min(size, MAX_RECENT_FILES);
        for (int i = 0; i < minSize; i++) {
            srbaccount = srbAccountList.get(i);
            if ((srbaccount[0] != null) && (srbaccount[1] != null) && (srbaccount[2] != null)
                    && (srbaccount[3] != null) && (srbaccount[4] != null) && (srbaccount[5] != null)
                    && (srbaccount[6] != null)) {
                put("srbaccount" + i + ".host", srbaccount[0]);
                put("srbaccount" + i + ".port", srbaccount[1]);
                put("srbaccount" + i + ".user", srbaccount[2]);
                put("srbaccount" + i + ".password", srbaccount[3]);
                put("srbaccount" + i + ".home", srbaccount[4]);
                put("srbaccount" + i + ".domain", srbaccount[5]);
                put("srbaccount" + i + ".resource", srbaccount[6]);
            }
        }

        // save default modules
        String moduleName = moduleListTreeView.elementAt(0);
        if ((moduleName != null) && (moduleName.length() > 0)) {
            put("module.treeview", moduleName);
        }

        moduleName = moduleListMetaDataView.elementAt(0);
        if ((moduleName != null) && (moduleName.length() > 0)) {
            put("module.metadataview", moduleName);
        }

        moduleName = moduleListTextView.elementAt(0);
        if ((moduleName != null) && (moduleName.length() > 0)) {
            put("module.textview", moduleName);
        }

        moduleName = moduleListTableView.elementAt(0);
        if ((moduleName != null) && (moduleName.length() > 0)) {
            put("module.tableview", moduleName);
        }

        moduleName = moduleListImageView.elementAt(0);
        if ((moduleName != null) && (moduleName.length() > 0)) {
            put("module.imageview", moduleName);
        }

        moduleName = moduleListPaletteView.elementAt(0);
        if ((moduleName != null) && (moduleName.length() > 0)) {
            put("module.paletteview", moduleName);
        }

        // save the current supported fileformat
        Enumeration<?> keys = FileFormat.getFileFormatKeys();
        String theKey = null;
        while (keys.hasMoreElements()) {
            theKey = (String) keys.nextElement();
            FileFormat theformat = FileFormat.getFileFormat(theKey);
            put("module.fileformat." + theKey, theformat.getClass().getName());
        }

        try {
            FileOutputStream fos = new FileOutputStream(propertyFile);
            store(fos, "User properties modified on ");
            fos.close();
        }
        catch (Exception e) {
            ;
        }
    }

    /** @return the name of the user property file */
    public static String getPropertyFile() {
        return propertyFile;
    }

    /** @return the default work directory, where the open file starts. */
    public static String getWorkDir() {
        String workPath = workDir;
        log.trace("getWorkDir: workDir={}", workDir);
        if (workPath == null) {
            workPath = System.getProperty("hdfview.workdir");
            log.trace("getWorkDir: hdfview.workdir={}", workPath);
            if (workPath == null) {
                workPath = System.getProperty("user.home");
            }
        }
        log.trace("getWorkDir: final workPath={}", workPath);
        return workPath;
    }

    /** @return the maximum number of the most recent file */
    public static int getMaxRecentFiles() {
        return MAX_RECENT_FILES;
    }

    /** @return the path of the HDFView users guide */
    public static String getUsersGuide() {
        return usersGuide;
    };

    /** @return the delimiter of data values */
    public static String getDataDelimiter() {
        return delimiter;
    }

    /** @return the image origin */
    public static String getImageOrigin() {
        return origin;
    }

    /** @return the default index type for display */
    public static String getIndexType() {
        return indexType;
    }

    /** @return the default index order for display */
    public static String getIndexOrder() {
        return indexOrder;
    }

    /** @return the font size */
    public static int getFontSize() {
        return fontSize;
    }

    /** @return the font type */
    public static String getFontType() {
        return fontType;
    }

    /** @return the file extensions of supported file formats */
    public static String getFileExtension() {
        return fileExt;
    }

    /** sets the font size
     *
     * @param fsize
     *            the font size
     */
    public static void setFontSize(int fsize) {
        fontSize = (fsize / 2) * 2;

        if (fontSize < 8) {
            fontSize = 8;
        }
    }

    /** sets the font type
     *
     * @param ftype
     *            the font type
     */
    public static void setFontType(String ftype) {
        if (ftype != null) {
            fontType = ftype.trim();
        }
    }

    /** @return the path of the H5toH5 converter */
    public static String getH4toH5() {
        return h4toh5;
    }

    /** @return the list of most recent files */
    public static Vector<String> getMRF() {
        return recentFiles;
    }

    /** @return the list of palette files */
    public static Vector<String> getPaletteList() {
        return paletteList;
    }

    public static Vector<String[]> getSrbAccount() {
        return srbAccountList;
    }

    /** @return a list of treeview modules */
    public static Vector<String> getTreeViewList() {
        return moduleListTreeView;
    }

    /** @return a list of metadataview modules */
    public static Vector<String> getMetaDataViewList() {
        return moduleListMetaDataView;
    }

    /** @return a list of textview modules */
    public static Vector<String> getTextViewList() {
        return moduleListTextView;
    }

    /** @return a list of tableview modules */
    public static Vector<String> getTableViewList() {
        return moduleListTableView;
    }

    /** @return a list of imageview modules */
    public static Vector<String> getImageViewList() {
        return moduleListImageView;
    }

    /** @return a list of paletteview modules */
    public static Vector<String> getPaletteViewList() {
        return moduleListPaletteView;
    }

    /** @return a list of helpview modules */
    public static Vector<String> getHelpViewList() {
        return moduleListHelpView;
    }

    /** set the path of H5View User's guide
     *
     * @param str
     *            the path
     */
    public static void setUsersGuide(String str) {
        if ((str == null) || (str.length() <= 0)) {
            return;
        }
        usersGuide = str;
    }

    /** set the path of the H4 to H5 converter
     *
     * @param tool
     *            the path of the H4 to H5 converter
     */
    public static void setH4toH5(String tool) {
        h4toh5 = tool;
    }

    /** set the path of the default work directory
     *
     * @param wDir
     *            the default work directory
     */
    public static void setWorkDir(String wDir) {
        log.trace("ViewProperties:setWorkDir wDir={}", wDir);
        workDir = wDir;
    }

    /** set the file extension
     *
     * @param ext
     *            the file extension
     */
    public static void setFileExtension(String ext) {
        fileExt = ext;
    }

    /** set the delimiter of data values
     *
     * @param delim
     *            the delimiter of data values
     */
    public static void setDataDelimiter(String delim) {
        delimiter = delim;
    }

    /** set the image origin
     *
     * @param o
     *            the image origin
     */
    public static void setImageOrigin(String o) {
        origin = o;
    }

    /** set the index type
     *
     * @param idxType
     *            the index type
     */
    public static void setIndexType(String idxType) {
        indexType = idxType;
    }

    /** set the index order
     *
     * @param idxOrder
     *            the index order
     */
    public static void setIndexOrder(String idxOrder) {
        indexOrder = idxOrder;
    }

    /**
     * Current Java applications such as HDFView cannot handle files with large
     * number of objects such as 1,000,000 objects. setMaxMembers() sets the
     * maximum number of objects that will be loaded into memory.
     *
     * @param n
     *            the maximum number of objects to load into memory
     */
    public static void setMaxMembers(int n) {
        max_members = n;
    }

    /**
     * Current Java applications such as HDFView cannot handle files with large
     * number of objects such as 1,000,000 objects. setStartMember() sets the
     * starting index of objects that will be loaded into memory.
     *
     * @param idx
     *            the maximum number of objects to load into memory
     */
    public static void setStartMembers(int idx) {
        if (idx < 0) {
            idx = 0;
        }

        start_members = idx;
    }

    /**
     * Current Java applications such as HDFView cannot handle files with large
     * number of objects such as 1,000,000 objects. getMaxMembers() returns the
     * maximum number of objects that will be loaded into memory.
     *
     * @return the maximum members
     */
    public static int getMaxMembers() {
        if (max_members < 0)
            return Integer.MAX_VALUE; // load the whole file

        return max_members;
    }

    /**
     * Current Java applications such as HDFView cannot handle files with large
     * number of objects such as 1,000,000 objects. getStartMembers() returns the
     * starting index of objects that will be loaded into memory.
     *
     * @return the start members
     */
    public static int getStartMembers() {
        return start_members;
    }

    /**
     * Returns true if auto contrast is used in image processing.
     *
     * @return true if auto contrast is used in image processing; otherwise,
     *         returns false.
     */
    public static boolean isAutoContrast() {
        return isAutoContrast;
    }

    /**
     * Returns true if "show image values" is set.
     *
     * @return true if "show image values" is set; otherwise, returns false.
     */
    public static boolean showImageValues() {
        return showImageValues;
    }

    /**
     * Set the flag to indicate if auto contrast is used in image process.
     *
     * @param b
     *            the flag to indicate if auto contrast is used in image
     *            process.
     */
    public static void setAutoContrast(boolean b) {
        isAutoContrast = b;
    }

    /**
     * Set the flag to indicate if "show image values" is set.
     *
     * @param b
     *            the flag to indicate if if "show image values" is set.
     */
    public static void setShowImageValue(boolean b) {
        showImageValues = b;
    }

    /**
     * Returns true if default file access is read only.
     *
     * @return true if default file access is read only; otherwise, returns
     *         false.
     */
    public static boolean isReadOnly() {
        return isReadOnly;
    }

    /**
     * Set the flag to indicate if default file access is read only.
     *
     * @param b
     *            the flag to indicate if default file access is read only.
     */
    public static void setReadOnly(boolean b) {
        isReadOnly = b;
    }

    /**
     * Returns true if default lib version is the earliest.
     *
     * @return true if default lib version is the earliest; otherwise, returns
     *         false.
     */
    public static boolean isEarlyLib() {
        return isEarlyLib;
    }

    /**
     * Set the flag to indicate if default lib version is the earliest.
     *
     * @param b
     *            the flag to indicate if default lib version is the earliest.
     */
    public static void setEarlyLib(boolean b) {
        isEarlyLib = b;
    }

    /**
     * @return the convertEnum
     */
    public static boolean isConvertEnum() {
        return convertEnum;
    }

    /**
     * Returns true if "show regref values" is set.
     *
     * @return true if "show regref values" is set; otherwise, returns false.
     */
    public static boolean showRegRefValues() {
        return showRegRefValues;
    }

    /**
     * @return the isIndexBase1
     */
    public static boolean isIndexBase1() {
        return isIndexBase1;
    }

    /**
     * @param convertEnum
     *            the convertEnum to set
     */
    public static void setConvertEnum(boolean convertEnum) {
        ViewProperties.convertEnum = convertEnum;
    }

    /**
     * Set the flag to indicate if "show RegRef values" is set.
     *
     * @param b
     *            the flag to indicate if if "show RegRef values" is set.
     */
    public static void setShowRegRefValue(boolean b) {
        showRegRefValues = b;
    }

    /**
     * Set the flag to indicate if IndexBase should start at 1.
     *
     * @param b
     *            the flag to indicate if IndexBase should start at 1.
     */
    public static void setIndexBase1(boolean b) {
        ViewProperties.isIndexBase1 = b;
    }

}
