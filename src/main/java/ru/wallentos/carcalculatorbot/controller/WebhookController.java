package ru.wallentos.carcalculatorbot.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.telegram.telegrambots.meta.api.objects.Update;

@RestController
public class WebhookController {
    private final UpdateProcessor updateProcessor;

    public WebhookController(UpdateProcessor updateProcessor) {
        this.updateProcessor = updateProcessor;
    }

    /*
        @PostMapping("/callback/update")
        public ResponseEntity<?> onReceivedUpdate(@RequestBody Update update) {
            updateProcessor.processUpdate(update);
            return ResponseEntity.ok().build();
        }
    
        }*/
    @RequestMapping(value = "/callback/update", method = RequestMethod.POST)
    public ResponseEntity<?> onUpdateReceived(@RequestBody Update update) {
        updateProcessor.processUpdate(update);
        return ResponseEntity.ok().build();
    }

    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<?> onUpdateReceived2() {
        //  updateProcessor.processUpdate(update);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/callback/update")
    public ResponseEntity<?> onReceivedUpdate222() {
        //updateProcessor.processUpdate(update);
        System.out.println("вы попали в метод get callbackUpdate");
        return ResponseEntity.accepted().build();
    }
}
