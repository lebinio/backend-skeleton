package com.lebinh.skeleton.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.lebinh.skeleton.entity.Authority;

/** Spring Data JPA repository for the Authority entity. */
public interface AuthorityRepository extends JpaRepository<Authority, String> {}
