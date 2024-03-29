package uk.gov.ida.stuboidcclient.services;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.util.JSONObjectUtils;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.openid.connect.sdk.rp.ApplicationType;
import net.minidev.json.JSONObject;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.ida.stuboidcclient.configuration.StubOidcClientConfiguration;
import uk.gov.ida.stuboidcclient.rest.Urls;

import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.Security;
import java.text.ParseException;
import java.util.Date;
import java.util.UUID;

import static java.util.Arrays.asList;

public class RegistrationService {

    private final RedisService redisService;
    private final StubOidcClientConfiguration configuration;

    public RegistrationService(RedisService redisService, StubOidcClientConfiguration configuration) {
        this.redisService = redisService;
        this.configuration = configuration;
    }

    private static final Logger LOG = LoggerFactory.getLogger(RegistrationService.class);

    public String sendRegistrationRequest(String ssa, String privateKey) throws JOSEException, ParseException, IOException {
        SignedJWT signedJWT;
        try {
            signedJWT = SignedJWT.parse(ssa);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        HttpResponse<String> httpResponse = sendClientRegRequest(signedJWT, privateKey);
        String body = httpResponse.body();
        LOG.info("HTTP RESPONSE AS STRING: " + body);
            JSONObject jsonObjectResponse = JSONObjectUtils.parse(body);
            saveClientID(jsonObjectResponse.get("client_id").toString());

        return body;
    }

    private HttpResponse sendClientRegRequest(SignedJWT jwt, String privateKey) throws JOSEException, IOException {
        URI uri = UriBuilder.fromUri(configuration.getStubOpURI()).path(Urls.StubOp.REGISTER).build();
        JWTClaimsSet registrationRequest = getRegistrationClaims(jwt.serialize());
        SignedJWT signedClientMetadata = createSignedClientMetadata(registrationRequest, privateKey);

        return sendHttpRequest(uri, signedClientMetadata.serialize());
    }

    private JWTClaimsSet getRegistrationClaims(String seralizedSoftwareStatement) {
        JWTClaimsSet registrationClaims = new JWTClaimsSet.Builder()
        .issuer(configuration.getSoftwareID())
        .issueTime(new Date())
        .expirationTime(new Date())
        .audience(configuration.getStubOpURI())
        .jwtID(UUID.randomUUID().toString())
        .claim("redirect_uris", asList(UriBuilder.fromUri(configuration.getStubClientURI()).path(Urls.StubClient.REDIRECT_URI).build().toString()))
        .claim("token_endpoint_auth_method", "tls_client_auth")
        .claim("tls_client_auth_subject_dn", "This MUST contain the Distinguished name (DN) of the certificate that the Client will present to the OP token endpoint.")
        .claim("grant_types", asList("hybrid"))
        .claim("response_types", asList("code id_token", "code id_token token"))
        .claim("application_type", ApplicationType.WEB)
        .claim("id_token_signed_response_alg", JWSAlgorithm.RS256)
        .claim("request_object_signing_alg", JWSAlgorithm.RS256)
        .claim("software_statement", seralizedSoftwareStatement)
        .build();

        return registrationClaims;
    }

    private SignedJWT createSignedClientMetadata(JWTClaimsSet registrationRequestClaims, String privateKeyString) throws JOSEException, IOException {
        Security.addProvider(new BouncyCastleProvider());

        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256).build();
        privateKeyString = privateKeyString.replaceAll("\\n", "").replace("-----BEGIN RSA PRIVATE KEY-----", "").replace("-----END RSA PRIVATE KEY-----", "");
        String someString = privateKeyString.replaceAll("\\s+", "");

        String anotherString = "-----BEGIN RSA PRIVATE KEY-----\n" + someString + "\n-----END RSA PRIVATE KEY-----";

        PEMParser pemParser = new PEMParser(new StringReader(anotherString));
        JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");
        Object object = pemParser.readObject();
        KeyPair kp = converter.getKeyPair((PEMKeyPair) object);
        PrivateKey privateKey = kp.getPrivate();

        JWSSigner signer = new RSASSASigner(privateKey);
        SignedJWT signedJWT = new SignedJWT(header, registrationRequestClaims);
        signedJWT.sign(signer);

        return signedJWT;
    }

    private HttpResponse<String> sendHttpRequest(URI uri, String postObject) {
        HttpClient httpClient = HttpClient.newBuilder()
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(postObject))
                .uri(uri)
                .build();

        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("This could be 1 out of 2 exceptions. Take your pick", e);
        }
    }

    private void saveClientID(String clientID) {
        redisService.set("CLIENT_ID",clientID);
    }
}

