package edu.uci.ics.crawler4j.examples.basic;

import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.parser.HtmlParseData;
import edu.uci.ics.crawler4j.url.WebURL;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

public class BasicCrawler extends WebCrawler {

	public LinkedList<WebURL> liste = new LinkedList<WebURL>();
	private final static Pattern FILTERS = Pattern.compile(".*(\\.(css|js|bmp|gif|jpe?g" + "|png|tiff?|mid|mp2|mp3|mp4"
			+ "|wav|avi|mov|mpeg|ram|m4v|pdf" + "|rm|smil|wmv|swf|wma|zip|rar|gz))$");

	/**
	 * You should implement this function to specify whether
	 * the given url should be crawled or not (based on your
	 * crawling logic).
	 */
	@Override
	public boolean shouldVisit(WebURL url) {
		String href = url.getURL().toLowerCase();
	
		return !FILTERS.matcher(href).matches() && (href.startsWith("http://mosaic.mcom.com/"));//  && href.contains("bios") //Keyword eklemesi
	
	}

	
	/**
	 * This function is called when a page is fetched and ready 
	 * to be processed by your program.
	 */
	@Override
	public void visit(Page page) {
		

		int docid = page.getWebURL().getDocid();
		String url = page.getWebURL().getURL();
		int parentDocid = page.getWebURL().getParentDocid();

		
		System.out.println("Docid: " + docid);
		System.out.println("URL: " + url);
		System.out.println("Docid of parent page: " + parentDocid);

		if (page.getParseData() instanceof HtmlParseData) {
			HtmlParseData htmlParseData = (HtmlParseData) page.getParseData();
			String text = htmlParseData.getText();
			String html = htmlParseData.getHtml();
			List<WebURL> links = htmlParseData.getOutgoingUrls();

			System.out.println("Text length: " + text.length());
			System.out.println("Html length: " + html.length());
			System.out.println("Number of outgoing links: " + links.size());
		}

		System.out.println("=============");
		
	}

	/*public void visit(Page page) {
		for(WebURL urll: liste){
		int docid = urll.getDocid();
		String url = urll.getURL();
		int parentDocid = urll.getParentDocid();

		System.out.println("Docid: " + docid);
		System.out.println("URL: " + url);
		System.out.println("Docid of parent page: " + parentDocid);

		

		System.out.println("=============");
	
	}
	}*/
	

	
}
