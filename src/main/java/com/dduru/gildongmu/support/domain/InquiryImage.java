package com.dduru.gildongmu.support.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "inquiry_images", indexes = @Index(name = "idx_inquiry_images_inquiry_order", columnList = "inquiry_id, sort_order"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InquiryImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inquiry_id", nullable = false)
    private Inquiry inquiry;

    @Column(name = "image_url", nullable = false, length = 2048)
    private String imageUrl;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    public static InquiryImage of(Inquiry inquiry, String imageUrl, int sortOrder) {
        InquiryImage image = new InquiryImage();
        image.inquiry = inquiry;
        image.imageUrl = imageUrl;
        image.sortOrder = sortOrder;
        return image;
    }
}
