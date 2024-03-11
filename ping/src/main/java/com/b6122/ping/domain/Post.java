package com.b6122.ping.domain;

import com.b6122.ping.NcpObjectStorageConfig;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.codec.binary.Hex;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.*;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.hibernate.annotations.ColumnDefault;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;


@Entity
@Getter @Setter
@Table(name = "post")
@NoArgsConstructor
public class Post extends TimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id")
    private Long id; //post id

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    @JoinColumn(name = "user_id")
    private User user; //사용자

    @Column
    private String location; //위치

    @Column
    private float latitude; //위도

    @Column
    private float longitude; //경도

    @Column(name = "title")
    private String title; // 제목
    @Column(name = "content", nullable = false, length = 1000)
    private String content;
    @Enumerated(EnumType.STRING)
    private PostScope scope; //공개 범위 [private, friends, public]
    @ColumnDefault("0")
    @Column(name = "viewCount")
    private int viewCount; // 조회수
    @ColumnDefault("0")
    @Column(name = "likeCount")
    private int likeCount; // 좋아요 수
    @OneToMany(mappedBy = "post")
    private List<Like> likes = new ArrayList<>();

    //연관관계 매서드//
    public void setUser(User user) {
        this.user = user;
        user.addPost(this); //user의 posts list에 post(this) 추가
    }


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

    public List<String> putImgs(List<MultipartFile> imgs) {
        List<String > objectNameList = new ArrayList<>();

        for (int i = 0; i < imgs.size(); i++) {
            String objectName = UUID.randomUUID() + "_" + imgs.get(i).getOriginalFilename();
            String bucketName = NcpObjectStorageConfig.PostImgBucketName;
            try {
                putObject(bucketName, objectName, imgs.get(i));
            } catch (IOException e) {
                e.printStackTrace();
            }
            objectNameList.add(objectName);
        }
        return objectNameList;
    }


    private String generateXML(List<String> imgsName) throws IOException {
        try {
            // XML 문서 생성을 위한 객체 생성
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.newDocument();

            // 최상위 요소 생성
            Element deleteElement = document.createElement("Delete");
            document.appendChild(deleteElement);

            // 이미지 이름들을 반복하여 XML에 넣기
            for (String key : imgsName) {
                Element objectElement = document.createElement("Object");
                Element keyElement = document.createElement("Key");
                keyElement.appendChild(document.createTextNode(key));
                objectElement.appendChild(keyElement);
                deleteElement.appendChild(objectElement);
            }

            // XML 문서를 문자열로 변환하여 반환
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            StringWriter stringWriter = new StringWriter();
            transformer.transform(new DOMSource(document), new StreamResult(stringWriter));
            return stringWriter.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

        public void deletePostImgObjectsInStorage(List<String> imgsName) throws IOException {

                HttpClient httpClient = HttpClientBuilder.create().build();
                //ACL 권한을 public-read로 설정.
                HttpPost request = new HttpPost(ENDPOINT + "/" + NcpObjectStorageConfig.ProfileImgBucketName + "?delete=");
                request.addHeader("Host", request.getURI().getHost());

                try {
                    authorization(request, REGION_NAME, ACCESS_KEY, SECRET_KEY);
                } catch (Exception e) {

                }

                try {
                    // MD5 해시 함수 생성
                    MessageDigest md = MessageDigest.getInstance("MD5");
                    md.update(content.getBytes());

                    // MD5 해시 값 얻기
                    byte[] digest = md.digest();

                    // Base64로 인코딩
                    String base64Encoded = Base64.getEncoder().encodeToString(digest);
                    request.addHeader("Content-MD5", base64Encoded);
                    // 출력
                } catch (Exception e) {
                    e.printStackTrace();
                }

                try {
                    // 현재 날짜 및 시간 얻기
                    Date currentDate = new Date();

                    // 날짜 및 시간 형식 지정
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
                    dateFormat.setTimeZone(TimeZone.getTimeZone("UTC")); // 시간대 설정

                    // x-amz-date 생성
                    String xAmzDate = dateFormat.format(currentDate);

                    request.addHeader("x-amz-date", xAmzDate);
                } catch (Exception e) {
                    e.printStackTrace();
                }


                try {
                    // SHA-256 해시 함수 생성
                    MessageDigest digest = MessageDigest.getInstance("SHA-256");
                    byte[] hash = digest.digest(content.getBytes("UTF-8"));

                    // Base64로 인코딩
                    String xAmzContentSha256 = Base64.getEncoder().encodeToString(hash);

                    // 출력
                    request.addHeader("x-amz-content-sha256", xAmzContentSha256);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // XML 내용을 StringEntity에 설정합니다.
                StringEntity entity = new StringEntity(generateXML(imgsName), ContentType.create("text/plain", "UTF-8"));
                request.setEntity(entity);

                HttpResponse response = httpClient.execute(request);
                System.out.println("Response : " + response.getStatusLine());
            }


    //대표사진만 반환
    public byte[] getPostImgObjectBytes(String postImgObjectName) {
        String objectName = postImgObjectName;
        String bucketName = NcpObjectStorageConfig.PostImgBucketName;
        byte[] imgBytes = new byte[0];
        try {
            return imgBytes = getObject(bucketName, objectName);

        } catch (Exception e) {

        }
        return imgBytes;
    }


    //모든 사진 반환
    public List<byte[]> getPostImgObjectsBytes(List<String> postImgObjectsName) {
        List<byte[]> imgsBytes = new ArrayList<byte[]>();
        String bucketName = NcpObjectStorageConfig.PostImgBucketName;
        for(int i = 0; i < postImgObjectsName.size(); i++) {
            String postImgObjectName = postImgObjectsName.get(i);

            try {
                imgsBytes.add(getObject(bucketName, postImgObjectName));

            } catch (Exception e) {

            }
        }
        return imgsBytes;
    }


    //NCP Object Storage에서 이미지 Object를 가져온 후, byte배열로 변환 후 return 한다
    public byte[] getObject(String bucketName, String objectName) throws Exception {
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

    public void putObject(String bucketName, String objectName,
                          MultipartFile imgFile) throws IOException {
        HttpClient httpClient = HttpClientBuilder.create().build();

        //MultipartFile을 전송하기 위해 File로 변환 (MultipartFile -> File)
        //운영체제의 임시 폴더에 저장
        System.out.println("imgFile = " + imgFile);
        System.out.println("imgFile = " + imgFile);
        File file = new File(System.getProperty("java.io.tmpdir")+"/"+objectName);
        file.createNewFile();
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        fileOutputStream.write(imgFile.getBytes());
        fileOutputStream.close();

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
    /*

    //이미지 파일 저장
    public List<String> saveImagesInStorage(List<MultipartFile> images) {
        List<String> savedImageNames = new ArrayList<>();

        for (MultipartFile image : images) {
            // Generate a random file name to prevent duplicate file names
            String randomFileName = UUID.randomUUID().toString();

            // Get the original file extension
            String originalFilename = image.getOriginalFilename();

            // Generate the full file path with the random file name and original file extension
            String imagePath = ImgPathProperties.postImgPath;
            File file = new File(imagePath);

            String imageName = randomFileName + originalFilename;
            //지정한 디렉토리가 없으면 생성
            if (!file.exists()) {
                file.mkdirs();
            }
            // Save the file
            try {
                image.transferTo(new File(imagePath, imageName));
                String path = imagePath + "\\" + imageName;
                savedImageNames.add(path);
            } catch (IOException e) {
                // Handle file saving error
                e.printStackTrace();
            }
        }

        return savedImageNames; //local storage path list return
    }

    //대표 이미지 반환
    public byte[] getByteArrayOfFirstImgByPath() {
//        byte[] fileByteArray = Files.readAllBytes("파일의 절대경로");
        if (!this.getImgPaths().isEmpty()) {
            try {
                Resource resource = new UrlResource(Path.of(this.getImgPaths().get(0)).toUri());
                if (resource.exists() && resource.isReadable()) {
                    // InputStream을 사용하여 byte 배열로 변환
                    try (InputStream inputStream = resource.getInputStream()) {
                        byte[] data = new byte[inputStream.available()];
                        inputStream.read(data);
                        return data;
                    }
                } else {
                    // 이미지를 찾을 수 없는 경우 예외 또는 다른 처리 방법을 선택
                    throw new RuntimeException("Image not found");
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return new byte[0];
    }

    //모든 이미지 반환
    public List<byte[]> getByteArraysOfImgsByPaths() {

        List<String> imagePaths = this.getImgPaths();
        List<byte[]> byteArrays = new ArrayList<>();

        for (String imagePath : imagePaths) {
            try {
                Resource resource = new UrlResource(Path.of(imagePath).toUri());
                if (resource.exists() && resource.isReadable()) {
                    // InputStream을 사용하여 byte 배열로 변환
                    try (InputStream inputStream = resource.getInputStream()) {
                        byte[] data = inputStream.readAllBytes();
                        byteArrays.add(data);
                    }
                } else {
                    // 이미지를 찾을 수 없는 경우 예외 또는 다른 처리 방법을 선택
                    throw new RuntimeException("Image not found: " + imagePath);
                }
            } catch (IOException e) {
                throw new RuntimeException("Error reading image: " + imagePath, e);
            }
        }

        return byteArrays;
    }


*/

}
