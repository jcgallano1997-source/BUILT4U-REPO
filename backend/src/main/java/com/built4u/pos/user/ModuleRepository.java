package com.built4u.pos.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ModuleRepository extends JpaRepository<Module, String> {
    java.util.List<Module> findAllByOrderBySortOrderAsc();
}
