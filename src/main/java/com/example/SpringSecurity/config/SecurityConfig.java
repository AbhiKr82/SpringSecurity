package com.example.SpringSecurity.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

        @Autowired
        private UserDetailsService userDetailsService;

        @Bean
        public AuthenticationProvider authenticationProvider() {
                DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
                provider.setPasswordEncoder(new BCryptPasswordEncoder(10));

                return provider;

        }

        @Bean
        SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {

                http.authorizeHttpRequests((requests) -> requests
                                .requestMatchers("/api/addUser").permitAll()
                                .requestMatchers("/api/hello").permitAll()
                                .anyRequest().authenticated()

                );

                http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
                http.csrf(csrf -> csrf.disable());
                // http.formLogin(Customizer.withDefaults());
                http.httpBasic(Customizer.withDefaults());
                return http.build();

        }

        /*
         * @Bean
         * public UserDetailsService userDetailsService(){
         * 
         * UserDetails user= User.withUsername("user")
         * .password("{noop}user")
         * // if you are not providing password encoder
         * .roles("USER").build();
         * 
         * UserDetails admin= User.withUsername("admin")
         * .password("{noop}admin")
         * .roles("ADMIN")
         * .build();
         * 
         * UserDetails normal= User.withUsername("normal")
         * .password("{noop}normal")
         * .build();
         * 
         * 
         * return new InMemoryUserDetailsManager(user,admin,normal);
         * }
         * 
         */

}
