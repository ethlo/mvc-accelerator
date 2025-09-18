package com.ethlo.mvc.demo;

import com.ethlo.mvc.MvcAccelerator;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/demo/performance")
public class PerformanceDemoController {
    @MvcAccelerator
    @GetMapping("/fast/{var1}/{var2}")
    public String fastPath(@PathVariable String var1,
                           @PathVariable String var2) {
        return var1 + " - " + var2 + "@" + System.currentTimeMillis();
    }


    @GetMapping("/normal/{var1}/{var2}")
    public String normalPath(@PathVariable String var1,
                             @PathVariable String var2) {
        return var1 + " - " + var2 + "@" + System.currentTimeMillis();
    }
}