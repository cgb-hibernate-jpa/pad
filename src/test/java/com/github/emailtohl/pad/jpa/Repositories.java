package com.github.emailtohl.pad.jpa;

import org.springframework.stereotype.Repository;

import com.github.emailtohl.pad.entities.oauth2.ClientDetails;
import com.github.emailtohl.pad.entities.session.SpringSession;
import com.github.emailtohl.pad.entities.session.SpringSessionAttributesForm;
import com.github.emailtohl.pad.jpa.AuditedRepository;
import com.github.emailtohl.pad.jpa.QueryRepository;
import com.github.emailtohl.pad.jpa.SearchRepository;
import com.github.emailtohl.pad.model.BankAccount;
import com.github.emailtohl.pad.model.Bid;
import com.github.emailtohl.pad.model.BillingDetails;
import com.github.emailtohl.pad.model.Category;
import com.github.emailtohl.pad.model.CreditCard;
import com.github.emailtohl.pad.model.Item;
import com.github.emailtohl.pad.model.Participator;

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
