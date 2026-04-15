package io.github.dushyna.ticketflow.booking.utils;

import io.github.dushyna.ticketflow.booking.dto.response.BookingResponseDto;
import io.github.dushyna.ticketflow.booking.entity.Booking;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;


@Mapper(componentModel = "spring")
public interface BookingMapper {

    @Mapping(target = "movieTitle", source = "showtime.movie.title")
    @Mapping(target = "hallName", source = "hall.name")
    @Mapping(target = "startTime", source = "showtime.startTime")
    @Mapping(target = "ticketLabel", source = "ticketType.label")
    @Mapping(target = "seat.row", source = "rowIndex")
    @Mapping(target = "seat.col", source = "colIndex")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "price", source = "finalPrice")
    BookingResponseDto toResponseDto(Booking booking);

}
