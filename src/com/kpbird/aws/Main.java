package com.kpbird.aws;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
import com.amazonaws.services.ec2.model.CreateKeyPairRequest;
import com.amazonaws.services.ec2.model.CreateKeyPairResult;
import com.amazonaws.services.ec2.model.CreateSecurityGroupRequest;
import com.amazonaws.services.ec2.model.CreateSecurityGroupResult;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.DescribeSpotInstanceRequestsRequest;
import com.amazonaws.services.ec2.model.DescribeSpotInstanceRequestsResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.IpPermission;
import com.amazonaws.services.ec2.model.KeyPair;
import com.amazonaws.services.ec2.model.LaunchSpecification;
import com.amazonaws.services.ec2.model.RequestSpotInstancesRequest;
import com.amazonaws.services.ec2.model.RequestSpotInstancesResult;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.SpotInstanceRequest;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.rds.AmazonRDSClient;
import com.amazonaws.services.rds.model.AuthorizeDBSecurityGroupIngressRequest;
import com.amazonaws.services.rds.model.CreateDBInstanceRequest;
import com.amazonaws.services.rds.model.CreateDBParameterGroupRequest;
import com.amazonaws.services.rds.model.CreateDBSecurityGroupRequest;
import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.DBSecurityGroup;
import com.amazonaws.services.rds.model.DescribeDBInstancesRequest;
import com.amazonaws.services.rds.model.DescribeDBInstancesResult;
import com.amazonaws.services.rds.model.ModifyDBParameterGroupRequest;
import com.amazonaws.services.rds.model.Parameter;



public class Main {
	private Logger log = Logger.getInstance(Main.class);
	
	// require variable for every operation	
	private String accessKey = "YOUR ACCESS KEY";
	private String secretKey = "YOUR SECRET KEY" ;
	private AWSCredentials credentials;
	private String endPoint ;
	private Region region ;
	private AmazonEC2Client ec2client ;
	private AmazonRDSClient rdsclient;	
	
	// EC2 Security Group  Variables
	private String groupName = "kpbirdec2securitygroup";
	private String groupDescription = "This is description";
	
	private String sshIpRange = "YOUR PUBLIC IP/32";
	private String sshprotocol = "tcp";
	private int sshFromPort = 22;
	private int sshToPort =22;
	
	private String httpIpRange = "0.0.0.0/0";
	private String httpProtocol = "tcp";
	private int httpFromPort = 80;
	private int httpToPort = 80;
	
	private String httpsIpRange = "0.0.0.0/0";
	private String httpsProtocol = "tcp";
	private int httpsFromPort = 443;
	private int httpsToProtocol = 443;
	
	// KeyPair variables
	private String keyName = "kpbirdkeypair";
	private String pemFilePath  = "PATH TO STORE PEM FILE"; //   /Users/kpbird/Desktop
	private String pemFileName = "kpbird_keypair.pem";
	
	
	// EC2 Instance variables
	private String imageId ="ami-64084736";
	private String instanceType ="t1.micro";
	private String instanceName = "kpbirdt1micro";
	
	// EC2 Spot Instance
	private String spotPrice = "0.080";
	
	//RDS - MySql instance
	private String rdsengine = "MySQL";
	private String EngineVersion = "5.5.31";
	private String LicenseModel = "general-public-license";
	private boolean AutoMinorVersionUpgrade = true;
	private String DBInstanceClass ="db.t1.micro";
	private boolean MultiAZ =false;
	private int AllocatedStorage = 25;
	
	private String DBInstanceIdentifier = "kpbirdrdsmysql";
	private String MasterUsername = "kpbird_user";
	private String MasterUserPassword = "kpbird_pass";
	private String DBName = "kpbirddb";
	private int Port = 3306;
	private int BackupRetentionPeriod =1;
	private boolean PubliclyAccessible = true;
	
	// db security group parameters
	private String DBSecurityGroupName = "kpbirddbsecuritygroup";
	private String DBSsecurityGroupDescription = "this is db security group description";
	private String OwnerId = "YOUR OWNER ID";
	
	// db parameter group 
	private String DBParameterGroupName = "kpbirdrdsparametergroup";
	private String DBParameterGroupDescription = "this is db parameter group description";
	private String DBParameterGroupFamily = "mysql5.5";
	
	private String DBParameterName1 = "max_connections";
	private String DBParameterValue1 = "200";
	private String DBParameterApplyMethod1 = "immediate";
	
	private String DBParameterName2 = "max_allowed_packet";
	private String DBParameterValue2 = "33552384";
	private String DBParameterApplyMethod2 = "immediate";	
	
	public static void main(String[] args) {
		Main m = new Main();
		m.init();
		m.createEC2SecurityGroup();
		m.createKeyPair();
		m.createEC2OnDemandInstance();
		m.createEC2SpotInstance();
		m.createRDSSecurityGroup();
		m.createRDS();
		
	}
	
	private void init(){
		credentials  = new BasicAWSCredentials(accessKey, secretKey);
		// end point for singapore 
		endPoint = "https://rds.ap-southeast-1.amazonaws.com";
		// regions for singapore
		region = Region.getRegion(Regions.AP_SOUTHEAST_1);
		ec2client = new AmazonEC2Client(credentials);
		ec2client.setEndpoint(endPoint);
		ec2client.setRegion(region);
		// RDSClient object
		rdsclient = new AmazonRDSClient(credentials);
		rdsclient.setRegion(region);
		rdsclient.setEndpoint(endPoint);
		
		
	}
	
	private void createEC2SecurityGroup(){
		try {
			log.Info("Create Security Group Request");			
			CreateSecurityGroupRequest createSecurityGroupRequest =  new CreateSecurityGroupRequest();
			createSecurityGroupRequest.withGroupName(groupName).withDescription(groupDescription);
			createSecurityGroupRequest.setRequestCredentials(credentials);
			CreateSecurityGroupResult csgr = ec2client.createSecurityGroup(createSecurityGroupRequest);
			
			String groupid = csgr.getGroupId();
			log.Info("Security Group Id : " + groupid);
			
			log.Info("Create Security Group Permission");
			Collection<IpPermission> ips = new ArrayList<IpPermission>();
			// Permission for SSH only to your ip
			IpPermission ipssh = new IpPermission();
			ipssh.withIpRanges(sshIpRange).withIpProtocol(sshprotocol).withFromPort(sshFromPort).withToPort(sshToPort);
			ips.add(ipssh);
			
			// Permission for HTTP, any one can access
			IpPermission iphttp = new IpPermission();
			iphttp.withIpRanges(httpIpRange).withIpProtocol(httpProtocol).withFromPort(httpFromPort).withToPort(httpToPort);
			ips.add(iphttp);
			
			//Permission for HTTPS, any one can accesss
			IpPermission iphttps = new IpPermission();
			iphttps.withIpRanges(httpsIpRange).withIpProtocol(httpsProtocol).withFromPort(httpsFromPort).withToPort(httpsToProtocol);
			ips.add(iphttps);
			
			log.Info("Attach Owner to security group");
			// Register this security group with owner
			AuthorizeSecurityGroupIngressRequest authorizeSecurityGroupIngressRequest =	new AuthorizeSecurityGroupIngressRequest();
			authorizeSecurityGroupIngressRequest.withGroupName(groupName).withIpPermissions(ips);
			ec2client.authorizeSecurityGroupIngress(authorizeSecurityGroupIngressRequest);
			
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
	}
	
	private void createKeyPair(){
		try {
			CreateKeyPairRequest ckpr = new CreateKeyPairRequest();
			ckpr.withKeyName(keyName);
			
			CreateKeyPairResult ckpresult = ec2client.createKeyPair(ckpr);
			KeyPair keypair = ckpresult.getKeyPair();
			String privateKey = keypair.getKeyMaterial();
			log.Info("KeyPair :" + privateKey);
			writePemFile(privateKey,pemFilePath,pemFileName);	
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
	}
	
	private void createEC2OnDemandInstance(){
		try {
			
			// request for new on demand instance
			RunInstancesRequest rir = new RunInstancesRequest();
			rir.withImageId(imageId);
			rir.withInstanceType(instanceType);
			rir.withMinCount(1);
			rir.withMaxCount(1);
			rir.withKeyName(keyName);
			rir.withMonitoring(true);
			rir.withSecurityGroups(groupName);
			
			RunInstancesResult riresult = ec2client.runInstances(rir);
			log.Info(riresult.getReservation().getReservationId());
			
			/// Find newly created instance id
			String instanceId=null;
			DescribeInstancesResult result = ec2client.describeInstances();
			Iterator<Reservation> i = result.getReservations().iterator();
			while (i.hasNext()) {
				Reservation r = i.next();
				List<Instance> instances = r.getInstances();
				for (Instance ii : instances) {
					log.Info(ii.getImageId() + "\t" + ii.getInstanceId()+ "\t" + ii.getState().getName() + "\t"+ ii.getPrivateDnsName());
					if (ii.getState().getName().equals("pending")) {
						instanceId = ii.getInstanceId();
					}
				}
			}
			
			log.Info("New Instance ID :" + instanceId);
			/// Waiting for Instance Running////
			boolean isWaiting = true;
			while (isWaiting) {
				log.Info("*** Waiting ***");
				Thread.sleep(1000);
				DescribeInstancesResult r = ec2client.describeInstances();
				Iterator<Reservation> ir= r.getReservations().iterator();
				while(ir.hasNext()){
					Reservation rr = ir.next();
					List<Instance> instances = rr.getInstances();
					for(Instance ii : instances){
						log.Info(ii.getImageId() + "\t" + ii.getInstanceId()+ "\t" + ii.getState().getName() + "\t"+ ii.getPrivateDnsName());
						if (ii.getState().getName().equals("running") && ii.getInstanceId().equals(instanceId) ) {
							log.Info(ii.getPublicDnsName());
							isWaiting=false;
						}
					}
				}
			}
			
			/// Creating Tag for New Instance ////
			log.Info("Creating Tags for New Instance");
			CreateTagsRequest crt = new CreateTagsRequest();
			ArrayList<Tag> arrTag = new ArrayList<Tag>();
			arrTag.add(new Tag().withKey("Name").withValue(instanceName));
			crt.setTags(arrTag);
			
			ArrayList<String> arrInstances = new ArrayList<String>();
			arrInstances.add(instanceId);
			crt.setResources(arrInstances);
			ec2client.createTags(crt);
			
			
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
	}
	
	private void createEC2SpotInstance(){
		try {
			/// Creating Spot Instance ////
			
			// Initializes a Spot Instance Request
			RequestSpotInstancesRequest requestRequest = new RequestSpotInstancesRequest();
			// Request 1 x t1.micro instance with a bid price of $0.03.
			requestRequest.setSpotPrice(spotPrice);
			requestRequest.setInstanceCount(Integer.valueOf(1));
			LaunchSpecification launchSpecification = new LaunchSpecification();
			launchSpecification.setImageId(imageId);
			launchSpecification.setInstanceType(instanceType);
			launchSpecification.setMonitoringEnabled(true);
			
			// Add the security group to the request.
			ArrayList<String> securityGroups = new ArrayList<String>();
			securityGroups.add(groupName);
			launchSpecification.setSecurityGroups(securityGroups);
			
			launchSpecification.setKeyName(keyName);

			// Add the launch specifications to the request.
			requestRequest.setLaunchSpecification(launchSpecification);

			// Call the RequestSpotInstance API.
			RequestSpotInstancesResult requestResult = ec2client.requestSpotInstances(requestRequest);
			
			List<SpotInstanceRequest> requestResponses = requestResult.getSpotInstanceRequests();

			// Setup an arraylist to collect all of the request ids we want to
			// watch hit the running state.
			ArrayList<String> spotInstanceRequestIds = new ArrayList<String>();

			// Add all of the request ids to the hashset, so we can determine when they hit the
			// active state.
			for (SpotInstanceRequest requestResponse : requestResponses) {
			    System.out.println("Created Spot Request: "+requestResponse.getSpotInstanceRequestId());
			    spotInstanceRequestIds.add(requestResponse.getSpotInstanceRequestId());
			    log.Info(requestResponse.getInstanceId() + "\t" + requestResponse.getState());
			}
			
			String instanceId=null;
			boolean isWaiting=true;
			while(isWaiting){
				log.Info("*** Waiting for Spot Instance Request ***");
				Thread.sleep(5000);
				 DescribeSpotInstanceRequestsRequest describeRequest = new DescribeSpotInstanceRequestsRequest();
				 describeRequest.setSpotInstanceRequestIds(spotInstanceRequestIds);
				 
				 DescribeSpotInstanceRequestsResult describeResult = ec2client.describeSpotInstanceRequests(describeRequest);
			     List<SpotInstanceRequest> describeResponses = describeResult.getSpotInstanceRequests();
			     for (SpotInstanceRequest describeResponse : describeResponses) {
			    	 log.Info(describeResponse.getInstanceId() + "\t" + describeResponse.getState() + "\t" + describeResponse.getSpotInstanceRequestId() + "\t" + describeResponse.getStatus().getCode() + "\t" + describeResponse.getStatus().getMessage());
			        if (describeResponse.getState().equals("active")) {
			            isWaiting = false;
			            instanceId = describeResponse.getInstanceId();
			            break;
			        }
			     }
				
			}
			isWaiting = true;
			while (isWaiting) {
				log.Info("*** Waiting for Instance Running ***");
				Thread.sleep(1000);
				DescribeInstancesResult r = ec2client.describeInstances();
				Iterator<Reservation> ir= r.getReservations().iterator();
				while(ir.hasNext()){
					Reservation rr = ir.next();
					List<Instance> instances = rr.getInstances();
					for(Instance ii : instances){
						log.Info(ii.getImageId() + "\t" + ii.getInstanceId()+ "\t" + ii.getState().getName() + "\t"+ ii.getPrivateDnsName());
						if (ii.getState().getName().equals("running") && ii.getInstanceId().equals(instanceId) ) {
							log.Info(ii.getPublicDnsName());
							String publicDNS = ii.getPublicDnsName();
							log.Info("Public DNS :" + publicDNS);
							isWaiting=false;
						}
					}
				}
			}
			
			
			
			/// Creating Tag for New Instance ////
			log.Info("Creating Tags for New Instance");
			CreateTagsRequest crt = new CreateTagsRequest();
			ArrayList<Tag> arrTag = new ArrayList<Tag>();
			arrTag.add(new Tag().withKey("Name").withValue(instanceName));
			crt.setTags(arrTag);
			
			ArrayList<String> arrInstances = new ArrayList<String>();
			arrInstances.add(instanceId);
			crt.setResources(arrInstances);
			ec2client.createTags(crt);
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void writePemFile(String privateKey,String pemFilePath,String keyname){
		try {
			PrintWriter writer = new PrintWriter(pemFilePath + "/" + keyname + ".pem", "UTF-8");
			writer.print(privateKey);
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public void createRDSSecurityGroup(){
		try {
			
			log.Info("About to Launch RDS");
			
			CreateDBSecurityGroupRequest d = new CreateDBSecurityGroupRequest();
			d.setDBSecurityGroupName(DBSecurityGroupName);
			d.setDBSecurityGroupDescription(DBSsecurityGroupDescription);
			rdsclient.createDBSecurityGroup(d);
			
			
			AuthorizeDBSecurityGroupIngressRequest auth = new AuthorizeDBSecurityGroupIngressRequest();
			auth.setDBSecurityGroupName(DBSecurityGroupName);
			auth.setEC2SecurityGroupName(groupName);
			auth.setEC2SecurityGroupOwnerId(OwnerId);
			
			
			DBSecurityGroup dbsecuritygroup= rdsclient.authorizeDBSecurityGroupIngress(auth);
			
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
	}
	
	public void createRDS(){
		try {
			log.Info("About to Launch RDS");
			
			
			log.Info("Createing DB Parameger Group");
			
			CreateDBParameterGroupRequest cdpgr = new CreateDBParameterGroupRequest();
			cdpgr.setDBParameterGroupName(DBParameterGroupName);
			cdpgr.setDescription(DBParameterGroupDescription);
			cdpgr.setDBParameterGroupFamily(DBParameterGroupFamily);
			rdsclient.createDBParameterGroup(cdpgr);
			
			
			Collection<Parameter> parameters = new ArrayList<Parameter>();
			parameters.add( new Parameter()
            .withParameterName(DBParameterName1)
            .withParameterValue(DBParameterValue1)
            .withApplyMethod(DBParameterApplyMethod1));
			parameters.add( new Parameter()
            .withParameterName(DBParameterName2)
            .withParameterValue(DBParameterValue2)
            .withApplyMethod(DBParameterApplyMethod2));
			
			
			rdsclient.modifyDBParameterGroup( new ModifyDBParameterGroupRequest().withDBParameterGroupName(DBParameterGroupName).withParameters(parameters));
			
			
			log.Info("Create DB Instance Request");
			/// create configuration of instance
			CreateDBInstanceRequest cdbir = new CreateDBInstanceRequest();
			cdbir.setEngine(rdsengine);
			cdbir.setEngineVersion(EngineVersion);
			cdbir.setLicenseModel(LicenseModel);
			cdbir.setAutoMinorVersionUpgrade(AutoMinorVersionUpgrade);
			cdbir.setDBInstanceClass(DBInstanceClass);
			cdbir.setMultiAZ(MultiAZ);
			cdbir.setAllocatedStorage(AllocatedStorage);
			cdbir.setDBInstanceIdentifier(DBInstanceIdentifier);
			cdbir.setMasterUsername(MasterUsername);
			cdbir.setMasterUserPassword(MasterUserPassword);
			cdbir.setDBName(DBName);
			cdbir.setPort(Port);
			cdbir.setBackupRetentionPeriod(BackupRetentionPeriod);
			cdbir.setPubliclyAccessible(PubliclyAccessible);
			cdbir.setDBParameterGroupName(DBParameterGroupName);
			ArrayList<String> arrDbSecur = new ArrayList<String>();
			arrDbSecur.add(DBSecurityGroupName);
			cdbir.setDBSecurityGroups(arrDbSecur);
			
			log.Info("Creating RDS DB Instance");
			// creating instance
			DBInstance dbi=  rdsclient.createDBInstance(cdbir);
			
			// wait till instance created
			boolean isWaiting = true;
			while(isWaiting){
				Thread.sleep(5000);
				DescribeDBInstancesRequest request = new DescribeDBInstancesRequest();
				request.setDBInstanceIdentifier(dbi.getDBInstanceIdentifier());
				DescribeDBInstancesResult result = rdsclient.describeDBInstances(request);
				List<DBInstance> d= result.getDBInstances();
				Iterator<DBInstance> i = d.iterator();
				
				while(i.hasNext()){
					DBInstance d1 = i.next();
					log.Info("RDS Status : " + d1.getDBInstanceStatus());
					if(d1.getDBInstanceStatus().equals("available")){
						isWaiting = false;
						log.Info("RDS Endpoint : " +	d1.getEndpoint().getAddress());
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
	}
	

}
