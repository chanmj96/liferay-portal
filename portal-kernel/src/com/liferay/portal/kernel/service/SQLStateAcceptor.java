/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.portal.kernel.service;

import com.liferay.portal.kernel.util.Validator;

import java.sql.SQLException;

import java.util.Map;

/**
 * @author Matthew Tambara
 */
public class SQLStateAcceptor implements RetryAcceptor {

	public static final String SQLSTATE = "SQLSTATE";

	public static final String SQLSTATE_INTEGRITY_CONSTRAINT_VIOLATION = "23";

	@Override
	public boolean acceptException(
		Throwable throwable, Map<String, String> propertyMap) {

		String sqlState = propertyMap.get(SQLSTATE);

		while (true) {
			if ((throwable instanceof SQLException) &&
				_scanForSQLState((SQLException)throwable, sqlState)) {

				return true;
			}

			Throwable causeThrowable = throwable.getCause();

			if ((causeThrowable == null) || (causeThrowable == throwable)) {
				break;
			}

			throwable = causeThrowable;
		}

		return false;
	}

	@Override
	public boolean acceptResult(
		Object returnValue, Map<String, String> propertyMap) {

		return false;
	}

	private boolean _scanForSQLState(
		SQLException sqle, String expectedSQLState) {

		while (true) {
			String sqlState = sqle.getSQLState();

			if (Validator.isNotNull(sqlState) &&
				sqlState.startsWith(expectedSQLState)) {

				return true;
			}

			SQLException nextSQLE = sqle.getNextException();

			if ((nextSQLE == null) || nextSQLE.equals(sqle)) {
				break;
			}

			sqle = nextSQLE;
		}

		return false;
	}

}