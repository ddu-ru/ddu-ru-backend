# 012. 게시글 공유 및 Android App Link 연동 가이드

게시글 상세 화면에서 HTTPS 링크를 공유하고, 앱이 설치된 기기에서는 해당 게시글 상세 화면을 직접 엽니다. 앱이 설치되지 않은 경우 동일한 URL에서 모바일 웹 미리보기를 표시합니다.

## 연동 규격

| 구분 | 값 |
|---|---|
| 공유 URL | `https://api.dduru.app/share/posts/{postId}` |
| Asset Links | `https://api.dduru.app/.well-known/assetlinks.json` |
| Android package | `com.gildongmu.dduru` |
| App Link path | `/share/posts/{postId}` |
| 인증 | 없음 |

공유 URL에는 JWT, 사용자 ID 등 개인 정보를 포함하지 않습니다. `postId`만 path variable로 전달합니다.

## 사용자 동작

```text
게시글 상세 공유 버튼
  -> Android Sharesheet로 HTTPS URL 공유
  -> 공유 링크 클릭
      -> 앱 설치 및 App Link 검증 완료: 앱의 게시글 상세 화면
      -> 앱 미설치 또는 검증 전: 모바일 웹 미리보기
```

웹 미리보기 조회는 게시글 조회 수를 증가시키지 않습니다. 삭제되었거나 존재하지 않는 게시글은 HTTP 404와 안내 HTML을 반환합니다.

## Android 구현

### 대상 파일

현재 Android 저장소 기준으로 다음 파일을 수정합니다.

| 파일 | 작업 |
|---|---|
| `app/src/main/AndroidManifest.xml` | App Link intent filter 등록 |
| `presentation/navigation/RootNavigation.kt` | 수신한 `postId`를 `RootNavRoute.MatchingDetail`로 연결하고 비어 있는 `onShareClick` 구현 |
| `presentation/feature/matching/post/component/MatchingDetailAppBarComponent.kt` | 상세 화면 공유 버튼 이벤트 연결 |

공유 Intent 생성 로직은 별도의 helper로 분리해도 됩니다.

### Manifest

`MainActivity`에 아래 intent filter를 추가합니다.

```xml
<intent-filter android:autoVerify="true">
    <action android:name="android.intent.action.VIEW" />

    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />

    <data
        android:scheme="https"
        android:host="api.dduru.app"
        android:pathPrefix="/share/posts/" />
</intent-filter>
```

### 공유하기

상세 화면의 `postId`로 공유 URL을 만들고 Android Sharesheet를 실행합니다.

```kotlin
fun sharePost(context: Context, postId: Long, title: String) {
    val shareUrl = "https://api.dduru.app/share/posts/$postId"
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, title)
        putExtra(
            Intent.EXTRA_TEXT,
            "$title\n뚜르에서 여행 동행을 확인해 보세요.\n$shareUrl"
        )
    }

    context.startActivity(Intent.createChooser(intent, "게시글 공유"))
}
```

### 링크 수신

수신한 URI가 아래 조건을 모두 만족할 때 마지막 path segment를 `postId`로 파싱합니다.

- scheme: `https`
- host: `api.dduru.app`
- path: `/share/posts/{positiveLong}`

유효한 값이면 `RootNavRoute.MatchingDetail(postId)`로 이동합니다. 앱이 종료된 상태와 실행 중인 상태 모두 처리하고, 같은 링크가 재전달되어 동일 상세 화면이 중복으로 쌓이지 않게 합니다.

게시글 조회는 비로그인으로 허용합니다. 좋아요·참여 신청 등 인증이 필요한 동작에서만 로그인 화면으로 이동하고, 로그인 완료 후 원래 `postId`의 상세 화면으로 복귀합니다.

## Asset Links와 운영 서명

현재 앱이 Play 배포 전이므로 운영 인증서 지문이 설정되기 전까지 `assetlinks.json`은 빈 배열을 반환합니다. 이 상태에서도 공유 URL과 웹 미리보기는 정상 동작하지만 Android가 링크를 앱으로 자동 연결하지는 않습니다.

Play 배포 준비 후 Android 팀은 Play Console의 다음 값을 백엔드 팀에 전달합니다.

```text
설정 -> 앱 무결성 -> 앱 서명 키 인증서 -> SHA-256 인증서 지문
```

백엔드 운영 환경에는 다음 값을 설정합니다.

```properties
ANDROID_APP_LINK_SHA256_FINGERPRINTS=AA:BB:CC:...
ANDROID_PLAY_STORE_URL=https://play.google.com/store/apps/details?id=com.gildongmu.dduru
```

인증서가 여러 개면 지문을 쉼표로 구분합니다.

```properties
ANDROID_APP_LINK_SHA256_FINGERPRINTS=AA:BB:CC:...,11:22:33:...
```

## 응답 확인

운영 인증서 설정 전:

```http
GET /.well-known/assetlinks.json

200 OK
Content-Type: application/json

[]
```

운영 인증서 설정 후:

```json
[
  {
    "relation": [
      "delegate_permission/common.handle_all_urls"
    ],
    "target": {
      "namespace": "android_app",
      "package_name": "com.gildongmu.dduru",
      "sha256_cert_fingerprints": [
        "AA:BB:CC:..."
      ]
    }
  }
]
```

`assetlinks.json`은 HTTPS에서 인증 없이 접근 가능해야 하고 리다이렉트 없이 JSON을 반환해야 합니다.

## QA 체크리스트

- [ ] 상세 화면 공유 버튼에서 Android Sharesheet가 열린다.
- [ ] 공유 메시지에 제목과 정확한 `postId` URL이 포함된다.
- [ ] 앱 종료 상태에서 링크를 클릭하면 해당 상세 화면이 열린다.
- [ ] 앱 실행 중 링크를 클릭해도 해당 상세 화면으로 이동한다.
- [ ] 비로그인 사용자도 상세 화면을 볼 수 있다.
- [ ] 인증이 필요한 동작 후 로그인하면 원래 상세 화면으로 복귀한다.
- [ ] 앱 미설치 상태에서는 모바일 웹 미리보기가 열린다.
- [ ] 삭제·미존재 게시글은 앱과 웹에서 오류 상태를 표시한다.
- [ ] Play App Signing 지문 설정 후 App Link 도메인 검증이 성공한다.

## 백엔드 구현 위치

- 공유 웹 진입점: `PostShareController`
- App Link 검증 응답: `AndroidAppLinkController`
- 공유 환경 설정: `PostShareProperties`
- 모바일 웹 템플릿: `templates/share/post-detail.html`
