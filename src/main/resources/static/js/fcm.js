const firebaseConfig = {
    apiKey: "AIzaSyBh8CQSJwdpYW0j8ouGeljQYEV3s1JgSeQ",
    authDomain: "pandasy.firebaseapp.com",
    projectId: "pandasy",
    storageBucket: "pandasy.firebasestorage.app",
    messagingSenderId: "869401599651",
    appId: "1:869401599651:web:10fcba84aab6af9ac96916",
    measurementId: "G-D3P4F8JN86"
};

// Firebase Console > Project Settings > Cloud Messaging > Web Push certificates 에서 확인
const VAPID_KEY = 'BHlwxEhHMTRo6N5Y1IVg0ORlL-zomChFe2E4dq1oEIx91M74q15BD5J04lhm_-Ahjrl1NIccGqe0XGlMTBRcEw8';

// iOS / PWA 환경 감지
const isIOS = () => /ipad|iphone|ipod/i.test(navigator.userAgent) && !window.MSStream;
const isInStandaloneMode = () =>
    window.matchMedia('(display-mode: standalone)').matches ||
    window.navigator.standalone === true;
const isFCMSupported = () =>
    'Notification' in window &&
    'serviceWorker' in navigator &&
    'PushManager' in window;

// Initialize Firebase
firebase.initializeApp(firebaseConfig);
const messaging = firebase.messaging();

// 전경 메시지 처리
messaging.onMessage((payload) => {
    const notificationTitle = payload.data?.title || payload.notification?.title;
    const notificationBody = payload.data?.body || payload.notification?.body;
    $('#custom-alert-title').text(notificationTitle);
    $('#custom-alert-body').text(notificationBody);
    $('#custom-fcm-alert').fadeIn();
});

$(document).on('click', '.custom-alert-close', function () {
    $('#custom-fcm-alert').fadeOut();
});

// SW 등록 및 FCM 토큰 발급
function registerAndGetToken() {
    navigator.serviceWorker.register('/firebase-messaging-sw.js')
        .then(function (registration) {
            return messaging.getToken({
                serviceWorkerRegistration: registration,
                vapidKey: VAPID_KEY
            });
        })
        .then((currentToken) => {
            if (currentToken) {
                sendTokenToServer(currentToken);
            } else {
                console.log('FCM: 토큰을 가져올 수 없습니다. 알림 권한을 확인하세요.');
            }
        })
        .catch((err) => {
            console.log('FCM 토큰 발급 오류:', err);
        });
}

// 알림 권한 요청 — 반드시 사용자 클릭 이벤트 안에서 호출
function requestFcmPermission() {
    if (!isFCMSupported()) return;

    Notification.requestPermission().then((permission) => {
        $('#fcm-permission-banner').fadeOut();
        if (permission === 'granted') {
            registerAndGetToken();
        } else {
            console.log('FCM: 알림 권한이 거부되었습니다.');
        }
    });
}

// iOS 홈화면 추가 유도 배너 표시
function showIOSInstallPrompt() {
    if (localStorage.getItem('ios-install-dismissed')) return;
    $('#ios-install-prompt').fadeIn();
}

// FCM 알림 권한 요청 배너 표시
function showFcmPermissionBanner() {
    if (localStorage.getItem('fcm-permission-dismissed')) return;
    $('#fcm-permission-banner').fadeIn();
}

$(document).ready(function () {
    if (isIOS()) {
        if (!isInStandaloneMode()) {
            // iOS + 브라우저: 홈화면 추가 유도
            showIOSInstallPrompt();
            return;
        }
        // iOS + 홈화면(PWA 모드): iOS 16.4+ 에서만 Web Push 지원
        if (!isFCMSupported()) {
            console.log('FCM: iOS 16.4 이상의 홈화면 PWA에서만 푸시 알림이 지원됩니다.');
            return;
        }
    }

    if (!isFCMSupported()) return;

    if (Notification.permission === 'granted') {
        registerAndGetToken();
    } else if (Notification.permission === 'default') {
        showFcmPermissionBanner();
    }
});

// FCM 권한 배너 — 허용 버튼 클릭 시 권한 요청
$(document).on('click', '#fcm-allow-btn', function () {
    requestFcmPermission();
});

// FCM 권한 배너 — 나중에 버튼
$(document).on('click', '#fcm-deny-btn', function () {
    $('#fcm-permission-banner').fadeOut();
    localStorage.setItem('fcm-permission-dismissed', '1');
});

// iOS 홈화면 배너 닫기
$(document).on('click', '.ios-install-close', function () {
    $('#ios-install-prompt').fadeOut();
    localStorage.setItem('ios-install-dismissed', '1');
});

function sendTokenToServer(token) {
    $.get('/apis/v1/auth/me')
        .done(function () {
            $.get('/apis/v1/fantasy/my-games?status=ONGOING&type=RULE_2')
                .done(function (games) {
                    games.forEach(game => {
                        $.ajax({
                            url: '/apis/v1/fcm/subscribe',
                            type: 'POST',
                            contentType: 'application/json',
                            data: JSON.stringify({ token: token, topic: 'game_' + game.seq }),
                            success: function () {
                                console.log('FCM: 토픽 구독 완료 - game_' + game.seq);
                            },
                            error: function (err) {
                                console.log('FCM: 토픽 구독 오류', err);
                            }
                        });
                    });
                })
                .fail(function () {
                    console.log('FCM: 진행 중인 게임을 조회할 수 없습니다.');
                });
        })
        .fail(function () {
            console.log('FCM: 로그인 상태가 아닙니다. 토픽 구독을 건너뜁니다.');
        });
}
