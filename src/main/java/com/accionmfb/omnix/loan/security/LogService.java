package com.accionmfb.omnix.loan.security;

/**
 *
 * @author dofoleta
 */
public interface LogService {

    public void logInfo(String app, String token, String logMessage, String logType, String requestId);

    public void logError(String app, String token, String logMessage, String logType, String requestId);
}
