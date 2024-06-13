package com.hkshenoy.jaltantraloopsb.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

@Configuration
@EnableWebSecurity
public class SpringSecurity {

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired UserRepository userRepository;

    @Bean
    public static PasswordEncoder passwordEncoder(){
        return
                new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf().disable()
                .authorizeHttpRequests((authorize) ->
                        authorize.requestMatchers("/","/images/**","/css/**","/register/**","/verify/**").permitAll()
                        //authorize.requestMatchers("/**").permitAll()
                                .anyRequest().authenticated()

                ).formLogin(
                        form -> form
                                .loginPage("/login")
                                .loginProcessingUrl("/login")
                                .defaultSuccessUrl("/")
                                .permitAll()
                                .successHandler(new AuthenticationSuccessHandler() {

                            @Override
                            public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                                                Authentication authentication) throws IOException, ServletException {
                                // run custom logics upon successful login

                                //get user details
                                UserDetails userDetails = (UserDetails) authentication.getPrincipal();
                                String email = userDetails.getUsername();

                                //get user objects
                                User user = userRepository.findByEmail(email);

                                UserSessionDetail userSessionDetail=new UserSessionDetail();
                                userSessionDetail.setSessionId(request.getSession().getId());
                                userSessionDetail.setClientIpAddress(request.getRemoteAddr());

                                String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
                                userSessionDetail.setLoginDatetime(timeStamp);


                                if(user.getSessionDetail()==null){
                                    user.setSessionDetail(new ArrayList<>());
                                }

                                System.out.println(userSessionDetail.toString());
                                user.getSessionDetail().add(userSessionDetail);

                                userRepository.save(user);

                                response.sendRedirect(request.getContextPath());




                            }
                        })
                )
                .logout(
                        logout -> logout
                                .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                                .logoutSuccessUrl("/")
                                .permitAll()

                );
        return http.build();
    }




    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth
                .userDetailsService(userDetailsService)
                .passwordEncoder(passwordEncoder());
    }
}