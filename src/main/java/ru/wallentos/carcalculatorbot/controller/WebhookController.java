package ru.wallentos.carcalculatorbot.controller;

import java.io.IOException;
import java.security.GeneralSecurityException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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

    @PostMapping("/callback/update")
    public ResponseEntity<?> onUpdateReceived(@RequestBody Update update) {
        updateProcessor.processUpdate(update);
        return ResponseEntity.ok().build();
    }

    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<?> onUpdateReceived2() {
        //  updateProcessor.processUpdate(update);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/mygoogle")
    public ResponseEntity<?> onReceivedUpdate222() throws GeneralSecurityException, IOException {
        return ResponseEntity.accepted().body(updateProcessor.processGoogle());
    }
}
