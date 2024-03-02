# Introduction to Quartz
## 1. Overview
[Quartz](http://www.quartz-scheduler.org/) is an open-source job-scheduling framework written entirely in Java and designed for use in both J2SE and J2EE applications. It offers excellent flexibility without sacrificing simplicity.
You can create complex schedules for executing any job. Examples are e.g. tasks that run daily, every other Friday at 7:30 p.m. or only on the last day of every month.
- Link get SQL to create table quartz: [quartz/src/main/resources/org/quartz/impl/jdbcjobstore](https://github.com/quartz-scheduler/quartz)
## 2. The Quartz API
The heart of the framework is the Scheduler. It is responsible for managing the runtime environment for our application.
To ensure scalability, Quartz is based on a multi-threaded architecture. When started, the framework initializes a set of worker threads that the Scheduler uses to execute Jobs.
<br>
This is how the framework can run many Jobs concurrently. It also relies on a loosely coupled set of ThreadPool management components for managing the thread environment.
<br>
The key interfaces of the API are:
- Scheduler – the primary API for interacting with the scheduler of the framework
- Job – an interface to be implemented by components that we wish to have executed
- JobDetail – used to define instances of Jobs
- Trigger – a component that determines the schedule upon which a given Job will be performed
- JobBuilder – used to build JobDetail instances, which define instances of Jobs
- TriggerBuilder – used to build Trigger instances
### 3. Scheduler
Before we can use the Scheduler, it needs to be instantiated. To do this, we can use the factory SchedulerFactory:
```java
SchedulerFactory schedulerFactory = new StdSchedulerFactory();
Scheduler scheduler = schedulerFactory.getScheduler();
```
A Scheduler’s life-cycle is bounded by its creation, via a SchedulerFactory and a call to its shutdown() method. Once created the Scheduler interface can be used to add, remove, and list Jobs and Triggers, and perform other scheduling-related operations (such as pausing a trigger).
<br>
However, ***the Scheduler will not act on any triggers until it has been started with the start() method***:
```java
scheduler.start();
```
### 4. Jobs
A Job is a class that implements the Job interface. It has only one simple method:
```java
public class SimpleJob implements Job {
    public void execute(JobExecutionContext arg0) throws JobExecutionException {
        System.out.println("This is a quartz job!");
    }
}
````
When the Job’s trigger fires, the execute() method gets invoked by one of the scheduler’s worker threads.
<br>
The JobExecutionContext object that is passed to this method provides the job instance, with information about its runtime environment, a handle to the Scheduler that executed it, a handle to the Trigger that triggered the execution, the job’s JobDetail object, and a few other items.
<br>
The JobDetail object is created by the Quartz client at the time the Job is added to the Scheduler. It is essentially the definition of the job instance:
```java
JobDetail job = JobBuilder.newJob(SimpleJob.class)
  .withIdentity("myJob", "group1")
  .build();
```
This object may also contain various property settings for the Job, as well as a JobDataMap, which can be used to store state information for a given instance of our job class.
### 4.1 JobDataMap
The JobDataMap is used to hold any amount of data objects that we wish to make available to the job instance when it executes. JobDataMap is an implementation of the Java Map interface and has some added convenience methods for storing and retrieving data of primitive types.
<br>
Here’s an example of putting data into the JobDataMap while building the JobDetail, before adding the job to the scheduler:
```java
JobDetail job = newJob(SimpleJob.class)
  .withIdentity("myJob", "group1")
  .usingJobData("jobSays", "Hello World!")
  .usingJobData("myFloatValue", 3.141f)
  .build();
```
And here is an example of how to access these data during the job's execution:
```java
public class SimpleJob implements Job { 
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap dataMap = context.getJobDetail().getJobDataMap();

        String jobSays = dataMap.getString("jobSays");
        float myFloatValue = dataMap.getFloat("myFloatValue");

        System.out.println("Job says: " + jobSays + ", and val is: " + myFloatValue);
    } 
}
```
The above example will print “Job says Hello World!, and val is 3.141”.
<br>
We can also add setter methods to our job class that corresponds to the names of keys in the JobDataMap.
<bR>
If we do this, Quartz’s default JobFactory implementation automatically calls those setters when the job is instantiated, thus preventing the need to explicitly get the values out of the map within our execute method.
### 5. Triggers
Trigger objects are used to trigger the execution of Jobs.
<br>
When we wish to schedule a Job, we need to instantiate a trigger and adjust its properties to configure our scheduling requirements:
```java
Trigger trigger = TriggerBuilder.newTrigger()
  .withIdentity("myTrigger", "group1")
  .startNow()
  .withSchedule(SimpleScheduleBuilder.simpleSchedule()
    .withIntervalInSeconds(40)
    .repeatForever())
  .build();
```
A Trigger may also have a JobDataMap associated with it. This is useful for passing parameters to a Job that are specific to the executions of the trigger.
<br>
There are different types of triggers for different scheduling needs. Each one has different TriggerKey properties for tracking their identities. However, some other properties are common to all trigger types:
- The jobKey property indicates the identity of the job that should be executed when the trigger fires.
- The startTime property indicates when the trigger’s schedule first comes into effect. The value is a java.util.Date object that defines a moment in time for a given calendar date. For some trigger types, the trigger fires at the given start time. For others, it simply marks the time that the schedule should start.
- The endTime property indicates when the trigger’s schedule should be canceled.
<br>
Quartz ships with a handful of different trigger types, but the most commonly used ones are SimpleTrigger and CronTrigger.
### 5.1 Priority
Sometimes, when we have many triggers, Quartz may not have enough resources to immediately fire all of the jobs are scheduled to fire at the same time. In this case, we may want to control which of our triggers gets available first. This is exactly what the priority property on a trigger is used for.
<br>
For example, when ten triggers are set to fire at the same time and merely four worker threads are available, the first four triggers with the highest priority will be executed first. When we do not set a priority on a trigger, it uses a default priority of five. Any integer value is allowed as a priority, positive or negative.
In the example below, we have two triggers with a different priority. If there aren't enough resources to fire all the triggers at the same time, triggerA will be the first one to be fired:
```java
Trigger triggerA = TriggerBuilder.newTrigger()
  .withIdentity("triggerA", "group1")
  .startNow()
  .withPriority(15)
  .withSchedule(SimpleScheduleBuilder.simpleSchedule()
    .withIntervalInSeconds(40)
    .repeatForever())
  .build();
            
Trigger triggerB = TriggerBuilder.newTrigger()
  .withIdentity("triggerB", "group1")
  .startNow()
  .withPriority(10)
  .withSchedule(SimpleScheduleBuilder.simpleSchedule()
    .withIntervalInSeconds(20)
    .repeatForever())
  .build();
```
### 5.2 Misfire Instructions
***A misfire occurs if a persistent trigger misses its firing time because of the Scheduler being shut down, or in case there are no available threads in Quartz’s thread pool.***
The different trigger types have different misfire instructions available. By default, they use a smart policy instruction. When the scheduler starts, it searches for any persistent triggers that have misfired. After that, it updates each of them based on their individually configured misfire instructions.
<br>
Let's take a look at the examples below:
```java
Trigger misFiredTriggerA = TriggerBuilder.newTrigger()
  .startAt(DateUtils.addSeconds(new Date(), -10))
  .build();
            
Trigger misFiredTriggerB = TriggerBuilder.newTrigger()
  .startAt(DateUtils.addSeconds(new Date(), -10))
  .withSchedule(SimpleScheduleBuilder.simpleSchedule()
    .withMisfireHandlingInstructionFireNow())
  .build();
```
We have scheduled the trigger to run 10 seconds ago (so it is 10 seconds late by the time it is created) to simulate a misfire, e.g. because the scheduler was down or didn't have a sufficient amount of worker threads available. Of course, in a real-world scenario, we would never schedule triggers like this.
<br>
In the first trigger (misFiredTriggerA) no misfire handling instructions are set. Hence a called smart policy is used in that case and is called: withMisfireHandlingInstructionFireNow(). This means that the job is executed immediately after the scheduler discovers the misfire.
<br>
The second trigger explicitly defines what kind of behavior we expect when misfiring occurs. In this example, it just happens to be the same smart policy.
### 5.3 SimpleTrigger
SimpleTrigger is used for scenarios in which we need to execute a job at a specific moment in time. This can either be exactly once or repeatedly at specific intervals.
<br>
An example could be to fire a job execution at exactly 12:20:00 AM on January 13, 2018. Similarly, we can start at that time, and then five more times, every ten seconds.
<br>
In the code below, the date myStartTime has previously been defined and is used to build a trigger for one particular timestamp:
```java
SimpleTrigger trigger = (SimpleTrigger) TriggerBuilder.newTrigger()
  .withIdentity("trigger1", "group1")
  .startAt(myStartTime)
  .forJob("job1", "group1")
  .build();
```
Next, let's build a trigger for a specific moment in time, then repeating every ten seconds ten times:
```java
SimpleTrigger trigger = (SimpleTrigger) TriggerBuilder.newTrigger()
  .withIdentity("trigger2", "group1")
  .startAt(myStartTime)
  .withSchedule(simpleSchedule()
    .withIntervalInSeconds(10)
    .withRepeatCount(10))
  .forJob("job1") 
  .build();
```
### 5.4 CronTrigger
***The CronTrigger is used when we need schedules based on calendar-like statements.*** For example, we can specify firing-schedules such as every Friday at noon or every weekday at 9:30 am.
Cron-Expressions are used to configure instances of CronTrigger. These expressions consist of Strings that are made up of seven sub-expressions. We can read more about Cron-Expressions [here](https://docs.oracle.com/cd/E12058_01/doc/doc.1014/e12030/cron_expressions.htm).
In the example below, we build a trigger that fires every other minute between 8 am and 5 pm, every day:
```java
CronTrigger trigger = TriggerBuilder.newTrigger()
  .withIdentity("trigger3", "group1")
  .withSchedule(CronScheduleBuilder.cronSchedule("0 0/2 8-17 * * ?"))
  .forJob("myJob", "group1")
  .build();
```
we have shown how to build a Scheduler to trigger a Job. We also saw some of the most common trigger options used: SimpleTrigger and CronTrigger.
<br>
Quartz can be used to create simple or complex schedules for executing dozens, hundreds, or even more jobs. 




