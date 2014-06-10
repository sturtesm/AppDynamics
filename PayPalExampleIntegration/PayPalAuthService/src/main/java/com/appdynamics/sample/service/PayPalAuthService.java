package com.appdynamics.sample.service;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.apache.log4j.Logger;

import com.paypal.api.payments.Address;
import com.paypal.api.payments.Amount;
import com.paypal.api.payments.Authorization;
import com.paypal.api.payments.CreditCard;
import com.paypal.api.payments.Details;
import com.paypal.api.payments.FundingInstrument;
import com.paypal.api.payments.Payer;
import com.paypal.api.payments.Payment;
import com.paypal.api.payments.Transaction;
import com.paypal.core.rest.APIContext;
import com.paypal.core.rest.PayPalRESTException;


@Path("/paypal")
public class PayPalAuthService {

	Logger logger = Logger.getLogger(PayPalAuthService.class);

	public PayPalAuthService() {

	}

	@GET
	@Path("/auth")
	@Produces("text/plain")
	public String authenticate(){
		String accessToken = null;

		try {
			// Retrieve the access token from
			// OAuthTokenCredential by passing in
			accessToken = GenerateAccessToken.getAccessToken();

			logger.info("PayPal Access Token=" + accessToken);

		}catch (Exception e) {
			e.printStackTrace();
		}

		return accessToken;
	}

	@GET
	@Path("/payment/{accessToken}")
	@Consumes("text/plain")
	@Produces("text/plain")
	public Authorization payment(@PathParam("accessToken") String accessToken) throws PayPalRESTException {
		
		System.out.println("Received an access token from the caller: " + accessToken);
		logger.info("Received an access token from the caller: " + accessToken);
		
		
		// ### Api Context
		// Pass in a `ApiContext` object to authenticate
		// the call and to send a unique request id
		// (that ensures idempotency). The SDK generates
		// a request id if you do not pass one explicitly.
		APIContext apiContext = new APIContext(accessToken);
		
		// Use this variant if you want to pass in a request id
		// that is meaningful in your application, ideally
		// a order id.
		/*
		 * String requestId = Long.toString(System.nanoTime(); APIContext
		 * apiContext = new APIContext(accessToken, requestId ));
		 */

		// ###Authorization
		// Retrieve an Authorization Id
		// by making a Payment with intent
		// as 'authorize' and parsing through
		// the Payment object
		String authorizationId = getAuthorizationID(apiContext);

		// Get Authorization by sending
		// a GET request with authorization Id
		// to the
		// URI v1/payments/authorization/{id}
		Authorization authorization = Authorization.get(apiContext,
				authorizationId);

		logger.info("Authorization id = " + authorization.getId()
				+ " and status = " + authorization.getState());
		
		return authorization;
	}

	private String getAuthorizationID(APIContext apiContext)
			throws PayPalRESTException {
		String authorizationID = null;

		// ###Details
		// Let's you specify details of a payment amount.
		Details details = new Details();
		details.setShipping("0.03");
		details.setSubtotal("107.41");
		details.setTax("0.03");

		// ###Amount
		// Let's you specify a payment amount.
		Amount amount = new Amount();
		amount.setCurrency("USD");
		amount.setTotal("107.47");
		amount.setDetails(details);

		// ###Transaction
		// A transaction defines the contract of a
		// payment - what is the payment for and who
		// is fulfilling it. Transaction is created with
		// a `Payee` and `Amount` types
		Transaction transaction = new Transaction();
		transaction.setAmount(amount);
		transaction
		.setDescription("This is the payment transaction description.");

		// The Payment creation API requires a list of
		// Transaction; add the created `Transaction`
		// to a List
		List<Transaction> transactions = new ArrayList<Transaction>();
		transactions.add(transaction);

		// ###Address
		// Base Address object used as shipping or billing
		// address in a payment. [Optional]
		Address billingAddress = new Address();
		billingAddress.setCity("Johnstown");
		billingAddress.setCountryCode("US");
		billingAddress.setLine1("52 N Main ST");
		billingAddress.setPostalCode("43210");
		billingAddress.setState("OH");

		// ###CreditCard
		// A resource representing a credit card that can be
		// used to fund a payment.
		CreditCard creditCard = new CreditCard();
		creditCard.setBillingAddress(billingAddress);
		creditCard.setCvv2("874");
		creditCard.setExpireMonth(11);
		creditCard.setExpireYear(2018);
		creditCard.setFirstName("Joe");
		creditCard.setLastName("Shopper");
		creditCard.setNumber("4417119669820331");
		creditCard.setType("visa");

		// ###FundingInstrument
		// A resource representing a Payeer's funding instrument.
		// Use a Payer ID (A unique identifier of the payer generated
		// and provided by the facilitator. This is required when
		// creating or using a tokenized funding instrument)
		// and the `CreditCardDetails`
		FundingInstrument fundingInstrument = new FundingInstrument();
		fundingInstrument.setCreditCard(creditCard);

		// The Payment creation API requires a list of
		// FundingInstrument; add the created `FundingInstrument`
		// to a List
		List<FundingInstrument> fundingInstruments = new ArrayList<FundingInstrument>();
		fundingInstruments.add(fundingInstrument);

		// ###Payer
		// A resource representing a Payer that funds a payment
		// Use the List of `FundingInstrument` and the Payment Method
		// as 'credit_card'
		Payer payer = new Payer();
		payer.setFundingInstruments(fundingInstruments);
		payer.setPaymentMethod("credit_card");

		// ###Payment
		// A Payment Resource; create one using
		// the above types and intent as 'authorize'
		Payment payment = new Payment();
		payment.setIntent("authorize");
		payment.setPayer(payer);
		payment.setTransactions(transactions);

		Payment responsePayment = payment.create(apiContext);

		// Retrieve the authorization Id
		authorizationID = responsePayment.getTransactions().get(0)
				.getRelatedResources().get(0).getAuthorization().getId();
		return authorizationID;
	}
}
