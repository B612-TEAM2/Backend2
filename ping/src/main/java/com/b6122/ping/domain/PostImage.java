package com.b6122.ping.domain;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Getter
public class PostImage {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    public Post post;

    public String postImageName;

    public static PostImage createPostImage(Post post, String imageName) {
        PostImage postImage = new PostImage();
        postImage.post = post;
        postImage.postImageName = imageName;

        return postImage;
    }

}
