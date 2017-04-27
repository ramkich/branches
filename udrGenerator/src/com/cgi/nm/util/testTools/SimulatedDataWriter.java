package com.cgi.nm.util.testTools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPOutputStream;

/**
 * @author ramakrishna.chekuri
 *
 */
public class SimulatedDataWriter {

	private static final int MAX_THREAD_POOL = 5;

	private static String fileNameformat = "CAR_usage.{0,date,yyyyMMdd}-{1}.log.{2}.gz";

	private final static String TRANSIENT_SUFFIX = ".transient";

	static List<String> outputFileNameList = new ArrayList<>();

	private Object[] filenameArgs = new Object[3];

	List<GZIPOutputStream> outputStreamsList = new ArrayList<GZIPOutputStream>();

	private Random rand = new Random();

	private SimulatedDataManagerMain dataManager = new SimulatedDataManagerMain();

	/**
	 * @param outputFilesDirArray
	 * @param outputFilesCount
	 * @return
	 */
	public List<GZIPOutputStream> createStreamWriters(final String[] outputFilesDirArray, final int outputFilesCount,
			final String radiusCARFilesDirectory) {

		Arrays.asList(outputFilesDirArray).stream().forEach(directory -> {
			for (int counter = 1; counter <= outputFilesCount; counter++) {
				outputStreamsList.add(getGZIPOutputStream(
						getOutputFileName(new Date(), counter, directory, radiusCARFilesDirectory)));
			}
		});

		return outputStreamsList;
	}

	/**
	 * @param recordTimestamp
	 * @param acctTosToGenerate
	 * @param outputDirectopriesList
	 * @param outputStreamsList
	 */
	public void writeRadiusRecordTemplatesToFile(final long recordTimestamp, final String[] acctTosToGenerate,
			final String[] outputDirectopriesList, final List<GZIPOutputStream> outputStreamsList) {

		ExecutorService executor = Executors.newFixedThreadPool(MAX_THREAD_POOL);

		Arrays.asList(acctTosToGenerate).parallelStream().forEach(arg -> {

			String tos = arg.split("=")[0];
			int qty = Integer.parseInt(arg.split("=")[1]);

			AcctTos acctTos = dataManager.getAcctTos(tos);
			acctTos.setCountOfAccountsToBeCreated(qty);

			executor.submit(() -> {

				String userName;

				while ((userName = acctTos.generateUsername()) != null) {
					writeDataToFile(userName, acctTos.getBaseIpAddress(), recordTimestamp, acctTos.getTemplate(),
							outputStreamsList);
				}

			});
		});

		try {
			executor.shutdown();
			executor.awaitTermination(30, TimeUnit.MINUTES);
		} catch (InterruptedException e) {
			System.err.println(
					String.format("Shutdown tasks interrupted - %s: %s", e.getClass().getSimpleName(), e.getMessage()));
		} finally {
			if (!executor.isTerminated()) {
				System.err.println("Forcing shutdown");
				executor.shutdownNow();
				System.err.println("Shutdown completed");
			}
		}

	}

	/**
	 * @param username
	 * @param baseIpAddress
	 * @param baseTs
	 * @param template
	 * @param outputStreamsList
	 */
	private void writeDataToFile(final String username, final String baseIpAddress, final long baseTs,
			final String template, final List<GZIPOutputStream> outputStreamsList) {

		String acctStatusType;

		long ts;
		if (rand.nextInt(100) >= 20) {
			acctStatusType = "Interim-Update";
			ts = baseTs + 3600000L - Math.abs(rand.nextLong() % 3000L);
		} else {
			acctStatusType = "Stop";
			ts = baseTs + Math.abs(rand.nextLong() % 3600000L);
		}

		try {
			outputStreamsList.get(rand.nextInt(outputStreamsList.size())).write(MessageFormat.format(
							template,
							new Date(ts),
							username,
							String.format("%s.%s.%s.%s", baseIpAddress, Math.abs(username.hashCode()) % 64 + 1, Math.abs(username.hashCode()) % 64 + 65, Math.abs(username.hashCode()) % 64 + 129),
					        acctStatusType, 
					        String.valueOf(convertDateToEpochTime(new Date(ts)) / 2L),
					        String.valueOf(convertDateToEpochTime(new Date(ts))),
					        Integer.toHexString(username.hashCode()).toUpperCase(),
					        String.valueOf(convertDateToEpochTime(new Date(ts)))).getBytes());
		} catch (IOException e) {
			throw new RuntimeException(String.format("%s: %s", e.getClass().getSimpleName(), e.getMessage()));
		}

	}

	/**
	 * Generates a new output stream
	 *
	 * @param fileName
	 * @return
	 * @throws IOException
	 */
	public GZIPOutputStream getGZIPOutputStream(final String fileName) {

		try {
			GZIPOutputStream outputStream = new GZIPOutputStream(
					new FileOutputStream(new File(fileName + TRANSIENT_SUFFIX)));
			return outputStream;
		} catch (IOException e) {
			throw new RuntimeException(String.format("%s: %s", e.getClass().getSimpleName(), e.getMessage()));
		}

	}

	/**
	 * @param pDate
	 * @param pDirectory
	 * @return
	 */
	public String getOutputFileName(final Date pDate, final int outputFilesCount, final String pDirectory,
			final String radiusCARFilesDirectory) {

		String fileName = null;

		fileName = radiusCARFilesDirectory + File.separator + pDirectory + File.separator
				+ getFilename(pDate, outputFilesCount, pDirectory);

		outputFileNameList.add(fileName);

		return fileName;
	}

	/**
	 * @param pDate
	 * @param pDirectory
	 * @return
	 */
	public String getFilename(final Date pDate, final int outputFilesCount, final String pDirectory) {

		filenameArgs[0] = pDate;
		filenameArgs[1] = outputFilesCount;
		filenameArgs[2] = pDirectory;

		MessageFormat filenameFormat = new MessageFormat(fileNameformat);

		return filenameFormat.format(filenameArgs);
	}

	public List<String> getOutputFileNamesList() {

		return outputFileNameList;
	}

	/**
	 * Closes the current report.
	 *
	 * @throws java.io.IOException
	 *             if there is an IO problem
	 */
	public void closeReport(final List<GZIPOutputStream> outputStreamsList, final List<String> outputFileNameList)
			throws IOException {

		outputStreamsList.forEach(outputStream -> {

			if (outputStream != null) {
				try {
					outputStream.flush();
					outputStream.close();
				} catch (Exception e) {
					e.printStackTrace();
				}

			}
		});

		outputFileNameList.forEach(fileName -> {

			File lFinalFile = new File(fileName);

			File transientFileName = new File(fileName + TRANSIENT_SUFFIX);

			transientFileName.renameTo(lFinalFile);

		});

	}

	public long convertDateToEpochTime(final Date recordDate) {

		long epochTimestampNow = 0L;

		try {

			SimpleDateFormat estFormat = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss.SSS");
			String dateHeaderNow = estFormat.format(recordDate);

			estFormat.setTimeZone(TimeZone.getTimeZone("America/Montreal"));
			Date headerAsDate = estFormat.parse(dateHeaderNow);

			epochTimestampNow = headerAsDate.getTime() / 1000L;

		} catch (ParseException e) {

		}

		return epochTimestampNow;
	}

}
