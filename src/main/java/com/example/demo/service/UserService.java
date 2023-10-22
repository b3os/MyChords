//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.example.demo.service;

import com.example.demo.dto.UserDTO;
import com.example.demo.dto.UserResponeDTO;
import com.example.demo.entity.ActivationToken;
import com.example.demo.entity.MusicianInformation;
import com.example.demo.entity.User;
import com.example.demo.repository.ActivationTokenRepository;
import com.example.demo.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.apache.commons.lang3.RandomStringUtils;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Transactional
@RequiredArgsConstructor
@Slf4j
@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    private boolean isTokenValid(LocalDateTime expiryDate) {
        LocalDateTime currentDateTime = LocalDateTime.now();
        return expiryDate != null && expiryDate.isAfter(currentDateTime);
    }

    // Sign Up
    public ResponseEntity<String> signup(UserDTO userDTO) {
        Optional<User> foundUser = this.userRepository.findUserByUsername(userDTO.getUsername());
        if (foundUser.isEmpty()) {
            Optional<String> mail = this.userRepository.findUserMail(userDTO.getMail());
            if (mail.isEmpty()) {
                try {
                    String token = RandomStringUtils.randomAlphanumeric(64);
                    User user = new User(userDTO.getUsername(),
                            this.passwordEncoder.encode(userDTO.getPassword()),
                            userDTO.getMail(),
                            userDTO.getRole(),
                            -1);
                    ActivationToken activationToken = new ActivationToken(token, LocalDateTime.now().plusHours(12), user);
                    user.setActivationToken(activationToken);
                    this.userRepository.save(user);
                    this.emailService.sendEmail(userDTO.getMail(),
                            "Activate Your Account",
                            "http://localhost:8080/api/v1/user/active?activetoken=" + token);
                    if (user.getRole().equals("MS")) {
                        MusicianInformation information = new MusicianInformation();
                        user.setInformation(information);
                        this.userRepository.save(user);
                    }
                    return new ResponseEntity<>("Signup Successfully", HttpStatus.OK);
                } catch (IllegalArgumentException e) {
                    return new ResponseEntity<>("Invalid Gender Value", HttpStatus.BAD_REQUEST);
                }
            }
            return new ResponseEntity<>("Email is already signed up", HttpStatus.NOT_IMPLEMENTED);
        }
        return new ResponseEntity<>("Username is already signed up", HttpStatus.NOT_IMPLEMENTED);
    }

    // Active Account
    public ResponseEntity<String> activateUserAccount(String token) {
        User foundUser = this.userRepository.findByActivationToken(token);
        if (foundUser != null && foundUser.getStatus() == -1 && isTokenValid(foundUser.getActivationToken().getExpiryDate())) {
            foundUser.setStatus(1);
            userRepository.save(foundUser);
            return new ResponseEntity<>("Active Successfully", HttpStatus.OK);
        } else {
            return new ResponseEntity<>("Active Failed", HttpStatus.BAD_REQUEST);
        }
    }

    // Admin Detail
    public User getDetailUser_Admin(Long id) {
        Optional<User> foundUser = this.userRepository.findById(id);
        if (foundUser.isPresent()) {
            return this.userRepository.findById(id).orElseThrow();
        }
        return null;
    }

    // User Detail
    public UserResponeDTO getDetailUser_User(Long id) {
        Optional<User> foundUser = this.userRepository.findById(id);
        if (foundUser.isPresent()) {
            User user = foundUser.get();
            UserResponeDTO dto = new UserResponeDTO();
            dto.setId(user.getId());
            dto.setUsername(user.getUsername());
            dto.setFullName(user.getFullName());
            dto.setGender(user.getGender().toString());
            return dto;
        }
        return null;
    }

    // Banned User
    public ResponseEntity<String> banUser(User newUser, Long id) {
        Optional<User> foundUser = this.userRepository.findById(id);
        if (foundUser.isPresent()) {
            User user = foundUser.get();
            user.setStatus(newUser.getStatus());
            this.userRepository.save(user);
            return new ResponseEntity<>("Ban Successfully", HttpStatus.OK);
        } else {
            return new ResponseEntity<>("Ban Failed", HttpStatus.NOT_IMPLEMENTED);
        }
    }

    // Update User Info
    public ResponseEntity<String> updateUserInfo(UserDTO userDTO, Long id) {
        Optional<User> foundUser = this.userRepository.findById(id);
        if (foundUser.isPresent()) {
            User.Gender gender = User.Gender.valueOf(userDTO.getGender());
            User user = foundUser.get();
            user.setFullName(userDTO.getFullName());
            user.setGender(gender);
            user.setMail(userDTO.getMail());
            user.setPhoneNumber(userDTO.getPhone());
            user.setAddress(userDTO.getAddress());
            this.userRepository.save(user);
            return new ResponseEntity<>("Update Successfully", HttpStatus.OK);
        } else {
            return new ResponseEntity<>("Update Failed", HttpStatus.NOT_IMPLEMENTED);
        }
    }

    // Update Admin Info
    public ResponseEntity<String> updateAdminInfo(UserDTO userDTO, Long id) {
        Optional<User> foundUser = this.userRepository.findById(id);
        if (foundUser.isPresent()) {
            User.Gender gender = User.Gender.valueOf(userDTO.getGender());
            User user = foundUser.get();
            user.setFullName(userDTO.getFullName());
            user.setGender(gender);
            this.userRepository.save(user);
            return new ResponseEntity<>("Update Successfully", HttpStatus.OK);
        } else {
            return new ResponseEntity<>("Update Failed", HttpStatus.NOT_IMPLEMENTED);
        }
    }

    // Update Musician Info
    public ResponseEntity<String> updateMusicianInfo(UserDTO userDTO, Long id) {
        Optional<User> foundUser = this.userRepository.findById(id);
        if (foundUser.isPresent()) {
            User.Gender gender = User.Gender.valueOf(userDTO.getGender());
            User user = foundUser.get();
            user.setFullName(userDTO.getFullName());
            user.setGender(gender);
            user.setMail(userDTO.getMail());
            user.setPhoneNumber(userDTO.getPhone());
            user.setAddress(userDTO.getAddress());
            MusicianInformation information = user.getInformation();
            information.setPrize(userDTO.getPrize());
            information.setProfessional(userDTO.getProfessional());
            information.setYear(userDTO.getYear());
            this.userRepository.save(user);
            return new ResponseEntity<>("Update Successfully", HttpStatus.OK);
        } else {
            return new ResponseEntity<>("Update Failed", HttpStatus.NOT_IMPLEMENTED);
        }
    }

    // Search User - username
    public List<User> searchByUserName(UserDTO userDTO) {
        List<User> userEntity = this.userRepository.searchByUserName(userDTO.getUsername());
        return userEntity.isEmpty() ? null : userEntity;
    }

    // Get All User
    public List<UserResponeDTO> getAllUsers() {
        List<User> userList = this.userRepository.findByOrderByStatusDesc();
        List<UserResponeDTO> userResponeDTOList;
        if (userList.isEmpty()) {
            return null;
        } else {
            userResponeDTOList = userList.stream().map(user -> new UserResponeDTO(
                    user.getId(),
                    user.getUsername(),
                    user.getFullName(),
                    user.getGender().toString(),
                    user.getRole(),
                    user.getMail()
                    ,
                    user.getPhoneNumber(),
                    user.getStatus())).collect(Collectors.toList());
            return userResponeDTOList;
        }
    }

}
