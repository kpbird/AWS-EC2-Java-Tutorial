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
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.IpPermission;
import com.amazonaws.services.ec2.model.KeyPair;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.Tag;



public class Main {
	private Logger log = Logger.getInstance(Main.class);
	
	// require variable for every operation	
	private String accessKey = "YOUR ACCESS KEY";
	private String secretKey = "YOUR SECRET KEY" ;
	private AWSCredentials credentials;
	private String endPoint ;
	private Region region ;
	private AmazonEC2Client ec2client ;
	
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
	
	
	
	public static void main(String[] args) {
		Main m = new Main();
		m.init();
		m.createEC2SecurityGroup();
		m.createKeyPair();
		m.createEC2OnDemandInstance();
		m.createEC2SpotInstance();
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

}
