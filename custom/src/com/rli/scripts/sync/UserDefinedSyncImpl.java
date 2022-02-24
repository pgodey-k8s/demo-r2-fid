/*
 * Created on Jan 11, 2005
 * Transformation script for RadiantOne Sync Services
 */
package com.rli.scripts.sync;

import com.rli.synsvcs.etl.BasicOperation;
import com.rli.synsvcs.etl.UserDefinedSync;
import com.rli.synsvcs.etl.UserDefineSyncUtils;
import com.rli.synsvcs.common.RliCon;
import java.util.*;

/**
 * 
 */
public class UserDefinedSyncImpl implements UserDefinedSync {

	private static String topologyName = null;
	private	static String transformationName = null;
	
	
	public void testTransform(BasicOperation src, BasicOperation dest,String opType) 
	{
		transform(src, dest);
	}
	
	/* (non-Javadoc)
	 * @see com.rli.synsvcs.etl.UserDefinedSync#transform(com.rli.synsvcs.etl.BasicOperation, com.rli.synsvcs.etl.BasicOperation)
	 */
	@SuppressWarnings("unchecked")
	public void transform(BasicOperation src, BasicOperation dest) {	

		String srcSyncObjectDN = "$srcSyncObjectDN"; // source sync object
		String destSyncObjectDN = "$destSyncObjectDN"; // dest syncobject
		boolean isInstance = true;
		boolean isFirst = true;
	
		Vector fieldNames = src.getNames();

		while (isInstance == true)
		{
			if (isFirst==true)
				isFirst = false;  //first time - no need for dest.newInstance()
			else
				dest.newInstance();

			// attribute  defining the operation RLICHANGETYPE ('insert','update','delete')
			dest.setValue(RliCon.CHANGE_TYPE_ATTR_NAME,src.getValue(RliCon.CHANGE_TYPE_ATTR_NAME)); 

			// begin specific code here
			
			// end specific code
			isInstance = src.nextInstance();
		}

	}

	// begin user functions

	// end user functions


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
