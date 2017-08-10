/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.tudarmstadt.ukp.experiments.roomfordebate;

import com.google.common.collect.ImmutableMap;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

/**
 * Fetching a single debate from Room for Debate
 *
 * @author Ivan Habernal
 */
public class DebateFetcher
{
    private static final int SLEEP_TIME_BETWEEN_REQUESTS = 10000;

    private final ChromeDriverService service;
    private final RemoteWebDriver driver;

    public DebateFetcher()
            throws IOException
    {
        this("roomfordebate/chromedriver/chromedriver-linux64-2.27");
    }

    public DebateFetcher(String chromeDriverFile)
            throws IOException
    {
        service = new ChromeDriverService.Builder()
                .usingDriverExecutable(
                        new File(chromeDriverFile))
                .usingAnyFreePort()
                .withEnvironment(ImmutableMap.of("DISPLAY", ":20")).build();
        service.start();

        DesiredCapabilities capabilities = DesiredCapabilities.chrome();
        driver = new RemoteWebDriver(service.getUrl(), capabilities);
    }

    public void finishFetching()
    {
        driver.quit();
        service.stop();
    }

    public String fetchDiscussionPage(String articleUrl)
            throws InterruptedException
    {
        driver.get(articleUrl);
        Thread.sleep(SLEEP_TIME_BETWEEN_REQUESTS);

        // roll-out the entire discussion
        List<WebElement> commentsExpandElements;
        do {
            commentsExpandElements = driver.findElements(By.cssSelector("div.comments-expand"));

            // remove all disabled or invisible
            Iterator<WebElement> iterator = commentsExpandElements.iterator();
            while (iterator.hasNext()) {
                WebElement commentsExpandElement = iterator.next();
                if (!commentsExpandElement.isDisplayed() || !commentsExpandElement.isEnabled()) {
                    iterator.remove();
                }
            }

            System.out.println(
                    "Found " + commentsExpandElements.size() + " div.comments-expand elements");

            // click on each of them
            for (WebElement commentsExpandElement : commentsExpandElements) {
                // only if visible & enabled
                commentsExpandElement.click();

                System.out.println("Clicking to unfold discussion...");

                // give it some time to load new comments
                Thread.sleep(SLEEP_TIME_BETWEEN_REQUESTS);
            }
        }
        // until there is one remaining that doesn't do anything...
        while (commentsExpandElements.size() > 0);

        // get the html
        return driver.getPageSource();
    }

    public static void main(String[] args)
            throws Exception
    {
        // this must be running....
        // $ /usr/bin/Xvfb :20 -screen 0 1280x4096x24 -ac +extension GLX +render -noreset

        String testingUrl = "http://www.nytimes.com/roomfordebate/2016/12/28/should-foreign-language-classes-be-mandatory-in-college/learning-to-think-is-more-important-than-learning-a-language";
        DebateFetcher debateFetcher = new DebateFetcher();
        String html = debateFetcher.fetchDiscussionPage(testingUrl);
        System.out.println(html.contains(
                "You only know that it did not make you communicatively competent at all 30"));
        System.out.println(html.contains("It was a requirement in my high school in Italy"));

        debateFetcher.finishFetching();
    }
}
