package mmi.osaas.txlforma.service;

import lombok.RequiredArgsConstructor;
import mmi.osaas.txlforma.dto.UserDTO;
import mmi.osaas.txlforma.enums.Role;
import mmi.osaas.txlforma.model.User;
import mmi.osaas.txlforma.repository.UserRepository;
import mmi.osaas.txlforma.security.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder encoder;

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));
    }

    @Transactional
    public User createUser(UserDTO dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email déjà utilisé");
        }

        return userRepository.save(User.builder()
                .firstname(dto.getFirstname())
                .lastname(dto.getLastname())
                .email(dto.getEmail())
                .password(encoder.encode(dto.getPassword()))
                .role(dto.getRole() != null ? dto.getRole() : Role.USER)
                .imageUrl(dto.getImageUrl())
                .createdAt(LocalDateTime.now())
                .build());
    }

    @Transactional
    public User updateUser(Long id, UserDTO dto) {
        User existing = getUserById(id);

        existing.setFirstname(dto.getFirstname());
        existing.setLastname(dto.getLastname());
        existing.setEmail(dto.getEmail());
        
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            existing.setPassword(encoder.encode(dto.getPassword()));
        }
        
        if (dto.getRole() != null) {
            existing.setRole(dto.getRole());
        }

        if (dto.getImageUrl() != null) {
            existing.setImageUrl(dto.getImageUrl());
        }

        return userRepository.save(existing);
    }

    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable");
        }
        userRepository.deleteById(id);
    }

    public List<User> getFormateurs() {
        return userRepository.findByRole(Role.FORMATEUR);
    }

    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur introuvable : " + email));
        return new UserPrincipal(user);
    }
}
