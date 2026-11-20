package com.dijo.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import static com.vaadin.flow.spring.security.VaadinSecurityConfigurer.vaadin;

@Configuration
// Enables @RolesAllowed on Spring services, e.g. admin-only operations
@EnableMethodSecurity(jsr250Enabled = true)
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, AuthenticationManager authenticationManager)
            throws Exception {
        // Use exactly our portal-aware manager (no fallback to a parent manager
        // that could authenticate users on the wrong portal).
        http.authenticationManager(authenticationManager);

        // Both login pages post to /login; the "portal" parameter on the form
        // action records which page was used. Configured before Vaadin's
        // defaults, which keep an existing failure handler. The login page is
        // set here too so Spring's generated default login page stays off.
        http.formLogin(form -> form
                .loginPage("/" + ClientLoginView.ROUTE)
                .authenticationDetailsSource(PortalAuthenticationDetails::new)
                .failureHandler(new PortalAuthenticationFailureHandler()));

        // Every request goes through Spring Security's filter chain, so the
        // session is validated on each request (UC-001 BR-02, UC-002 BR-04).
        // Flow views are protected via @RolesAllowed / @AnonymousAllowed.
        // Unauthenticated users are sent to the client login page; after login,
        // HomeView forwards each role to its own dashboard.
        http.with(vaadin(), vaadin -> vaadin
                .loginView(ClientLoginView.class)
                .defaultSuccessUrl("/", true)
                .logoutSuccessHandler(new RoleBasedLogoutSuccessHandler()));
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        // BCrypt by default (UC-001 BR-03)
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    AuthenticationManager authenticationManager(UserDetailsService userDetailsService,
                                                PasswordEncoder passwordEncoder) {
        ProviderManager manager = new ProviderManager(
                new PortalAuthenticationProvider(userDetailsService, passwordEncoder));
        manager.setEraseCredentialsAfterAuthentication(true);
        return manager;
    }
}
