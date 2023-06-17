/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.loan.service;

import com.accionmfb.omnix.loan.constant.ResponseCodes;
import com.accionmfb.omnix.loan.jwt.JwtTokenUtil;
import com.accionmfb.omnix.loan.model.*;
import com.accionmfb.omnix.loan.payload.*;
import com.accionmfb.omnix.loan.repository.LoanRepository;
import com.accionmfb.omnix.loan.repository.SmsRepository;
import com.google.gson.Gson;
import com.itextpdf.kernel.color.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.property.TextAlignment;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.temenos.tocf.t24ra.T24Connection;
import com.temenos.tocf.t24ra.T24DefaultConnectionFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.MessageDigest;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * @author bokon
 */
@Service
@Slf4j
public class GenericServiceImpl implements GenericService {

    @Autowired
    LoanRepository loanRepository;
    @Autowired
    JwtTokenUtil jwtToken;
    @Autowired
    MessageSource messageSource;
    @Autowired
    Gson gson;
    @Autowired
    SmsRepository smsRepository;
    @Autowired
    NotificationService notificationService;
    @Value("${omnix.t24.host}")
    private String HOST_ADDRESS;
    @Value("${omnix.t24.port}")
    private String PORT_NUMBER;
    @Value("${omnix.t24.ofs.id}")
    private String OFS_ID;
    @Value("${omnix.t24.ofs.source}")
    private String OFS_STRING;
    @Value("${omnix.middleware.host.ip}")
    private String middlewareHostIP;
    @Value("${omnix.middleware.host.port}")
    private String middlewareHostPort;
    @Value("${omnix.middleware.authorization}")
    private String middlewareAuthorization;
    @Value("${omnix.middleware.signature.method}")
    private String middlewareSignatureMethod;
    @Value("${omnix.middleware.user.secret}")
    private String middlewareUserSecretKey;
    @Value("${omnix.middleware.username}")
    private String middlewareUsername;
    @Value("${omnix.start.morning}")
    private String startMorning;
    @Value("${omnix.end.morning}")
    private String endMorning;
    @Value("${omnix.start.afternoon}")
    private String startAfternoon;
    @Value("${omnix.end.afternoon}")
    private String endAfternoon;
    @Value("${omnix.start.evening}")
    private String startEvening;
    @Value("${omnix.end.evening}")
    private String endEvening;
    @Value("${omnix.start.night}")
    private String startNight;
    @Value("${omnix.end.night}")
    private String endNight;
    @Value("${omnix.channel.user.default}")
    private String t24Credentials;
    @Value("${omnix.version.loan.portfolio}")
    private String loanPortfolioVersion;
    @Value("${admin.consol.ip}")
    private String adminIP;
    @Value("${omnix.mail.username}")
    private String mailUsername;
    @Value("${omnix.mail.password}")
    private String mailPassword;
    @Value("${omnix.mail.contact.center.username}")
    private String contactCenterEmailUsername;
    @Value("${omnix.mail.contact.center.password}")
    private String contactCenterEmailPassword;
    @Value("${server.temp.dir}")
    private String tempDirectory;
    @Value("${omnix.creditBureau.data.dump.path}")
    private String crcDataDumpPath;
    @Value("${omnix.creditBureau.data.upload.path}")
    private String crcDataUploadPath;
    @Value("${omnix.creditBureau.data.upload.crc.individual}")
    private String individualDataUploadUrl;
    @Value("${omnix.creditBureau.data.upload.crc.credit}")
    private String creditDataUploadUrl;
    @Value("${omnix.creditBureau.data.upload.crc.guarantor}")
    private String guarantorDataUploadUrl;
    @Value("${omnix.creditBureau.data.upload.crc.apikey}")
    private String dataUploadApiKey;
    @Value("${omnix.digital.branch.code}")
    private String digitalBranchCode;
    @Value("${omnix.digital.loan.code}")
    private String digitalLoanCode;
    private static final String CRC_USERNAME = "10852215accion";
    private static final String CRC_PASSWORD = "AcC10nmFb2i2o";
    private static SecretKeySpec secretKey;
    private static byte[] key;
    Logger logger = LoggerFactory.getLogger(GenericServiceImpl.class);

    @Override
    public void generateLog(String app, String token, String logMessage, String logType, String logLevel, String requestId) {
        try {
            String requestBy = jwtToken.getUsernameFromToken(token);
            String remoteIP = jwtToken.getIPFromToken(token);
            String channel = jwtToken.getChannelFromToken(token);

            StringBuilder strBuilder = new StringBuilder();
            strBuilder.append(logType.toUpperCase(Locale.ENGLISH));
            strBuilder.append(" - ");
            strBuilder.append("[").append(remoteIP).append(":").append(channel.toUpperCase(Locale.ENGLISH)).append(":").append(requestBy.toUpperCase(Locale.ENGLISH)).append("]");
            strBuilder.append("[").append(app.toUpperCase(Locale.ENGLISH).toUpperCase(Locale.ENGLISH)).append(":").append(requestId.toUpperCase(Locale.ENGLISH)).append("]");
            strBuilder.append("[").append(logMessage).append("]");

            if ("INFO".equalsIgnoreCase(logLevel.trim())) {
                if (logger.isInfoEnabled()) {
                    logger.info(strBuilder.toString());
                }
            }

            if ("DEBUG".equalsIgnoreCase(logLevel.trim())) {
                if (logger.isDebugEnabled()) {
                    logger.error(strBuilder.toString());
                }
            }

        } catch (Exception ex) {
            if (logger.isDebugEnabled()) {
                logger.debug(ex.getMessage());
            }
        }
    }

    @Override
    public void createUserActivity(String accountNumber, String activity, String amount, String channel, String message, String mobileNumber, char status) {
        UserActivity newActivity = new UserActivity();
        newActivity.setCustomerId(accountNumber);
        newActivity.setActivity(activity);
        newActivity.setAmount(amount);
        newActivity.setChannel(channel);
        newActivity.setCreatedAt(LocalDateTime.now());
        newActivity.setMessage(message);
        newActivity.setMobileNumber(mobileNumber);
        newActivity.setStatus(status);
        loanRepository.createUserActivity(newActivity);
    }

    @Override
    public String postToT24(String requestBody) {
        try {
            T24DefaultConnectionFactory connectionFactory = new T24DefaultConnectionFactory();
            connectionFactory.setHost(HOST_ADDRESS);
            connectionFactory.setPort(Integer.valueOf(PORT_NUMBER));
            connectionFactory.enableCompression();

            Properties properties = new Properties();
            properties.setProperty("allow input", "true");
            properties.setProperty(OFS_STRING, OFS_ID);
            connectionFactory.setConnectionProperties(properties);

            T24Connection t24Connection = connectionFactory.getConnection();
            String ofsResponse = t24Connection.processOfsRequest(requestBody);

            t24Connection.close();
            return ofsResponse;
        } catch (Exception ex) {
            return ex.getMessage();
        }
    }

    @Override
    public String postToMiddleware(String requestEndPoint, String requestBody) {
        try {
            String middlewareEndpoint = "http://" + middlewareHostIP + ":" + middlewareHostPort + "/T24Gateway/services/generic" + requestEndPoint;
            String NONCE = String.valueOf(Math.random());
            String TIMESTAMP = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
            String SignaturePlain = String.format("%s:%s:%s:%s", NONCE, TIMESTAMP, middlewareUsername, middlewareUserSecretKey);
            String SIGNATURE = hash(SignaturePlain, middlewareSignatureMethod);
            Unirest.setTimeouts(0, 0);
            HttpResponse<String> httpResponse = Unirest.post(middlewareEndpoint)
                    .header("Authorization", middlewareAuthorization)
                    .header("SignatureMethod", middlewareSignatureMethod)
                    .header("Accept", "application/json")
                    .header("Timestamp", TIMESTAMP)
                    .header("Nonce", NONCE)
                    .header("Content-Type", "application/json")
                    .header("Signature", SIGNATURE)
                    .body(requestBody)
                    .asString();
            return httpResponse.getBody();
        } catch (Exception ex) {
            return ex.getMessage();
        }
    }

    @Override
    public String getActiveLoanFromMiddleware(String requestEndPoint, String requestBody) {
        try {
            String middlewareEndpoint = requestEndPoint;
            String NONCE = String.valueOf(Math.random());
            String TIMESTAMP = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
            String SignaturePlain = String.format("%s:%s:%s:%s", NONCE, TIMESTAMP, "T24AGT", "TYqZHpdjamZZs3XtgrDorw==");
            String SIGNATURE = hash(SignaturePlain, middlewareSignatureMethod);
            Unirest.setTimeouts(0, 0);
            HttpResponse<String> httpResponse = Unirest.post(middlewareEndpoint)
                    .header("Authorization", middlewareAuthorization)
                    .header("SignatureMethod", middlewareSignatureMethod)
                    .header("Accept", "application/json")
                    .header("Timestamp", TIMESTAMP)
                    .header("Nonce", NONCE)
                    .header("Content-Type", "application/json")
                    .header("Signature", SIGNATURE)
                    .body(requestBody)
                    .asString();
            return httpResponse.getBody();
        } catch (Exception ex) {
            return ex.getMessage();
        }
    }

    private String hash(String plainText, String algorithm) {
        StringBuilder hexString = new StringBuilder();
        if (algorithm.equals("SHA512")) {
            algorithm = "SHA-512";
        }
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            md.update(plainText.getBytes());

            byte byteData[] = md.digest();

            //convert the byte to hex format method 1
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < byteData.length; i++) {
                sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
            }

            System.out.println("Hex format : " + sb.toString());

            //convert the byte to hex format method 2
            for (int i = 0; i < byteData.length; i++) {
                String hex = Integer.toHexString(0xff & byteData[i]);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        return hexString.toString().toUpperCase(Locale.ENGLISH);
    }

    public static void setKey(String myKey) {
        MessageDigest sha = null;
        try {
            key = myKey.getBytes("UTF-8");
            sha = MessageDigest.getInstance("SHA-1");
            key = sha.digest(key);
            key = Arrays.copyOf(key, 16);
            secretKey = new SecretKeySpec(key, "AES");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public String decryptString(String textToDecrypt, String encryptionKey) {
        try {
            String secret = encryptionKey.trim();
            setKey(secret);
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            String decryptedResponse = new String(cipher.doFinal(java.util.Base64.getDecoder().decode(textToDecrypt.trim())));
            String[] splitString = decryptedResponse.split(":");
            StringJoiner rawString = new StringJoiner(":");
            for (String str : splitString) {
                rawString.add(str.trim());
            }
            return rawString.toString();
        } catch (Exception ex) {
            return ex.getMessage();
        }
    }

    @Override
    public String validateT24Response(String responseString) {
        String responsePayload = null;
        if (responseString.contains("Authentication failed")) {
            responsePayload = responseString;
        }

        if (responseString.contains("Maximum T24 users")) {
            responsePayload = responseString;
        }

        if (responseString.contains("Failed to receive message")) {
            responsePayload = responseString;
        }

        if (responseString.contains("No records were found") || responseString.contains("No entries for the period")) {
            responsePayload = responseString;
        }

        if (responseString.contains("INVALID COMPANY SPECIFIED")) {
            responsePayload = responseString;
        }

        if (responseString.contains("java.lang.OutOfMemoryError")) {
            responsePayload = responseString;
        }

        if (responseString.contains("Failed to connect to host")) {
            responsePayload = responseString;
        }

        if (responseString.contains("No Cheques found")) {
            responsePayload = responseString;
        }

        if (responseString.contains("<Unreadable>")) {
            responsePayload = responseString;
        }

        if (responseString.contains("MANDATORY INPUT")) {
            responsePayload = responseString;
        }

        if (responseString.contains("Some errors while encountered")) {
            responsePayload = responseString;
        }

        if (responseString.contains("Some override conditions have not been met")) {
            responsePayload = responseString;
        }

        if (responseString.contains("don't have permissions to access this data")) {
            responsePayload = responseString;
        }

        if ("<Unreadable>".equalsIgnoreCase(responseString)) {
            responsePayload = responseString;
        }

        if (responseString.contains("User has no id")) {
            responsePayload = responseString;
        }

        if ("java.net.SocketException: Unexpected end of file from server".equalsIgnoreCase(responseString)) {
            responsePayload = responseString;
        }

        if (responseString.contains("No Cash available")) {
            responsePayload = responseString;
        }

        if (responseString.contains("INVALID ACCOUNT")) {
            responsePayload = responseString;
        }

        if (responseString.contains("MISSING") && !responseString.substring(0, 4).equals("\"//1")) {
            responsePayload = responseString;
        }

        if (responseString.contains("java.net.SocketException")
                || responseString.contains("java.net.ConnectException")
                || responseString.contains("java.net.NoRouteToHostException")
                || responseString.contains("Connection timed out")
                || responseString.contains("Connection refused")) {
            responsePayload = responseString;
        }

        if (responseString.contains("SECURITY VIOLATION")) {
            responsePayload = responseString;
        }

        if (responseString.contains("NOT SUPPLIED")) {
            responsePayload = responseString;
        }

        if (responseString.contains("NO EN\\\"\\t\\\"TRIES FOR PERIOD")) {
            responsePayload = responseString;
        }

        if (responseString.contains("CANNOT ACCESS RECORD IN ANOTHER COMPANY")) {
            responsePayload = responseString;
        }

        if ("NO DATA PRESENT IN MESSAGE".equalsIgnoreCase(responseString)) {
            responsePayload = responseString;
        }

        if (responseString.contains("//-1") || responseString.contains("//-2")) {
            responsePayload = responseString;
        }

        if ("RECORD MISSING".equalsIgnoreCase(responseString)) {
            responsePayload = responseString;
        }

        if (responseString.contains("INVALID/ NO SIGN ON NAME SUPPLIED DURING SIGN ON PROCESS")) {
            responsePayload = responseString;
        }

        return responsePayload == null ? null : responsePayload;
    }

    @Override
    public String getT24TransIdFromResponse(String response) {
        String[] splitString = response.split("/");
        return splitString[0].replace("\"", "");
    }

    @Override
    public String getTextFromOFSResponse(String ofsResponse, String textToExtract) {
        try {
            String[] splitOfsResponse = ofsResponse.split(",");
            for (String str : splitOfsResponse) {
                String[] splitText = str.split("=");
                if (splitText[0].equalsIgnoreCase(textToExtract)) {
                    return splitText[1].isBlank() ? "" : splitText[1].trim();
                }
            }
        } catch (Exception ex) {
            return null;
        }
        return null;
    }

    @Override
    public String formatDateWithHyphen(String dateToFormat) {
        StringBuilder newDate = new StringBuilder(dateToFormat);
        if (dateToFormat.length() == 8) {
            newDate.insert(4, "-").insert(7, "-");
            return newDate.toString();
        }

        return "";
    }

    @Override
    public char getTimePeriod() {
        char timePeriod = 'M';
        int hour = LocalDateTime.now().getHour();
        int morningStart = Integer.valueOf(startMorning);
        int morningEnd = Integer.valueOf(endMorning);
        int afternoonStart = Integer.valueOf(startAfternoon);
        int afternoonEnd = Integer.valueOf(endAfternoon);
        int eveningStart = Integer.valueOf(startEvening);
        int eveningEnd = Integer.valueOf(endEvening);
        int nightStart = Integer.valueOf(startNight);
        int nightEnd = Integer.valueOf(endNight);
        //Check the the period of the day
        if (hour >= morningStart && hour <= morningEnd) {
            timePeriod = 'M';
        }
        if (hour >= afternoonStart && hour <= afternoonEnd) {
            timePeriod = 'A';
        }
        if (hour >= eveningStart && hour <= eveningEnd) {
            timePeriod = 'E';
        }
        if (hour >= nightStart && hour <= nightEnd) {
            timePeriod = 'N';
        }
        return timePeriod;
    }

    @Override
    public String getPostingDate() {
        //This returns the current posting date
        String ofsRequest = "DATES,INPUT/S/PROCESS," + t24Credentials + "/,NG0010001";
        String response = postToT24(ofsRequest);
        if (response == null) {
            return "Invalid Posting Date";
        }
        return getTextFromOFSResponse(response, "TODAY:1:1");
    }

    @Override
    public String encryptString(String textToEncrypt, String token) {
        String encryptionKey = jwtToken.getEncryptionKeyFromToken(token);
        try {
            String secret = encryptionKey.trim();
            setKey(secret);
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            return java.util.Base64.getEncoder().encodeToString(cipher.doFinal(textToEncrypt.trim().getBytes("UTF-8")));
        } catch (Exception ex) {
            return ex.getMessage();
        }
    }

    @Override
    public String generateRequestId() {
        return UUID.randomUUID().toString();
    }

    @Override
    public String hashIdentityValidationRequest(IdentityRequestPayload requestPayload) {
        StringJoiner rawString = new StringJoiner(":");
        rawString.add(requestPayload.getMobileNumber().trim());
        rawString.add(requestPayload.getIdType().trim());
        rawString.add(requestPayload.getIdNumber().trim());
        rawString.add(requestPayload.getLastName());
        rawString.add(requestPayload.getFirstName().trim());
        rawString.add(requestPayload.getRequestId());
        return encryptString(rawString.toString(), requestPayload.getToken());
    }

    @Override
    public String hashBVNValidationRequest(BVNRequestPayload requestPayload) {
        StringJoiner rawString = new StringJoiner(":");
        rawString.add(requestPayload.getBvn().trim());
        rawString.add(requestPayload.getRequestId().trim());
        return encryptString(rawString.toString(), requestPayload.getToken());
    }

    @Override
    public String hashCreditBureauValidationRequest(CreditBureauRequestPayload requestPayload) {
        StringJoiner rawString = new StringJoiner(":");
        rawString.add(requestPayload.getMobileNumber().trim());
        rawString.add(requestPayload.getBvn().trim());
        rawString.add(requestPayload.getSearchType().trim());
        rawString.add(requestPayload.getRequestId().trim());
        return encryptString(rawString.toString(), requestPayload.getToken());
    }

    @Override
    public String loanRepayment(BigDecimal principal, BigDecimal interestRate, int tenor) {
        BigDecimal rate = interestRate.divide(new BigDecimal(100));
        BigDecimal one = BigDecimal.ONE;
        BigDecimal onePlusR = rate.add(one);
        BigDecimal onePlusRPower = onePlusR.pow(tenor);
        BigDecimal onePlusRPowerMinusOne = onePlusRPower.subtract(BigDecimal.ONE);
        BigDecimal onePlusRPowerMultiplyRate = onePlusRPower.multiply(rate);
        BigDecimal amount = (principal.multiply(onePlusRPowerMultiplyRate)).divide(onePlusRPowerMinusOne, 2, RoundingMode.HALF_UP);
        return amount.toString();
    }

    @Override
    public ArrayList<String> loanRepaymentFlatRateList(BigDecimal approvedAmount, BigDecimal interestRate, int tenor) {

        double interestRateDouble = interestRate.doubleValue();
        double approvedAmountDouble = approvedAmount.doubleValue();
        interestRateDouble /= 100.0;

        double monthlyRate = interestRateDouble;

        double amount =
                (approvedAmountDouble * monthlyRate) /
                        (1 - Math.pow(1 + monthlyRate, -tenor));

        DecimalFormat decimalFormat = new DecimalFormat("0.00");
        amount = Double.parseDouble(decimalFormat.format(amount));
        ArrayList<String> amountString = new ArrayList<>();
        for (int i = 0; i < tenor; i++) {
            amountString.add(String.valueOf(amount));
        }


        return amountString;
    }

    @Override
    public String loanRepaymentFlatRate(BigDecimal approvedAmount, BigDecimal interestRate, int tenor) {

        double interestRateDouble = interestRate.doubleValue();
        double approvedAmountDouble = approvedAmount.doubleValue();

        interestRateDouble /= 100.0;

        double monthlyRate = interestRateDouble;

        double amount = (approvedAmountDouble * monthlyRate) / (1 - Math.pow(1 + monthlyRate, -tenor));

        DecimalFormat decimalFormat = new DecimalFormat("0.00");
        amount = Double.parseDouble(decimalFormat.format(amount));
        return String.valueOf(amount);
    }

    @Override
    public String loanRepaymentWithReducingBalance(BigDecimal principal, BigDecimal interestRate, int tenor) {
        BigDecimal rate = interestRate.divide(new BigDecimal(100));
        BigDecimal one = BigDecimal.ONE;
        BigDecimal onePlusR = rate.add(one);
        BigDecimal onePlusRPower = onePlusR.pow(tenor);
        BigDecimal onePlusRPowerMinusOne = onePlusRPower.subtract(BigDecimal.ONE);
        BigDecimal onePlusRPowerMultiplyRate = onePlusRPower.multiply(rate);
        BigDecimal amount = (principal.multiply(onePlusRPowerMultiplyRate)).divide(onePlusRPowerMinusOne, 2, RoundingMode.HALF_UP);
        return amount.toString();
    }

    @Override
    public Object getLoanBalances(String customerBranch, String customerNo, String userCredentials) {
        //Generate the OFS string
        String ofsRequest = "ENQUIRY.SELECT,," + userCredentials + "/" + customerBranch + "," + loanPortfolioVersion + ",";
        String middlewareResponse = postToT24(ofsRequest);

        //Check for errors
        String validationResponse = validateT24Response(middlewareResponse);
        if (validationResponse != null) {
            return validationResponse;
        }

        try {
            String stringReplace = middlewareResponse.replace("\\t", "*").replace("\"", "").replace("\\,", "|");
            String[] splitString = stringReplace.split("\\|");

            Collection<PortfolioPayload> statementData = new ArrayList<>();
            List<PortfolioPayload> balances = new ArrayList<>();
            for (int i = 0; i < splitString.length; i++) {
                if (i > 1) {
                    String[] fields = splitString[i].replace("\\", "").replace("\"", "").split("\\*");
                    if (fields.length > 3) {
                        if (fields[0].trim().equalsIgnoreCase(customerNo.trim())) {
                            PortfolioPayload payload = new PortfolioPayload();
                            payload.setLoanBalance(fields[21].trim());
                            payload.setLoanId(fields[2].trim());
                            balances.add(payload);
                        }
                    }
                }
            }
            return balances;
        } catch (Exception ex) {
            return ex.getMessage();
        }
    }

    @Override
    public String checkAdminIP(String ipAddress) {
        OmniResponsePayload ex = new OmniResponsePayload();
        String[] ipAddresses = adminIP.split(",");
        boolean remoteIPAccepted = true;
        for (String ip : ipAddresses) {
            if (ipAddress.trim().equals(ip)) {
                remoteIPAccepted = true;
            }
        }
        if (!remoteIPAccepted) {
            ex.setResponseCode(ResponseCodes.IP_BANNED_CODE.getResponseCode());
            ex.setResponseMessage(messageSource.getMessage("appMessages.ip.banned", new Object[0], Locale.ENGLISH));
            String response = gson.toJson(ex);
            return response;
        }
        return null;
    }

    @Override
    public boolean namesMatching(String[] namesToMatch, String[] namesToCompare) {
        int nameMatch = 0;
        List<String> firstNameArray = Arrays.asList(namesToMatch);
        for (String name : namesToCompare) {
            if (firstNameArray.contains(name != null ? name.trim() : "")) {
                nameMatch++;
            }
        }

        return nameMatch >= 2;
    }

    @Override
    public String formatAmountWithComma(String amaountToFormat) {
        if (amaountToFormat == null || amaountToFormat.equals("")) {
            return "";
        }

        if (!amaountToFormat.matches("[0-9.]{1,}")) {
            return amaountToFormat;
        }

        double value = new Double(amaountToFormat.replace(",", ""));
        NumberFormat nf = NumberFormat.getNumberInstance();
        nf.setMaximumFractionDigits(2);
        nf.setRoundingMode(RoundingMode.FLOOR);
        String formattedAmount = nf.format(value);
        return formattedAmount;
    }

    @Override
    public String generateTransRef(String transType) {
        long number = (long) Math.floor(Math.random() * 9_000_000_000L) + 1_000_000_000L;
        return transType + number;
    }

    @Override
    public List<LoanRepaymentSchedule> generateLoanRepaymentSchedule(Loan loan) {
        List<LoanRepaymentSchedule> repaymentSchedule = new ArrayList<>();
        double rate = loan.getLoanSetup().getInterestRate() / 100;
        int tenor = Integer.valueOf(loan.getLoanTenor());
        double presentValue = loan.getLoanAmountApproved().doubleValue();
        repaymentSchedule = repaymentSchedule(rate, tenor, presentValue);
        return repaymentSchedule;
    }

    private static double calculateSimpleInterest(double rate, double noOfPayments, double presentValue) {
        //double percent = 100;
        double S_Iint = (rate * (noOfPayments / 12) * presentValue);
        return S_Iint;
    }

    private static double monthlyInterest(double SI, double noOfPayments) {
        double M_Int = SI / noOfPayments;
        return M_Int;
    }

    //This will return the PMT for the current month
    private static double calculatePMT(double rate, double noOfPayments, double presentValue) {
        double a = (1 + (rate / 12));
        double b = (-(noOfPayments / 12) * 12);
        double PMT = (presentValue * (rate / 12)) / (1 - Math.pow(a, b));
        return PMT;
    }

    private static List<LoanRepaymentSchedule> repaymentSchedule(double rate, double noOfPayments, double loanAmount) {
        DecimalFormat df = new DecimalFormat("0.00");
        LocalDate startDate = LocalDate.now();
        List<LoanRepaymentSchedule> pmt = new ArrayList<>();
        double PMT = 0; //variable for PMT
        double monthlyInterest = 0; //variable for monthly interest
        double sInt = 0; //variable for simple interest
        double monthlyPrincipal = 0; //variable for monthly principal excluding interest
        //double prinWithInt = 0; //variable for outstanding principal
        double presentValue = loanAmount;
        String np = String.valueOf(noOfPayments);
        String[] str = np.split("\\.");
        String nper = str[0];
        int i = Integer.valueOf(nper);
        for (int j = 1; j <= i; j++) {
            LoanRepaymentSchedule repay = new LoanRepaymentSchedule();
            //Calculate PMT
            PMT = calculatePMT(rate, noOfPayments, presentValue);

            //Calculate simple interest
            sInt = calculateSimpleInterest(rate, noOfPayments, presentValue);

            //total loan repayment
            //prinWithInt = pv + sInt;
            //Monthly Interest
            monthlyInterest = monthlyInterest(sInt, noOfPayments);

            //calculate outstadin
            monthlyPrincipal = PMT - monthlyInterest;

            //Add to list
            repay.setPaymentDate(startDate.getMonth() + "-" + startDate.getYear());
            repay.setInterestPayment(String.valueOf(Double.valueOf(df.format(monthlyInterest))));
            repay.setPrinciplPayment(String.valueOf(Double.valueOf(df.format(monthlyPrincipal))));
            repay.setTotalPaymenet(String.valueOf(Double.valueOf(df.format(PMT))));
            repay.setBalance(String.valueOf(df.format(presentValue - monthlyPrincipal)));
            repay.setOutstanding(String.valueOf(df.format(presentValue)));
            pmt.add(repay);

            //Set outstanding principal
            presentValue = presentValue - monthlyPrincipal;
            noOfPayments = noOfPayments - 1;
            startDate = startDate.plusMonths(1);
        }
        return pmt;
    }

    @Override
    public boolean addressMatch(Customer customer) {
        boolean addressMatch = false;
        List<CreditBureauAddress> addressList = loanRepository.getCustomerAddress(customer);
        if (addressList != null) {
            for (CreditBureauAddress add : addressList) {
                if (add.getAddress().contains(customer.getResidenceAddress())) {
                    addressMatch = true;
                }
            }
            return addressMatch;
        }
        return false;
    }

    @Override
    public int getInquiryInLastThreeMonths(Customer customer) {
        int inquiry = 0;
        List<CreditBureauCreditRisk> inquiryList = loanRepository.getCRCCreditRiskUsingCustomer(customer);
        if (inquiryList != null) {
            for (CreditBureauCreditRisk month : inquiryList) {
                if (month.getIndicatorType().equalsIgnoreCase("INQUIRY_COUNT_3M")) {
                    inquiry += Integer.parseInt(month.getIndicatorValue());
                }
            }
            return inquiry;
        }
        return 0;
    }

    @Override
    public int getDishonouredCheque(Customer customer) {
        int dishonouredCheque = 0;
        List<CreditBureauCreditRisk> dishornouredCheque = loanRepository.getCRCCreditRiskUsingCustomer(customer);
        if (dishornouredCheque != null) {
            for (CreditBureauCreditRisk ch : dishornouredCheque) {
                if (ch.getIndicatorType().equalsIgnoreCase("CHEQUE_DISHONOURS")) {
                    dishonouredCheque += Integer.parseInt(ch.getIndicatorValue());
                }
            }
            return dishonouredCheque;
        }
        return 0;
    }

    @Override
    public int getMaxOverdueDays(Customer customer) {
        int daysOverdue = 0;
        List<CreditBureauCreditRisk> dueDay = loanRepository.getCRCCreditRiskUsingCustomer(customer);
        if (dueDay != null) {
            for (CreditBureauCreditRisk day : dueDay) {
                if (day.getIndicatorType().equalsIgnoreCase("MAX_OVERDUE_DAYS")) {
                    daysOverdue += Integer.parseInt(day.getIndicatorValue());
                }
            }
            return daysOverdue;
        }
        return 0;
    }

    @Override
    public int getSuitAndWriteOff(Customer customer) {
        int daysOverdue = 0;
        List<CreditBureauCreditRisk> suitFiled = loanRepository.getCRCCreditRiskUsingCustomer(customer);
        if (suitFiled != null) {
            for (CreditBureauCreditRisk day : suitFiled) {
                if (day.getIndicatorType().trim().equalsIgnoreCase("SUITS_FILED")) {
                    daysOverdue += Integer.parseInt(day.getIndicatorValue());
                }
            }
            return daysOverdue;
        }
        return 0;
    }

    @Override
    public int getMaxOverdueFacility(Customer customer) {
        int overdueFacility = 0;
        List<CreditBureauCreditRisk> suitFiled = loanRepository.getCRCCreditRiskUsingCustomer(customer);
        if (suitFiled != null) {
            for (CreditBureauCreditRisk day : suitFiled) {
                if (day.getIndicatorType().trim().equalsIgnoreCase("MAX_OVERDUE_FACILITY")) {
                    overdueFacility += Integer.parseInt(day.getIndicatorValue());
                }
            }
            return overdueFacility;
        }
        return 0;
    }

    @Override
    public BigDecimal getDeliquentLoan(Customer customer) {
        BigDecimal totalCount = BigDecimal.ZERO;
        BigDecimal nonPerformingCount = BigDecimal.ZERO;

        List<CreditBureauPerformanceSummary> loan = loanRepository.getCreditBureauPerformanceSummaryUsingCustomer(customer);
        if (loan != null) {
            for (CreditBureauPerformanceSummary d : loan) {
                if (!d.getFacilityCount().equals("")) {
                    totalCount = totalCount.add(new BigDecimal(d.getFacilityCount()));
                }
                if (!d.getNonPerformingFacility().equals("")) {
                    nonPerformingCount = nonPerformingCount.add(new BigDecimal(d.getNonPerformingFacility()));
                }
            }
            //Check for division by zero
            if (totalCount.compareTo(BigDecimal.ZERO) <= 0) {
                return BigDecimal.ZERO;
            }
            BigDecimal percent = new BigDecimal("100");
            BigDecimal nonPerformancePercent = BigDecimal.ZERO;
            nonPerformancePercent = nonPerformingCount.multiply(percent).divide(totalCount, 2, RoundingMode.CEILING);
            return nonPerformancePercent;
        }
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal getMaxOverdueAmount(Customer customer) {
        BigDecimal overdueAmount = BigDecimal.ZERO;
        BigDecimal outstandingBalance = BigDecimal.ZERO;
        List<CreditBureauPerformanceSummary> performanceDetails = loanRepository.getCreditBureauPerformanceSummaryUsingCustomer(customer);
        if (performanceDetails != null) {
            for (CreditBureauPerformanceSummary amo : performanceDetails) {
                if (!amo.getAccountBalance().equalsIgnoreCase("")) {
                    outstandingBalance = outstandingBalance.add(new BigDecimal(amo.getAccountBalance().replace(",", "")));
                }
                if (!amo.getOverdueAmount().equalsIgnoreCase("")) {
                    overdueAmount = overdueAmount.add(new BigDecimal(amo.getOverdueAmount().replace(",", "")));
                }
            }
            //Check for division by zero
            if (outstandingBalance.compareTo(BigDecimal.ZERO) <= 0) {
                return BigDecimal.ZERO;
            }
            BigDecimal maxOverdueAmt = overdueAmount.multiply(new BigDecimal("100")).divide(outstandingBalance, 2, RoundingMode.CEILING);
            return maxOverdueAmt;
        }
        return BigDecimal.ZERO;
    }

    @Override
    public void createPdf(String dest, String text) throws IOException {
        PdfDocument pdf = new PdfDocument(new PdfWriter(dest));
        Document document = new Document(pdf);
        document.setTextAlignment(TextAlignment.LEFT);
        document.setFontSize(8.0F);
        document.setLeftMargin(3.0F);
        document.setRightMargin(3.0F);
        document.setBottomMargin(3.0F);
        document.setTopMargin(3.0F);
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(text), "UTF8"));
        PdfFont normal = PdfFontFactory.createFont("Courier");
        Paragraph para = new Paragraph();
        para.setFont(normal);
        String line;
        while ((line = br.readLine()) != null) {
            line = line.replace("\u0020", "\u00A0");
            para.add(line + "\n");
        }
        document.add(para);
        document.close();
        br.close();
    }

    @Override
    public String hashEmailNotificationRequest(EmailRequestPayload requestPayload) {
        StringJoiner rawString = new StringJoiner(":");
        rawString.add(requestPayload.getMobileNumber().trim());
        rawString.add(requestPayload.getRecipientName().trim());
        rawString.add(requestPayload.getRecipientEmail().trim());
        rawString.add(requestPayload.getAttachmentFilePath().trim());
        rawString.add(requestPayload.getRequestId().trim());
        return encryptString(rawString.toString(), requestPayload.getToken());
    }

    @Override
    public String hashSMSNotificationRequest(SMSRequestPayload requestPayload) {
        StringJoiner rawString = new StringJoiner(":");
        rawString.add(requestPayload.getMobileNumber().trim());
        rawString.add(requestPayload.getAccountNumber().trim());
        rawString.add(requestPayload.getMessage().trim());
        rawString.add(requestPayload.getSmsFor().trim());
        rawString.add(requestPayload.getRequestId().trim());
        return encryptString(rawString.toString(), requestPayload.getToken());
    }

    @Async
    @Override
    public CompletableFuture<String> sendDebitSMS(NotificationPayload requestPayload) {
        StringBuilder smsMessage = new StringBuilder();
        smsMessage.append("Debit of N");
        smsMessage.append(requestPayload.getAmount());
        smsMessage.append(" on ").append(requestPayload.getAccountNumber());
        smsMessage.append(" at ").append(requestPayload.getBranch());
        smsMessage.append(" on ").append(requestPayload.getTransDate());
        smsMessage.append(" by ").append(requestPayload.getTransTime()).append(". ").append(requestPayload.getNarration());
        smsMessage.append(". Balance N").append(requestPayload.getAccountBalance());
        smsMessage.append(". Info 07000222466, #StaySafe.");

        SMSRequestPayload smsRequest = new SMSRequestPayload();
        smsRequest.setMobileNumber(requestPayload.getMobileNumber());
        smsRequest.setAccountNumber(requestPayload.getAccountNumber());
        smsRequest.setMessage(smsMessage.toString());
        smsRequest.setSmsFor(requestPayload.getSmsFor());
        smsRequest.setSmsType("D");
        smsRequest.setRequestId(requestPayload.getRequestId());
        smsRequest.setHash(hashSMSNotificationRequest(smsRequest));

        String requestJson = gson.toJson(smsRequest);
//        notificationService.smsNotification(requestPayload.getToken(), requestJson);  //Disabled as requested by Emmanuel Akhigbe 2022-06-06
        return CompletableFuture.completedFuture("");
    }

    @Async
    @Override
    public CompletableFuture<String> sendCreditSMS(NotificationPayload requestPayload) {
        StringBuilder smsMessage = new StringBuilder();
        smsMessage.append("Credit of N");
        smsMessage.append(requestPayload.getAmount());
        smsMessage.append(" on ").append(requestPayload.getAccountNumber());
        smsMessage.append(" at ").append(requestPayload.getBranch());
        smsMessage.append(" on ").append(requestPayload.getTransDate());
        smsMessage.append(" by ").append(requestPayload.getTransTime()).append(". ").append(requestPayload.getNarration());
        smsMessage.append(". Balance N").append(requestPayload.getAccountBalance());
        smsMessage.append(". Info 07000222466, #StaySafe.");

        SMSRequestPayload smsRequest = new SMSRequestPayload();
        smsRequest.setMobileNumber(requestPayload.getMobileNumber());
        smsRequest.setAccountNumber(requestPayload.getAccountNumber());
        smsRequest.setMessage(smsMessage.toString());
        smsRequest.setSmsFor(requestPayload.getSmsFor());
        smsRequest.setSmsType("C");
        smsRequest.setRequestId(requestPayload.getRequestId());
        smsRequest.setHash(hashSMSNotificationRequest(smsRequest));

        String requestJson = gson.toJson(smsRequest);
//        notificationService.smsNotification(requestPayload.getToken(), requestJson);  //Disabled as requested by Emmanuel Akhigbe 2022-06-06
        return CompletableFuture.completedFuture("");
    }

    @Async
    @Override
    public CompletableFuture<String> sendRubyxLoanRenewalSMS(NotificationPayload requestPayload) {
        BigDecimal rate = new BigDecimal(requestPayload.getInterestRate() / 100);
        BigDecimal one = BigDecimal.ONE;
        BigDecimal onePlusR = rate.add(one);
        BigDecimal onePlusRPower = onePlusR.pow(requestPayload.getTenor());
        BigDecimal onePlusRPowerMinusOne = onePlusRPower.subtract(BigDecimal.ONE);
        BigDecimal onePlusRPowerMultiplyRate = onePlusRPower.multiply(rate);
        BigDecimal monthlyRepayment = (requestPayload.getAmountApproved().multiply(onePlusRPowerMultiplyRate)).divide(onePlusRPowerMinusOne, 2, RoundingMode.HALF_UP);

        StringBuilder smsMessage = new StringBuilder();
        smsMessage.append("Dear Customer, Congratulations! You have been pre-approved for a loan of N");
        smsMessage.append(requestPayload.getAmountApproved().toString());
        smsMessage.append(" for ").append(requestPayload.getTenor());
//        smsMessage.append(" months ");
//        smsMessage.append(" on ").append(LocalTime.now().getHour()).append(":").append(LocalTime.now().getMinute());
//        smsMessage.append(". Repayment amt N").append(monthlyRepayment.toString());
        smsMessage.append(". Kindly dial *572*7*3# to apply or call 07000222466 for details.");

        SMSRequestPayload smsRequest = new SMSRequestPayload();
        smsRequest.setMobileNumber(requestPayload.getMobileNumber());
        smsRequest.setAccountNumber("0123456789");   //Defaulted to bypass billing
        smsRequest.setMessage(smsMessage.toString());
        smsRequest.setSmsFor("LOAN");
        smsRequest.setSmsType("N");
        smsRequest.setRequestId(requestPayload.getRequestId());
        smsRequest.setToken(requestPayload.getToken());
        smsRequest.setHash(hashSMSNotificationRequest(smsRequest));

        String requestJson = gson.toJson(smsRequest);
        notificationService.smsNotification(requestPayload.getToken(), requestJson);
        return CompletableFuture.completedFuture("Success");
    }

    @Async
    @Override
    public CompletableFuture<String> sendLoanSMS(NotificationPayload requestPayload) {
        BigDecimal rate = new BigDecimal(requestPayload.getInterestRate() / 100);
        BigDecimal one = BigDecimal.ONE;
        BigDecimal onePlusR = rate.add(one);
        BigDecimal onePlusRPower = onePlusR.pow(requestPayload.getTenor());
        BigDecimal onePlusRPowerMinusOne = onePlusRPower.subtract(BigDecimal.ONE);
        BigDecimal onePlusRPowerMultiplyRate = onePlusRPower.multiply(rate);
        BigDecimal monthlyRepayment = (requestPayload.getAmountApproved().multiply(onePlusRPowerMultiplyRate)).divide(onePlusRPowerMinusOne, 2, RoundingMode.HALF_UP);

        StringBuilder smsMessage = new StringBuilder();
        smsMessage.append("Disbursement of N");
        smsMessage.append(requestPayload.getAmountApproved().toString());
        smsMessage.append(" on your ").append(requestPayload.getDisbursementAccount());
        smsMessage.append(" account at ").append(LocalDate.now().toString());
        smsMessage.append(" on ").append(LocalTime.now().getHour()).append(":").append(LocalTime.now().getMinute());
        smsMessage.append(". Repayment amt N").append(monthlyRepayment.toString());
        smsMessage.append(". Maturity date ").append(requestPayload.getMaturedAt().toString()).append("#StaySafe");

        SMSRequestPayload smsRequest = new SMSRequestPayload();
        smsRequest.setMobileNumber(requestPayload.getMobileNumber());
        smsRequest.setAccountNumber(requestPayload.getDisbursementAccount());
        smsRequest.setMessage(smsMessage.toString());
        smsRequest.setSmsFor("LOAN");
        smsRequest.setSmsType("N");
        smsRequest.setRequestId(requestPayload.getRequestId());
//        smsRequest.setHash(hashSMSNotificationRequest(smsRequest));

        String requestJson = gson.toJson(smsRequest);
        notificationService.smsNotification(requestPayload.getToken(), requestJson);
        return CompletableFuture.completedFuture("Success");
    }

    @Async
    public CompletableFuture<String> sendStaffLoanOfferEmail(NotificationPayload requestPayload) {
        EmailRequestPayload emailRequest = new EmailRequestPayload();
        emailRequest.setMobileNumber(requestPayload.getMobileNumber());
        emailRequest.setRecipientName(requestPayload.getRecipientName());
        emailRequest.setRecipientEmail(requestPayload.getRecipientEmail());
        emailRequest.setAttachmentFilePath("");
        emailRequest.setRequestId(requestPayload.getRequestId());
        emailRequest.setHash(hashEmailNotificationRequest(emailRequest));

        //Add the rest of the payload
        emailRequest.setEmailBody(staffLoanMessageBody(requestPayload));
        emailRequest.setEmailLoginPassword(mailUsername);
        emailRequest.setEmailLoginUsername(mailPassword);

        emailRequest.setSubject("Loan Offer Letter");
        String fileName = generateStaffLoanOfferLetter(requestPayload);
        emailRequest.setAttachmentFilePath(fileName);
        String requestJson = gson.toJson(emailRequest);
        notificationService.emailNotification(requestPayload.getToken(), requestJson);
        return CompletableFuture.completedFuture("Success");
    }

    private String staffLoanMessageBody(NotificationPayload requestPayload) {
        String message = "<img style=\"text-align:center;margin-right:40%;margin-left:40%;\" src=\"http://accionmfb.com/images/about/logo.png\" width=\"200\" height=\"110\" /><hr/>"
                + "<div style=\"width:80%;height:100%;\"><br/>"
                + "<b>Loan Request Notification!</b><br/>"
                + "<b>Dear, " + requestPayload.getRecipientName() + "</b><br/>"
                + "<p>You have a pending loan authorization. See details below; </p>"
                + "<p>Employee : <b style=\"color:#ea7600;\">" + requestPayload.getEmployeeId() + "</b></p>"
                + "<p>Loan Type : <b style=\"color:#ea7600;\">" + requestPayload.getLoanType() + "</b></p>"
                + "<p>Loan ID : <b style=\"color:#ea7600;\">" + requestPayload.getLoanId() + "</b></p>"
                + "<p>Loan Amount : <b style=\"color:#ea7600;\">" + requestPayload.getLoanAmount() + "</b></p>"
                + "<p>Remarks : <b style=\"color:#ea7600;\">" + requestPayload.getRemarks() + "</b></p>"
                + "<br/><br/>"
                + "<img src=\"http://accionmfb.com/images/service/footer.gif\" alt=\"Email footer\" width=\"500\" height=\"100\" \\>\n"
                + "<p>Our channels are always available to you</p>"
                + "<p>For enquiries and complains, you can reach out to our contact center on +234 01-252-764 | 070 000 222 466. www.accionmfb.com</p>"
                + "</div>";
        return message;
    }

    private String generateStaffLoanOfferLetter(NotificationPayload requestPayload) {
        try {
            String destinationDirectory = tempDirectory + File.separator + "offerletter" + File.separator;
            File file = new File(destinationDirectory + requestPayload.getLoanId() + ".pdf");
            file.createNewFile();
            String dest = destinationDirectory + requestPayload.getLoanId() + ".pdf";
            PdfWriter writer = new PdfWriter(dest);
            PdfDocument pdf = new PdfDocument(writer);
            Document doc = new Document(pdf);
            doc.setFontSize(6f);

            String staffName = requestPayload.getRecipientName();
            //Add the date 
            Paragraph pg1 = new Paragraph(LocalDate.now().toString())
                    .setFontColor(new DeviceRgb(0, 0, 0))
                    .setFontSize(6f).setTextAlignment(TextAlignment.JUSTIFIED);
            doc.add(pg1);

            Paragraph pg3 = new Paragraph("Accion Microfinance Bank Limited")
                    .setFontColor(new DeviceRgb(0, 0, 0))
                    .setFontSize(6f).setTextAlignment(TextAlignment.JUSTIFIED);
            doc.add(pg3);

            Paragraph pg4 = new Paragraph(requestPayload.getBranchName() + ", " + requestPayload.getBranchState())
                    .setFontColor(new DeviceRgb(0, 0, 0))
                    .setFontSize(6f).setTextAlignment(TextAlignment.JUSTIFIED);
            doc.add(pg4);

            Paragraph pg5 = new Paragraph("Dear " + staffName)
                    .setFontColor(new DeviceRgb(0, 0, 0))
                    .setFontSize(6f).setTextAlignment(TextAlignment.JUSTIFIED);
            doc.add(pg5);

            Paragraph pg6 = new Paragraph("OFFER LETTER - PERSONAL LOAN")
                    .setFontColor(new DeviceRgb(0, 0, 0))
                    .setFontSize(6f).setTextAlignment(TextAlignment.JUSTIFIED)
                    .setUnderline();
            doc.add(pg6);

            Paragraph pg7 = new Paragraph("Your Personal Loan application refers")
                    .setFontColor(new DeviceRgb(0, 0, 0))
                    .setFontSize(6f).setTextAlignment(TextAlignment.JUSTIFIED);
            doc.add(pg7);

            Paragraph pg8 = new Paragraph("The Management is pleased to offer you the above loan under the following terms and conditions:")
                    .setFontColor(new DeviceRgb(0, 0, 0))
                    .setFontSize(6f).setTextAlignment(TextAlignment.JUSTIFIED);
            doc.add(pg8);

            Paragraph pg9 = new Paragraph("Lender : Accion Microfinance Bank Limited")
                    .setFontColor(new DeviceRgb(0, 0, 0))
                    .setFontSize(6f).setTextAlignment(TextAlignment.JUSTIFIED);
            doc.add(pg9);

            Paragraph pg10 = new Paragraph("Borrower : " + staffName)
                    .setFontColor(new DeviceRgb(0, 0, 0))
                    .setFontSize(6f).setTextAlignment(TextAlignment.JUSTIFIED);
            doc.add(pg10);

            Paragraph pg11 = new Paragraph("Facility : Car Loan")
                    .setFontColor(new DeviceRgb(0, 0, 0))
                    .setFontSize(6f).setTextAlignment(TextAlignment.JUSTIFIED);
            doc.add(pg11);

            Paragraph pg12 = new Paragraph("Amount: N" + formatAmountWithComma(requestPayload.getLoanApprovedAmount()))
                    .setFontColor(new DeviceRgb(0, 0, 0))
                    .setFontSize(6f).setTextAlignment(TextAlignment.JUSTIFIED);
            doc.add(pg12);

            Paragraph pg13 = new Paragraph("Tenor : " + requestPayload.getLoanTenor() + " month(s)")
                    .setFontColor(new DeviceRgb(0, 0, 0))
                    .setFontSize(6f).setTextAlignment(TextAlignment.JUSTIFIED);
            doc.add(pg13);

            Paragraph pg14 = new Paragraph("Repayment: As per attached repayment schedule")
                    .setFontColor(new DeviceRgb(0, 0, 0))
                    .setFontSize(6f).setTextAlignment(TextAlignment.JUSTIFIED);
            doc.add(pg14);

            Paragraph pg15 = new Paragraph("Interest Rate : " + requestPayload.getInterestRate() + "% per annum")
                    .setFontColor(new DeviceRgb(0, 0, 0))
                    .setFontSize(6f).setTextAlignment(TextAlignment.JUSTIFIED);
            doc.add(pg15);

            Paragraph pg16 = new Paragraph("Security / Collateral : ")
                    .setFontColor(new DeviceRgb(0, 0, 0))
                    .setFontSize(6f).setTextAlignment(TextAlignment.JUSTIFIED)
                    .setUnderline();
            doc.add(pg16);

            Paragraph pg17 = new Paragraph("1.	Charge over your basic allowance for 12  months commencing from December 2020")
                    .setFontColor(new DeviceRgb(0, 0, 0))
                    .setFontSize(6f).setTextAlignment(TextAlignment.JUSTIFIED);
            doc.add(pg17);

            Paragraph pg18 = new Paragraph("2.	Employment guarantor in place;")
                    .setFontColor(new DeviceRgb(0, 0, 0))
                    .setFontSize(6f).setTextAlignment(TextAlignment.JUSTIFIED);
            doc.add(pg18);

            Paragraph pg19 = new Paragraph("3.	Outstanding amount becomes payable in full upon your resignation of appointment from Accion;")
                    .setFontColor(new DeviceRgb(0, 0, 0))
                    .setFontSize(6f).setTextAlignment(TextAlignment.JUSTIFIED);
            doc.add(pg19);

            Paragraph pg20 = new Paragraph("Although this loan has been granted for a stated period on stated terms and conditions, the bank in accordance with general banking practice reserves the right to reverse, revise or cancel this loan at any time and without notice should any circumstance arise which in the sole opinion of the bank justifies such a course of action")
                    .setFontColor(new DeviceRgb(0, 0, 0))
                    .setFontSize(6f).setTextAlignment(TextAlignment.JUSTIFIED);
            doc.add(pg20);

            Paragraph pg26 = new Paragraph("Please indicate your acceptance by executing the memorandum of acceptance below and forwarding the enclosed copy to the bank")
                    .setFontColor(new DeviceRgb(0, 0, 0))
                    .setFontSize(6f).setTextAlignment(TextAlignment.JUSTIFIED);
            doc.add(pg26);

            Paragraph pg27 = new Paragraph("Yours Sincerely,")
                    .setFontColor(new DeviceRgb(0, 0, 0))
                    .setFontSize(6f).setTextAlignment(TextAlignment.JUSTIFIED);
            doc.add(pg27);

            Paragraph pg28 = new Paragraph("For: ACCION MICROFINANCE BANK LIMITED")
                    .setFontColor(new DeviceRgb(0, 0, 0))
                    .setFontSize(6f).setTextAlignment(TextAlignment.JUSTIFIED);
            doc.add(pg28);

            Paragraph pg29 = new Paragraph(" ")
                    .setFontColor(new DeviceRgb(0, 0, 0))
                    .setFontSize(6f).setTextAlignment(TextAlignment.JUSTIFIED);
            doc.add(pg29);

            Paragraph pg30 = new Paragraph("MOYINOLUWA OLUSADA                                              AROME IDACHABA")
                    .setFontColor(new DeviceRgb(0, 0, 0))
                    .setFontSize(6f).setTextAlignment(TextAlignment.JUSTIFIED);
            doc.add(pg30);

            Paragraph pg31 = new Paragraph("Human Resources                                                Team Lead (Comp, Benefits & HRIS)")
                    .setFontColor(new DeviceRgb(0, 0, 0))
                    .setFontSize(6f).setTextAlignment(TextAlignment.JUSTIFIED);
            doc.add(pg31);

            Paragraph pg32 = new Paragraph("")
                    .setFontColor(new DeviceRgb(0, 0, 0))
                    .setFontSize(6f).setTextAlignment(TextAlignment.JUSTIFIED)
                    .setUnderline();
            doc.add(pg32);

            Paragraph pg33 = new Paragraph("MEMORANDUM OF ACCEPTANCE")
                    .setFontColor(new DeviceRgb(0, 0, 0))
                    .setFontSize(6f).setTextAlignment(TextAlignment.CENTER)
                    .setUnderline();
            doc.add(pg33);

            Paragraph pg34 = new Paragraph("“By signing this offer letter and by drawing on the loan,")
                    .setFontColor(new DeviceRgb(0, 0, 0))
                    .setFontSize(6f).setTextAlignment(TextAlignment.JUSTIFIED);
            doc.add(pg34);

            Paragraph pg35 = new Paragraph("I, ……………….……………………" + staffName + "………………………………………………………………")
                    .setFontColor(new DeviceRgb(0, 0, 0))
                    .setFontSize(6f).setTextAlignment(TextAlignment.JUSTIFIED);
            doc.add(pg35);

            Paragraph pg36 = new Paragraph("i.	Agree to accept the loan granted and confirm that the conditions listed in this offer letter are acceptable to me.")
                    .setFontColor(new DeviceRgb(0, 0, 0))
                    .setFontSize(6f).setTextAlignment(TextAlignment.JUSTIFIED);
            doc.add(pg36);

            Paragraph pg37 = new Paragraph("ii.	Covenant to repay the loan as and when due. In the event that I fail to repay the loan as agreed, and the loan becomes delinquent, the Bank shall have the right to report the delinquent loan to credit bureaus, the CBN through the Credit Risk management System (CRMS), or by any other means, and request the CBN to exercise its regulatory power to direct all banks and other financial institutions under its regulatory purview to set-off my indebtedness from any money standing to my credit in any bank account and from any other financial assets they may be holding for my benefit")
                    .setFontColor(new DeviceRgb(0, 0, 0))
                    .setFontSize(6f).setTextAlignment(TextAlignment.JUSTIFIED);
            doc.add(pg37);

            Paragraph pg38 = new Paragraph("iii.	Covenant and warrant that the Bank shall have power to set-off my indebtedness under this loan agreement from all such monies and funds standing to my credit/benefit in any and all such accounts or from any other financial assets belonging to me and in the custody of any such bank")
                    .setFontColor(new DeviceRgb(0, 0, 0))
                    .setFontSize(6f).setTextAlignment(TextAlignment.JUSTIFIED);
            doc.add(pg38);

            Paragraph pg39 = new Paragraph("iv.	Hereby waive any right of confidentiality whether arising under common law or statute or in any other manner whatsoever and irrevocably agree that I shall not argue to the contrary before any court of law, tribunal, administrative authority or any other body acting in any judicial or quasi-judicial capacity")
                    .setFontColor(new DeviceRgb(0, 0, 0))
                    .setFontSize(6f).setTextAlignment(TextAlignment.JUSTIFIED);
            doc.add(pg39);

            Paragraph pg40 = new Paragraph("v.	Agree that the loan shall become immediately repayable should I leave the service of the bank for any reason before the maturity of the loan and after 3 months of exiting the Bank, the interest chargeable shall be at commercial rate subject to prevailing market forces")
                    .setFontColor(new DeviceRgb(0, 0, 0))
                    .setFontSize(6f).setTextAlignment(TextAlignment.JUSTIFIED);
            doc.add(pg40);

            // Creating a table       
            float[] pointColumnWidths = {250F, 250F, 250F, 250F, 250F, 250F};
            Table table = new Table(pointColumnWidths);
            Paragraph para = new Paragraph("Loan repayment Schedule")
                    .setFontColor(new DeviceRgb(0, 0, 0))
                    .setFontSize(6f).setTextAlignment(TextAlignment.CENTER);
            doc.add(para);

            table.addCell(new Cell().add("Repayment Month"));
            table.addCell(new Cell().add("Principal"));
            table.addCell(new Cell().add("Repayment"));
            table.addCell(new Cell().add("Interest"));
            table.addCell(new Cell().add("Principal & Interest"));
            table.addCell(new Cell().add("Balance"));

            List<LoanRepaymentSchedule> repayment = generateLoanRepaymentSchedule(null);
            for (LoanRepaymentSchedule r : repayment) {
                table.addCell(new Cell().add(r.getPaymentDate()));
                table.addCell(new Cell().add(r.getOutstanding()));
                table.addCell(new Cell().add(r.getPrinciplPayment()));
                table.addCell(new Cell().add(r.getInterestPayment()));
                table.addCell(new Cell().add(r.getTotalPaymenet()));
                table.addCell(new Cell().add(r.getBalance()));
            }

            doc.add(table);

            doc.close();
            return tempDirectory + File.separator + "offerletter" + File.separator + requestPayload.getLoanId() + ".pdf";
        } catch (Exception ex) {
            return ex.getMessage();
        }
    }

    @Override
    @Async
    public CompletableFuture<String> sendArtisanLoanOfferEmail(NotificationPayload requestPayload) {
        EmailRequestPayload emailRequest = new EmailRequestPayload();
        emailRequest.setMobileNumber(requestPayload.getMobileNumber());
        emailRequest.setRecipientName(requestPayload.getRecipientName());
        emailRequest.setRecipientEmail(requestPayload.getRecipientEmail());
        emailRequest.setAttachmentFilePath("");
        emailRequest.setRequestId(requestPayload.getRequestId());
        emailRequest.setHash(hashEmailNotificationRequest(emailRequest));

        //Generate the content for the body of the email
        String message = "<img style=\"text-align:center;margin-right:40%;margin-left:40%;\" src=\"http://accionmfb.com/images/about/logo.png\" width=\"200\" height=\"110\" /><hr/>"
                + "<div style=\"width:80%;height:100%;\"><br/>"
                + "<b>Loan Offer Letter!</b><br/>"
                + "<b>Dear, " + requestPayload.getRecipientName() + "</b><br/>"
                + "<p>Your application for a " + requestPayload.getAmount() + " "
                + requestPayload.getLoanType() + " with Loan ID: "
                + " has been approved. Find attached the offer letter </p>"
                + "<br/><br/>"
                + "<img src=\"http://accionmfb.com/images/service/footer.gif\" alt=\"Email footer\" width=\"500\" height=\"100\" \\>\n"
                + "<p>Our channels are always available to you</p>"
                + "<p>For enquiries and complains, you can reach out to our contact center on +234 01-252-764 | 070 000 222 466. www.accionmfb.com</p>"
                + "</div>";

        //Add the other part of the payload
        emailRequest.setEmailBody(message);
        emailRequest.setEmailLoginPassword(contactCenterEmailPassword);
        emailRequest.setEmailLoginUsername(contactCenterEmailUsername);
        emailRequest.setSubject("Loan Offer Letter");

        String requestJson = gson.toJson(emailRequest);
        notificationService.emailNotification(requestPayload.getToken(), requestJson);
        return CompletableFuture.completedFuture("Success");
    }

    @Override
    @Async
    public CompletableFuture<String> sendRubyxLoanRenewalEmail(NotificationPayload requestPayload) {
        EmailRequestPayload emailRequest = new EmailRequestPayload();
        emailRequest.setMobileNumber(requestPayload.getMobileNumber());
        emailRequest.setRecipientName(requestPayload.getRecipientName());
        emailRequest.setRecipientEmail(requestPayload.getRecipientEmail());
        emailRequest.setAttachmentFilePath("");
        emailRequest.setRequestId(requestPayload.getRequestId());
        emailRequest.setToken(requestPayload.getToken());
        emailRequest.setHash(hashEmailNotificationRequest(emailRequest));

        //Generate the content for the body of the email
        String message = "<img style=\"text-align:center;margin-right:40%;margin-left:40%;\" src=\"http://accionmfb.com/images/about/logo.png\" width=\"200\" height=\"110\" /><hr/>"
                + "<div style=\"width:80%;height:100%;\"><br/>"
                + "<b>Loan Offer Letter!</b><br/>"
                + "<b>Dear sir/madam,</b><br/>"
                + "<p>A customer has been profiled for a <b>" + "Rubyx Loan Renewal" + "</b> with the details <br/>"
                + "Customer Name                              : " + requestPayload.getLastName() + ", " + requestPayload.getOtherName() + "<br/>"
                + "Mobile Number                              : " + requestPayload.getMobileNumber() + "<br/>"
                + "Loan Amount                                : " + requestPayload.getLoanAmount() + "<br/>"
                + "Loan Tenor                                 : " + requestPayload.getLoanTenor() + "<br/>"
                + "Loan Type                                  : " + requestPayload.getLoanType() + "<br/>"
                + "Total Loans from Credit Bureau             : " + requestPayload.getTotalLoans() + "<br/>"
                + "Total Performing Loans from Credit Bureau  : " + requestPayload.getTotalPerformingLoans() + "<br/>"
                + "Total Loan Balance from Credit Bureau      : " + requestPayload.getTotalLoanBalance() + "<br/>"
                + "Performing Loan Balance from Credit Bureau : " + requestPayload.getTotalPerformingLoanBalance() + "<br/>"
                + "<br/><br/>"
                + "<img src=\"http://accionmfb.com/images/service/footer.gif\" alt=\"Email footer\" width=\"500\" height=\"100\" \\>\n"
                + "<p>Our channels are always available to you</p>"
                + "<p>For enquiries and complains, you can reach out to our contact center on +234 01-252-764 | 070 000 222 466. www.accionmfb.com</p>"
                + "</div>";

        //Add the other part of the payload
        emailRequest.setEmailBody(message);
        emailRequest.setEmailLoginPassword(contactCenterEmailPassword);
        emailRequest.setEmailLoginUsername(contactCenterEmailUsername);
        emailRequest.setSubject("Rubyx Loan Renewal");

        String requestJson = gson.toJson(emailRequest);
        notificationService.emailNotification(requestPayload.getToken(), requestJson);
        return CompletableFuture.completedFuture("Success");
    }

    @Override
    @Async
    public CompletableFuture<String> sendDigitalLoanOfferEmail(NotificationPayload requestPayload) {
        EmailRequestPayload emailRequest = new EmailRequestPayload();
        emailRequest.setMobileNumber(requestPayload.getMobileNumber());
        emailRequest.setRecipientName(requestPayload.getRecipientName());
        emailRequest.setRecipientEmail(requestPayload.getRecipientEmail());
        emailRequest.setAttachmentFilePath("");
        emailRequest.setRequestId(requestPayload.getRequestId());
        emailRequest.setHash(hashEmailNotificationRequest(emailRequest));

        //Generate the content for the body of the email
        String message = "<img style=\"text-align:center;margin-right:40%;margin-left:40%;\" src=\"http://accionmfb.com/images/about/logo.png\" width=\"200\" height=\"110\" /><hr/>"
                + "<div style=\"width:80%;height:100%;\"><br/>"
                + "<b>Loan Offer Letter!</b><br/>"
                + "<b>Dear, " + requestPayload.getRecipientName() + "</b><br/>"
                + "<p>Your application for a " + requestPayload.getAmount() + " "
                + requestPayload.getLoanType() + " with Loan ID: "
                + " has been approved. Find attached the offer letter </p>"
                + "<br/><br/>"
                + "<img src=\"http://accionmfb.com/images/service/footer.gif\" alt=\"Email footer\" width=\"500\" height=\"100\" \\>\n"
                + "<p>Our channels are always available to you</p>"
                + "<p>For enquiries and complains, you can reach out to our contact center on +234 01-252-764 | 070 000 222 466. www.accionmfb.com</p>"
                + "</div>";

        //Add the other part of the payload
        emailRequest.setEmailBody(message);
        emailRequest.setEmailLoginPassword(contactCenterEmailPassword);
        emailRequest.setEmailLoginUsername(contactCenterEmailUsername);
        emailRequest.setSubject("Loan Offer Letter");

        String requestJson = gson.toJson(emailRequest);
        notificationService.emailNotification(requestPayload.getToken(), requestJson);
        return CompletableFuture.completedFuture("Success");
    }

    @Override
    @Async
    public CompletableFuture<String> sendDigitalLoanAcceptanceEmail(NotificationPayload requestPayload) {
        EmailRequestPayload emailRequest = new EmailRequestPayload();
        emailRequest.setMobileNumber(requestPayload.getMobileNumber());
        emailRequest.setRecipientName(requestPayload.getRecipientName());
        emailRequest.setRecipientEmail(requestPayload.getRecipientEmail());
        emailRequest.setAttachmentFilePath("");
        emailRequest.setRequestId(requestPayload.getRequestId());
        emailRequest.setHash(hashEmailNotificationRequest(emailRequest));

        //Generate the content for the body of the email
        String message = "<img style=\"text-align:center;margin-right:40%;margin-left:40%;\" src=\"http://accionmfb.com/images/about/logo.png\" width=\"200\" height=\"110\" /><hr/>"
                + "<div style=\"width:80%;height:100%;\"><br/>"
                + "<b>Digital Loan Request Notification!</b><br/>"
                + "<b>Dear Team Call Center" + "</b><br/>"
                + "<p>Kindly reach out to the Brighta Loan customer with details below for further authentication. </p>"
                + "<p>Customer Name: <b style=\"color:#ea7600;\">" + requestPayload.getRecipientName() + "</b></p>"
                + "<p>Disbursement Account: <b style=\"color:#ea7600;\">" + requestPayload.getDisbursementAccount() + "</b></p>"
                + "<p>Customer Phone Number: <b style=\"color:#ea7600;\">" + requestPayload.getMobileNumber() + "</b></p>"
                + "<p>Loan Process: <b style=\"color:#ea7600;\">" + " Automatic</b></p>"
                + "<p>Loan ID: <b style=\"color:#ea7600;\">" + requestPayload.getLoanId() + "</b></p>"
                + "<p>Loan Amount: N <b style=\"color:#ea7600;\">" + requestPayload.getLoanAmount() + "</b></p>"
                + "<br/><br/>"
                + "<img src=\"http://accionmfb.com/images/service/footer.gif\" alt=\"Email footer\" width=\"500\" height=\"100\" \\>\n"
                + "<p>If you require any assistance, kindly email itservicedesk@accionmfb.com or call ext 127.</p>"
                + "</div>";

        //Add the other part of the payload
        emailRequest.setEmailBody(message);
        emailRequest.setEmailLoginPassword(contactCenterEmailPassword);
        emailRequest.setEmailLoginUsername(contactCenterEmailUsername);

        emailRequest.setSubject("Digital Loan Notification");
        String requestJson = gson.toJson(emailRequest);
        notificationService.emailNotification(requestPayload.getToken(), requestJson);
        return CompletableFuture.completedFuture("Success");
    }

    @Override
    @Async
    public CompletableFuture<String> sendDigitalLoanDeclineEmail(NotificationPayload requestPayload) {
        EmailRequestPayload emailRequest = new EmailRequestPayload();
        emailRequest.setMobileNumber(requestPayload.getMobileNumber());
        emailRequest.setRecipientName(requestPayload.getRecipientName());
        emailRequest.setRecipientEmail(requestPayload.getRecipientEmail());
        emailRequest.setAttachmentFilePath("");
        emailRequest.setRequestId(requestPayload.getRequestId());
        emailRequest.setHash(hashEmailNotificationRequest(emailRequest));

        //Generate the content for the body of the email
        String message = "<img style=\"text-align:center;margin-right:40%;margin-left:40%;\" src=\"http://accionmfb.com/images/about/logo.png\" width=\"200\" height=\"110\" /><hr/>"
                + "<div style=\"width:80%;height:100%;\"><br/>"
                + "<b>Digital Loan Decline Notification!</b><br/>"
                + "<b>Dear Team Call Center" + "</b><br/>"
                + "<p>Kindly reach out to the Brighta Loan customer with details below for further authentication. </p>"
                + "<p>Customer Name: <b style=\"color:#ea7600;\">" + requestPayload.getRecipientName() + "</b></p>"
                + "<p>Disbursement Account: <b style=\"color:#ea7600;\">" + requestPayload.getDisbursementAccount() + "</b></p>"
                + "<p>Customer Phone Number: <b style=\"color:#ea7600;\">" + requestPayload.getMobileNumber() + "</b></p>"
                + "<p>Loan Process: <b style=\"color:#ea7600;\">" + " Automatic</b></p>"
                + "<p>Loan ID: <b style=\"color:#ea7600;\">" + requestPayload.getLoanId() + "</b></p>"
                + "<p>Loan Amount: N <b style=\"color:#ea7600;\">" + requestPayload.getLoanAmount() + "</b></p>"
                + "<br/><br/>"
                + "<img src=\"http://accionmfb.com/images/service/footer.gif\" alt=\"Email footer\" width=\"500\" height=\"100\" \\>\n"
                + "<p>If you require any assistance, kindly email itservicedesk@accionmfb.com or call ext 127.</p>"
                + "</div>";

        //Add the other part of the payload
        emailRequest.setEmailBody(message);
        emailRequest.setEmailLoginPassword(contactCenterEmailPassword);
        emailRequest.setEmailLoginUsername(contactCenterEmailUsername);

        emailRequest.setSubject("Digital Loan Notification");
        String requestJson = gson.toJson(emailRequest);
        notificationService.emailNotification(requestPayload.getToken(), requestJson);
        return CompletableFuture.completedFuture("Success");
    }

    @Override
    @Async
    public CompletableFuture<String> uploadToCreditBureau(String token, Loan loan) {
        DateTimeFormatter dtFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
        DateTimeFormatter dayMonthYearFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        List<CRCDataUploadIndividualPayload> individualList = new ArrayList<>();
        //Start a thread to push data to Credit Bureau
        CRCDataUploadIndividualPayload individualPayload = new CRCDataUploadIndividualPayload();
        individualPayload.setCustomerID(loan.getCustomer().getCustomerNumber());
        individualPayload.setBranchCode(loan.getCustomer().getBranch().getBranchCode());
        individualPayload.setSurname(loan.getCustomer().getLastName());
        String[] otherNames = loan.getCustomer().getOtherName().split(" ");
        individualPayload.setFirstname(otherNames.length >= 1 ? otherNames[0] : "");
        individualPayload.setMiddlename(otherNames.length >= 2 ? otherNames[1] : "");
        individualPayload.setDateofBirth(dayMonthYearFormatter.format(loan.getCustomer().getDob()));
        individualPayload.setNationalIdentityNumber(loan.getCustomer().getNin() == null ? "null" : loan.getCustomer().getNin().getIdNumber());
        individualPayload.setDriversLicenseNo(loan.getCustomer().getDriversLicense() == null ? "null" : loan.getCustomer().getDriversLicense().getIdNumber());
        individualPayload.setBVNNo(loan.getCustomer().getBvn() == null ? "null" : loan.getCustomer().getBvn().getCustomerBvn());
        individualPayload.setPassportNo(loan.getCustomer().getPassport() == null ? "null" : loan.getCustomer().getPassport().getIdNumber());
        individualPayload.setGender(loan.getCustomer().getGender());
        individualPayload.setNationality("Nigeria");
        individualPayload.setMaritalStatus(loan.getCustomer().getMaritalStatus());
        individualPayload.setMobilenumber(loan.getMobileNumber());
        individualPayload.setPrimaryAddressLine1(loan.getCustomer().getResidenceAddress());
        individualPayload.setPrimaryAddressLine2("null");
        individualPayload.setPrimarycity(loan.getCustomer().getResidenceCity());
        individualPayload.setPrimaryState(loan.getCustomer().getResidenceState());
        individualPayload.setPrimaryCountry(loan.getCustomer().getResidenceCountry());
        individualPayload.setEmploymentStatus("null");
        individualPayload.setOccupation("null");
        individualPayload.setBusinessCategory("null");
        individualPayload.setBusinessSector("null");
        individualPayload.setBorrowerType("null");
        individualPayload.setOtherID("null");
        individualPayload.setTaxID("null");
        individualPayload.setPictureFilePath("null");
        individualPayload.setEmailaddress(loan.getCustomer().getEmail());
        individualPayload.setEmployerName("null");
        individualPayload.setEmployerAddressLine1("null");
        individualPayload.setEmployerAddressLine2("null");
        individualPayload.setEmployerCity("null");
        individualPayload.setEmployerState("null");
        individualPayload.setEmployerCountry("Nigeria");
        individualPayload.setTitle(loan.getCustomer().getTitle());
        individualPayload.setPlaceofBirth("null");
        individualPayload.setWorkphone("null");
        individualPayload.setHomephone("null");
        individualPayload.setSecondaryAddressLine1("null");
        individualPayload.setSecondaryAddressLine2("null");
        individualPayload.setSecondaryAddressCity("null");
        individualPayload.setSecondaryAddressState("null");
        individualPayload.setSecondaryAddressCountry("null");
        individualPayload.setSpousesSurname("null");
        individualPayload.setSpousesFirstname("null");
        individualPayload.setSpousesMiddlename("null");

        //Add to list to create an array as required by CRC
        individualList.add(individualPayload);
        UploadToCreditBureau("Individual", token, individualList);

        //Set the Credit information for upload to Credit Bureau
        List<CRCDataUploadCreditPayload> creditList = new ArrayList<>();
        CRCDataUploadCreditPayload creditPayload = new CRCDataUploadCreditPayload();
        creditPayload.setCustomerID(loan.getCustomer().getCustomerNumber());
        creditPayload.setAccountNumber(loan.getDisbursementAccount());
        creditPayload.setAccountStatus("Open");
        creditPayload.setAccountStatusDate("");
        creditPayload.setDateOfLoan(dtFormatter.format(loan.getDisbursedAt()));
        creditPayload.setCreditLimit(loan.getLoanAmountApproved().toString());
        creditPayload.setLoanAmount(loan.getLoanAmountApproved().toString());
        creditPayload.setOutstandingBalance(loan.getLoanAmountApproved().toString());
        creditPayload.setInstalmentAmount(loan.getMonthlyRepayment().toString());
        creditPayload.setCurrency("Naira");
        creditPayload.setDaysInArrears("0");
        creditPayload.setOverdueAmount("0");
        creditPayload.setLoanType("Commercial revolving renewable short-term credit");
        creditPayload.setLoanTenor(loan.getLoanTenor());
        creditPayload.setRepaymentFrequency("Monthly");
        creditPayload.setLastPaymentDate("");
        creditPayload.setLastPaymentAmount("");
        creditPayload.setMaturityDate(dtFormatter.format(loan.getMaturedAt()));
        creditPayload.setLoanClassification("Performing");
        creditPayload.setLegalChallengeStatus("");
        creditPayload.setLitigationDate("");
        creditPayload.setConsentStatus("");
        creditPayload.setLoanSecurityStatus("");
        creditPayload.setCollateralType("");
        creditPayload.setCollateralDetails("");
        creditPayload.setPreviousAccountNumber("");
        creditPayload.setPreviousName("");
        creditPayload.setPreviousCustomerID("");
        creditPayload.setPreviousBranchCode("");

        //Add to list to create array as required CRC 
        creditList.add(creditPayload);
        UploadToCreditBureau("Credit", token, creditList);

        //Add Guarantor upload
        return CompletableFuture.completedFuture("Success");
    }

    private void UploadToCreditBureau(String customerType, String token, List<?> dataList) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
            String dayMonthFormat = "^([0-2][0-9]|(3)[0-1])(\\/)(((0)[0-9])|((1)[0-2]))(\\/)\\d{4}$";
            String monthDayFormat = "^(0[1-9]|1[0-2])\\/(0[1-9]|1\\d|2\\d|3[01])\\/(19|20)\\d{2}$";
            List<String> accountStatus = Arrays.asList("OPEN", "CLOSED", "FREEZED", "INACTIVE", "WRITTEN OFF");
            List<String> loanClassification = Arrays.asList("PERFORMING", "PASS AND WATCH", "SUB STANDARD", "DOUBTFUL", "LOST");
            List<String> interestType = Arrays.asList("FIXED", "FLOATING");
            List<String> repaymentFrequency = Arrays.asList("BALLOON", "BULLET", "DAILY", "WEEKLY", "FORNIGHTLY", "MONTHLY", "BI MONTHLY", "QUARTERLY", "SEMI ANNUAL", "ANNUAL", "DEMAND", "OTHERS");

            if (customerType.equalsIgnoreCase("Individual")) {
                String requestJson = gson.toJson(dataList);
                requestJson = requestJson.replace("Primarycity", "Primarycity/LGA");
                requestJson = requestJson.replace("SecondaryAddressCity", "SecondaryAddressCity/LGA");
                requestJson = requestJson.replace("emailAddress", "E-mailaddress");
                generateLog("Individual Record Upload To Credit Bureau", token, requestJson, "API Request", "INFO", "");
                Unirest.setTimeouts(0, 0);
                HttpResponse<String> responseJson = Unirest.post(individualDataUploadUrl)
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .field("payload", requestJson)
                        .field("userid", "automations").asString();
                generateLog("Individual Record Upload To Credit Bureau", token, responseJson.getBody(), "API Response", "INFO", "");

            }

            if (customerType.equalsIgnoreCase("Credit")) {
                String requestJson = gson.toJson(dataList);
                //Update key mappings for Credit Information
                requestJson = requestJson.replace("DateOfLoan", "DateOfLoan(Facility)Disbursement/LoanEffectiveDate");
                requestJson = requestJson.replace("LoanLimit", "CreditLimit(Facility)Amount/GlobalLimit");
                requestJson = requestJson.replace("LoanAmount", "Loan(Facility)Amount/AvailedLimit");
                requestJson = requestJson.replace("LoanTenor", "Loan(Facility)Tenor");
                requestJson = requestJson.replace("CreditLimit", "CreditLimit(Facility)Amount/GlobalLimit");
                requestJson = requestJson.replace("LoanType", "Loan(Facility)Type");
                generateLog("Credit Record Upload To Credit Bureau", token, requestJson, "API Request", "INFO", "");
                HttpResponse<String> responseJson = Unirest.post(creditDataUploadUrl)
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .field("payload", requestJson)
                        .field("userid", "automations").asString();
                generateLog("Credit Record Upload To Credit Bureau", token, responseJson.getBody(), "API Response", "INFO", "");

            }

            if (customerType.equalsIgnoreCase("Guarantor")) {
                String requestJson = gson.toJson(dataList);
                //Update the key mappings for Guarantor Information
                requestJson = requestJson.replace("GuarantorsDateOfBirth", "GuarantorsDateOfBirth/Incorporation");
                requestJson = requestJson.replace("GuarantorsPrimaryAddressCity", "GuarantorsPrimaryAddressCity/LGA");
                generateLog("Guarantor Record Upload To Credit Bureau", "", requestJson, "API Request", "INFO", "");
                HttpResponse<String> responseJson = Unirest.post(guarantorDataUploadUrl)
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .field("payload", requestJson)
                        .field("userid", "automations").asString();
                generateLog("Guarantor Record Upload To Credit Bureau", token, responseJson.getBody(), "API Response", "INFO", "");

            }
        } catch (Exception ex) {
            generateLog("Loan Upload To Credit Bureau", token, ex.getMessage(), "API Response", "INFO", "");
        }
    }

    @Override
    public String formatOfsUserCredentials(String ofs, String userCredentials) {
        String[] userCredentialsSplit = userCredentials.split("/");
        String newUserCredentials = userCredentialsSplit[0] + "/#######";
        String newOfsRequest = ofs.replace(userCredentials, newUserCredentials);
        return newOfsRequest;
    }

    @Override
    public String hashAccountBalanceRequest(AccountNumberPayload requestPayload) {
        StringJoiner rawString = new StringJoiner(":");
        rawString.add(requestPayload.getAccountNumber().trim());
        rawString.add(requestPayload.getRequestId().trim());
        return encryptString(rawString.toString(), requestPayload.getToken());
    }

    @Override
    public String hashRubyxLoanRenewal(RubyxLoanRenewalPayload requestPayload, String token) {
        StringJoiner rawString = new StringJoiner(":");
        rawString.add(requestPayload.getCustomerNumber().trim());
        rawString.add(requestPayload.getAccountOfficer().trim());
        rawString.add(requestPayload.getBranchCode().trim());
        rawString.add(requestPayload.getProductCode());
        rawString.add(requestPayload.getRenewalScore().trim());
        rawString.add(requestPayload.getRenewalRating().trim());
        rawString.add(requestPayload.getRenewalAmount().trim());
        rawString.add(requestPayload.getRequestId());
        return encryptString(rawString.toString(), token);
    }

    @Override
    public String hashCustomerDetailsRequest(OmniRequestPayload requestPayload) {
        StringJoiner rawString = new StringJoiner(":");
        rawString.add(requestPayload.getMobileNumber().trim());
        rawString.add(requestPayload.getRequestId().trim());
        return encryptString(rawString.toString(), requestPayload.getToken());
    }

    @Override
    public List<String> getOfsValues(String ofsResponse, String fieldName) {
        List<TextValuePayload> lst = parseOfsResponse(ofsResponse);
        if (lst != null && lst.size() > 0) {
            List<String> values = lst.stream().filter(p -> p.getText().equals(fieldName)).map(p -> p.getValue()).collect(Collectors.toList());
            return values;
        } else {
            return new ArrayList();
        }
    }

    public String getOfsValue(String ofsResponse, String fieldName) {
        List<TextValuePayload> lst = parseOfsResponse(ofsResponse);
        if (lst != null && lst.size() > 0) {
            Optional<TextValuePayload> value = lst.stream().filter(p -> p.getText().equals(fieldName)).findFirst();
            if (value.isPresent()) {
                return value.get().getValue();
            } else {
                return "";
            }
        } else {
            return "";
        }
    }

    @Override
    public String sendLoanEmail(NotificationPayload notificationPayload) {
        Email email = new SimpleEmail();
        email.setHostName("smtppro.zoho.com");
        email.setSmtpPort(587);
        email.setAuthentication("takano@accionmfb.com", "Oluwatimah@100");
        email.setStartTLSEnabled(true);

        String send = "";
        try {
            String message;
            if (notificationPayload.getStatus().equalsIgnoreCase("DISBURSED")) {
                message = "<img style=\"text-align:center;margin-right:40%;margin-left:40%;\" src=\"https://www.accionmfb.com/images/about/logo.png\" width=\"200\" height=\"110\" /><hr/>"
                        + "<div style=\"width:80%;height:100%;\"><br/>"
                        + "<b>Digital Loan Booking Notification!</b><br/>"
                        + "<b>Dear Team Call Center" + "</b><br/>"
                        + "<p> Kindly reach out to the Brighta Loan customer with details below for further authentication. </p>"
                        + "<p>Customer Name: <b style=\"color:#ea7600;\">" + notificationPayload.getRecipientName() + "</b></p>"
                        + "<p>Account Number    : <b style=\"color:#ea7600;\">" + notificationPayload.getDisbursementAccount() + "</b></p>"
                        + "<p>Status: <b style=\"color:#ea7600;\">" + notificationPayload.getStatus() + "</b></p>"
                        + "<p>Customer Phone Number: <b style=\"color:#ea7600;\">" + notificationPayload.getMobileNumber() + "</b></p>"
                        + "<p>Loan Process: <b style=\"color:#ea7600;\">" + " Automatic </b></p>"
                        + "<p>Loan ID: <b style=\"color:#ea7600;\">" + notificationPayload.getLoanDisbursementId() + "</b></p>"
                        + "<p>Loan Amount: N <b style=\"color:#ea7600;\">" + notificationPayload.getLoanAmount() + "</b></p>"
                        + "<br/><br/>"
                        + "<img src=\"http://accionmfb.com/images/service/footer.gif\" alt=\"Email footer\" width=\"500\" height=\"100\" \\>\n"
                        + "<p>If you require any assistance, kindly email itservicedesk@accionmfb.com or call ext 127.</p>"
                        + "</div>";
            } else {
                message = "<img style=\"text-align:center;margin-right:40%;margin-left:40%;\" src=\"https://www.accionmfb.com/images/about/logo.png\" width=\"200\" height=\"110\" /><hr/>"
                        + "<div style=\"width:80%;height:100%;\"><br/>"
                        + "<b>Digital Loan Booking Notification!</b><br/>"
                        + "<b>Dear Team Call Center" + "</b><br/>"
                        + "<p> Customer with details below applied for a Loan </p>"
                        + "<p>Customer Name: <b style=\"color:#ea7600;\">" + notificationPayload.getRecipientName() + "</b></p>"
                        + "<p>Disbursement Account: <b style=\"color:#ea7600;\">" + notificationPayload.getDisbursementAccount() + "</b></p>"
                        + "<p>Status: <b style=\"color:#ea7600;\">" + notificationPayload.getStatus() + "</b></p>"
                        + "<p>Failure Reason: <b style=\"color:#ea7600;\">" + notificationPayload.getFailureReason() + "</b></p>"
                        + "<p>Customer Phone Number: <b style=\"color:#ea7600;\">" + notificationPayload.getMobileNumber() + "</b></p>"
                        + "<p>Loan Process: <b style=\"color:#ea7600;\">" + " Automatic</b></p>"
                        + "<p>Loan ID: <b style=\"color:#ea7600;\">" + notificationPayload.getLoanDisbursementId() + "</b></p>"
                        + "<p>Loan Amount: N <b style=\"color:#ea7600;\">" + notificationPayload.getLoanAmount() + "</b></p>"
                        + "<br/><br/>"
                        + "<img src=\"http://accionmfb.com/images/service/footer.gif\" alt=\"Email footer\" width=\"500\" height=\"100\" \\>\n"
                        + "<p>If you require any assistance, kindly email itservicedesk@accionmfb.com or call ext 127.</p>"
                        + "</div>";

            }
            email.setFrom("takano@accionmfb.com");

            String[] recipientEmails = {"dbekenawei@accionmfb.com", "takano@accionmfb.com", "digitalsupport@accionmfb.com", "contact@accionmfb.com"};
            for (String recipientEmail : recipientEmails) {
                email.addTo(recipientEmail);
            }
            email.setSubject(notificationPayload.getEmailSubject());
            email.setContent(message, "text/html");
//            email.setMsg(message);

            send = email.send();
            log.info("EMAIL NOTIFICATION PAYLOAD : {}", gson.toJson(email));
            log.info("EMAIL NOTIFICATION RESPONSE : {}", gson.toJson(send));
        } catch (EmailException e) {
            System.out.println("Failed to send email. Error: " + e.getMessage());
            log.info("Failed to send email. Error : {}", e.getMessage());
        }
        return send;
    }

    public List<TextValuePayload> parseOfsResponse(String ofsResponse) {
        List<TextValuePayload> res = new ArrayList();
        ofsResponse = ofsResponse.replaceAll(":\\d{1,2}:\\d=", "=");
        String text;
        String value;
        for (String item : ofsResponse.split(",")) {

            String[] textValuePair = item.split("=");

            text = textValuePair[0];
            try {
                value = textValuePair[1];
            } catch (Exception ex) {
                value = "";
            }
            res.add(new TextValuePayload(value, text));
        }
        return res;
    }

//    @Override
//    @Async
//    public CompletableFuture<String> processDigitalLoanDisbursement(String token, LoanIdRequestPayload requestPayload)
//     {
//        OmniResponsePayload errorResponse = new OmniResponsePayload();
//        String channel = jwtToken.getChannelFromToken(token);
//        String requestBy = jwtToken.getUsernameFromToken(token);
//        String response = "";
//        //Log the request
//        String requestJson = gson.toJson(requestPayload);
//        generateLog("Digital Loan Disbursement", token, requestJson, "API Request", "INFO", requestPayload.getRequestId());
//        try {
//            //Check if the loan record exist
//            Loan loanRecord = loanRepository.getLoanUsingLoanId(requestPayload.getLoanId());
//            if (loanRecord == null) {
//                //Log the error
//               generateLog("Digital Loan Disbursement", token, messageSource.getMessage("appMessages.loan.record.noexist", new Object[]{requestPayload.getLoanId()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
//                //Create User Activity log
//                createUserActivity("", "Digital Loan Disbursement", "", channel, messageSource.getMessage("appMessages.loan.record.noexist", new Object[0], Locale.ENGLISH), "", 'F');
//
//                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
//                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.loan.record.noexist", new Object[]{requestPayload.getLoanId()}, Locale.ENGLISH));
//                //return gson.toJson(errorResponse);
//                return CompletableFuture.completedFuture(errorResponse.toString());
//            }
//
//            Customer customer = loanRepository.getCustomerUsingMobileNumber(loanRecord.getCustomer().getMobileNumber());
//            if (customer == null) {
//                //Log the error
//                 generateLog("Digital Loan Disbursement", token, messageSource.getMessage("appMessages.customer.noexist", new Object[]{loanRecord.getCustomer().getMobileNumber()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
//                //Create User Activity log
//                 createUserActivity("", "Digital Loan Disbursement", String.valueOf(loanRecord.getLoanAmountApproved()), channel, messageSource.getMessage("appMessages.customer.noexist", new Object[0], Locale.ENGLISH), loanRecord.getCustomer().getMobileNumber(), 'F');
//
//                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
//                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.customer.noexist", new Object[]{loanRecord.getCustomer().getMobileNumber()}, Locale.ENGLISH));
//                return CompletableFuture.completedFuture(errorResponse.toString());
//            }
//
//            //Set the customer number
//            String customerNumber = customer.getCustomerNumber().trim();
//            //Check the status of the customer
//            if (!"ACTIVE".equalsIgnoreCase(customer.getStatus())) {
//                //Log the error
//                 generateLog("Digital Loan Disbursement", token, messageSource.getMessage("appMessages.customer.inactive", new Object[]{loanRecord.getCustomer().getMobileNumber()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
//                //Create User Activity log
//                 createUserActivity(customerNumber, "Digital Loan Disbursement", String.valueOf(loanRecord.getLoanAmountApproved()), channel, messageSource.getMessage("appMessages.customer.inactive", new Object[0], Locale.ENGLISH), loanRecord.getCustomer().getMobileNumber(), 'F');
//
//                errorResponse.setResponseCode(ResponseCodes.CUSTOMER_DISABLED.getResponseCode());
//                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.customer.inactive", new Object[]{loanRecord.getCustomer().getMobileNumber()}, Locale.ENGLISH));
//                return CompletableFuture.completedFuture(errorResponse.toString());
//            }
//
//            //Check the status of the loan request
//            if (!"ACCEPTED".equalsIgnoreCase(loanRecord.getStatus())) {
//                //Log the error
//                 generateLog("Digital Loan Disbursement", token, messageSource.getMessage("appMessages.loan.not.pending", new Object[]{loanRecord.getStatus()}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
//                //Create User Activity log
//                 createUserActivity("", "Digital Loan Disbursement", "", channel, messageSource.getMessage("appMessages.loan.not.pending", new Object[]{loanRecord.getStatus()}, Locale.ENGLISH), "", 'F');
//
//                errorResponse.setResponseCode(ResponseCodes.ACTIVE_LOAN_EXIST.getResponseCode());
//                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.loan.not.pending", new Object[]{loanRecord.getStatus()}, Locale.ENGLISH));
//               return CompletableFuture.completedFuture(errorResponse.toString());
//            }
//
//            //Check if the loan is a digital loan
//            if (!"Digital".equalsIgnoreCase(loanRecord.getLoanSetup().getLoanCategory())) {
//                //Log the error
//                 generateLog("Digital Loan Disbursement", token, messageSource.getMessage("appMessages.loan.not.digital", new Object[0], Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
//                //Create User Activity log
//                 createUserActivity(loanRecord.getCustomer().getCustomerNumber(), "Digital Loan Disbursement", String.valueOf(loanRecord.getLoanAmountApproved()), channel, messageSource.getMessage("appMessages.loan.not.digital", new Object[0], Locale.ENGLISH), loanRecord.getCustomer().getMobileNumber(), 'F');
//
//                errorResponse.setResponseCode(ResponseCodes.INVALID_TYPE.getResponseCode());
//                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.loan.not.digital", new Object[0], Locale.ENGLISH));
//              return CompletableFuture.completedFuture(errorResponse.toString());
//            }
//
//            //Check the channel information
//            AppUser appUser = loanRepository.getAppUserUsingUsername(requestBy);
//            if (appUser == null) {
//                //Log the error
//                 generateLog("Digital Loan Disbursement", token, messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH), "API Response", "INFO", requestPayload.getRequestId());
//                //Create User Activity log
//                 createUserActivity(loanRecord.getDisbursementAccount(), "Digital Loan Disbursement", String.valueOf(loanRecord.getLoanAmountApproved()), channel, messageSource.getMessage("appMessages.user.notexist", new Object[0], Locale.ENGLISH), requestBy, 'F');
//
//                errorResponse.setResponseCode(ResponseCodes.RECORD_NOT_EXIST_CODE.getResponseCode());
//                errorResponse.setResponseMessage(messageSource.getMessage("appMessages.user.notexist", new Object[]{requestBy}, Locale.ENGLISH));
//               return CompletableFuture.completedFuture(errorResponse.toString());
//            }
//
//            //Call the disbursement API
//            DisburseLoanRequestPayload disburse = new DisburseLoanRequestPayload();
//            disburse.setAmount(loanRecord.getLoanAmountApproved().toString());
//            disburse.setBranchCode(digitalBranchCode);
//            disburse.setCategory(digitalLoanCode);
//            disburse.setCurrency("NGN");
//            disburse.setCustomerId(loanRecord.getCustomer().getCustomerNumber());
//            disburse.setDrawDownAccount(loanRecord.getDisbursementAccount());
//            disburse.setFrequency(String.valueOf(loanRecord.getLoanTenor()));
//            disburse.setInterestRate(String.valueOf(loanRecord.getLoanSetup().getInterestRate()));
//            LocalDate valueDate = LocalDate.now();
//            LocalDate maturityDate = valueDate.plusMonths(Long.valueOf(loanRecord.getLoanTenor()));
//            disburse.setMaturityDate(maturityDate.toString().replace("-", ""));
//            disburse.setValueDate(valueDate.toString().replace("-", ""));
//
//            String ofsRequest = gson.toJson(disburse);
//
//            //Generate the OFS Response log
//             generateLog("Digital Loan Disbursement", token, ofsRequest, "OFS Request", "INFO", requestPayload.getRequestId());
//            String middlewareResponse =  postToMiddleware("/loan/disburseDigitalLoan", ofsRequest);
//
//            //Generate the OFS Response log
//             generateLog("Digital Loan Disbursement", token, middlewareResponse, "OFS Response", "INFO", requestPayload.getRequestId());
//            String validationResponse =  validateT24Response(middlewareResponse);
//            if (validationResponse != null) {
//                //Log the response
//                generateLog("Digital Loan Disbursement", token, validationResponse, "API Error", "DEBUG", requestPayload.getRequestId());
//
//                errorResponse.setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
//                errorResponse.setResponseMessage(validationResponse);
//                return CompletableFuture.completedFuture(errorResponse.toString());
//            }
//
//            DisburseLoanResponsePayload disbursePayload = gson.fromJson(middlewareResponse, DisburseLoanResponsePayload.class);
//            if (!"00".equalsIgnoreCase(disbursePayload.getResponseCode())) {
//                //Log the response
//                 generateLog("Digital Loan Disbursement", token, validationResponse, "API Error", "DEBUG", requestPayload.getRequestId());
//
//                loanRecord.setStatus("FAILED");
//                loanRepository.updateLoan(loanRecord);
//
//                errorResponse.setResponseCode(ResponseCodes.FAILED_TRANSACTION.getResponseCode());
//                errorResponse.setResponseMessage(disbursePayload.getResponseMessage());
//                return CompletableFuture.completedFuture(errorResponse.toString());
//            }
//
//            //Disbursement was successful
//            loanRecord.setDisbursedAt(LocalDate.now());
//            loanRecord.setStatus("DISBURSED");
//            loanRecord.setLoanDisbursementId(disbursePayload.getContractNumber());
//            loanRepository.updateLoan(loanRecord);
//
//            LoanResponsePayload loanResponse = new LoanResponsePayload();
//            loanResponse.setCustomerName(loanRecord.getCustomer().getLastName() + ", " + loanRecord.getCustomer().getOtherName());
//            loanResponse.setFirstRepaymentDate(LocalDate.now().toString());
//            loanResponse.setInterestRate(loanRecord.getLoanSetup().getInterestRate() + "%");
//            loanResponse.setLoanAmountRequested(loanRecord.getLoanAmountRequested().toString());
//            loanResponse.setLoanAmountApproved(loanRecord.getLoanAmountApproved().toString());
//            loanResponse.setLoanId(loanRecord.getLoanId());
//            loanResponse.setLoanDisbursementId(loanRecord.getLoanDisbursementId());
//            loanResponse.setLoanTenor(loanRecord.getLoanTenor());
//            loanResponse.setLoanType(loanRecord.getLoanSetup().getLoanName());
//            loanResponse.setMonthlyRepayment(loanRecord.getMonthlyRepayment().toString());
//            loanResponse.setResponseCode(ResponseCodes.SUCCESS_CODE.getResponseCode());
//            response = gson.toJson(loanResponse);
//
//            //Start a thread to send the loan email notification
//            NotificationPayload emailPayload = new NotificationPayload();
//            emailPayload.setMobileNumber(loanRecord.getCustomer().getMobileNumber());
//            emailPayload.setRecipientName(loanRecord.getCustomer().getLastName() + ", " + loanRecord.getCustomer().getOtherName());
//            emailPayload.setRecipientEmail(loanRecord.getCustomer().getEmail());
//            emailPayload.setRequestId(requestPayload.getRequestId());
//            emailPayload.setDisbursementAccount(loanRecord.getDisbursementAccount());
//            emailPayload.setLoanId(loanRecord.getLoanId());
//            emailPayload.setLoanAmount(String.valueOf(loanRecord.getLoanAmountRequested()));
//            emailPayload.setLoanType(loanRecord.getLoanSetup().getLoanName());
//            emailPayload.setToken(token);
//            sendDigitalLoanOfferEmail(emailPayload);
//
//            //Start a thread to send SMS
//            NotificationPayload smsRequest = new NotificationPayload();
//            smsRequest.setInterestRate(loanRecord.getLoanSetup().getInterestRate());
//            smsRequest.setTenor(Integer.valueOf(loanRecord.getLoanTenor()));
//            smsRequest.setAmountApproved(new BigDecimal(loanRecord.getLoanAmountApproved().toString()));
//            smsRequest.setMaturedAt(loanRecord.getMaturedAt());
//            smsRequest.setMobileNumber(loanRecord.getCustomer().getMobileNumber());
//            smsRequest.setDisbursementAccount(loanRecord.getDisbursementAccount());
//            smsRequest.setRequestId(requestPayload.getRequestId());
//            smsRequest.setToken(token);
//             sendLoanSMS(smsRequest);
//
//            //Call the thread to upload loan data to CRC Credit Bureau
//             uploadToCreditBureau(token, loanRecord);
//             return CompletableFuture.completedFuture(response.toString());
//        } catch (Exception ex) {
//            //Log the response
//             generateLog("Digital Loan Disbursement", token, ex.getMessage(), "API Error", "DEBUG", requestPayload.getRequestId());
//
//            errorResponse.setResponseCode(ResponseCodes.INTERNAL_SERVER_ERROR.getResponseCode());
//            errorResponse.setResponseMessage(ex.getMessage());
//          return CompletableFuture.completedFuture(errorResponse.toString());
//        }
//    }

    public String postToSMSService(String requestEndPoint, String requestBody) {
        String middlewareHostIP = "192.168.1.37";
        String middlewareHostPort = "8087";

        log.info("SMS Host: {}", middlewareHostIP);
        log.info("SMS Port: {}", middlewareHostPort);
        try {
            String middlewareEndpoint = "http://" + middlewareHostIP + ":" + middlewareHostPort + "/T24Gateway/services/generic" + requestEndPoint;
            String NONCE = String.valueOf(Math.random());
            String TIMESTAMP = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
            String SignaturePlain = String.format("%s:%s:%s:%s", NONCE, TIMESTAMP, middlewareUsername, middlewareUserSecretKey);
            String SIGNATURE = hash(SignaturePlain, middlewareSignatureMethod);
            Unirest.setTimeouts(0, 0);
            HttpResponse<String> httpResponse = Unirest.post(middlewareEndpoint)
                    .header("Authorization", middlewareAuthorization)
                    .header("SignatureMethod", middlewareSignatureMethod)
                    .header("Accept", "application/json")
                    .header("Timestamp", TIMESTAMP)
                    .header("Nonce", NONCE)
                    .header("Content-Type", "application/json")
                    .header("Signature", SIGNATURE)
                    .body(requestBody)
                    .asString();
            return httpResponse.getBody();
        } catch (UnirestException ex) {
            return ex.getMessage();
        }
    }

    @Override
    public String hashLocalTransferValidationRequest(LocalTransferWithInternalPayload requestPayload) {
        StringJoiner rawString = new StringJoiner(":");
        rawString.add(requestPayload.getMobileNumber().trim());
        rawString.add(requestPayload.getDebitAccount().trim());
        rawString.add(requestPayload.getCreditAccount().trim());
        rawString.add(requestPayload.getAmount().trim());
        rawString.add(requestPayload.getNarration().trim());
        rawString.add(requestPayload.getTransType().trim());
        rawString.add(requestPayload.getBranchCode().trim());
        rawString.add(requestPayload.getInputter().trim());
        rawString.add(requestPayload.getAuthorizer().trim());
        rawString.add(requestPayload.getNoOfAuthorizer().trim());
        rawString.add(requestPayload.getRequestId().trim());
        return encryptString(rawString.toString(), requestPayload.getToken());
    }

}
