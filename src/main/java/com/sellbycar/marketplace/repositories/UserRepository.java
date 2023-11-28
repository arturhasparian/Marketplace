package com.sellbycar.marketplace.repositories;

import com.sellbycar.marketplace.models.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    Optional<User> findUserByFirstName(String name);

    Optional<User> findUserByUniqueCode(String uniqueCode);

}