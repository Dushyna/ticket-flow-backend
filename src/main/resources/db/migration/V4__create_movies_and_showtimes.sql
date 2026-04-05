CREATE TABLE movies (
                        id UUID PRIMARY KEY,
                        title VARCHAR(255) NOT NULL,
                        description TEXT,
                        duration_minutes INTEGER NOT NULL,
                        poster_url TEXT,
                        release_date DATE,
                        created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                        updated_at TIMESTAMP WITH TIME ZONE
);
CREATE TABLE showtimes (
                           id UUID PRIMARY KEY,
                           movie_id UUID NOT NULL,
                           hall_id UUID NOT NULL,
                           start_time TIMESTAMP WITH TIME ZONE NOT NULL,
                           end_time TIMESTAMP WITH TIME ZONE NOT NULL,
                           base_price DECIMAL(10, 2) NOT NULL,
                           created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                           updated_at TIMESTAMP WITH TIME ZONE,
                           CONSTRAINT fk_showtime_movie FOREIGN KEY (movie_id) REFERENCES movies(id) ON DELETE CASCADE,
                           CONSTRAINT fk_showtime_hall FOREIGN KEY (hall_id) REFERENCES movie_halls(id) ON DELETE CASCADE
);
CREATE INDEX idx_showtimes_movie_id ON showtimes(movie_id);
CREATE INDEX idx_showtimes_hall_id ON showtimes(hall_id);
CREATE INDEX idx_showtimes_start_time ON showtimes(start_time);

