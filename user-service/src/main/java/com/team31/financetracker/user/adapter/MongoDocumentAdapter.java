package com.team31.financetracker.user.adapter;

import com.team31.financetracker.user.dto.ActivityEventDTO;
import com.team31.financetracker.user.model.nosql.AuthEvent;
import org.springframework.stereotype.Component;

@Component
public class MongoDocumentAdapter {

    public ActivityEventDTO adapt(AuthEvent event) {
        return new ActivityEventDTO(event.getAction(), event.getTimestamp(), event.getDetails());
    }
}
