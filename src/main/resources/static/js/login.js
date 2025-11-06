/**
 * 로그인 페이지 JavaScript
 */

document.addEventListener('DOMContentLoaded', function() {
    const loginForm = document.getElementById('login-form');
    const usernameInput = document.getElementById('username');
    const passwordInput = document.getElementById('password');
    const togglePasswordBtn = document.getElementById('toggle-password');
    const loginBtn = document.getElementById('login-btn');
    const loginBtnText = document.getElementById('login-btn-text');
    const loginBtnSpinner = document.getElementById('login-btn-spinner');
    const errorMessage = document.getElementById('error-message');
    const errorText = document.getElementById('error-text');

    // 비밀번호 보기/숨기기 토글
    togglePasswordBtn.addEventListener('click', function() {
        const type = passwordInput.getAttribute('type') === 'password' ? 'text' : 'password';
        passwordInput.setAttribute('type', type);
        
        const icon = togglePasswordBtn.querySelector('i');
        if (type === 'password') {
            icon.classList.remove('fa-eye-slash');
            icon.classList.add('fa-eye');
        } else {
            icon.classList.remove('fa-eye');
            icon.classList.add('fa-eye-slash');
        }
    });

    // 에러 메시지 숨기기
    function hideError() {
        errorMessage.style.display = 'none';
    }

    // 에러 메시지 표시
    function showError(message) {
        errorText.textContent = message;
        errorMessage.style.display = 'flex';
        errorMessage.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    }

    // 로딩 상태 설정
    function setLoading(loading) {
        if (loading) {
            loginBtn.disabled = true;
            loginBtnText.style.display = 'none';
            loginBtnSpinner.style.display = 'inline-block';
        } else {
            loginBtn.disabled = false;
            loginBtnText.style.display = 'inline';
            loginBtnSpinner.style.display = 'none';
        }
    }

    // 로그인 폼 제출
    loginForm.addEventListener('submit', async function(e) {
        e.preventDefault();
        hideError();

        const username = usernameInput.value.trim();
        const password = passwordInput.value;

        // 입력 검증
        if (!username) {
            showError('사용자명을 입력하세요');
            usernameInput.focus();
            return;
        }

        if (!password) {
            showError('비밀번호를 입력하세요');
            passwordInput.focus();
            return;
        }

        setLoading(true);

        try {
            const response = await fetch('/api/auth/login', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                credentials: 'include',  // 쿠키 수신을 위해 필요
                body: JSON.stringify({
                    username: username,
                    password: password
                })
            });

            const data = await response.json();
            
            console.log('로그인 응답:', { status: response.status, ok: response.ok, data });

            if (!response.ok) {
                // 에러 응답 처리
                const errorMsg = data.message || data.error || '로그인에 실패했습니다';
                showError(errorMsg);
                setLoading(false);
                return;
            }

            // 로그인 성공
            // 쿠키는 서버에서 자동으로 설정되므로, 응답에 token이 없어도 괜찮음
            if (data.username) {
                // username만 localStorage에 저장 (UI 표시용)
                localStorage.setItem('username', data.username);
                
                console.log('로그인 성공 - 리다이렉트 실행');
                
                // 메인 페이지로 리다이렉트
                window.location.href = '/';
            } else {
                console.error('로그인 응답 형식 오류:', data);
                showError('로그인 응답 형식이 올바르지 않습니다');
                setLoading(false);
            }

        } catch (error) {
            console.error('로그인 오류:', error);
            showError('로그인 중 오류가 발생했습니다. 다시 시도해주세요.');
            setLoading(false);
        }
    });

    // 입력 필드 포커스 시 에러 숨기기
    usernameInput.addEventListener('focus', hideError);
    passwordInput.addEventListener('focus', hideError);

    // Enter 키로 폼 제출 (기본 동작 유지)
    usernameInput.addEventListener('keypress', function(e) {
        if (e.key === 'Enter') {
            passwordInput.focus();
        }
    });

    passwordInput.addEventListener('keypress', function(e) {
        if (e.key === 'Enter' && !loginBtn.disabled) {
            loginForm.dispatchEvent(new Event('submit'));
        }
    });
});

