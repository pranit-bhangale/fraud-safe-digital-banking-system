package com.banking.helpdesk.constant;

import lombok.experimental.UtilityClass;

/**
 * Constants for Server-Sent Events (SSE) streaming protocols.
 */
@UtilityClass
public class StreamEventConstant {

    public static final String EVENT_TOKEN = "token";
    public static final String EVENT_TOOL = "tool";
    public static final String EVENT_ERROR = "error";
    public static final String EVENT_DONE = "done";
}
