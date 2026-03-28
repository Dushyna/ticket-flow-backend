CREATE TABLE cinemas (
                         id UUID PRIMARY KEY,
                         name VARCHAR(255) NOT NULL,
                         address TEXT,
                         organization_id UUID NOT NULL,
                         created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                         updated_at TIMESTAMP WITH TIME ZONE,
                         CONSTRAINT fk_cinema_organization FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE
);

CREATE TABLE movie_halls (
                             id UUID PRIMARY KEY,
                             name VARCHAR(255) NOT NULL,
                             cinema_id UUID NOT NULL,
                             rows_count INTEGER NOT NULL,
                             cols_count INTEGER NOT NULL,
                             layout_config JSONB NOT NULL,
                             created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                             updated_at TIMESTAMP WITH TIME ZONE,
                             CONSTRAINT fk_hall_cinema FOREIGN KEY (cinema_id) REFERENCES cinemas(id) ON DELETE CASCADE
);

CREATE INDEX idx_halls_cinema_id ON movie_halls(cinema_id);
CREATE INDEX idx_cinemas_org_id ON cinemas(organization_id);
