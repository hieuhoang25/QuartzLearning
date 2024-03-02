package com.hicode.quartzjob.controller;

import com.hicode.quartzjob.payload.EmailRequest;
import com.hicode.quartzjob.payload.EmailResponse;
import com.hicode.quartzjob.quartz.EmailJob;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZonedDateTime;
import java.util.Date;
import java.util.UUID;

@Slf4j
@RestController
public class EmailSchedulerController {

    @Autowired
    private Scheduler scheduler;

    @PostMapping("/schedule/email")
    public ResponseEntity<EmailResponse> scheduleEmail(@RequestBody EmailRequest emailRequest) {
        try {
            ZonedDateTime dateTime = ZonedDateTime.of(emailRequest.getDateTime(), emailRequest.getTimeZone());
            if (dateTime.isBefore(ZonedDateTime.now())) {
                EmailResponse emailResponse = new EmailResponse(false, "dateTime  must be after current time.");
                return ResponseEntity.internalServerError().body(emailResponse);
            }
            JobDetail jobDetail = buildJobDetail(emailRequest);
            Trigger trigger = buildTrigger(jobDetail, dateTime);

            scheduler.scheduleJob(jobDetail, trigger);

            EmailResponse emailResponse = new EmailResponse(true, jobDetail.getKey().getName(), jobDetail.getKey().getGroup(), "Email scheduled successfully");
            return ResponseEntity.ok(emailResponse);
        } catch (SchedulerException se) {
            log.error("Error while scheling email", se);
            return ResponseEntity.badRequest().body(new EmailResponse(false, "Error"));
        }

    }

    @GetMapping("/get/test")
    public ResponseEntity<String> getApiTest(){
        log.info("dateTimeNow {}",  ZonedDateTime.now());
        log.info("timeZone {}",  ZonedDateTime.now().getZone());
        return ResponseEntity.ok("Get API Test-Pass");
    }


    private JobDetail buildJobDetail(EmailRequest emailRequest){
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("email", emailRequest.getEmail());
        jobDataMap.put("subject", emailRequest.getSubject());
        jobDataMap.put("body", emailRequest.getBody());
        return JobBuilder.newJob(EmailJob.class)
                .withIdentity(UUID.randomUUID().toString(), "email-jobs")
                .withDescription("Send email job")
                .usingJobData(jobDataMap)
                .storeDurably().build();
    }

    private Trigger buildTrigger(JobDetail jobDetail, ZonedDateTime startAt){
        return TriggerBuilder.newTrigger()
                .forJob(jobDetail)
                .withIdentity(jobDetail.getKey().getName(), "email-triggers")
                .withDescription("Send email trigger")
                .startAt(Date.from(startAt.toInstant()))
                .withSchedule(SimpleScheduleBuilder.simpleSchedule().withMisfireHandlingInstructionFireNow())
                .build();
    }
}
