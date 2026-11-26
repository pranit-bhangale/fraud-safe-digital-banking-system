package com.banking.user.repository;

import com.banking.user.entity.Role;
import com.banking.user.model.RoleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Short> {

    Optional<Role> findByName(RoleType name);
}
