package com.example.WordDocumentsFiller.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
@Controller
public class InstrumentsController {

    @GetMapping("/instruments")
    public String showInstruments(){
        return "instruments";
    }

    @GetMapping("/customs_duties")
    public String showDuty(){
        return "customs_duties";
    }

    @GetMapping("/unloading")
    public String instructions(){
        return "unloading";
    }

    @GetMapping("/pickup_instructions")
    public String pickupInstructions(){
        return "pickup_instructions";
    }



}
