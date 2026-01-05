package mmi.osaas.txlforma.config;

import lombok.RequiredArgsConstructor;
import mmi.osaas.txlforma.security.jwt.JwtAuthFilter;
import mmi.osaas.txlforma.security.jwt.JwtAuthenticationEntryPoint;
import mmi.osaas.txlforma.security.util.JwtUtils;
import mmi.osaas.txlforma.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationEntryPoint unauthorizedHandler;
    private final JwtUtils jwtUtils;

    @Value("${app.base-url:http://localhost:5173}")
    private String baseUrl;

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtAuthFilter jwtAuthFilter(@Lazy UserService userService) {
        return new JwtAuthFilter(jwtUtils, userService);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthFilter jwtAuthFilter) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .exceptionHandling(eh -> eh.authenticationEntryPoint(unauthorizedHandler))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // PUBLIC - Accès sans authentification
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/payments/webhook").permitAll()
                        
                        // PUBLIC READ - Lecture publique (GET uniquement)
                        .requestMatchers(HttpMethod.GET, "/api/categories/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/formations/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/sessions/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/files/**").permitAll()
                        
                        // AUTHENTICATED - Tous les utilisateurs connectés
                        .requestMatchers("/api/payments/**").authenticated()
                        .requestMatchers("/api/users/me").authenticated()
                        .requestMatchers("/api/users/{id}").authenticated()
                        .requestMatchers("/api/panier/**").authenticated()
                        .requestMatchers("/api/participations/me").authenticated()
                        .requestMatchers("/api/participations/**").authenticated()
                        .requestMatchers("/api/emargements/**").authenticated()
                        .requestMatchers("/api/notes/**").authenticated()
                        .requestMatchers("/api/attestations/**").authenticated()
                        
                        // FILE UPLOAD - Upload de fichiers authentifié
                        .requestMatchers(HttpMethod.POST, "/api/files/upload/**").authenticated()
                        
                        // FORMATEUR/ADMIN - Formateurs et administrateurs
                        .requestMatchers("/api/emargements/session/**").hasAnyRole("FORMATEUR", "ADMIN")
                        .requestMatchers("/api/notes/session/**").hasAnyRole("FORMATEUR", "ADMIN")
                        .requestMatchers("/api/formateur/**").hasRole("FORMATEUR")
                        
                        // ADMIN ONLY - Administrateurs uniquement
                        .requestMatchers("/api/statistics/**").hasRole("ADMIN")
                        .requestMatchers("/api/attestations/regenerate/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/categories/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/categories/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/categories/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/users/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/users/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/users/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/formations/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/formations/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/formations/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/sessions/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/sessions/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/sessions/**").hasRole("ADMIN")
                        
                        // FALLBACK - Toute autre requête nécessite une authentification
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Liste des origines autorisées : localhost pour le dev + URL de production depuis app.base-url
        List<String> allowedOrigins = Arrays.asList(
                "http://localhost:5173",
                "http://localhost:3000",
                "http://127.0.0.1:5173",
                "http://127.0.0.1:3000",
                baseUrl  // URL de production depuis la variable d'environnement
        );

        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setExposedHeaders(List.of("Authorization", "Content-Type", "X-Total-Count"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }
}