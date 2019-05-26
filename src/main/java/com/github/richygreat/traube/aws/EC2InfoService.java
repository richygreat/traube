package com.github.richygreat.traube.aws;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.autoscaling.AmazonAutoScaling;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClientBuilder;
import com.amazonaws.services.autoscaling.model.AutoScalingInstanceDetails;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingInstancesResult;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.util.EC2MetadataUtils;
import com.github.richygreat.traube.param.model.AwsParams;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class EC2InfoService {
	public String getCurrentInstancePrivateIp() {
		return EC2MetadataUtils.getPrivateIpAddress();
	}

	public List<String> getAsgInstances(AwsParams awsParams) {
		List<String> asgInstances = new ArrayList<>();
		BasicAWSCredentials awsCreds = new BasicAWSCredentials(awsParams.getAccessId(), awsParams.getSecretKey());
		AWSStaticCredentialsProvider credentialsProvider = new AWSStaticCredentialsProvider(awsCreds);

		String curInstanceId = EC2MetadataUtils.getInstanceId();
		log.info("curInstanceId: {}", curInstanceId);

		AmazonAutoScaling amazonAutoScaling = AmazonAutoScalingClientBuilder.standard()
				.withCredentials(credentialsProvider).build();
		DescribeAutoScalingInstancesResult result = amazonAutoScaling.describeAutoScalingInstances();

		Optional<AutoScalingInstanceDetails> optionalAsgCurrentInstance = result.getAutoScalingInstances().stream()
				.filter(asg -> curInstanceId.equals(asg.getInstanceId())).findFirst();

		log.info("optionalAsgCurrentInstance: {}", optionalAsgCurrentInstance);

		String asgName = optionalAsgCurrentInstance.get().getAutoScalingGroupName();
		log.info("asgName: {}", asgName);
		List<String> instanceIds = result.getAutoScalingInstances().stream()
				.filter(asg -> asgName.equals(asg.getAutoScalingGroupName()))
				.map(AutoScalingInstanceDetails::getInstanceId).collect(Collectors.toList());
		log.info("instanceIds: {}", instanceIds);

		AmazonEC2 amazonEC2 = AmazonEC2ClientBuilder.standard().withCredentials(credentialsProvider).build();
		DescribeInstancesRequest describeInstancesRequest = new DescribeInstancesRequest();
		describeInstancesRequest.setInstanceIds(instanceIds);
		DescribeInstancesResult instancesResult = amazonEC2.describeInstances(describeInstancesRequest);
		log.info("instancesResultSize: {}", instancesResult.getReservations().size());

		asgInstances = instancesResult.getReservations().stream()
				.map(res -> res.getInstances().get(0).getPrivateIpAddress()).collect(Collectors.toList());
		log.info("asgInstances: {}", asgInstances);
		return asgInstances;
	}
}
