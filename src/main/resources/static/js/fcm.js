const firebaseConfig = {
    apiKey: "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
    authDomain: "pandasy.firebaseapp.com",
    projectId: "pandasy",
    storageBucket: "pandasy.firebasestorage.app",
    messagingSenderId: "12341234",
    appId: "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
    measurementId: "aaaaaaaaaaaaaaaaaaa"
};

// Initialize Firebase
firebase.initializeApp(firebaseConfig);

const messaging = firebase.messaging();

// Handle foreground messages
messaging.onMessage((payload) => {
    console.log('Message received. ', payload);
    const notificationTitle = payload.notification.title;
    const notificationOptions = {
        body: payload.notification.body,
        icon: '/favicon.png'
    };

    // Show a browser notification or a custom alert if permission is granted
    if (Notification.permission === 'granted') {
        new Notification(notificationTitle, notificationOptions);
    } else {
        alert(notificationTitle + "\n" + payload.notification.body);
    }
});

function requestFcmPermission() {
    console.log('Requesting permission...');
    Notification.requestPermission().then((permission) => {
        if (permission === 'granted') {
            console.log('Notification permission granted.');
            // Get Instance ID token. Initially this makes a network call, once retrieved
            // subsequent calls to getToken will return from cache.
            messaging.getToken()
                .then((currentToken) => {
                    if (currentToken) {
                        sendTokenToServer(currentToken);
                    } else {
                        console.log('No registration token available. Request permission to generate one.');
                    }
                }).catch((err) => {
                    console.log('An error occurred while retrieving token. ', err);
                });
        } else {
            console.log('Unable to get permission to notify.');
        }
    });
}

function sendTokenToServer(token) {
    $.ajax({
        url: '/apis/v1/fcm/subscribe',
        type: 'POST',
        contentType: 'application/json',
        data: JSON.stringify({ token: token }),
        success: function() {
            console.log('Subscribed to topic');
        },
        error: function(err) {
            console.log('Error subscribing to topic', err);
        }
    });
}

// Request permission on load if we are authenticated (or we can just ask everyone)
$(document).ready(function() {
    requestFcmPermission();
});
