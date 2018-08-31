/*******************************************************************************
 * ============LICENSE_START==================================================
 * * org.onap.dmaap
 * * ===========================================================================
 * * Copyright © 2017 AT&T Intellectual Property. All rights reserved.
 * * ===========================================================================
 * * Licensed under the Apache License, Version 2.0 (the "License");
 * * you may not use this file except in compliance with the License.
 * * You may obtain a copy of the License at
 * *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 * *
 *  * Unless required by applicable law or agreed to in writing, software
 * * distributed under the License is distributed on an "AS IS" BASIS,
 * * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * * See the License for the specific language governing permissions and
 * * limitations under the License.
 * * ============LICENSE_END====================================================
 * *
 * * ECOMP is a trademark and service mark of AT&T Intellectual Property.
 * *
 ******************************************************************************/
package org.onap.dmaap.datarouter.provisioning.beans;

import org.junit.*;
import org.onap.dmaap.datarouter.provisioning.utils.DB;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.util.Collection;
import java.util.Date;
import java.util.List;

public class GroupTest {
  private static EntityManagerFactory emf;
  private static EntityManager em;
  private Group group;
  private DB db;

  @BeforeClass
  public static void init() {
    emf = Persistence.createEntityManagerFactory("dr-unit-tests");
    em = emf.createEntityManager();
    System.setProperty(
        "org.onap.dmaap.datarouter.provserver.properties",
        "src/test/resources/h2Database.properties");
  }

  @AfterClass
  public static void tearDownClass() {
    em.clear();
    em.close();
    emf.close();
  }

  @Before
  public void setUp() throws Exception {
    db = new DB();
    group = new Group("GroupTest", "", "");
    group.doInsert(db.getConnection());
  }

  @Test
  public void Given_Group_Exists_In_Db_GetAllGroups_Returns_Correct_Group() {
    Collection<Group> groups = Group.getAllgroups();
    Assert.assertEquals("Group1", ((List<Group>) groups).get(0).getName());
  }

  @Test
  public void Given_Group_Inserted_Into_Db_GetGroupMatching_Returns_Created_Group() {
    Assert.assertEquals(group, Group.getGroupMatching(group));
  }

  @Test
  public void Given_Group_Inserted_With_Same_Name_GetGroupMatching_With_Id_Returns_Correct_Group()
      throws Exception {
    Group sameGroupName = new Group("GroupTest", "This group has a description", "");
    sameGroupName.doInsert(db.getConnection());
    Assert.assertEquals(
        "This group has a description", Group.getGroupMatching(group, 2).getDescription());
    sameGroupName.doDelete(db.getConnection());
  }

  @Test
  public void Given_Group_Inserted_GetGroupById_Returns_Correct_Group() {
    Assert.assertEquals(group, Group.getGroupById(group.getGroupid()));
  }

  @Test
  public void Given_Group_AuthId_Updated_GetGroupByAuthId_Returns_Correct_Group() throws Exception {
    group.setAuthid("Basic TmFtZTp6Z04wMFkyS3gybFppbXltNy94ZDhuMkdEYjA9");
    group.doUpdate(db.getConnection());
    Assert.assertEquals(group, Group.getGroupByAuthId("Basic TmFtZTp6Z04wMFkyS3gybFppbXltNy94ZDhuMkdEYjA9"));
  }

  @After
  public void tearDown() throws Exception {
    group.doDelete(db.getConnection());
  }
}
