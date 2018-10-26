package com.github.emailtohl.lib.jpa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import javax.persistence.AccessType;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.emailtohl.lib.config.TestEnvironment;
import com.github.emailtohl.lib.entities.session.SessionForm;
import com.github.emailtohl.lib.entities.session.SpringSession;
import com.github.emailtohl.lib.entities.session.SpringSessionAttributes;
import com.github.emailtohl.lib.model.Address;
import com.github.emailtohl.lib.model.AuctionType;
import com.github.emailtohl.lib.model.Category;
import com.github.emailtohl.lib.model.Image;
import com.github.emailtohl.lib.model.Item;
import com.github.emailtohl.lib.model.Participator;
import com.github.emailtohl.lib.util.LocalDateUtil;

public class QueryRepositoryTest extends TestEnvironment {
	private ObjectMapper om = new ObjectMapper();
	@Autowired
	private Item purpleOutfit;
	@Autowired
	private ItemRepo itemRepo;
	@Autowired
	private ParticipatorRepo participatorRepo;
	@Autowired
	private SpringSessionRepo springSessionRepo;
	@Autowired
	private EntityManagerFactory factory;

	@Before
	public void setUp() throws Exception {

	}

	@After
	public void tearDown() throws Exception {
	}
	
	@Test
	public void testQueryForPage() throws JsonProcessingException {
		Pageable pageable = PageRequest.of(0, 20);
		Page<Item> page = itemRepo.queryForPage(null, pageable);
		assertTrue(page.getSize() > 0);
		// 自定义查询条件
		class QueryItem extends Item {
			private static final long serialVersionUID = 5903837845910494789L;
			@Instruction(propertyName = "auctionType", operator = Operator.IN)
			public AuctionType[] in = new AuctionType[] {AuctionType.HIGHEST_BID, AuctionType.LOWEST_BID};
			@Instruction(propertyName = "auctionEnd", operator = Operator.LTE)
			public Date auctionEndLte = LocalDateUtil.toDate(LocalDate.now().plusDays(100));
		}
		QueryItem example = new QueryItem();
		example.setCreatedOn(purpleOutfit.getCreatedOn());
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
		
		page = itemRepo.queryForPage(example, pageable);
		assertTrue(page.getSize() > 0);
	}

	@Test
	public void testQueryForList() throws JsonProcessingException {
		List<Item> ls = itemRepo.queryForList(null);
		assertTrue(ls.size() > 0);
		
		Item example = new Item();
		example.setCreatedOn(purpleOutfit.getCreatedOn());
		example.setApproved(true);
		example.setBuyNowPrice(new BigDecimal(500.00));
		
		Address fooAddress = new Address("street1", "12345", "city");
		Participator foo = new Participator("foo");
		foo.setHomeAddress(fooAddress);
		example.setSeller(foo);
		Category sup = new Category("super"), sub = new Category("sub", sup);
		
		// 循环引用
		example.getCategories().add(sub);
		sub.getItems().add(example);
		
		Image purpleOutfit1 = new Image("purpleOutfit1", "/var/image/purpleOutfit1", 160, 80);
		example.getImages().add(purpleOutfit1);
		
		ls = itemRepo.queryForList(example);
		assertTrue(ls.size() > 0);
		
		// 测试loginNames为空的时候
		Participator user = new Participator();
		List<Participator> users = participatorRepo.queryForList(user);
		assertFalse(users.isEmpty());
	}
	
	/**
	 * 最常见的关联关系的查找测试，包括相互引用
	 * @throws JsonProcessingException
	 */
	@Test
	public void testGetPredicates1() throws JsonProcessingException {
		EntityManager em = factory.createEntityManager();
		em.getTransaction().begin();
		CriteriaBuilder b = em.getCriteriaBuilder();
		CriteriaQuery<Item> q = b.createQuery(Item.class);
		Root<Item> r = q.from(Item.class);
		Item example = (Item) purpleOutfit.clone();
		Set<Predicate> predicates = itemRepo.getPredicates(example, r, b);
		assertTrue(predicates.size() > 0);
		
		Predicate[] restriction = new Predicate[predicates.size()];
		q = q.select(r).where(predicates.toArray(restriction));
		List<Item> ls = em.createQuery(q).getResultList();
		System.out.println(om.writeValueAsString(ls));
		
		em.getTransaction().commit();
		em.close();
	}
	
	/**
	 * 具有嵌入式Id类型，以及自定义比较的测试，此处Id字段存在反过来引用的情况
	 * @throws JsonProcessingException
	 */
	@Test
	public void testGetPredicates2() throws JsonProcessingException {
		EntityManager em = factory.createEntityManager();
		em.getTransaction().begin();
		CriteriaBuilder b = em.getCriteriaBuilder();
		CriteriaQuery<SpringSession> q = b.createQuery(SpringSession.class);
		Root<SpringSession> r = q.from(SpringSession.class);
		Random ran = new Random();
		SpringSession springSession = new SpringSession();
		springSession.setSessionId(UUID.randomUUID().toString());
		springSession.setCreationTime(System.currentTimeMillis());
		springSession.setExpiryTime(System.currentTimeMillis() + 1000 * 3600);
		springSession.setLastAccessTime(System.currentTimeMillis() + 1000);
		springSession.setMaxInactiveInterval(1000);
		springSession.setPrimaryId(Math.abs(ran.nextInt()) + "");
		springSession.setPrincipalName("foo");

		SpringSessionAttributes attr = new SpringSessionAttributes();
		attr.setAttributeBytes("WORLD".getBytes());
		SpringSessionAttributes.Id id = new SpringSessionAttributes.Id();
		id.setAttributeName("HELLO");
		// id作为SpringSessionAttributes的属性反过来依赖springSession
		id.setSpringSession(springSession);
		attr.setId(id);
		
		springSession.getSpringSessionAttributes().add(attr);

		em.persist(springSession);
		em.persist(attr);

		em.getTransaction().commit();

		Set<Predicate> predicates = springSessionRepo.getPredicates(springSession, r, b);
		assertTrue(predicates.size() > 0);
		
		Predicate[] restrictions = new Predicate[predicates.size()];
		q = q.select(r).where(predicates.toArray(restrictions));
		q.select(r).where(restrictions);
		List<SpringSession> ls = em.createQuery(q).getResultList();
		assertEquals(1, ls.size());
		assertEquals(springSession, ls.get(0));
		System.out.println(om.writeValueAsString(ls));
		
		q = b.createQuery(SpringSession.class);
		r = q.from(SpringSession.class);
		SessionForm form = new SessionForm();
		// maxInactiveInterval属性被设置了@InitialValueAsCondition注解，即便是初始值0也会作为条件参数
		// 所以这里就需要传入一个查询条件
		form.setMaxInactiveInterval(springSession.getMaxInactiveInterval());
		form.setCreationTime(springSession.getCreationTime());
		predicates = springSessionRepo.getPredicates(form, r, b);
		assertTrue(predicates.size() > 0);
		Predicate[] restriction = new Predicate[predicates.size()];
		q = q.select(r).where(predicates.toArray(restriction));
		ls = em.createQuery(q).getResultList();
		assertEquals(1, ls.size());
		assertEquals(springSession, ls.get(0));
		
		em.remove(springSession);
		em.close();
	}
	
	@Test
	public void testEmptyAndNull() {
		class ItemForm extends Item {
			private static final long serialVersionUID = 7909725823129616068L;
			@Instruction(propertyName = "bids", operator = Operator.EMPTY)
			public short empty;
			@Instruction(propertyName = "bids", operator = Operator.NOT_EMPTY)
			public short notEmpty;
			@Instruction(propertyName = "seller", operator = Operator.NULL)
			public short _null;
			@Instruction(propertyName = "seller", operator = Operator.NOT_NULL)
			public short notNull;
			@ExcludeCondition
			public double excludeFile = 1.1;
			@ExcludeCondition
			public double getExcludeFileProp() {
				return 1.1 * 1.2;
			}
			@SuppressWarnings("unused")
			public void setExcludeFileProp(double empty) {
			}
		}
		ItemForm form = new ItemForm();
		form.setCreatedOn(null);
		form.setApproved(false);
		form.setAuctionType(null);
		
		EntityManager em = factory.createEntityManager();
		em.getTransaction().begin();
		CriteriaBuilder b = em.getCriteriaBuilder();
		
		form.empty = 1;
		CriteriaQuery<Item> q = b.createQuery(Item.class);
		Root<Item> r = q.from(Item.class);
		Set<Predicate> predicates = itemRepo.getPredicates(form, r, b);
		assertTrue(predicates.size() > 0);
		Predicate[] restrictions = new Predicate[predicates.size()];
		q = q.select(r).where(predicates.toArray(restrictions));
		q.select(r).where(restrictions);
		List<Item> ls = em.createQuery(q).getResultList();
		assertTrue(ls.isEmpty());
		
		form.empty = 0;
		form.notEmpty = 1;
		q = b.createQuery(Item.class);
		r = q.from(Item.class);
		predicates = itemRepo.getPredicates(form, r, b);
		assertTrue(predicates.size() > 0);
		restrictions = new Predicate[predicates.size()];
		q = q.select(r).where(predicates.toArray(restrictions));
		q.select(r).where(restrictions);
		ls = em.createQuery(q).getResultList();
		assertFalse(ls.isEmpty());
		
		form.notEmpty = 0;
		form.notNull = 1;
		q = b.createQuery(Item.class);
		r = q.from(Item.class);
		predicates = itemRepo.getPredicates(form, r, b);
		assertTrue(predicates.size() > 0);
		restrictions = new Predicate[predicates.size()];
		q = q.select(r).where(predicates.toArray(restrictions));
		q.select(r).where(restrictions);
		ls = em.createQuery(q).getResultList();
		System.out.println(ls);
		assertFalse(ls.isEmpty());
		
		form.notNull = 0;
		form._null = 1;
		q = b.createQuery(Item.class);
		r = q.from(Item.class);
		predicates = itemRepo.getPredicates(form, r, b);
		assertTrue(predicates.size() > 0);
		restrictions = new Predicate[predicates.size()];
		q = q.select(r).where(predicates.toArray(restrictions));
		q.select(r).where(restrictions);
		ls = em.createQuery(q).getResultList();
		assertTrue(ls.isEmpty());
		
		em.getTransaction().commit();
		em.close();
	}
	
	@Test
	public void testAvailableObj() {
		Object o = new Date();
		assertTrue(itemRepo.availableObj(o));
		o = 1;
		assertTrue(itemRepo.availableObj(o));
		o = true;
		assertTrue(itemRepo.availableObj(o));
		o = AccessType.FIELD;
		assertTrue(itemRepo.availableObj(o));
		o = "HELLO WORLD";
		assertTrue(itemRepo.availableObj(o));
	}

	@Test
	public void testAvailableCollection() {
		Set<String> set = new HashSet<>();
		assertFalse(itemRepo.availableCollection(set));
		set.add("HELLO");
		assertTrue(itemRepo.availableCollection(set));
		int[] arr = new int[0];
		assertFalse(itemRepo.availableCollection(arr));
		arr = new int[1];
		assertTrue(itemRepo.availableCollection(arr));
	}
	
	@Test
	public void testIgnorePrimitive() {
		assertFalse(itemRepo.ignorePrimitive(Object.class, new Object()));
		assertFalse(itemRepo.ignorePrimitive(int.class, 1));
		assertTrue(itemRepo.ignorePrimitive(int.class, 0));
		assertFalse(itemRepo.ignorePrimitive(long.class, 1L));
		assertTrue(itemRepo.ignorePrimitive(long.class, 0L));
		assertFalse(itemRepo.ignorePrimitive(double.class, 1.1));
		assertTrue(itemRepo.ignorePrimitive(double.class, 0.0));
		assertFalse(itemRepo.ignorePrimitive(float.class, 1.1f));
		assertTrue(itemRepo.ignorePrimitive(float.class, 0.0f));
		assertFalse(itemRepo.ignorePrimitive(short.class, (short) 1));
		assertTrue(itemRepo.ignorePrimitive(short.class, (short) 0));
		assertFalse(itemRepo.ignorePrimitive(boolean.class, true));
		assertTrue(itemRepo.ignorePrimitive(boolean.class, false));
		assertFalse(itemRepo.ignorePrimitive(byte.class, (byte) 1));
		assertTrue(itemRepo.ignorePrimitive(byte.class, (byte) 0));
		assertFalse(itemRepo.ignorePrimitive(char.class, 'A'));
		assertTrue(itemRepo.ignorePrimitive(char.class, (char) 0));
	}
	
	@Test
	public void testToCollection() {
		List<Integer> ls = new ArrayList<>();
		ls.add(1);
		ls.add(2);
		assertEquals(ls, itemRepo.toCollection(ls));
		
		Integer[] arr = {1, 2};
		System.out.println(itemRepo.toCollection(arr));
	}

}
