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

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Date;


@RunWith(PowerMockRunner.class)
@SuppressStaticInitializationFor({"org.onap.dmaap.datarouter.provisioning.beans.Group"})
public class GroupTest {
    private Group group;

    @Test
    public void Validate_Group_Created_With_Default_Contructor() {
        group = new Group();
        Assert.assertEquals(group.getGroupid(), -1);
        Assert.assertEquals(group.getName(), "");
    }

    @Test
    public void Validate_Getters_And_Setters() {
        group = new Group();
        group.setGroupid(1);
        group.setAuthid("Auth");
        group.setClassification("Class");
        group.setDescription("Description");
        Date date = new Date();
        group.setLast_mod(date);
        group.setMembers("Members");
        group.setName("NewName");
        Assert.assertEquals(1, group.getGroupid());
        Assert.assertEquals("Auth", group.getAuthid());
        Assert.assertEquals("Class", group.getClassification());
        Assert.assertEquals("Description", group.getDescription());
        Assert.assertEquals(date, group.getLast_mod());
        Assert.assertEquals("Members", group.getMembers());
    }

    @Test
    public void Validate_Equals() {
        group = new Group();
        group.setGroupid(1);
        group.setAuthid("Auth");
        group.setClassification("Class");
        group.setDescription("Description");
        Date date = new Date();
        group.setLast_mod(date);
        group.setMembers("Members");
        group.setName("NewName");
        Group group2 = new Group("NewName", "Description", "Members");
        group2.setGroupid(1);
        group2.setAuthid("Auth");
        group2.setClassification("Class");
        group2.setLast_mod(date);
        Assert.assertEquals(group, group2);
    }
}
