package com.built4u.pos.user;

import com.built4u.pos.auth.Modules;
import com.built4u.pos.common.exception.BadRequestException;
import com.built4u.pos.common.exception.ConflictException;
import com.built4u.pos.common.exception.NotFoundException;
import com.built4u.pos.user.dto.CreateRoleRequest;
import com.built4u.pos.user.dto.ModuleDto;
import com.built4u.pos.user.dto.RoleDetailDto;
import com.built4u.pos.user.dto.UpdateRoleRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Admin CRUD for roles + their module grants.
 *
 * <ul>
 *   <li>The wildcard role (ADMIN) is immutable and undeletable.</li>
 *   <li>Built-in roles (ADMIN/MANAGER/CASHIER) cannot be deleted; MANAGER/CASHIER
 *       module sets ARE editable.</li>
 *   <li>A role assigned to >= 1 user cannot be deleted (reassign first).</li>
 *   <li>New roles can never be created as wildcard.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RoleAdminService {

    private final RoleRepository roleRepository;
    private final ModuleRepository moduleRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<ModuleDto> listModules() {
        return moduleRepository.findAllByOrderBySortOrderAsc().stream()
            .map(m -> new ModuleDto(m.getCode(), m.getName(), m.getDescription(), m.getSortOrder()))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<RoleDetailDto> list() {
        return roleRepository.findAll().stream()
            .sorted(Comparator.comparing(Role::getCode))
            .map(this::toDetail)
            .toList();
    }

    @Transactional(readOnly = true)
    public RoleDetailDto get(Long id) {
        return toDetail(loadRole(id));
    }

    @Transactional
    public RoleDetailDto create(CreateRoleRequest req) {
        String code = req.code().trim().toUpperCase();
        if ("ADMIN".equals(code)) {
            throw new BadRequestException("ADMIN is a reserved system role");
        }
        if (roleRepository.findByCode(code).isPresent()) {
            throw new ConflictException("Role code '" + code + "' already exists");
        }
        Set<String> modules = validateModules(req.moduleCodes());

        Role role = Role.builder()
            .code(code)
            .name(req.name().trim())
            .description(blankToNull(req.description()))
            .builtIn("N")
            .wildcard("N")
            .moduleCodes(modules)
            .build();
        Role saved = roleRepository.save(role);
        log.info("Role {} created with modules {}", code, modules);
        return toDetail(saved);
    }

    @Transactional
    public RoleDetailDto update(Long id, UpdateRoleRequest req) {
        Role role = loadRole(id);
        if (role.isWildcard()) {
            throw new BadRequestException(
                "The ADMIN role has full access and is system-managed — it cannot be modified.");
        }
        Set<String> modules = validateModules(req.moduleCodes());
        role.setName(req.name().trim());
        role.setDescription(blankToNull(req.description()));
        role.setModuleCodes(modules);
        log.info("Role {} updated; modules now {}", role.getCode(), modules);
        return toDetail(roleRepository.save(role));
    }

    @Transactional
    public void delete(Long id) {
        Role role = loadRole(id);
        if (role.isBuiltIn()) {
            throw new BadRequestException("Built-in roles cannot be deleted");
        }
        long inUse = userRepository.countByRoles_Id(id);
        if (inUse > 0) {
            throw new ConflictException(
                "Role '" + role.getCode() + "' is assigned to " + inUse +
                " user(s). Reassign them before deleting this role.");
        }
        roleRepository.delete(role);
        log.info("Role {} deleted", role.getCode());
    }

    private Role loadRole(Long id) {
        return roleRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Role " + id + " not found"));
    }

    private Set<String> validateModules(List<String> requested) {
        Set<String> valid = moduleRepository.findAll().stream()
            .map(Module::getCode).collect(Collectors.toSet());
        Set<String> result = new LinkedHashSet<>();
        for (String raw : requested) {
            String code = raw == null ? "" : raw.trim();
            if (!valid.contains(code)) {
                throw new BadRequestException("Unknown module: " + raw);
            }
            result.add(code);
        }
        return result;
    }

    private RoleDetailDto toDetail(Role r) {
        List<String> modules = r.isWildcard()
            ? Modules.ALL.stream().sorted().toList()
            : r.getModuleCodes().stream().sorted().toList();
        return new RoleDetailDto(
            r.getId(), r.getCode(), r.getName(), r.getDescription(),
            r.isBuiltIn(), r.isWildcard(), modules);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
