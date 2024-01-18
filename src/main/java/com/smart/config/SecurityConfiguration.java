package com.smart.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

//	@Autowired
//	HttpSecurity httpSecurity;

	@Bean
	public UserDetailsService getUserDetailsService() {

		return new UserDetailServiceImple();
	}

	@Bean
	public BCryptPasswordEncoder passwordEncoder() {

		return new BCryptPasswordEncoder();
	}

	@Bean
	public DaoAuthenticationProvider authenticationProvider() {

		DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider();
		daoAuthenticationProvider.setUserDetailsService(getUserDetailsService());
		daoAuthenticationProvider.setPasswordEncoder(passwordEncoder());

		return daoAuthenticationProvider;
	}

	@Bean
	public SecurityFilterChain settingUpHttpSecurity(HttpSecurity httpSecurity) throws Exception {

		httpSecurity.authorizeHttpRequests(customizer -> {
			customizer.requestMatchers("/user/**").authenticated();
			customizer.requestMatchers("/**").permitAll();

		});

//		httpSecurity.formLogin(Customizer.withDefaults());
		httpSecurity.formLogin(customizer -> {
			customizer.loginPage("/signin").defaultSuccessUrl("/user/dashboard");
			

		});

//		httpSecurity.logout(customizer ->
//
//		{
//			customizer.logoutUrl("/logout").logoutSuccessUrl("/signin?logout");
//		}
//
//		);

//		httpSecurity.logout((logout) -> logout.logoutUrl("/signin"));

		httpSecurity.httpBasic(Customizer.withDefaults());

		return httpSecurity.build();
	}

}
