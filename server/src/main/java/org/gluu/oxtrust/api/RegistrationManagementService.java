/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxtrust.api;

import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

import org.gluu.oxtrust.api.client.RegistrationManagementRequest;
import org.gluu.oxtrust.api.client.RegistrationManagementResponse;
import org.gluu.oxtrust.ldap.service.AttributeService;
import org.gluu.oxtrust.ldap.service.JsonConfigurationService;
import org.gluu.oxtrust.ldap.service.OrganizationService;
import org.gluu.oxtrust.model.GluuOrganization;
import org.gluu.oxtrust.model.RegistrationConfiguration;
import org.gluu.oxtrust.service.filter.ProtectedApi;
import org.gluu.oxtrust.util.OxTrustApiConstants;
import org.gluu.oxtrust.util.OxTrustConstants;
import org.slf4j.Logger;
import org.xdi.config.oxtrust.AppConfiguration;
import org.xdi.model.GluuAttribute;
import org.xdi.util.StringHelper;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation for register REST web services.
 *
 * @author Shoeb Khan
 * @version July 06, 2018
 */
@Path(OxTrustApiConstants.BASE_API_URL + "/configurations/registration")
@ProtectedApi
public class RegistrationManagementService {

    public static final String SERVER_DENIED_THE_REQUEST = "Server denied the request.";
    public static final String MALFORMED_REQUEST = "The request is malformed.";

    @Inject
    private Logger log;

    @Inject
    private OrganizationService organizationService;

    @Inject
    private AttributeService attributeService;

    @Inject
    private JsonConfigurationService jsonConfigurationService;


    /**
     * This operation retrieves the configuration info for the specified findAttributesForPattern criteria,
     * or all the configurations if no findAttributesForPattern criteria specified.
     * <p>
     *
     * @param searchPattern   Search pattern.
     * @return response
     */
    @GET
    @Produces({MediaType.APPLICATION_JSON})
    @ApiOperation(
            value = "Reads registration configuration info.",
            notes = "Reads registration configuration info."
    )
    @ApiResponses(value = {
            @ApiResponse(code = 403, message = "access_denied\n" +
                    SERVER_DENIED_THE_REQUEST),
            @ApiResponse(code = 200, response = RegistrationManagementResponse.class, message = "Success")
    })
    public Response getConfiguration(@QueryParam(value = "searchPattern") String searchPattern) {
        log.debug("Attempting to read configurations: searchPattern = {}", searchPattern);
        Response.ResponseBuilder builder = search(searchPattern);
        Response response = builder.build();
        return response;
    }

    /**
     * Saves the registration configuration data
     *
     * @return response
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Saves configuration.",
            notes = "Saves configuration.",
            response = Response.class
    )
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "invalid_request\n" +
                    MALFORMED_REQUEST),
            @ApiResponse(code = 403, message = "access_denied\n" +
                    SERVER_DENIED_THE_REQUEST)

    })
    public Response saveConfiguration(@Valid RegistrationManagementRequest request) {
        try {

            final Boolean captchaDisabled = request.getCaptchaDisabled();
            return save(captchaDisabled, request.getSelectedAttributes());

        } catch (Exception otherEx) {
            log.error(otherEx.getMessage(), otherEx);
            return Response.serverError().build();
        }
    }

    private Response save(boolean captchaDisabled, List<GluuAttribute> selectedAttributes) {
        Response.ResponseBuilder builder = Response.ok();
        /*
        Unlike action class, the value of configureRegistrationForm would be true all the time for REST API.
         */
        boolean configureRegistrationForm = true;
        AppConfiguration appConfiguration = jsonConfigurationService.getOxTrustappConfiguration();
        save(organizationService, captchaDisabled, configureRegistrationForm, jsonConfigurationService, appConfiguration, selectedAttributes);
        return builder.build();
    }


    private Response.ResponseBuilder search(String searchPattern) {
        final AppConfiguration oxTrustAppConfiguration = getOxTrustAppConfiguration();
        final List<GluuAttribute> attributes = new ArrayList<GluuAttribute>();
        final List<GluuAttribute> selectedAttributes = new ArrayList<GluuAttribute>();
        final GluuOrganization org = organizationService.getOrganization();
        final RegistrationConfiguration config = org.getOxRegistrationConfiguration();
        final RegistrationManagementResponse regResponse = new RegistrationManagementResponse();
        if (config != null) {
            regResponse.setCaptchaDisabled(config.isCaptchaDisabled());
            List<String> attributeList = config.getAdditionalAttributes();
            if (attributeList != null && !attributeList.isEmpty()) {
                for (String attributeInum : attributeList) {
                    GluuAttribute attribute = attributeService.getAttributeByInum(attributeInum);
                    selectedAttributes.add(attribute);
                    attributes.add(attribute);
                }
            }
        }
        regResponse.setSelectedAttributes(selectedAttributes);
        if (oxTrustAppConfiguration != null) {
            regResponse.setCssLocation(oxTrustAppConfiguration.getCssLocation());
            regResponse.setGetRecaptchaSecretKey(oxTrustAppConfiguration.getRecaptchaSecretKey());
            regResponse.setJsLocation(oxTrustAppConfiguration.getJsLocation());
            regResponse.setGetRecaptchaSiteKey(oxTrustAppConfiguration.getRecaptchaSiteKey());
        }
        try {
            regResponse.setAttributes(findAttributesForPattern(selectedAttributes, searchPattern, attributeService));
        } catch (Exception ex) {
            log.error("Failed to find attributes", ex);
            return Response.serverError();
        }
        return Response.ok(regResponse);
    }

    private AppConfiguration getOxTrustAppConfiguration() {
        return jsonConfigurationService.getOxTrustappConfiguration();
    }

    /*
		Finds attribute list for specified search pattern.
		 */
    public static List<GluuAttribute> findAttributesForPattern(final List<GluuAttribute> selectedAttributes, final String searchPattern, AttributeService attributeService) throws Exception {
        final List<GluuAttribute> attributes;
        if (StringHelper.isEmpty(searchPattern)) {
            attributes = attributeService.getAllAttributes();
        } else {
            attributes = attributeService.searchAttributes(searchPattern, OxTrustConstants.searchPersonsSizeLimit);
        }
        for (GluuAttribute selectedAttribute : selectedAttributes) {
            if (!attributes.contains(selectedAttribute)) {
                attributes.add(selectedAttribute);
            }
        }
        return attributes;
    }

    public static void save(OrganizationService organizationService, boolean captchaDisabled, boolean configureRegistrationForm, JsonConfigurationService jsonConfigurationService, AppConfiguration oxTrustappConfiguration, List<GluuAttribute> selectedAttributes) {
        GluuOrganization org = organizationService.getOrganization();
        RegistrationConfiguration config = org.getOxRegistrationConfiguration();
        if (config == null) {
            config = new RegistrationConfiguration();
        }

        config.setCaptchaDisabled(captchaDisabled);

        List<String> attributeList = new ArrayList<String>();
        if (configureRegistrationForm) {
            for (GluuAttribute attribute : selectedAttributes) {
                attributeList.add(attribute.getInum());
            }
        }
        config.setAdditionalAttributes(attributeList);
        org.setOxRegistrationConfiguration(config);
        organizationService.updateOrganization(org);

        jsonConfigurationService.saveOxTrustappConfiguration(oxTrustappConfiguration);



    }


}