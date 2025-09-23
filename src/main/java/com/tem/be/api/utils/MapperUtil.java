package com.tem.be.api.utils;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Utility class for JSON deserialization using Jackson's ObjectMapper.
 * Provides a method to convert a JSON string into a Java object of a specified type.
 */
public class MapperUtil {

    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private MapperUtil() {
        // Private constructor to hide the implicit public one
    }

    static ObjectMapper mapper = new ObjectMapper();

    /**
     * Converts a JSON string into an object of the specified class type.
     *
     * @param clazz The class type to convert the JSON into.
     * @param value The JSON string to be deserialized.
     * @param <T>   The type of the resulting object.
     * @return The deserialized object, or null if deserialization fails.
     */
    public static <T> T readAsObject(Class<T> clazz, String value) {
        try {
            return mapper.readValue(value, clazz);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
