package com.b6122.ping.domain;

import com.b6122.ping.ImgPathProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


//이미지 파일 가져오기
//friend 글 목록 페이지: 이미지 , 제몯, 냉ㅇ 15자, 공개범위, 날짜, 좋앙, 요청 id가 좋앙 눌러는지, 최신순
//탈퇴 시 글 삭제
//friend map: 받은 위치정보에 해당하는 글을 찾아 같은 주소로 이미지, 냉ㅇ,공개 범위, 날짜, 제목
//public : 공개범위가 public인 글만

@Entity
@Getter @Setter
@Table(name = "post")
@NoArgsConstructor
public class Post extends TimeEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id")
    private Long id ; //post id

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user; //사용자

    @Column
    private String location; //위치

    @Column
    private float  latitude; //위도

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

    @Column(length = 1000)
    private List<String> imgPaths = new ArrayList<>();

    public void addImgPath(String path) {
        this.imgPaths.add(path);
    }

    //연관관계 매서드//
    public void setUser(User user) {
        this.user = user;
        user.addPost(this); //user의 posts list에 post(this) 추가
    }


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




}
