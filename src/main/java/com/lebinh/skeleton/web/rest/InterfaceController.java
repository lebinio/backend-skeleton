package com.lebinh.skeleton.web.rest;

import java.net.URI;
import java.net.URISyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class InterfaceController {

  private final Logger log = LoggerFactory.getLogger(InterfaceController.class);

  @RequestMapping(
      value = "/upload",
      method = RequestMethod.POST,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<String> createAttachment(
      @RequestParam(value = "username") String username,
      @RequestParam(value = "files") MultipartFile[] files)
      throws URISyntaxException {

    // 1. check file
    // check extension: xlsx
    // check content: 
    //  - 2 sheet
    //  - data sheet project name
    //  - check number of columns 
    // 2. read file
    // 3. insert or update data

    

    return ResponseEntity.created(new URI("/api/upload")).body("success");
  }
}
