package me.crespel.karaplan.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

import me.crespel.karaplan.security.OidcUserServiceWrapper;

@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter {

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http
			.authorizeRequests()
				.antMatchers("/", "/home").permitAll()
				.antMatchers("/**/*.css", "/**/*.js", "/**/*.js.map", "/webjars/**").permitAll()
				.antMatchers("/api/", "/swagger-ui.html", "/v*/api-docs/**", "/swagger-resources/**", "/csrf").permitAll()
				.antMatchers("/api/v1/account/**").permitAll()
				.antMatchers("/actuator/health", "/actuator/info").permitAll()
				.antMatchers("/actuator/**").hasRole("ADMIN")
				.anyRequest().authenticated()
				.and()
			.csrf()
				.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
				.and()
			.oauth2Login()
				.userInfoEndpoint().oidcUserService(oidcUserService());
	}

	@Bean
	public OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService() {
		return new OidcUserServiceWrapper(new OidcUserService());
	}

}
