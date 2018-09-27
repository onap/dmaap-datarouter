/*******************************************************************************
 * ============LICENSE_START==================================================
 * * org.onap.dmaap
 * * ===========================================================================
 * * Copyright Â© 2017 AT&T Intellectual Property. All rights reserved.
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

package org.onap.dmaap.datarouter.provisioning.utils;


import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class RLEBitSetTest {


    private static RLEBitSet RLEBSet;

    @Before
    public void setUp() throws Exception {
        RLEBSet = new RLEBitSet();
    }

    @Test
    public void Given_Method_Is_Length_And_BitSet_Is_Empty_Return_0() {
        assertThat(RLEBSet.length(), is(0L));
    }

    @Test
    public void Given_Method_Is_Length_And_BitSet_Is_Not_Empty_Return_Length() {
        RLEBSet.set(2L, 5L);
        assertThat(RLEBSet.length(), is(5L));
    }

    @Test
    public void Given_Method_Is_Get_And_Value_Is_Inside_BitSet_Return_True() {
        long val = 4L;
        RLEBSet.set(2L, 7L);
        assertThat(RLEBSet.get(val), is(true));
    }

    @Test
    public void Given_Method_Is_Get_And_Value_Is_Not_Inside_BitSet_Return_False() {
        long val = 9L;
        RLEBSet.set(2L, 7L);
        assertThat(RLEBSet.get(val), is(false));
    }

    @Test
    public void Given_Method_Is_Set_And_Value_Is_Range_And_Is_Outside_BitSet_Then_Update_Range() {
        String val = "4-8";
        RLEBSet.set(2L, 7L);
        RLEBSet.set(val);
        assertThat(RLEBSet.toString(), is("2-8"));
    }

    @Test
    public void Given_Method_Is_Set_And_Value_Is_Not_Range_And_Is_Inside_BitSet_Then_Keep_Same_Range() {
        String val = "3";
        RLEBSet.set(2L, 7L);
        RLEBSet.set(val);
        assertThat(RLEBSet.toString(), is("2-6"));
    }

    @Test
    public void Given_Method_Is_Set_And_Value_Is_Not_Range_And_Is_Outside_BitSet_Then_Add_New_Range() {
        String val = "9";
        RLEBSet.set(2L, 7L);
        RLEBSet.set(val);
        assertThat(RLEBSet.toString(), is("2-6,9"));
    }

    @Test
    public void Given_Method_Is_Clear_Return_Blank_Set() {
        RLEBSet.set(2L, 7L);
        RLEBSet.clear();
        assertThat(RLEBSet.toString(), is(""));
    }

    @Test
    public void Given_Method_Is_Clear_ValueRange_And_Range_Overlaps_BitSet_Return_Trimmed_BitSet() {
        long from = 1L;
        long to = 4L;
        RLEBSet.set(2L, 7L);
        RLEBSet.clear(from, to);
        assertThat(RLEBSet.toString(), is("4-6"));
    }

    @Test
    public void Given_Method_Is_Clear_ValueRange_And_Range_Is_Inside_BitSet_Return_Split_BitSets() {
        long from = 4L;
        long to = 7L;
        RLEBSet.set(2L, 9L);
        RLEBSet.clear(from, to);
        assertThat(RLEBSet.toString(), is("2-3,7-8"));
    }

    @Test
    public void Given_Method_Is_Clear_ValueRange_And_Range_Is_Outside_BitSet_Return_Unchanged_BitSets() {
        long from = 8L;
        long to = 11L;
        RLEBSet.set(2L, 7L);
        RLEBSet.clear(from, to);
        assertThat(RLEBSet.toString(), is("2-6"));
    }

    @Test
    public void Given_Method_Is_And_Return_Common_Value_Set() {
        RLEBitSet bitSet = new RLEBitSet();
        bitSet.set(6L, 11L);
        RLEBSet.set(2L, 9L);
        RLEBSet.and(bitSet);
        assertThat(RLEBSet.toString(), is("6-8"));
    }

    @Test
    public void Given_Method_Is_AndNot_Return_Non_Common_Value_Set() {
        RLEBitSet bitSet = new RLEBitSet();
        bitSet.set(6L, 11L);
        RLEBSet.set(2L, 9L);
        RLEBSet.andNot(bitSet);
        assertThat(RLEBSet.toString(), is("2-5"));
    }

    @Test
    public void Given_Method_Is_Cardinality_Return_Bits_Set_To_True() {
        RLEBSet.set(2L, 9L);
        assertThat(RLEBSet.cardinality(), is(7));
    }

    @Test
    public void Given_Method_Is_Clone_Return_Identical_Clone() {
        RLEBSet.set(2L, 9L);
        assertThat(RLEBSet.clone().toString(), is(RLEBSet.toString()));
    }
}
