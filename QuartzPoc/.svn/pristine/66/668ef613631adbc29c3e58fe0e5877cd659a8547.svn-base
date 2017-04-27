package com.cgi.nm.radius.poc;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.PersistJobDataAfterExecution;
import org.quartz.Scheduler;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;

import com.cgi.network.sa.util.FileFinder;

public class QuartzPoolPoc {

	@PersistJobDataAfterExecution
	public static class HelloJob implements Job {

		@Override
		public void execute(final JobExecutionContext context) throws JobExecutionException {
			System.out.printf("Job %s%n", context.getJobDetail().getKey());
			long sleepTime = 3000;
			 if (context.getJobDetail().getKey().toString().equals("group2.job 6")) {
				 sleepTime = 10000;
	            }

			try {
				Thread.sleep(sleepTime);
			} catch (InterruptedException e) {
				throw new JobExecutionException(e);
			}
		}

	}

	public static void main(final String[] args) throws Exception {

		System.out.println("Calling second Scheduler");
		secondScheduler();
	}

	private static void secondScheduler() throws Exception {
		
		InputStream stream = FileFinder.findFileInClassPath("radiusQuartz.properties");
        Properties quartzProperties = new Properties();
        quartzProperties.load(stream);
        StdSchedulerFactory schedulerFactory = new StdSchedulerFactory(quartzProperties);
        Scheduler scheduler = schedulerFactory.getScheduler();
		try {
			List<JobDetail> jobs = new ArrayList<>();

            for (int id = 1; id <= 30; id++) {

                JobDetail job = JobBuilder.newJob(HelloJob.class)
                        .withIdentity("job_in_order " + id, "group2")
                        .build();

                job.getJobDataMap().put("name", String.format("user %d", id));

                jobs.add(job);

            }

            for (JobDetail job : jobs) {

                System.out.printf("Scheduling %s%n", job);

                scheduler.scheduleJob(job, TriggerBuilder.newTrigger()
                        .withIdentity("trigger_for_" + job, "group2")
                        .startNow()
                        .build());
            }

			scheduler.start();

			System.out.println("Scheduler has been started, now sleeping...");

			Thread.sleep(20000);

			System.out.println("Shutting down 1.");
		} finally {
			scheduler.shutdown();
		}

	}

}
