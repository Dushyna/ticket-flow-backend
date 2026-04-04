CREATE TABLE bookings (
                          id UUID PRIMARY KEY,
                          user_id UUID NOT NULL,
                          hall_id UUID NOT NULL,
                          row_index INTEGER NOT NULL,
                          col_index INTEGER NOT NULL,
                          status VARCHAR(50) NOT NULL,
                          created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                          updated_at TIMESTAMP WITH TIME ZONE,
                          CONSTRAINT fk_booking_user FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE,
                          CONSTRAINT fk_booking_hall FOREIGN KEY (hall_id) REFERENCES movie_halls(id) ON DELETE CASCADE
);

CREATE INDEX idx_bookings_hall_id ON bookings(hall_id);
CREATE INDEX idx_bookings_user_id ON bookings(user_id);

CREATE UNIQUE INDEX idx_unique_seat_booking ON bookings(hall_id, row_index, col_index)
    WHERE status IN ('PENDING', 'CONFIRMED');
