package com.github.emailtohl.lib.config;

import java.math.BigDecimal;
import java.time.LocalDate;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.NoResultException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.emailtohl.lib.entities.oauth2.ClientDetails;
import com.github.emailtohl.lib.model.Address;
import com.github.emailtohl.lib.model.BankAccount;
import com.github.emailtohl.lib.model.Bid;
import com.github.emailtohl.lib.model.Category;
import com.github.emailtohl.lib.model.CreditCard;
import com.github.emailtohl.lib.model.Image;
import com.github.emailtohl.lib.model.Item;
import com.github.emailtohl.lib.model.Participator;
import com.github.emailtohl.lib.util.LocalDateUtil;

/**
 * 初始化实体
 * 
 * @author HeLei
 */
@Configuration
class Entities {
	LocalDate tody = LocalDate.now();

	Address fooAddress = new Address("street1", "12345", "city"), barAddress = new Address("street2", "23456", "city"),
			bazAddress = new Address("street3", "34567", "city"), quxAddress = new Address("street4", "45678", "city");
	Participator foo = new Participator("foo"), bar = new Participator("bar"), baz = new Participator("baz"),
			qux = new Participator("qux");
	CreditCard creditCard = new CreditCard("baz", "123456", "12", "2025");
	BankAccount bankAccount = new BankAccount("qux", "654321", "bankname", "swift");
	Image purpleOutfit1 = new Image("purpleOutfit1", "/var/image/purpleOutfit1", 160, 80),
			purpleOutfit2 = new Image("purpleOutfit2", "/var/image/purpleOutfit2", 240, 100),
			orangeOutfitBid1 = new Image("orangeOutfitBid1", "/var/image/orangeOutfit1", 240, 100),
			orangeOutfitBid2 = new Image("orangeOutfitBid2", "/var/image/orangeOutfit2", 240, 100);
	Item purpleOutfit = new Item("Purple outfit", LocalDateUtil.toDate(tody.plusDays(2)), foo),
			orangeOutfit = new Item("Orange outfit", LocalDateUtil.toDate(tody.plusDays(3)), bar);
	Category sup = new Category("super"), sub = new Category("sub", sup);

	Bid purpleOutfitBid = new Bid("purpleOutfitBid", purpleOutfit, baz, new BigDecimal(1000.00)),
			orangeOutfitBid = new Bid("orangeOutfitBid", orangeOutfit, qux, new BigDecimal(2000.00));

	ClientDetails clientDetails = new ClientDetails();

	@Autowired
	EntityManagerFactory factory;

	public Entities() {
		foo.setHomeAddress(fooAddress);
		bar.setHomeAddress(barAddress);
		baz.setHomeAddress(bazAddress);
		qux.setHomeAddress(quxAddress);

		purpleOutfit.setSeller(foo);
		purpleOutfit.setApproved(true);
		purpleOutfit.setBuyNowPrice(new BigDecimal(980.00));
		purpleOutfit.getImages().add(purpleOutfit1);
		purpleOutfit.getImages().add(purpleOutfit2);
		purpleOutfit.getBids().add(purpleOutfitBid);
		purpleOutfit.getCategories().add(sub);

		orangeOutfit.setSeller(bar);
		orangeOutfit.setApproved(true);
		orangeOutfit.setBuyNowPrice(new BigDecimal(1700.00));
		orangeOutfit.getImages().add(orangeOutfitBid1);
		orangeOutfit.getImages().add(orangeOutfitBid2);
		orangeOutfit.getBids().add(orangeOutfitBid);
		orangeOutfit.getCategories().add(sub);

		sub.getItems().add(purpleOutfit);
		sub.getItems().add(orangeOutfit);

		clientDetails.setAppId("123-456-789-0");
		clientDetails.setAccessTokenValidity(3600);
		clientDetails.setAdditionalInformation("HELLO WORLD");
		clientDetails.setAppSecret("123456");
		clientDetails.setAuthorities("ADMIN,MANAGER");
		clientDetails.setAutoApproveScopes("ApproveScopes");
		clientDetails.setGrantTypes("READ,WRITE");
		clientDetails.setRedirectUrl("localhost");
		clientDetails.setRefreshTokenValidity(7200);
		clientDetails.setResourceIds("first-one");
		clientDetails.setScope("ALL");
	}

	@Bean
	Participator foo() {
		EntityManager em = factory.createEntityManager();
		em.getTransaction().begin();
		CriteriaBuilder b = em.getCriteriaBuilder();
		CriteriaQuery<Long> q = b.createQuery(Long.class);
		Root<Participator> r = q.from(Participator.class);
		q = q.select(r.get("id")).where(b.equal(r.get("name"), foo.getName()));
		Long id = null;
		try {
			id = em.createQuery(q).getSingleResult();
		} catch (NoResultException e) {
		}
		if (id == null) {
			em.persist(foo);
		} else {
			foo.setId(id);
		}
		em.getTransaction().commit();
		em.close();
		return foo;
	}

	@Bean
	Participator bar() {
		EntityManager em = factory.createEntityManager();
		em.getTransaction().begin();
		CriteriaBuilder b = em.getCriteriaBuilder();
		CriteriaQuery<Long> q = b.createQuery(Long.class);
		Root<Participator> r = q.from(Participator.class);
		q = q.select(r.get("id")).where(b.equal(r.get("name"), bar.getName()));
		Long id = null;
		try {
			id = em.createQuery(q).getSingleResult();
		} catch (NoResultException e) {
		}
		if (id == null) {
			em.persist(bar);
		} else {
			bar.setId(id);
		}
		em.getTransaction().commit();
		em.close();
		return bar;
	}

	@Bean
	Participator baz() {
		EntityManager em = factory.createEntityManager();
		em.getTransaction().begin();
		CriteriaBuilder b = em.getCriteriaBuilder();
		CriteriaQuery<Long> q = b.createQuery(Long.class);
		Root<Participator> r = q.from(Participator.class);
		q = q.select(r.get("id")).where(b.equal(r.get("name"), baz.getName()));
		Long id = null;
		try {
			id = em.createQuery(q).getSingleResult();
		} catch (NoResultException e) {
		}
		if (id == null) {
			em.persist(baz);
		} else {
			baz.setId(id);
		}
		em.getTransaction().commit();
		em.close();
		return baz;
	}

	@Bean
	Participator qux() {
		EntityManager em = factory.createEntityManager();
		em.getTransaction().begin();
		CriteriaBuilder b = em.getCriteriaBuilder();
		CriteriaQuery<Long> q = b.createQuery(Long.class);
		Root<Participator> r = q.from(Participator.class);
		q = q.select(r.get("id")).where(b.equal(r.get("name"), qux.getName()));
		Long id = null;
		try {
			id = em.createQuery(q).getSingleResult();
		} catch (NoResultException e) {
		}
		if (id == null) {
			em.persist(qux);
		} else {
			qux.setId(id);
		}
		em.getTransaction().commit();
		em.close();
		return qux;
	}

	@Bean
	CreditCard creditCard(@Qualifier("foo") Participator foo, @Qualifier("bar") Participator bar,
			@Qualifier("baz") Participator baz, @Qualifier("qux") Participator qux) {
		EntityManager em = factory.createEntityManager();
		em.getTransaction().begin();
		CriteriaBuilder b = em.getCriteriaBuilder();
		CriteriaQuery<Long> q = b.createQuery(Long.class);
		Root<CreditCard> r = q.from(CreditCard.class);
		q = q.select(r.get("id")).where(b.equal(r.get("cardNumber"), creditCard.getCardNumber()));
		Long id = null;
		try {
			id = em.createQuery(q).getSingleResult();
		} catch (NoResultException e) {
		}
		if (id == null) {
			em.persist(creditCard);
		} else {
			creditCard.setId(id);
		}
		em.getTransaction().commit();
		em.close();
		return creditCard;
	}

	@Bean
	BankAccount bankAccount(@Qualifier("foo") Participator foo, @Qualifier("bar") Participator bar,
			@Qualifier("baz") Participator baz, @Qualifier("qux") Participator qux) {
		EntityManager em = factory.createEntityManager();
		em.getTransaction().begin();
		CriteriaBuilder b = em.getCriteriaBuilder();
		CriteriaQuery<Long> q = b.createQuery(Long.class);
		Root<BankAccount> r = q.from(BankAccount.class);
		q = q.select(r.get("id")).where(b.equal(r.get("account"), bankAccount.getAccount()));
		Long id = null;
		try {
			id = em.createQuery(q).getSingleResult();
		} catch (NoResultException e) {
		}
		if (id == null) {
			em.persist(bankAccount);
		} else {
			bankAccount.setId(id);
		}
		em.getTransaction().commit();
		em.close();
		return bankAccount;
	}

	@Bean
	Category sup() {
		EntityManager em = factory.createEntityManager();
		em.getTransaction().begin();
		CriteriaBuilder b = em.getCriteriaBuilder();
		CriteriaQuery<Long> q = b.createQuery(Long.class);
		Root<Category> r = q.from(Category.class);
		q = q.select(r.get("id")).where(b.equal(r.get("name"), sup.getName()));
		Long id = null;
		try {
			id = em.createQuery(q).getSingleResult();
		} catch (NoResultException e) {
		}
		if (id == null) {
			em.persist(sup);
		} else {
			sup.setId(id);
		}
		em.getTransaction().commit();
		em.close();
		return sup;
	}

	@Bean
	Category sub(@Qualifier("sup") Category sup) {
		EntityManager em = factory.createEntityManager();
		em.getTransaction().begin();
		CriteriaBuilder b = em.getCriteriaBuilder();
		CriteriaQuery<Long> q = b.createQuery(Long.class);
		Root<Category> r = q.from(Category.class);
		q = q.select(r.get("id")).where(b.equal(r.get("name"), sub.getName()));
		Long id = null;
		try {
			id = em.createQuery(q).getSingleResult();
		} catch (NoResultException e) {
		}
		if (id == null) {
			em.persist(sub);
		} else {
			sub.setId(id);
		}
		em.getTransaction().commit();
		em.close();
		return sub;
	}

	@Bean
	Item purpleOutfit(@Qualifier("foo") Participator foo, @Qualifier("bar") Participator bar,
			@Qualifier("baz") Participator baz, @Qualifier("qux") Participator qux, @Qualifier("sup") Category sup,
			@Qualifier("sub") Category sub) {
		EntityManager em = factory.createEntityManager();
		em.getTransaction().begin();
		CriteriaBuilder b = em.getCriteriaBuilder();
		CriteriaQuery<Long> q = b.createQuery(Long.class);
		Root<Item> r = q.from(Item.class);
		q = q.select(r.get("id")).where(b.equal(r.get("name"), purpleOutfit.getName()));
		Long id = null;
		try {
			id = em.createQuery(q).getSingleResult();
		} catch (NoResultException e) {
		}
		if (id == null) {
			em.persist(purpleOutfit);
		} else {
			purpleOutfit.setId(id);
		}
		em.getTransaction().commit();
		em.close();
		return purpleOutfit;
	}

	@Bean
	Item orangeOutfit(@Qualifier("foo") Participator foo, @Qualifier("bar") Participator bar,
			@Qualifier("baz") Participator baz, @Qualifier("qux") Participator qux, @Qualifier("sup") Category sup,
			@Qualifier("sub") Category sub) {
		EntityManager em = factory.createEntityManager();
		em.getTransaction().begin();
		CriteriaBuilder b = em.getCriteriaBuilder();
		CriteriaQuery<Long> q = b.createQuery(Long.class);
		Root<Item> r = q.from(Item.class);
		q = q.select(r.get("id")).where(b.equal(r.get("name"), orangeOutfit.getName()));
		Long id = null;
		try {
			id = em.createQuery(q).getSingleResult();
		} catch (NoResultException e) {
		}
		if (id == null) {
			em.persist(orangeOutfit);
		} else {
			orangeOutfit.setId(id);
		}
		em.getTransaction().commit();
		return orangeOutfit;
	}

	@Bean
	Bid purpleOutfitBid(@Qualifier("foo") Participator foo, @Qualifier("bar") Participator bar,
			@Qualifier("baz") Participator baz, @Qualifier("qux") Participator qux,
			@Qualifier("purpleOutfit") Item purpleOutfit, @Qualifier("orangeOutfit") Item orangeOutfit) {
		EntityManager em = factory.createEntityManager();
		em.getTransaction().begin();
		CriteriaBuilder b = em.getCriteriaBuilder();
		CriteriaQuery<Long> q = b.createQuery(Long.class);
		Root<Bid> r = q.from(Bid.class);
		q = q.select(r.get("id")).where(b.equal(r.get("name"), purpleOutfitBid.getName()));
		Long id = null;
		try {
			id = em.createQuery(q).getSingleResult();
		} catch (NoResultException e) {
		}
		if (id == null) {
			em.persist(purpleOutfitBid);
		} else {
			purpleOutfitBid.setId(id);
		}
		em.getTransaction().commit();
		em.close();
		return purpleOutfitBid;
	}

	@Bean
	Bid orangeOutfitBid(@Qualifier("foo") Participator foo, @Qualifier("bar") Participator bar,
			@Qualifier("baz") Participator baz, @Qualifier("qux") Participator qux,
			@Qualifier("purpleOutfit") Item purpleOutfit, @Qualifier("orangeOutfit") Item orangeOutfit) {
		EntityManager em = factory.createEntityManager();
		em.getTransaction().begin();
		CriteriaBuilder b = em.getCriteriaBuilder();
		CriteriaQuery<Long> q = b.createQuery(Long.class);
		Root<Bid> r = q.from(Bid.class);
		q = q.select(r.get("id")).where(b.equal(r.get("name"), orangeOutfitBid.getName()));
		Long id = null;
		try {
			id = em.createQuery(q).getSingleResult();
		} catch (NoResultException e) {
		}
		if (id == null) {
			em.persist(orangeOutfitBid);
		} else {
			orangeOutfitBid.setId(id);
		}
		em.getTransaction().commit();
		return orangeOutfitBid;
	}

	@Bean
	ClientDetails clientDetails() {
		EntityManager em = factory.createEntityManager();
		em.getTransaction().begin();
		CriteriaBuilder b = em.getCriteriaBuilder();
		CriteriaQuery<ClientDetails> q = b.createQuery(ClientDetails.class);
		Root<ClientDetails> r = q.from(ClientDetails.class);
		q = q.select(r).where(b.equal(r.get("appId"), clientDetails.getAppId()));
		ClientDetails entity = null;
		try {
			entity = em.createQuery(q).getSingleResult();
		} catch (NoResultException e) {
		}
		if (entity == null) {
			em.persist(clientDetails);
			entity = clientDetails;
		}
		em.getTransaction().commit();
		return clientDetails;
	}
}
