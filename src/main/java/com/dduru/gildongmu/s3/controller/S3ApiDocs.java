package com.dduru.gildongmu.s3.controller;

import com.dduru.gildongmu.s3.dto.request.ImageUploadRequest;
import com.dduru.gildongmu.s3.dto.response.ImageUploadResponse;
import com.dduru.gildongmu.common.annotation.ApiErrorResponses;
import com.dduru.gildongmu.common.dto.ApiResult;
import com.dduru.gildongmu.common.exception.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Tag(name = "Images", description = "이미지 관리 API")
@SecurityRequirement(name = "JWT")
public interface S3ApiDocs {

    String IMAGE_UPLOAD_POLICY = "JPG, JPEG, PNG만 허용하며 파일당 최대 5MB(5,242,880바이트)입니다. "
            + "files 배열에 fileName과 실제 업로드할 파일의 fileSize(바이트)를 전달합니다. "
            + "S3 PUT 요청의 Content-Length는 요청한 fileSize와 같아야 합니다. URL 유효시간은 10분입니다. ";

    @Operation(
            summary = "게시글 이미지 업로드를 위한 Presigned URL 생성",
            description = "게시글 작성 시 사용할 이미지를 S3에 직접 업로드하기 위한 Presigned URL을 생성합니다. "
                    + IMAGE_UPLOAD_POLICY
                    + "여러 파일을 한 번에 요청할 수 있습니다 (최대 10개). "
                    + "파일명은 UUID로 변환되어 중복을 방지합니다. "
                    + "클라이언트는 Presigned URL로 S3에 업로드한 뒤, 받은 fileUrl을 게시글 생성·수정 요청의 photoUrl에 넣습니다."
    )
    @ApiResponse(responseCode = "200", description = "Presigned URL 생성 성공")
    @ApiErrorResponses({
            ErrorCode.INVALID_INPUT_VALUE,
            ErrorCode.INVALID_FILE_EXTENSION,
            ErrorCode.INVALID_FILE_SIZE,
            ErrorCode.UNSUPPORTED_MEDIA_TYPE,
            ErrorCode.UNAUTHORIZED
    })
    ResponseEntity<ApiResult<List<ImageUploadResponse>>> preparePostImageUpload(
            @Valid @RequestBody ImageUploadRequest request
    );

    @Operation(
            summary = "프로필 이미지 업로드를 위한 Presigned URL 생성",
            description = "사용자 프로필 이미지를 S3에 직접 업로드하기 위한 Presigned URL을 생성합니다. "
                    + IMAGE_UPLOAD_POLICY
                    + "여러 파일을 한 번에 요청할 수 있습니다 (최대 10개). "
                    + "파일명은 UUID로 변환되어 중복을 방지합니다. "
                    + "클라이언트는 각 Presigned URL로 직접 S3에 업로드한 후, 받은 fileUrl을 프로필 이미지 변경 요청에 포함합니다."
    )
    @ApiResponse(responseCode = "200", description = "Presigned URL 생성 성공")
    @ApiErrorResponses({
            ErrorCode.INVALID_INPUT_VALUE,
            ErrorCode.INVALID_FILE_EXTENSION,
            ErrorCode.INVALID_FILE_SIZE,
            ErrorCode.UNSUPPORTED_MEDIA_TYPE,
            ErrorCode.UNAUTHORIZED
    })
    ResponseEntity<ApiResult<List<ImageUploadResponse>>> prepareProfileImageUpload(
            @Valid @RequestBody ImageUploadRequest request
    );

    @Operation(
            summary = "나의 여정 대표 이미지 업로드를 위한 Presigned URL 생성",
            description = "나의 여정 대표 사진을 S3에 직접 업로드하기 위한 Presigned URL을 생성합니다. "
                    + IMAGE_UPLOAD_POLICY
                    + "여러 파일을 한 번에 요청할 수 있습니다 (최대 10개). "
                    + "파일명은 UUID로 변환되어 중복을 방지합니다. "
                    + "클라이언트는 각 Presigned URL로 직접 S3에 업로드한 후, 받은 fileUrl을 나의 여정 대표 사진 수정 요청의 photoUrl에 넣습니다."
    )
    @ApiResponse(responseCode = "200", description = "Presigned URL 생성 성공")
    @ApiErrorResponses({
            ErrorCode.INVALID_INPUT_VALUE,
            ErrorCode.INVALID_FILE_EXTENSION,
            ErrorCode.INVALID_FILE_SIZE,
            ErrorCode.UNSUPPORTED_MEDIA_TYPE,
            ErrorCode.UNAUTHORIZED
    })
    ResponseEntity<ApiResult<List<ImageUploadResponse>>> prepareJourneyImageUpload(
            @Valid @RequestBody ImageUploadRequest request
    );

    /*@Operation(
            summary = "설문조사 이미지 업로드를 위한 Presigned URL 생성",
            description = "설문조사 질문 이미지를 S3에 직접 업로드하기 위한 Presigned URL을 생성합니다. "
                    + "여러 파일을 한 번에 요청할 수 있습니다 (최대 10개). "
                    + "설문조사 이미지는 고정된 파일명을 사용합니다 (예: q1.png, q2.png). "
                    + "클라이언트는 각 Presigned URL로 직접 S3에 업로드한 후, 받은 fileUrl을 설문 질문에 저장합니다."
    )
    @ApiResponse(responseCode = "200", description = "Presigned URL 생성 성공")
    @ApiErrorResponses({
            ErrorCode.INVALID_INPUT_VALUE,
            ErrorCode.INVALID_FILE_EXTENSION
    })
    ResponseEntity<ApiResult<List<ImageUploadResponse>>> prepareSurveyImageUpload(@RequestBody ImageUploadRequest request);*/

    @Operation(
            summary = "채팅 이미지 업로드를 위한 Presigned URL 생성",
            description = "채팅으로 보낸 사진을 S3에 직접 업로드하기 위한 Presigned URL을 생성합니다. "
                    + IMAGE_UPLOAD_POLICY
                    + "여러 파일을 한 번에 요청할 수 있습니다 (최대 10개). "
                    + "파일명은 UUID로 변환되어 중복을 방지합니다. "
                    + "클라이언트는 각 Presigned URL로 직접 S3에 업로드한 후, 받은 fileUrl을 채팅 IMAGE 메시지의 content로 전송합니다."
    )
    @ApiResponse(responseCode = "200", description = "Presigned URL 생성 성공")
    @ApiErrorResponses({
            ErrorCode.INVALID_INPUT_VALUE,
            ErrorCode.INVALID_FILE_EXTENSION,
            ErrorCode.INVALID_FILE_SIZE,
            ErrorCode.UNSUPPORTED_MEDIA_TYPE,
            ErrorCode.UNAUTHORIZED
    })
    ResponseEntity<ApiResult<List<ImageUploadResponse>>> prepareChatImageUpload(
            @Valid @RequestBody ImageUploadRequest request
    );
}
