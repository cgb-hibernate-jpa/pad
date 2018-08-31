package com.github.emailtohl.lib.jpa;

import java.math.BigDecimal;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.github.emailtohl.lib.model.Address;
import com.github.emailtohl.lib.model.Category;
import com.github.emailtohl.lib.model.Image;
import com.github.emailtohl.lib.model.Item;
import com.github.emailtohl.lib.model.Participator;

public class JsonBuilderTest {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testBuild() {
		Item example = new Item();
		example.setApproved(true);
		example.setBuyNowPrice(new BigDecimal(500.00));
		
		Address fooAddress = new Address("street1", "12345", "city");
		Participator foo = new Participator("foo");
		foo.setHomeAddress(fooAddress);
		foo.getLoginNames().add("foo");
		foo.getLoginNames().add("foo@localhost");
		example.setSeller(foo);
		Category sup = new Category("super"), sub = new Category("sub", sup);
		
		// 循环引用
		example.getCategories().add(sub);
		sub.getItems().add(example);
		
		Image purpleOutfit1 = new Image("purpleOutfit1", "/var/image/purpleOutfit1", 160, 80);
		example.getImages().add(purpleOutfit1);
		
		String json = JsonBuilder.build(example);
		System.out.println(json);
/*		
		ObjectMapper om = new ObjectMapper();
		Item item = om.convertValue(json, Item.class);
		System.out.println(item);*/
	}

}
