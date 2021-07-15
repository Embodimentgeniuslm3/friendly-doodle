package org.apereo.cas.webauthn.web.flow;

import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.util.CollectionUtils;
import org.apereo.cas.web.support.WebUtils;

import com.yubico.core.RegistrationStorage;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.webflow.action.AbstractAction;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

import javax.servlet.http.HttpServletResponse;

/**
 * This is {@link WebAuthnStartRegistrationAction}.
 *
 * @author Misagh Moayyed
 * @since 6.1.0
 */
@RequiredArgsConstructor
@Getter
public class WebAuthnStartRegistrationAction extends AbstractAction {

    /**
     * Attribute name that points to the web application id put into the webflow.
     */
    public static final String FLOW_SCOPE_WEB_AUTHN_APPLICATION_ID = "webauthnApplicationId";

    private final RegistrationStorage webAuthnCredentialRepository;

    private final CasConfigurationProperties casProperties;

    private final CsrfTokenRepository csrfTokenRepository;

    @Override
    protected Event doExecute(final RequestContext requestContext) {
        val webAuthn = casProperties.getAuthn().getMfa().getWebAuthn();
        val authn = WebUtils.getAuthentication(requestContext);
        val principal = authn.getPrincipal();
        val attributes = principal.getAttributes();

        val flowScope = requestContext.getFlowScope();
        if (attributes.containsKey(webAuthn.getDisplayNameAttribute())) {
            flowScope.put("displayName",
                CollectionUtils.firstElement(attributes.get(webAuthn.getDisplayNameAttribute())));
        } else {
            flowScope.put("displayName", principal.getId());
        }
        flowScope.put(FLOW_SCOPE_WEB_AUTHN_APPLICATION_ID, webAuthn.getApplicationId());

        val request = WebUtils.getHttpServletRequestFromExternalWebflowContext(requestContext);
        val response = WebUtils.getHttpServletResponseFromExternalWebflowContext(requestContext);
        request.setAttribute(HttpServletResponse.class.getName(), response);
        
        var csrfToken = csrfTokenRepository.loadToken(request);
        if (csrfToken == null) {
            csrfToken = csrfTokenRepository.generateToken(request);
            csrfTokenRepository.saveToken(csrfToken, request, response);
        }
        flowScope.put(csrfToken.getParameterName(), csrfToken);
        return null;
    }
}
