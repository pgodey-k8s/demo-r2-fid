/*
 * Created on Jan 31, 2005
 * Transformation script from RadiantOne Virtual Identity Hub to DataStore   
 */
package com.rli.scripts.sync;


import com.rli.synsvcs.common.RliCon;
import com.rli.synsvcs.etl.BasicOperation;
import com.rli.synsvcs.etl.UserDefinedSync;
import com.rli.synsvcs.etl.UserDefineSyncUtils;
import com.rli.synsvcs.etl.VIH;
import com.rli.vds.util.XMLOperation; 
import java.util.Vector;
import com.rli.util.djava.ScriptHelper;
/***<User_><imports_>***/

/***</imports_></User_>***/

/**
 * Template to 'push' changes toward different datastores
 */
public class fromVIH implements UserDefinedSync {

	/* (non-Javadoc)
	 * @see com.rli.synsvcs.etl.UserDefinedSync#transform(com.rli.synsvcs.etl.BasicOperation, com.rli.synsvcs.etl.BasicOperation)
	 * defines a template for applying changes in the vim table to provision / modify dest
	 * minimal columns in vim table : VUID,SOURCEID,ACTIVE,REFDN,REFDNLO,CID
	 */
	
	private BasicOperation src=null;
	private BasicOperation dest=null;
	private String chgType=null;
	private XMLOperation authority=null;
		
	
	public void testTransform(BasicOperation src, BasicOperation dest,String opType) 
	{
		chgType=opType;
		transform(src, dest);
	}
	
	public void transform(BasicOperation src, BasicOperation dest) {
		
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
		String destRDN="$destRDN";
		String srcKeyname = "VUID";
		String destKeyname="$keyname";
		/***</Src_and_Dst_var_></Auto_>***/
	
		/*
		 *  vimNativeConnector = true : use native connector false: -use vds connector   
		 *  destNativeConnector = true : use native connector false: -use vds connector
		 */
		
		/***<Auto_><Dst_and_Vim_conector_type_>***/
		boolean vimNativeConnector=$vimNativeConnector; 
		boolean destNativeConnector=$destNativeConnector;
		/***</Dst_and_Vim_conector_type_></Auto_>***/
		
		int vimsourceid=256;		
		int datasourceid=0;
		/*
		 * enumerate possible sourceid numbers
		 * set datasourceid to appropriate one
		 * Change SourcesToProcess with appropriate ones
		 */
		
		// enumerate each possible datasourceid (1, 2, 4, 8, 16, 32, 64, 128...)
		/***<Auto_><data_Source_enumeration_>***/
			int sunone=1,ad=2,lotus=4, nt1=8,nt2=16; 
		/***</data_Source_enumeration_></Auto_>***/
			
		// set datasourceid of source object
		/***<Auto_><data_Source_setting_>***/
			datasourceid= $datasourceid;
			vimsourceid= $vimsourceid;
		/***</data_Source_setting_></Auto_>***/
		
		// set sourceid we are interested in processsing
		/***<Auto_><Source_processsing_>***/
			int SourcesToProcess= $SourcesToProcess;  // change it !!!
		/***</Source_processsing_></Auto_>***/
			
		/*
		 * indicate the list of Attributes we are interested in (column names from the vim table)  
		 */
		// example: mail and password
		/***<Auto_><Attributes_list_>***/
			String[] srcAttrlist={$srcAttrlist};		
		/***</Attributes_list_></Auto_>***/
			
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
		vim.setDataSourceinfo(destSyncObjectDN,datasourceid,destRDN,destKeyname,destNativeConnector,true);

		boolean isInstance = true;
		//  Message may be made of multiple instances, the script loops on each 
		while (isInstance == true)
		{
			authority= vim.buildAuthoritativeAttributes(src,dest,SourcesToProcess,srcAttrlist);
			
			dest.setValue(RliCon.CHANGE_MODE,RliCon.CHANGE_MODE_SYNCHRONIZED);
			
			if((src.getValue(VIH.CID)==null)||(src.getValue(VIH.CID).startsWith("-")))
				vim.setChangeType(RliCon.CHANGE_TYPE_ABORT);// modif sebastien PEHU 06/26/06
			
				//dest.setValue(RliCon.CHANGE_TYPE_ATTR_NAME,RliCon.CHANGE_TYPE_ABORT);
			
			
			if(chgType!=null)
				vim.setChangeType(this.chgType);
			
			if (vim.isChangeType(RliCon.CHANGE_TYPE_ABORT)==false)
			{
				String ChangeOrigin=src.getValue("SOURCEID");
				int insource=0;
				try {insource=Integer.parseInt(ChangeOrigin);} catch (Exception e){}
				
				/***<Auto_><OnAllOpType_>***/
					int []authoritativeDataSource= new int[]{};					
				
				/***</OnAllOpType_></Auto_>***/
						
				boolean isAuthority=isAuthoritative(authoritativeDataSource,insource);
					

				if(isAuthority)
				{
					if(src.getValue("ACTIVE").equals("0"))
						vim.setChangeType(RliCon.CHANGE_TYPE_DELETE);
				}
				else
				{
					dest.setValue(RliCon.CHANGE_MODE,RliCon.CHANGE_MODE_LOOKUP);
					vim.setChangeType(RliCon.CHANGE_TYPE_UPDATE);
				}

					
				if(chgType!=null)
					vim.setChangeType(this.chgType);
				
				if (vim.isChangeType(RliCon.CHANGE_TYPE_INSERT))
				{
					/***<Auto_><OnInsert_>***/
			
					/***</OnInsert_></Auto_>***/
				}
				if (vim.isChangeType(RliCon.CHANGE_TYPE_UPDATE))
				{
					/***<Auto_><OnUpdate_>***/
					
					/***</OnUpdate_></Auto_>***/
						
				}
				if (vim.isChangeType(RliCon.CHANGE_TYPE_DELETE))
				{		
					/***<Auto_><OnDelete_>***/
					
					/***</OnDelete_></Auto_>***/
					
				}
				setDestDN(vim);
			}
			// get next instance of the message...
			isInstance = src.nextInstance();
		}
		// end transform method
	}
	
	private boolean isAuthoritative(int[] authoritativeDataSource,int insource)
	{
		for(int i=0;i<authoritativeDataSource.length;i++)
			if(insource==authoritativeDataSource[i])
				return true;
			
		return false;
	}
	

	/***<Auto_><GeneratedFunctions_>***/{}

	/***</GeneratedFunctions_></Auto_>***/

	//<Main_>
	//</Main_>

	

	public void log(String message,int level)
	{
		UserDefineSyncUtils.log(this.getClass(),message,level);
	}
	
	public void log(Exception e)
	{
		UserDefineSyncUtils.log(this.getClass(),e);
	}



}
