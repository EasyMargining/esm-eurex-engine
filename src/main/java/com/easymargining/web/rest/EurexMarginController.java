package com.easymargining.web.rest;

import com.codahale.metrics.annotation.Timed;
import com.easymargining.domain.Trade;
import com.easymargining.service.MarginService;
import com.easymargining.web.rest.dto.results.MarginResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Created by Gilles Marchal on 21/01/2016.
 */
@Slf4j
@RestController
@RequestMapping(value = "/api/margin")
public class EurexMarginController {

    @Autowired
    MarginService marginService;

    @RequestMapping(value = "/computeEtd",
        method = RequestMethod.POST,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    public ResponseEntity<MarginResult> computeEtdMargin(@RequestBody List<Trade> trades) {

        MarginResult result = marginService.computeEtdMargin(trades);

        return new ResponseEntity<>(result, HttpStatus.OK);
    }


}
