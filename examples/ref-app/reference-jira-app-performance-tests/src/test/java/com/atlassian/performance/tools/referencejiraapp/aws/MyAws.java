package com.atlassian.performance.tools.referencejiraapp.aws;

import com.amazonaws.auth.AWSCredentialsProviderChain;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Regions;
import com.atlassian.performance.tools.aws.api.Aws;
import com.atlassian.performance.tools.jiraperformancetests.api.ExplainingAwsCredentialsProvider;

import java.time.Duration;

public class MyAws {

    /**
     * The following AWS regions are not yet supported: EU_WEST_3, SA_EAST_1, GovCloud
     */
    public final Aws aws = new Aws.Builder(Regions.US_EAST_1)
        .credentialsProvider(
            new AWSCredentialsProviderChain(
                new DefaultAWSCredentialsProviderChain(),
                new ExplainingAwsCredentialsProvider(MyAws.class)
            )
        )
        .batchingCloudformationRefreshPeriod(Duration.ofSeconds(20))
        .build();
}
