CREATE TABLE user_organization_assignments (
                                               id UUID NOT NULL,
                                               tenant_id UUID NOT NULL,
                                               user_id UUID NOT NULL,
                                               organizational_unit_id UUID NOT NULL,
                                               reports_to_assignment_id UUID,
                                               position_title VARCHAR(150),
                                               primary_assignment BOOLEAN NOT NULL,
                                               status VARCHAR(30) NOT NULL,
                                               valid_from TIMESTAMP WITH TIME ZONE NOT NULL,
                                               valid_until TIMESTAMP WITH TIME ZONE,
                                               created_by_user_id UUID NOT NULL,
                                               created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                               updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

                                               CONSTRAINT pk_user_organization_assignments
                                                   PRIMARY KEY (id),

                                               CONSTRAINT uk_user_org_assignment_tenant_id
                                                   UNIQUE (tenant_id, id),

                                               CONSTRAINT ck_user_org_assignment_validity
                                                   CHECK (
                                                       valid_until IS NULL
                                                           OR valid_until > valid_from
                                                       ),

                                               CONSTRAINT ck_user_org_assignment_not_self_reporting
                                                   CHECK (
                                                       reports_to_assignment_id IS NULL
                                                           OR reports_to_assignment_id <> id
                                                       )
);


ALTER TABLE user_organization_assignments
    ADD CONSTRAINT fk_user_org_assignment_tenant
        FOREIGN KEY (tenant_id)
            REFERENCES tenants (id);


ALTER TABLE user_organization_assignments
    ADD CONSTRAINT fk_user_org_assignment_user
        FOREIGN KEY (user_id)
            REFERENCES app_users (id);


ALTER TABLE user_organization_assignments
    ADD CONSTRAINT fk_user_org_assignment_unit
        FOREIGN KEY (organizational_unit_id)
            REFERENCES organizational_units (id);


ALTER TABLE user_organization_assignments
    ADD CONSTRAINT fk_user_org_assignment_created_by
        FOREIGN KEY (created_by_user_id)
            REFERENCES app_users (id);


ALTER TABLE user_organization_assignments
    ADD CONSTRAINT fk_user_org_assignment_reports_to
        FOREIGN KEY (reports_to_assignment_id)
            REFERENCES user_organization_assignments (id);


CREATE INDEX idx_user_org_assignment_user
    ON user_organization_assignments (
                                      tenant_id,
                                      user_id,
                                      status
        );


CREATE INDEX idx_user_org_assignment_unit
    ON user_organization_assignments (
                                      tenant_id,
                                      organizational_unit_id,
                                      status
        );


CREATE INDEX idx_user_org_assignment_primary
    ON user_organization_assignments (
                                      tenant_id,
                                      user_id,
                                      primary_assignment,
                                      status
        );


CREATE INDEX idx_user_org_assignment_reports_to
    ON user_organization_assignments (
                                      tenant_id,
                                      reports_to_assignment_id
        );


CREATE INDEX idx_user_org_assignment_validity
    ON user_organization_assignments (
                                      tenant_id,
                                      valid_from,
                                      valid_until
        );