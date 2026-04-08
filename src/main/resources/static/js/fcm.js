const firebaseConfig =  {
    apiKey: "AIzaSyBh8CQSJwdpYW0j8ouGeljQYEV3s1JgSeQ",
    authDomain: "pandasy.firebaseapp.com",
    projectId: "pandasy",
    storageBucket: "pandasy.firebasestorage.app",
    messagingSenderId: "869401599651",
    appId: "1:869401599651:web:10fcba84aab6af9ac96916",
    measurementId: "G-D3P4F8JN86"
};

// Initialize Firebase
firebase.initializeApp(firebaseConfig);

const messaging = firebase.messaging();

// Handle foreground messages
messaging.onMessage((payload) => {
    console.log('Message received. ', payload);
    const notificationTitle = payload.notification.title;
    const notificationBody = payload.notification.body;

    $('#custom-alert-title').text(notificationTitle);
    $('#custom-alert-body').text(notificationBody);
    $('#custom-fcm-alert').fadeIn();
});

$(document).on('click', '.custom-alert-close', function() {
    $('#custom-fcm-alert').fadeOut();
});

function requestFcmPermission() {
    console.log('Requesting permission...');
    Notification.requestPermission().then((permission) => {
        if (permission === 'granted') {
            console.log('Notification permission granted.');

            if ('serviceWorker' in navigator) {
                navigator.serviceWorker.register('/firebase-messaging-sw.js')
                    .then(function(registration) {
                        console.log('Service Worker registered with scope:', registration.scope);
                        return messaging.getToken({ serviceWorkerRegistration: registration });
                    })
                    .then((currentToken) => {
                        if (currentToken) {
                            sendTokenToServer(currentToken);
                        } else {
                            console.log('No registration token available. Request permission to generate one.');
                        }
                    })
                    .catch((err) => {
                        console.log('An error occurred while retrieving token or registering SW. ', err);
                    });
            } else {
                console.log('Service workers are not supported.');
                // Fallback
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
            }
        } else {
            console.log('Unable to get permission to notify.');
        }
    });
}

function sendTokenToServer(token) {
    // 1. Check if user is logged in
    $.get('/apis/v1/auth/me')
        .done(function() {
            // 2. Fetch user's active games
            $.get('/apis/v1/fantasy/my-games?status=ONGOING&type=RULE_2')
                .done(function(games) {
                    games.forEach(game => {
                        $.ajax({
                            url: '/apis/v1/fcm/subscribe',
                            type: 'POST',
                            contentType: 'application/json',
                            data: JSON.stringify({ token: token, topic: 'game_' + game.seq }),
                            success: function() {
                                console.log('Subscribed to topic: game_' + game.seq);
                            },
                            error: function(err) {
                                console.log('Error subscribing to topic', err);
                            }
                        });
                    });
                })
                .fail(function() {
                    console.log('Could not fetch games for FCM subscription');
                });
        })
        .fail(function() {
            console.log('User not logged in, skipping game topic subscriptions.');
        });
}

// Request permission on load if we are authenticated (or we can just ask everyone)
$(document).ready(function() {
    requestFcmPermission();
});
