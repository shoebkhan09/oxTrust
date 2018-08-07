/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */
package org.gluu.oxtrust.model.registration;

import org.apache.commons.lang.StringUtils;

/**
 * List of parameters for registration management requests
 *
 * @author Shoeb Khan
 * @version 07/08/2018
 */


public enum ManagementRegReqParam {

    /**
     * Whether captcha is disabled
     */
    CAPTCHA_DISABLED("captchaDisabled"),

    /**
     * Selected Attributes
     */
    SELECTED_ATTRIBUTES("selectedAttributes");

    /**
     * Parameter name
     */
    private final String name;

    /**
     *
     * @param name parameter name
     */
    ManagementRegReqParam(String name) {
        this.name = name;
    }

    /**
     * Gets parameter name.
     *
     * @return parameter name
     */
    public String getName() {
        return name;
    }

    public static boolean isStandard(String parameterName) {
        if (StringUtils.isNotBlank(parameterName)) {
            for (ManagementRegReqParam t : values()) {
                if (t.getName().equalsIgnoreCase(parameterName)) {
                    return true;
                }
            }
        }
        return false;
    }


    public static boolean isCustomParameterValid(String parameterName) {
        return !isStandard(parameterName);
    }


    @Override
    public String toString() {
        return name;
    }
}