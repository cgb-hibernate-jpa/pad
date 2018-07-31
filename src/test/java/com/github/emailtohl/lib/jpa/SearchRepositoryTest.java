package com.github.emailtohl.lib.jpa;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.transaction.Transactional;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.github.emailtohl.lib.config.TestEnvironment;
import com.github.emailtohl.lib.model.Bid;
import com.github.emailtohl.lib.model.Category;
import com.github.emailtohl.lib.model.Item;
import com.github.emailtohl.lib.model.Participator;

public class SearchRepositoryTest extends TestEnvironment {
	@Autowired
	private ItemSearchRepo itemSearchRepo;
	@Autowired
	@Qualifier("purpleOutfit")
	private Item purpleOutfit;
	@Autowired
	@Qualifier("purpleOutfitBid")
	private Bid purpleOutfitBid;
	@Autowired
	@Qualifier("foo")
	private Participator foo;
	@Autowired
	@Qualifier("sub")
	private Category sub;

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testSearchRepository() {
		SearchRepository<Item, Long> repo = new SearchRepository<Item, Long>() {};
		Set<String> onFields = new HashSet<String>(Arrays.asList(repo.onFields));
		assertTrue(onFields.contains("bids.name"));
		assertTrue(onFields.contains("images.name"));
		assertTrue(onFields.contains("seller.name"));
		assertTrue(onFields.contains("name"));
		assertTrue(onFields.contains("categories.name"));
		assertTrue(onFields.contains("auctionType"));
	}

	@Transactional
	@Test
	public void testSearchStringPageable() {
		Pageable pageable = PageRequest.of(0, 20);
		Page<Item> page = itemSearchRepo.search(purpleOutfitBid.getName(), pageable);
		assertTrue(page.getSize() > 0);
		page = itemSearchRepo.search(foo.getName(), pageable);
		assertTrue(page.getSize() > 0);
		page = itemSearchRepo.search(purpleOutfit.getName(), pageable);
		assertTrue(page.getSize() > 0);
	}

	@Transactional
	@Test
	public void testSearchString() {
		List<Item> ls = itemSearchRepo.search(sub.getName());
		assertTrue(ls.size() > 0);
		ls = itemSearchRepo.search(purpleOutfit.getAuctionType().toString());
		assertTrue(ls.size() > 0);
	}

}
