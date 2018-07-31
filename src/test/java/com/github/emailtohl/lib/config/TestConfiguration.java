package com.github.emailtohl.lib.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.TransactionManagementConfigurer;

@Configuration
@Import({ JpaConfiguration.class, Entities.class })
@ComponentScan(basePackages = "com.github.emailtohl.lib", excludeFilters = @ComponentScan.Filter({ Controller.class,
		Configuration.class }))
class TestConfiguration implements TransactionManagementConfigurer {
	@Autowired
	PlatformTransactionManager platformTransactionManager;

	@Override
	public PlatformTransactionManager annotationDrivenTransactionManager() {
		return platformTransactionManager;
	}

}
