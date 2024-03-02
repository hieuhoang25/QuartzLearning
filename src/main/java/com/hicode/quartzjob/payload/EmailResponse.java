package com.hicode.quartzjob.payload;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class EmailResponse {
    private boolean success;
    private String jobId;
    private String jobGroup;
    private String message;

    public EmailResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
}
