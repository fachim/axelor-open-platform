/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2012-2014 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axelor.app;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axelor.common.reflections.Reflections;
import com.axelor.meta.loader.ModuleManager;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.inject.AbstractModule;

/**
 * The application module scans the classpath and finds all the
 * {@link AxelorModule} and installs them in the dependency order.
 * 
 */
public class AppModule extends AbstractModule {

	private static Logger log = LoggerFactory.getLogger(AppModule.class);

	private List<Class<? extends AxelorModule>> findAll() {
		
		final List<String> mods = ModuleManager.findInClassPath(false);
		final List<Class<? extends AxelorModule>> all = Lists.newArrayList();
		
		for (Class<? extends AxelorModule> module : Reflections
				.findSubTypesOf(AxelorModule.class)
				.having(AxelorModuleInfo.class)
				.find()) {
			String name = module.getAnnotation(AxelorModuleInfo.class).name();
			if (mods.contains(name)) {
				all.add(module);
			}
		}
		
		Collections.sort(all, new Comparator<Class<?>>() {
			
			@Override
			public int compare(Class<?> o1, Class<?> o2) {
				String n1 = o1.getAnnotation(AxelorModuleInfo.class).name();
				String n2 = o2.getAnnotation(AxelorModuleInfo.class).name();
				int idx1 = mods.indexOf(n1);
				int idx2 = mods.indexOf(n2);
				return idx1 - idx2;
			}
		});
				
		return all;
	}
	
	@Override
	protected void configure() {
		
		final List<Class<? extends AxelorModule>> all = findAll();
		if (all.isEmpty()) {
			return;
		}
		
		log.info("Configuring app modules...");
		for (Class<? extends AxelorModule> module : all) {
			try {
				log.info("Configure: {}", module.getName());
				install(module.newInstance());
			} catch (InstantiationException | IllegalAccessException e) {
				throw Throwables.propagate(e);
			}
		}
		log.info("App modules configured.");
	}
}
