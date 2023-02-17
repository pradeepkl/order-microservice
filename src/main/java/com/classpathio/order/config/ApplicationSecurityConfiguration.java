package com.classpathio.order.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

@Configuration
public class ApplicationSecurityConfiguration extends WebSecurityConfigurerAdapter{
	/**
	 * Steps to perform the authorization
	 * 1. Validate the JWT token
	 *    - using the keys
	 * 2. From the JWT token extract the claims
	 *    - groups
	 * 3. Convert the groups to Spring security roles
	 * 4. Provides access/deny based on the roles
	 *       
	 */
	@Override
	protected void configure(HttpSecurity http) throws Exception {
		//authorization rules to allow/deny
		http.cors().disable();
		http.csrf().disable();
		http.headers().frameOptions().disable();

		http.authorizeRequests().antMatchers("/login**", "/api/state/**", "/logout**", "/h2-console/**").permitAll()
				.antMatchers(HttpMethod.GET, "/api/v1/orders**", "/api/v1/orders/**")
					.hasAnyRole("Everyone", "super_admins", "admins")
				.antMatchers(HttpMethod.POST, "/api/v1/orders**")
					.hasAnyRole("super_admins", "admins")
				.antMatchers(HttpMethod.DELETE, "/api/v1/orders/**")
					.hasRole("super_admins")
				.and()
				.oauth2ResourceServer()
				.jwt();

	}
	
	@Bean
	public JwtAuthenticationConverter jwtAuthenticationConverter() {
		JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
		JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
		jwtGrantedAuthoritiesConverter.setAuthoritiesClaimName("groups");
		jwtGrantedAuthoritiesConverter.setAuthorityPrefix("ROLE_");
		jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter);
		return jwtAuthenticationConverter;
	}

}
