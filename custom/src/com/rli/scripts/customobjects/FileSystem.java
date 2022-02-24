package com.rli.scripts.customobjects;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import javax.naming.directory.Attribute; //import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import com.rli.exception.RliException;
import com.rli.slapd.server.LDAPAttribute;
import com.rli.synsvcs.common.ConnString2;
import com.rli.tools.sync.cacherefresh.tools.DnUtils;
import com.rli.util.ConnectionString;
import com.rli.util.FileUtils;
import com.rli.util.RLIConstants; //import com.rli.util.RliScriptsHelper;
import com.rli.util.conf.VdsParameters;
import com.rli.util.djava.ScriptHelper;
import com.rli.util.metaHelper.RliStringObjectFactory;
import com.rli.vds.util.InterceptParam; //import com.rli.vds.util.UserDefinedEntry;
//import com.rli.vds.util.UserDefinedEntry2;
import com.rli.vds.util.UserDefinedInterception2;

public class FileSystem implements UserDefinedInterception2 {
	private final static Logger log4jLogger = LogManager.getLogger(FileSystem.class);

	private String rootPath; // root read in connection string

	private static final String ROOTPATH = "rootpath";

	private static final String DOCUMENT_NAME = "DocumentName";
	private static final String DOCUMENT_DATE = "DocumentDate";
	private static final String DOCUMENT_EXTENSION = "DocumentExtension";
	private static final String DOCUMENT_PATH = "DocumentPath";
	private static final String DOCUMENT_CONTENT = "DocumentContent";
	private static final String DOCUMENT_TYPE = "DocumentType";
	// private static final String DOCUMENT_SIZE = "documentSize";

	private static final String OBJECTNAME_FILE = "file";
	private static final String OBJECTNAME_DIR = "folder";

	private static final String OBJECTCLASS = "objectclass";

	private String initPersonRoot(String dn) {
		System.out.println("Init the person root");

		// search for the first uid in the dn and create the directory
		String[] parts = dn.split(",");
		for (String part : parts) {
			if (part.startsWith("uid=")) {
				String uid = part.substring("uid=".length());
				if (!(new File(rootPath + File.separatorChar + uid)).exists())
					createFolder(rootPath + File.separatorChar + uid);
				return uid;
			}
		}

		return "";
	}

	private void initRoot(String connectionString) {
		System.out.println("Init the root");
		ConnString2 cs2 = RliStringObjectFactory
				.getConnString2(connectionString);
		rootPath = cs2.getProperty(ROOTPATH);

		// ConnectionString sv = new ConnectionString(connectionString);
		// Properties psv= sv.getProperties();
		// Object root=psv.get(ROOTPATH);
		// if(root != null){
		if (!ScriptHelper.isEmpty(rootPath)) {
			// rootPath = (String)root;
			System.out.println("Root directory:" + rootPath);
		} else {
			// System.err.println("Invalid connection string, the root path is missing");
			// use default value
			rootPath = VdsParameters.getVdsPrimaryInstance().getCustomDir() + File.separator
					+ "entreprisedirectory";
		}
		// lets create it if it doesn't exist
		if (!(new File(rootPath)).exists()) {
			createFolder(rootPath);
		}
	}

	private boolean createFolder(String filePath) {
		try {
			System.out.println("Create the directory:" + filePath);
			return (new File(filePath)).mkdir();

		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	private boolean createFolder(File file) {
		try {
			System.out
					.println("Create the directory:" + file.getAbsolutePath());
			return file.mkdir();

		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	private void deleteDocument(InterceptParam prop) {

		try {
			initRoot(prop.getConnectionstring());

			// get the dn
			String dn = prop.getVirtualBaseDn();

			String uid = initPersonRoot(dn);

			File file = dnToFile(dn, uid);

			String commandstr = prop.getCommand();
			System.out.println(commandstr);
			boolean ok = false;

			// Attribute ldapAttr=prop.getAttributes().get("DocumentType");
			// String type=(String) (ldapAttr!=null?ldapAttr.get():null);
			// //default folder
			// if(type==null){
			// type=OBJECTNAME_DIR;
			// }
			//			
			// boolean ok=false;
			// if(OBJECTNAME_DIR.equals(type)){
			// if (file.isDirectory())
			// ok=file.delete();
			// }
			// else{
			// if (!file.isFile())
			// ok=file.delete();
			// }

			ok = file.delete();
			ok = !file.exists();

			if (ok) {
				prop.setStatusOk();
			} else {
				prop.setStatusFailed();
			}
		} catch (Exception iex) {
			prop.setStatusFailed();
		}
	}

	private String fileToDn(File file) {
		StringBuffer result = new StringBuffer();
		fileToDn(file, result);
		return result.toString();
	}

	private File dnToFile(String dn, String uid) {
		return new File(dnToPath(dn, uid));
	}

	private String dnToPath(String dn, String uid) {
		if (dn == null) {
			return null;
		}
		StringBuffer sbFileName = new StringBuffer(rootPath
				+ File.separatorChar + uid);
		String[] dnElts = dn.split(",");
		// reverse dir order
		for (int i = dnElts.length - 1; i >= 0; i--) {
			String value = dnElts[i];
			// remove rdn name
			int indexEqual = value.indexOf('=');
			String object = value.substring(0, indexEqual).trim();
			boolean toAdd = (OBJECTNAME_FILE.equalsIgnoreCase(object) || OBJECTNAME_DIR
					.equalsIgnoreCase(object));
			int nbElement = 0;
			if (toAdd) {
				value = value.substring(indexEqual + 1, value.length()).trim();

				// add separ
				sbFileName.append(File.separatorChar);

				sbFileName.append(value);
				nbElement++;
			}
			if (sbFileName.length() != 0 && nbElement == 1) {
				// add separ (root dir)
				sbFileName.append(File.separatorChar);
			}
		}

		return sbFileName.toString();
	}

	private void extractResultFile(File file, int scope, List listFiles) {
		// ask for fileList from the path
		if (file == null) {
			return;
		}
		File[] results = null;

		// base
		if (scope == SearchControls.OBJECT_SCOPE) {
			System.out.println("base on " + file.getAbsolutePath());
			results = new File[1];
			results[0] = file;
		} else {
			System.out.println("one on " + file.getAbsolutePath());
			// one or sub, add current dir
			if (file.isDirectory()) {
				results = file.listFiles();
			}
		}

		// save results
		if (results != null) {
			// add results to current list
			for (int i = 0; i < results.length; i++) {
				System.out.println("File object to result :" + results[i]);
				listFiles.add(results[i]);
			}

			// recursive call
			if (scope == SearchControls.SUBTREE_SCOPE) {
				for (int i = 0; i < results.length; i++) {
					extractResultFile(results[i], scope, listFiles);
				}
			}
		}
	}

	private void fileToDn(File file, StringBuffer dn) {
		if (file == null) {
			return;
		}
		// add name of file part
		if (file.isDirectory())
			dn.append(OBJECTNAME_DIR);
		else
			dn.append(OBJECTNAME_FILE);
		dn.append("=");
		String value = file.getName();
		if ("".equals(value)) {
			// root, use dir name then
			value = file.getAbsolutePath();
			// remove separ char
			value = value.substring(0, value.length() - 1);
		}
		dn.append(value);
		File parent;
		if ((parent = file.getParentFile()) != null) {
			if (!parent.getAbsolutePath().equals(rootPath)) {
				dn.append(",");
				fileToDn(parent, dn);
			}
		}
	}

	public static void main(String[] args) {

		File f = new File("C://mymysic//");
		System.out.println(f.getAbsolutePath());
		// File f=new File("c:/");
		// File [] list=f.listFiles();
		// for (int i = 0; i < list.length; i++) {
		// String path=list[i].getAbsolutePath();
		// // String dn=path2Name(path);
		// String dn2=fileToDn(list[i]);
		// String path2=dnToPath(dn2);
		// System.out.println("found: "+path);
		// System.out.println("todn: "+dn2);
		// System.out.println("topath: "+path2);
		// System.out.println();
		// }

		// String dn="file=C:,dv=temp,o=vds one";
		// String dn="dv=mycomputer,o=vds";
		// String dn="file=AUTOEXEC.BAT,file=C:,dv=mycomputer,o=vds";
		// // String dn="file=C:,dv=mycomputer,o=vds";
		// String path=dnToPath(dn);
		// System.out.println(path);
		// UserDefinedEntry ud = new FileSystem();
		// XMLOperation src = new XMLOperation();
		// XMLOperation dest = new XMLOperation();
		// src.setValue("dn", dn);
		// src.setValue("scope", "one");
		// ud.select(src, dest);
	}

	public void authenticate(InterceptParam arg0) {
		// TODO Auto-generated method stub

	}

	public void compare(InterceptParam arg0) {
		// TODO Auto-generated method stub

	}

	public void delete(InterceptParam prop) {
		deleteDocument(prop);
	}

	public void insert(InterceptParam prop) {
		System.out.println("Begin insert....");
		writeDocument(prop, true);
	}

	public void invoke(InterceptParam arg0) {
		// TODO Auto-generated method stub

	}

	public void select(InterceptParam prop) {

		try {

			initRoot(prop.getConnectionstring());

			// get the dn
			String dn = prop.getVirtualBaseDn();

			String uid = initPersonRoot(dn);

			System.out.println("dn=" + dn);
			// get object file
			File file = dnToFile(dn, uid);

			System.out.println("Path=" + file.getAbsolutePath());

			// String commandstr = prop.getCommand();
			// XMLOperation command=new XMLOperation();
			// command.importXML(commandstr);
			// file or folder
			// String type=command.getValue("object");
			// String type=prop.getTypename();
			// if(!OBJECTNAME_DIR.equals(type)){
			//				
			// }

			String type = prop.getTypename();
			// default folder
			if (type == null) {
				type = OBJECTNAME_DIR;
			}

			String scope = prop.getScope();
			log4jLogger.info(dn + " " + scope);

			int scopeInt = SearchControls.OBJECT_SCOPE;
			if ("one".equalsIgnoreCase(scope)) {
				scopeInt = SearchControls.ONELEVEL_SCOPE;
			} else if ("sub".equalsIgnoreCase(scope)) {
				scopeInt = SearchControls.SUBTREE_SCOPE;
			}

			// generate results
			List listFilesToPublish = new ArrayList();
			extractResultFile(file, scopeInt, listFilesToPublish);

			// produce result
			List listSearchResult = new Vector(listFilesToPublish.size());
			boolean added = false;
			for (int i = 0; i < listFilesToPublish.size(); i++) {
				File file2 = (File) listFilesToPublish.get(i);
				// System.out.println("Object type="+type);
				boolean isDir = OBJECTNAME_DIR.equalsIgnoreCase(type);
				boolean isFile = OBJECTNAME_FILE.equalsIgnoreCase(type);
				if ((isDir && file2.isDirectory())
						|| (isFile && file2.isFile())) {
					BasicAttributes currentAttrs = new BasicAttributes();
					// addAttribute("dn", fileToDn(file), currentAttrs);
					String fileName = file2.getName();
					addAttribute(DOCUMENT_NAME, file2.getName(), currentAttrs);
					if (file.isDirectory()) {
						addAttribute(OBJECTCLASS, "top#vdacontainer#dir",
								currentAttrs);
						addAttribute(DOCUMENT_TYPE, OBJECTNAME_DIR,
								currentAttrs);

					} else {
						addAttribute(OBJECTCLASS, "top#file", currentAttrs);
						addAttribute(DOCUMENT_TYPE, OBJECTNAME_FILE,
								currentAttrs);
					}
					String key = file2.getName();
					addAttribute("cn", key, currentAttrs);
					// fileToDn(file2);
					String rdn = type + "=" + key;
					SearchResult sr = new SearchResult(rdn, null, currentAttrs);
					listSearchResult.add(sr);
					// sr.getName()

					// remy jul27/2009- support missing attributes
					addAttribute(DOCUMENT_DATE, new Date(file2.lastModified())
							.toString(), currentAttrs);
					int indexDot = fileName.lastIndexOf('.');
					if (isFile && indexDot != -1) {
						addAttribute(DOCUMENT_EXTENSION, fileName
								.substring(indexDot + 1), currentAttrs);
					}
					addAttribute(DOCUMENT_PATH, file2.getAbsolutePath(),
							currentAttrs);
					// doc content, only if requested
					if (prop.getAttrs().contains(DOCUMENT_CONTENT)) {
						currentAttrs.put(DOCUMENT_CONTENT, FileUtils
								.generateBytesFromFile(file2));
					}

				}
			}
			prop.setStatusOk();
			prop.setResultSet((Vector) listSearchResult);
		} catch (Exception iex) {
			ScriptHelper.logException(iex);
			prop.setStatusFailed();
		}

	}

	private static void addAttribute(String name, String value,
			BasicAttributes attrs) {
		BasicAttribute attr = new BasicAttribute(name);
		if (value != null && value.contains("#")) {
			String[] values = value.split("#");
			for (int i = 0; i < values.length; i++) {
				attr.add(values[i]);
			}
		} else {
			attr.add(value);
		}
		attrs.put(attr);
	}

	public void update(InterceptParam arg0) {
		writeDocument(arg0, false);
	}

	private void writeDocument(InterceptParam prop, boolean isinsert) {
		Exception exception = null;
		try {
			boolean isUpdate = RLIConstants.STR_ACTION_ATTR_UPDATE
					.equalsIgnoreCase(prop.getAction());

			System.out.println("Begin write");
			initRoot(prop.getConnectionstring());

			// get the dn
			String dn = prop.getVirtualBaseDn();
			File file = null;
			String uid = initPersonRoot(dn);
			file = dnToFile(dn, uid);
			Attribute attrPath = prop.getAttributeToInsertOrUpdate(DOCUMENT_PATH);
			if (attrPath != null && attrPath.size() != 0) {
				// if path is provided use if
				String path = (String) attrPath.get();
				// check path provided is under root path
				if (FileUtils.isUnder(path, rootPath)) {
					if (isUpdate) {
						// update and path provided, do move
						file.renameTo(new File(path));
					}
					file = new File(path);
				} else {
					throw new RliException(
							"Not allowed, is out of scope of root dir: "
									+ rootPath);
				}
			}

			// String commandstr = src.getValue("command");
			// XMLOperation command=new XMLOperation();
			// command.importXML(commandstr);
			//			
			// System.out.println("Commandstr:");
			// System.out.println(commandstr);

			Attribute ldapAttr = prop.getAttributeToInsert(DOCUMENT_TYPE);
			String type = (String) (ldapAttr != null ? ldapAttr.get() : null);
			if (type == null) {
				if (OBJECTNAME_FILE.equals(prop.getName())) {
					type = OBJECTNAME_FILE;
				} else {
					// default folder
					type = OBJECTNAME_DIR;
				}
			}

			System.out.println("Object=" + type);

			// FOLDER
			if (OBJECTNAME_DIR.equalsIgnoreCase(type)) {
				// create the folder if not exists otherwise do nothing
				if (!file.exists()) {
					if (!createFolder(file)) {
						throw new RliException("Cannot create folder "
								+ file.getAbsolutePath());
					}
				} else {
					if (isinsert) {
						throw new RliException("Folder "
								+ file.getAbsolutePath() + " already exist");
					}
					// else update nothing to do
				}
			} else {// FILE
				// create file
				// get data
				Attribute attrData = prop
						.getAttributeToInsertOrUpdate(DOCUMENT_CONTENT);
				Object attrDataValue = attrData == null ? null : attrData.get();
				byte[] data = null;
				if (attrDataValue != null) {
					if (attrDataValue instanceof byte[]) {
						data = (byte[]) attrDataValue;
					} else if (attrDataValue instanceof String) {
						data = LDAPAttribute
								.generateByteValue((String) attrDataValue);
					}
				}
				if (!isUpdate || attrDataValue != null) {
					// only write for insert and for update with data provided
					FileUtils.writeByteToFile(file, data);
					if (!file.exists()) {
						throw new RliException("Cannot create File "
								+ file.getAbsolutePath());
					}
				}
			}
		} catch (Exception iex) {
			exception = iex;
		} finally {
			if (exception == null) {
				prop.setStatusOk();
			} else {
				prop.setStatusFailed();
				prop.setErrormessage(exception.getMessage());
			}
		}
	}
}
