package com.github.emailtohl.lib.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.sql.DataSource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.hibernate5.HibernateExceptionTranslator;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * JPA相关配置
 * 
 * @author HeLei
 */
@Configuration
@Import(DataSourceConfiguration.class)
@EnableJpaRepositories(basePackages = "com.github.emailtohl.lib", repositoryImplementationPostfix = "Impl", transactionManagerRef = "annotationDrivenTransactionManager", entityManagerFactoryRef = "entityManagerFactory")
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
class JpaConfiguration {
	private static final Logger LOG = LogManager.getLogger();

	@Bean
	public JpaVendorAdapter jpaVendorAdapter(Environment env) {
		HibernateJpaVendorAdapter adapter = new HibernateJpaVendorAdapter();
		String driverClassName = env.getProperty("spring.datasource.driverClassName", "org.h2.Driver");
		switch (driverClassName) {
		case "org.h2.Driver":
			adapter.setDatabase(Database.H2);
			adapter.setDatabasePlatform("org.hibernate.dialect.H2Dialect");
			break;
		case "org.postgresql.Driver":
			adapter.setDatabase(Database.POSTGRESQL);
			adapter.setDatabasePlatform("org.hibernate.dialect.PostgreSQL94Dialect");
			break;
		case "com.mysql.jdbc.Driver":
			adapter.setDatabase(Database.MYSQL);
			adapter.setDatabasePlatform("org.hibernate.dialect.MySQL57InnoDBDialect");
			break;
		default:
			String cause = "Only postgresql and mysql are supported";
			LOG.debug(cause);
			throw new IllegalArgumentException(cause);
		}
		return adapter;
	}

	@Bean
	public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource,
			JpaVendorAdapter jpaVendorAdapter, Environment env) {
		LocalContainerEntityManagerFactoryBean emfb = new LocalContainerEntityManagerFactoryBean();
		emfb.setDataSource(dataSource);
		emfb.setJpaVendorAdapter(jpaVendorAdapter);
		// hibernate可以扫描类路径下有JPA注解的实体类，但是JPA规范并没有此功能，所以最好还是告诉它实际所在位置
		emfb.setPackagesToScan("com.github.emailtohl.lib.entities", "com.github.emailtohl.lib.model");
		String hibernate_hbm2ddl_auto = env.getProperty("spring.jpa.properties.hibernate.hbm2ddl.auto", "update");
		String showSql = env.getProperty("spring.jpa.show-sql", "true");
		String formatSql = env.getProperty("spring.jpa.format_sql", "true");
		String generateDddl = env.getProperty("spring.jpa.generate-ddl", "true");
		// hibernate.search.default.directory_provider默认是filesystem
		// 设置hibernate.search.default.indexBase可指定索引目录
		String searchDirectoryProvider = env
				.getProperty("spring.jpa.properties.hibernate.search.default.directory_provider", "filesystem");
		LOG.debug(
				"hibernate.hbm2ddl.auto={}  hibernate.show_sql={}  hibernate.format_sql={} hibernate.generate-ddl={} hibernate.search.default.directory_provider={}",
				hibernate_hbm2ddl_auto, showSql, formatSql, generateDddl, searchDirectoryProvider);

		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put("hibernate.hbm2ddl.auto", hibernate_hbm2ddl_auto);
		properties.put("hibernate.show_sql", showSql);
		properties.put("hibernate.format_sql", formatSql);
		properties.put("hibernate.generate-ddl", generateDddl);
		if ("filesystem".equalsIgnoreCase(searchDirectoryProvider)) {// 使用内存数据库一般是测试环境，可以使用内存来做索引的存储空间
			String indexBase = env.getProperty("", "lucene_index");
			properties.put("hibernate.search.default.indexBase", indexBase);
			LOG.debug("hibernate.search.default.indexBase={}", indexBase);
		} else if ("local-heap".equalsIgnoreCase(searchDirectoryProvider) || "ram".equalsIgnoreCase(searchDirectoryProvider)) {
			properties.put("hibernate.search.default.directory_provider", searchDirectoryProvider);
		}
		emfb.setJpaPropertyMap(properties);
		return emfb;
	}

	/*
	 * 默认情况下，Spring总是使用ID为annotationDrivenTransactionManager的事务管理器
	 * 若要自定义提供事务管理器则需实现TransactionManagementConfigurer接口
	 * 若没有实现接口TransactionManagementConfigurer，
	 * 且事务管理器的名字不是默认的annotationDrivenTransactionManager，
	 * 可在注解 @Transactional的value指定。
	 */
	@Bean(name = "annotationDrivenTransactionManager")
	public PlatformTransactionManager jpaTransactionManager(LocalContainerEntityManagerFactoryBean bean) {
		return new JpaTransactionManager(bean.getObject());
	}

	@Bean
	public PersistenceExceptionTranslator persistenceExceptionTranslator() {
		return new HibernateExceptionTranslator();
	}

	@Bean(name = "auditorAware")
	public AuditorAware<String> auditorAwareImpl() {
		// getCurrentAuditor
		return () -> {
			String name = "anonymous";
			return Optional.<String>of(name);
		};
	}
}
