/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.uci.ics.crawler4j.crawler;

import edu.uci.ics.crawler4j.fetcher.PageFetchResult;
import edu.uci.ics.crawler4j.fetcher.PageFetchStatus;
import edu.uci.ics.crawler4j.fetcher.PageFetcher;
import edu.uci.ics.crawler4j.frontier.DocIDServer;
import edu.uci.ics.crawler4j.frontier.Frontier;
import edu.uci.ics.crawler4j.parser.HtmlParseData;
import edu.uci.ics.crawler4j.parser.ParseData;
import edu.uci.ics.crawler4j.parser.Parser;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtServer;
import edu.uci.ics.crawler4j.url.WebURL;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * WebCrawler class in the Runnable class that is executed by each crawler
 * thread.
 * 
 * @author Yasser Ganjisaffar <lastname at gmail dot com>
 */
public class WebCrawler implements Runnable {
	
	protected static final Logger logger = Logger.getLogger(WebCrawler.class.getName());


List<Page> biospagenot = new ArrayList<Page>(50); //BFS Belirtilen keyword'ü içermeyen page'lerin toplandýðý liste

	/**
	 * The id associated to the crawler thread running this instance
	 */
	protected int myId;

	/**
	 * The controller instance that has created this crawler thread. This
	 * reference to the controller can be used for getting configurations of the
	 * current crawl or adding new seeds during runtime.
	 */
	protected CrawlController myController;

	/**
	 * The thread within which this crawler instance is running.
	 */
	private Thread myThread;

	/**
	 * The parser that is used by this crawler instance to parse the content of
	 * the fetched pages.
	 */
	private Parser parser;

	/**
	 * The fetcher that is used by this crawler instance to fetch the content of
	 * pages from the web.
	 */
	private PageFetcher pageFetcher;

	/**
	 * The RobotstxtServer instance that is used by this crawler instance to
	 * determine whether the crawler is allowed to crawl the content of each
	 * page.
	 */
	private RobotstxtServer robotstxtServer;

	/**
	 * The DocIDServer that is used by this crawler instance to map each URL to
	 * a unique docid.
	 */
	private DocIDServer docIdServer;

	/**
	 * The Frontier object that manages the crawl queue.
	 */
	private Frontier frontier;

	/**
	 * Is the current crawler instance waiting for new URLs? This field is
	 * mainly used by the controller to detect whether all of the crawler
	 * instances are waiting for new URLs and therefore there is no more work
	 * and crawling can be stopped.
	 */
	private boolean isWaitingForNewURLs;
	
	private String schedulingtype;

	/**
	 * Initializes the current instance of the crawler
	 * 
	 * @param myId
	 *            the id of this crawler instance
	 * @param crawlController
	 *            the controller that manages this crawling session
	 */
	public void init(int myId, CrawlController crawlController) {
		this.myId = myId;
		this.pageFetcher = crawlController.getPageFetcher();
		this.robotstxtServer = crawlController.getRobotstxtServer();
		this.docIdServer = crawlController.getDocIdServer();
		this.frontier = crawlController.getFrontier();
		this.parser = new Parser(crawlController.getConfig());
		this.myController = crawlController;
		this.isWaitingForNewURLs = false;
	}

	/**
	 * Get the id of the current crawler instance
	 * 
	 * @return the id of the current crawler instance
	 */
	public int getMyId() {
		return myId;
	}

	public CrawlController getMyController() {
		return myController;
	}

	/**
	 * This function is called just before starting the crawl by this crawler
	 * instance. It can be used for setting up the data structures or
	 * initializations needed by this crawler instance.
	 */
	public void onStart() {
	}

	/**
	 * This function is called just before the termination of the current
	 * crawler instance. It can be used for persisting in-memory data or other
	 * finalization tasks.
	 */
	public void onBeforeExit() {
	}

	/**
	 * The CrawlController instance that has created this crawler instance will
	 * call this function just before terminating this crawler thread. Classes
	 * that extend WebCrawler can override this function to pass their local
	 * data to their controller. The controller then puts these local data in a
	 * List that can then be used for processing the local data of crawlers (if
	 * needed).
	 */
	public Object getMyLocalData() {
		return null;
	}
	/*
	 * In this  function, everthing is based on processpage function which is basically indexing
	 * Url, and making something to use as recursive calling Depth first search, we changed WebURL class
	 * and added status int. for controlling. After that, the main function that crawls data is , 
	 * visit(page) like in original one. 
	 */
	private int dfsPage(WebURL curURL){
		if (curURL == null) {
			return -1;
		}
		if(curURL.getDepth() == myController.getConfig().getMaxDepthOfCrawling() +1){
			System.out.println("We hit the limit of max depth");
			return -1;
		}
		else{//we marked url status to 1;
			curURL.setStatus(1);
			/*
			 * Below code is mainly based on original processPage on Breadth first search 
			 */
			PageFetchResult fetchResult = null;
			fetchResult = pageFetcher.fetchHeader(curURL);
			Page page = new Page(curURL);
			int docid = curURL.getDocid();
			if (fetchResult.fetchContent(page) && parser.parse(page, curURL.getURL())) {
				ParseData parseData = page.getParseData();
				if (parseData instanceof HtmlParseData) {
					HtmlParseData htmlParseData = (HtmlParseData) parseData;
					
					int maxCrawlDepth = myController.getConfig().getMaxDepthOfCrawling();
					for (WebURL webURL : htmlParseData.getOutgoingUrls()) {//for every outgoing url, means childs
						webURL.setParentDocid(docid);//set their parent it
						int newdocid = docIdServer.getDocId(webURL.getURL());
						if (newdocid > 0) {
							webURL.setDepth((short) -1);
							webURL.setDocid(newdocid);
						} else {
							webURL.setDocid(-1);
							webURL.setDepth((short) (curURL.getDepth() + 1));
							if (maxCrawlDepth == -1 || curURL.getDepth() < maxCrawlDepth) {
								if (shouldVisit(webURL) && robotstxtServer.allows(webURL)) {
									webURL.setDocid(docIdServer.getNewDocID(webURL.getURL()));
									if(webURL.getStatus() == 0){//if this url is not visited
										dfsPage(webURL);//recursive call url
									}
								}
							}
						}
					}
					//frontier.scheduleAll(toSchedule);
					visit(page);
					curURL.setStatus(2);//after all works done, set url to visited.
				}
				
			}
		}
		return 0;
	}

	public void run() {
		onStart();
		while (true) {
			List<WebURL> assignedURLs = new ArrayList<WebURL>(50);
			isWaitingForNewURLs = true;
			frontier.getNextURLs(50, assignedURLs);
			isWaitingForNewURLs = false;
			if (assignedURLs.size() == 0) { 
				
				
				
				if (frontier.isFinished()) {
					return;
				}
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			
			} else {
			
			//	System.out.println("elseelse");
					
					for (WebURL curURL : assignedURLs) {
						if (curURL != null) {
							/*
							 * If curUrl is not empty, then get our menu choice and do work according to that.
							 */
							if(myController.getChoice()==1){// Genel
								 						dfsPage(curURL);} //Genel
								 						else{//Genel
								 						bfsPage(curURL);} //Genel
							//processPage(curURL);
							frontier.setProcessed(curURL);
						}
						
			
						if (myController.isShuttingDown()) {
							logger.info("Exiting because of controller shutdown.");
							return;
						}
					}
			
			
			
			
					
			}
		}
	}

	/**
	 * Classes that extends WebCrawler can overwrite this function to tell the
	 * crawler whether the given url should be crawled or not. The following
	 * implementation indicates that all urls should be included in the crawl.
	 * 
	 * @param url
	 *            the url which we are interested to know whether it should be
	 *            included in the crawl or not.
	 * @return if the url should be included in the crawl it returns true,
	 *         otherwise false is returned.
	 */
	public boolean shouldVisit(WebURL url) {
		return true;
	}
	

	/**
	 * Classes that extends WebCrawler can overwrite this function to process
	 * the content of the fetched and parsed page.
	 * 
	 * @param page
	 *            the page object that is just fetched and parsed.
	 */
	public void visit(Page page) {
	}
	
	private int bfsPage(WebURL curURL) {
		if (curURL == null) {
			return -1;
		}
		PageFetchResult fetchResult = null;
		try {
	//		System.out.println("try-----1" );
			fetchResult = pageFetcher.fetchHeader(curURL);
			if (fetchResult.getStatusCode() != PageFetchStatus.OK) {
		//		System.out.println("OK-----2" );
				if (fetchResult.getStatusCode() == PageFetchStatus.Moved) {
			//		System.out.println("Moved-----3" );
					if (myController.getConfig().isFollowRedirects()) {
					//	System.out.println("getconfÝsfollowRedirect-----4" );
						String movedToUrl = curURL.getURL();
						if (movedToUrl == null) {
					//		System.out.println("Movedtourl=null-----5" );
							return PageFetchStatus.MovedToUnknownLocation;
						}
						int newDocId = docIdServer.getDocId(movedToUrl);
						if (newDocId > 0) {
				//			System.out.println("newdocýd-----6" );
							return PageFetchStatus.RedirectedPageIsSeen; 
						} else {
					//		System.out.println("ELSE-----7" );
							WebURL webURL = new WebURL();
							webURL.setURL(movedToUrl);
							webURL.setParentDocid(curURL.getParentDocid());
							webURL.setDepth(curURL.getDepth());
							webURL.setDocid(-1);
							if (shouldVisit(webURL) && robotstxtServer.allows(webURL)) {
				//				System.out.println("shouldvisit ve robotstxtserverallows-----8" );
								webURL.setDocid(docIdServer.getNewDocID(movedToUrl));
								frontier.schedule(webURL);
							}
						}
					}
					return PageFetchStatus.Moved;
				} else if (fetchResult.getStatusCode() == PageFetchStatus.PageTooBig) {
					logger.info("Skipping a page which was bigger than max allowed size: " + curURL.getURL());
				}
				return fetchResult.getStatusCode();
			}

			if (!curURL.getURL().equals(fetchResult.getFetchedUrl())) {
				if (docIdServer.isSeenBefore(fetchResult.getFetchedUrl())) {
			//		System.out.println("isseenbefore-----9" );
					return PageFetchStatus.RedirectedPageIsSeen;
				}
		//		System.out.println("!url fetchurl-----10" );
				curURL.setURL(fetchResult.getFetchedUrl());
				curURL.setDocid(docIdServer.getNewDocID(fetchResult.getFetchedUrl()));
			}
	//		System.out.println("page-----11" );
			Page page = new Page(curURL);
			int docid = curURL.getDocid();
			if (fetchResult.fetchContent(page) && parser.parse(page, curURL.getURL())) {
		//		System.out.println("parser and fetchcontent true-----12" );
				ParseData parseData = page.getParseData();
				if (parseData instanceof HtmlParseData) {
					//System.out.println("parse instance html isee-----13" );
					HtmlParseData htmlParseData = (HtmlParseData) parseData;

					List<WebURL> toSchedule = new ArrayList<WebURL>();
					int maxCrawlDepth = myController.getConfig().getMaxDepthOfCrawling();
					for (WebURL webURL : htmlParseData.getOutgoingUrls()) {
						webURL.setParentDocid(docid);
						int newdocid = docIdServer.getDocId(webURL.getURL());
						if (newdocid > 0) {
							// This is not the first time that this Url is
							// visited. So, we set the depth to a negative
							// number.
						//	System.out.println("docýd>0-----14" );
							webURL.setDepth((short) -1); 
							webURL.setDocid(newdocid);
						} else {
						//	System.out.println("docýd<=0-----15" );
							webURL.setDocid(-1);
							webURL.setDepth((short) (curURL.getDepth() + 1));
							if (maxCrawlDepth == -1 || curURL.getDepth() < maxCrawlDepth) {
							//	System.out.println("maxcralwdept=-1 veya get<max-----16" );
								if (shouldVisit(webURL) && robotstxtServer.allows(webURL)) {
									webURL.setDocid(docIdServer.getNewDocID(webURL.getURL()));
									toSchedule.add(webURL);
								}
							}
						}
					}
				//	System.out.println("frontier schedule all-----17" );
					frontier.scheduleAll(toSchedule);
				}
				
				
				//	System.out.println("Visite girdi orji:------------------------ " );
				
				/*Ýf komutu ile belirttilen keyword içerilmiyorsa 
				 * yukarýda global olarak tanýmladýðýmýz biospagenot listemize ekliyoruz. 
				 * Belirtilen Keyword içeriliyor ise page visit ediliyor. En sonunda sistem
				 * gidilecek page bulamayýnca CrawlController.java dosyasý içinde tanýmladýðýmýz
				 * biospagenot listesindeki page'leri visit ediyoruz.
				 * 
				 */
					if(!page.getWebURL().toString().contains("bios")){ //BFS 
						biospagenot.add(page); //BFS
					} //BFS
					else //BFS
				visit(page);
				
			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage() + ", while processing: " + curURL.getURL());
		} finally {
			if (fetchResult != null) {
				fetchResult.discardContentIfNotConsumed();
			}
		}
		return 0;
	}
	
	
	

	public Thread getThread() {
		return myThread;
	}

	public void setThread(Thread myThread) {
		this.myThread = myThread;
	}

	public boolean isNotWaitingForNewURLs() {
		return !isWaitingForNewURLs;
	}

	public String getSchedulingType() {
		return this.schedulingtype;
	}
	
}
