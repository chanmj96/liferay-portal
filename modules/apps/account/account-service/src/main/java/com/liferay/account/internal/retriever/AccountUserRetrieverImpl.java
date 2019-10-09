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

package com.liferay.account.internal.retriever;

import com.liferay.account.retriever.AccountUserRetriever;
import com.liferay.account.service.AccountEntryUserRelLocalService;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.service.UserLocalService;
import com.liferay.portal.vulcan.util.TransformUtil;

import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Drew Brokke
 */
@Component(service = AccountUserRetriever.class)
public class AccountUserRetrieverImpl implements AccountUserRetriever {

	@Override
	public List<User> getAccountUsers(long accountEntryId) {
		return TransformUtil.transform(
			_accountEntryUserRelLocalService.
				getAccountEntryUserRelsByAccountEntryId(accountEntryId),
			accountEntryUserRel -> _userLocalService.getUserById(
				accountEntryUserRel.getAccountUserId()));
	}

	@Reference
	private AccountEntryUserRelLocalService _accountEntryUserRelLocalService;

	@Reference
	private UserLocalService _userLocalService;

}