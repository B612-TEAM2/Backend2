package com.b6122.ping.domain;

import com.b6122.ping.ImgPathProperties;
import com.b6122.ping.NcpObjectStorageConfig;
import com.b6122.ping.dto.UpdateUserProfileDto;
import com.b6122.ping.dto.UserProfileReqDto;
import com.b6122.ping.dto.UserProfileResDto;
import jakarta.persistence.*;
import lombok.*;
import org.apache.commons.codec.binary.Hex;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.URLEncoder;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(unique = true)
    private String nickname; // 사용자가 직접 입력하는 고유닉네임

    /** oauth2 연동 유저정보(username, providerId, provider) **/
    private String provider; //"google", "kakao", etc.
    private String providerId; //google, kakao 등 사용자의 고유Id
    private String username; // provider + _ + providerId

    //NCP Object Storage에 저장되는 파일 이름
    private String profileImgObjectName;

    @Enumerated(EnumType.STRING)
    private UserRole role; // ROLE_USER or ROLE_ADMIN

    @OneToMany(mappedBy = "user")
    private List<Post> posts = new ArrayList<>();


    @Transient
    private final String REGION_NAME = "kr-standard";

    @Transient
    private final String ENDPOINT = NcpObjectStorageConfig.endPoint;

    @Transient
    private final String ACCESS_KEY = NcpObjectStorageConfig.accessKey;

    @Transient
    private final String SECRET_KEY = NcpObjectStorageConfig.secretKey;

    @Transient
    private final String CHARSET_NAME = "UTF-8";

    @Transient
    private final String HMAC_ALGORITHM = "HmacSHA256";

    @Transient
    private final String HASH_ALGORITHM = "SHA-256";

    @Transient
    private final String AWS_ALGORITHM = "AWS4-HMAC-SHA256";

    @Transient
    private final String SERVICE_NAME = "s3";

    @Transient
    private final String REQUEST_TYPE = "aws4_request";

    @Transient
    private final String UNSIGNED_PAYLOAD = "UNSIGNED-PAYLOAD";

    @Transient
    private final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("yyyyMMdd");

    @Transient
    private final SimpleDateFormat TIME_FORMATTER = new SimpleDateFormat("yyyyMMdd\'T\'HHmmss\'Z\'");

    public void addPost(Post p) {//외부에서 post 생성시 posts list에 추가
        this.posts.add(p);
    }

    public void updateProfile(UserProfileReqDto reqDto) {

        String objectName = null;
        if (reqDto.getProfileImg() != null) {
            objectName = UUID.randomUUID() + "_" + reqDto.getProfileImg().getOriginalFilename();
            String bucketName = NcpObjectStorageConfig.ProfileImgBucketName;
            try {
                putObject(bucketName, objectName, reqDto.getProfileImg());
            } catch(IOException e) {
                e.printStackTrace();
            }
        }
        this.nickname = reqDto.getNickname();
        this.profileImgObjectName = objectName;
    }

    //회원 정보(nickname, profileImg, id)
    public UserProfileResDto getProfileInfo() {
        return new UserProfileResDto(nickname, this.getProfileObjectImgBytes(), id);
    }


    public byte[] getProfileObjectImgBytes() {

        String objectName = this.getProfileImgObjectName();
        String bucketName = NcpObjectStorageConfig.ProfileImgBucketName;
        byte[] imgBytes = new byte[0];
        try {
            return imgBytes = getObject(bucketName, objectName);

        } catch (Exception e) {

        }
        return imgBytes;
    }


    //NCP Object Storage에서 이미지 Object를 가져온 후, byte배열로 변환 후 return 한다
    private byte[] getObject(String bucketName, String objectName) throws Exception {
        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpGet request = new HttpGet(ENDPOINT + "/" + bucketName + "/" + objectName);
        request.addHeader("Host", request.getURI().getHost());

        authorization(request, REGION_NAME, ACCESS_KEY, SECRET_KEY);

        HttpResponse response = httpClient.execute(request);
        System.out.println("Response : " + response.getStatusLine());

        InputStream is = response.getEntity().getContent();
        //임시 폴더에 저장
//        File targetFile = new File(System.getProperty("java.io.tmpdir")+"/"+objectName);
//        OutputStream os = new FileOutputStream(targetFile);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[8 * 1024];
        int bytesRead;
        while ((bytesRead = is.read(buffer)) != -1) {
            baos.write(buffer, 0, bytesRead);
        }
        byte[] result = baos.toByteArray();
        baos.close();
        is.close();
        return result;
    }

    private void putObject(String bucketName, String objectName,
                           MultipartFile imgFile) throws IOException {
        HttpClient httpClient = HttpClientBuilder.create().build();

        //MultipartFile을 전송하기 위해 File로 변환 (MultipartFile -> File)
        //운영체제의 임시 폴더에 저장
        File file = new File(System.getProperty("java.io.tmpdir")+"/"+objectName);
        file.createNewFile();
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        fileOutputStream.write(imgFile.getBytes());
        fileOutputStream.close();

        //ACL 권한을 public-read로 설정.
        HttpPut request = new HttpPut(ENDPOINT + "/" + bucketName + "/" + objectName);
        request.addHeader("Host", request.getURI().getHost());
        request.setEntity(new FileEntity(file));

        try {
            authorization(request, REGION_NAME, ACCESS_KEY, SECRET_KEY);
        } catch (Exception e) {

        }
        HttpResponse response = httpClient.execute(request);

        //로컬 임시폴더에 저장된 file 삭제
        file.delete();
        System.out.println("Response : " + response.getStatusLine());
    }

    private void authorization(HttpUriRequest request, String regionName, String accessKey, String secretKey) throws Exception {
        Date now = new Date();
        DATE_FORMATTER.setTimeZone(TimeZone.getTimeZone("UTC"));
        TIME_FORMATTER.setTimeZone(TimeZone.getTimeZone("UTC"));
        String datestamp = DATE_FORMATTER.format(now);
        String timestamp = TIME_FORMATTER.format(now);

        request.addHeader("X-Amz-Date", timestamp);

        request.addHeader("X-Amz-Content-Sha256", UNSIGNED_PAYLOAD);

        String standardizedQueryParameters = getStandardizedQueryParameters(request.getURI().getQuery());

        TreeMap<String, String> sortedHeaders = getSortedHeaders(request.getAllHeaders());
        String signedHeaders = getSignedHeaders(sortedHeaders);
        String standardizedHeaders = getStandardizedHeaders(sortedHeaders);

        String canonicalRequest = getCanonicalRequest(request, standardizedQueryParameters, standardizedHeaders, signedHeaders);
        System.out.println("> canonicalRequest :");
        System.out.println(canonicalRequest);

        String scope = getScope(datestamp, regionName);

        String stringToSign = getStringToSign(timestamp, scope, canonicalRequest);
        System.out.println("> stringToSign :");
        System.out.println(stringToSign);

        String signature = getSignature(secretKey, datestamp, regionName, stringToSign);

        String authorization = getAuthorization(accessKey, scope, signedHeaders, signature);
        request.addHeader("Authorization", authorization);
    }

    private String getAuthorization(String accessKey, String scope, String signedHeaders, String signature) {
        String signingCredentials = accessKey + "/" + scope;
        String credential = "Credential=" + signingCredentials;
        String signerHeaders = "SignedHeaders=" + signedHeaders;
        String signatureHeader = "Signature=" + signature;

        StringBuilder authHeaderBuilder = new StringBuilder().append(AWS_ALGORITHM).append(" ")
                .append(credential).append(", ")
                .append(signerHeaders).append(", ")
                .append(signatureHeader);

        return authHeaderBuilder.toString();
    }

    private byte[] sign(String stringData, byte[] key) throws NoSuchAlgorithmException, UnsupportedEncodingException, InvalidKeyException {
        byte[] data = stringData.getBytes(CHARSET_NAME);
        Mac e = Mac.getInstance(HMAC_ALGORITHM);
        e.init(new SecretKeySpec(key, HMAC_ALGORITHM));
        return e.doFinal(data);
    }

    private String hash(String text) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest e = MessageDigest.getInstance(HASH_ALGORITHM);
        e.update(text.getBytes(CHARSET_NAME));
        return Hex.encodeHexString(e.digest());
    }

    private String getStandardizedQueryParameters(String queryString) throws UnsupportedEncodingException {
        TreeMap<String, String> sortedQueryParameters = new TreeMap<>();
        // sort by key name
        if (queryString != null && !queryString.isEmpty()) {
            String[] queryStringTokens = queryString.split("&");
            for (String field : queryStringTokens) {
                String[] fieldTokens = field.split("=");
                if (fieldTokens.length > 0) {
                    if (fieldTokens.length > 1) {
                        sortedQueryParameters.put(fieldTokens[0], fieldTokens[1]);
                    } else {
                        sortedQueryParameters.put(fieldTokens[0], "");
                    }
                }
            }
        }

        StringBuilder standardizedQueryParametersBuilder = new StringBuilder();
        int count = 0;
        for (String key : sortedQueryParameters.keySet()) {
            if (count > 0) {
                standardizedQueryParametersBuilder.append("&");
            }
            standardizedQueryParametersBuilder.append(key).append("=");

            if (sortedQueryParameters.get(key) != null && !sortedQueryParameters.get(key).isEmpty()) {
                standardizedQueryParametersBuilder.append(URLEncoder.encode(sortedQueryParameters.get(key), CHARSET_NAME));
            }

            count++;
        }
        return standardizedQueryParametersBuilder.toString();
    }

    private TreeMap<String, String> getSortedHeaders(Header[] headers) {
        TreeMap<String, String> sortedHeaders = new TreeMap<>();
        // sort by header name
        for (Header header : headers) {
            sortedHeaders.put(header.getName(), header.getValue());
        }

        return sortedHeaders;
    }

    private String getSignedHeaders(TreeMap<String, String> sortedHeaders) {
        StringBuilder signedHeadersBuilder = new StringBuilder();
        for (String headerName : sortedHeaders.keySet()) {
            signedHeadersBuilder.append(headerName.toLowerCase()).append(";");
        }
        String s = signedHeadersBuilder.toString();
        if (s.endsWith(";")) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }

    private String getStandardizedHeaders(TreeMap<String, String> sortedHeaders) {
        StringBuilder standardizedHeadersBuilder = new StringBuilder();
        for (String headerName : sortedHeaders.keySet()) {
            standardizedHeadersBuilder.append(headerName.toLowerCase()).append(":").append(sortedHeaders.get(headerName)).append("\n");
        }

        return standardizedHeadersBuilder.toString();
    }

    private String getCanonicalRequest(HttpUriRequest request, String standardizedQueryParameters, String standardizedHeaders, String signedHeaders) {
        StringBuilder canonicalRequestBuilder = new StringBuilder().append(request.getMethod()).append("\n")
                .append(request.getURI().getPath()).append("\n")
                .append(standardizedQueryParameters).append("\n")
                .append(standardizedHeaders).append("\n")
                .append(signedHeaders).append("\n")
                .append(UNSIGNED_PAYLOAD);

        return canonicalRequestBuilder.toString();
    }

    private String getScope(String datestamp, String regionName) {
        StringBuilder scopeBuilder = new StringBuilder().append(datestamp).append("/")
                .append(regionName).append("/")
                .append(SERVICE_NAME).append("/")
                .append(REQUEST_TYPE);
        return scopeBuilder.toString();
    }

    private String getStringToSign(String timestamp, String scope, String canonicalRequest) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        StringBuilder stringToSignBuilder = new StringBuilder(AWS_ALGORITHM)
                .append("\n")
                .append(timestamp).append("\n")
                .append(scope).append("\n")
                .append(hash(canonicalRequest));

        return stringToSignBuilder.toString();
    }

    private String getSignature(String secretKey, String datestamp, String regionName, String stringToSign) throws NoSuchAlgorithmException, UnsupportedEncodingException, InvalidKeyException {
        byte[] kSecret = ("AWS4" + secretKey).getBytes(CHARSET_NAME);
        byte[] kDate = sign(datestamp, kSecret);
        byte[] kRegion = sign(regionName, kDate);
        byte[] kService = sign(SERVICE_NAME, kRegion);
        byte[] signingKey = sign(REQUEST_TYPE, kService);

        return Hex.encodeHexString(sign(stringToSign, signingKey));
    }
}
