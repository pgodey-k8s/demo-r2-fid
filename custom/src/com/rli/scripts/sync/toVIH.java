package com.rli.scripts.sync;


import com.rli.synsvcs.etl.BasicOperation;
import com.rli.synsvcs.etl.UserDefinedSync;
import com.rli.synsvcs.etl.UserDefineSyncUtils;
import com.rli.synsvcs.etl.VIH;
import com.rli.synsvcs.common.RliCon;
import java.util.Vector;
import com.rli.util.djava.ScriptHelper;

/***<User_><imports_>***/

/***</imports_></User_>***/


/**
*	Template to feed a Virtual Identity Hub table  
*/
public class toVIH implements UserDefinedSync {

	/* (non-Javadoc)
	 * @see com.rli.synsvcs.etl.UserDefinedSync#transform(com.rli.synsvcs.etl.BasicOperation, com.rli.synsvcs.etl.BasicOperation)
	 * defines a template for loading / updating a vim table
	 * 11 to avoid problems of 'case' and other remapping, use all uppercase for column names
	 * also to hold authority level append STATUS to column name, without _ 
	 * minimal columns in vim table : VUID,SOURCEID,ACTIVE,REFDN,REFDNLO, CID
	 * VUID :(int) primary key
	 * SOURCEID :(int) each datastore has a bit set in sourceid (1 ,2 , 4, 8...)
	 * ACTIVE : (int) 1 : the row represents an existing entry in the source datastore.
	 * 				  0 : the row represents a deleted entry in the source datastore
	 * REFDN  : candidate key. DN that identifies one entry  in the source datastore
	 * REFDNLO: candidate key in lower case  (column must be indexed in the vim table)
	 * 			used for quick look up in the vim table. 
	 * CID:		common identifier - column built -must be declared indexed   
	 * 			- Represents the identifier that associate / join the different datastores
	 * 	other attributes that are recorded in vim table :		
	 * 	for each column in the vim table, an extra column is added <AttributeName>STATUS
	 *   		that contains an  authority level:
	 * 			0 : no authority
	 *			1 to 255 . The highest authority level  has the authoritative value.
	 *
	 * The function always Checks if the row already exists in the vim table,
	 * except if bulk load (initial upload) is detected.
	 * Standard insert, may find already the row (as we do not delete the row
	 * but set ACTIVE=0 ). Also, when detecting changes in the vim table (2 ways connector),
	 *  Actual row deletion in the vim table should not trigger any event (or should be disregarded )
	 *  To redo a bulk load (initial upload) , and we are not sure of what is already 
	 * in the vim table,delete all rows in the vim table, with the clause : 
	 * where sourceid= <<the sourceid of the data source>> . That will clean only the rows 
	 * from this sourceid , and not induce any other side effect.   				   
	 */
	 
	private BasicOperation src=null;
	private BasicOperation dest=null;
	private String chgType=null;
		
	 
	public void testTransform(BasicOperation src, BasicOperation dest,String opType) 
	{
		chgType=opType;
		transform(src, dest);
	}
	
	public void transform(BasicOperation src, BasicOperation dest) {
		
		this.src=src;
		this.dest=dest;
		
		/*
		 *  Replace $vimRDN    with RDN name (generally VIM table name same as syncobject name)
		 *  Replace $srcRDN with data store RDN name used in the syncobject view
		 *  Replace $keyname with the primary key name of the source object( "uid" for instance)
		 */
		
		/***<Auto_><Src_and_Dst_var_>***/
			String srcSyncObjectDN = "$srcSyncObjectDN";
			String destSyncObjectDN = "$destSyncObjectDN";
			String srcRDN="$srcRDN";		
			String destRDN = "$vimRDN";
			String srcKeyname="$keyname";
			String destKeyname = "VUID";
		/***</Src_and_Dst_var_></Auto_>***/

			
		/*
		 *   vimNativeConnector = true : use native connector false: -use vds connector   
		 *   srcNativeConnector = true : use native connector false: -use vds connector
		 */
			
		/***<Auto_><Src_and_Vim_conector_type_>***/
			boolean vimNativeConnector=$vimNativeConnector;  
			boolean srcNativeConnector=$srcNativeConnector;
		/***</Src_and_Vim_conector_type_></Auto_>***/
		
		int vimsourceid=256;		
		int datasourceid=0;		
		/*
		 *  enumerate possible sourceid numbers
		 *  set datasourceid to appropriate one
		 *  Change SourcesToProcess with appropriate ones
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
		
		/***<Auto_><vih_setting_>***/
			VIH vim= new VIH("$topologyName");
		/***</vih_setting_></Auto_>***/

		// Virtual Identity Hub information (object/table that stores identities and properties)		
		vim.setVIMinfo(destSyncObjectDN,vimsourceid,destRDN,destKeyname,vimNativeConnector);
		// DataSource Information setting : source of the change  
		vim.setDataSourceinfo(srcSyncObjectDN,datasourceid,srcRDN,srcKeyname,srcNativeConnector,false);
		boolean isInstance = true;

		vim.vdsConnect();

		//  Message may be made of multiple instances, the script loops on each 
		while (isInstance == true)
		{

			
			int mode=BasicOperation.COPY_NOTEMPTY;
			
			vim.processMessageInstance(src,dest);	
			
			
			/***<Auto_><OnAllOpType_>***/	

			/***</OnAllOpType_></Auto_>***/	
			
			if(chgType!=null)
				vim.setChangeType(this.chgType);
			
			if (vim.isChangeType(RliCon.CHANGE_TYPE_INSERT))
			{				
				/***<Auto_><OnInsert_>***/
				
				/***</OnInsert_></Auto_>***/
				
				/***<Auto_><CID_setting_>***/	

				/***</CID_setting_></Auto_>***/	
				
			}else
			if (vim.isChangeType(RliCon.CHANGE_TYPE_UPDATE))
			{
				/***<Auto_><OnUpdate_>***/

				/***</OnUpdate_></Auto_>***/
			}else
			if (vim.isChangeType(RliCon.CHANGE_TYPE_DELETE))
			{
				/***<Auto_><OnDelete_>***/

				/***</OnDelete_></Auto_>***/
			}
			
			// get next instance of the message...
			isInstance = src.nextInstance();
		}
		// end transform method
		vim.vdsDisconnect();
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
