package com.cgi.nm.util.testTools;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

/**
 * @author ramakrishna.chekuri
 *
 */
@SuppressWarnings("static-access")
public class SimulatedDataManagerMain {

	private static final String SPLITTER = "\\s*,\\s*";

	private static Option OPTION_ACCT_TOS = OptionBuilder.withArgName("acctTos").hasArg().isRequired(true)
			.withDescription("acctTos").create("acctTos");

	private static Option OPTION_RECORD_TIMESTAMP = OptionBuilder.withArgName("recordTimestamp").hasArg()
			.isRequired(true).withDescription("recordTimestamp").create("recordTimestamp");

	private String recordTimestamp;

	private String acctTos;

	private Properties arguments;

	private String recordsContextPath = "D:/Project/RadiusRecordsSimulator/etc/radiusRecordTemplate.xml";

	private int outputFilesCount = 0;

	private String directories = null;

	private String radiusFilesDirectoryPath = null;

	private volatile ApplicationContext RECORDS_SIMULATOR_CONTEXT;

	public SimulatedDataManagerMain() {

		if (RECORDS_SIMULATOR_CONTEXT == null) {
			RECORDS_SIMULATOR_CONTEXT = new FileSystemXmlApplicationContext(recordsContextPath);
		}
	}

	/**
	 * @return the recordTimestamp
	 */
	public String getRecordTimestamp() {
		return recordTimestamp;
	}

	/**
	 * @param recordTimestamp
	 *            the recordTimestamp to set
	 */
	public void setRecordTimestamp(final String recordTimestamp) {
		this.recordTimestamp = recordTimestamp;
	}

	/**
	 * @return the acctTos
	 */
	public String getAcctTos() {
		return acctTos;
	}

	/**
	 * @param acctTos
	 *            the acctTos to set
	 */
	public void setAcctTos(final String acctTos) {
		this.acctTos = acctTos;
	}

	public AcctTos getAcctTos(final String acctTos) {

		return (AcctTos) RECORDS_SIMULATOR_CONTEXT.getBean(acctTos);
	}

	public MarketPropertyConfigurator getPropertyConfigurator(final String property) {

		return (MarketPropertyConfigurator) RECORDS_SIMULATOR_CONTEXT.getBean(property);
	}

	/**
	 * Returns base set of command line options.
	 *
	 * @return
	 */
	protected Options getOptions() {

		return new Options().addOption(OPTION_ACCT_TOS).addOption(OPTION_RECORD_TIMESTAMP);
	}

	/**
	 * Parse command line arguments and set
	 *
	 * @param args
	 *
	 * @throws ParseException
	 *             if there are missing parameters
	 * @throws org.apache.commons.cli.ParseException
	 */
	@SuppressWarnings("unchecked")
	// Caused by third party
	protected void initArguments(final String[] args) throws ParseException {

		// get options
		Options options = getOptions();

		// create the parser
		CommandLineParser parser = new GnuParser();
		CommandLine line = null;
		// parse the command line arguments
		line = parser.parse(options, args);

		// map command line arguments into a properties object
		arguments = new Properties();
		for (final Option option : (Collection<Option>) options.getOptions()) {
			if (line.hasOption(option.getOpt())) {
				arguments.put(option.getOpt(), line.getOptionValue(option.getOpt()));
			}
		}

		String lAcctTos = arguments.getProperty(OPTION_ACCT_TOS.getOpt());
		setAcctTos(lAcctTos);

		String lRecordTimestamp = arguments.getProperty(OPTION_RECORD_TIMESTAMP.getOpt());
		setRecordTimestamp(lRecordTimestamp);

		System.out.println("Received the following arguments: " + arguments.toString());
	}

	/**
	 * Get the command line options.
	 *
	 * @return the command line options.
	 */
	private static void displayUsage() {

		System.out.println("\nUsage: SimulatedDataManagerMain <acct_tos> <record_timestamp>");
	}

	/**
	 * @param pArgs
	 * @param dataManager
	 * @throws java.text.ParseException
	 * @throws IOException
	 * @throws ParseException
	 */
	public void execute(final String[] pArgs) throws java.text.ParseException, IOException, ParseException {

		SimulatedDataWriter dataWriter = new SimulatedDataWriter();

		initArguments(pArgs);

		String acctTos = getAcctTos();

		long recordTimestamp = new SimpleDateFormat("yyyyMMddHH").parse(getRecordTimestamp()).getTime();

		String[] acctTosToGenerate = acctTos.split(SPLITTER);

		if (getAcctTos() == null || getRecordTimestamp() == null) {
			displayUsage();
			return;
		}

		MarketPropertyConfigurator propConfigurator = getPropertyConfigurator("marketPropertiesConfigurator");

		directories = propConfigurator.getOutputDirectories();
		outputFilesCount = Integer.parseInt(propConfigurator.getOutputFilesCount());
		radiusFilesDirectoryPath = propConfigurator.getRadiusCARFilesDirectory();

		if (outputFilesCount == 0 || directories == null) {
			System.out.println("Output count or directories is not specified in config.properties");
			return;
		}

		String[] outputDirectopriesList = directories.split(SPLITTER);

		List<GZIPOutputStream> outputStreamsList = dataWriter.createStreamWriters(outputDirectopriesList,
				outputFilesCount, radiusFilesDirectoryPath);

		dataWriter.writeRadiusRecordTemplatesToFile(recordTimestamp, acctTosToGenerate, outputDirectopriesList,
				outputStreamsList);

		List<String> outputFileNameList = dataWriter.getOutputFileNamesList();

		dataWriter.closeReport(outputStreamsList, outputFileNameList);

	}

	/**
	 * @param args
	 */
	public static void main(final String[] args) {

		try {

			SimulatedDataManagerMain dataManager = new SimulatedDataManagerMain();

			dataManager.execute(args);

		} catch (Exception e) {
			e.printStackTrace();
			System.err
					.println(String.format("Got an Exception - %s: %s", e.getClass().getSimpleName(), e.getMessage()));
		}

	}

}
