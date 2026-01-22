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
            localStorage.setItem('isLoggedIn', 'true');
            if (response && response.name) {
                localStorage.setItem('userName', response.name);
            }
            closeLoginModal();

            // Check for redirect
            const redirectUrl = localStorage.getItem('loginRedirect');
            if (redirectUrl) {
                localStorage.removeItem('loginRedirect');
                window.location.href = redirectUrl;
            } else {
                location.reload();
            }
        },
        error: function(xhr) {
            $('#login-error').text('Invalid user ID or password').show();
        }
    });
}

function logout() {
    $.post('/apis/v1/auth/logout', function() {
        localStorage.removeItem('isLoggedIn');
        localStorage.removeItem('userName');
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

    // Check for login query param
    const urlParams = new URLSearchParams(window.location.search);
    if (urlParams.get('login') === 'required') {
        openLoginModal();
        const redirectTarget = urlParams.get('redirect');
        if (redirectTarget) {
             localStorage.setItem('loginRedirect', redirectTarget);
        }
        // Clean URL
        const newUrl = window.location.protocol + "//" + window.location.host + window.location.pathname;
        window.history.replaceState({path: newUrl}, '', newUrl);
    }
});

// Global AJAX Setup for jQuery
$.ajaxSetup({
    statusCode: {
        403: function() {
            openLoginModal();
        },
        401: function() {
            openLoginModal();
        }
    }
});

// Basic Fetch Interceptor
const originalFetch = window.fetch;
window.fetch = async function(...args) {
    const response = await originalFetch(...args);
    if (response.status === 403 || response.status === 401) {
        openLoginModal();
    }
    return response;
};
