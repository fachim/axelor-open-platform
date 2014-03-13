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
package com.axelor.auth;

import javax.inject.Inject;

import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.axelor.JpaTest;
import com.axelor.auth.db.Group;
import com.axelor.auth.db.Permission;
import com.axelor.auth.db.Role;
import com.axelor.auth.db.User;
import com.axelor.db.JPA;
import com.axelor.db.JpaSecurity.AccessType;
import com.google.inject.persist.Transactional;

public class AuthTest extends JpaTest {
	
	@Inject
	private AuthService authService;
	
	@Inject
	private AuthSecurity authSecurity;

	@Before
	@Transactional
	public void setUp() {
		if (User.all().count() == 0) {
			createDemoData();
		}
	}

	private void login(String user, String password) {
		final Subject subject = AuthUtils.getSubject();
		final UsernamePasswordToken token = new UsernamePasswordToken(user, password);
		
		subject.login(token);
	}

	private void createDemoData() {
		
		User admin = new User("admin", "Administrator");
		User demo = new User("demo", "Demo");
		User guest = new User("guest", "Guest");
		
		admin.setPassword("admin");
		demo.setPassword("demo");
		guest.setPassword("guest");
		
		Group admins = new Group("admins", "Administrators");
		Group users = new Group("users", "Users");
		
		admin.setGroup(admins);
		demo.setGroup(users);
		guest.setGroup(users);
		
		authService.encrypt(admin);
		authService.encrypt(demo);
		authService.encrypt(guest);
		
		admin.save();
		demo.save();
		guest.save();
		
		Role superUserRole = new Role("super.user");
		Role normalUserRole = new Role("normal.user");
		Role guestUserRole = new Role("guest.user");
		
		Permission grantAll = new Permission("grant.all");
		grantAll.setObject("com.axelor.auth.db.*");
		grantAll.setCanCreate(true);
		grantAll.setCanRead(true);
		grantAll.setCanWrite(true);
		grantAll.setCanRemove(true);
		grantAll.setCanExport(true);

		Permission grantRead = new Permission("grant.read");
		grantRead.setObject("com.axelor.auth.db.User");
		grantRead.setCanCreate(false);
		grantRead.setCanRead(true);
		grantRead.setCanWrite(false);
		grantRead.setCanRemove(false);
		grantRead.setCanExport(false);
		
		Permission grantReadSelf = new Permission("grant.read.self");
		grantReadSelf.setObject("com.axelor.auth.db.User");
		grantReadSelf.setCanCreate(false);
		grantReadSelf.setCanRead(true);
		grantReadSelf.setCanWrite(false);
		grantReadSelf.setCanRemove(false);
		grantReadSelf.setCanExport(false);
		grantReadSelf.setCondition("self.code = ?");
		grantReadSelf.setConditionParams("__user__.code");

		Permission grantWriteSelf = new Permission("grant.write.self");
		grantWriteSelf.setObject("com.axelor.auth.db.User");
		grantWriteSelf.setCanCreate(false);
		grantWriteSelf.setCanRead(true);
		grantWriteSelf.setCanWrite(true);
		grantWriteSelf.setCanRemove(false);
		grantWriteSelf.setCanExport(false);
		grantWriteSelf.setCondition("self.code = ?");
		grantWriteSelf.setConditionParams("__user__.code");
		
		superUserRole.addPermission(grantAll);
		normalUserRole.addPermission(grantWriteSelf);
		normalUserRole.addPermission(grantReadSelf);
		guestUserRole.addPermission(grantReadSelf);

		admins.addRole(superUserRole);
		users.addRole(normalUserRole);
		
		guest.addRole(guestUserRole);
		
		admin.save();
		demo.save();
		guest.save();
	}
	
	@Test
	public void testSuperUser() {
		login("admin", "admin");
		superUserTest();
	}
	
	@Transactional
	public void superUserTest() {
		// ensure has super user role
		Assert.assertTrue(authSecurity.hasRole("super.user"));
				
		// check that super use has full permissions
		Assert.assertTrue(authSecurity.isPermitted(AccessType.READ, User.class));
		Assert.assertTrue(authSecurity.isPermitted(AccessType.WRITE, Group.class));
		Assert.assertTrue(authSecurity.isPermitted(AccessType.CREATE, Role.class));
		Assert.assertTrue(authSecurity.isPermitted(AccessType.REMOVE, Permission.class));
	}
	
	@Test
	public void testNoramlUser() {
		login("demo", "demo");
		normalUserTest();
	}
	
	@Transactional
	public void normalUserTest() {
		// ensure has super user role
		Assert.assertTrue(authSecurity.hasRole("normal.user"));
		
		// but not super user role
		Assert.assertFalse(authSecurity.hasRole("super.user"));

		// check if has read access to User model
		Assert.assertTrue(authSecurity.isPermitted(AccessType.READ, User.class));
		
		// check if no other models are accessible
		Assert.assertFalse(authSecurity.isPermitted(AccessType.READ, Group.class));
		Assert.assertFalse(authSecurity.isPermitted(AccessType.READ, Role.class));
		Assert.assertFalse(authSecurity.isPermitted(AccessType.READ, Permission.class));
		
		// check if can update own user instance
		Assert.assertTrue(authSecurity.isPermitted(AccessType.WRITE, User.class, User.findByCode("demo").getId()));
		// but not others
		Assert.assertFalse(authSecurity.isPermitted(AccessType.WRITE, User.class, User.findByCode("admin").getId()));
	}
	
	@Test
	public void testGuestUser() {
		login("guest", "guest");
		guestUserTest();
	}
	
	@Transactional
	public void guestUserTest() {
		// ensure has super user role
		Assert.assertTrue(authSecurity.hasRole("guest.user"));

		// but not super user role
		Assert.assertFalse(authSecurity.hasRole("super.user"));

		// check if has read access to User model
		Assert.assertTrue(authSecurity.isPermitted(AccessType.READ, User.class));
		// and can only read self record
		Assert.assertTrue(authSecurity.isPermitted(AccessType.READ, User.class, User.findByCode("guest").getId()));
		// and not others
		Assert.assertFalse(authSecurity.isPermitted(AccessType.READ, User.class, User.findByCode("demo").getId()));
		
		// check if no other models are accessible
		Assert.assertFalse(authSecurity.isPermitted(AccessType.READ, Group.class));
		Assert.assertFalse(authSecurity.isPermitted(AccessType.READ, Role.class));
		Assert.assertFalse(authSecurity.isPermitted(AccessType.READ, Permission.class));
		
		// check if can update own user instance
		Assert.assertTrue(authSecurity.isPermitted(AccessType.WRITE, User.class, User.findByCode("guest").getId()));
		// but not others
		Assert.assertFalse(authSecurity.isPermitted(AccessType.WRITE, User.class, User.findByCode("demo").getId()));
	}

	@Test
	public void testEncrypt() {
		login("demo", "demo");
		encryptTest();
	}
	
	@Transactional
	public void encryptTest() {
		
		User user = User.all().filter("self.code = ?", "demo").fetchOne();

		Assert.assertNotNull(user);

		User current = AuthUtils.getUser();

		Assert.assertTrue(JPA.em().contains(current));

		JPA.clear();

		Assert.assertFalse(JPA.em().contains(current));

		User user2 = new User();
		user2.setCode("demo2");
		user2.setName("Demo2");
		user2.setPassword("demo2");

		authService.encrypt(user2);

		user2.save();

		Assert.assertNotNull(user2.getCreatedBy());

		authService.match("demo2", user2.getPassword());
	}
}
