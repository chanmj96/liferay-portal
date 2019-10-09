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

package com.liferay.app.builder.web.internal.deploy;

import com.liferay.app.builder.constants.AppBuilderAppConstants;
import com.liferay.app.builder.deploy.AppDeployer;
import com.liferay.app.builder.model.AppBuilderApp;
import com.liferay.app.builder.model.AppBuilderAppDeployment;
import com.liferay.app.builder.service.AppBuilderAppDeploymentLocalService;
import com.liferay.app.builder.service.AppBuilderAppLocalService;
import com.liferay.app.builder.web.internal.application.list.ProductMenuAppPanelApp;
import com.liferay.app.builder.web.internal.application.list.ProductMenuAppPanelCategory;
import com.liferay.app.builder.web.internal.constants.AppBuilderPanelCategoryKeys;
import com.liferay.app.builder.web.internal.constants.AppBuilderPortletKeys;
import com.liferay.app.builder.web.internal.portlet.AppPortlet;
import com.liferay.application.list.PanelApp;
import com.liferay.application.list.PanelCategory;
import com.liferay.application.list.constants.PanelCategoryKeys;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactory;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.util.HashMapDictionary;
import com.liferay.portal.kernel.util.LocaleThreadLocal;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import javax.portlet.Portlet;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Jeyvison Nascimento
 */
@Component(
	immediate = true, property = "app.builder.deploy.type=productMenu",
	service = AppDeployer.class
)
public class ProductMenuAppDeployer implements AppDeployer {

	@Activate
	public void activate(BundleContext bundleContext) {
		_bundleContext = bundleContext;
	}

	@Deactivate
	public void deactivate() {
		_bundleContext = null;

		_serviceRegistrationsMap.clear();
	}

	@Override
	public void deploy(long appId) throws Exception {
		String appBuilderPanelCategoryKey = _getPanelCategoryKey(appId);
		String portletName = _getPortletName(appId);

		AppBuilderApp appBuilderApp =
			_appBuilderAppLocalService.getAppBuilderApp(appId);

		String appName = appBuilderApp.getName(
			LocaleThreadLocal.getDefaultLocale());

		AppBuilderAppDeployment appBuilderAppDeployment =
			_appBuilderAppDeploymentLocalService.getAppBuilderAppDeployment(
				appId, "productMenu");

		JSONObject jsonObject = _jsonFactory.createJSONObject(
			appBuilderAppDeployment.getSettings());

		JSONArray jsonArray = jsonObject.getJSONArray("scope");

		if (jsonArray.length() == 2) {
			_serviceRegistrationsMap.computeIfAbsent(
				appId,
				key -> new ServiceRegistration<?>[] {
					_deployPortlet(appId, appName, portletName),
					_deployPanelApp(
						appBuilderPanelCategoryKey, portletName,
						JSONUtil.toLongArray(
							jsonObject.getJSONArray("siteIds"))),
					_deployPanelCategory(
						appBuilderPanelCategoryKey, appName,
						PanelCategoryKeys.CONTROL_PANEL),
					_deployPanelCategory(
						appBuilderPanelCategoryKey, appName,
						PanelCategoryKeys.SITE_ADMINISTRATION_CONTENT)
				});
		}
		else {
			_serviceRegistrationsMap.computeIfAbsent(
				appId,
				mapKey -> new ServiceRegistration<?>[] {
					_deployPortlet(appId, appName, portletName),
					_deployPanelApp(
						appBuilderPanelCategoryKey, portletName,
						JSONUtil.toLongArray(
							jsonObject.getJSONArray("siteIds"))),
					_deployPanelCategory(
						appBuilderPanelCategoryKey, appName,
						jsonArray.getString(0))
				});
		}

		appBuilderApp.setStatus(
			AppBuilderAppConstants.Status.DEPLOYED.getValue());

		_appBuilderAppLocalService.updateAppBuilderApp(appBuilderApp);
	}

	@Override
	public void undeploy(long appId) throws Exception {
		ServiceRegistration<?>[] serviceRegistrations =
			_serviceRegistrationsMap.remove(appId);

		if (serviceRegistrations == null) {
			return;
		}

		for (ServiceRegistration serviceRegistration : serviceRegistrations) {
			serviceRegistration.unregister();
		}

		AppBuilderApp appBuilderApp =
			_appBuilderAppLocalService.getAppBuilderApp(appId);

		appBuilderApp.setStatus(
			AppBuilderAppConstants.Status.UNDEPLOYED.getValue());

		_appBuilderAppLocalService.updateAppBuilderApp(appBuilderApp);
	}

	private ServiceRegistration<?> _deployPanelApp(
		String panelCategoryKey, String portletName, long[] siteIds) {

		return _bundleContext.registerService(
			PanelApp.class, new ProductMenuAppPanelApp(portletName, siteIds),
			new HashMapDictionary<String, Object>() {
				{
					put("panel.app.order:Integer", 100);
					put("panel.category.key", panelCategoryKey);
				}
			});
	}

	private ServiceRegistration<?> _deployPanelCategory(
		String panelCategoryKey, String appName,
		String parentPanelCategoryKey) {

		Dictionary<String, Object> properties =
			new HashMapDictionary<String, Object>() {
				{
					put("key", panelCategoryKey);
					put("label", appName);
					put("panel.category.key", parentPanelCategoryKey);
					put("panel.category.order:Integer", 600);
				}
			};

		return _bundleContext.registerService(
			PanelCategory.class, new ProductMenuAppPanelCategory(properties),
			properties);
	}

	private ServiceRegistration<?> _deployPortlet(
		long appId, String appName, String portletName) {

		return _bundleContext.registerService(
			Portlet.class, new AppPortlet(appId),
			AppPortlet.getProperties(appName, portletName, new HashMap<>()));
	}

	private String _getPanelCategoryKey(long appId) {
		return AppBuilderPanelCategoryKeys.CONTROL_PANEL_APP_BUILDER_APP +
			appId;
	}

	private String _getPortletName(long appId) {
		return AppBuilderPortletKeys.PRODUCT_MENU_APP + "_" + appId;
	}

	@Reference
	private AppBuilderAppDeploymentLocalService
		_appBuilderAppDeploymentLocalService;

	@Reference
	private AppBuilderAppLocalService _appBuilderAppLocalService;

	private BundleContext _bundleContext;

	@Reference
	private JSONFactory _jsonFactory;

	private final ConcurrentHashMap<Long, ServiceRegistration<?>[]>
		_serviceRegistrationsMap = new ConcurrentHashMap<>();

}