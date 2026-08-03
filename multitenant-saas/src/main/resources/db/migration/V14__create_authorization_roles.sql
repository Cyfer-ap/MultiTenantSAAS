CREATE TABLE authorization_roles (
                                     id UUID NOT NULL,
                                     tenant_id UUID NOT NULL,
                                     code VARCHAR(60) NOT NULL,
                                     name VARCHAR(150) NOT NULL,
                                     description VARCHAR(500),
                                     source VARCHAR(30) NOT NULL,
                                     status VARCHAR(30) NOT NULL,
                                     created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                     updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

                                     CONSTRAINT pk_authorization_roles
                                         PRIMARY KEY (id),

                                     CONSTRAINT uk_authorization_role_tenant_code
                                         UNIQUE (
                                                 tenant_id,
                                                 code
                                             ),

                                     CONSTRAINT fk_authorization_role_tenant
                                         FOREIGN KEY (tenant_id)
                                             REFERENCES tenants (id),

                                     CONSTRAINT ck_authorization_role_source
                                         CHECK (
                                             source IN (
                                                        'SYSTEM',
                                                        'TENANT'
                                                 )
                                             ),

                                     CONSTRAINT ck_authorization_role_status
                                         CHECK (
                                             status IN (
                                                        'ACTIVE',
                                                        'INACTIVE'
                                                 )
                                             )
);


CREATE INDEX idx_authorization_role_tenant_status
    ON authorization_roles (
                            tenant_id,
                            status
        );


CREATE INDEX idx_authorization_role_tenant_source
    ON authorization_roles (
                            tenant_id,
                            source
        );


CREATE TABLE authorization_role_permissions (
                                                id UUID NOT NULL,
                                                tenant_id UUID NOT NULL,
                                                role_id UUID NOT NULL,
                                                permission_id UUID NOT NULL,
                                                created_at TIMESTAMP WITH TIME ZONE NOT NULL,

                                                CONSTRAINT pk_authorization_role_permissions
                                                    PRIMARY KEY (id),

                                                CONSTRAINT uk_authorization_role_permission
                                                    UNIQUE (
                                                            role_id,
                                                            permission_id
                                                        ),

                                                CONSTRAINT fk_authorization_role_permission_tenant
                                                    FOREIGN KEY (tenant_id)
                                                        REFERENCES tenants (id),

                                                CONSTRAINT fk_authorization_role_permission_role
                                                    FOREIGN KEY (role_id)
                                                        REFERENCES authorization_roles (id),

                                                CONSTRAINT fk_authorization_role_permission_permission
                                                    FOREIGN KEY (permission_id)
                                                        REFERENCES authorization_permissions (id)
);


CREATE INDEX idx_authorization_role_permission_role
    ON authorization_role_permissions (
                                       tenant_id,
                                       role_id
        );


CREATE INDEX idx_authorization_role_permission_permission
    ON authorization_role_permissions (
                                       permission_id
        );