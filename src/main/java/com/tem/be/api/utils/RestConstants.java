package com.tem.be.api.utils;

/**
 * Contains constant values used for standard REST API responses such as status codes and messages.
 */
public class RestConstants {

    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private RestConstants() {
        // Private constructor to hide the implicit public one
    }

    public static final int SUCCESS_CODE = 200;

    public static final int NO_DATA_CODE = 404;

    public static final int ERROR_CODE = 500;

    public static final int UNAUTHORIZED_CODE = 401;


    public static final String SUCCESS_STRING = "Success";

    public static final String FAIL_STRING = "Failed";

    public static final String ERROR_STRING = "Error";

    public static final String UNAUTHORIZED_STRING = "Unauthorized";


}
