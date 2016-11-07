/**
 * Copyright 2016 SAP SE
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.sap.cloud.sfsf.timeoff;

import java.util.Collections;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsByNameServiceWrapper;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.security.web.authentication.preauth.j2ee.J2eePreAuthenticatedProcessingFilter;

@Configuration
@EnableWebSecurity
@Order(10)
public class TimeOffSecurityConfig extends WebSecurityConfigurerAdapter {

  @Override
  public void configure(final WebSecurity web) throws Exception {
    web.ignoring().antMatchers("/services/**", "/ide/**");
  }

  @Override
  protected void configure(final HttpSecurity http) throws Exception {
    //@formatter:off
    http
      .addFilterAfter(j2eePreAuthenticatedProcessingFilter(), J2eePreAuthenticatedProcessingFilter.class)
      .authenticationProvider(preauthAuthProvider())
      .antMatcher("/**").authorizeRequests().anyRequest().authenticated();
    //@formatter:on
  }


  @Override
  protected void configure(final AuthenticationManagerBuilder auth) throws Exception {
    auth.authenticationProvider(preauthAuthProvider());
  }

  @Bean
  J2eePreAuthenticatedProcessingFilter j2eePreAuthenticatedProcessingFilter() throws Exception {
    final J2eePreAuthenticatedProcessingFilter filter = new J2eePreAuthenticatedProcessingFilter();
    filter.setAuthenticationManager(authenticationManager());
    return filter;
  }

  @Bean
  UserDetailsByNameServiceWrapper<PreAuthenticatedAuthenticationToken> userDetailsServiceWrapper() throws Exception {
    final UserDetailsByNameServiceWrapper<PreAuthenticatedAuthenticationToken> wrapper =
        new UserDetailsByNameServiceWrapper<>();
    wrapper.setUserDetailsService(SAPUserDetailsService());
    return wrapper;
  }

  @Bean
  PreAuthenticatedAuthenticationProvider preauthAuthProvider() throws Exception {
    final PreAuthenticatedAuthenticationProvider provider = new PreAuthenticatedAuthenticationProvider();
    provider.setPreAuthenticatedUserDetailsService(userDetailsServiceWrapper());
    return provider;
  }

  @Bean
  UserDetailsService SAPUserDetailsService() {
    return username -> {
      return new User(username, "nope", Collections.emptyList());
    };
  }

}
