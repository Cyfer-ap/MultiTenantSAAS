CREATE TABLE user_invitations (
                                  id UUID NOT NULL,
                                  tenant_id UUID NOT NULL,
                                  invited_by_user_id UUID,
                                  invited_by_system_admin_id UUID,

                                  full_name VARCHAR(150) NOT NULL,
                                  email VARCHAR(150) NOT NULL,
                                  role VARCHAR(30) NOT NULL,

                                  token_hash VARCHAR(64) NOT NULL,
                                  status VARCHAR(30) NOT NULL,

                                  expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                  accepted_at TIMESTAMP WITH TIME ZONE,
                                  revoked_at TIMESTAMP WITH TIME ZONE,

                                  CONSTRAINT pk_user_invitations PRIMARY KEY (id),
                                  CONSTRAINT uk_user_invitation_token_hash UNIQUE (token_hash),

                                  CONSTRAINT fk_user_invitation_tenant
                                      FOREIGN KEY (tenant_id)
                                          REFERENCES tenants (id),

                                  CONSTRAINT fk_user_invitation_actor_user
                                      FOREIGN KEY (invited_by_user_id)
                                          REFERENCES app_users (id),

                                  CONSTRAINT fk_user_invitation_actor_system_admin
                                      FOREIGN KEY (invited_by_system_admin_id)
                                          REFERENCES system_admins (id)
);

CREATE INDEX idx_user_invitation_tenant
    ON user_invitations (tenant_id);

CREATE INDEX idx_user_invitation_email
    ON user_invitations (tenant_id, email);

CREATE INDEX idx_user_invitation_status
    ON user_invitations (status);

CREATE INDEX idx_user_invitation_expires_at
    ON user_invitations (expires_at);