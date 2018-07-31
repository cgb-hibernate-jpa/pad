package com.github.emailtohl.lib.jpa;

import org.springframework.stereotype.Repository;

import com.github.emailtohl.lib.entities.oauth2.ClientDetails;
import com.github.emailtohl.lib.entities.session.SpringSession;
import com.github.emailtohl.lib.entities.session.SpringSessionAttributesForm;
import com.github.emailtohl.lib.model.BankAccount;
import com.github.emailtohl.lib.model.Bid;
import com.github.emailtohl.lib.model.BillingDetails;
import com.github.emailtohl.lib.model.Category;
import com.github.emailtohl.lib.model.CreditCard;
import com.github.emailtohl.lib.model.Item;
import com.github.emailtohl.lib.model.Participator;

@Repository
class ParticipatorRepo extends QueryRepository<Participator, Long> {}

@Repository
class BillingDetailsRepo extends QueryRepository<BillingDetails, Long> {}

@Repository
class BankAccountRepo extends QueryRepository<BankAccount, Long> {}

@Repository
class CreditCardRepo extends QueryRepository<CreditCard, Long> {}

@Repository
class CategoryRepo extends QueryRepository<Category, Long> {}

@Repository
class ItemRepo extends QueryRepository<Item, Long> {}

@Repository
class ItemSearchRepo extends SearchRepository<Item, Long> {}

@Repository
class ItemAuditedRepo extends AuditedRepository<Item, Long> {}

@Repository
class BidRepo extends QueryRepository<Bid, Long> {}

@Repository
class SpringSessionRepo extends QueryRepository<SpringSession, String> {}

@Repository
class SpringSessionAttributesRepo extends QueryRepository<SpringSessionAttributesForm, SpringSessionAttributesForm.Id> {}

@Repository
class ClientDetailsRepo extends AuditedRepository<ClientDetails, String> {}
