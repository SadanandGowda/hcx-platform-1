package io.hcxprotocol.functions;

import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ResultSeverityEnum;
import ca.uhn.fhir.validation.SingleValidationMessage;
import ca.uhn.fhir.validation.ValidationResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.hcxprotocol.dto.HCXIntegrator;
import io.hcxprotocol.dto.ResponseError;
import io.hcxprotocol.interfaces.IncomingInterface;
import io.hcxprotocol.utils.Constants;
import io.hcxprotocol.utils.JSONUtils;
import io.hcxprotocol.validator.HCXFHIRValidator;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.swasth.jose.jwe.JweRequest;

import java.io.*;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Incoming implements IncomingInterface {

    private final HCXIntegrator hcxIntegrator = HCXIntegrator.getInstance();

    /**
     * This method is to process the incoming requests.
     *
     * @param jwePayload
     * @param operation
     * @param output
     * @return
     * @throws Exception
     */
    @Override
    public boolean processFunction(String jwePayload, HCXIntegrator.OPERATIONS operation, Map<String, Object> output) throws Exception {
        Map<String, Object> error = new HashMap<>();
        boolean result = false;
        if (!validateRequest(jwePayload, error)) {
            sendResponse(error, output);
        } else if (!decryptPayload(jwePayload, output)) {
            sendResponse(output, output);
        } else if (!validatePayload(JSONUtils.serialize(output.get(Constants.PAYLOAD)), operation, error)) {
            sendResponse(error, output);
        } else {
            if(sendResponse(error, output)) result = true;
        }
        return result;
    }

    @Override
    public boolean validateRequest(String jwePayload, Map<String,Object> error){
        return true;
    }

    @Override
    public boolean decryptPayload(String jwePayload, Map<String,Object> output) {
        try {
            JweRequest jweRequest = new JweRequest(JSONUtils.deserialize(jwePayload, Map.class));
            jweRequest.decryptRequest(getRsaPrivateKey(hcxIntegrator.getPrivateKey()));
            output.put(Constants.HEADERS, jweRequest.getHeaders());
            output.put(Constants.FHIR_PAYLOAD, jweRequest.getPayload());
            return true;
        } catch (Exception e) {
            output.put(HCXIntegrator.ERROR_CODES.ERR_INVALID_ENCRYPTION.toString(), e.getMessage());
            return false;
        }
    }

    private static RSAPrivateKey getRsaPrivateKey(String privateKey) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        InputStream stream = new ByteArrayInputStream(privateKey.getBytes());
        Reader fileReader = new InputStreamReader(stream);
        PemReader pemReader = new PemReader(fileReader);
        PemObject pemObject = pemReader.readPemObject();
        PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(pemObject.getContent());
        KeyFactory factory = KeyFactory.getInstance("RSA");
        return (RSAPrivateKey) factory.generatePrivate(privateKeySpec);
    }

    @Override
    public boolean validatePayload(String fhirPayload, HCXIntegrator.OPERATIONS operation, Map<String,Object> error) throws Exception {
        FhirValidator validator = HCXFHIRValidator.getValidator();
        ValidationResult result = validator.validateWithResult(fhirPayload);
        List<SingleValidationMessage> messages = result.getMessages();
        ObjectMapper mapper = new ObjectMapper();
        Map<String, String> map = mapper.readValue(fhirPayload, Map.class);
        if(map.get("resourceType") != operation.getFhirResourceType()){
            error.put(String.valueOf(HCXIntegrator.ERROR_CODES.ERR_WRONG_DOMAIN_PAYLOAD), "Incorrect eObject is sent as the domain payload");
            return false;
        }
        for(SingleValidationMessage message: messages){
            if(message.getSeverity() == ResultSeverityEnum.ERROR){
                error.put(String.valueOf(HCXIntegrator.ERROR_CODES.ERR_INVALID_DOMAIN_PAYLOAD),message.getMessage());
                return false;
            }
        return true;
    }

    @Override
    public boolean sendResponse(Map<String,Object> error, Map<String,Object> output) {
        Map<String, Object> responseObj = new HashMap<>();
        responseObj.put(Constants.TIMESTAMP, System.currentTimeMillis());
        boolean result = false;
        if (error.isEmpty()) {
            Map<String, Object> headers = (Map<String, Object>) output.get(Constants.HEADERS);
            responseObj.put(Constants.API_CALL_ID, headers.get(Constants.HCX_API_CALL_ID));
            responseObj.put(Constants.CORRELATION_ID, headers.get(Constants.HCX_CORRELATION_ID));
            result = true;
        } else {
            // Fetching only the first error and constructing the error object
            String code = (String) error.keySet().toArray()[0];
            responseObj.put(Constants.ERROR, new ResponseError(code, (String) error.get(code), ""));
        }
        output.put(Constants.RESPONSE_OBJ, responseObj);
        return result;
    }
}
