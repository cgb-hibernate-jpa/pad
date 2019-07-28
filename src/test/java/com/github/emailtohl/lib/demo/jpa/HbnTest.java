package com.github.emailtohl.lib.demo.jpa;

import static org.junit.Assert.*;

import javax.persistence.EntityManager;
import javax.sql.DataSource;
import javax.transaction.UserTransaction;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.github.emailtohl.lib.entities.oauth2.OauthCode;

public class HbnTest {
	private TransactionManagerSetup TM;
	private HbnSetup hbn;

	@Before
	public void setUp() throws Exception {
		// 设置数据源和事务,默认DataSourceName是H2
		TM = new TransactionManagerSetup(DatabaseProduct.H2);
		hbn = new HbnSetup("H2", DatabaseProduct.H2);
	}

	@After
	public void tearDown() throws Exception {
		if (hbn != null) {
			hbn.entityManagerFactory.close();
		}
		if (TM != null) {
			TM.stop();
		}
	}
	
	@Test
	public void test() {
		DataSource dataSource = TM.getDataSource();
		assertNotNull(dataSource);
		
		UserTransaction ut = TM.getUserTransaction();
		assertNotNull(ut);
		
		EntityManager em = hbn.entityManagerFactory.createEntityManager();
		OauthCode a = new OauthCode();
		a.setCode("a token");
		a.setAuthentication("a token".getBytes());
		
		em.getTransaction().begin();
		em.persist(a);
		em.getTransaction().commit();
		
		a = em.find(OauthCode.class, a.getCode());
		assertNotNull(a);
	}

}
