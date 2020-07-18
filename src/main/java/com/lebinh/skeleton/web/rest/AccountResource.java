package com.lebinh.skeleton.web.rest;

import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.lebinh.skeleton.entity.User;
import com.lebinh.skeleton.repository.UserRepository;
import com.lebinh.skeleton.security.SecurityUtils;
import com.lebinh.skeleton.service.MailService;
import com.lebinh.skeleton.service.UserService;
import com.lebinh.skeleton.service.dto.UserDto;
import com.lebinh.skeleton.web.rest.vm.KeyAndPasswordVm;
import com.lebinh.skeleton.web.rest.vm.ManagedUserVm;

/** REST controller for managing the current user's account. */
@RestController
@RequestMapping("/api")
public class AccountResource {

  private final Logger log = LoggerFactory.getLogger(AccountResource.class);

  private final UserRepository userRepository;

  private final UserService userService;

  private final MailService mailService;

  public AccountResource(
      UserRepository userRepository, UserService userService, MailService mailService) {

    this.userRepository = userRepository;
    this.userService = userService;
    this.mailService = mailService;
  }

  /**
   * POST /register : register the user.
   *
   * @param managedUserVm the managed user View Model
   * @throws InvalidPasswordException 400 (Bad Request) if the password is incorrect
   * @throws EmailAlreadyUsedException 400 (Bad Request) if the email is already used
   * @throws LoginAlreadyUsedException 400 (Bad Request) if the login is already used
   */
  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  public void registerAccount(@Valid @RequestBody ManagedUserVm managedUserVm) {
    if (!checkPasswordLength(managedUserVm.getPassword())) {}
    userRepository.findOneByLogin(managedUserVm.getLogin().toLowerCase()).ifPresent(u -> {});
    userRepository.findOneByEmailIgnoreCase(managedUserVm.getEmail()).ifPresent(u -> {});
    User user = userService.registerUser(managedUserVm, managedUserVm.getPassword());
    mailService.sendActivationEmail(user);
  }

  /**
   * GET /activate : activate the registered user.
   *
   * @param key the activation key
   * @throws RuntimeException 500 (Internal Server Error) if the user couldn't be activated
   */
  @GetMapping("/activate")
  public void activateAccount(@RequestParam(value = "key") String key) {
    Optional<User> user = userService.activateRegistration(key);
    if (!user.isPresent()) {}
  }

  /**
   * GET /authenticate : check if the user is authenticated, and return its login.
   *
   * @param request the HTTP request
   * @return the login if the user is authenticated
   */
  @GetMapping("/authenticate")
  public String isAuthenticated(HttpServletRequest request) {
    log.debug("REST request to check if the current user is authenticated");
    return request.getRemoteUser();
  }

  /**
   * GET /account : get the current user.
   *
   * @return the current user
   * @throws RuntimeException 500 (Internal Server Error) if the user couldn't be returned
   */
  @GetMapping("/account")
  public UserDto getAccount() {
    return userService
        .getUserWithAuthorities()
        .map(UserDto::new)
        .orElseThrow(() -> new RuntimeException());
  }

  /**
   * POST /account : update the current user information.
   *
   * @param userDto the current user information
   * @throws EmailAlreadyUsedException 400 (Bad Request) if the email is already used
   * @throws RuntimeException 500 (Internal Server Error) if the user login wasn't found
   */
  @PostMapping("/account")
  public void saveAccount(@Valid @RequestBody UserDto userDto) {
    final String userLogin =
        SecurityUtils.getCurrentUserLogin().orElseThrow(() -> new RuntimeException());
    Optional<User> existingUser = userRepository.findOneByEmailIgnoreCase(userDto.getEmail());
    if (existingUser.isPresent() && (!existingUser.get().getLogin().equalsIgnoreCase(userLogin))) {}
    Optional<User> user = userRepository.findOneByLogin(userLogin);
    if (!user.isPresent()) {}
    userService.updateUser(
        userDto.getFirstName(),
        userDto.getLastName(),
        userDto.getEmail(),
        userDto.getLangKey(),
        userDto.getImageUrl());
  }

  /**
   * POST /account/change-password : changes the current user's password
   *
   * @param password the new password
   * @throws InvalidPasswordException 400 (Bad Request) if the new password is incorrect
   */
  @PostMapping(path = "/account/change-password")
  public void changePassword(@RequestBody String password) {
    if (!checkPasswordLength(password)) {}
    userService.changePassword(password);
  }

  /**
   * POST /account/reset-password/init : Send an email to reset the password of the user
   *
   * @param mail the mail of the user
   * @throws EmailNotFoundException 400 (Bad Request) if the email address is not registered
   */
  @PostMapping(path = "/account/reset-password/init")
  public void requestPasswordReset(@RequestBody String mail) {
    mailService.sendPasswordResetMail(
        userService.requestPasswordReset(mail).orElseThrow(RuntimeException::new));
  }

  /**
   * POST /account/reset-password/finish : Finish to reset the password of the user
   *
   * @param keyAndPassword the generated key and the new password
   * @throws InvalidPasswordException 400 (Bad Request) if the password is incorrect
   * @throws RuntimeException 500 (Internal Server Error) if the password could not be reset
   */
  @PostMapping(path = "/account/reset-password/finish")
  public void finishPasswordReset(@RequestBody KeyAndPasswordVm keyAndPassword) {
    if (!checkPasswordLength(keyAndPassword.getNewPassword())) {}
    Optional<User> user =
        userService.completePasswordReset(keyAndPassword.getNewPassword(), keyAndPassword.getKey());
    if (!user.isPresent()) {}
  }

  private static boolean checkPasswordLength(String password) {
    return !StringUtils.isEmpty(password)
        && password.length() >= ManagedUserVm.PASSWORD_MIN_LENGTH
        && password.length() <= ManagedUserVm.PASSWORD_MAX_LENGTH;
  }
}
