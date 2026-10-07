# 이미지 업로드

게시글, 프로필, 여정, 채팅 이미지에 동일한 업로드 정책을 적용합니다.

- 허용 확장자: `jpg`, `jpeg`, `png` (대소문자 구분 없음)
- 이미지 한 장당 1~5,242,880바이트 (최대 5MB)
- Presigned URL 발급 요청당 최대 10개
- Presigned URL 유효시간: 10분
- 여정 게시글 첨부 개수 제한은 기존 최대 4장 유지

## URL 발급 요청

기존 `fileNames` 배열 대신 `files` 배열로 파일명과 크기를 함께 전달합니다.
이 변경은 기존 클라이언트와 호환되지 않으므로 앱 요청 형식도 함께 수정해야 합니다.

`POST /api/v1/images/{posts|profiles|journeys|chats}/presigned-url`

```json
{
  "files": [
    { "fileName": "photo.jpg", "fileSize": 1048576 },
    { "fileName": "photo.png", "fileSize": 5242880 }
  ]
}
```

`fileSize`는 압축/리사이즈 등 처리를 완료한 실제 업로드 파일의 바이트 수입니다.
크기 누락, 0 이하, 5MB 초과 요청은 `INVALID_INPUT_VALUE`로 거절합니다.
서비스 계층의 파일 크기 검증에서는 `INVALID_FILE_SIZE`로 거절합니다.
GIF 등 지원하지 않는 확장자는 `INVALID_FILE_EXTENSION`으로 거절합니다.

## S3 업로드

응답의 `presignedUrl`로 파일 바이너리를 PUT합니다. multipart/form-data로 감싸지 않습니다.
`Content-Length`가 서명에 포함되므로 요청한 `fileSize`와 정확히 같아야 합니다.
Android에서는 길이가 정해진 파일 RequestBody를 사용하고, 길이를 알 수 없는 스트리밍 업로드는 피합니다.
파일이 변경되었다면 변경된 크기로 URL을 다시 발급받습니다.

업로드 성공 후 응답의 `fileUrl`을 게시글/프로필/여정/채팅 API에 전달합니다.
응답 필드 `presignedUrl`, `fileUrl`은 기존과 동일합니다.

## 적용 범위

새 URL 발급과 저장 요청의 이미지 URL 검증에서 GIF를 허용하지 않습니다.
이미 저장된 파일을 삭제하거나 기존 조회 응답을 변경하지는 않습니다.
기존 GIF URL을 수정 요청에 다시 제출하면 이미지 URL 검증에서 거절될 수 있습니다.
이미 발급된 URL은 만료될 때까지 이전 조건이 적용됩니다.
이 검증은 확장자와 크기 기준이며, 파일 바이트의 실제 이미지 형식을 판별하지는 않습니다.
