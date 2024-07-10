# mqConnectionQuarkusPOC

## Use Case

1. **Sending Messages:**
    - Endpoint: `/mq/send-message`
    - Purpose: Produces messages on the MQ topic "request-topic."

2. **Receiving and Reproducing Messages:**
    - Messages produced on "request-topic" are received and reproduced to another MQ topic, "response-topic."

3. **Capturing Responses:**
    - Captures messages from "response-topic."
    - Resends the captured response to the original endpoint response (`/mq/send-message`).

4. **Additional Endpoint:**
    - Endpoint: `/mq/receive-message`
    - Purpose: Retrieves a specific message response related to the originally produced request message, identified by the correlation ID.

This setup allows for the seamless flow of messages, starting from message production, through reproduction, capturing responses, and providing a dedicated endpoint for receiving specific message responses based on correlation IDs.
