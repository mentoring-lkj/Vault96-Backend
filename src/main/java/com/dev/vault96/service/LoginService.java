    package com.dev.vault96.service;

    import com.dev.vault96.dto.user.Member;
    import com.dev.vault96.service.member.MemberService;
    import lombok.RequiredArgsConstructor;
    import org.springframework.security.authentication.AuthenticationManager;
    import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
    import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
    import org.springframework.security.core.userdetails.UserDetails;
    import org.springframework.security.core.userdetails.UserDetailsService;
    import org.springframework.security.core.userdetails.UsernameNotFoundException;
    import org.springframework.stereotype.Service;

    @Service
    @RequiredArgsConstructor
    public class LoginService implements UserDetailsService {

        private final MemberService memberService;
        private final AuthenticationConfiguration authenticationConfiguration; // ✅ AuthenticationManager를 직접 가져오기 위해 사용

        @Override
        public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

            Member member =  memberService.findMemberByEmail(email);
            if(member == null) {throw new UsernameNotFoundException("No Such User");}
            return member;
        }

        public Member authenticate(String email, String rawPassword) {
            try {
                AuthenticationManager authenticationManager = authenticationConfiguration.getAuthenticationManager();
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(email, rawPassword)
                );

                Member member =  memberService.findMemberByEmail(email);
                if(member == null) {throw new UsernameNotFoundException("No Such User");};
                return member;

            } catch (Exception e) {
                throw new RuntimeException("Authentication failed", e);
            }
        }
    }
