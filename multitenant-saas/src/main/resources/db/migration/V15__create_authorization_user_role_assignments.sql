CREATE TABLE authorization_user_role_assignments (
                                                     id UUID NOT NULL,
                                                     tenant_id UUID NOT NULL,
                                                     user_id UUID NOT NULL,
                                                     role_id UUID NOT NULL,
                                                     scope_type VARCHAR(40) NOT NULL,
                                                     scope_target_id UUID,
                                                     scope_key VARCHAR(100) NOT NULL,
                                                     status VARCHAR(30) NOT NULL,
                                                     valid_from TIMESTAMP WITH TIME ZONE NOT NULL,
                                                     valid_until TIMESTAMP WITH TIME ZONE,
                                                     created_by_user_id UUID NOT NULL,
                                                     created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                                     updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

                                                     CONSTRAINT pk_auth_user_role_assignments
                                                         PRIMARY KEY (id),

                                                     CONSTRAINT fk_auth_user_role_assignment_tenant
                                                         FOREIGN KEY (tenant_id)
                                                             REFERENCES tenants (id),

                                                     CONSTRAINT fk_auth_user_role_assignment_user
                                                         FOREIGN KEY (user_id)
                                                             REFERENCES app_users (id),

                                                     CONSTRAINT fk_auth_user_role_assignment_role
                                                         FOREIGN KEY (role_id)
                                                             REFERENCES authorization_roles (id),

                                                     CONSTRAINT fk_auth_user_role_assignment_created_by
                                                         FOREIGN KEY (created_by_user_id)
                                                             REFERENCES app_users (id),

                                                     CONSTRAINT ck_auth_user_role_assignment_scope
                                                         CHECK (
                                                             (
                                                                 scope_type IN (
                                                                                'TENANT',
                                                                                'SELF'
                                                                     )
                                                                     AND scope_target_id IS NULL
                                                                 )
                                                                 OR
                                                             (
                                                                 scope_type IN (
                                                                                'ORGANIZATIONAL_UNIT',
                                                                                'ORGANIZATIONAL_SUBTREE',
                                                                                'DIRECT_REPORTS',
                                                                                'PROJECT'
                                                                     )
                                                                     AND scope_target_id IS NOT NULL
                                                                 )
                                                             ),

                                                     CONSTRAINT ck_auth_user_role_assignment_status
                                                         CHECK (
                                                             status IN (
                                                                        'ACTIVE',
                                                                        'INACTIVE'
                                                                 )
                                                             ),

                                                     CONSTRAINT ck_auth_user_role_assignment_validity
                                                         CHECK (
                                                             valid_until IS NULL
                                                                 OR valid_until > valid_from
                                                             )
);


CREATE INDEX idx_auth_user_role_assignment_user
    ON authorization_user_role_assignments (
                                            tenant_id,
                                            user_id,
                                            status
        );


CREATE INDEX idx_auth_user_role_assignment_role
    ON authorization_user_role_assignments (
                                            tenant_id,
                                            role_id,
                                            status
        );


CREATE INDEX idx_auth_user_role_assignment_scope
    ON authorization_user_role_assignments (
                                            tenant_id,
                                            scope_type,
                                            scope_key,
                                            status
        );


CREATE INDEX idx_auth_user_role_assignment_validity
    ON authorization_user_role_assignments (
                                            tenant_id,
                                            valid_from,
                                            valid_until
        );