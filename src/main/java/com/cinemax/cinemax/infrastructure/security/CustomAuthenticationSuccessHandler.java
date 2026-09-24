package com.cinemax.cinemax.infrastructure.security;

import com.cinemax.cinemax.domain.config.AuditLog;
import com.cinemax.cinemax.domain.config.AuditService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

        @org.springframework.beans.factory.annotation.Autowired
        private AuditService auditService;

        @Override
        public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                            Authentication authentication) throws IOException, ServletException {
            
            String redirectUrl = "/"; // Halaman default untuk USER biasa

            // Cek role dari user yang baru saja login
            Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
            for (GrantedAuthority grantedAuthority : authorities) {
                String role = grantedAuthority.getAuthority();
                if (role.equals("ROLE_ADMIN") || role.equals("ROLE_SUPER_ADMIN")) {
                    redirectUrl = "/admin/dashboard"; // Arahkan ke dashboard admin jika role-nya ADMIN atau SUPER_ADMIN
                    auditService.log(AuditLog.ActionType.LOGIN, "Sistem Admin", "Admin melakukan login ke sistem");
                    break;
                }
            }
            
            response.sendRedirect(redirectUrl);
    }
}
