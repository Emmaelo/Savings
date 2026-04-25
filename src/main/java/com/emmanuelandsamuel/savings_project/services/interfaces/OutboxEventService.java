package com.emmanuelandsamuel.savings_project.services.interfaces;

import com.emmanuelandsamuel.savings_project.dtos.requests.SaveOutboxEventRequest;

public interface OutboxEventService {

    void saveOutboxEvent(SaveOutboxEventRequest saveOutboxEventRequest);

}
