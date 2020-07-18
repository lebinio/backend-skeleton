package com.lebinh.skeleton.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.lebinh.skeleton.constant.CommonConstant;
import com.lebinh.skeleton.entity.Authority;
import com.lebinh.skeleton.entity.User;
import com.lebinh.skeleton.repository.AuthorityRepository;
import com.lebinh.skeleton.repository.UserRepository;
import com.lebinh.skeleton.security.AuthoritiesConstants;
import com.lebinh.skeleton.security.SecurityUtils;
import com.lebinh.skeleton.service.dto.UserDto;
import com.lebinh.skeleton.utils.RandomUtil;

/** Service class for managing users. */
@Service
@Transactional
public class UserService {

  private final Logger log = LoggerFactory.getLogger(UserService.class);

  private final UserRepository userRepository;

  private final PasswordEncoder passwordEncoder;

  private final AuthorityRepository authorityRepository;

  public UserService(
      UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      AuthorityRepository authorityRepository) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.authorityRepository = authorityRepository;
  }

  public Optional<User> activateRegistration(String key) {
    log.debug("Activating user for activation key {}", key);
    return userRepository
        .findOneByActivationKey(key)
        .map(
            user -> {
              // activate given user for the registration key.
              user.setActivated(true);
              user.setActivationKey(null);
              log.debug("Activated user: {}", user);
              return user;
            });
  }

  public Optional<User> completePasswordReset(String newPassword, String key) {
    log.debug("Reset user password for reset key {}", key);

    return userRepository
        .findOneByResetKey(key)
        .filter(user -> user.getResetDate().isAfter(Instant.now().minusSeconds(86400)))
        .map(
            user -> {
              user.setPassword(passwordEncoder.encode(newPassword));
              user.setResetKey(null);
              user.setResetDate(null);
              return user;
            });
  }

  public Optional<User> requestPasswordReset(String mail) {
    return userRepository
        .findOneByEmailIgnoreCase(mail)
        .filter(User::getActivated)
        .map(
            user -> {
              user.setResetKey(RandomUtil.generateResetKey());
              user.setResetDate(Instant.now());
              return user;
            });
  }

  public User registerUser(UserDto userDto, String password) {

    User newUser = new User();
    Authority authority = authorityRepository.getOne(AuthoritiesConstants.USER);
    Set<Authority> authorities = new HashSet<>();
    String encryptedPassword = passwordEncoder.encode(password);
    newUser.setLogin(userDto.getLogin());
    // new user gets initially a generated password
    newUser.setPassword(encryptedPassword);
    newUser.setFirstName(userDto.getFirstName());
    newUser.setLastName(userDto.getLastName());
    newUser.setEmail(userDto.getEmail());
    newUser.setImageUrl(userDto.getImageUrl());
    newUser.setLangKey(userDto.getLangKey());
    // new user is not active
    newUser.setActivated(false);
    // new user gets registration key
    newUser.setActivationKey(RandomUtil.generateActivationKey());
    authorities.add(authority);
    newUser.setAuthorities(authorities);
    userRepository.save(newUser);
    log.debug("Created Information for User: {}", newUser);
    return newUser;
  }

  public User createUser(UserDto userDto) {
    User user = new User();
    user.setLogin(userDto.getLogin());
    user.setFirstName(userDto.getFirstName());
    user.setLastName(userDto.getLastName());
    user.setEmail(userDto.getEmail());
    user.setImageUrl(userDto.getImageUrl());
    if (userDto.getLangKey() == null) {
      user.setLangKey(CommonConstant.DEFAULT_LANGUAGE); // default language
    } else {
      user.setLangKey(userDto.getLangKey());
    }
    if (userDto.getAuthorities() != null) {
      Set<Authority> authorities =
          userDto
              .getAuthorities()
              .stream()
              .map(authorityRepository::getOne)
              .collect(Collectors.toSet());
      user.setAuthorities(authorities);
    }
    String encryptedPassword = passwordEncoder.encode(RandomUtil.generatePassword());
    user.setPassword(encryptedPassword);
    user.setResetKey(RandomUtil.generateResetKey());
    user.setResetDate(Instant.now());
    user.setActivated(true);
    userRepository.save(user);
    log.debug("Created Information for User: {}", user);
    return user;
  }

  /**
   * Update basic information (first name, last name, email, language) for the current user.
   *
   * @param firstName first name of user
   * @param lastName last name of user
   * @param email email id of user
   * @param langKey language key
   * @param imageUrl image URL of user
   */
  public void updateUser(
      String firstName, String lastName, String email, String langKey, String imageUrl) {
    SecurityUtils.getCurrentUserLogin()
        .flatMap(userRepository::findOneByLogin)
        .ifPresent(
            user -> {
              user.setFirstName(firstName);
              user.setLastName(lastName);
              user.setEmail(email);
              user.setLangKey(langKey);
              user.setImageUrl(imageUrl);
              log.debug("Changed Information for User: {}", user);
            });
  }

  /**
   * Update all information for a specific user, and return the modified user.
   *
   * @param userDto user to update
   * @return updated user
   */
  public Optional<UserDto> updateUser(UserDto userDto) {
    return Optional.of(userRepository.getOne(userDto.getId()))
        .map(
            user -> {
              user.setLogin(userDto.getLogin());
              user.setFirstName(userDto.getFirstName());
              user.setLastName(userDto.getLastName());
              user.setEmail(userDto.getEmail());
              user.setImageUrl(userDto.getImageUrl());
              user.setActivated(userDto.isActivated());
              user.setLangKey(userDto.getLangKey());
              Set<Authority> managedAuthorities = user.getAuthorities();
              managedAuthorities.clear();
              userDto
                  .getAuthorities()
                  .stream()
                  .map(authorityRepository::getOne)
                  .forEach(managedAuthorities::add);
              log.debug("Changed Information for User: {}", user);
              return user;
            })
        .map(UserDto::new);
  }

  public void deleteUser(String login) {
    userRepository
        .findOneByLogin(login)
        .ifPresent(
            user -> {
              userRepository.delete(user);
              log.debug("Deleted User: {}", user);
            });
  }

  public void changePassword(String password) {
    SecurityUtils.getCurrentUserLogin()
        .flatMap(userRepository::findOneByLogin)
        .ifPresent(
            user -> {
              String encryptedPassword = passwordEncoder.encode(password);
              user.setPassword(encryptedPassword);
              log.debug("Changed password for User: {}", user);
            });
  }

  @Transactional(readOnly = true)
  public Page<UserDto> getAllManagedUsers(Pageable pageable) {
    return userRepository.findAllByLoginNot(pageable, CommonConstant.ANONYMOUS_USER).map(UserDto::new);
  }

  @Transactional(readOnly = true)
  public Optional<User> getUserWithAuthoritiesByLogin(String login) {
    return userRepository.findOneWithAuthoritiesByLogin(login);
  }

  @Transactional(readOnly = true)
  public Optional<User> getUserWithAuthorities(Long id) {
    return userRepository.findOneWithAuthoritiesById(id);
  }

  @Transactional(readOnly = true)
  public Optional<User> getUserWithAuthorities() {
    return SecurityUtils.getCurrentUserLogin()
        .flatMap(userRepository::findOneWithAuthoritiesByLogin);
  }

  /**
   * Not activated users should be automatically deleted after 3 days.
   *
   * <p>This is scheduled to get fired everyday, at 01:00 (am).
   */
  @Scheduled(cron = "0 0 1 * * ?")
  public void removeNotActivatedUsers() {
    List<User> users =
        userRepository.findAllByActivatedIsFalseAndCreatedDateBefore(
            Instant.now().minus(3, ChronoUnit.DAYS));
    for (User user : users) {
      log.debug("Deleting not activated user {}", user.getLogin());
      userRepository.delete(user);
    }
  }

  /** @return a list of all the authorities */
  public List<String> getAuthorities() {
    return authorityRepository
        .findAll()
        .stream()
        .map(Authority::getName)
        .collect(Collectors.toList());
  }
}
