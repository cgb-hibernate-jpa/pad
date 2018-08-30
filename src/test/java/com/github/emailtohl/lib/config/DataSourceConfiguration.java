package com.github.emailtohl.lib.config;

import javax.sql.DataSource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

@Configuration
@PropertySource({ "classpath:application.properties" })
class DataSourceConfiguration {
	private static final Logger LOG = LogManager.getLogger();
	/**
	 * 静态配置方法，该方法将在最早执行，这样才能读取properties配置
	 * 
	 * @return
	 */
	@Bean
	public static PropertySourcesPlaceholderConfigurer placeholderConfigurer() {
		return new PropertySourcesPlaceholderConfigurer();
	}

	@Bean
	public DataSource dataSource(Environment env) {
		String driverClassName = env.getProperty("spring.datasource.driverClassName", "org.h2.Driver");
		LOG.debug("driverClassName: {}", driverClassName);
		if ("org.h2.Driver".equalsIgnoreCase(driverClassName)) {
			return new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2)
					// .addScripts("classpath:test-data.sql")
					.build();
		} else {
			String url = env.getProperty("spring.datasource.url");
			String username = env.getProperty("spring.datasource.username");
			String password = env.getProperty("spring.datasource.password");
			LOG.debug("url={}  username={}  password: ********", url, username);
			// 创建连接池属性对象
			PoolProperties poolProps = new PoolProperties();
			poolProps.setUrl(url);
			poolProps.setDriverClassName(driverClassName);
			poolProps.setUsername(username);
			poolProps.setPassword(password);
			// 创建连接池, 使用了 tomcat 提供的的实现，它实现了 javax.sql.DataSource 接口
			org.apache.tomcat.jdbc.pool.DataSource dataSource = new org.apache.tomcat.jdbc.pool.DataSource();
			// 为连接池设置属性
			dataSource.setPoolProperties(poolProps);
			return dataSource;
		}
	}
}
