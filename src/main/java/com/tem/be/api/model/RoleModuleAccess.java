package com.tem.be.api.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

/**
 * Represents a role module access entity.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "role_module_access")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class RoleModuleAccess {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id")
    @JsonBackReference
    private Role role;

    @Column(name = "module_name")
    private String moduleName;

    @Column(name = "access_type")
    private String accessType;

    public RoleModuleAccess(String moduleName, String accessType, Role role) {
        this.moduleName = moduleName;
        this.accessType = accessType;
        this.role = role;
    }
}

