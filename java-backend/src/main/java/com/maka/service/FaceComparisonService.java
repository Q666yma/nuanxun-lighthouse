package com.maka.service;
import org.springframework.web.multipart.MultipartFile;
public interface FaceComparisonService {
    String compareFace(MultipartFile file) throws Exception;
    void addFace(MultipartFile file, String userId, String userName) throws Exception;
    boolean checkQuality(MultipartFile file) throws Exception;
    void deleteUser(String userId) throws Exception;

    String compareFaceFromVideo(MultipartFile file) throws Exception;
}
