package com.rss.api;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import com.rss.utils.DislogLogger;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.net.URL;

@RestController
public class ApiController {

    private DislogLogger logger = new DislogLogger(this.getClass());

    @RequestMapping("/")
    public String test() throws Exception {
        logger.info("ayyyy");
        String url = "https://reddit.com/r/japan/new.rss";
        SyndFeed feed = new SyndFeedInput().build(new XmlReader(new URL(url)));
        System.out.println(feed.getTitle());
        for (SyndEntry e : feed.getEntries()) {
            System.out.println(e);
        }
        return "ayyy"; 
    }

    @RequestMapping(value = "/subsription", method = RequestMethod.POST)
    public String registerSubcscription() throws Exception {
        return "ehh";
    }
}
