package org.onap.dmaap.datarouter.provisioning.utils;

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

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONString;
import org.json.JSONTokener;

/**
 * A JSONObject is an unordered collection of name/value pairs. Its external
 * form is a string wrapped in curly braces with colons between the names and
 * values, and commas between the values and names. The internal form is an
 * object having <code>get</code> and <code>opt</code> methods for accessing the
 * values by name, and <code>put</code> methods for adding or replacing values
 * by name. The values can be any of these types: <code>Boolean</code>,
 * <code>JSONArray</code>, <code>JSONObject</code>, <code>Number</code>,
 * <code>String</code>, or the <code>JSONObject.NULL</code> object. A JSONObject
 * constructor can be used to convert an external form JSON text into an
 * internal form whose values can be retrieved with the <code>get</code> and
 * <code>opt</code> methods, or to convert values into a JSON text using the
 * <code>put</code> and <code>toString</code> methods. A <code>get</code> method
 * returns a value if one can be found, and throws an exception if one cannot be
 * found. An <code>opt</code> method returns a default value instead of throwing
 * an exception, and so is useful for obtaining optional values.
 *
 * <p>The generic <code>get()</code> and <code>opt()</code> methods return an
 * object, which you can cast or query for type. There are also typed
 * <code>get</code> and <code>opt</code> methods that do type checking and type
 * coercion for you. The opt methods differ from the get methods in that they do
 * not throw. Instead, they return a specified value, such as null.
 *
 * <p>The <code>put</code> methods add or replace values in an object. For example,
 *
 * <pre>
 * myString = new JSONObject().put(&quot;JSON&quot;, &quot;Hello, World!&quot;).toString();
 * </pre>
 *
 * <p>* produces the string <code>{"JSON": "Hello, World"}</code>.
 *
 * <p>The texts produced by the <code>toString</code> methods strictly conform to
 * the JSON syntax rules. The constructors are more forgiving in the texts they
 * will accept:
 * <ul>
 * <li>An extra <code>,</code>&nbsp;<small>(comma)</small> may appear just
 * before the closing brace.</li>
 * <li>Strings may be quoted with <code>'</code>&nbsp;<small>(single
 * quote)</small>.</li>
 * <li>Strings do not need to be quoted at all if they do not begin with a quote
 * or single quote, and if they do not contain leading or trailing spaces, and
 * if they do not contain any of these characters:
 * <code>{ } [ ] / \ : , = ; #</code> and if they do not look like numbers and
 * if they are not the reserved words <code>true</code>, <code>false</code>, or
 * <code>null</code>.</li>
 * <li>Keys can be followed by <code>=</code> or <code>=></code> as well as by
 * <code>:</code>.</li>
 * <li>Values can be followed by <code>;</code> <small>(semicolon)</small> as
 * well as by <code>,</code> <small>(comma)</small>.</li>
 * </ul>
 *
 * @author JSON.org
 * @version 2012-12-01
 */

public class LOGJSONObject {

    /**
     * The maximum number of keys in the key pool.
     */
    private static final int KEY_POOL_SIZE = 100;
    private static final String USING_DEFAULT_VALUE = "Using defaultValue: ";
    private static final String JSON_OBJECT_CONST = "JSONObject[";

    /**
     * Key pooling is like string interning, but without permanently tying up
     * memory. To help conserve memory, storage of duplicated key strings in
     * JSONObjects will be avoided by using a key pool to manage unique key
     * string objects. This is used by JSONObject.put(string, object).
     */
    private static Map<String, Object> keyPool = new LinkedHashMap<>(KEY_POOL_SIZE);

    private static final EELFLogger intlogger = EELFManager.getInstance().getLogger("InternalLog");

    /**
     * JSONObject.NULL is equivalent to the value that JavaScript calls null,
     * whilst Java's null is equivalent to the value that JavaScript calls
     * undefined.
     */
    private static final class Null {

        /**
         * There is only intended to be a single instance of the NULL object,
         * so the clone method returns itself.
         *
         * @return NULL.
         */
        protected final Object clone() {
            return this;
        }

        /**
         * A Null object is equal to the null value and to itself.
         *
         * @param object An object to test for nullness.
         * @return true if the object parameter is the JSONObject.NULL object
         * or null.
         */
        public boolean equals(Object object) {
            return object == null || object == this;
        }

        /**
         * Returns a hash code value for the object. This method is
         * supported for the benefit of hash tables such as those provided by
         * {@link HashMap}.
         *
         * <p>* The general contract of {@code hashCode} is:
         * <ul>
         * <li>Whenever it is invoked on the same object more than once during
         * an execution of a Java application, the {@code hashCode} method
         * must consistently return the same integer, provided no information
         * used in {@code equals} comparisons on the object is modified.
         * This integer need not remain consistent from one execution of an
         * application to another execution of the same application.
         * <li>If two objects are equal according to the {@code equals(Object)}
         * method, then calling the {@code hashCode} method on each of
         * the two objects must produce the same integer result.
         * <li>It is <em>not</em> required that if two objects are unequal
         * according to the {@link Object#equals(Object)}
         * method, then calling the {@code hashCode} method on each of the
         * two objects must produce distinct integer results.  However, the
         * programmer should be aware that producing distinct integer results
         * for unequal objects may improve the performance of hash tables.
         * </ul>
         *
         * <p>* As much as is reasonably practical, the hashCode method defined by
         * class {@code Object} does return distinct integers for distinct
         * objects. (This is typically implemented by converting the internal
         * address of the object into an integer, but this implementation
         * technique is not required by the
         * Java&trade; programming language.)
         *
         * @return a hash code value for this object.
         * @see Object#equals(Object)
         * @see System#identityHashCode
         */
        @Override
        public int hashCode() {
            return super.hashCode();
        }

        /**
         * Get the "null" string value.
         *
         * @return The string "null".
         */
        public String toString() {
            return "null";
        }
    }

    /**
     * The map where the JSONObject's properties are kept.
     */
    private final Map<String, Object> map;

    /**
     * It is sometimes more convenient and less ambiguous to have a
     * <code>NULL</code> object than to use Java's <code>null</code> value.
     * <code>JSONObject.NULL.equals(null)</code> returns <code>true</code>.
     * <code>JSONObject.NULL.toString()</code> returns <code>"null"</code>.
     */
    private static final Object NULL = new Null();

    /**
     * Construct an empty JSONObject.
     */
    public LOGJSONObject() {
        this.map = new LinkedHashMap<>();
    }

    /**
     * Construct a JSONObject from a subset of another JSONObject.
     * An array of strings is used to identify the keys that should be copied.
     * Missing keys are ignored.
     *
     * @param jo    A JSONObject.
     * @param names An array of strings.
     */
    public LOGJSONObject(LOGJSONObject jo, String[] names) {
        this();
        for (int i = 0; i < names.length; i += 1) {
            try {
                this.putOnce(names[i], jo.opt(names[i]));
            } catch (Exception ignore) {
                intlogger.error("PROV0001 LOGJSONObject: " + ignore.getMessage(), ignore);
            }
        }
    }

    /**
     * Construct a JSONObject from a JSONTokener.
     *
     * @param tokener A JSONTokener object containing the source string.
     * @throws JSONException If there is a syntax error in the source string
     *                       or a duplicated key.
     */
    public LOGJSONObject(JSONTokener tokener) {
        this();
        char chr;
        String key;

        if (tokener.nextClean() != '{') {
            throw tokener.syntaxError("A JSONObject text must begin with '{'");
        }
        for (; ; ) {
            chr = tokener.nextClean();
            switch (chr) {
                case 0:
                    throw tokener.syntaxError("A JSONObject text must end with '}'");
                case '}':
                    return;
                default:
                    tokener.back();
                    key = tokener.nextValue().toString();
            }

            // The key is followed by ':'. We will also tolerate '=' or '=>'.

            chr = tokener.nextClean();
            if (chr == '=') {
                if (tokener.next() != '>') {
                    tokener.back();
                }
            } else if (chr != ':') {
                throw tokener.syntaxError("Expected a ':' after a key");
            }
            this.putOnce(key, tokener.nextValue());

            // Pairs are separated by ','. We will also tolerate ';'.

            switch (tokener.nextClean()) {
                case ';':
                case ',':
                    if (tokener.nextClean() == '}') {
                        return;
                    }
                    tokener.back();
                    break;
                case '}':
                    return;
                default:
                    throw tokener.syntaxError("Expected a ',' or '}'");
            }
        }
    }

    /**
     * Construct a JSONObject from a Map.
     *
     * @param map A map object that can be used to initialize the contents of
     *            the JSONObject.
     * @throws JSONException json exception
     */
    public LOGJSONObject(Map<String, Object> map) {
        this.map = new LinkedHashMap<>();
        if (map != null) {
            Iterator<Map.Entry<String, Object>> iterator = map.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, Object> entry = iterator.next();
                Object value = entry.getValue();
                if (value != null) {
                    this.map.put(entry.getKey(), wrap(value));
                }
            }
        }
    }

    /**
     * Construct a JSONObject from an Object using bean getters.
     * It reflects on all of the public methods of the object.
     * For each of the methods with no parameters and a name starting
     * with <code>"get"</code> or <code>"is"</code> followed by an uppercase letter,
     * the method is invoked, and a key and the value returned from the getter method
     * are put into the new JSONObject.
     *
     * <p>* The key is formed by removing the <code>"get"</code> or <code>"is"</code> prefix.
     * If the second remaining character is not upper case, then the first
     * character is converted to lower case.
     *
     * <p>* For example, if an object has a method named <code>"getName"</code>, and
     * if the result of calling <code>object.getName()</code> is <code>"Larry Fine"</code>,
     * then the JSONObject will contain <code>"name": "Larry Fine"</code>.
     *
     * @param bean An object that has getter methods that should be used
     *             to make a JSONObject.
     */
    public LOGJSONObject(Object bean) {
        this();
        this.populateMap(bean);
    }

    /**
     * Accumulate values under a key. It is similar to the put method except
     * that if there is already an object stored under the key then a
     * JSONArray is stored under the key to hold all of the accumulated values.
     * If there is already a JSONArray, then the new value is appended to it.
     * In contrast, the put method replaces the previous value.
     *
     * <p>* If only one value is accumulated that is not a JSONArray, then the
     * result will be the same as using put. But if multiple values are
     * accumulated, then the result will be like append.
     *
     * @param key   A key string.
     * @param value An object to be accumulated under the key.
     * @return this.
     * @throws JSONException If the value is an invalid number
     *                       or if the key is null.
     */
    public LOGJSONObject accumulate(
        String key,
        Object value
    ) {
        testValidity(value);
        Object object = this.opt(key);
        if (object == null) {
            this.put(key, value instanceof JSONArray
                ? new JSONArray().put(value)
                : value);
        } else if (object instanceof JSONArray) {
            ((JSONArray) object).put(value);
        } else {
            this.put(key, new JSONArray().put(object).put(value));
        }
        return this;
    }

    /**
     * Append values to the array under a key. If the key does not exist in the
     * JSONObject, then the key is put in the JSONObject with its value being a
     * JSONArray containing the value parameter. If the key was already
     * associated with a JSONArray, then the value parameter is appended to it.
     *
     * @param key   A key string.
     * @param value An object to be accumulated under the key.
     * @return this.
     * @throws JSONException If the key is null or if the current value
     *                       associated with the key is not a JSONArray.
     */
    public LOGJSONObject append(String key, Object value) {
        testValidity(value);
        Object object = this.opt(key);
        if (object == null) {
            this.put(key, new JSONArray().put(value));
        } else if (object instanceof JSONArray) {
            this.put(key, ((JSONArray) object).put(value));
        } else {
            throw new JSONException(JSON_OBJECT_CONST + key
                + "] is not a JSONArray.");
        }
        return this;
    }

    /**
     * Produce a string from a double. The string "null" will be returned if
     * the number is not finite.
     *
     * @param dub A double.
     * @return A String.
     */
    public static String doubleToString(double dub) {
        if (Double.isInfinite(dub) || Double.isNaN(dub)) {
            return "null";
        }

        // Shave off trailing zeros and decimal point, if possible.

        String string = Double.toString(dub);
        if (string.indexOf('.') > 0 && string.indexOf('e') < 0
            && string.indexOf('E') < 0) {
            while (string.endsWith("0")) {
                string = string.substring(0, string.length() - 1);
            }
            if (string.endsWith(".")) {
                string = string.substring(0, string.length() - 1);
            }
        }
        return string;
    }

    /**
     * Get the value object associated with a key.
     *
     * @param key A key string.
     * @return The object associated with the key.
     * @throws JSONException if the key is not found.
     */
    public Object get(String key) {
        if (key == null) {
            throw new JSONException("Null key.");
        }
        Object object = this.opt(key);
        if (object == null) {
            throw new JSONException(JSON_OBJECT_CONST + quote(key)
                + "] not found.");
        }
        return object;
    }

    /**
     * Get the boolean value associated with a key.
     *
     * @param key A key string.
     * @return The truth.
     * @throws JSONException if the value is not a Boolean or the String "true" or "false".
     */
    public boolean getBoolean(String key) {
        Object object = this.get(key);
        if (object.equals(Boolean.FALSE)
            || (object instanceof String
            && "false".equalsIgnoreCase((String) object))) {
            return false;
        } else if (object.equals(Boolean.TRUE)
            || (object instanceof String
                && "true".equalsIgnoreCase((String) object))) {
            return true;
        }
        throw new JSONException(JSON_OBJECT_CONST + quote(key)
            + "] is not a Boolean.");
    }

    /**
     * Get the double value associated with a key.
     *
     * @param key A key string.
     * @return The numeric value.
     * @throws JSONException if the key is not found or
     *                       if the value is not a Number object and cannot be converted to a number.
     */
    public double getDouble(String key) {
        Object object = this.get(key);
        try {
            return object instanceof Number
                ? ((Number) object).doubleValue()
                : Double.parseDouble((String) object);
        } catch (Exception e) {
            intlogger.error(JSON_OBJECT_CONST + quote(key) + "] is not a number.", e);
            throw new JSONException(JSON_OBJECT_CONST + quote(key) + "] is not a number.");
        }
    }

    /**
     * Get the int value associated with a key.
     *
     * @param key A key string.
     * @return The integer value.
     * @throws JSONException if the key is not found or if the value cannot
     *                       be converted to an integer.
     */
    public int getInt(String key) {
        Object object = this.get(key);
        try {
            return object instanceof Number
                ? ((Number) object).intValue()
                : Integer.parseInt((String) object);
        } catch (Exception e) {
            intlogger.error(JSON_OBJECT_CONST + quote(key) + "] is not an int.", e);
            throw new JSONException(JSON_OBJECT_CONST + quote(key) + "] is not an int.");
        }
    }

    /**
     * Get the JSONArray value associated with a key.
     *
     * @param key A key string.
     * @return A JSONArray which is the value.
     * @throws JSONException if the key is not found or
     *                       if the value is not a JSONArray.
     */
    public JSONArray getJSONArray(String key) {
        Object object = this.get(key);
        if (object instanceof JSONArray) {
            return (JSONArray) object;
        }
        throw new JSONException(JSON_OBJECT_CONST + quote(key)
            + "] is not a JSONArray.");
    }

    /**
     * Get the JSONObject value associated with a key.
     *
     * @param key A key string.
     * @return A JSONObject which is the value.
     * @throws JSONException if the key is not found or
     *                       if the value is not a JSONObject.
     */
    public LOGJSONObject getJSONObject(String key) {
        Object object = this.get(key);
        if (object instanceof LOGJSONObject) {
            return (LOGJSONObject) object;
        }
        throw new JSONException(JSON_OBJECT_CONST + quote(key)
            + "] is not a JSONObject.");
    }

    /**
     * Get the long value associated with a key.
     *
     * @param key A key string.
     * @return The long value.
     * @throws JSONException if the key is not found or if the value cannot
     *                       be converted to a long.
     */
    public long getLong(String key) {
        Object object = this.get(key);
        try {
            return object instanceof Number
                ? ((Number) object).longValue()
                : Long.parseLong((String) object);
        } catch (Exception e) {
            intlogger.error(JSON_OBJECT_CONST + quote(key) + "] is not a long.", e);
            throw new JSONException(JSON_OBJECT_CONST + quote(key) + "] is not a long.");
        }
    }

    /**
     * Get an array of field names from a JSONObject.
     *
     * @return An array of field names, or null if there are no names.
     */
    public static String[] getNames(LOGJSONObject jo) {
        int length = jo.length();
        if (length == 0) {
            return new String[]{};
        }
        Iterator<String> iterator = jo.keys();
        String[] names = new String[length];
        int iter = 0;
        while (iterator.hasNext()) {
            names[iter] = iterator.next();
            iter += 1;
        }
        return names;
    }

    /**
     * Get the string associated with a key.
     *
     * @param key A key string.
     * @return A string which is the value.
     * @throws JSONException if there is no string value for the key.
     */
    public String getString(String key) {
        Object object = this.get(key);
        if (object instanceof String) {
            return (String) object;
        }
        throw new JSONException(JSON_OBJECT_CONST + quote(key)
            + "] not a string.");
    }

    /**
     * Determine if the JSONObject contains a specific key.
     *
     * @param key A key string.
     * @return true if the key exists in the JSONObject.
     */
    public boolean has(String key) {
        return this.map.containsKey(key);
    }

    /**
     * Increment a property of a JSONObject. If there is no such property,
     * create one with a value of 1. If there is such a property, and if
     * it is an Integer, Long, Double, or Float, then add one to it.
     *
     * @param key A key string.
     * @return this.
     * @throws JSONException If there is already a property with this name
     *                       that is not an Integer, Long, Double, or Float.
     */
    public LOGJSONObject increment(String key) {
        Object value = this.opt(key);
        if (value == null) {
            this.put(key, 1);
        } else if (value instanceof Integer) {
            this.put(key, ((Integer) value).intValue() + 1);
        } else if (value instanceof Long) {
            this.put(key, ((Long) value).longValue() + 1);
        } else if (value instanceof Double) {
            this.put(key, ((Double) value).doubleValue() + 1);
        } else if (value instanceof Float) {
            this.put(key, ((Float) value).floatValue() + 1);
        } else {
            throw new JSONException("Unable to increment [" + quote(key) + "].");
        }
        return this;
    }

    /**
     * Get an enumeration of the keys of the JSONObject.
     *
     * @return An iterator of the keys.
     */
    public Iterator<String> keys() {
        return this.keySet().iterator();
    }

    /**
     * Get a set of keys of the JSONObject.
     *
     * @return A keySet.
     */
    public Set<String> keySet() {
        return this.map.keySet();
    }

    /**
     * Get the number of keys stored in the JSONObject.
     *
     * @return The number of keys in the JSONObject.
     */
    public int length() {
        return this.map.size();
    }

    /**
     * Produce a JSONArray containing the names of the elements of this
     * JSONObject.
     *
     * @return A JSONArray containing the key strings, or null if the JSONObject
     * is empty.
     */
    public JSONArray names() {
        JSONArray ja = new JSONArray();
        Iterator<String> keys = this.keys();
        while (keys.hasNext()) {
            ja.put(keys.next());
        }
        return ja.length() == 0 ? null : ja;
    }

    /**
     * Produce a string from a Number.
     *
     * @param number A Number
     * @return A String.
     * @throws JSONException If n is a non-finite number.
     */
    public static String numberToString(Number number) {
        if (number == null) {
            throw new JSONException("Null pointer");
        }
        testValidity(number);

        // Shave off trailing zeros and decimal point, if possible.

        String string = number.toString();
        if (string.indexOf('.') > 0 && string.indexOf('e') < 0
            && string.indexOf('E') < 0) {
            while (string.endsWith("0")) {
                string = string.substring(0, string.length() - 1);
            }
            if (string.endsWith(".")) {
                string = string.substring(0, string.length() - 1);
            }
        }
        return string;
    }

    /**
     * Get an optional value associated with a key.
     *
     * @param key A key string.
     * @return An object which is the value, or null if there is no value.
     */
    public Object opt(String key) {
        return key == null ? null : this.map.get(key);
    }

    /**
     * Get an optional boolean associated with a key.
     * It returns the defaultValue if there is no such key, or if it is not
     * a Boolean or the String "true" or "false" (case insensitive).
     *
     * @param key          A key string.
     * @param defaultValue The default.
     * @return The truth.
     */
    public boolean optBoolean(String key, boolean defaultValue) {
        try {
            return this.getBoolean(key);
        } catch (Exception e) {
            intlogger.trace(USING_DEFAULT_VALUE + defaultValue, e);
            return defaultValue;
        }
    }

    /**
     * Get an optional double associated with a key, or the
     * defaultValue if there is no such key or if its value is not a number.
     * If the value is a string, an attempt will be made to evaluate it as
     * a number.
     *
     * @param key          A key string.
     * @param defaultValue The default.
     * @return An object which is the value.
     */
    public double optDouble(String key, double defaultValue) {
        try {
            return this.getDouble(key);
        } catch (Exception e) {
            intlogger.trace(USING_DEFAULT_VALUE + defaultValue, e);
            return defaultValue;
        }
    }

    /**
     * Get an optional int value associated with a key,
     * or the default if there is no such key or if the value is not a number.
     * If the value is a string, an attempt will be made to evaluate it as
     * a number.
     *
     * @param key          A key string.
     * @param defaultValue The default.
     * @return An object which is the value.
     */
    public int optInt(String key, int defaultValue) {
        try {
            return this.getInt(key);
        } catch (Exception e) {
            intlogger.trace(USING_DEFAULT_VALUE + defaultValue, e);
            return defaultValue;
        }
    }


    /**
     * Get an optional long value associated with a key,
     * or the default if there is no such key or if the value is not a number.
     * If the value is a string, an attempt will be made to evaluate it as
     * a number.
     *
     * @param key          A key string.
     * @param defaultValue The default.
     * @return An object which is the value.
     */
    public long optLong(String key, long defaultValue) {
        try {
            return this.getLong(key);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * Get an optional string associated with a key.
     * It returns the defaultValue if there is no such key.
     *
     * @param key          A key string.
     * @param defaultValue The default.
     * @return A string which is the value.
     */
    public String optString(String key, String defaultValue) {
        Object object = this.opt(key);
        return NULL.equals(object) ? defaultValue : object.toString();
    }

    private void populateMap(Object bean) {
        Class<?> klass = bean.getClass();

        // If klass is a System class then set includeSuperClass to false.

        boolean includeSuperClass = klass.getClassLoader() != null;

        Method[] methods = includeSuperClass
            ? klass.getMethods()
            : klass.getDeclaredMethods();
        for (int i = 0; i < methods.length; i += 1) {
            try {
                Method method = methods[i];
                if (Modifier.isPublic(method.getModifiers())) {
                    String name = method.getName();
                    String key = "";
                    if (name.startsWith("get")) {
                        if ("getClass".equals(name)
                            || "getDeclaringClass".equals(name)) {
                            key = "";
                        } else {
                            key = name.substring(3);
                        }
                    } else if (name.startsWith("is")) {
                        key = name.substring(2);
                    }
                    if (key.length() > 0
                        && Character.isUpperCase(key.charAt(0))
                        && method.getParameterTypes().length == 0) {
                        if (key.length() == 1) {
                            key = key.toLowerCase();
                        } else if (!Character.isUpperCase(key.charAt(1))) {
                            key = key.substring(0, 1).toLowerCase()
                                + key.substring(1);
                        }

                        Object result = method.invoke(bean, (Object[]) null);
                        if (result != null) {
                            this.map.put(key, wrap(result));
                        }
                    }
                }
            } catch (Exception ignore) {
                intlogger.trace("populateMap: " + ignore.getMessage(), ignore);
            }
        }
    }

    /**
     * Put a key/double pair in the JSONObject.
     *
     * @param key   A key string.
     * @param value A double which is the value.
     * @return this.
     * @throws JSONException If the key is null or if the number is invalid.
     */
    public LOGJSONObject put(String key, double value) {
        this.put(key, new Double(value));
        return this;
    }

    /**
     * Put a key/int pair in the JSONObject.
     *
     * @param key   A key string.
     * @param value An int which is the value.
     * @return this.
     * @throws JSONException If the key is null.
     */
    public LOGJSONObject put(String key, int value) {
        this.put(key, new Integer(value));
        return this;
    }

    /**
     * Put a key/long pair in the JSONObject.
     *
     * @param key   A key string.
     * @param value A long which is the value.
     * @return this.
     * @throws JSONException If the key is null.
     */
    public LOGJSONObject put(String key, long value) {
        this.put(key, new Long(value));
        return this;
    }

    /**
     * Put a key/value pair in the JSONObject. If the value is null,
     * then the key will be removed from the JSONObject if it is present.
     *
     * @param key   A key string.
     * @param value An object which is the value. It should be of one of these
     *              types: Boolean, Double, Integer, JSONArray, JSONObject, Long, String,
     *              or the JSONObject.NULL object.
     * @return this.
     * @throws JSONException If the value is non-finite number
     *                       or if the key is null.
     */
    public LOGJSONObject put(String key, Object value) {
        String pooled;
        if (key == null) {
            throw new JSONException("Null key.");
        }
        if (value != null) {
            testValidity(value);
            pooled = (String) keyPool.get(key);
            if (pooled == null) {
                if (keyPool.size() >= KEY_POOL_SIZE) {
                    keyPool = new LinkedHashMap<>(KEY_POOL_SIZE);
                }
                keyPool.put(key, key);
            } else {
                key = pooled;
            }
            this.map.put(key, value);
        } else {
            this.remove(key);
        }
        return this;
    }

    /**
     * Put a key/value pair in the JSONObject, but only if the key and the
     * value are both non-null, and only if there is not already a member
     * with that name.
     *
     * @param key string key
     * @param value obj value
     * @return this LOGJSONObject
     * @throws JSONException if the key is a duplicate
     */
    public LOGJSONObject putOnce(String key, Object value) {
        if (key != null && value != null) {
            if (this.opt(key) != null) {
                throw new JSONException("Duplicate key \"" + key + "\"");
            }
            this.put(key, value);
        }
        return this;
    }

    /**
     * Produce a string in double quotes with backslash sequences in all the
     * right places. In JSON text, a string
     * cannot contain a control character or an unescaped quote or backslash.
     *
     * @param string A String
     * @return A String correctly formatted for insertion in a JSON text.
     */
    public static String quote(String string) {
        StringWriter sw = new StringWriter();
        synchronized (sw.getBuffer()) {
            try {
                return quote(string, sw).toString();
            } catch (IOException e) {
                intlogger.trace("Ignore Exception message: ", e);
                return "";
            }
        }
    }

    /**
     * Writer method.
     * @param string string
     * @param writer writer
     * @return A writer
     * @throws IOException input/output exception
     */
    public static Writer quote(String string, Writer writer) throws IOException {
        if (string == null || string.length() == 0) {
            writer.write("\"\"");
            return writer;
        }

        char char1;
        char char2 = 0;
        String hhhh;
        int len = string.length();

        writer.write('"');
        for (int i = 0; i < len; i += 1) {
            char1 = char2;
            char2 = string.charAt(i);
            switch (char2) {
                case '\\':
                case '"':
                    writer.write('\\');
                    writer.write(char2);
                    break;
                case '/':
                    if (char1 == '<') {
                        writer.write('\\');
                    }
                    writer.write(char2);
                    break;
                case '\b':
                    writer.write("\\b");
                    break;
                case '\t':
                    writer.write("\\t");
                    break;
                case '\n':
                    writer.write("\\n");
                    break;
                case '\f':
                    writer.write("\\f");
                    break;
                case '\r':
                    writer.write("\\r");
                    break;
                default:
                    if (char2 < ' ' || (char2 >= '\u0080' && char2 < '\u00a0')
                        || (char2 >= '\u2000' && char2 < '\u2100')) {
                        writer.write("\\u");
                        hhhh = Integer.toHexString(char2);
                        writer.write("0000", 0, 4 - hhhh.length());
                        writer.write(hhhh);
                    } else {
                        writer.write(char2);
                    }
            }
        }
        writer.write('"');
        return writer;
    }

    /**
     * Remove a name and its value, if present.
     *
     * @param key The name to be removed.
     * @return The value that was associated with the name,
     * or null if there was no value.
     */
    public Object remove(String key) {
        return this.map.remove(key);
    }

    /**
     * Try to convert a string into a number, boolean, or null. If the string
     * can't be converted, return the string.
     *
     * @param string A String.
     * @return A simple JSON value.
     */
    public static Object stringToValue(String string) {
        Double dub;
        if ("".equals(string)) {
            return string;
        }
        if ("true".equalsIgnoreCase(string)) {
            return Boolean.TRUE;
        }
        if ("false".equalsIgnoreCase(string)) {
            return Boolean.FALSE;
        }
        if ("null".equalsIgnoreCase(string)) {
            return LOGJSONObject.NULL;
        }

        /*
         * If it might be a number, try converting it.
         * If a number cannot be produced, then the value will just
         * be a string. Note that the plus and implied string
         * conventions are non-standard. A JSON parser may accept
         * non-JSON forms as long as it accepts all correct JSON forms.
         */

        char chr = string.charAt(0);
        if ((chr >= '0' && chr <= '9') || chr == '.' || chr == '-' || chr == '+') {
            try {
                if (string.indexOf('.') > -1 || string.indexOf('e') > -1
                    || string.indexOf('E') > -1) {
                    dub = Double.valueOf(string);
                    if (!dub.isInfinite() && !dub.isNaN()) {
                        return dub;
                    }
                } else {
                    Long myLong = new Long(string);
                    if (myLong == myLong.intValue()) {
                        return myLong.intValue();
                    } else {
                        return myLong;
                    }
                }
            } catch (Exception e) {
                intlogger.trace("Ignore Exception message: ", e);
            }
        }
        return string;
    }

    /**
     * Throw an exception if the object is a NaN or infinite number.
     *
     * @param obj The object to test.
     * @throws JSONException If o is a non-finite number.
     */
    public static void testValidity(Object obj) {
        if (obj != null) {
            if (obj instanceof Double) {
                if (((Double) obj).isInfinite() || ((Double) obj).isNaN()) {
                    throw new JSONException(
                        "JSON does not allow non-finite numbers.");
                }
            } else if (obj instanceof Float && (((Float) obj).isInfinite() || ((Float) obj).isNaN())) {
                throw new JSONException(
                    "JSON does not allow non-finite numbers.");
            }
        }
    }

    /**
     * Produce a JSONArray containing the values of the members of this
     * JSONObject.
     *
     * @param names A JSONArray containing a list of key strings. This
     *              determines the sequence of the values in the result.
     * @return A JSONArray of values.
     * @throws JSONException If any of the values are non-finite numbers.
     */
    public JSONArray toJSONArray(JSONArray names) {
        if (names == null || names.length() == 0) {
            return null;
        }
        JSONArray ja = new JSONArray();
        for (int i = 0; i < names.length(); i += 1) {
            ja.put(this.opt(names.getString(i)));
        }
        return ja;
    }

    /**
     * Make a JSON text of this JSONObject. For compactness, no whitespace
     * is added. If this would not result in a syntactically correct JSON text,
     * then null will be returned instead.
     *
     * <p>* Warning: This method assumes that the data structure is acyclical.
     *
     * @return a printable, displayable, portable, transmittable
     * representation of the object, beginning
     * with <code>{</code>&nbsp;<small>(left brace)</small> and ending
     * with <code>}</code>&nbsp;<small>(right brace)</small>.
     */
    public String toString() {
        try {
            return this.toString(0);
        } catch (Exception e) {
            intlogger.trace("Exception: ", e);
            return "";
        }
    }

    /**
     * Make a prettyprinted JSON text of this JSONObject.
     *
     * <p>* Warning: This method assumes that the data structure is acyclical.
     *
     * @param indentFactor The number of spaces to add to each level of
     *                     indentation.
     * @return a printable, displayable, portable, transmittable
     * representation of the object, beginning
     * with <code>{</code>&nbsp;<small>(left brace)</small> and ending
     * with <code>}</code>&nbsp;<small>(right brace)</small>.
     * @throws JSONException If the object contains an invalid number.
     */
    public String toString(int indentFactor) {
        StringWriter writer = new StringWriter();
        synchronized (writer.getBuffer()) {
            return this.write(writer, indentFactor, 0).toString();
        }
    }

    /**
     * Make a JSON text of an Object value. If the object has an
     * value.toJSONString() method, then that method will be used to produce
     * the JSON text. The method is required to produce a strictly
     * conforming text. If the object does not contain a toJSONString
     * method (which is the most common case), then a text will be
     * produced by other means. If the value is an array or Collection,
     * then a JSONArray will be made from it and its toJSONString method
     * will be called. If the value is a MAP, then a JSONObject will be made
     * from it and its toJSONString method will be called. Otherwise, the
     * value's toString method will be called, and the result will be quoted.
     *
     * <p>* Warning: This method assumes that the data structure is acyclical.
     *
     * @param value The value to be serialized.
     * @return a printable, displayable, transmittable
     * representation of the object, beginning
     * with <code>{</code>&nbsp;<small>(left brace)</small> and ending
     * with <code>}</code>&nbsp;<small>(right brace)</small>.
     * @throws JSONException If the value is or contains an invalid number.
     */
    @SuppressWarnings("unchecked")
    public static String valueToString(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof JSONString) {
            String object;
            try {
                object = ((JSONString) value).toJSONString();
            } catch (Exception e) {
                throw new JSONException(e);
            }
            if (object != null) {
                return object;
            }
            throw new JSONException("Bad value from toJSONString: " + object);
        }
        if (value instanceof Number) {
            return numberToString((Number) value);
        }
        if (value instanceof Boolean || value instanceof LOGJSONObject
            || value instanceof JSONArray) {
            return value.toString();
        }
        if (value instanceof Map) {
            return new LOGJSONObject((Map<String, Object>) value).toString();
        }
        if (value instanceof Collection) {
            return new JSONArray((Collection<Object>) value).toString();
        }
        if (value.getClass().isArray()) {
            return new JSONArray(value).toString();
        }
        return quote(value.toString());
    }

    /**
     * Wrap an object, if necessary. If the object is null, return the NULL
     * object. If it is an array or collection, wrap it in a JSONArray. If
     * it is a map, wrap it in a JSONObject. If it is a standard property
     * (Double, String, et al) then it is already wrapped. Otherwise, if it
     * comes from one of the java packages, turn it into a string. And if
     * it doesn't, try to wrap it in a JSONObject. If the wrapping fails,
     * then null is returned.
     *
     * @param object The object to wrap
     * @return The wrapped value
     */
    @SuppressWarnings("unchecked")
    public static Object wrap(Object object) {
        try {
            if (object == null) {
                return NULL;
            }
            if (object instanceof LOGJSONObject || object instanceof JSONArray
                || NULL.equals(object) || object instanceof JSONString
                || object instanceof Byte || object instanceof Character
                || object instanceof Short || object instanceof Integer
                || object instanceof Long || object instanceof Boolean
                || object instanceof Float || object instanceof Double
                || object instanceof String) {
                return object;
            }

            if (object instanceof Collection) {
                return new JSONArray((Collection<Object>) object);
            }
            if (object.getClass().isArray()) {
                return new JSONArray(object);
            }
            if (object instanceof Map) {
                return new LOGJSONObject((Map<String, Object>) object);
            }
            Package objectPackage = object.getClass().getPackage();
            String objectPackageName = objectPackage != null
                ? objectPackage.getName()
                : "";
            if (
                objectPackageName.startsWith("java.")
                    || objectPackageName.startsWith("javax.")
                    || object.getClass().getClassLoader() == null
            ) {
                return object.toString();
            }
            return new LOGJSONObject(object);
        } catch (Exception exception) {
            intlogger.trace("Exception: ", exception);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    static Writer writeValue(Writer writer, Object value, int indentFactor, int indent) throws IOException {
        if (value == null) {
            writer.write("null");
        } else if (value instanceof LOGJSONObject) {
            ((LOGJSONObject) value).write(writer, indentFactor, indent);
        } else if (value instanceof JSONArray) {
            ((JSONArray) value).write(writer, indentFactor, indent);
        } else if (value instanceof Map) {
            new LOGJSONObject((Map<String, Object>) value).write(writer, indentFactor, indent);
        } else if (value instanceof Collection) {
            new JSONArray((Collection<Object>) value).write(writer, indentFactor, indent);
        } else if (value.getClass().isArray()) {
            new JSONArray(value).write(writer, indentFactor, indent);
        } else if (value instanceof Number) {
            writer.write(numberToString((Number) value));
        } else if (value instanceof Boolean) {
            writer.write(value.toString());
        } else if (value instanceof JSONString) {
            Object obj;
            try {
                obj = ((JSONString) value).toJSONString();
            } catch (Exception e) {
                throw new JSONException(e);
            }
            writer.write(obj != null ? obj.toString() : quote(value.toString()));
        } else {
            quote(value.toString(), writer);
        }
        return writer;
    }

    private static void indent(Writer writer, int indent) throws IOException {
        for (int i = 0; i < indent; i += 1) {
            writer.write(' ');
        }
    }

    /**
     * Write the contents of the JSONObject as JSON text to a writer. For
     * compactness, no whitespace is added.
     *
     * <p>* Warning: This method assumes that the data structure is acyclical.
     *
     * @return The writer.
     * @throws JSONException JSON exception
     */
    Writer write(Writer writer, int indentFactor, int indent) {
        try {
            boolean commanate = false;
            final int length = this.length();
            Iterator<String> keys = this.keys();
            writer.write('{');

            if (length == 1) {
                Object key = keys.next();
                writer.write(quote(key.toString()));
                writer.write(':');
                if (indentFactor > 0) {
                    writer.write(' ');
                }
                writeValue(writer, this.map.get(key), indentFactor, indent);
            } else if (length != 0) {
                final int newindent = indent + indentFactor;
                while (keys.hasNext()) {
                    if (commanate) {
                        writer.write(',');
                    }
                    if (indentFactor > 0) {
                        writer.write('\n');
                    }
                    indent(writer, newindent);
                    Object key = keys.next();
                    writer.write(quote(key.toString()));
                    writer.write(':');
                    if (indentFactor > 0) {
                        writer.write(' ');
                    }
                    writeValue(writer, this.map.get(key), indentFactor,
                        newindent);
                    commanate = true;
                }
                if (indentFactor > 0) {
                    writer.write('\n');
                }
                indent(writer, indent);
            }
            writer.write('}');
            return writer;
        } catch (IOException exception) {
            throw new JSONException(exception);
        }
    }
}
