/*
 * Created on Jan 11, 2005
 * Cache Refresh script for RadiantOne Sync Services
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
public class RefreshCacheImpl implements UserDefinedSync {

	private static String topologyName = null;
	private	static String transformationName = null;
	
	/* (non-Javadoc)
	 * @see com.rli.synsvcs.etl.UserDefinedSync#transform(com.rli.synsvcs.etl.BasicOperation, com.rli.synsvcs.etl.BasicOperation)
	 */
	@SuppressWarnings("unchecked")
	public void transform(BasicOperation src, BasicOperation dest) {	

		String srcSyncObjectDN = "$srcSyncObjectDN"; // source sync object
		String destSyncObjectDN = "$destSyncObjectDN"; // refresh cache object
		boolean isInstance = true;
		boolean isFirst = true;

		Vector fieldNames = src.getNames();

		while (isInstance == true)
		{
			if (isFirst==true)
				isFirst = false;  //first time - 
			else
				dest.newInstance();			
			String oper = src.getValue(RliCon.CHANGE_TYPE_ATTR_NAME);
			String primKey = src.getValue(RliCon.CHANGE_BASE_TABLE_PK); //"RLIPRIMKEY"
			String dn = src.getValue("dn");
			String keyValues = null;
			// build KeyValue(s) 
			if (primKey != null) 
			{
				StringTokenizer st = new java.util.StringTokenizer(primKey, ",");
			    while(st.hasMoreTokens() == true)
			    {
			         String keyName = st.nextToken();
			         String keyValue = src.getValue(keyName);
			         if (keyValues == null)
			             keyValues = keyValue;
			         else
			             keyValues = keyValues + " :: " + keyValue;
			    }
			}
//			 copy sources attributes to dest
			dest.setInstance(src);
// 			add extra attributes RLICHANGETYPE , RLIOPERATION ....
			dest.setValue(RliCon.CHANGE_TYPE_ATTR_NAME,RliCon.CHANGE_TYPE_REFRESH);
			dest.setValue(RliCon.CHANGE_OPERATION, oper);
			dest.setValue("RLISYNCOBJECT",srcSyncObjectDN);
			if (keyValues != null) 
			    dest.setValue("RLIKEYVALUES", keyValues);
			if (dn != null) 
			    dest.setValue("dn", dn);			
			isInstance = src.nextInstance();
		}

	}

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
