package com.cgi.nm.radius.poc;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.PersistJobDataAfterExecution;
import org.quartz.Scheduler;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;

import com.cgi.network.sa.util.FileFinder;

public class QuartzPoc {

    private static final int NUMBER_OF_JOBS_TO_SCHEDULE = 20;
    private static final Random RAND = new Random();
    private static final List<Integer> running = Collections.synchronizedList(new ArrayList<>());

    @DisallowConcurrentExecution
    @PersistJobDataAfterExecution
    public static class HelloJob implements Job {

        @Override
        public void execute(final JobExecutionContext context) throws JobExecutionException {

            int jobNumber = context.getJobDetail().getJobDataMap().getInt("jobNumber");

            long sleepTime = 500L + RAND.nextInt(500);

            if (context.getJobDetail().getKey().toString().equals("group1.job 5")) {
                sleepTime = 4000;
            }

            running.add(jobNumber);
            System.out.printf("%tT.%<tL - %s%n", System.currentTimeMillis(), ident(String.format(
                    "Started   job %s in %,.1f secs.",
                    context.getJobDetail().getKey(),
                    sleepTime / 1000f), jobNumber, true, running.toArray(new Integer[0])));

            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                throw new JobExecutionException(e);
            }

            running.remove(new Integer(jobNumber));
            System.out.printf("%tT.%<tL - %s%n", System.currentTimeMillis(), ident(String.format(
                    "Completed job %s in %,.1f secs.",
                    context.getJobDetail().getKey(),
                    sleepTime / 1000f), jobNumber, false, running.toArray(new Integer[0])));

        }

    }
    
    @PersistJobDataAfterExecution
	public static class MyJob implements Job {

		@Override
		public void execute(final JobExecutionContext context) throws JobExecutionException {
			long sleepTime = 600L + RAND.nextInt(500);;
			System.out.printf("Started   job %s in %,.1f secs.%n",
                    context.getJobDetail().getKey(),
                    sleepTime / 1000f);
			try {
				Thread.sleep(sleepTime);
			} catch (InterruptedException e) {
				throw new JobExecutionException(e);
			}
		}

	}

    public static void main(final String[] args) throws Exception {

        possibleSolution();
    }

    private static void possibleSolution() throws Exception {

        // Grab the Scheduler instance from the Factory
        Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
        InputStream stream = FileFinder.findFileInClassPath("radiusQuartz.properties");
        Properties quartzProperties = new Properties();
        quartzProperties.load(stream);
        StdSchedulerFactory schedulerFactory = new StdSchedulerFactory(quartzProperties);
        Scheduler scheduler1 = schedulerFactory.getScheduler();

        try {

            AtomicInteger countOfJobsCreated = new AtomicInteger();
            AtomicInteger countOfJobsScheduled = new AtomicInteger(NUMBER_OF_JOBS_TO_SCHEDULE);

            Stream.generate(() -> {
                int jobNumber = countOfJobsCreated.incrementAndGet();
                return JobBuilder.newJob(HelloJob.class).withIdentity("job " + jobNumber, "group1").usingJobData("jobNumber", jobNumber).build();
            })
                    .limit(NUMBER_OF_JOBS_TO_SCHEDULE)
                    .forEach(job -> {
                        try {
                            scheduler.scheduleJob(job, TriggerBuilder.newTrigger()
                                    .withIdentity("trigger_for_" + job, "group1")
                                    .withPriority(countOfJobsScheduled.getAndDecrement())
                                    .build());
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    });

            AtomicInteger JOB_ID = new AtomicInteger();
            Stream.generate(() -> JobBuilder.newJob(MyJob.class)
                    .withIdentity("job" + JOB_ID.incrementAndGet(), "group2")
                    .build()).limit(30).forEach(job -> {
                        try {
                            scheduler1.scheduleJob(job, TriggerBuilder.newTrigger()
                                    .withIdentity("triggering_for_" + job, "group2")
                                    .startNow()
                                    .build());
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    });

            scheduler.start();
            
            scheduler1.start();

			System.out.println("Scheduler has been started, now sleeping...");

			Thread.sleep(30000);

			System.out.println("Shutting down.");
            
            Thread.sleep(20000);

            System.out.println("Shutting down 1.");

        } finally {

            scheduler.shutdown();
            scheduler1.shutdown();
        }
    }

    
    private static String ident(String s, int jobNo, boolean start, Integer[] r) {

        List<Integer> running = Arrays.asList(r);

        StringBuilder sb = new StringBuilder();

        if (start) {
            sb.append(String.format("#%-3d ", jobNo));
        } else {
            sb.append("     ");
        }

        for (int num = 0; num < jobNo; num++) {
            if (running.contains(new Integer(num))) {
                sb.append(" |  ");
            } else {
                sb.append("    ");
            }
        }

        if (start) {
            sb.append(" .--");
        } else {
            sb.append(" `--");
        }

        sb.append(s);

        return sb.toString();
    }
}
