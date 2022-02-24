/*
 * Created on Jan 11, 2005
 * Transformation script for RadiantOne Sync Services
 */
package com.rli.scripts.sync;

import com.rli.synsvcs.etl.BasicOperation;
import com.rli.synsvcs.etl.UserDefinedSync;
import com.rli.synsvcs.etl.UserDefineSyncUtils;
import com.rli.synsvcs.common.RliCon;
import com.rli.synsvcs.etl.VIH;
import com.rli.synsvcs.etl.XmlOp;
import java.util.*;

/**
 * 
 */
public class fromtoVIH implements UserDefinedSync {

	
		
	private BasicOperation src=null;
	private BasicOperation dest=null;
	private String chgType=null;
	
	/* (non-Javadoc)
	 * @see com.rli.synsvcs.etl.UserDefinedSync#transform(com.rli.synsvcs.etl.BasicOperation, com.rli.synsvcs.etl.BasicOperation)
	 */
	public void transform(BasicOperation src, BasicOperation dest) 
	{	

		
		this.src=src;
		this.dest=dest;
		
		/*
		 *  Replace $vimRDN with RDN name (generally VIM table name same as syncobject name)  
		 *  Replace $destRDN with destination datastore RDN name used in the syncobject view
		 *  Replace $keyname with the primary key name of the destination object ( "uid" for instance)
		 */
		/***<Auto_><Src_and_Dst_var_>***/
		String srcSyncObjectDN = "$srcSyncObjectDN";
		String destSyncObjectDN = "$destSyncObjectDN";
		String srcRDN = "$vimRDN";
		String destRDN="$vimRDN";
		String srcKeyname = "VUID";
		String destKeyname="VUID";
		/***</Src_and_Dst_var_></Auto_>***/

		
		/***<Auto_><Dst_and_Vim_conector_type_>***/
		boolean vimNativeConnector=true; 
		boolean destNativeConnector=false;
		/***</Dst_and_Vim_conector_type_></Auto_>***/
		
		int vimsourceid=256;		
		int datasourceid=0;

			
		// set datasourceid of source object
		/***<Auto_><data_Source_setting_>***/
			datasourceid= $datasourceid;
			vimsourceid= $vimsourceid;
		/***</data_Source_setting_></Auto_>***/


		//VIH vim= new VIH(src.getvalue("RLI_TOPOLOGYNAME",""));
		
		
		/***<Auto_><vih_setting_>***/
			VIH vim= new VIH("$topologyName");
		/***</vih_setting_></Auto_>***/
		
		/* 
		 * Replace vdsHost,vdsPort,vdsUser, vdsPassword , useSSL with vds connection info.
		 * If you don't want the password in clear, encode it using the password encoder utility located 
		 * on the Tools menu in the Virtual Directory Server Administration Console. Enter in the
		 * password and then copy the encoded value into the vim.setVDSInfo line.
		 * As an alternative to using the vim.setVDSinfo line in your script, you can remove it
		 * and the default #VDS/LDAP server settings from the vd_acl_jdbc.conf file will be used.
   		 */
		
		//vim.setVDSinfo (vdsHost,vdsPort,vdsUser,vdsPassword,useSSL);
		/*
		vim.setVDSinfo ("localhost","2389","cn=directory manager","secret", 0);	
		*/
	
		 // Virtual Identity Hub information (object/table that stores identities and properties)	
		vim.setVIMinfo(srcSyncObjectDN,vimsourceid,srcRDN,srcKeyname,vimNativeConnector);
		 // destination datastore information setting
		vim.setDataSourceinfo(destSyncObjectDN,vimsourceid,destRDN,destKeyname,destNativeConnector,true);

		boolean isInstance = true;
		//  Message may be made of multiple instances, the script loops on each 
		
		/***<Auto_><authoritativeDataSource_>***/
			int[] authoritativeDataSource={1,2};
		/***</authoritativeDataSource_></Auto_>***/
		boolean bAuthoritative=false;
			
		if (vim.isConnectorMode() && (vim.isAutoReevaluation()==false))
		{
			dest.setValue(RliCon.CHANGE_TYPE_ATTR_NAME,RliCon.CHANGE_TYPE_ABORT);
		}
		else
			{
			
				while (isInstance == true)
				{
					int insource=0;
					String ChangeOrigin=src.getValue("SOURCEID");
					try {insource=Integer.parseInt(ChangeOrigin);} catch (Exception e){}
						
					if(bAuthoritative=isAuthoritative(authoritativeDataSource,insource))
						break;
					
					isInstance = src.nextInstance();
				}
				
				if(bAuthoritative)
				{
					Hashtable allRules= new Hashtable(10,70.0f);
					String orphanSearchFilter="(CID=-*)";
					
					if(allRules!=null)
					{
						/***<Auto_><dependent_Source_Rules_>***/
							AllRules.put("1",new String[]{"rule1","rule2","rule3"});
							AllRules.put("2",new String[]{"rule1","rule2","rule3"});
							AllRules.put("4",new String[]{"rule1","rule2","rule3"});
							AllRules.put("8",new String[]{"rule1","rule2","rule3"});
						/***</dependent_Source_Rules_></Auto_>***/
					}
					
					vim.correlateOrphans(orphanSearchFilter,allRules,authoritativeDataSource,dest);
				}
			}
		
	}
	
	private boolean isAuthoritative(int[] authoritativeDataSource,int insource)
	{
		for(int i=0;i<authoritativeDataSource.length;i++)
			if(insource==authoritativeDataSource[i])
				return true;
			
		return false;
	}
	
	// <Main_> 
	public static void main(String[] args) {
	}
	// </Main_> 

	

	public void log(String message,int level)
	{
		UserDefineSyncUtils.log(this.getClass(),message,level);
	}
	
	public void log(Exception e)
	{
		UserDefineSyncUtils.log(this.getClass(),e);
	}



}
