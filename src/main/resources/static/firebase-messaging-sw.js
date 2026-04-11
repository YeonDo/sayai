importScripts('https://www.gstatic.com/firebasejs/9.2.0/firebase-app-compat.js');
importScripts('https://www.gstatic.com/firebasejs/9.2.0/firebase-messaging-compat.js');

const firebaseConfig = {
    apiKey: "AIzaSyBh8CQSJwdpYW0j8ouGeljQYEV3s1JgSeQ",
    authDomain: "pandasy.firebaseapp.com",
    projectId: "pandasy",
    storageBucket: "pandasy.firebasestorage.app",
    messagingSenderId: "869401599651",
    appId: "1:869401599651:web:10fcba84aab6af9ac96916",
    measurementId: "G-D3P4F8JN86"
};

firebase.initializeApp(firebaseConfig);

const messaging = firebase.messaging();

// 백그라운드 메시지 처리
messaging.onBackgroundMessage(function (payload) {
    console.log('[firebase-messaging-sw.js] Background message received:', payload);

    const notificationTitle = payload.data?.title || '새 알림';
    const notificationOptions = {
        body: payload.data?.body || '',
        icon: '/favicon.png',
        badge: '/favicon.png',
        data: payload.data || {}
    };

    self.registration.showNotification(notificationTitle, notificationOptions);
});

// 알림 클릭 시 앱 화면 열기 또는 포커스
self.addEventListener('notificationclick', function (event) {
    event.notification.close();

    event.waitUntil(
        clients.matchAll({ type: 'window', includeUncontrolled: true })
            .then(function (windowClients) {
                for (var i = 0; i < windowClients.length; i++) {
                    var client = windowClients[i];
                    if (client.url.startsWith(self.location.origin) && 'focus' in client) {
                        return client.focus();
                    }
                }
                if (clients.openWindow) {
                    return clients.openWindow('/');
                }
            })
    );
});
