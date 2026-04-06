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

messaging.onBackgroundMessage(function(payload) {
  console.log('[firebase-messaging-sw.js] Received background message ', payload);
  const notificationTitle = payload.notification.title;
  const notificationOptions = {
    body: payload.notification.body,
    icon: '/favicon.png'
  };

  self.registration.showNotification(notificationTitle, notificationOptions);
});
