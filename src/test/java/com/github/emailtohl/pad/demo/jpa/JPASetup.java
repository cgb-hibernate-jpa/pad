package com.github.emailtohl.pad.demo.jpa;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

/**
 * Creates an EntityManagerFactory.
 * <p>
 * Configuration of the persistence units is taken from
 * <code>META-INF/persistence.xml</code> and other sources. Additional
 * <code>hbm.xml</code> file names can be given to the constructor.
 * </p>
 */
public class JPASetup {
	public final String persistenceUnitName;
	public final Map<String, String> properties = new HashMap<>();
	public final EntityManagerFactory entityManagerFactory;

	public JPASetup(DatabaseProduct databaseProduct, String persistenceUnitName, String... hbmResources) {

		this.persistenceUnitName = persistenceUnitName;

		// No automatic scanning by Hibernate, all persistence units list explicit
		// classes/packages
		properties.put("hibernate.archive.autodetection", "none");

		// Really the only way how we can get hbm.xml files into an explicit persistence
		// unit (where Hibernate scanning is disabled)
		properties.put("hibernate.hbmxml.files", String.join(",", hbmResources != null ? hbmResources : new String[0]));

		// We don't want to repeat these settings for all units in persistence.xml, so
		// they are set here programmatically
		properties.put("hibernate.format_sql", "true");
		properties.put("hibernate.use_sql_comments", "true");

		// Select database SQL dialect
		properties.put("hibernate.dialect", databaseProduct.hibernateDialect);

		entityManagerFactory = Persistence.createEntityManagerFactory(persistenceUnitName, properties);
	}

	public void createSchema() {
		generateSchema("create");
	}

	public void dropSchema() {
		generateSchema("drop");
	}

	public void generateSchema(String action) {
		// Take exiting EMF properties, override the schema generation setting on a copy
		Map<String, String> createSchemaProperties = new HashMap<>(properties);
		createSchemaProperties.put("javax.persistence.schema-generation.database.action", action);
		Persistence.generateSchema(persistenceUnitName, createSchemaProperties);
	}
}
