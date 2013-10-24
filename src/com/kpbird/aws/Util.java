package com.kpbird.aws;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeImagesRequest;
import com.amazonaws.services.ec2.model.DescribeImagesResult;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.DescribeTagsResult;
import com.amazonaws.services.ec2.model.Image;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.TagDescription;

public class Util {

	public static String getAmiIdFromName(String aminame,AWSCredentials awscredentials,String endPoint, Region region){
		String amiid=null;
		try {
			
			AmazonEC2Client ec2 = new AmazonEC2Client(awscredentials);
			ec2.setEndpoint(endPoint);
			ec2.setRegion(region);

			DescribeImagesRequest request = new DescribeImagesRequest();
			ArrayList<String> o = new ArrayList<String>();
			o.add("self");
			request.setOwners(o);
			DescribeImagesResult result = ec2.describeImages(request);
			List<Image> arrImages= result.getImages();
			Iterator<Image> iArr= arrImages.iterator();
			while(iArr.hasNext()){
				Image i = iArr.next();
				System.out.println(i.getImageId() + "\t" + i.getDescription() + "\t" + i.getName() );
				if(i.getName().equals(aminame)){
					amiid = i.getImageId();
					break;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return amiid;
	}
	
	public static String getAvailabilityZone(String instanceId,AWSCredentials awscredentials,String endPoint, Region region){
		String azone = null;
		try {
			AmazonEC2Client ec2 = new AmazonEC2Client(awscredentials);
			ec2.setEndpoint(endPoint);
			ec2.setRegion(region);
			
			DescribeInstancesResult result = ec2.describeInstances();
			List<Reservation> lst= result.getReservations();
			Iterator<Reservation> ir = lst.iterator();
			while(ir.hasNext()){
				Reservation r = ir.next();
				List<com.amazonaws.services.ec2.model.Instance> arrInstances =  r.getInstances();
				for(com.amazonaws.services.ec2.model.Instance instance : arrInstances){
					System.out.println();
					System.out.println(instance.getImageId());
					System.out.println(instance.getInstanceId());
					System.out.println(instance.getPlacement().getAvailabilityZone());
					System.out.println(instance.getTags().get(0).getValue());
					if(instance.getInstanceId().equals(instanceId)){
						azone = instance.getPlacement().getAvailabilityZone();
						break;
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return azone; 
	}
	
	public static String getInstanceId(String instanceName,AWSCredentials awscredentials,String endPoint, Region region){
		String instanceId = null;
		AmazonEC2Client ec2 = new AmazonEC2Client(awscredentials);
		ec2.setEndpoint(endPoint);
		ec2.setRegion(region);
		DescribeTagsResult result =  ec2.describeTags();
		List<TagDescription> lstTags= result.getTags();
		Iterator<TagDescription> iTag = lstTags.iterator();
		while(iTag.hasNext()){
			TagDescription td =  iTag.next();
			System.out.println(td.getKey() +"\t" + td.getValue() + "\t" + td.getResourceId() + "\t" + td.getResourceType());
			if(td.getValue().equals(instanceName)){
				instanceId = td.getResourceId();
				break;
			}
		}
		return instanceId;
	}
	
}
