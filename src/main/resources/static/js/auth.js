// Login Modal Logic
function openLoginModal() {
    document.getElementById('login-modal').style.display = 'flex';
}

function closeLoginModal() {
    document.getElementById('login-modal').style.display = 'none';
    document.getElementById('login-error').style.display = 'none';
    document.getElementById('login-username').value = '';
    document.getElementById('login-password').value = '';
}

function performLogin() {
    const username = $('#login-username').val();
    const password = $('#login-password').val();

    $.ajax({
        url: '/apis/v1/auth/login',
        type: 'POST',
        contentType: 'application/json',
        data: JSON.stringify({ username: username, password: password }),
        success: function(response) {
            localStorage.setItem('accessToken', response.accessToken);
            closeLoginModal();
            location.reload();
        },
        error: function(xhr) {
            $('#login-error').text('Invalid username or password').show();
        }
    });
}

function logout() {
    localStorage.removeItem('accessToken');
    location.reload();
}

// Global Headers for AJAX with JWT
$(document).ajaxSend(function(event, xhr, settings) {
    const token = localStorage.getItem('accessToken');
    if (token) {
        xhr.setRequestHeader('Authorization', 'Bearer ' + token);
    }
});

// Update GNB on load
$(document).ready(function() {
    const token = localStorage.getItem('accessToken');
    const loginBtn = $('#gnb-login-btn');
    if (token) {
        loginBtn.text('Logout');
        loginBtn.attr('onclick', 'logout()');
    } else {
        loginBtn.text('Login');
        loginBtn.attr('onclick', 'openLoginModal()');
    }
});
