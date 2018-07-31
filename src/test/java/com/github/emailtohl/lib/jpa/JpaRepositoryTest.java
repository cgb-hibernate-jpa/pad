package com.github.emailtohl.lib.jpa;

import java.io.Serializable;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class JpaRepositoryTest {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testJpaRepository() {
		Left l = new Left();
		Assert.assertSame(Long.class, l.idClass);
		Assert.assertSame(String.class, l.entityClass);

		Right r = new Right();
		Assert.assertSame(Long.class, r.idClass);
		Assert.assertSame(String.class, r.entityClass);

		Pair p = new Pair();
		Assert.assertSame(Long.class, p.idClass);
		Assert.assertSame(String.class, p.entityClass);
	}

	@Test(expected = IllegalStateException.class)
	public void testSpecific() {
		// 要获取类的泛型参数，必须在其导出类中获取，在本层次中获取不了泛型参数，所以下列情况都会出现异常
		SpecificLeftDefined<String> s1 = new SpecificLeftDefined<String>();
		Assert.assertNotNull(s1.entityClass);
		Assert.assertSame(Long.class, s1.idClass);

		SpecificRightDefined<Long> s2 = new SpecificRightDefined<Long>();
		Assert.assertSame(String.class, s2.entityClass);
		Assert.assertNotNull(s2.idClass);

		SpecificPairDefined<String, Long> s3 = new SpecificPairDefined<String, Long>();
		Assert.assertNull(s3.entityClass);
		Assert.assertNull(s3.idClass);
	}

}

abstract class LeftDefined<A extends Serializable> extends JpaRepository<String, A> {
}

abstract class RightDefined<B extends Serializable> extends JpaRepository<B, Long> {
}

abstract class PairDefined extends JpaRepository<String, Long> {
}

class Left extends LeftDefined<Long> {
}

class Right extends RightDefined<String> {
}

class Pair extends PairDefined {
}

class SpecificLeftDefined<A extends Serializable> extends JpaRepository<String, Long> {
}

class SpecificRightDefined<B extends Serializable> extends JpaRepository<String, Long> {
}

class SpecificPairDefined<A extends Serializable, B extends Serializable> extends JpaRepository<A, B> {
}

