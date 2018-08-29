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

package org.onap.dmaap.datarouter.provisioning.utils;

import java.io.CharArrayWriter;
import java.io.Writer;
import org.json.JSONArray;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;
import java.util.HashMap;
import java.util.Map;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;


@RunWith(PowerMockRunner.class)
public class LOGJSONObjectTest {

  private static LOGJSONObject logJO;

  @Before
  public void setUp() throws Exception {
    Map<String, Object> map = new HashMap<>();
    map.put("key", null);
    logJO = new LOGJSONObject(map);
  }

  @Test
  public void Given_Method_Is_Accumulate_And_Value_Is_Valid_Put_Value_Into_New_JSONArray()
      throws Exception {
    Map<String, Object> map = new HashMap<>();
    map.put("key", 3);
    LOGJSONObject logJObject = new LOGJSONObject(map);

    String s = "key";
    logJObject.accumulate(s, null);
    assertThat(logJObject.get("key").toString(), is("[3,null]"));
  }

  @Test
  public void Given_Method_Is_Accumulate_And_Value_Is_Null_Dont_Add_Key_Value_Pair()
      throws Exception {
    String s = "key";
    logJO.accumulate(s, null);
    assertThat(logJO.has("key"), is(false));
  }

  @Test
  public void Given_Method_Is_Append_And_Value_Is_Null_Append_New_Value()
      throws Exception {
    String s = "key";
    double d = 2.0;
    logJO.append(s, d);
    assertThat(logJO.getJSONArray("key").get(0), is(2.0));
  }


  @Test
  public void Given_Method_Is_DoubleToString_And_Value_Is_NaN_Return_Null()
      throws Exception {
    double d = 2.0;
    assertThat(logJO.doubleToString(d), is("2"));
  }


  @Test
  public void Given_Method_Is_GetBoolean_And_Value_Is_False_Return_False()
      throws Exception {
    Map<String, Object> map = new HashMap<>();
    map.put("key", false);
    LOGJSONObject logJObject = new LOGJSONObject(map);

    String s = "key";
    assertThat(logJObject.getBoolean(s), is(false));
  }

  @Test
  public void Given_Method_Is_GetBoolean_And_Value_Is_True_Return_True()
      throws Exception {
    Map<String, Object> map = new HashMap<>();
    map.put("key", true);
    LOGJSONObject logJObject = new LOGJSONObject(map);

    String s = "key";
    assertThat(logJObject.getBoolean(s), is(true));
  }

  @Test
  public void Given_Method_Is_GetDouble_And_Value_Is_A_Double_Return_Value()
      throws Exception {
    Map<String, Object> map = new HashMap<>();
    map.put("key", 2.0);
    LOGJSONObject logJObject = new LOGJSONObject(map);

    String s = "key";
    assertThat(logJObject.getDouble(s), is(2.0));
  }

  @Test
  public void Given_Method_Is_GetInt_And_Value_Is_An_Int_Return_Value()
      throws Exception {
    Map<String, Object> map = new HashMap<>();
    map.put("key", 3);
    LOGJSONObject logJObject = new LOGJSONObject(map);

    String s = "key";
    assertThat(logJObject.getInt(s), is(3));
  }

  @Test
  public void Given_Method_Is_GetJSONArray_And_Value_Is_A_JSONArray_Return_Value()
      throws Exception {
    JSONArray jA = new JSONArray();
    Map<String, Object> map = new HashMap<>();
    map.put("key", jA);
    LOGJSONObject logJObject = new LOGJSONObject(map);

    String s = "key";
    assertThat(logJObject.getJSONArray(s), is(jA));
  }

  @Test
  public void Given_Method_Is_GetJSONObject_And_Value_Is_A_JSONObject_Return_Value()
      throws Exception {
    LOGJSONObject logJObj = new LOGJSONObject();
    logJObj.put("stub_key", 1);
    Map<String, Object> map = new HashMap<>();
    map.put("key", logJObj);
    LOGJSONObject logJObject = new LOGJSONObject(map);

    String s = "key";
    assertThat(logJObject.getJSONObject(s), is(logJObj));
  }

  @Test
  public void Given_Method_Is_GetLong_And_Value_Is_A_Long_Return_Value()
      throws Exception {
    long l = 5;
    Map<String, Object> map = new HashMap<>();
    map.put("key", l);
    LOGJSONObject logJObject = new LOGJSONObject(map);

    String s = "key";
    assertThat(logJObject.getLong(s), is(5L));
  }

  @Test
  public void Given_Method_Is_getNames_And_Value_Is_A_LOGJSONObject_Return_StringArray()
      throws Exception {
    LOGJSONObject logJObj = new LOGJSONObject();
    logJObj.put("name", "stub_name");
    String[] sArray = new String[logJObj.length()];
    sArray[0] = "name";
    Map<String, Object> map = new HashMap<>();
    map.put("key", sArray);
    LOGJSONObject logJObject = new LOGJSONObject(map);

    String s = "key";
    assertThat(logJObject.getNames(logJObj), is(sArray));
  }

  @Test
  public void Given_Method_Is_GetString_And_Value_Is_A_String_Return_Value()
      throws Exception {
    String val = "value";
    Map<String, Object> map = new HashMap<>();
    map.put("key", val);
    LOGJSONObject logJObject = new LOGJSONObject(map);

    String s = "key";
    assertThat(logJObject.getString(s), is("value"));
  }

  @Test
  public void Given_Method_Is_Increment_And_Value_Is_Null_Put_Defualt_Value()
      throws Exception {
    Map<String, Object> mapResult = new HashMap<>();
    mapResult.put("key", 1);
    LOGJSONObject logJObjectResult = new LOGJSONObject(mapResult);

    String val = null;
    Map<String, Object> map = new HashMap<>();
    map.put("key", val);
    LOGJSONObject logJObject = new LOGJSONObject(map);

    String s = "key";
    logJObject.increment(s);
    assertThat(logJObject.get("key"), is(logJObjectResult.get("key")));
  }

  @Test
  public void Given_Method_Is_Increment_And_Value_Is_An_Int_Put_Value_Plus_One()
      throws Exception {
    Map<String, Object> mapResult = new HashMap<>();
    mapResult.put("key", 3);
    LOGJSONObject logJObjectResult = new LOGJSONObject(mapResult);

    int val = 2;
    Map<String, Object> map = new HashMap<>();
    map.put("key", val);
    LOGJSONObject logJObject = new LOGJSONObject(map);

    String s = "key";
    logJObject.increment(s);
    assertThat(logJObject.get("key"), is(logJObjectResult.get("key")));
  }

  @Test
  public void Given_Method_Is_Increment_And_Value_Is_A_Long_Put_Value_Plus_One()
      throws Exception {
    Map<String, Object> mapResult = new HashMap<>();
    mapResult.put("key", 4L);
    LOGJSONObject logJObjectResult = new LOGJSONObject(mapResult);

    long val = 3;
    Map<String, Object> map = new HashMap<>();
    map.put("key", val);
    LOGJSONObject logJObject = new LOGJSONObject(map);

    String s = "key";
    logJObject.increment(s);
    assertThat(logJObject.get("key"), is(logJObjectResult.get("key")));
  }

  @Test
  public void Given_Method_Is_Increment_And_Value_Is_A_Double_Put_Value_Plus_One()
      throws Exception {
    Map<String, Object> mapResult = new HashMap<>();
    mapResult.put("key", 5.0);
    LOGJSONObject logJObjectResult = new LOGJSONObject(mapResult);

    double val = 4.0;
    Map<String, Object> map = new HashMap<>();
    map.put("key", val);
    LOGJSONObject logJObject = new LOGJSONObject(map);

    String s = "key";
    logJObject.increment(s);
    assertThat(logJObject.get("key"), is(logJObjectResult.get("key")));
  }

  @Test
  public void Given_Method_Is_Increment_And_Value_Is_A_Float_Put_Value_Plus_One()
      throws Exception {
    Map<String, Object> mapResult = new HashMap<>();
    mapResult.put("key", 5.0);
    LOGJSONObject logJObjectResult = new LOGJSONObject(mapResult);

    float val = 4.0f;
    Map<String, Object> map = new HashMap<>();
    map.put("key", val);
    LOGJSONObject logJObject = new LOGJSONObject(map);

    String s = "key";
    logJObject.increment(s);
    assertThat(logJObject.get("key"), is(logJObjectResult.get("key")));
  }

  @Test
  public void Given_Method_Is_Names_And_Object_Contains_Keys_Put_Keys_Into_New_JSONArray()
      throws Exception {
    JSONArray ja = new JSONArray();
    ja.put("key");

    String val = "value";
    Map<String, Object> map = new HashMap<>();
    map.put("key", val);
    LOGJSONObject logJObject = new LOGJSONObject(map);

    String s = "key";
    assertThat(logJObject.names().get(0), is(ja.get(0)));
  }

  @Test
  public void Given_Method_Is_NumberToString_And_Number_is_Not_Null_Return_Reformatted_Number_As_String()
      throws Exception {
    Number num = 3.0;

    String val = "value";
    Map<String, Object> map = new HashMap<>();
    map.put("key", val);
    LOGJSONObject logJObject = new LOGJSONObject(map);

    String s = "key";
    assertThat(logJObject.numberToString(num), is("3"));
  }

  @Test
  public void Given_Method_Is_OptBoolean_And_Value_is_Boolean_Return_Value()
      throws Exception {
    boolean val = true;
    Map<String, Object> map = new HashMap<>();
    map.put("key", val);
    LOGJSONObject logJObject = new LOGJSONObject(map);

    String s = "key";
    assertThat(logJObject.optBoolean(s, false), is(true));
  }

  @Test
  public void Given_Method_Is_OptBoolean_And_Value_is_Not_Boolean_Return_Default_Value()
      throws Exception {
    String val = "not_boolean";
    Map<String, Object> map = new HashMap<>();
    map.put("key", val);
    LOGJSONObject logJObject = new LOGJSONObject(map);

    String s = "key";
    assertThat(logJObject.optBoolean(s, false), is(false));
  }

  @Test
  public void Given_Method_Is_OptDouble_And_Value_is_Double_Return_Value()
      throws Exception {
    double val = 2.0;
    Map<String, Object> map = new HashMap<>();
    map.put("key", val);
    LOGJSONObject logJObject = new LOGJSONObject(map);

    String s = "key";
    assertThat(logJObject.optDouble(s, 0.0), is(2.0));
  }

  @Test
  public void Given_Method_Is_OptDouble_And_Value_is_Not_Double_Return_Default_Value()
      throws Exception {
    String val = "not_double";
    Map<String, Object> map = new HashMap<>();
    map.put("key", val);
    LOGJSONObject logJObject = new LOGJSONObject(map);

    String s = "key";
    assertThat(logJObject.optDouble(s, 0.0), is(0.0));
  }

  @Test
  public void Given_Method_Is_OptInt_And_Value_is_Int_Return_Value()
      throws Exception {
    int val = 1;
    Map<String, Object> map = new HashMap<>();
    map.put("key", val);
    LOGJSONObject logJObject = new LOGJSONObject(map);

    String s = "key";
    assertThat(logJObject.optInt(s, 0), is(1));
  }

  @Test
  public void Given_Method_Is_OptInt_And_Value_Is_Null_Return_Default_Value()
      throws Exception {
    Map<String, Object> map = new HashMap<>();
    map.put("key", null);
    LOGJSONObject logJObject = new LOGJSONObject(map);

    String s = "key";
    assertThat(logJObject.optInt(s, 0), is(0));
  }

  @Test
  public void Given_Method_Is_OptLong_And_Value_is_Long_Return_Value()
      throws Exception {
    long val = 4;
    Map<String, Object> map = new HashMap<>();
    map.put("key", val);
    LOGJSONObject logJObject = new LOGJSONObject(map);

    String s = "key";
    assertThat(logJObject.optLong(s, 0), is(4L));
  }

  @Test
  public void Given_Method_Is_OptLong_And_Value_is_Not_Long_Return_Default_Value()
      throws Exception {
    Map<String, Object> map = new HashMap<>();
    map.put("key", null);
    LOGJSONObject logJObject = new LOGJSONObject(map);

    String s = "key";
    assertThat(logJObject.optLong(s, 0), is(0L));
  }

  @Test
  public void Given_Method_Is_OptString_And_Value_is_String_Return_Value()
      throws Exception {
    String val = "value";
    Map<String, Object> map = new HashMap<>();
    map.put("key", val);
    LOGJSONObject logJObject = new LOGJSONObject(map);

    String s = "key";
    assertThat(logJObject.optString(s, "default_value"), is("value"));
  }

  @Test
  public void Given_Method_Is_putOnce_And_KeyValuePair_Does_Not_Exist_In_logJObject_Put_KeyValuePair_Into_logJObject()
      throws Exception {
    String val = "value";
    Map<String, Object> map = new HashMap<>();
    LOGJSONObject logJObject = new LOGJSONObject(map);

    String s = "key";
    assertThat(logJObject.putOnce(s, val).get("key"), is("value"));
  }

  @Test
  public void Given_Method_Is_StringToValue_And_Value_Is_Number_Return_Number()
      throws Exception {
    String val = "312";
    Map<String, Object> map = new HashMap<>();
    LOGJSONObject logJObject = new LOGJSONObject(map);

    assertThat(logJObject.stringToValue(val), is(312));
  }

  @Test
  public void Given_Method_Is_ToJSONArray_And_KeyValue_Exists_Return_Value_Array()
      throws Exception {
    JSONArray names = new JSONArray();
    Map<String, Object> map = new HashMap<>();
    map.put("name", "value");
    names.put("name");
    LOGJSONObject logJObject = new LOGJSONObject(map);

    assertThat(logJObject.toJSONArray(names).get(0), is("value"));
  }

  @Test
  public void Given_Method_Is_ValueToString_And_Value_Is_JSONArray_Return_Value_To_String()
      throws Exception {
    JSONArray val = new JSONArray();
    Map<String, Object> map = new HashMap<>();
    map.put("key", "value");
    val.put("value");
    LOGJSONObject logJObject = new LOGJSONObject(map);

    assertThat(logJObject.valueToString(val), is("[\"value\"]"));
  }

  @Test
  public void Given_Method_Is_writeValue_And_Value_IS_Not_Null_Return_Writer_With_Value()
      throws Exception {
    Writer writer = new CharArrayWriter();
    String val = "value";
    Map<String, Object> map = new HashMap<>();
    map.put("key", "value");
    LOGJSONObject logJObject = new LOGJSONObject(map);

    assertThat(logJObject.writeValue(writer, val, 3, 1).toString(), is("\"value\""));
  }

  @Test
  public void Given_Method_Is_write_And_Length_Of_logJObject_Is_One_Write_Value_With_Indent()
      throws Exception {
    Writer writer = new CharArrayWriter();
    String val = "value";
    Map<String, Object> map = new HashMap<>();
    map.put("key", "value");
    LOGJSONObject logJObject = new LOGJSONObject(map);

    assertThat(logJObject.write(writer, 3, 1).toString(), is("{\"key\": \"value\"}"));
  }

  @Test
  public void Given_Method_Is_write_And_Length_Of_logJObject_Is_Not_One_Or_Zero_Write_Value_With_New_Indent()
      throws Exception {
    Writer writer = new CharArrayWriter();
    String val = "value";
    Map<String, Object> map = new HashMap<>();
    map.put("key", "value");
    map.put("key1", "value1");
    LOGJSONObject logJObject = new LOGJSONObject(map);

    assertThat(logJObject.write(writer, 3, 1).toString(), is("{\n    \"key1\": \"value1\",\n    \"key\": \"value\"\n }"));
  }
}
