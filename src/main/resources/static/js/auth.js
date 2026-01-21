// Login Modal Logic
function openLoginModal() {
    document.getElementById('login-modal').style.display = 'flex';
}

function closeLoginModal() {
    document.getElementById('login-modal').style.display = 'none';
    document.getElementById('login-error').style.display = 'none';
    document.getElementById('login-userId').value = '';
    document.getElementById('login-password').value = '';
}

function performLogin() {
    const userId = $('#login-userId').val();
    const password = $('#login-password').val();

    $.ajax({
        url: '/apis/v1/auth/login',
        type: 'POST',
        contentType: 'application/json',
        data: JSON.stringify({ userId: userId, password: password }),
        success: function(response) {
            // Cookie is set by server
            closeLoginModal();
            location.reload();
        },
        error: function(xhr) {
            $('#login-error').text('Invalid username or password').show();
        }
    });
}

function logout() {
    $.post('/apis/v1/auth/logout', function() {
        location.reload();
    });
}

function getCookie(name) {
    const value = `; ${document.cookie}`;
    const parts = value.split(`; ${name}=`);
    if (parts.length === 2) return parts.pop().split(';').shift();
}

// Update GNB on load
$(document).ready(function() {
    // Check if accessToken cookie exists (note: HttpOnly cookies can't be read by JS,
    // so we rely on API call success or specific non-HttpOnly flag cookie for UI state if needed.
    // For now, let's assume if the login API succeeds, we are logged in.
    // However, to persist "Logout" button state on reload without HttpOnly read access:
    // Ideally, server sets a separate "is_logged_in=true" non-HttpOnly cookie.
    // OR we just try to access a protected resource.
    // Simplest for now: Assume session logic or keep a flag in localStorage just for UI state (not security).

    // Better approach: Let's assume we are logged out by default.
    // If we can access a protected API, we are logged in.
    // Or, for this MVP, let's set a localStorage flag 'isLoggedIn' purely for UI toggle.
    // Security relies on the Cookie.

    const isLoggedIn = localStorage.getItem('isLoggedIn');
    const userName = localStorage.getItem('userName');

    const loginBtn = $('#gnb-login-btn');
    const userNameSpan = $('#gnb-user-name');

    if (isLoggedIn) {
        loginBtn.text('Logout');
        loginBtn.attr('onclick', 'logout()');
        if (userName) {
            userNameSpan.text(userName + '님 반갑습니다.');
            userNameSpan.show();
        }
    } else {
        loginBtn.text('Login');
        loginBtn.attr('onclick', 'openLoginModal()');
        userNameSpan.hide();
    }
});

// Intercept Login/Logout to set UI flag
const originalLogin = performLogin;
performLogin = function() {
    const userId = $('#login-userId').val();
    const password = $('#login-password').val();

    $.ajax({
        url: '/apis/v1/auth/login',
        type: 'POST',
        contentType: 'application/json',
        data: JSON.stringify({ userId: userId, password: password }),
        success: function(response) {
            localStorage.setItem('isLoggedIn', 'true');
            if (response && response.name) {
                localStorage.setItem('userName', response.name);
            }
            closeLoginModal();
            location.reload();
        },
        error: function(xhr) {
            $('#login-error').text('Invalid user ID or password').show();
        }
    });
}

const originalLogout = logout;
logout = function() {
    $.post('/apis/v1/auth/logout', function() {
        localStorage.removeItem('isLoggedIn');
        localStorage.removeItem('userName');
        location.reload();
    });
}
