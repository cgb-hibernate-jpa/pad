package com.github.emailtohl.lib.jpa;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import javax.transaction.Transactional;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.github.emailtohl.lib.config.TestEnvironment;
import com.github.emailtohl.lib.entities.oauth2.ClientDetails;
import com.github.emailtohl.lib.jpa.AuditedRepository.Snapshoot;
import com.github.emailtohl.lib.model.Item;

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
		List<Snapshoot<Item>> ls = itemAuditedRepo.getRevisions(purpleOutfit.getId());
		assertFalse(ls.isEmpty());
	}

	@Test
	public void testGetEntityAtRevision() {
		List<Snapshoot<Item>> ls = itemAuditedRepo.getRevisions(orangeOutfit.getId());
		ls.forEach(ss -> {
			Item item = itemAuditedRepo.getEntityAtRevision(orangeOutfit.getId(), ss.defaultRevisionEntity.getId());
			assertNotNull(item);
		});
	}

	@Test
	public void testRollback() {
		List<Snapshoot<ClientDetails>> ls = clientDetailsRepo.getRevisions(clientDetails.getAppId());
		ls.forEach(ss -> {
			ClientDetails rev = clientDetailsRepo.getEntityAtRevision(clientDetails.getAppId(), ss.defaultRevisionEntity.getId());
			clientDetailsRepo.rollback(rev.getAppId(), ss.defaultRevisionEntity.getId());
		});
	}

}
