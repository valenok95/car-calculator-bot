package ru.wallentos.carcalculatorbot.controller;

import java.io.IOException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    @GetMapping("/mygoogle")
    public ResponseEntity<?> credLocation()  {
        return ResponseEntity.accepted().body(updateProcessor.processGoogle());
    }
}
