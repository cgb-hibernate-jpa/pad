package com.github.emailtohl.pad.jpa;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import javax.transaction.Transactional;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.github.emailtohl.pad.config.TestEnvironment;
import com.github.emailtohl.pad.entities.oauth2.ClientDetails;
import com.github.emailtohl.pad.jpa.AuditedRepository.RevTuple;
import com.github.emailtohl.pad.model.Item;

@Transactional
public class AuditedRepositoryTest extends TestEnvironment {
	@Autowired
	private ItemAuditedRepo itemAuditedRepo;
	@Autowired
	private ClientDetailsRepo clientDetailsRepo;
	@Autowired
	@Qualifier("purpleOutfit")
	private Item purpleOutfit;
	@Autowired
	@Qualifier("orangeOutfit")
	private Item orangeOutfit;
	@Autowired
	private ClientDetails clientDetails;
	
	@Before
	public void setUp() throws Exception {}

	@After
	public void tearDown() throws Exception {}

	@Test
	public void testGetRevisions() {
		List<RevTuple<Item>> ls = itemAuditedRepo.getRevisions(purpleOutfit.getId());
		assertFalse(ls.isEmpty());
	}

	@Test
	public void testGetEntityAtRevision() {
		List<RevTuple<Item>> ls = itemAuditedRepo.getRevisions(orangeOutfit.getId());
		ls.forEach(rt -> {
			Item item = itemAuditedRepo.getEntityAtRevision(orangeOutfit.getId(), rt.defaultRevisionEntity.getId());
			assertNotNull(item);
		});
	}

	@Test
	public void testRollback() {
		List<RevTuple<ClientDetails>> ls = clientDetailsRepo.getRevisions(clientDetails.getAppId());
		ls.forEach(rt -> {
			ClientDetails rev = clientDetailsRepo.getEntityAtRevision(clientDetails.getAppId(), rt.defaultRevisionEntity.getId());
			clientDetailsRepo.rollback(rev.getAppId(), rt.defaultRevisionEntity.getId());
		});
	}

}
