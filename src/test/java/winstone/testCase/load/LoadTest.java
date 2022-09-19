/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package winstone.testCase.load;

import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;
import winstone.Logger;
import winstone.WinstoneResourceBundle;

import winstone.cmdline.Option;

/**
 * This class is an attempt to benchmark performance under load for winstone. It
 * works by hitting a supplied URL with parallel threads (with keep-alives or
 * without) at an escalating rate, and counting the no of failures.
 *
 * It uses {@link java.net.http.HttpClient} for the connection.
 *
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: LoadTest.java,v 1.2 2006/02/28 07:32:49 rickknowles Exp $
 */
@Ignore("Intended to be run manually")
public class LoadTest {
    private String url;
    private boolean useKeepAlives;
    private int startThreads;
    private int endThreads;
    private int stepSize;
    private long stepPeriod;
    private long gracePeriod;
    private long successTimeTotal;
    private int successCount;
    private WinstoneResourceBundle resources;

    private static String LOCAL_RESOURCE_FILE = "winstone.testCase.load.LocalStrings";

    public LoadTest(WinstoneResourceBundle resources, String url,
            boolean useKeepAlives, int startThreads, int endThreads,
            int stepSize, long stepPeriod, long gracePeriod) {
        this.resources = resources;
        this.url = url;
        this.useKeepAlives = useKeepAlives;
        this.startThreads = startThreads;
        this.endThreads = endThreads;
        this.stepSize = stepSize;
        this.stepPeriod = stepPeriod;
        this.gracePeriod = gracePeriod;

        Logger.log(Logger.INFO, resources, "LoadTest.Config", this.url, this.useKeepAlives + "", this.startThreads + "",
                this.endThreads + "", this.stepSize + "", this.stepPeriod + "",
                this.gracePeriod + "");
    }

    @Test
    public void test() throws InterruptedException {
        HttpClient client = null;

        // Loop through in steps
        for (int n = this.startThreads; n <= this.endThreads; n += this.stepSize) {
            if (this.useKeepAlives)
                client = HttpClient.newHttpClient();

            // Spawn the threads
            int noOfSeconds = (int) this.stepPeriod / 1000;
            List<LoadTestThread> threads = new ArrayList<>();
            for (int m = 0; m < n; m++)
                threads.add(new LoadTestThread(this.url, this, this.resources,
                        client, noOfSeconds - 1));

            // Sleep for step period
            Thread.sleep(this.stepPeriod + gracePeriod);

            // int errorCount = (noOfSeconds * n) - this.successCount;
            Long averageSuccessTime = this.successCount == 0 ? null : this.successTimeTotal / this.successCount;

            // Write out results
            Logger.log(Logger.INFO, resources, "LoadTest.LineResult",
                    n + "", this.successCount + "",
                    ((noOfSeconds * n) - this.successCount) + "",
                    averageSuccessTime + "");

            // Close threads
            for (LoadTestThread thread : threads) thread.destroy();

            this.successTimeTotal = 0;
            this.successCount = 0;

        }
    }

    public void incTimeTotal(long amount) {
        this.successTimeTotal += amount;
    }

    public void incSuccessCount() {
        this.successCount++;
    }

    public static void main(String[] args) throws Exception {
        WinstoneResourceBundle resources = new WinstoneResourceBundle(
                LOCAL_RESOURCE_FILE);

        // Loop for args
        Map<String,String> options = new HashMap<>();
        // String operation = "";
        for (String option : args) {
            if (option.startsWith("--")) {
                int equalPos = option.indexOf('=');
                String paramName = option.substring(2, equalPos == -1 ? option
                        .length() : equalPos);
                String paramValue = (equalPos == -1 ? "true" : option
                        .substring(equalPos + 1));
                options.put(paramName, paramValue);
            }
        }

        if (options.size() == 0) {
            printUsage(resources);
            return;
        }
        Logger.setCurrentDebugLevel(Integer.parseInt(Option.stringArg(options, "debug", "5")));

        String url = Option.stringArg(options, "url", "http://localhost:8080/");
        boolean keepAlive = Option.booleanArg(options, "keepAlive", true);
        String startThreads = Option.stringArg(options, "startThreads", "20");
        String endThreads = Option.stringArg(options, "endThreads", "1000");
        String stepSize = Option.stringArg(options, "stepSize", "20");
        String stepPeriod = Option.stringArg(options, "stepPeriod", "5000");
        String gracePeriod = Option.stringArg(options, "gracePeriod", "5000");

        LoadTest lt = new LoadTest(resources, url, keepAlive, Integer
                .parseInt(startThreads), Integer.parseInt(endThreads), Integer
                .parseInt(stepSize), Integer.parseInt(stepPeriod), Integer
                .parseInt(gracePeriod));

        lt.test();
    }

    /**
     * Displays the usage message
     */
    private static void printUsage(WinstoneResourceBundle resources) {
        System.out.println(resources.getString("LoadTest.Usage"));
    }

}
