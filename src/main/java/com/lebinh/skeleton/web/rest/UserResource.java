package com.lebinh.skeleton.web.rest;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import javax.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.lebinh.skeleton.constant.CommonConstant;
import com.lebinh.skeleton.entity.User;
import com.lebinh.skeleton.repository.UserRepository;
import com.lebinh.skeleton.security.AuthoritiesConstants;
import com.lebinh.skeleton.service.MailService;
import com.lebinh.skeleton.service.UserService;
import com.lebinh.skeleton.service.dto.UserDto;
import com.lebinh.skeleton.utils.HeaderUtil;
import com.lebinh.skeleton.utils.PaginationUtil;
import com.lebinh.skeleton.utils.ResponseUtil;

/**
 * REST controller for managing users.
 *
 * <p>This class accesses the User entity, and needs to fetch its collection of authorities.
 *
 * <p>For a normal use-case, it would be better to have an eager relationship between User and
 * Authority, and send everything to the client side: there would be no View Model and Dto, a lot
 * less code, and an outer-join which would be good for performance.
 *
 * <p>We use a View Model and a Dto for 3 reasons:
 *
 * <ul>
 *   <li>We want to keep a lazy association between the user and the authorities, because people
 *       will quite often do relationships with the user, and we don't want them to get the
 *       authorities all the time for nothing (for performance reasons). This is the #1 goal: we
 *       should not impact our users' application because of this use-case.
 *   <li>Not having an outer join causes n+1 requests to the database. This is not a real issue as
 *       we have by default a second-level cache. This means on the first HTTP call we do the n+1
 *       requests, but then all authorities come from the cache, so in fact it's much better than
 *       doing an outer join (which will get lots of data from the database, for each HTTP call).
 *   <li>As this manages users, for security reasons, we'd rather have a Dto layer.
 * </ul>
 *
 * <p>Another option would be to have a specific JPA entity graph to handle this case.
 */
@RestController
@RequestMapping("/api")
public class UserResource {

  private final Logger log = LoggerFactory.getLogger(UserResource.class);

  private final UserRepository userRepository;

  private final UserService userService;

  private final MailService mailService;

  public UserResource(
      UserRepository userRepository, UserService userService, MailService mailService) {

    this.userRepository = userRepository;
    this.userService = userService;
    this.mailService = mailService;
  }

  /**
   * POST /users : Creates a new user.
   *
   * <p>Creates a new user if the login and email are not already used, and sends an mail with an
   * activation link. The user needs to be activated on creation.
   *
   * @param userDto the user to create
   * @return the ResponseEntity with status 201 (Created) and with body the new user, or with status
   *     400 (Bad Request) if the login or email is already in use
   * @throws URISyntaxException if the Location URI syntax is incorrect
   * @throws BadRequestAlertException 400 (Bad Request) if the login or email is already in use
   */
  @PostMapping("/users")
  @Secured(AuthoritiesConstants.ADMIN)
  public ResponseEntity<User> createUser(@Valid @RequestBody UserDto userDto)
      throws URISyntaxException {
    log.debug("REST request to save User : {}", userDto);

    if (userDto.getId() != null) {
    } else if (userRepository.findOneByLogin(userDto.getLogin().toLowerCase()).isPresent()) {
    } else if (userRepository.findOneByEmailIgnoreCase(userDto.getEmail()).isPresent()) {
    } else {
      User newUser = userService.createUser(userDto);
      mailService.sendCreationEmail(newUser);
      return ResponseEntity.created(new URI("/api/users/" + newUser.getLogin()))
          .headers(
              HeaderUtil.createAlert(
                  "A user is created with identifier " + newUser.getLogin(), newUser.getLogin()))
          .body(newUser);
    }
    return null;
  }

  /**
   * PUT /users : Updates an existing User.
   *
   * @param userDto the user to update
   * @return the ResponseEntity with status 200 (OK) and with body the updated user
   * @throws EmailAlreadyUsedException 400 (Bad Request) if the email is already in use
   * @throws LoginAlreadyUsedException 400 (Bad Request) if the login is already in use
   */
  @PutMapping("/users")
  @Secured(AuthoritiesConstants.ADMIN)
  public ResponseEntity<UserDto> updateUser(@Valid @RequestBody UserDto userDto) {
    log.debug("REST request to update User : {}", userDto);
    Optional<User> existingUser = userRepository.findOneByEmailIgnoreCase(userDto.getEmail());
    if (existingUser.isPresent() && (!existingUser.get().getId().equals(userDto.getId()))) {}
    existingUser = userRepository.findOneByLogin(userDto.getLogin().toLowerCase());
    if (existingUser.isPresent() && (!existingUser.get().getId().equals(userDto.getId()))) {}
    Optional<UserDto> updatedUser = userService.updateUser(userDto);

    return ResponseUtil.wrapOrNotFound(
        updatedUser,
        HeaderUtil.createAlert(
            "A user is updated with identifier " + userDto.getLogin(), userDto.getLogin()));
  }

  /**
   * GET /users : get all users.
   *
   * @param pageable the pagination information
   * @return the ResponseEntity with status 200 (OK) and with body all users
   */
  @GetMapping("/users")
  public ResponseEntity<List<UserDto>> getAllUsers(Pageable pageable) {
    final Page<UserDto> page = userService.getAllManagedUsers(pageable);
    HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/users");
    return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
  }

  /** @return a string list of the all of the roles */
  @GetMapping("/users/authorities")
  @Secured(AuthoritiesConstants.ADMIN)
  public List<String> getAuthorities() {
    return userService.getAuthorities();
  }

  /**
   * GET /users/:login : get the "login" user.
   *
   * @param login the login of the user to find
   * @return the ResponseEntity with status 200 (OK) and with body the "login" user, or with status
   *     404 (Not Found)
   */
  @GetMapping("/users/{login:" + CommonConstant.LOGIN_REGEX + "}")
  public ResponseEntity<UserDto> getUser(@PathVariable String login) {
    log.debug("REST request to get User : {}", login);
    return ResponseUtil.wrapOrNotFound(
        userService.getUserWithAuthoritiesByLogin(login).map(UserDto::new));
  }

  /**
   * DELETE /users/:login : delete the "login" User.
   *
   * @param login the login of the user to delete
   * @return the ResponseEntity with status 200 (OK)
   */
  @DeleteMapping("/users/{login:" + CommonConstant.LOGIN_REGEX + "}")
  @Secured(AuthoritiesConstants.ADMIN)
  public ResponseEntity<Void> deleteUser(@PathVariable String login) {
    log.debug("REST request to delete User: {}", login);
    userService.deleteUser(login);
    return ResponseEntity.ok()
        .headers(HeaderUtil.createAlert("A user is deleted with identifier " + login, login))
        .build();
  }
}
