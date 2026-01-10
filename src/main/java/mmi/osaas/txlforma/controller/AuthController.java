package mmi.osaas.txlforma.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mmi.osaas.txlforma.dto.*;
import mmi.osaas.txlforma.enums.Role;
import mmi.osaas.txlforma.model.User;
import mmi.osaas.txlforma.security.util.JwtUtils;
import mmi.osaas.txlforma.security.UserPrincipal;
import mmi.osaas.txlforma.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final UserService userService;


    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterDTO dto) {
        UserDTO.UserDTOBuilder userDTOBuilder = UserDTO.builder()
                .firstname(dto.getFirstname())
                .lastname(dto.getLastname())
                .email(dto.getEmail())
                .password(dto.getPassword())
                .role(Role.USER);
        
        if (dto.getImageUrl() != null && !dto.getImageUrl().isBlank()) {
            userDTOBuilder.imageUrl(dto.getImageUrl());
        }
        
        User saved = userService.createUser(userDTOBuilder.build());
        return ResponseEntity.ok("User created: " + saved.getEmail());
    }



    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginDTO dto) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(dto.getEmail(), dto.getPassword())
            );

            UserPrincipal userPrincipal = (UserPrincipal) auth.getPrincipal();
            String token = jwtUtils.generateJwtToken(auth);

            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("user", userPrincipal.getUser());

            return ResponseEntity.ok(response);
        } catch (BadCredentialsException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "Erreur dans le login ou le mot de passe");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        } catch (AuthenticationException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "Erreur dans le login ou le mot de passe");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }
    }
}