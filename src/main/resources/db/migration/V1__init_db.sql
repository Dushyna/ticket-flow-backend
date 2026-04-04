CREATE TABLE organizations (
                               id UUID PRIMARY KEY,
                               name VARCHAR(255) NOT NULL UNIQUE,
                               slug VARCHAR(255) NOT NULL UNIQUE,
                               contact_email VARCHAR(255),
                               created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                               updated_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE app_user (
                          id UUID PRIMARY KEY,
                          email VARCHAR(255) NOT NULL UNIQUE,
                          password VARCHAR(255),
                          first_name VARCHAR(100),
                          last_name VARCHAR(100),
                          birth_date DATE,
                          phone VARCHAR(50),
                          confirm_status VARCHAR(50) DEFAULT 'UNCONFIRMED',
                          role VARCHAR(50) NOT NULL,
                          provider VARCHAR(50) DEFAULT 'LOCAL',
                          provider_id VARCHAR(255),
                          organization_id UUID,
                          CONSTRAINT fk_user_organization FOREIGN KEY (organization_id) REFERENCES organizations(id)
);

CREATE TABLE confirm_code (
                              id UUID PRIMARY KEY,
                              expired TIMESTAMP WITH TIME ZONE NOT NULL,
                              user_id UUID NOT NULL UNIQUE,
                              CONSTRAINT fk_confirm_user FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE
);

CREATE TABLE password_reset_tokens (
                                       id UUID PRIMARY KEY,
                                       token VARCHAR(255) NOT NULL UNIQUE,
                                       user_id UUID NOT NULL,
                                       expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                       used BOOLEAN NOT NULL DEFAULT FALSE,
                                       CONSTRAINT fk_password_reset_user FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE
);

CREATE INDEX idx_user_provider ON app_user(provider, provider_id);
CREATE INDEX idx_password_reset_token_lookup ON password_reset_tokens(token);
