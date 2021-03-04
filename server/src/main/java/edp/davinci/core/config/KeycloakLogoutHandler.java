package edp.davinci.core.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Herry Hong
 * @description Propagates logouts to Keycloak.
 * @date 2021/3/1
 */
@Slf4j
@Component
public class KeycloakLogoutHandler implements org.springframework.security.web.authentication.logout.LogoutHandler {

    @Autowired
    private RestTemplate restTemplate;

    @Override
    public void logout(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Authentication authentication) {
        log.debug("==>logout LogoutHandler");
        try {
            if (null != authentication && authentication instanceof OAuth2AuthenticationToken) {
                propagateLogoutToKeycloak((OidcUser) authentication.getPrincipal());
            }
            httpServletRequest.logout();
            httpServletResponse.sendRedirect("/");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void propagateLogoutToKeycloak(OidcUser user) {

        String endSessionEndpoint = user.getIssuer() + "/protocol/openid-connect/logout";

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(endSessionEndpoint)
                .queryParam("id_token_hint", user.getIdToken().getTokenValue());

        ResponseEntity<String> logoutResponse = restTemplate.getForEntity(builder.toUriString(), String.class);
        if (logoutResponse.getStatusCode().is2xxSuccessful()) {
            log.info("Successfulley logged out");
        } else {
            log.info("Could not propagate logout");
        }
    }
}
