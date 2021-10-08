package com.t1908e.memeportalapi.service;

import com.t1908e.memeportalapi.dto.UserDTO;
import com.t1908e.memeportalapi.dto.RegisterDTO;
import com.t1908e.memeportalapi.entity.Account;
import com.t1908e.memeportalapi.entity.Role;
import com.t1908e.memeportalapi.entity.User;
import com.t1908e.memeportalapi.repository.AccountRepository;
import com.t1908e.memeportalapi.repository.RoleRepository;
import com.t1908e.memeportalapi.repository.UserRepository;
import com.t1908e.memeportalapi.util.RESTResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.*;


@Service
@Transactional
@RequiredArgsConstructor
public class AuthenticationService implements UserDetailsService {
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private static final String USER_ROLE = "user";

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<Account> accountOptional = accountRepository.findByUsername(username);
        Account account = accountOptional.orElse(null);
        if (account == null) {
            throw new UsernameNotFoundException("User not found in database");
        }
        Collection<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(account.getRole().getName()));
        return new org.springframework.security.core.userdetails.User(account.getUsername(), account.getPassword(), authorities);

    }

    public ResponseEntity<Object> saveAccount(RegisterDTO registerDTO) {
        HashMap<String, Object> restResponse = new HashMap<>();
        //create new user role if not exist
        Optional<Role> userRoleOptional = roleRepository.findByName(USER_ROLE);
        Role userRole = userRoleOptional.orElse(null);
        if (userRole == null) {
            //create new role
            userRole = roleRepository.save(new Role(USER_ROLE));
        }
        //check if username has exist
        Optional<Account> byUsername = accountRepository.findByUsername(registerDTO.getUsername());
        if (byUsername.isPresent()) {
            restResponse = new RESTResponse.CustomError()
                    .setCode(HttpStatus.BAD_REQUEST.value())
                    .setMessage("username " + byUsername.get().getUsername() + " is already exist").build();
            return ResponseEntity.badRequest().body(restResponse);
        }
        //save new account
        Account account = new Account();

        account.setUsername(registerDTO.getUsername());
        account.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
        account.setCreatedAt(new Date());
        account.setUpdatedAt(new Date());
        account.setStatus(1);
        account.setRole(userRole);

        //user profile
        User user = new User();
        user.setPhone(registerDTO.getPhone());
        user.setFullName(registerDTO.getFullName());
        user.setStatus(1); //active
        user.setDisplayNameColor("#111");
        user.setCreatedAt(new Date());
        user.setUpdatedAt(new Date());
        user.setAccount(account);
        try {
            User save = userRepository.save(user);
            restResponse = new RESTResponse.Success()
                    .setMessage("success")
                    .setStatus(HttpStatus.OK.value())
                    .setData(new UserDTO(save)).build();

            return ResponseEntity.ok().body(restResponse);
        } catch (Exception exception) {
            restResponse = new RESTResponse.CustomError()
                    .setCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .setMessage("Register failed " + exception.getMessage()).build();
            return ResponseEntity.internalServerError().body(restResponse);
        }
    }

    public Account getAccount(String username) {
        Optional<Account> byUsername = accountRepository.findByUsername(username);
        return byUsername.orElse(null);
    }

    public User getAppUser(String username) {
        Optional<Account> byUsername = accountRepository.findByUsername(username);
        if (byUsername.isPresent()) {
            Account account = byUsername.get();
            User user = account.getUser();
            return user;
        }
        return null;
    }
}
