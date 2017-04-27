package com.cgi.nm.util.testTools;

import java.util.concurrent.atomic.AtomicInteger;

public abstract class AcctTos {

	protected String userBase;
	protected String userDomain;
	protected String baseIpAddress;
	protected String template;

	private final AtomicInteger counter = new AtomicInteger();
	private int accountsToBeCreated;

	public String getTemplate() {

		return this.template;
	}

	public String generateUsername() {

		if (accountsToBeCreated == 0) {
			throw new IllegalArgumentException("You must setCountOfAccountsToBeCreated");
		}

		final int accountNumber = counter.incrementAndGet();

		if (accountNumber > accountsToBeCreated) {
			return null;
		} else {
			return String.format("%s%08d@%s", this.userBase, accountNumber, this.userDomain);
		}
	}

	public void setCountOfAccountsToBeCreated(final int count) {

		this.accountsToBeCreated = count;
	}

	public String getBaseIpAddress() {

		return baseIpAddress;
	}

}
