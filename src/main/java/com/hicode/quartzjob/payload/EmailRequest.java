package com.hicode.quartzjob.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class EmailRequest {
  private String email;
  private String subject;
  private String body;
  private LocalDateTime dateTime;
  private ZoneId timeZone;
}
