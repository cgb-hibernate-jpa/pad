package com.github.emailtohl.pad.demo.jpa;

import java.util.Properties;

import javax.persistence.EntityManagerFactory;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.service.ServiceRegistry;

import com.github.emailtohl.pad.entities.oauth2.OauthCode;

/**
 * Creates an EntityManagerFactory.
 */
public class HbnSetup {
	public final Properties properties = new Properties();
	public final EntityManagerFactory entityManagerFactory;

	public HbnSetup(String dataSourceName, DatabaseProduct databaseProduct, String... hbmResources) {
		ServiceRegistry standardRegistry = new StandardServiceRegistryBuilder()
//			.applySetting("hibernate.connection.driver_class", "org.h2.Driver")
//			.applySetting("hibernate.connection.url", "jdbc:h2:mem:db1;DB_CLOSE_DELAY=-1;MVCC=TRUE")
//			.applySetting("hibernate.connection.username", "sa")
//			.applySetting("hibernate.connection.password", "")
			.applySetting("hibernate.connection.datasource", dataSourceName)
			.applySetting("hibernate.dialect", databaseProduct.hibernateDialect)
			.applySetting("hibernate.hbm2ddl.auto", "create-drop")
			.applySetting("hibernate.show_sql", "true")
			.applySetting("hibernate.format_sql", "true")
			.applySetting("hibernate.use_sql_comments", "true")
			.applySetting("hibernate.hbmxml.files", String.join(",", hbmResources != null ? hbmResources : new String[0]))
			.build();
		Metadata metadata = new MetadataSources(standardRegistry)
			.addAnnotatedClass(OauthCode.class)
			.getMetadataBuilder()
			.applyImplicitNamingStrategy(ImplicitNamingStrategyJpaCompliantImpl.INSTANCE)
			.build();
		entityManagerFactory = metadata.getSessionFactoryBuilder().build();
	}

}
