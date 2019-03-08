package com.github.emailtohl.lib.jpa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Basic;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.JoinTable;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToMany;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.github.emailtohl.lib.model.AuctionType;
import com.github.emailtohl.lib.model.Category;
import com.github.emailtohl.lib.model.Item;
import com.github.emailtohl.lib.util.LocalDateUtil;

public class EntityInspectorTest {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}
	
	@Test
	public void testIsEntity() {
		@Entity
		class Foo {
			@Id
			private int id;
		}
		@Embeddable
		class Bar {}
		
		class Baz extends Foo {}
		class Qux extends Bar {}
		
		assertTrue(EntityInspector.isEntity(Baz.class));
		assertTrue(EntityInspector.isEntity(Qux.class));
	}

	@Test
	public void testGetAnnotation() throws IntrospectionException {
		@SuppressWarnings("unused")
		@Embeddable
		class Hello {
			String foo;
			String bar;
			String baz;
			@Basic
			public String getBar() {
				return bar;
			}
			@Basic
			public void setFoo(String foo) {
				this.foo = foo;
			}
			@Basic
			public String getBaz() {
				return baz;
			}
			public void setBaz(String baz) {
				this.baz = baz;
			}
		}
		// 分别测试三种情况：
		// 1.仅有setter
		// 2.仅有getter
		// 3.getter和setter均存在
		for (PropertyDescriptor pd : Introspector.getBeanInfo(Hello.class, Hello.class.getSuperclass())
				.getPropertyDescriptors()) {
			Basic basic = EntityInspector.getAnnotation(pd, Basic.class);
			assertNotNull(basic);
		}
	}

	@Test
	public void testGetAccessType() {
		@Embeddable
		class Qux {}
		// 首先测试嵌入式类，此类没有标准@Access，也没有标准@Id，所以最后应该判断为默认的AccessType.PROPERTY
		AccessType accessType = EntityInspector.getAccessType(Qux.class);
		assertEquals(AccessType.PROPERTY, accessType);
		
		// 然后测试根据标准了@Id来确定访问类型
		@Entity
		class Foo {
			@Id
			private int id;
		}
		accessType = EntityInspector.getAccessType(Foo.class);
		assertEquals(AccessType.FIELD, accessType);
		
		// 再测试返回PROPERTY的情况
		@SuppressWarnings("unused")
		@Entity
		class Bar {
			private int id;
			@Id
			public int getId() {
				return id;
			}
			public void setId(int id) {
				this.id = id;
			}
		}
		accessType = EntityInspector.getAccessType(Bar.class);
		assertEquals(AccessType.PROPERTY, accessType);
		
		// 最后测试指明@Access的情况
		@Entity
		@Access(AccessType.FIELD)
		class Baz {
			@Id
			private int id;
		}
		accessType = EntityInspector.getAccessType(Baz.class);
		assertEquals(AccessType.FIELD, accessType);
	}

	@Test
	public void testGetConditions() {
		class Foo {
			@Instruction(operator = Operator.LIKE)
			String prop = "foo";
		}
		@SuppressWarnings("unused")
		@MappedSuperclass
		class Bar extends Foo {
			public String prop = null; // 这是实体的属性
			@Instruction(operator = Operator.LT)
			public double numerical = 2.0;
		}
		@Entity
		class Baz extends Bar {
			@Id
			public int id;
			@Instruction(operator = Operator.NOT_LIKE)
			String prop = "baz";
			@Temporal(TemporalType.DATE)
			public Date today = new Date();
			@Transient
			@Instruction(propertyName = "today", operator = Operator.GT)
			public Date getTodayGreaterThan() {
				LocalDate yesterday = LocalDate.now().minusDays(1);
				return LocalDateUtil.toDate(yesterday);
			}
		}
		class Qux extends Baz {
			@Transient
			@Instruction(propertyName = "today", operator = Operator.LT)
			public Date getTodayLessThan() {
				LocalDate tomorrow = LocalDate.now().plusDays(1);
				return LocalDateUtil.toDate(tomorrow);
			}
		}
		Qux qux = new Qux();
		Set<Condition> conditions = EntityInspector.getConditions(qux.getClass());
		for (Condition condition : conditions) {
			// 不管是实体属性还是非实体属性均会扫描并分析
			switch (condition.fromName()) {
			case "prop":
				// 在Foo类中找到一个条件：prop LIKE "foo"
				assertEquals("prop", condition.propertyName);
				assertEquals(Operator.NOT_LIKE, condition.operator);
				assertEquals("baz", condition.getValue(qux));
				break;
			case "numerical":
				// 在Bar上找到条件：numerical > 2.0
				assertEquals("numerical", condition.propertyName);
				assertEquals(Operator.LT, condition.operator);
				assertTrue(equal(2.0, (double) condition.getValue(qux)));
				break;
			case "todayGreaterThan":
				// 在Baz上找到条件：today > 昨天
				assertEquals("today", condition.propertyName);
				assertEquals(Operator.GT, condition.operator);
				assertTrue(qux.today.compareTo((Date) condition.getValue(qux)) > 0);
				break;
			case "todayLessThan":
				// 在Qux上找到条件：today < 明天
				assertEquals("today", condition.propertyName);
				assertEquals(Operator.LT, condition.operator);
				assertTrue(qux.today.compareTo((Date) condition.getValue(qux)) < 0);
				break;
			default:
				break;
			}
		}
	}

	@Test
	public void testFindEntityBound() {
		class Foo {} // 没有注解，与实体无关
		@MappedSuperclass
		class Bar extends Foo {} // 标注@MappedSuperclass属于被继承的类
		@Entity
		class Baz extends Bar { // 实体
			@Id
			public int id;
		}
		class Qux extends Baz {} // 继承实体类的类
		Class<?>[] bound = EntityInspector.findEntityBound(Qux.class);
		assertEquals(Baz.class, bound[0]);
		assertEquals(Foo.class, bound[1]);
		
		class Fooo {}
		@Embeddable
		class Barr extends Fooo {} // 可嵌入类
		bound = EntityInspector.findEntityBound(Barr.class);
		assertEquals(Barr.class, bound[0]);
		assertEquals(Fooo.class, bound[1]);
	}

	@Test
	public void testGetEntityPropertyByField() {
		class Foo {} // 没有注解，与实体无关
		@SuppressWarnings("unused")
		@MappedSuperclass
		class Bar extends Foo implements Serializable {
			private static final long serialVersionUID = -5138679260079606065L;
			public String field = "bar";
			public transient String trans = "trans";
		} // 标注@MappedSuperclass属于被继承的类
		@Entity
		class Baz extends Bar { // 实体
			private static final long serialVersionUID = -4522192978773295718L;
			@Id
			private int id = 11;
		}
		class Qux extends Baz {
			private static final long serialVersionUID = 8521158475639243659L;
		} // 继承实体类的类
		Set<EntityProperty> result = EntityInspector.getEntityPropertyByField(Qux.class);
		// 预期：实体属性既有私有字段中被注解的id，也有公开子段的field，并且能获取到正确值
		Qux obj = new Qux();
		for (EntityProperty ep : result) {
			if (ep.name.equals("id")) {
				assertEquals(11, ep.getValue(obj));
			} else if (ep.name.equals("field")) {
				assertEquals("bar", ep.getValue(obj));
			}
		}
	}

	@Test
	public void testGetEntityPropertyByJpaDefinition() {
		class Foo {
			@SuppressWarnings("unused")
			public String field = "foo";
		} // 没有注解，与实体无关
		@SuppressWarnings("unused")
		@MappedSuperclass
		class Bar extends Foo implements Serializable {
			private static final long serialVersionUID = 491632401134000904L;
			private String id = "bar";
			public String field = "bar";
			public transient String trans = "trans";
			@Id
			public String getId() {
				return id;
			}
			public void setId(String id) {
				this.id = id;
			}
		} // 标注@MappedSuperclass属于被继承的类
		@SuppressWarnings("unused")
		@Entity
		class Baz extends Bar { // 实体
			private static final long serialVersionUID = -3410803262376947455L;
			public static final boolean isProp = false;
			private String id = "baz";
			public String baz = "baz";
			public String field = "baz";
			@Id
			public String getId() {
				return id;
			}
			public void setId(String id) {
				this.id = id;
			}
		}
		class Qux extends Baz {
			private static final long serialVersionUID = -8785102859340804808L;
		} // 继承实体类的类
		Qux obj = new Qux();
		Set<EntityProperty> result = EntityInspector.getEntityPropertyByJpaDefinition(Qux.class);
		// 预期：扩展类的属性覆盖被继承的类的属性，id和field的值都应该是"baz"
		for (EntityProperty ep : result) {
			if (ep.name.equals("id")) {
				assertEquals("baz", ep.getValue(obj));
			} else if (ep.name.equals("field")) {
				assertEquals("baz", ep.getValue(obj));
			}
		}
	}

	@Test
	public void testGetEntityProperty() {
		Item item = new Item();
		Set<EntityProperty> properties = EntityInspector.getEntityProperty(item.getClass());
		for (EntityProperty prop : properties) {
			if (prop.name.equals("auctionType")) {
				assertEquals(AuctionType.HIGHEST_BID, prop.getValue(item));
				assertNotNull(prop.getAnnotation(Enumerated.class));
			}
			if (prop.name.equals("categories")) {
				Class<?> categoriesType = prop.getType();
				assertEquals(Set.class, categoriesType);
				assertEquals(Category.class, prop.getGenericClass()[0]);
			}
		}
		
		@Entity
		class ItemMeta {
			@Id
			int id;
			@SuppressWarnings("unused")
			String metaName;
			@OneToMany
			@JoinTable
			Set<Item> items = new HashSet<>();
		}
		ItemMeta itemMeta = new ItemMeta();
		properties = EntityInspector.getEntityProperty(itemMeta.getClass());
		for (EntityProperty prop : properties) {
			if (prop.name.equals("items")) {
				Class<?> categoriesType = prop.getType();
				assertEquals(Set.class, categoriesType);
				assertEquals(Item.class, prop.getGenericClass()[0]);
			}
		}
	}

	@Test
	public void testGetGenericClassPropertyDescriptor() {
		// 已在testGetEntityProperty中测试
	}

	@Test
	public void testGetGenericClassField() {
		// 已在testGetEntityProperty中测试
	}

	boolean equal(double num1, double num2) {
		if ((num1 - num2 > -0.000001) && (num1 - num2) < 0.000001)
			return true;
		else
			return false;
	}
	
	@SuppressWarnings({"unchecked"})
	@Test
	public void testInjectField() throws IllegalArgumentException, IllegalAccessException {
		class Foo {
			short s1;
			Short s2 = new Short((short) 0);
			int i1;
			Integer i2;
			String str = "";
			boolean b1;
			Boolean b2 = new Boolean(false);
			Em em = Em.A;
			byte by1;
			Byte by2 = new Byte((byte) 0);
			char c1 = 'A';
			Character c2 = new Character('A');
		}
		class Bar extends Foo {
			double d1;
			Double d2 = new Double(0.0);
			long l1;
			Long l2 = new Long(0l);
			LocalDate now1 = LocalDate.now();
			LocalTime now2 = LocalTime.now();
			Instant now3 = Instant.now();
			LocalDateTime now4 = LocalDateTime.now();
			Date now5 = new Date();
			Calendar now6 = Calendar.getInstance();
		}
		
		Bar src = new Bar(), tar = new Bar();
		src.d1 = 1.1;
		src.d2 = new Double(1.1);
		src.l1 = 1l;
		src.l2 = new Long(1l);
		src.now1 = src.now1.minusYears(1l);
		src.now2 = src.now2.minusHours(1l);
		src.now3 = src.now3.minusSeconds(1000l);
		src.now4 = src.now4.minusYears(1l);
		src.now5 = Date.from(src.now3);
		src.now6.set(Calendar.YEAR, src.now1.getYear());
		src.s1 = 1;
		src.s2 = new Short((short) 1);
		src.i1 = 1;
		src.i2 = new Integer(1);
		src.str = "hellow world";
		src.b1 = true;
		src.b2 = new Boolean(true);
		src.em = Em.B;
		src.by1 = 1;
		src.by2 = new Byte((byte) 1);
		src.c1 = 'B';
		src.c2 = new Character('B');
		
		Class<? extends Foo> clz = Bar.class;
		while (!clz.equals(Object.class)) {
			for (Field f : clz.getDeclaredFields()) {
				int modifiers = f.getModifiers();
				// isStrict 内部类连接外围类的引用
				if (Modifier.isStatic(modifiers) || Modifier.isStrict(modifiers) || Modifier.isFinal(modifiers)) {
					continue;
				}
				if (EntityInspector.isValueType(f.getType())) {
					EntityInspector.injectField(f, tar, f.get(src));
				}
			}
			clz = (Class<? extends Foo>) clz.getSuperclass();
		}
		
		assertTrue(src.d1 == tar.d1);
		assertEquals(src.d2, tar.d2);
		
		assertTrue(src.l1 == tar.l1);
		assertEquals(src.l2, tar.l2);
		
		assertEquals(src.now1, tar.now1);
		assertEquals(src.now2, tar.now2);
		assertEquals(src.now3, tar.now3);
		assertEquals(src.now4, tar.now4);
		assertEquals(src.now5, tar.now5);
		assertEquals(src.now6, tar.now6);
		
		assertTrue(src.s1 == tar.s1);
		assertEquals(src.s2, tar.s2);
		
		assertTrue(src.i1 == tar.i1);
		assertEquals(src.i2, tar.i2);
		
		assertEquals(src.str, tar.str);
		
		assertTrue(src.b1 == tar.b1);
		assertEquals(src.b2, tar.b2);
		
		assertTrue(src.em == tar.em);
		
		assertTrue(src.by1 == tar.by1);
		assertEquals(src.by2, tar.by2);
		
		assertTrue(src.c1 == tar.c1);
		assertEquals(src.c2, tar.c2);
	}
}

enum Em {
	A, B
}
