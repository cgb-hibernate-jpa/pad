package com.github.emailtohl.lib;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import javax.validation.Valid;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.github.emailtohl.lib.config.TestEnvironment;
import com.github.emailtohl.lib.exception.NotAcceptableException;
import com.github.emailtohl.lib.jpa.Paging;
import com.github.emailtohl.lib.model.Item;

public class StandardServiceTest extends TestEnvironment {
	@Autowired
	private ItemTestService itemTestService;

	@Before
	public void setUp() throws Exception {
		
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test(expected = NotAcceptableException.class)
	public void testValidate() {
		Item i = new Item();
		itemTestService.validate(i);
	}

	@Test
	public void testHasText() {
		assertFalse(itemTestService.hasText(null));
		assertFalse(itemTestService.hasText(""));
		assertTrue(itemTestService.hasText("abc"));
	}
	
	@Test
	public void testUserId() {
		itemTestService.setUserId(1L);
		assertEquals(Long.valueOf(1L), itemTestService.getUserId());
	}

}

@Service
class ItemTestService extends StandardService<Item, Long, Long> {

	@Override
	public Item create(@Valid Item entity) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Item read(Long id) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Paging<Item> query(Item example, Pageable pageable) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<Item> query(Item example) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Item update(Long id, @Valid Item newEntity) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void delete(Long id) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected Item toTransient(Item entity) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected Item transientDetail(Item entity) {
		throw new UnsupportedOperationException();
	}
}
