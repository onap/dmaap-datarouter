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

package datarouter.provisioning;


import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
        IntegrationTestDrFeedsPost.class,
        IntegrationTestDrFeedsGet.class,
        IntegrationTestDrFeedsPut.class,
        IntegrationTestDrFeedsDelete.class,
        IntegrationTestFeedPut.class,
        IntegrationTestSubscribePost.class,
        IntegrationTestInternalGet.class,
        IntegrationTestInternalMisc.class,
        IntegrationTestPublish.class,
        IntegrationTestLogGet.class,
        IntegrationTestFeedDelete.class,
        IntegrationTestCleanup.class,
        IntegrationTestRleBitSet.class
    })

public class AllTests {
}
