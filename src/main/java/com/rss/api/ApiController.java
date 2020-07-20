package com.rss.api;

import com.rss.utils.DislogLogger;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ApiController {

    private DislogLogger logger = new DislogLogger(this.getClass());

    @RequestMapping("/")
    public String test() throws Exception {
        return "ayyy"; 
    }

    @RequestMapping(value = "/subsription", method = RequestMethod.POST)
    public String registerSubcscription() throws Exception {
        return "ehh";
    }
}
