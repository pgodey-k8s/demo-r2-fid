/*
 * Created on Jan 11, 2005
 * Transformation script for RadiantOne Sync Services
 */
package com.rli.scripts.sync;

import com.rli.synsvcs.etl.BasicOperation;
import com.rli.synsvcs.etl.UserDefinedSync;
import com.rli.synsvcs.etl.UserDefineSyncUtils;
import com.rli.synsvcs.common.RliCon;


/**
 * Template for Replication from one LDAP directory to another using 'generic' objects.
 * Warning: modifying template files may cause automatic-generation of topology code
 * to fail.
 * 
 * 
 */
public class LdapReplicationTemplate implements UserDefinedSync {

	private static String topologyName = null;
	private	static String transformationName = null;
	
	public void testTransform(BasicOperation src, BasicOperation dest,String opType) 
	{
		transform(src, dest);
	}
	
	/* (non-Javadoc)
	 * @see com.rli.synsvcs.etl.UserDefinedSync#transform(com.rli.synsvcs.etl.BasicOperation, com.rli.synsvcs.etl.BasicOperation)
	 */
	public void transform(BasicOperation src, BasicOperation dest) {	

	
		String srcSyncObjectDN = "$srcSyncObjectDN"; // source sync object
		String destSyncObjectDN = "$destSyncObjectDN"; // dest syncobject
		boolean isInstance = true;
		boolean isFirst = true;

		String srcSuffix = "$srcSuffix";
		String destSuffix = "$destSuffix";
		
		while (isInstance == true)
		{
			if (isFirst==true)
			{
				isFirst = false;  //first time - no need for dest.newInstance()
				dest.setInstance(src);
			}
			else
				dest.newInstance(src);

			String srcDn = src.getValue("dn");
			int srcSuffixPos = srcDn.toLowerCase().indexOf(srcSuffix.toLowerCase());
			if(srcSuffixPos > 0)
			{
				String targetDn = srcDn.substring(0, srcSuffixPos) + destSuffix;
				dest.setValue("dn", targetDn);
				dest.setValue(RliCon.CHANGE_MODE, RliCon.CHANGE_MODE_SYNCHRONIZED_UPDATE_IF_MODIFIED);
			}
			else // ignore what it is not in the proper branch
			{
				dest.setValue(RliCon.CHANGE_TYPE_ATTR_NAME, RliCon.CHANGE_TYPE_ABORT);
			}

			isInstance = src.nextInstance();
		}

	}

	// <Main_>
	public static void main(String[] args) {
	}
	// </Main_>

	
	public void log(String message)
	{
		parseNames();
		UserDefineSyncUtils.log(topologyName,transformationName,message,1);
	}

	public void log(String message,int level)
	{
		parseNames();
		UserDefineSyncUtils.log(topologyName,transformationName,message,level);
	}

	public void log(Exception e)
	{
		parseNames();
		UserDefineSyncUtils.log(topologyName,transformationName,e);
	}



	private void parseNames() 
	{
		if(topologyName ==null || transformationName ==null)
		{
			String className = this.getClass().getName();
			
			int lastIndexOfDot = className.lastIndexOf(".");
			if(lastIndexOfDot>=0)
			{
				transformationName = className.substring(lastIndexOfDot+1);
				className=className.substring(0, lastIndexOfDot);
				lastIndexOfDot = className.lastIndexOf(".");
				if(lastIndexOfDot>=0)
				{
					topologyName = className.substring(lastIndexOfDot+1);
				}
			}
		}
	}


}
