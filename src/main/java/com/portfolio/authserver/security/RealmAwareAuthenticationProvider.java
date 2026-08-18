package com.portfolio.authserver.security;

import com.portfolio.authserver.service.RealmAwareUserLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.AbstractUserDetailsAuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

@RequiredArgsConstructor
public class RealmAwareAuthenticationProvider extends AbstractUserDetailsAuthenticationProvider {

    private final RealmAwareUserLookupService userLookupService;
    private final PasswordEncoder passwordEncoder;

    @Override
    protected void additionalAuthenticationChecks(UserDetails userDetails,
                                                  UsernamePasswordAuthenticationToken authentication) {
        String presentedPassword = (String) authentication.getCredentials();
        if (presentedPassword == null || !passwordEncoder.matches(presentedPassword, userDetails.getPassword())) {
            throw new BadCredentialsException("Credenziali non valide");
        }
    }

    @Override
    protected UserDetails retrieveUser(String username, UsernamePasswordAuthenticationToken authentication) {
        String realm = (authentication.getDetails() instanceof RealmAuthenticationDetails details)
                ? details.getRealm() : null;

        if (realm == null || realm.isBlank()) {
            throw new InternalAuthenticationServiceException("Parametro realm mancante nella request di login");
        }
        return userLookupService.loadUserByRealmAndUsername(realm, username);
    }
}